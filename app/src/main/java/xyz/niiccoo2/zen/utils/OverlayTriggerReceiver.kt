package xyz.niiccoo2.zen.utils // Or the correct package you decided on (e.g., xyz.niiccoo2.zen.receivers)

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import xyz.niiccoo2.zen.activities.OverlayActivity // Make sure this path is correct!

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

        // Check if this is the broadcast action for your blocking screen
        // Make sure this action string matches what your AccessibilityService sends
        if (intent.action == "xyz.niiccoo2.zen.action.SHOW_BLOCK_OVERLAY") { // Or "xyz.niiccoo2.zen.action.SHOW_YOUTUBE_OVERLAY" if that's what you used
            Log.i(TAG, "Target app launch detected via broadcast! Launching OverlayActivity.")

            // --- START THE OverlayActivity ---
            val activityIntent = Intent(context, OverlayActivity::class.java)

            // CRITICAL: When starting an Activity from a context that is not an Activity
            // (like a BroadcastReceiver or a Service), you MUST add FLAG_ACTIVITY_NEW_TASK.
            activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            // Optional: You can pass data to your OverlayActivity if needed
            // For example, if your AccessibilityService detected which app was launched
            // and you want to display its name on the blocking screen.
            // val blockedAppName = intent.getStringExtra("BLOCKED_APP_NAME") // Assuming AccessibilityService added this
            // if (blockedAppName != null) {
            //     activityIntent.putExtra("APP_NAME_TO_DISPLAY", blockedAppName)
            // }

            context.startActivity(activityIntent)
            Log.d(TAG, "OverlayActivity started.")

        } else {
            Log.d(TAG, "Received broadcast with unhandled action: ${intent.action}")
        }
    }
}
