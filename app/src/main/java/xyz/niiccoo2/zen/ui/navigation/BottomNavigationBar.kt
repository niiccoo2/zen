package xyz.niiccoo2.zen.ui.navigation

// --- Standard Compose and Navigation Imports ---
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import xyz.niiccoo2.zen.Destination

@Composable
fun BottomNavigationBar(modifier: Modifier = Modifier) {
    val navController: NavHostController = rememberNavController()
    // Use YOUR Destination enum here
    val startDestination = Destination.STATS // Assuming HOME is an entry in your Destination enum
    var selectedItemIndex by rememberSaveable { mutableIntStateOf(startDestination.ordinal) }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            NavigationBar(windowInsets = NavigationBarDefaults.windowInsets) {
                // Use YOUR Destination enum's entries here
                // Destination.entries is available if using Kotlin 1.9+
                // If on older Kotlin, you might need a helper like Destination.values().toList()
                // or a custom method in your enum like Destination.getAllDestinations()
                Destination.entries.filter { it.showInBottomBar } .forEachIndexed { index, destinationEntry ->
                    NavigationBarItem(
                        selected = selectedItemIndex == index,
                        onClick = {
                            selectedItemIndex = index
                            if (navController.currentDestination?.route != destinationEntry.route) {
                                navController.navigate(route = destinationEntry.route) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = {
                            destinationEntry.icon?.let {
                                Icon(
                                    imageVector = it, // Use icon from your Destination enum entry
                                    contentDescription = destinationEntry.contentDescription
                                )
                            }
                        },
                        label = { destinationEntry.label?.let { Text(it) } } // Use label from your Destination enum entry
                    )
                }
            }
        }
    ) { contentPadding ->
        // Call YOUR AppNavHost Composable
        AppNavHost(
            navController = navController,
            startDestination = startDestination, // Pass your start destination
            modifier = Modifier.padding(contentPadding)
        )
    }
}
