// In xyz.niiccoo2.zen.utils/BlockedAppsUtils.kt (or as methods in a BlockedAppsManager class)
package xyz.niiccoo2.zen.utils

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.util.Log

// This function doesn't need to change much, as AppSettings.saveBlockedApps is already suspend
//suspend fun saveListOfBlockedApps(context: Context, newBlockedAppsSet: Set<String>) {
//    try {
//        AppSettings.saveBlockedApps(context, newBlockedAppsSet)
//        Log.i("BlockedAppsUtils", "Blocked apps saved successfully: $newBlockedAppsSet")
//    } catch (e: Exception) {
//        Log.e("BlockedAppsUtils", "Error saving blocked apps", e)
//    }
//}
//
//suspend fun addAppToBlockList(context: Context, packageName: String?) {
//    // 1. READ the current set using the Flow from AppSettings
//    // .firstOrNull() gets the first emission (current state) and then cancels the Flow collection.
//    // It's a suspend function.
//    val currentApps = AppSettings.getBlockedApps(context).firstOrNull()?.toMutableSet() ?: mutableSetOf()
//
//    // 2. MODIFY the set
//    if (packageName != null && currentApps.add(packageName)) { // .add() returns true if the element was added
//        // 3. WRITE the modified set back
//        saveListOfBlockedApps(context, currentApps) // Use the utility function above
//        Log.i("BlockedAppsUtils", "$packageName added to block list.")
//    } else {
//        Log.i("BlockedAppsUtils", "$packageName is already in the block list.")
//    }
//}
//
//suspend fun removeAppFromBlockList(context: Context, packageName: String?) {
//    // 1. READ the current set
//    val currentApps = AppSettings.getBlockedApps(context).firstOrNull()?.toMutableSet() ?: mutableSetOf()
//
//    // 2. MODIFY the set
//    if (currentApps.remove(packageName)) { // .remove() returns true if the element was removed
//        // 3. WRITE the modified set back
//        saveListOfBlockedApps(context, currentApps)
//        Log.i("BlockedAppsUtils", "$packageName removed from block list.")
//    } else {
//        Log.i("BlockedAppsUtils", "$packageName was not found in the block list.")
//    }
//}

// These functions are for testing/demonstration.
// They need a CoroutineScope and Context to call the suspend functions.
//fun blockAppsExample(coroutineScope: CoroutineScope, context: Context) {
//    coroutineScope.launch {
//        Log.i("BlockedAppsUtils", "Blocking apps (example)")
//        val appsToBlock = setOf(
//            "com.discord",
//            "org.mozilla.firefox",
//            "com.google.android.youtube"
//        )
//        saveListOfBlockedApps(context, appsToBlock)
//    }
//}
//
//fun unblockAllAppsExample(coroutineScope: CoroutineScope, context: Context) {
//    coroutineScope.launch {
//        Log.i("BlockedAppsUtils", "Unblocking all apps (example)")
//        saveListOfBlockedApps(context, emptySet())
//    }
//}


/**
 * Gets the package name of the current foreground application using UsageStatsManager.
 *
 * @param context The application context.
 * @return The package name of the current foreground application, or null if not found.
 */
fun getForegroundAppPackageName(context: Context): String? {
    val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    var foregroundApp: String? = null
    val time = System.currentTimeMillis()
    // Query events in a reasonable recent window (e.g., last 1 minute or 30 seconds)
    val events = usageStatsManager.queryEvents(time - (1000 * 60000), time) // Query last 60 seconds
    val event = UsageEvents.Event()

    var lastActivityResumedTime: Long = 0

    while (events.hasNextEvent()) {
        events.getNextEvent(event)

        // Use the non-deprecated constant ACTIVITY_RESUMED
        if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
            // Check if this ACTIVITY_RESUMED event is more recent
            if (event.timeStamp > lastActivityResumedTime) {
                foregroundApp = event.packageName
                lastActivityResumedTime = event.timeStamp
            }
        }
        // You might also want to consider ACTIVITY_PAUSED if an app goes to background
        // but for "current foreground", ACTIVITY_RESUMED is the primary one.
    }

    if (foregroundApp == null) {
        Log.w("ForegroundCheck", "Could not determine foreground app via UsageEvents query (ACTIVITY_RESUMED).")
    } else {
        Log.d("ForegroundCheck", "Determined foreground app via UsageEvents (ACTIVITY_RESUMED): $foregroundApp")
    }
    return foregroundApp
}