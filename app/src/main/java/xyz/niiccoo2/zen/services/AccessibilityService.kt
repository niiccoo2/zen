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
        // Start observing the flow
        observeEffectivelyBlockedApps()

        Log.i(TAG, "Accessibility Service configured and observing effectively blocked apps.")
    }

    private fun observeEffectivelyBlockedApps() {
        serviceScope.launch {
            AppSettings.getEffectivelyBlockedPackagesFlow(applicationContext).collectLatest { updatedEffectivelyBlockedPackages ->
                Log.d(TAG, "Effectively blocked packages updated: $updatedEffectivelyBlockedPackages")
                currentEffectivelyBlockedApps = updatedEffectivelyBlockedPackages
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.packageName == null) {
            return
        }

        val packageNameString = event.packageName.toString()

        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            Log.i(TAG, "Received accessibility event for $packageNameString")

            serviceScope.launch {
                val isBlockedNow = AppSettings.isAppEffectivelyBlockedNow(applicationContext, packageNameString)

                if (isBlockedNow) {
                    Log.i(TAG, "App $packageNameString is currently effectively blocked (checked on event). Showing overlay.")
                    showBlockOverlay(packageNameString)
                } else {
                    Log.i(TAG, "App $packageNameString is NOT currently effectively blocked (checked on event). Overlay NOT shown.")

                }
            }
        }
    }

    private fun showBlockOverlay(packageName: String) {
        var appName = packageName
        try {
            val pm = packageManager
            val applicationInfo = pm.getApplicationInfo(packageName, 0)
            appName = pm.getApplicationLabel(applicationInfo).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            Log.w(TAG, "Could not get app name for $packageName. Using package name.", e)
        }

        val overlayTriggerIntent = Intent(ACTION_SHOW_BLOCK_OVERLAY).apply {
            putExtra(EXTRA_PACKAGE_NAME, packageName)
            putExtra(EXTRA_APP_NAME, appName)
            setPackage(this@ZenAccessibilityService.packageName)
        }
        sendBroadcast(overlayTriggerIntent)
        Log.d(TAG, "Broadcast sent for $appName ($packageName) to show overlay: $ACTION_SHOW_BLOCK_OVERLAY")
    }

    override fun onInterrupt() {
        Log.w(TAG, "Accessibility Service interrupted.")

    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel() // Cancel all coroutines started in serviceScope
        Log.i(TAG, "Accessibility Service destroyed.")
    }
}
