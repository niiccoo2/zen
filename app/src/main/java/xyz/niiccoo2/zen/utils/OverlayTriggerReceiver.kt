package xyz.niiccoo2.zen.utils

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

        if (intent.action == ZenAccessibilityService.ACTION_SHOW_BLOCK_OVERLAY) {
            Log.i(TAG, "Block overlay action received from AccessibilityService.")


            val packageName = intent.getStringExtra(ZenAccessibilityService.EXTRA_PACKAGE_NAME)
            val appName = intent.getStringExtra(ZenAccessibilityService.EXTRA_APP_NAME)

            if (packageName == null) {
                Log.w(TAG, "Package name was null in the received intent. Cannot start OverlayActivity properly.")
                return
            }

            Log.i(TAG, "Target app: $appName (Package: $packageName). Launching OverlayActivity.")

            val activityIntent = Intent(context, OverlayActivity::class.java)

            activityIntent.putExtra(ZenAccessibilityService.EXTRA_PACKAGE_NAME, packageName)
            activityIntent.putExtra(ZenAccessibilityService.EXTRA_APP_NAME, appName ?: packageName)

            activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            context.startActivity(activityIntent)
            Log.d(TAG, "OverlayActivity started for $appName.")

        } else {
            Log.d(TAG, "Received broadcast with unhandled action: ${intent.action}")
        }
    }
}
