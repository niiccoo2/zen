package xyz.niiccoo2.zen.services

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo // Add this import
import android.content.Intent // Add this import
import android.view.accessibility.AccessibilityEvent
import android.util.Log

class ZenAccessibilityService : AccessibilityService() {

    companion object {
        // Define a TAG for logging, makes it easier to filter in Logcat
        private const val TAG = "PlaceholderAccService"
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) {
            Log.v(TAG, "Event was null, ignoring.")
            return
        }

        // We are interested in events indicating a new window/activity has opened
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString()
            val className = event.className?.toString() // For more detailed logging if needed

            Log.d(TAG, "Window state changed - Pkg: $packageName, Class: $className")

            if (packageName != null) {
                // Check if the launched app is YouTube
                if (packageName == "com.discord") { // We will have to change this to the list of blocked apps eventually
                    Log.i(TAG, "Discord was launched!")

                    // --- THIS IS WHERE YOU'LL TRIGGER YOUR OVERLAY FUNCTION ---
                    // For now, let's prepare by sending a broadcast intent.
                    // Your app (e.g., an Activity or another BroadcastReceiver)
                    // will listen for this broadcast and then show the overlay.

                    val overlayTriggerIntent = Intent("xyz.niiccoo2.zen.action.SHOW_BLOCK_OVERLAY")
                    // You can add extras if needed, e.g.,
                    // overlayTriggerIntent.putExtra("target_app", packageName)
                    overlayTriggerIntent.setPackage(this.packageName) // Deliver only to your app
                    sendBroadcast(overlayTriggerIntent)
                    Log.d(TAG, "Broadcast sent: xyz.niiccoo2.zen.action.SHOW_BLOCK_OVERLAY")

                }
                // You can add more 'else if' blocks here to detect other apps
                // else if (packageName == "com.another.app") {
                //     Log.i(TAG, ">>> Another App was launched! <<<")
                //     // Handle other app launch
                // }
            } else {
                Log.i(TAG, "Opened $packageName")
            }
        } else {
            // Optional: Log other event types if you're curious, but filter them out for app launch detection
            // Log.v(TAG, "Ignoring event type: ${AccessibilityEvent.eventTypeToString(event.eventType)}")
        }
    }

    override fun onInterrupt() {
        Log.w(TAG, "Accessibility Service interrupted.")
        // Called when the system wants to interrupt the feedback your service is providing.
    }

    override fun onServiceConnected() {
        super.onServiceConnected() // Important to call super

        // This is a good place to configure your service if you're not doing it
        // entirely through the XML configuration file.
        // For detecting app launches, XML configuration is often sufficient.
        val serviceInfo = AccessibilityServiceInfo()
        serviceInfo.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED // Only listen to window state changes
        // serviceInfo.packageNames = arrayOf("com.google.android.youtube", "com.twitter.android") // Optional: Listen only to specific apps
        serviceInfo.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC // Generic feedback type
        serviceInfo.notificationTimeout = 100 // ms, time to process an event

        // IMPORTANT: For just detecting app launch by package name, you typically DON'T need canRetrieveWindowContent.
        // Setting it to true asks for more permissions and shows a stronger warning to the user.
        // serviceInfo.flags = serviceInfo.flags or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        // serviceInfo.flags = serviceInfo.flags or AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS // If you needed view IDs

        setServiceInfo(serviceInfo) // Apply the configuration

        Log.i(TAG, "Accessibility Service connected and configured.")
    }
}
