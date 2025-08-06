package xyz.niiccoo2.zen.activities // Or your preferred package

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import xyz.niiccoo2.zen.services.ZenAccessibilityService
import xyz.niiccoo2.zen.ui.theme.ZenTheme
import xyz.niiccoo2.zen.utils.AlarmReceiver
import xyz.niiccoo2.zen.utils.AppSettings.setAppOnBreak
import xyz.niiccoo2.zen.utils.getSingleAppUsage
import xyz.niiccoo2.zen.utils.millisToNormalTime

class OverlayActivity : ComponentActivity() {
    companion object {
        const val EXTRA_PACKAGE_TO_BLOCK = "xyz.niiccoo2.zen.PACKAGE_TO_BLOCK"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val receivedPackageName = intent.getStringExtra(ZenAccessibilityService.EXTRA_PACKAGE_NAME)
        val receivedAppName = intent.getStringExtra(ZenAccessibilityService.EXTRA_APP_NAME)

        val alarmMgr = this.getSystemService(ALARM_SERVICE) as AlarmManager
//        var alarmIntent: PendingIntent = Intent(this, AlarmReceiver::class.java).let { intent ->
//            PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
//        }

        val onBackPressedCallback = object : OnBackPressedCallback(true) {

            // 'true' means this callback is enabled by default
            override fun handleOnBackPressed() {
                Log.d("OverlayActivity", "Back press intercepted by OnBackPressedCallback. Doing nothing.")
            }
        }
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)


        setContent {
            ZenTheme {
                BlockingScreenComposable (
                    appName = "$receivedAppName",
                    packageName = "$receivedPackageName",
                    onContinueToApp = {
                        // Create the Intent with extras and the PendingIntent HERE
                        val intentForAlarm = Intent(this@OverlayActivity, AlarmReceiver::class.java).apply {
                            putExtra(EXTRA_PACKAGE_TO_BLOCK, receivedPackageName)
                        }

                        val requestCode = receivedPackageName.hashCode() // Unique per package

                        val pendingAlarmIntent: PendingIntent = PendingIntent.getBroadcast(
                            this@OverlayActivity,
                            requestCode,
                            intentForAlarm,
                            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                        )

                        lifecycleScope.launch {
                            setAppOnBreak(context = this@OverlayActivity, packageName = receivedPackageName)
                        }

                        if (!alarmMgr.canScheduleExactAlarms()) {
                            Log.w("OverlayActivity", "Cannot schedule exact alarm, permission denied.")
                            Toast.makeText(this@OverlayActivity, "Exact alarm permission needed for re-blocking.", Toast.LENGTH_LONG).show()
                            // Consider guiding to settings or making it clear re-blocking might fail
                        } else {
                            alarmMgr.setExactAndAllowWhileIdle(
                                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                                SystemClock.elapsedRealtime() + 300 * 1000, // Wait 5 minutes
                                pendingAlarmIntent
                            )
                            Log.d("OverlayActivity", "Alarm set to re-block $receivedPackageName in 5 minute.")
                            Toast.makeText(this@OverlayActivity, "Unblocked for 5 mins.", Toast.LENGTH_SHORT).show()
                        }
                        finish()
                    },
                    onDoSomethingElse = {
                        val intent = Intent(Intent.ACTION_MAIN)
                        intent.addCategory(Intent.CATEGORY_HOME)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        startActivity(intent)
                        finish()
                    }
                )
            }
        }
    }

}

// --- BlockingScreenComposable remains the same ---
@Composable
fun BlockingScreenComposable(
    appName: String,
    packageName: String,
    onContinueToApp: () -> Unit,
    onDoSomethingElse: () -> Unit
) {
    val context = LocalContext.current
    val appTime = millisToNormalTime(getSingleAppUsage(context, packageName), true)


    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Hold On!",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "You've spent $appTime on $appName.",
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "What would you like to do?",
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = onContinueToApp,
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                Text("Unblock $appName for 5 minutes")
            }
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
                onClick = onDoSomethingElse,
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                Text("Do Something Else Instead")
            }
        }
    }
}

//@Preview(showBackground = true, device = "spec:shape=Normal,width=360,height=640,unit=dp,dpi=480")
//@Composable
//fun BlockingScreenPreview() {
//    ZenTheme {
//        BlockingScreenComposable(appName = "YouTube", onContinueToApp = {}, onDoSomethingElse = {})
//    }
//}
