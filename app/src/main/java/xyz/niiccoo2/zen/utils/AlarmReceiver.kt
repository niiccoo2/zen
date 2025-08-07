package xyz.niiccoo2.zen.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import xyz.niiccoo2.zen.ui.screens.OverlayActivity
import xyz.niiccoo2.zen.services.ZenAccessibilityService.Companion.ACTION_SHOW_BLOCK_OVERLAY
import xyz.niiccoo2.zen.services.ZenAccessibilityService.Companion.EXTRA_APP_NAME
import xyz.niiccoo2.zen.services.ZenAccessibilityService.Companion.EXTRA_PACKAGE_NAME
import xyz.niiccoo2.zen.utils.AppSettings.clearAppBreak

class AlarmReceiver : BroadcastReceiver() {

    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync() // Good practice for async work in receiver
        val packageName = intent.getStringExtra(OverlayActivity.EXTRA_PACKAGE_TO_BLOCK)
        Log.d("AlarmReceiver", "Alarm fired!")

        if (packageName != null && packageName.isNotEmpty()) {
            Log.d("AlarmReceiver", "Package to re-block: $packageName")

            coroutineScope.launch {
                try {
                    // 1. Add app back to the persistent block list
                    clearAppBreak(context, packageName)
                    Log.d("AlarmReceiver", "$packageName added back to block list.")

                    // 2. Check if this app is currently in the foreground
                    val foregroundApp = getForegroundAppPackageName(context)
                    Log.d("AlarmReceiver", "Current foreground app: $foregroundApp")

                    if (foregroundApp == packageName) {
                        Log.i("AlarmReceiver", "$packageName is currently in foreground. Triggering overlay directly.")
                        // Get app name (similar to how AccessibilityService does it, or pass it in alarm intent too)
                        var appName = packageName
                        try {
                            val pm = context.packageManager
                            val applicationInfo = pm.getApplicationInfo(packageName, 0)
                            appName = pm.getApplicationLabel(applicationInfo).toString()
                        } catch (e: Exception) {
                            Log.e("AlarmReceiver", "Could not get app name for $packageName", e)
                        }

                        // Send the same broadcast your AccessibilityService sends
                        val overlayTriggerIntent = Intent(ACTION_SHOW_BLOCK_OVERLAY).apply {
                            putExtra(EXTRA_PACKAGE_NAME, packageName)
                            putExtra(EXTRA_APP_NAME, appName)
                            setClass(context, OverlayTriggerReceiver::class.java)
                        }

                        context.sendBroadcast(overlayTriggerIntent)
                        Log.d("AlarmReceiver", "Broadcast sent for $packageName to show overlay immediately.")
                    }
                } catch (e: Exception) {
                    Log.e("AlarmReceiver", "Error in coroutine: ${e.message}", e)
                } finally {
                    pendingResult.finish()
                }
            }
        } else {
            Log.w("AlarmReceiver", "Package name not found in alarm intent.")
            pendingResult.finish()
        }
    }

}