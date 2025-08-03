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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import xyz.niiccoo2.zen.services.PlaceholderAccessibilityService

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

@Composable
fun BlockScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner: LifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    // State to hold whether the service is enabled. This will drive your UI.
    var serviceEnabledState by remember {
        mutableStateOf(isAccessibilityServiceEnabled(context, PlaceholderAccessibilityService::class.java))
    }

    // Effect to observe lifecycle events (like ON_RESUME)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // When the app resumes, re-check the permission status
                serviceEnabledState = isAccessibilityServiceEnabled(context, PlaceholderAccessibilityService::class.java)
                //Log.d("BlockScreen", "App Resumed. Service enabled: $serviceEnabledState")
            }
        }

        // Add the observer to the lifecycle
        lifecycleOwner.lifecycle.addObserver(observer)

        // When the effect leaves the Composition, remove the observer
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
        if (serviceEnabledState) {
            Text(text = "Accessibility Service Enabled", fontSize = 24.sp)
            // You could potentially launch another effect here if you need to fetch data
            // *after* the service is confirmed enabled, similar to your example:
            // LaunchedEffect(Unit) { /* Fetch app usage stats if needed */ }
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally, // For Column content
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Accessibility Service Disabled",
                    fontSize = 24.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Button(onClick = {
                    openAccessibilityServiceSettings(context)
                }) {
                    Text(text = "Grant Accessibility Permission")
                }
            }
        }
    }
}