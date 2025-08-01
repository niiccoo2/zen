package xyz.niiccoo2.zen

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

// This is an "enum class". It's a special kind of class for defining a fixed set of constants.
enum class Destination(
    val route: String,          // Unique ID for navigation
    val icon: ImageVector,      // The icon for the tab
    val label: String,          // The text label for the tab
    val contentDescription: String // For accessibility
) {
    // Define your actual screens here. I'm making up some examples.
    // You can change these names, routes, icons, and labels to fit your app.

    STATS( // This is the first "destination" or "screen"
        route = "stats_screen",                     // How navigation will refer to it
        icon = Icons.Filled.Schedule,                  // The icon (from Material Icons library)
        label = "Stats",                            // Text under the icon
        contentDescription = "Go to Home Screen"   // For screen readers
    ), // Comma separates enum entries

    BLOCKS( // Second destination
        route = "blocker_screen",
        icon = Icons.Filled.Block, // Example icon, change as needed!
        label = "Blocks",
        contentDescription = "Go to Blocker Settings"
    ),

    APP_SETTINGS( // Third destination
        route = "app_settings_screen",
        icon = Icons.Filled.Settings,
        label = "Settings",
        contentDescription = "Go to App Settings"
    ); // Semicolon at the end of enum entries is optional in modern Kotlin but good practice

    // This part allows you to easily get all entries.
    // In Kotlin 1.9+, `Destination.entries` is available by default for enums.
    // If you are on an older Kotlin or for explicit clarity, you can keep this.
    // companion object {
    //    fun getAllDestinations(): List<Destination> = entries.toList()
    // }
}