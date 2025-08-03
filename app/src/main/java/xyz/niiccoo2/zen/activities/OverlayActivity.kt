package xyz.niiccoo2.zen.activities // Or your preferred package

import android.os.Bundle
import android.util.Log
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xyz.niiccoo2.zen.ui.theme.ZenTheme

class OverlayActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // --- Handle Back Press using OnBackPressedDispatcher ---
        val onBackPressedCallback = object : OnBackPressedCallback(true) {
            // 'true' means this callback is enabled by default
            override fun handleOnBackPressed() {
                // This is where your custom back press logic goes.
                // For this blocking screen, we want to do nothing to prevent dismissal.
                Log.d("OverlayActivity", "Back press intercepted by OnBackPressedCallback. Doing nothing.")
                // If you wanted to allow back press sometimes, you could:
                // isEnabled = false // Disable this callback
                // requireActivity().onBackPressedDispatcher.onBackPressed() // Then invoke the default behavior or another callback
            }
        }
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
        // You can also add the callback with a LifecycleOwner to have it automatically
        // removed when the LifecycleOwner is destroyed:
        // onBackPressedDispatcher.addCallback(this /* LifecycleOwner */, onBackPressedCallback)

        setContent {
            ZenTheme {
                BlockingScreenComposable (
                    appName = "YouTube", // You could pass this from the intent if needed
                    onContinueToApp = {
                        finish()
                    },
                    onDoSomethingElse = {
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
    onContinueToApp: () -> Unit,
    onDoSomethingElse: () -> Unit
) {
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
                text = "You've opened $appName.",
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
                Text("Continue to $appName")
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
