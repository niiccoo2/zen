package xyz.niiccoo2.zen.ui.screens

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityManager
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
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import xyz.niiccoo2.zen.services.ZenAccessibilityService
import xyz.niiccoo2.zen.utils.AppSettings
import xyz.niiccoo2.zen.utils.getAppNameAndIcon
import xyz.niiccoo2.zen.utils.getSingleAppUsage
import xyz.niiccoo2.zen.utils.millisToNormalTime

fun openAccessibilityServiceSettings(context: Context) {
    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
    context.startActivity(intent)
}

fun isAccessibilityServiceEnabled(context: Context, serviceClass: Class<out AccessibilityService>): Boolean {
    val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
        ?: return false // Return false if AccessibilityManager is not available

    // Get a list of enabled accessibility services.
    // FEEDBACK_ALL_MASK includes all types of feedback.
    val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)

    val targetServiceInfo = ComponentName(context, serviceClass)

    for (enabledService in enabledServices) {
        //val serviceInfo = AccessibilityServiceInfo.CONTENTS_FILE_DESCRIPTOR
        val enabledServiceComponentName = ComponentName.unflattenFromString(enabledService.id)
        if (enabledServiceComponentName != null && enabledServiceComponentName == targetServiceInfo) {
            return true // The service is enabled
        }
    }
    return false // The service is not found among enabled services
}

/**
 * Checks if the app has the "Display over other apps" permission.
 *
 * @param context The application context.
 * @return True if the permission is granted, false otherwise.
 */
fun canDrawOverlays(context: Context): Boolean {
    return Settings.canDrawOverlays(context)
}

/**
 * Opens the system settings screen for the app to allow "Display over other apps" permission.
 *
 * @param context The application context or an Activity context.
 */
fun requestOverlayPermission(context: Context) {
    // Check if the context can start an activity. If it's the application context,
    // you need to add FLAG_ACTIVITY_NEW_TASK.
    val intent = Intent(
        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
        "package:${context.packageName}".toUri()
    )
    context.startActivity(intent)
}

@Composable
fun BlockCard(blockedPackage: String) {
    val context = LocalContext.current
    val appDetails by remember(blockedPackage) {
        mutableStateOf(getAppNameAndIcon(context = context, packageName = blockedPackage))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Check if appDetails is not null
            if (appDetails != null) {
                // Inside this block, appDetails is smart-cast to Pair<String, Drawable> (non-nullable)
                // So, destructuring is safe.
                val (name, icon) = appDetails!! // Using !! makes it explicit it's non-null here, though not strictly needed due to smart cast.

                AsyncImage(
                    model = icon,
                    contentDescription = "$name icon",
                    modifier = Modifier.size(48.dp)
                )
                Spacer(Modifier.width(8.dp)) // Good practice for spacing
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = name)
                    // Consider remembering this value too if getSingleAppUsage is expensive
                    val totalTime = millisToNormalTime(getSingleAppUsage(context = LocalContext.current, packageName = blockedPackage), true)
                    Text(text = "Total time: $totalTime")
                }
            } else {
                // Optional: What to display if appDetails is null (app info not found)
                // For example:
                Text("Information for $blockedPackage not available.", modifier = Modifier.padding(8.dp))
            }
        }
    }
}

@Composable
fun BlockScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope() // Get a CoroutineScope
    val blockedAppsSet: Set<String> by AppSettings.getBlockedApps(context)
        .collectAsState(initial = emptySet())

    // State for Accessibility Service
    var accessibilityServiceEnabled by remember {
        mutableStateOf(isAccessibilityServiceEnabled(context, ZenAccessibilityService::class.java))
    }

    // State for Overlay Permission
    var overlayPermissionGranted by remember {
        mutableStateOf(canDrawOverlays(context))
    }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                accessibilityServiceEnabled = isAccessibilityServiceEnabled(context, ZenAccessibilityService::class.java)
                overlayPermissionGranted = canDrawOverlays(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Function to save the list of blocked apps
    fun saveListOfBlockedApps(blockedAppsSet: Set<String>) {
        coroutineScope.launch { // Use the coroutineScope obtained from rememberCoroutineScope()
            try {
                AppSettings.saveBlockedApps(context, blockedAppsSet) // Use context from LocalContext.current
                Log.i("BlockScreen", "Blocked apps saved successfully: $blockedAppsSet")
                // You could show a Toast or update UI here (ensure UI updates are on the main thread if needed)
            } catch (e: Exception) {
                Log.e("BlockScreen", "Error saving blocked apps", e)
                // Handle error, show a message to the user, etc.
            }
        }
    }
    fun addAppToBlockList(packageName: String) {
        val currentApps = blockedAppsSet.toMutableSet()
        currentApps.add(packageName)
        saveListOfBlockedApps(currentApps)
    }

    fun removeAppFromBlockList(packageName: String) {
        val currentApps = blockedAppsSet.toMutableSet()
        currentApps.remove(packageName)
        saveListOfBlockedApps(currentApps)
    }

    fun blockApps() {
        Log.i("BlockScreen", "Blocking apps")
        val appsToBlock = setOf(
            "com.discord",
            "com.instagram.android",
            "com.google.android.youtube"
        )
        saveListOfBlockedApps(appsToBlock)
    }

    fun unblockApps() {
        Log.i("BlockScreen", "Unblocking apps")
        saveListOfBlockedApps(emptySet())
    }


    if (accessibilityServiceEnabled && overlayPermissionGranted) { // If all permissions are granted
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Column(horizontalAlignment = Alignment.Start) { // Align to the left or start of writing
                Text(text = "Current Blocks:", fontSize = 20.sp, modifier = modifier.padding(16.dp))
                if (blockedAppsSet.isEmpty()) {
                    Text("No apps are currently blocked.", style = MaterialTheme.typography.bodyMedium)
                } else {
                    LazyColumn (
                        modifier = Modifier.weight(1f)
                    ) {
                        items(
                            items = blockedAppsSet.toList(),
                            key = { packageName -> packageName }
                        ) { packageName ->
                            BlockCard(blockedPackage = packageName)
                        }
                    }
                }

            }
        }
    } else if (!accessibilityServiceEnabled) {
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
                Text(
                    text = "Accessibility Service Disabled",
                    fontSize = 20.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Button(onClick = { openAccessibilityServiceSettings(context) }) {
                    Text(text = "Grant Accessibility Permission")
                }
            }
        }
    } else { // Overlay permission not granted
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
                Text(
                    text = "Overlay Permission Disabled",
                    fontSize = 20.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Button(onClick = { requestOverlayPermission(context) }) {
                    Text(text = "Grant Overlay Permission")
                }
            }
        }
    }

}