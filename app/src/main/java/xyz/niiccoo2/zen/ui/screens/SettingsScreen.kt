package xyz.niiccoo2.zen.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import xyz.niiccoo2.zen.utils.AppSettings

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope() // Get a CoroutineScope
    fun saveMyListOfBlockedApps(blockedAppsSet: Set<String>) {
        coroutineScope.launch { // Use the coroutineScope obtained from rememberCoroutineScope()
            try {
                AppSettings.saveBlockedApps(context, blockedAppsSet) // Use context from LocalContext.current
                Log.i("BlockScreen", "Blocked apps saved successfully: $blockedAppsSet")
                // You could show a Toast or update UI here (ensure UI updates are on the main thread if needed)
            } catch (e: Exception) {
                Log.e("BlockScreen", "Error saving blocked apps", e)
                // Handle error, show a message to the user, etc.
            }
        }
    }

    fun blockApps() {
        Log.i("BlockScreen", "Blocking apps")
        val appsToBlock = setOf(
            "com.discord",
            "org.mozilla.firefox",
            "com.google.android.youtube"
        )
        saveMyListOfBlockedApps(appsToBlock)
    }

    fun unblockApps() {
        Log.i("BlockScreen", "Unblocking apps")
        saveMyListOfBlockedApps(emptySet())
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Button(
                onClick = { blockApps() }, // Calls the updated blockApps
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Text(text = "Block Apps")
            }
            OutlinedButton(
                onClick = { unblockApps() }, // Calls the updated unblockApps
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Text(text = "Unblock Apps")
            }
        }
    }
}