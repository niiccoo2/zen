package xyz.niiccoo2.zen.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import xyz.niiccoo2.zen.Destination
import xyz.niiccoo2.zen.ui.screens.StatsScreen
import xyz.niiccoo2.zen.ui.screens.BlockScreen
import xyz.niiccoo2.zen.ui.screens.SettingsScreen


// This is our main navigation controller.
@Composable
fun AppNavHost(
    navController: NavHostController,       // The controller that manages navigation
    startDestination: Destination,          // Which screen to show first (from your enum)
    modifier: Modifier = Modifier           // Standard Compose modifier
) {

    val veryFastAnimationSpec = tween<Float>(durationMillis = 50) // Makes frames to make the animation faster
    // NavHost is the container that swaps different screen Composables
    NavHost(
        navController = navController,            // Pass the controller
        startDestination = startDestination.route, // Use the 'route' string from your enum
        modifier = modifier,                        // Apply any modifiers
        enterTransition = { fadeIn(animationSpec = veryFastAnimationSpec) },
        exitTransition = { fadeOut(animationSpec = veryFastAnimationSpec) },
        popEnterTransition = { fadeIn(animationSpec = veryFastAnimationSpec) },
        popExitTransition = { fadeOut(animationSpec = veryFastAnimationSpec) }
    ) {
        // Now, for each 'route' in your Destination enum, define what Composable screen to show.
        // The 'route' string in composable() MUST MATCH the 'route' string in your Destination enum.

        composable(route = Destination.STATS.route) { StatsScreen() }

        composable(route = Destination.BLOCKS.route) { BlockScreen() }

        composable(route = Destination.APP_SETTINGS.route) { SettingsScreen() }
    }
}

