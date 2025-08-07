package xyz.niiccoo2.zen.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler // Import this
import androidx.compose.ui.text.style.TextDecoration // For underline
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current // Get the UriHandler

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) { // Center column content
            Text(
                text = "There aren't any settings for the settings screen yet. :(",
                style = MaterialTheme.typography.titleSmall
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Thank you for downloading Zen, I hope it helps your life.",
                style = MaterialTheme.typography.titleSmall
            )

            Spacer(modifier = Modifier.height(24.dp)) // Added more space

            val websiteUrl = "https://niiccoo2.xyz/zen"
            Text(
                text = "Visit my Website",
                style = MaterialTheme.typography.bodyLarge.copy( // Or another appropriate style
                    color = MaterialTheme.colorScheme.primary, // Make it look like a link
                    textDecoration = TextDecoration.Underline // Underline to indicate clickability
                ),
                modifier = Modifier
                    .clickable {
                        try {
                            uriHandler.openUri(websiteUrl)
                        } catch (e: Exception) {
                            // Handle potential errors, e.g., no browser installed (rare on Android)
                            // You could show a Toast or log an error
                            println("Could not open URL: $websiteUrl. Error: ${e.message}")
                        }
                    }
                    .padding(8.dp) // Add some padding to make it easier to tap
            )
        }
    }
}