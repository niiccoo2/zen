package xyz.niiccoo2.zen.utils

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Process
import androidx.annotation.OptIn
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import xyz.niiccoo2.zen.ui.screens.statStartTimeFlow

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
    val usageEvents = usageStatsManager.queryEvents(start - (3 * 60 * 60 * 1000), end)

    val usageMap = mutableMapOf<String, Long>()
    val lastResumedEvents = mutableMapOf<String, UsageEvents.Event>()

    while (usageEvents.hasNextEvent()) {
        val event = UsageEvents.Event()
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
    lastResumedEvents.values
        .groupBy { it.packageName }
        .forEach { (packageName, events) ->
            val mostRecent = events.maxByOrNull { it.timeStamp }!!
            usageMap[packageName] = usageMap.getOrDefault(packageName, 0L) + (end - mostRecent.timeStamp)
        }

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
    var startTime = "00:00"
    CoroutineScope(Dispatchers.IO).launch {
        context.statStartTimeFlow.collect { value ->
            startTime = value
        }
    }
    val start = getStartOfTodayMillis(startTime)
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

    val packageName = context.packageName
    val mode = appOpsManager.checkOpNoThrow(
        AppOpsManager.OPSTR_GET_USAGE_STATS,
        Process.myUid(),
        packageName
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
fun getAppNameAndIcon(context: Context, packageName: String?): Pair<String, Drawable>? {
    val pm = context.packageManager
    if (packageName == null) {
        return null
    }
    return try {

        val app = pm.getApplicationInfo(packageName, 0)
        val name = pm.getApplicationLabel(app).toString()
        val icon = pm.getApplicationIcon(app)
        Pair(name, icon)
    } catch (_: PackageManager.NameNotFoundException) {
        Log.w(
            "AppInfo",
            "Application package not found: $packageName. It might have been uninstalled."
        )
        null
    } catch (_: Exception) {
        Log.e(
            "AppInfo",
            "An unexpected error occurred while getting info for package: $packageName"
        )
        null
    }
}

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

    val events = usageStatsManager.queryEvents(time - (1000 * 60000), time)
    val event = UsageEvents.Event()

    var lastActivityResumedTime: Long = 0

    while (events.hasNextEvent()) {
        events.getNextEvent(event)

        if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
            if (event.timeStamp > lastActivityResumedTime) {
                foregroundApp = event.packageName
                lastActivityResumedTime = event.timeStamp
            }
        }
    }

    if (foregroundApp == null) {
        android.util.Log.w("ForegroundCheck", "Could not determine foreground app via UsageEvents query (ACTIVITY_RESUMED).")
    } else {
        android.util.Log.d("ForegroundCheck", "Determined foreground app via UsageEvents (ACTIVITY_RESUMED): $foregroundApp")
    }
    return foregroundApp
}

