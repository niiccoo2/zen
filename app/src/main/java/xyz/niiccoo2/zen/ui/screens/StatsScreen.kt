package xyz.niiccoo2.zen.ui.screens

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Process
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xyz.niiccoo2.zen.getDailyStats
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone


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

fun getStartOfTodayMillis(): Long {
    val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))


    // Set the time to the beginning of the day
    calendar.set(Calendar.HOUR_OF_DAY, 0) // 0 for midnight
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)

    return calendar.timeInMillis
}

/**
 * Calculates the time on the given app today.
 *
 * @param context The application context.
 * @param appPackageName The package name of the app you want to calculate the time for.
 * @return The time spent on the app today in milliseconds.
 */
fun timeForApp(context: Context, appPackageName: String): Long { // This makes it take a sting as input and return a long
    val startMillis = getStartOfTodayMillis()
    val endMillis = System.currentTimeMillis()

    val mUsageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager?
    val lUsageStatsMap = mUsageStatsManager?.queryAndAggregateUsageStats(startMillis, endMillis)
    val totalTimeUsageInMillis = lUsageStatsMap?.get(appPackageName)?.totalTimeInForeground
    return totalTimeUsageInMillis ?: 0
}

/**
 * Gives the map of time on all maps for the past day
 *
 * @param context The application context.
 * @return The map of time spent on each app today in milliseconds.
 */
fun timeForAllApps(context: Context): Map<String, UsageStats>? { // This makes it take a sting as input and return a long
    val startMillis = getStartOfTodayMillis()
    val endMillis = System.currentTimeMillis()

    val mUsageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager?
    val lUsageStatsMap = mUsageStatsManager?.queryAndAggregateUsageStats(startMillis, endMillis)
    return lUsageStatsMap ?: emptyMap() // If the map is null, return an empty map
}

fun getAccurateAppUsage(context: Context, start: Long, end: Long): Map<String, Long> {
    val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    val usageEvents = usageStatsManager.queryEvents(start - (3 * 60 * 60 * 1000), end) // 3-hour buffer

    val usageMap = mutableMapOf<String, Long>()
    val lastResumedEvents = mutableMapOf<String, UsageEvents.Event>()

    val event = UsageEvents.Event()
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


@Composable
fun StatsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    // State to trigger recomposition for permission check after returning from settings
    var permissionCheckTrigger by remember { mutableStateOf(false) }

    // Define the launcher AT THE TOP LEVEL of the Composable, unconditionally
    val usageAccessSettingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { _ -> // We don't typically use the activityResult directly for this permission type
        // This callback is invoked when the user returns from the settings screen.
        // We trigger a re-check of the permission status.
        Log.d("StatsScreen", "Returned from settings.")
        permissionCheckTrigger = !permissionCheckTrigger // Toggle the state to force recomposition
    }

    // This state will hold the current permission status and update the UI accordingly
    val isPermissionGranted by remember(permissionCheckTrigger) {
        mutableStateOf(hasUsageStatsPermission(context))
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isPermissionGranted) { // Use the state variable here
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(text = "Permission Granted", fontSize = 24.sp)
                //val timeOnApp = timeForApp(context, "com.snapchat.android")
                val timeOnAllApps = timeForAllApps(context)
                val timeOnAllAppsNew = getDailyStats(context)

                // Old time way
                var time = 0L // The L makes this a Long so that we can add more longs to it
                for (key in timeOnAllApps?.keys ?: emptyList()) {
                    val foregroundTime: Long? = timeOnAllApps?.get(key)?.totalTimeInForeground
                    time += foregroundTime ?: 0L // If foregroundTime is null, add 0L
                }
                val formattedTime = String.format(Locale.getDefault(), "%.2f", time/60000.0)

                // New time way
                var timeNew = 0L // The L makes this a Long so that we can add more longs to it
                for (keyNew in timeOnAllAppsNew) {
                    val foregroundTime: Long? = keyNew.totalTime
                    timeNew += foregroundTime ?: 0L // If foregroundTime is null, add 0L
                }
                val formattedTimeNew = String.format(Locale.getDefault(), "%.2f", timeNew/60000.0)

                val start = getStartOfTodayMillis()
                val end = System.currentTimeMillis()
                val accurateUsages = getAccurateAppUsage(context, start, end)

// Sum total time (ms)
                val totalTimeMs = accurateUsages.values.sum()
                val totalTimeMinutes = totalTimeMs / 60000.0

                Text(text = "Time (Old): $formattedTime", fontSize = 24.sp)
                Text(text = "Time (New): $formattedTimeNew", fontSize = 24.sp)
                Text(text = "Time (New): $totalTimeMinutes", fontSize = 24.sp)


            }
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Permission Denied",
                    fontSize = 24.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Button(onClick = {
                    try {
                        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                        usageAccessSettingsLauncher.launch(intent) // Now it's in scope
                    } catch (e: Exception) {
                        Log.e(
                            "StatsScreen",
                            "Could not open ACTION_USAGE_ACCESS_SETTINGS",
                            e
                        )
                        // Fallback
                        try {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            intent.data = Uri.fromParts("package", context.packageName, null)
                            usageAccessSettingsLauncher.launch(intent) // Also in scope
                        } catch (e2: Exception) {
                            Log.e(
                                "StatsScreen",
                                "Could not open ACTION_APPLICATION_DETAILS_SETTINGS",
                                e2
                            )
                        }
                    }
                }) {
                    Text(text = "Grant Usage Access Permission")
                }
            }
        }
    }
}

@Preview (showBackground = true)
@Composable
fun Preview(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = "Permission Granted", fontSize = 24.sp)
            val timeOnApp = 1576737 // Just set this to a number for the preview

            val formattedTime = String.format(Locale.getDefault(), "%.2f", timeOnApp/60000.0)
            // ^^^ This converts the time to minutes and formats the time to two decimal places
            Text(text = "Time (mins): $formattedTime", fontSize = 24.sp)
            Text(text = "Time (milli): $timeOnApp", fontSize = 24.sp)
            Text(text = "Current Time (milli): ${System.currentTimeMillis()}", fontSize = 24.sp)
            Text(text = "Start of today (milli): ${getStartOfTodayMillis()}", fontSize = 24.sp)
    }
}

}