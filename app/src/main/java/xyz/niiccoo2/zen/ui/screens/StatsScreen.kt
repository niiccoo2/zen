package xyz.niiccoo2.zen.ui.screens

import android.app.AppOpsManager
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar
import java.util.Locale


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
    val calendar = Calendar.getInstance() // Gets a Calendar instance in the current default timezone and locale

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
                val timeOnApp = timeForApp(context, "com.spotify.music")

                val formattedTime = String.format(Locale.getDefault(), "%.2f", timeOnApp/60000.0)
                // ^^^ This converts the time to minutes and formats the time to two decimal places
                Text(text = "Time (mins): $formattedTime", fontSize = 24.sp)
                Text(text = "Time (milli): $timeOnApp", fontSize = 24.sp)
                Text(text = "Current Time (milli): ${System.currentTimeMillis()}", fontSize = 24.sp)
                Text(text = "Start of today (milli): ${getStartOfTodayMillis()}", fontSize = 24.sp)


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

