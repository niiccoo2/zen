package xyz.niiccoo2.zen.utils // Or the correct package you decided on (e.g., xyz.niiccoo2.zen.receivers)

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import xyz.niiccoo2.zen.ui.screens.OverlayActivity // Make sure this path is correct!
// Import the constants from your Accessibility Service
import xyz.niiccoo2.zen.services.ZenAccessibilityService

class OverlayTriggerReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "OverlayTriggerReceiver"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) {
            Log.w(TAG, "Received null context or intent, ignoring.")
            return
        }

        Log.d(TAG, "Broadcast received with action: ${intent.action}")

        // Check if this is the broadcast action from your AccessibilityService
        if (intent.action == ZenAccessibilityService.ACTION_SHOW_BLOCK_OVERLAY) {
            Log.i(TAG, "Block overlay action received from AccessibilityService.")

            // --- Retrieve the names sent by ZenAccessibilityService ---
            val packageName = intent.getStringExtra(ZenAccessibilityService.EXTRA_PACKAGE_NAME)
            val appName = intent.getStringExtra(ZenAccessibilityService.EXTRA_APP_NAME)

            if (packageName == null) {
                Log.w(TAG, "Package name was null in the received intent. Cannot start OverlayActivity properly.")
                return // Or handle this error appropriately
            }
            // appName can be null if the lookup failed in the service, but packageName is critical.

            Log.i(TAG, "Target app: $appName (Package: $packageName). Launching OverlayActivity.")

            // --- START THE OverlayActivity AND PASS THE NAMES ---
            val activityIntent = Intent(context, OverlayActivity::class.java)

            // Add the retrieved names as extras to the intent for OverlayActivity
            activityIntent.putExtra(ZenAccessibilityService.EXTRA_PACKAGE_NAME, packageName)
            activityIntent.putExtra(ZenAccessibilityService.EXTRA_APP_NAME, appName ?: packageName) // Pass appName, or packageName if appName is null

            // CRITICAL: When starting an Activity from a context that is not an Activity
            // (like a BroadcastReceiver or a Service), you MUST add FLAG_ACTIVITY_NEW_TASK.
            activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            // Optional: Add other flags if needed, e.g., to bring an existing task to front
            // activityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            // activityIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)

            context.startActivity(activityIntent)
            Log.d(TAG, "OverlayActivity started for $appName.")

        } else {
            Log.d(TAG, "Received broadcast with unhandled action: ${intent.action}")
        }
    }
}
