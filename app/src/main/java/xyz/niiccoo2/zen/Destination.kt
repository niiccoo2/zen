package xyz.niiccoo2.zen

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

enum class Destination(
    val route: String,
    val icon: ImageVector?,
    val label: String?,
    val contentDescription: String,
    val showInBottomBar: Boolean = true
) {
    STATS(
        route = "stats_screen",
        icon = Icons.Filled.Schedule,
        label = "Stats",
        contentDescription = "Go to Home Screen"
    ),

    BLOCKS(
        route = "blocker_screen",
        icon = Icons.Filled.Block,
        label = "Blocks",
        contentDescription = "Go to Blocker Settings"
    ),

    APP_SETTINGS(
        route = "app_settings_screen",
        icon = Icons.Filled.Settings,
        label = "Settings",
        contentDescription = "Go to App Settings"
    ),

    NEW_BLOCK(
        route = "add_app_to_block_screen",
        icon = null,
        label = null,
        contentDescription = "Add app to block list screen",
        false
    );

}