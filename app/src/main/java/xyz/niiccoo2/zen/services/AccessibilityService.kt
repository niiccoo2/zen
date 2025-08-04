package xyz.niiccoo2.zen.services

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo // Add this import
import android.content.Intent // Add this import
import android.content.pm.PackageManager
import android.view.accessibility.AccessibilityEvent
import android.util.Log
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

    // Coroutine scope for the service
    private val serviceJob = Job()
    private val serviceScope =
        CoroutineScope(Dispatchers.Main + serviceJob) // Use Dispatchers.Main or IO

    // Hold the current set of blocked apps
    @Volatile // Ensure visibility across threads, though updates are on Main
    private var currentBlockedApps: Set<String> = emptySet()

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i(TAG, "Accessibility Service connected.")

        // Configure service info (as you had before)
        val serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 100
            // packageNames = null // Listen to all apps, or specify if needed
        }
        setServiceInfo(serviceInfo)

        // Start observing blocked apps from DataStore
        observeBlockedApps()

        Log.i(TAG, "Accessibility Service configured and observing blocked apps.")
    }

    private fun observeBlockedApps() {
        serviceScope.launch {
            // Use 'applicationContext' as the context for AppSettings
            AppSettings.getBlockedApps(applicationContext).collectLatest { updatedBlockedApps ->
                Log.d(TAG, "Blocked apps updated: $updatedBlockedApps")
                currentBlockedApps = updatedBlockedApps
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
            val className = event.className?.toString()

            Log.d(TAG, "Window state changed - Pkg: $packageName, Class: $className")

            if (packageName != null) {
                // *** THIS IS THE CORRECTED CHECK ***
                if (packageName in currentBlockedApps) {
                    Log.i(TAG, "Blocked app launched: $packageName")

                    var appName = packageName
                    try {
                        val pm = packageManager
                        val applicationInfo = pm.getApplicationInfo(packageName, 0)
                        appName = pm.getApplicationLabel(applicationInfo).toString()
                    } catch (e: PackageManager.NameNotFoundException) {
                        Log.e(TAG, "Could not get app name for $packageName", e)
                    }

                    Log.i(TAG, "$appName ($packageName) was launched and is blocked!")

                    val overlayTriggerIntent = Intent(ACTION_SHOW_BLOCK_OVERLAY).apply {
                        putExtra(EXTRA_PACKAGE_NAME, packageName)
                        putExtra(EXTRA_APP_NAME, appName)
                        setPackage(this@ZenAccessibilityService.packageName) // Explicitly set target package
                    }
                    sendBroadcast(overlayTriggerIntent)
                    Log.d(TAG, "Broadcast sent for $appName: $ACTION_SHOW_BLOCK_OVERLAY")
                } else {
                    // Log.i(TAG, "App launched (not blocked): $packageName") // Optional logging
                }
            } else {
                // Log.i(TAG, "Event with null package name.") // Optional
            }
        }
    }

    override fun onInterrupt() {
        Log.w(TAG, "Accessibility Service interrupted.")
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel() // Cancel coroutines when the service is destroyed
        Log.i(TAG, "Accessibility Service destroyed.")
    }
}