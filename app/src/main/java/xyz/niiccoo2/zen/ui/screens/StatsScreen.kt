package xyz.niiccoo2.zen.ui.screens

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
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
import xyz.niiccoo2.zen.utils.getTodaysAppUsage
import xyz.niiccoo2.zen.utils.hasUsageStatsPermission
import xyz.niiccoo2.zen.utils.millisToHourAndMinute


data class Stat(
    val packageName: String,
    val totalTime: Long,
    val appName: String?,
    val appIconDrawable: android.graphics.drawable.Drawable?
)

@Composable
fun AppUsageRowVisual(stat: Stat, totalUsageMillis: Long) {

    val usagePercentage = if (totalUsageMillis > 0) {
        (stat.totalTime.toDouble() / totalUsageMillis.toDouble()) * 100
    } else {
        0.0
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
            if (stat.appIconDrawable != null) {
                Image(
                    painter = rememberAsyncImagePainter(model = stat.appIconDrawable),
                    contentDescription = "${stat.appName ?: stat.packageName} icon",
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
            } else {
                Spacer(modifier = Modifier.size(40.dp))
                Spacer(modifier = Modifier.width(12.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stat.appName ?: stat.packageName,
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
                    text = "$textMinute min",
                    fontSize = 16.sp
                )
            } else if (textHour != 0) {
                Text(
                    text = "$textHour h $textMinute min",
                    fontSize = 16.sp
                )
            } else {
                val textSecs = stat.totalTime / 1000
                Text(
                    text = "$textSecs secs",
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

    var isLoading by remember { mutableStateOf(false) }
    var appStatsListState by remember { mutableStateOf<List<Stat>>(emptyList()) }
    var totalTimeMsState by remember { mutableLongStateOf(0L) }


    LaunchedEffect(key1 = isPermissionGranted) {
        if (isPermissionGranted) {
            isLoading = true
            val (fetchedStats, fetchedTotalTime) = withContext(Dispatchers.IO) {
                val usageMap = getTodaysAppUsage(context)
                val pm = context.packageManager

                val stats = usageMap.map { (packageName, time) ->
                    var appName: String? = null
                    var appIcon: android.graphics.drawable.Drawable? = null
                    try {
                        val appInfoPm = pm.getApplicationInfo(packageName, 0)
                        appName = pm.getApplicationLabel(appInfoPm).toString()
                        appIcon = pm.getApplicationIcon(appInfoPm)
                    } catch (e: PackageManager.NameNotFoundException) {
                        Log.w("StatsScreen", "App not found: $packageName", e)
                    }
                    Stat(packageName, time, appName, appIcon)
                }.sortedByDescending { it.totalTime }

                val totalMs = stats.sumOf { it.totalTime }
                Pair(stats, totalMs)
            }

            appStatsListState = fetchedStats
            totalTimeMsState = fetchedTotalTime
            isLoading = false
        } else {
            appStatsListState = emptyList()
            totalTimeMsState = 0L
            isLoading = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        if (isPermissionGranted) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val (hour, minute) = millisToHourAndMinute(totalTimeMsState)

                if (totalTimeMsState > 0 || !isLoading) {
                    if (hour == 0) {
                        Text(text = "Total time: $minute min", fontSize = 24.sp, modifier = Modifier.padding(bottom = 16.dp))
                    } else {
                        Text(text = "Total time: $hour hr $minute min", fontSize = 24.sp, modifier = Modifier.padding(bottom = 16.dp))
                    }
                }

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
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Usage Access Permission Disabled",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Text(
                    text = "Zen needs usage access permissions to track app usage.",
                    style = MaterialTheme.typography.titleSmall,
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