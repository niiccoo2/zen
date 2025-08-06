package xyz.niiccoo2.zen.services

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import xyz.niiccoo2.zen.utils.AppSettings // Ensure this path is correct

class ZenAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "ZenAccessibilitySvc"
        const val ACTION_SHOW_BLOCK_OVERLAY = "xyz.niiccoo2.zen.action.SHOW_BLOCK_OVERLAY"
        const val EXTRA_PACKAGE_NAME = "xyz.niiccoo2.zen.extra.PACKAGE_NAME"
        const val EXTRA_APP_NAME = "xyz.niiccoo2.zen.extra.APP_NAME"
    }

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    // This set will now be populated by getEffectivelyBlockedPackagesFlow
    // It contains package names that are currently scheduled to be blocked AND are not on break.
    @Volatile
    private var currentEffectivelyBlockedApps: Set<String> = emptySet()

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i(TAG, "Accessibility Service connected.")

        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 100
            // packageNames = null // Listen to all apps
        }
        serviceInfo = info

        observeEffectivelyBlockedApps() // Start observing the flow

        Log.i(TAG, "Accessibility Service configured and observing effectively blocked apps.")
    }

    private fun observeEffectivelyBlockedApps() {
        serviceScope.launch {
            // Use the new method from AppSettings
            AppSettings.getEffectivelyBlockedPackagesFlow(applicationContext).collectLatest { updatedEffectivelyBlockedPackages ->
                Log.d(TAG, "Effectively blocked packages updated: $updatedEffectivelyBlockedPackages")
                currentEffectivelyBlockedApps = updatedEffectivelyBlockedPackages
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.packageName == null) { // More concise null check
            // Log.v(TAG, "Event or package name was null, ignoring.") // Optional: reduce verbosity
            return
        }

        val packageName = event.packageName.toString()

        // Only act on window state changes, as before
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            // The core logic is now much simpler:
            // If the app that just came to the foreground is in our set of
            // effectively blocked apps, then trigger the overlay.
            // The AppSettings.getEffectivelyBlockedPackagesFlow() has already done
            // the heavy lifting of checking schedules and break status.
            if (packageName in currentEffectivelyBlockedApps) {
                Log.i(TAG, "App $packageName is currently effectively blocked. Showing overlay.")

                var appName = packageName // Default to package name
                try {
                    val pm = packageManager
                    val applicationInfo = pm.getApplicationInfo(packageName, 0)
                    appName = pm.getApplicationLabel(applicationInfo).toString()
                } catch (e: PackageManager.NameNotFoundException) {
                    Log.w(TAG, "Could not get app name for $packageName. Using package name.", e)
                }

                val overlayTriggerIntent = Intent(ACTION_SHOW_BLOCK_OVERLAY).apply {
                    putExtra(EXTRA_PACKAGE_NAME, packageName)
                    putExtra(EXTRA_APP_NAME, appName) // Pass the resolved app name
                    // Important: Set the package for the broadcast to ensure it's handled by your app
                    setPackage(this@ZenAccessibilityService.packageName)
                }
                sendBroadcast(overlayTriggerIntent)
                Log.d(TAG, "Broadcast sent for $appName ($packageName) to show overlay: $ACTION_SHOW_BLOCK_OVERLAY")

            } else {
                // Log.v(TAG, "App $packageName is NOT currently effectively blocked. Overlay NOT shown.") // Optional verbose log
            }
        }
    }

    override fun onInterrupt() {
        Log.w(TAG, "Accessibility Service interrupted.")
        // serviceJob.cancel() // Consider if you want to cancel the job here or if onServiceConnected will handle restart
        // The serviceJob will be cancelled in onDestroy. If the service is restarted by the system,
        // onServiceConnected will be called again, and observeEffectivelyBlockedApps will restart.
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel() // Cancel all coroutines started in serviceScope
        Log.i(TAG, "Accessibility Service destroyed.")
    }
}
