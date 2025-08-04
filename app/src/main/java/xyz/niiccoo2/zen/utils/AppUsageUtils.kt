package xyz.niiccoo2.zen.utils

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Process
import androidx.annotation.OptIn
import androidx.compose.runtime.remember
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi

/**
 * Returns a map of all app usage from the start time to the end time.
 *
 * @param context The application context.
 * @param start The start time in unix milliseconds.
 * @param end The end time in unix milliseconds.
 * @return A map of package names to their usage time in milliseconds.
 */
fun getAppUsage(context: Context, start: Long, end: Long): Map<String, Long> {
    val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    val usageEvents = usageStatsManager.queryEvents(start - (3 * 60 * 60 * 1000), end) // 3-hour buffer

    val usageMap = mutableMapOf<String, Long>()
    val lastResumedEvents = mutableMapOf<String, UsageEvents.Event>()

    //val event = UsageEvents.Event()
    while (usageEvents.hasNextEvent()) {
        val event = UsageEvents.Event()  // new object each iteration
        usageEvents.getNextEvent(event)
        val key = event.packageName + event.className

        when (event.eventType) {
            UsageEvents.Event.ACTIVITY_RESUMED -> lastResumedEvents[key] = event
            UsageEvents.Event.ACTIVITY_PAUSED,
            UsageEvents.Event.ACTIVITY_STOPPED -> {
                val resumedEvent = lastResumedEvents.remove(key)
                if (resumedEvent != null && event.timeStamp > start) {
                    val resumeTime = maxOf(resumedEvent.timeStamp, start)
                    val duration = event.timeStamp - resumeTime
                    usageMap[event.packageName] = usageMap.getOrDefault(event.packageName, 0L) + duration
                }
            }
        }
    }

    // Add ongoing sessions from last resumed events
    lastResumedEvents.values
        .groupBy { it.packageName }
        .forEach { (packageName, events) ->
            val mostRecent = events.maxByOrNull { it.timeStamp }!!
            usageMap[packageName] = usageMap.getOrDefault(packageName, 0L) + (end - mostRecent.timeStamp)
        }

    // Return usage time in seconds
    return usageMap.filterValues { it > 0 }
        .mapValues { it.value }
}

/**
 * Returns a map of all app usage since midnight.
 *
 * @param context The application context.
 * @return A map of package names to their usage time in milliseconds.
 */
fun getTodaysAppUsage(context: Context): Map<String, Long> {
    val start = getStartOfTodayMillis()
    val end = System.currentTimeMillis()
    return getAppUsage(context, start, end)
}

/**
 * Returns the total usage of a specific package in milliseconds.
 *
 * @param context The application context.
 * @param packageName The name of the package.
 * @return The total usage of the package in milliseconds.
 */
fun getSingleAppUsage(context: Context, packageName: String): Long {
    val appUsage = getTodaysAppUsage(context)
    return appUsage[packageName] ?: 0
}

/**
 * Checks if the app has the necessary permission to access usage statistics.
 *
 * @param context The application context.
 * @return True if the permission is granted, false otherwise.
 */
fun hasUsageStatsPermission(context: Context): Boolean {
    val appOpsManager = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    // If the permission is allowed mode == 0
    val packageName = context.packageName
    val mode = appOpsManager.checkOpNoThrow(
        AppOpsManager.OPSTR_GET_USAGE_STATS,
        Process.myUid(), // This gets the UID of your app's process
        packageName      // Use the dynamically obtained package name
    )
    return mode == AppOpsManager.MODE_ALLOWED
}

/**
 * Returns a packages name and icon.
 *
 * @param context The application context.
 * @param packageName The name of the package.
 * @return A pair of the package name and icon.
 */
@OptIn(UnstableApi::class)
fun getAppNameAndIcon(context: Context, packageName: String): Pair<String, Drawable>? {
    val pm = context.packageManager

    return try {
        // --- Code that might throw an exception goes in the 'try' block ---
        val app = pm.getApplicationInfo(packageName, 0) // This line can throw NameNotFoundException
        val name = pm.getApplicationLabel(app).toString()
        val icon = pm.getApplicationIcon(app)
        Pair(name, icon) // If all goes well, return the Pair
    } catch (e: PackageManager.NameNotFoundException) { // If package manager can't find the package
        Log.w("AppInfo", "Application package not found: $packageName. It might have been uninstalled.")
        null
    } catch (e: Exception) { // To catch anything else
        Log.e("AppInfo", "An unexpected error occurred while getting info for package: $packageName")
        null
    }
}