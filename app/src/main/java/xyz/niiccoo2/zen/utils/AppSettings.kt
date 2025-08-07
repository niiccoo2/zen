package xyz.niiccoo2.zen.utils

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import java.io.IOException
import java.time.LocalDateTime
import java.time.LocalTime

// Create a DataStore instance
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "zen_settings")

// Helper function to check if an app is currently blocked based on its schedule
fun isCurrentlyBlocked( // How does this know what app its checking for...
    scheduledBlocks: List<TimeBlock>,
    currentTime: LocalDateTime
): Boolean {
    if (scheduledBlocks.isEmpty()) {
        return false // No schedule, so not blocked by schedule
    }

    val nowTime = currentTime.toLocalTime()

    for (block in scheduledBlocks) {
        Log.i("TAG", "Checking block: $block")
        Log.i("TAG", "nowTime: $nowTime")
        Log.i("TAG", "block.startTime: ${block.startTime}")
        Log.i("TAG", "block.endTime: ${block.endTime}")

        // Handle "always blocked" case (covers the entire day)
        if (block.startTime == LocalTime.MIN && block.endTime == LocalTime.MAX) {
            return true // Always blocked
        }

        // Handle blocks that DO NOT cross midnight
        if (block.startTime.isBefore(block.endTime) || block.startTime == block.endTime) {
            if ((nowTime.isAfter(block.startTime) || nowTime == block.startTime) &&
                nowTime.isBefore(block.endTime)
            ) {
                return true
            }
        } else { // Handle blocks that DO cross midnight (e.g., 9 PM to 9 AM)
            if (nowTime.isAfter(block.startTime) || nowTime == block.startTime || // After start until midnight
                nowTime.isBefore(block.endTime) // From midnight until end
            ) {
                return true
            }
        }
    }
    return false // Not within any defined block
}


object AppSettings {

    // Increment version if schema changes significantly and needs migration logic (not implemented here)
    private val BLOCKED_APP_SETTINGS_MAP_KEY = stringPreferencesKey("blocked_app_settings_map_v4")

    private val json = Json {
        ignoreUnknownKeys = true // Important for schema evolution
        isLenient = true
        // Make sure LocalTimeSerializer is available to this Json instance if not globally configured
        // serializersModule = SerializersModule { contextual(LocalTime::class, LocalTimeSerializer) }
        // However, @Serializable(with=...) should be sufficient without contextual.
    }

