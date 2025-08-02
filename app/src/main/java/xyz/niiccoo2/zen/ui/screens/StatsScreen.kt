package xyz.niiccoo2.zen.ui.screens

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Process
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime


data class Stat(
    val packageName: String,
    val totalTime: Long
    // Add any other fields AppUsageRowVisual might eventually need from a Stat object
)

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
 * Returns the time at midnight in the users local timezone.
 *
 * @return The time at midnight in the users local timezone.
 */
fun getStartOfTodayMillis(): Long {
    val todayLocalDate: LocalDate = LocalDate.now(ZoneId.systemDefault())

    val startOfTodayZonedDateTime: ZonedDateTime = todayLocalDate.atStartOfDay(ZoneId.systemDefault())

    return startOfTodayZonedDateTime.toInstant().toEpochMilli()
}

/**
 * Returns a pair of hours and minutes from a given number of milliseconds.
 *
 * @param millis The number of milliseconds.
 * @return A pair of hours and minutes.
 */
fun millisToHourAndMinute(millis: Long): Pair<Int, Int> {
    if (millis <= 0) {
        return Pair(0, 0)
    }
    val totalMinutes = (millis / 60000.0)
    val hours = totalMinutes.toInt() / 60
    val minutes = totalMinutes.toInt() % 60
    return Pair(hours, minutes)
}

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

@Composable
fun AppUsageRowVisual(stat: Stat, totalUsageMillis: Long) {
    val context = LocalContext.current
    val usagePercentage = if (totalUsageMillis > 0) {
        (stat.totalTime.toDouble() / totalUsageMillis.toDouble()) * 100
    } else {
        0.0
    }

    // Attempt to load app icon and name
    val appInfo = remember(stat.packageName) {
        try {
            val pm = context.packageManager
            val app = pm.getApplicationInfo(stat.packageName, 0)
            object {
                val name = pm.getApplicationLabel(app).toString()
                val icon = pm.getApplicationIcon(app)
            }
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (appInfo != null) {
                Image(
                    painter = rememberAsyncImagePainter(model = appInfo.icon),
                    contentDescription = "${appInfo.name} icon",
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
            } else {
                Spacer(modifier = Modifier.size(40.dp)) // Placeholder if icon fails
                Spacer(modifier = Modifier.width(12.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = appInfo?.name ?: stat.packageName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${"%.1f".format(usagePercentage)}%",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            val (textHour, textMinute) = millisToHourAndMinute(stat.totalTime)
            if (textHour == 0 && textMinute >= 1) {
                Text(
                    text = "${textMinute}min",
                    fontSize = 16.sp
                )
            } else if (textHour != 0) {
                Text(
                    text = "${textHour}h ${textMinute}min",
                    fontSize = 16.sp
                )
            } else {
                val textSecs = stat.totalTime / 1000
                Text(
                    text = "${textSecs}secs",
                    fontSize = 16.sp
                )
            }

        }
    }
}


@Composable
fun StatsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var permissionCheckTrigger by remember { mutableStateOf(false) }
    val usageAccessSettingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        Log.d("StatsScreen", "Returned from settings.")
        permissionCheckTrigger = !permissionCheckTrigger
    }
    val isPermissionGranted by remember(permissionCheckTrigger) {
        mutableStateOf(hasUsageStatsPermission(context))
    }

    // --- State variables for your data and loading status ---
    // These are declared once for the StatsScreen
    var isLoading by remember { mutableStateOf(false) }
    var appStatsListState by remember { mutableStateOf<List<Stat>>(emptyList()) }
    var totalTimeMsState by remember { mutableLongStateOf(0L) }
    // --- End of states ---

    // Use LaunchedEffect to load data when permission is granted (or permission status changes)
    // This will run when isPermissionGranted changes, or on initial composition if true.
    LaunchedEffect(key1 = isPermissionGranted) {
        if (isPermissionGranted) {
            isLoading = true // 1. Set loading to true

            // 2. Perform data fetching in a background coroutine
            val (fetchedStats, fetchedTotalTime) = withContext(Dispatchers.IO) {
                val start = getStartOfTodayMillis()
                val end = System.currentTimeMillis()
                val usageMap = getAppUsage(context, start, end)

                // Transform map to List<Stat>
                val stats = usageMap.map { (packageName, time) ->
                    Stat(packageName, time)
                }.sortedByDescending { it.totalTime } // Optional: sort by time

                val totalMs = stats.sumOf { it.totalTime }

                Pair(stats, totalMs) // Return both results
            }

            // 3. Update Compose states with the fetched data (back on the Main thread)
            appStatsListState = fetchedStats
            totalTimeMsState = fetchedTotalTime
            isLoading = false // 4. Set loading to false
        } else {
            // Optional: Clear data if permission is revoked
            appStatsListState = emptyList()
            totalTimeMsState = 0L
            isLoading = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        // Changed to TopCenter if you want the list to start from the top
        // contentAlignment = Alignment.Center
        contentAlignment = Alignment.TopCenter
    ) {
        if (isPermissionGranted) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                // Removed Arrangement.Center from Column if using TopCenter for Box
                // verticalArrangement = Arrangement.Center
            ) {
                // Display Total Time (now using the state variable)
                val (hour, minute) = millisToHourAndMinute(totalTimeMsState)

                // Only show total time if there is data, or always show it
                if (totalTimeMsState > 0 || !isLoading) { // Show if data or not loading
                    if (hour == 0 && minute == 0 && !isLoading && appStatsListState.isEmpty()) {
                        // Avoid showing "Time: 0min" if there's genuinely no data yet and not loading
                        // But if loading is finished and it's still 0, then show it.
                    } else if (hour == 0) {
                        Text(text = "Time: ${minute}min", fontSize = 24.sp, modifier = Modifier.padding(bottom = 16.dp))
                    } else {
                        Text(text = "Time: ${hour}hr ${minute}min", fontSize = 24.sp, modifier = Modifier.padding(bottom = 16.dp))
                    }
                }


                // Display List of Apps
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.padding(top = 20.dp))
                } else if (appStatsListState.isEmpty()) {
                    Text("No app usage data for today.", modifier = Modifier.padding(top = 20.dp))
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = appStatsListState,
                            key = { stat -> stat.packageName }
                        ) { statItem: Stat ->
                            AppUsageRowVisual(
                                stat = statItem,
                                totalUsageMillis = totalTimeMsState
                            )
                        }
                    }
                }
            }
        } else { // Permission Denied UI
            Column(
                modifier = Modifier.fillMaxSize(), // Ensure this column also fills to center its content
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
                        usageAccessSettingsLauncher.launch(intent)
                    } catch (e: Exception) {
                        Log.e("StatsScreen", "Could not open ACTION_USAGE_ACCESS_SETTINGS", e)
                        try {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            intent.data = Uri.fromParts("package", context.packageName, null)
                            usageAccessSettingsLauncher.launch(intent)
                        } catch (e2: Exception) {
                            Log.e("StatsScreen", "Could not open ACTION_APPLICATION_DETAILS_SETTINGS", e2)
                        }
                    }
                }) {
                    Text(text = "Grant Usage Access Permission")
                }
            }
        }
    }
}