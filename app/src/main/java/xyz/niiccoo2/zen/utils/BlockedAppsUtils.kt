// In xyz.niiccoo2.zen.utils/BlockedAppsUtils.kt (or as methods in a BlockedAppsManager class)
package xyz.niiccoo2.zen.utils

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope // For example functions, if not part of a class
import kotlinx.coroutines.flow.firstOrNull // Import this
import kotlinx.coroutines.launch          // For example functions

// This function doesn't need to change much, as AppSettings.saveBlockedApps is already suspend
suspend fun saveListOfBlockedApps(context: Context, newBlockedAppsSet: Set<String>) {
    try {
        AppSettings.saveBlockedApps(context, newBlockedAppsSet)
        Log.i("BlockedAppsUtils", "Blocked apps saved successfully: $newBlockedAppsSet")
    } catch (e: Exception) {
        Log.e("BlockedAppsUtils", "Error saving blocked apps", e)
    }
}

suspend fun addAppToBlockList(context: Context, packageName: String) {
    // 1. READ the current set using the Flow from AppSettings
    // .firstOrNull() gets the first emission (current state) and then cancels the Flow collection.
    // It's a suspend function.
    val currentApps = AppSettings.getBlockedApps(context).firstOrNull()?.toMutableSet() ?: mutableSetOf()

    // 2. MODIFY the set
    if (currentApps.add(packageName)) { // .add() returns true if the element was added
        // 3. WRITE the modified set back
        saveListOfBlockedApps(context, currentApps) // Use the utility function above
        Log.i("BlockedAppsUtils", "$packageName added to block list.")
    } else {
        Log.i("BlockedAppsUtils", "$packageName is already in the block list.")
    }
}

suspend fun removeAppFromBlockList(context: Context, packageName: String) {
    // 1. READ the current set
    val currentApps = AppSettings.getBlockedApps(context).firstOrNull()?.toMutableSet() ?: mutableSetOf()

    // 2. MODIFY the set
    if (currentApps.remove(packageName)) { // .remove() returns true if the element was removed
        // 3. WRITE the modified set back
        saveListOfBlockedApps(context, currentApps)
        Log.i("BlockedAppsUtils", "$packageName removed from block list.")
    } else {
        Log.i("BlockedAppsUtils", "$packageName was not found in the block list.")
    }
}

// These functions are for testing/demonstration.
// They need a CoroutineScope and Context to call the suspend functions.
fun blockAppsExample(coroutineScope: CoroutineScope, context: Context) {
    coroutineScope.launch {
        Log.i("BlockedAppsUtils", "Blocking apps (example)")
        val appsToBlock = setOf(
            "com.discord",
            "org.mozilla.firefox",
            "com.google.android.youtube"
        )
        saveListOfBlockedApps(context, appsToBlock)
    }
}

fun unblockAllAppsExample(coroutineScope: CoroutineScope, context: Context) {
    coroutineScope.launch {
        Log.i("BlockedAppsUtils", "Unblocking all apps (example)")
        saveListOfBlockedApps(context, emptySet())
    }
}

