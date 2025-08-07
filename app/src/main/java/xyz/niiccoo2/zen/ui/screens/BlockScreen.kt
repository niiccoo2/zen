package xyz.niiccoo2.zen.ui.screens

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Activity
import android.app.AlarmManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Build // Added for version checking
import android.provider.Settings
import android.util.Log // Added for logging
import android.view.accessibility.AccessibilityManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect // Added for side effects
import androidx.compose.runtime.State // Added explicit import
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import coil.compose.AsyncImage
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import xyz.niiccoo2.zen.Destination
import xyz.niiccoo2.zen.services.ForegroundAppCheckerService // Added import for your service
import xyz.niiccoo2.zen.services.ZenAccessibilityService
import xyz.niiccoo2.zen.utils.AppSettings
import xyz.niiccoo2.zen.utils.AppSettings.getSpecificAppSetting
import xyz.niiccoo2.zen.utils.AppSettings.removeAppFromBlockList
import xyz.niiccoo2.zen.utils.BlockedAppSettings
import xyz.niiccoo2.zen.utils.getAppNameAndIcon
import xyz.niiccoo2.zen.utils.getFormattedScheduledBlockTimes
import xyz.niiccoo2.zen.utils.getSingleAppUsage
import xyz.niiccoo2.zen.utils.millisToNormalTime
import java.time.LocalTime
import java.time.format.FormatStyle
import kotlin.text.format


fun openAccessibilityServiceSettings(context: Context) {
    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
    context.startActivity(intent)
}

fun isAccessibilityServiceEnabled(context: Context, serviceClass: Class<out AccessibilityService>): Boolean {
    val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
        ?: return false
    val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
    val targetServiceInfo = ComponentName(context, serviceClass)
    for (enabledService in enabledServices) {
        val enabledServiceComponentName = ComponentName.unflattenFromString(enabledService.id)
        if (enabledServiceComponentName != null && enabledServiceComponentName == targetServiceInfo) {
            return true
        }
    }
    return false
}

fun canDrawOverlays(context: Context): Boolean {
    return Settings.canDrawOverlays(context)
}

fun requestOverlayPermission(context: Context) {
    val intent = Intent(
        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
        "package:${context.packageName}".toUri()
    )
    context.startActivity(intent)
}

