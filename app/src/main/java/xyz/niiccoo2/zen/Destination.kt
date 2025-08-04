package xyz.niiccoo2.zen

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

// This is an "enum class". It's a special kind of class for defining a fixed set of constants.
enum class Destination(
    val route: String,          // Unique ID for navigation
    val icon: ImageVector?,      // The icon for the tab
    val label: String?,          // The text label for the tab
    val contentDescription: String, // For accessibility
    val showInBottomBar: Boolean = true // Whether to show in the bottom bar
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
    ),

    NEW_BLOCK( // Name it descriptively
        route = "add_app_to_block_screen",
        icon = null, // Not shown in bottom bar, so can be null
        label = null,      // Not shown in bottom bar
        contentDescription = "Add app to block list screen", // Still good for general accessibility context
        false // Not shown in bottom bar
    );

}