package xyz.niiccoo2.zen.ui.screens

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import xyz.niiccoo2.zen.services.ZenAccessibilityService

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
fun BlockScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current

    // State for Accessibility Service
    var accessibilityServiceEnabled by remember {
        mutableStateOf(isAccessibilityServiceEnabled(context, ZenAccessibilityService::class.java))
    }

    // State for Overlay Permission
    var overlayPermissionGranted by remember {
        mutableStateOf(canDrawOverlays(context))
    }

    // Effect to observe lifecycle events (like ON_RESUME)
    // This will now update BOTH states on resume.
    DisposableEffect(lifecycleOwner, context) { // Add context as a key if its instance might change
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // When the app resumes, re-check both permission statuses
                accessibilityServiceEnabled = isAccessibilityServiceEnabled(context, ZenAccessibilityService::class.java)
                overlayPermissionGranted = canDrawOverlays(context)
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        // Updated conditional logic
        if (accessibilityServiceEnabled && overlayPermissionGranted) {
            Text(text = "All Permissions Granted!", fontSize = 24.sp)
            // Main content when both permissions are available
        } else if (!accessibilityServiceEnabled) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Accessibility Service Disabled",
                    fontSize = 20.sp, // Adjusted for clarity
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Button(onClick = { openAccessibilityServiceSettings(context) }) {
                    Text(text = "Grant Accessibility Permission")
                }
            }
        } else { // Implies accessibility IS enabled, but overlay is NOT
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Overlay Permission Disabled",
                    fontSize = 20.sp, // Adjusted for clarity
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Button(onClick = { requestOverlayPermission(context) }) {
                    Text(text = "Grant Overlay Permission")
                }
            }
        }
    }
}