    fun getBlockedAppSettingsMap(context: Context): Flow<Map<String, BlockedAppSettings>> {
        return context.dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    Log.e("AppSettings", "Error reading preferences.", exception)
                    emit(emptyPreferences())
                } else {
                    Log.e("AppSettings", "Non-IOException reading preferences.", exception) // Log other errors too
                    throw exception
                }
            }
            .map { preferences ->
                val jsonString = preferences[BLOCKED_APP_SETTINGS_MAP_KEY]
                if (!jsonString.isNullOrEmpty()) {
                    try {
                        json.decodeFromString<Map<String, BlockedAppSettings>>(jsonString)
                    } catch (e: Exception) {
                        Log.e("AppSettings", "Error decoding BlockedAppSettingsMap JSON V4", e)
                        emptyMap<String, BlockedAppSettings>()
                    }
                } else {
                    emptyMap<String, BlockedAppSettings>()
                }
            }
    }

    private suspend fun saveBlockedAppSettingsMap(context: Context, appSettingsMap: Map<String, BlockedAppSettings>) {
        try {
            val jsonString = json.encodeToString(appSettingsMap)
            context.dataStore.edit { preferences ->
                preferences[BLOCKED_APP_SETTINGS_MAP_KEY] = jsonString
            }
            Log.d("AppSettings", "Saved BlockedAppSettingsMap (v4) successfully.")
        } catch (e: Exception) {
            Log.e("AppSettings", "Error encoding or saving BlockedAppSettingsMap JSON (v4)", e)
        }
    }

    suspend fun updateSpecificAppSetting(
        context: Context,
        packageName: String?,
        updateAction: (BlockedAppSettings) -> BlockedAppSettings
    ) {
        if (packageName.isNullOrEmpty()) { // check for empty too
            Log.w("AppSettings", "updateSpecificAppSetting called with null or empty package name.")
            return
        }

        val currentMap = getBlockedAppSettingsMap(context).first()
        // Ensure packageName is non-null for BlockedAppSettings constructor
        val defaultSettings = BlockedAppSettings(packageName = packageName, scheduledBlocks = TimeBlock.NO_SCHEDULE, isOnBreak = false)
        val existingSettings = currentMap[packageName] ?: defaultSettings

        val newSettings = updateAction(existingSettings)

        val updatedMap = currentMap.toMutableMap()
        updatedMap[packageName] = newSettings
        saveBlockedAppSettingsMap(context, updatedMap.toMap())
        Log.d("AppSettings", "Updated settings (v4) for $packageName: $newSettings")
    }

    suspend fun getSpecificAppSetting(context: Context, packageName: String): BlockedAppSettings? {
        if (packageName.isEmpty()) return null
        return getBlockedAppSettingsMap(context).first()[packageName]
    }

    /**
     * Checks if a specific app is effectively blocked right now.
     * This involves fetching its current settings and evaluating them against the current time.
     * An app is effectively blocked if:
     * 1. It has settings.
     * 2. It's NOT on break.
     * 3. Its current schedule (from settings) indicates it should be blocked NOW.
     *
     * @param context The application context.
     * @param packageName The package name of the app to check.
     * @return True if the app is currently effectively blocked, false otherwise.
     */
    suspend fun isAppEffectivelyBlockedNow(context: Context, packageName: String): Boolean {
        if (packageName.isEmpty()) {
            Log.w("AppSettings", "isAppEffectivelyBlockedNow called with empty package name.")
            return false
        }

        // 1. Get the specific app's settings
        val settings = getSpecificAppSetting(context, packageName)

        // 2. Check if settings exist and if the app is on break
        if (settings == null) {
            // Log.v("AppSettings", "No settings found for $packageName, not blocked.") // Optional: for debugging
            return false // No settings, so not blocked
        }
        if (settings.isOnBreak) {
            // Log.d("AppSettings", "$packageName is on break, not blocked.") // Optional: for debugging
            return false // On break, so not blocked by schedule
        }

        // 3. Use the existing isCurrentlyBlocked logic with the current time
        val now = LocalDateTime.now()
        // 'isCurrentlyBlocked' is the helper function already defined at the top of your file.
        return isCurrentlyBlocked(settings.scheduledBlocks, now)
    }

    suspend fun removeSpecificAppConfiguration(context: Context, packageName: String?) {
        if (packageName.isNullOrEmpty()) {
            Log.w("AppSettings", "removeSpecificAppConfiguration called with null or empty package name.")
            return
        }
        val currentMap = getBlockedAppSettingsMap(context).first()
        if (currentMap.containsKey(packageName)) {
            val updatedMap = currentMap.toMutableMap()
            updatedMap.remove(packageName)
            saveBlockedAppSettingsMap(context, updatedMap.toMap())
            Log.d("AppSettings", "Removed specific configuration (v4) for $packageName")
        }
    }

    // --- NEW FUNCTIONS TO MANAGE BLOCKING STATES ---

    /**
     * Marks an app as "always blocked" by setting its schedule to the full day block.
     * Also ensures its break flag is false.
     */
    suspend fun setAppAsAlwaysBlocked(context: Context, packageName: String) {
        updateSpecificAppSetting(context, packageName) { settings ->
            // Use the convenience function if available and settings is BlockedAppSettings type
            // settings.setAlwaysBlocked(true)
            // settings.copy(isOnBreak = false)
            // Or directly:
            settings.copy(
                scheduledBlocks = TimeBlock.ALWAYS_BLOCKED_SCHEDULE,
                isOnBreak = false // Ensure break is off if setting to always blocked
            )
        }
        Log.d("AppSettings", "Set $packageName as always blocked.")
    }

    /**
     * Clears any "always blocked" state and any custom schedule.
     * The app will not be blocked by schedule unless new blocks are added.
     */
    suspend fun clearAppScheduledBlocking(context: Context, packageName: String) {
        updateSpecificAppSetting(context, packageName) { settings ->
            settings.copy(
                scheduledBlocks = TimeBlock.NO_SCHEDULE, // Clears all schedules
                isOnBreak = false // Also clear break when removing from block list by default
            )
        }
        Log.d("AppSettings", "Cleared scheduled blocking for $packageName.")
    }

    /**
     * Adds a specific time block to an app's schedule.
     */
    suspend fun addCustomScheduleToApp(context: Context, packageName: String, timeBlock: TimeBlock) {
        updateSpecificAppSetting(context, packageName) { settings ->
            val newScheduledBlocks = settings.scheduledBlocks.toMutableList()
            // Avoid adding if it's currently "always blocked" or if block already exists
            if (settings.isEffectivelyAlwaysBlocked) {
                Log.w("AppSettings", "App $packageName is 'always blocked'. Clear that before adding custom schedule.")
                // Decide behavior: replace "always blocked" or ignore?
                // For now, let's assume we replace it:
                newScheduledBlocks.clear()
                newScheduledBlocks.add(timeBlock)
            } else if (!newScheduledBlocks.contains(timeBlock)) {
                newScheduledBlocks.add(timeBlock)
            }
            settings.copy(scheduledBlocks = newScheduledBlocks.toList(), isOnBreak = false)
        }
        Log.d("AppSettings", "Added custom schedule $timeBlock for $packageName.")
    }

    /**
     * Removes a specific time block from an app's schedule.
     */
    suspend fun removeCustomScheduleFromApp(context: Context, packageName: String, timeBlock: TimeBlock) {
        updateSpecificAppSetting(context, packageName) { settings ->
            if (settings.isEffectivelyAlwaysBlocked && timeBlock == TimeBlock.ALWAYS_BLOCKED_SCHEDULE.firstOrNull()) {
                // If removing the "always blocked" marker, set to no schedule
                settings.copy(scheduledBlocks = TimeBlock.NO_SCHEDULE)
            } else {
                val newScheduledBlocks = settings.scheduledBlocks.toMutableList()
                newScheduledBlocks.remove(timeBlock)
                settings.copy(scheduledBlocks = newScheduledBlocks.toList())
            }
        }
        Log.d("AppSettings", "Removed custom schedule $timeBlock for $packageName.")
    }


    suspend fun setAppOnBreak(context: Context, packageName: String?) {
        updateSpecificAppSetting(context, packageName) { settings ->
            settings.copy(isOnBreak = true)
            // Scheduled blocks are preserved. The break temporarily overrides them.
        }
        Log.d("AppSettings", "Set $packageName on break.")
    }

    suspend fun clearAppBreak(context: Context, packageName: String) {
        updateSpecificAppSetting(context, packageName) { settings ->
            settings.copy(isOnBreak = false)
        }
        Log.d("AppSettings", "Cleared break for $packageName.")
    }

    /**
     * Flow that emits the set of package names that are currently *effectively* blocked.
     * An app is effectively blocked if:
     * 1. It's NOT on break, AND
     * 2. Its current schedule indicates it should be blocked NOW.
     */
    fun getEffectivelyBlockedPackagesFlow(context: Context): Flow<Set<String>> {
        return getBlockedAppSettingsMap(context).map { appSettingsMap ->
            val now = LocalDateTime.now() // Get current time once per emission
            appSettingsMap.filter { (_, settings) ->
                // An app is effectively blocked if:
                // It's NOT 'isOnBreak' AND its schedule dictates it's currently blocked time.
                !settings.isOnBreak && isCurrentlyBlocked(settings.scheduledBlocks, now)
            }.keys // Get only the package names
        }
    }


    // --- Functions to replace your original simple Set<String> logic ---

    /**
     * Call this to mark an app as "always blocked". Replaces old add logic.
     */
    suspend fun addAppToAlwaysBlockList(context: Context, packageName: String) {
        setAppAsAlwaysBlocked(context, packageName)
    }

    /**
     * Call this to remove an app from scheduled blocking (clears its schedule).
     * Replaces old remove logic.
     */
    suspend fun removeAppFromBlockList(context: Context, packageName: String?) {
        if (packageName == null) return

        //clearAppScheduledBlocking(context, packageName) // This only clears the schedule

        removeSpecificAppConfiguration(context = context, packageName = packageName)
    }
}
