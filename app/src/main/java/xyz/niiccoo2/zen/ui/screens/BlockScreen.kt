package xyz.niiccoo2.zen.ui.screens

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Activity
import android.app.AlarmManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.provider.Settings
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
import kotlinx.coroutines.launch
import xyz.niiccoo2.zen.Destination
import xyz.niiccoo2.zen.services.ZenAccessibilityService
import xyz.niiccoo2.zen.utils.AppSettings
import xyz.niiccoo2.zen.utils.BlockedAppSettings
import xyz.niiccoo2.zen.utils.getAppNameAndIcon
import xyz.niiccoo2.zen.utils.getSingleAppUsage
import xyz.niiccoo2.zen.utils.millisToNormalTime
import androidx.compose.runtime.State
import xyz.niiccoo2.zen.utils.AppSettings.removeAppFromBlockList

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
    // Explicitly typing appDetails for clarity, though compiler might infer Pair<String, Drawable?>
    val appDetails: Pair<String, Drawable?>? by remember(blockedPackage) {
        mutableStateOf(getAppNameAndIcon(context = context, packageName = blockedPackage))
    }
    val coroutineScope = rememberCoroutineScope()
    val fabSize = 40.dp

    Card(
        modifier = Modifier
            // .fillMaxWidth() // fillMaxWidth is good on the Row, not always needed on Card if Row does it
            .padding(bottom = 8.dp), // A bit less padding than 16dp if items are dense
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp), // Adjust padding as needed
            verticalAlignment = Alignment.CenterVertically
        ) {
            val currentAppDetails = appDetails // Use a stable val for the if check to help smart cast

            if (currentAppDetails?.second != null) { // Check if icon drawable is also not null
                val (name, icon) = currentAppDetails // Smart cast is enough
                AsyncImage(
                    model = icon, // Known to be non-null here
                    contentDescription = "$name icon",
                    modifier = Modifier.size(48.dp)
                )
                Spacer(Modifier.width(12.dp)) // Slightly more space
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    val totalTime by remember(blockedPackage) { // Key by what makes it unique
                        val usage = getSingleAppUsage(context = context, packageName = blockedPackage)
                        mutableStateOf(millisToNormalTime(usage, true))
                    }
                    Text(text = "Time used: $totalTime", style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            } else {
                // Placeholder if app details or icon are not available
                Column(
                    modifier = Modifier.weight(1f) // Ensure this column also takes weight
                ) {
                    Text(
                        text = currentAppDetails?.first ?: blockedPackage, // Show name if available, else package
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "App info not fully available.",
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(Modifier.width(8.dp)) // Space before FAB

            SmallFloatingActionButton(
                onClick = {
                    coroutineScope.launch {
                        removeAppFromBlockList(context, blockedPackage)
                    }
                },
                modifier = Modifier.size(fabSize) // Correct modifier usage
            ) {
                Icon(Icons.Outlined.Delete, "Remove $blockedPackage from block list")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class) // Added ExperimentalAnimationApi
@Composable
fun BlockScreen(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current

    val blockedAppSettingsMapState: State<Map<String, BlockedAppSettings>> =
        AppSettings.getBlockedAppSettingsMap(context).collectAsState(initial = emptyMap())

    // Access the map value (this will trigger recomposition when the map changes)
    val currentBlockedAppSettings: Map<String, BlockedAppSettings> = blockedAppSettingsMapState.value

    // If you only need the package names (keys) from this map for some specific UI part:
    val packageNamesInConfiguration: Set<String> = currentBlockedAppSettings.keys


    val activity = context as? Activity

    val skipAccessibilityPermission = false // Lets you skip the accessibility permission prompt for testing TODO: Make sure this is off false before shipping

    var accessibilityServiceEnabled by remember(context) {
        mutableStateOf(isAccessibilityServiceEnabled(context, ZenAccessibilityService::class.java))
    }
    var overlayPermissionGranted by remember(context) {
        mutableStateOf(canDrawOverlays(context))
    }

    var canScheduleAlarms by remember(context) {
        mutableStateOf(
            // The result of the whole expression is passed to mutableStateOf
            (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).canScheduleExactAlarms()
        )
    }


    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
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
        (skipAccessibilityPermission || accessibilityServiceEnabled) && // Handles the accessibility part
                overlayPermissionGranted &&
                canScheduleAlarms

    Box(modifier = modifier.fillMaxSize()) {
        if (allPermissionsGranted) {
            // ^^^ It will let you skip the accessibility perms if skipAccessibilityPermission is true
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp) // Adjusted padding
            ) {
                Text( // This kinda looks bad
                    text = "Current Blocks:",
                    style = MaterialTheme.typography.headlineSmall, // Using MaterialTheme typography
                    modifier = Modifier.padding(bottom = 12.dp, top = 8.dp)
                )
                if (packageNamesInConfiguration.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f), // Ensure it takes space if LazyColumn is not shown
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No apps are currently blocked.",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f) // LazyColumn takes available space
                    ) {
                        items(
                            items = packageNamesInConfiguration.toList(), // Important: provide a list
                            key = { packageName -> packageName } // Crucial for animations and item tracking
                        ) { packageName ->
                            AnimatedVisibility(
                                visible = packageNamesInConfiguration.contains(packageName), // Controls visibility based on presence in the set
                                enter = fadeIn(animationSpec = tween(durationMillis = 200)) + expandVertically(animationSpec = tween(durationMillis = 300)),
                                exit = fadeOut(animationSpec = tween(durationMillis = 200)) + shrinkVertically(animationSpec = tween(durationMillis = 300))
                            ) {
                                // `key` modifier for AnimatedVisibility's direct child is helpful if its content changes identity
                                // but for a stable child like BlockCard based on packageName, it's often not strictly needed
                                // if the LazyColumn key is doing its job.
                                BlockCard(blockedPackage = packageName)
                            }
                        }
                    }
                }
            }
        } else if (!accessibilityServiceEnabled and !skipAccessibilityPermission) {
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
                        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                        intent.data = "package:xyz.niiccoo2.zen".toUri()
                        activity?.startActivity(intent)
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
