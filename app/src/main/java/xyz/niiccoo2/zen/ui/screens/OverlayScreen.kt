package xyz.niiccoo2.zen.ui.screens // Or your preferred package

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
import androidx.compose.animation.core.copy
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
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

@Composable
fun AutosizeText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyLarge, // Default style
    maxLines: Int = 1, // Crucial for single-line auto-sizing
    targetFontSize: TextUnit = style.fontSize, // The ideal font size you want to achieve
    minFontSize: TextUnit = 8.sp // A reasonable minimum font size
) {
    var currentFontSize by remember(text, targetFontSize, minFontSize) { mutableStateOf(targetFontSize) }
    var readyToDraw by remember(text, targetFontSize, minFontSize) { mutableStateOf(false) }

    // If the text or target/min font size changes, reset the readyToDraw flag
    // and currentFontSize to allow recalculation.
    // This is implicitly handled by `remember` keys above if these parameters change.

    BasicText(
        text = text,
        modifier = modifier.then(
            Modifier.drawWithContent {
                if (readyToDraw) {
                    drawContent()
                }
            }
        ),
        style = style.copy(fontSize = currentFontSize),
        maxLines = maxLines,
        overflow = TextOverflow.Clip, // Clip if it still overflows at minFontSize
        onTextLayout = { textLayoutResult ->
            if (textLayoutResult.hasVisualOverflow && currentFontSize.value > minFontSize.value) {
                // Check if the text actually overflowed its container visually.
                // didOverflowHeight might be true if the text height exceeds the line height,
                // but hasVisualOverflow is more accurate for "did it fit in the available width for a single line".

                // Reduce font size. You can adjust the factor (0.9f) as needed.
                val newCalculatedSizeValue = currentFontSize.value * 0.9f

                // Ensure the new font size doesn't go below minFontSize
                currentFontSize = if (newCalculatedSizeValue > minFontSize.value) {
                    newCalculatedSizeValue.sp
                } else {
                    minFontSize
                }
                readyToDraw = false // Trigger re-layout
            } else {
                // Text fits or minFontSize has been reached
                readyToDraw = true
            }
        }
    )
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
            AutosizeText(
                text = "You've spent $appTime on $appName.",
                style = MaterialTheme.typography.bodyLarge.copy( // Or another base style
                    fontSize = 18.sp, // This becomes the initial targetFontSize if not overridden
                    color = MaterialTheme.colorScheme.onBackground
                ),
                targetFontSize = 18.sp, // Explicitly set the desired maximum size
                minFontSize = 10.sp,    // Set your desired minimum shrink size
                maxLines = 1,           // Ensure it tries to fit on one line
                modifier = Modifier.fillMaxWidth() // Give it the full width to calculate against
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
                AutosizeText(
                    text = "Unblock $appName for 5 minutes",
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = MaterialTheme.colorScheme.onPrimary
                    ),
                    targetFontSize = MaterialTheme.typography.labelLarge.fontSize, // e.g., 14.sp
                    minFontSize = 10.sp, // Your desired minimum
                    modifier = Modifier.padding(vertical = 4.dp)
                )
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
