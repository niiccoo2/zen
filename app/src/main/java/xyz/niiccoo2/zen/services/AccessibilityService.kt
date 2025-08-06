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
import xyz.niiccoo2.zen.utils.AppSettings

class ZenAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "ZenAccessibilitySvc"
        const val ACTION_SHOW_BLOCK_OVERLAY = "xyz.niiccoo2.zen.action.SHOW_BLOCK_OVERLAY"
        const val EXTRA_PACKAGE_NAME = "xyz.niiccoo2.zen.extra.PACKAGE_NAME"
        const val EXTRA_APP_NAME = "xyz.niiccoo2.zen.extra.APP_NAME"
    }

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    @Volatile
    private var currentBlockedApps: Set<String> = emptySet()

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i(TAG, "Accessibility Service connected.")

        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 100
            // Consider setting packageNames explicitly if you know which apps you might block,
            // though for a general blocker, listening to all is common.
            // packageNames = null
        }
        serviceInfo = info

        observeBlockedApps()

        Log.i(TAG, "Accessibility Service configured and observing effectively blocked apps.")
    }

    private fun observeBlockedApps() {
        serviceScope.launch {
            // Use the new method from AppSettings
            AppSettings.getEffectivelyBlockedPackagesFlow(applicationContext).collectLatest { updatedEffectivelyBlockedPackages ->
                Log.d(TAG, "Effectively blocked packages updated: $updatedEffectivelyBlockedPackages")
                currentBlockedApps = updatedEffectivelyBlockedPackages
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) {
            Log.v(TAG, "Event was null, ignoring.")
            return
        }

        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString()
            // val className = event.className?.toString() // className might be useful for more granular blocking in the future

            if (packageName != null) {
                serviceScope.launch {
                    // The logic here remains the same as currentBlockedApps is still Set<String>
                    //if (packageName in currentBlockedApps) {

                    Log.d(TAG, "Window state changed - Pkg: $packageName") // Kept for debugging if needed
                    val appSettings = AppSettings.getSpecificAppSetting(applicationContext, packageName)
                    val isAppCurrentlyOnBreak = appSettings?.isOnBreak == true // true if configured and on break, false otherwise (including not configured)

                    if (!isAppCurrentlyOnBreak) {
                        Log.i(TAG, "Effectively blocked app launched: $packageName")

                        var appName = packageName // Default to package name
                        try {
                            val pm = packageManager
                            val applicationInfo = pm.getApplicationInfo(packageName, 0)
                            appName = pm.getApplicationLabel(applicationInfo).toString()
                        } catch (e: PackageManager.NameNotFoundException) {
                            Log.w(
                                TAG,
                                "Could not get app name for $packageName. Using package name.",
                                e
                            )
                        }

                        Log.i(
                            TAG,
                            "$appName ($packageName) was launched and is effectively blocked!"
                        )

                        val overlayTriggerIntent = Intent(ACTION_SHOW_BLOCK_OVERLAY).apply {
                            putExtra(EXTRA_PACKAGE_NAME, packageName)
                            putExtra(EXTRA_APP_NAME, appName)
                            // Explicitly set the target package for the broadcast receiver
                            // to ensure it's handled by your app's receiver.
                            setPackage(this@ZenAccessibilityService.packageName)
                        }
                        sendBroadcast(overlayTriggerIntent)
                        Log.d(
                            TAG,
                            "Broadcast sent for $appName to show overlay: $ACTION_SHOW_BLOCK_OVERLAY"
                        )
                    }
                }
            }
        }
    }

    override fun onInterrupt() {
        Log.w(TAG, "Accessibility Service interrupted.")
        // Consider if any state needs to be cleaned up or if re-observation is needed upon reconnection.
        // onServiceConnected() will be called again if the service is restarted.
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel() // Cancel all coroutines started in serviceScope
        Log.i(TAG, "Accessibility Service destroyed.")
    }
}
