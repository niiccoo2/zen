package xyz.niiccoo2.zen.utils

// Assuming BlockedAppSettings is now in the same package, or import it:
// import xyz.niiccoo2.zen.data.BlockedAppSettings
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

// Create a DataStore instance
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "zen_settings")

object AppSettings {

    private val BLOCKED_APP_SETTINGS_MAP_KEY = stringPreferencesKey("blocked_app_settings_map_v3") // New version for this structure

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Retrieves the entire map of BlockedAppSettings.
     */
    fun getBlockedAppSettingsMap(context: Context): Flow<Map<String, BlockedAppSettings>> {
        return context.dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    Log.e("AppSettings", "Error reading preferences.", exception)
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                val jsonString = preferences[BLOCKED_APP_SETTINGS_MAP_KEY]
                if (!jsonString.isNullOrEmpty()) {
                    try {
                        json.decodeFromString<Map<String, BlockedAppSettings>>(jsonString)
                    } catch (e: Exception) {
                        Log.e("AppSettings", "Error decoding BlockedAppSettingsMap JSON", e)
                        emptyMap<String, BlockedAppSettings>()
                    }
                } else {
                    emptyMap<String, BlockedAppSettings>()
                }
            }
    }

    /**
     * Saves the entire map of BlockedAppSettings. (Private as updates should go through specific functions)
     */
    private suspend fun saveBlockedAppSettingsMap(context: Context, appSettingsMap: Map<String, BlockedAppSettings>) {
        try {
            val jsonString = json.encodeToString(appSettingsMap)
            context.dataStore.edit { preferences ->
                preferences[BLOCKED_APP_SETTINGS_MAP_KEY] = jsonString
            }
            Log.d("AppSettings", "Saved BlockedAppSettingsMap (v3) successfully.")
        } catch (e: Exception) {
            Log.e("AppSettings", "Error encoding or saving BlockedAppSettingsMap JSON (v3)", e)
        }
    }

    /**
     * Updates settings for a single app or adds it if it doesn't exist.
     */
    suspend fun updateSpecificAppSetting(
        context: Context,
        packageName: String?,
        updateAction: (BlockedAppSettings) -> BlockedAppSettings
    ) {
        if (packageName == null) {
            Log.w("AppSettings", "updateSpecificAppSetting called with null package name. Cannot update settings without a package name.")
            return // Or throw an IllegalArgumentException
        }
        // Now, packageName is smart-cast to non-null String for the rest of this scope.

        val currentMap = getBlockedAppSettingsMap(context).first()
        val existingSettings = currentMap[packageName] // Uses non-null packageName
            ?: BlockedAppSettings(packageName = packageName) // Passes non-null packageName

        val newSettings = updateAction(existingSettings)

        val updatedMap = currentMap.toMutableMap()
        updatedMap[packageName] = newSettings // << NOW SAFE: packageName is non-null
        saveBlockedAppSettingsMap(context, updatedMap.toMap())
        Log.d("AppSettings", "Updated settings (v3) for $packageName: $newSettings")
    }

    /**
     * Retrieves settings for a specific app, or null if not configured.
     */
    suspend fun getSpecificAppSetting(context: Context, packageName: String): BlockedAppSettings? {
        return getBlockedAppSettingsMap(context).first()[packageName]
    }

    /**
     * Removes all specific settings for an app.
     */
    suspend fun removeSpecificAppConfiguration(context: Context, packageName: String?) {
        val currentMap = getBlockedAppSettingsMap(context).first()
        if (currentMap.containsKey(packageName)) {
            val updatedMap = currentMap.toMutableMap()
            updatedMap.remove(packageName)
            saveBlockedAppSettingsMap(context, updatedMap.toMap())
            Log.d("AppSettings", "Removed specific configuration (v3) for $packageName")
        }
    }

    // --- NEW FUNCTIONS TO MANAGE BLOCKING STATES (adapted for simpler BlockedAppSettings) ---

    /**
     * Marks an app as "always blocked" and ensures its break flag is false.
     */
    suspend fun setAppAsAlwaysBlocked(context: Context, packageName: String) {
        updateSpecificAppSetting(context, packageName) { settings ->
            settings.copy(
                isAlwaysBlocked = true,
                isOnBreak = false // Ensure break is off if setting to always blocked
            )
        }
    }

    /**
     * Puts an app on a temporary break by setting its isOnBreak flag to true.
     * The duration of this break is NOT stored here. It must be managed externally
     * (e.g., an AlarmManager will later call clearAppBreak).
     */
    suspend fun setAppOnBreak(context: Context, packageName: String?) {
        updateSpecificAppSetting(context, packageName) { settings ->
            settings.copy(isOnBreak = true)
            // isAlwaysBlocked state is preserved. The break temporarily overrides it.
        }
    }

    /**
     * Clears an app's break state by setting its isOnBreak flag to false.
     * If 'isAlwaysBlocked' is true, the app will become effectively blocked again.
     */
    suspend fun clearAppBreak(context: Context, packageName: String) {
        updateSpecificAppSetting(context, packageName) { settings ->
            settings.copy(isOnBreak = false)
        }
    }

    /**
     * Unmarks an app as "always blocked" and also clears its break state.
     */
    suspend fun setAppAsNotAlwaysBlocked(context: Context, packageName: String) {
        updateSpecificAppSetting(context, packageName) { settings ->
            settings.copy(
                isAlwaysBlocked = false,
                isOnBreak = false
            )
        }
    }


    /**
     * Flow that emits the set of package names that are currently *effectively* blocked.
     * With the simpler BlockedAppSettings, an app is blocked if:
     * 1. It's 'isAlwaysBlocked' AND it's NOT 'isOnBreak'.
     */
    fun getEffectivelyBlockedPackagesFlow(context: Context): Flow<Set<String>> {
        return getBlockedAppSettingsMap(context).map { appSettingsMap ->
            appSettingsMap.filter { (_, settings) ->
                // An app is effectively blocked if:
                // It's marked as 'isAlwaysBlocked' AND it's NOT 'isOnBreak'.
                settings.isAlwaysBlocked && !settings.isOnBreak
            }.keys // Get only the package names
        }
    }


    // --- Functions to replace your original simple Set<String> logic ---

    /**
     * Call this to mark an app as "always blocked". Replaces old add logic.
     */
    suspend fun addAppToBlockList(context: Context, packageName: String) {
        setAppAsAlwaysBlocked(context, packageName)
    }

    /**
     * Call this to unmark an app as "always blocked". Replaces old remove logic.
     * This also ensures its 'isOnBreak' flag is cleared.
     */
    suspend fun removeAppFromBlockList(context: Context, packageName: String?) {
        removeSpecificAppConfiguration(context = context, packageName = packageName)
    }

}
