package xyz.niiccoo2.zen.services // Or your appropriate services package

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import xyz.niiccoo2.zen.R
import xyz.niiccoo2.zen.activities.MainActivity
import xyz.niiccoo2.zen.utils.AppSettings
import xyz.niiccoo2.zen.utils.getForegroundAppPackageName
import xyz.niiccoo2.zen.utils.hasUsageStatsPermission

class ForegroundAppCheckerService : Service() {

    companion object {
        private const val TAG = "FgAppCheckerSvc"
        private const val NOTIFICATION_CHANNEL_ID = "ForegroundAppCheckerChannel"
        private const val NOTIFICATION_ID = 1337 // Choose a unique ID
        private const val CHECK_INTERVAL_MS = 60_000L // 1 minute

        // Actions for block overlay
        const val ACTION_SHOW_BLOCK_OVERLAY = "xyz.niiccoo2.zen.action.SHOW_BLOCK_OVERLAY"
        const val EXTRA_PACKAGE_NAME = "xyz.niiccoo2.zen.extra.PACKAGE_NAME"
        const val EXTRA_APP_NAME = "xyz.niiccoo2.zen.extra.APP_NAME" // Added this constant

    }

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var periodicCheckRunnable: Runnable

    private var isScreenOn = true
    private var currentlyBlockedAppByThisService: String? = null

    private val screenStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_ON -> {
                    Log.d(TAG, "Screen ON")
                    isScreenOn = true
                    startPeriodicChecks()
                }
                Intent.ACTION_SCREEN_OFF -> {
                    Log.d(TAG, "Screen OFF")
                    isScreenOn = false
                    stopPeriodicChecks()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate")
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        registerReceiver(screenStateReceiver, filter)

        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        isScreenOn =
            powerManager.isInteractive

        setupPeriodicCheckRunnable()
        if (isScreenOn) {
            startPeriodicChecks()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand received")

        if (isScreenOn) {
            startPeriodicChecks()
        }
        return START_STICKY
    }

    private fun setupPeriodicCheckRunnable() {
        periodicCheckRunnable = Runnable {
            if (!isScreenOn) {
                Log.d(TAG, "Screen is OFF, skipping check.")
                return@Runnable
            }

            serviceScope.launch {
                if (!hasUsageStatsPermission(applicationContext)) {
                    Log.w(TAG, "Usage stats permission not granted. Skipping foreground app check.")
                    if (isScreenOn) handler.postDelayed(periodicCheckRunnable, CHECK_INTERVAL_MS)
                    return@launch
                }

                val foregroundAppPackageName = getForegroundAppPackageName(applicationContext)

                if (foregroundAppPackageName.isNullOrEmpty() || foregroundAppPackageName == packageName) {
                    if (currentlyBlockedAppByThisService != null) {
                        Log.d(TAG, "Foreground app is self, empty, or changed. Clearing our block state for ${currentlyBlockedAppByThisService}.")

                        currentlyBlockedAppByThisService = null
                    }
                    if (isScreenOn) handler.postDelayed(periodicCheckRunnable, CHECK_INTERVAL_MS)
                    return@launch
                }

                Log.d(TAG, "Periodic Check: Current foreground app: $foregroundAppPackageName")
                val shouldBeBlocked = AppSettings.isAppEffectivelyBlockedNow(applicationContext, foregroundAppPackageName)

                if (shouldBeBlocked) {
                    if (currentlyBlockedAppByThisService != foregroundAppPackageName) {
                        Log.i(TAG, "App $foregroundAppPackageName should be blocked. Sending show overlay broadcast.")
                        sendShowBlockOverlayBroadcast(foregroundAppPackageName)
                        currentlyBlockedAppByThisService = foregroundAppPackageName
                    } else {
                        Log.d(TAG, "App $foregroundAppPackageName already marked as blocked by this service.")
                    }
                } else {
                    if (currentlyBlockedAppByThisService == foregroundAppPackageName) {
                        Log.i(TAG, "App $foregroundAppPackageName should NO LONGER be blocked by schedule. Clearing our state.")
                        currentlyBlockedAppByThisService = null
                    }
                }
                if (isScreenOn) {
                    handler.postDelayed(periodicCheckRunnable, CHECK_INTERVAL_MS)
                }
            }
        }
    }

    private fun startPeriodicChecks() {
        Log.d(TAG, "Starting periodic checks.")
        handler.removeCallbacks(periodicCheckRunnable)
        handler.post(periodicCheckRunnable)
    }

    private fun stopPeriodicChecks() {
        Log.d(TAG, "Stopping periodic checks.")
        handler.removeCallbacks(periodicCheckRunnable)
    }


    private fun sendShowBlockOverlayBroadcast(packageNameStr: String) {
        var appName = packageNameStr // Default to package name if app name retrieval fails
        try {
            val pm = packageManager
            val applicationInfo = pm.getApplicationInfo(packageNameStr, 0)
            appName = pm.getApplicationLabel(applicationInfo).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            Log.w(TAG, "Could not get app name for $packageNameStr. Using package name.", e)
        }

        val intent = Intent(ACTION_SHOW_BLOCK_OVERLAY).apply {
            putExtra(EXTRA_PACKAGE_NAME, packageNameStr)
            putExtra(EXTRA_APP_NAME, appName)
            setPackage(this@ForegroundAppCheckerService.packageName)
        }
        sendBroadcast(intent)
        Log.d(TAG, "Broadcast sent for $appName ($packageNameStr) to show overlay: $ACTION_SHOW_BLOCK_OVERLAY")
    }

    private fun createNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntentFlags =
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, pendingIntentFlags)

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name) + " Checker Active")
            .setContentText("Monitoring apps for scheduled blocks.")
            .setSmallIcon(R.drawable.outline_lock_24_white)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        val serviceChannel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "Zen App Checker", // User visible name
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Monitors app usage for Zen scheduling features."
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(serviceChannel)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy")
        try {
            unregisterReceiver(screenStateReceiver)
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "Screen state receiver was not registered or already unregistered.", e)
        }
        stopPeriodicChecks()
        serviceJob.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        Log.d(TAG, "Foreground service stopped.")
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