@Composable
fun BlockCard(blockedPackage: String) {
    val context = LocalContext.current
    var currentSettings by remember(blockedPackage) {
        mutableStateOf<BlockedAppSettings?>(null)
    }
    var isLoadingSettings by remember(blockedPackage) { mutableStateOf(true) } // For loading state

    LaunchedEffect(key1 = blockedPackage, key2 = context) {
        isLoadingSettings = true
        try {
            val settings = getSpecificAppSetting(context, blockedPackage)
            currentSettings = settings
            Log.d("BlockCard", "Settings for $blockedPackage: $settings")
        } catch (e: Exception) {
            Log.e("BlockCard", "Error fetching settings for $blockedPackage", e)
            currentSettings = null
        } finally {
            isLoadingSettings = false
        }
    }

    // This local helper function was inside BlockCard, ensure it's accessible
    // or use the one from your utils if you moved it there.
    // fun formatLocalTime(time: LocalTime, style: FormatStyle = FormatStyle.SHORT): String {
    //     return try {
    //         val formatter = DateTimeFormatter.ofLocalizedTime(style)
    //         time.format(formatter)
    //     } catch (e: Exception) {
    //         Log.w("TimeFormatUtil", "Error formatting time $time with style $style. Falling back.", e)
    //         time.toString()
    //     }
    // }
    // It's better if formatLocalTime is a top-level function in your utils package
    // and imported, rather than nested here. Assuming it is.

    val appDetails: Pair<String, Drawable?>? by remember(blockedPackage) {
        mutableStateOf(getAppNameAndIcon(context = context, packageName = blockedPackage))
    }
    val coroutineScope = rememberCoroutineScope()
    val fabSize = 40.dp

    Card(
        modifier = Modifier
            .padding(bottom = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val (appName, appIcon) = appDetails ?: Pair(blockedPackage, null)

            if (appIcon != null) {
                AsyncImage(
                    model = appIcon,
                    contentDescription = "$appName icon",
                    modifier = Modifier.size(48.dp)
                )
                Spacer(Modifier.width(12.dp))
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = appName, // Use the resolved appName
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Display total time used
                val totalTime by remember(blockedPackage, currentSettings) { // Also depends on currentSettings if that affects usage context
                    val usage = getSingleAppUsage(context = context, packageName = blockedPackage)
                    mutableStateOf(millisToNormalTime(usage, true))
                }
                Text(
                    text = "Time used: $totalTime",
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // --- Integration of formatted block times ---
                if (isLoadingSettings) {
                    Text(
                        text = "Loading schedule...",
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else if (currentSettings != null) {
                    val settings = currentSettings!! // Safe call due to null check
                    val formattedBlockTimes = getFormattedScheduledBlockTimes(settings, context)

                    if (formattedBlockTimes != null) {
                        Text(
                            text = formattedBlockTimes, // e.g., "Blocked: 10:00 AM - 5:00 PM"
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2, // Allow some wrapping if multiple schedules
                            overflow = TextOverflow.Ellipsis
                        )
                    } else if (settings.isEffectivelyAlwaysBlocked) {
                        Text(
                            text = "Always Blocked",
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else {
                        // This case covers when scheduledBlocks is empty (NO_SCHEDULE)
                        // and getFormattedScheduledBlockTimes returns null for it.
                        Text(
                            text = "No specific block schedule",
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else {
                    // This case is if currentSettings is null AFTER loading (e.g., error fetching)
                    Text(
                        text = "Schedule not available",
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                // --- End of integration ---

            }
            Spacer(Modifier.width(8.dp))
            SmallFloatingActionButton(
                onClick = {
                    coroutineScope.launch {
                        removeAppFromBlockList(context, blockedPackage)
                        // Optionally, you might want to trigger a refresh of the list
                        // or rely on your AppSettings.getBlockedAppSettingsMap(context).collectAsState
                        // to automatically update the parent BlockScreen.
                    }
                },
                modifier = Modifier.size(fabSize)
            ) {
                Icon(Icons.Outlined.Delete, "Remove $blockedPackage from block list")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun BlockScreen(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current

    val blockedAppSettingsMapState: State<Map<String, BlockedAppSettings>> =
        AppSettings.getBlockedAppSettingsMap(context).collectAsState(initial = emptyMap())
    val currentBlockedAppSettings: Map<String, BlockedAppSettings> = blockedAppSettingsMapState.value
    val packageNamesInConfiguration: Set<String> = currentBlockedAppSettings.keys

    val activity = context as? Activity

    val skipAccessibilityPermission = false // Make sure this is false before shipping

    var accessibilityServiceEnabled by remember(context) {
        mutableStateOf(isAccessibilityServiceEnabled(context, ZenAccessibilityService::class.java))
    }
    var overlayPermissionGranted by remember(context) {
        mutableStateOf(canDrawOverlays(context))
    }
    var canScheduleAlarms by remember(context) {
        mutableStateOf(
            (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).canScheduleExactAlarms()
        )
    }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                Log.d("BlockScreen", "ON_RESUME: Re-checking permissions.")
                accessibilityServiceEnabled = isAccessibilityServiceEnabled(context, ZenAccessibilityService::class.java)
                overlayPermissionGranted = canDrawOverlays(context)
                canScheduleAlarms = (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).canScheduleExactAlarms()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val allPermissionsGranted =
        (skipAccessibilityPermission || accessibilityServiceEnabled) &&
                overlayPermissionGranted &&
                canScheduleAlarms

    // LaunchedEffect to manage the ForegroundAppCheckerService based on allPermissionsGranted
    LaunchedEffect(key1 = allPermissionsGranted, key2 = context) {
        val serviceIntent = Intent(context, ForegroundAppCheckerService::class.java)
        if (allPermissionsGranted) {
            Log.d("BlockScreen", "All permissions granted. Attempting to start ForegroundAppCheckerService.")
            try {
                context.startForegroundService(serviceIntent)
                Log.i("BlockScreen", "ForegroundAppCheckerService start initiated.")
            } catch (e: Exception) {
                Log.e("BlockScreen", "Error starting ForegroundAppCheckerService: ${e.message}", e)
            }
        } else {
            Log.d("BlockScreen", "Not all permissions granted. Attempting to stop ForegroundAppCheckerService.")
            try {
                context.stopService(serviceIntent)
                Log.i("BlockScreen", "ForegroundAppCheckerService stop initiated due to missing permissions.")
            } catch (e: Exception) {
                Log.e("BlockScreen", "Error stopping ForegroundAppCheckerService: ${e.message}", e)
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (allPermissionsGranted) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Current Blocks:",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 12.dp, top = 8.dp)
                )
                if (packageNamesInConfiguration.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No apps are currently blocked.",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f)
                    ) {
                        items(
                            items = packageNamesInConfiguration.toList(),
                            key = { packageName -> packageName }
                        ) { packageName ->
                            AnimatedVisibility(
                                visible = packageNamesInConfiguration.contains(packageName),
                                enter = fadeIn(animationSpec = tween(durationMillis = 200)) + expandVertically(animationSpec = tween(durationMillis = 300)),
                                exit = fadeOut(animationSpec = tween(durationMillis = 200)) + shrinkVertically(animationSpec = tween(durationMillis = 300))
                            ) {
                                BlockCard(blockedPackage = packageName)
                            }
                        }
                    }
                }
            }
        } else if (!accessibilityServiceEnabled && !skipAccessibilityPermission) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Accessibility Service Disabled",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Text(
                        text = "Zen needs accessibility permissions to block apps.",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Button(onClick = { openAccessibilityServiceSettings(context) }) {
                        Text(text = "Grant Accessibility Permission")
                    }
                }
            }
        } else if (!canScheduleAlarms) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Alarm Permission Disabled",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Text(
                        text = "Zen needs alarm permissions to set timers for blocks.",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Button(onClick = {
                        try {
                            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                            intent.data = "package:xyz.niiccoo2.zen".toUri() // Ensure your package name is correct
                            activity?.startActivity(intent)
                        } catch (e: Exception) {
                            Log.e("BlockScreen", "Error opening SCHEDULE_EXACT_ALARM settings: ${e.message}", e)
                        }
                    }) {
                        Text(text = "Grant Alarm Permission")
                    }
                }
            }
        } else { // Overlay permission not granted
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Overlay Permission Disabled",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Text(
                        text = "Zen needs overlay permission to block apps.",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Button(onClick = { requestOverlayPermission(context) }) {
                        Text(text = "Grant Overlay Permission")
                    }
                }
            }
        }

        if (allPermissionsGranted) {
            FloatingActionButton(
                onClick = {
                    navController.navigate(Destination.NEW_BLOCK.route)
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(Icons.Filled.Add, "Add new app to block list")
            }
        }
    }
}
