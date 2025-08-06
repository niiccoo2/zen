package xyz.niiccoo2.zen.ui.screens

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import xyz.niiccoo2.zen.utils.AppSettings.addAppToBlockList

data class AppDisplayInfo(
    val packageName: String,
    val name: String,
    val icon: android.graphics.drawable.Drawable? // Or whatever type getAppNameAndIcon returns for icon
)

/**
 * Returns a list of AppDisplayInfo objects for all installed apps.
 *
 * @param context The context to use for accessing package manager.
 * @return A list of AppDisplayInfo objects.
 */
fun getInstalledPackageNames(context: Context): List<AppDisplayInfo> {
    val pm = context.packageManager
    val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)

    return packages
        .filter { appInfo ->
            (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) == 0 || (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
        }
        .mapNotNull { appInfo ->
            // Assuming getAppNameAndIcon can be adapted or directly use appInfo
            val name = appInfo.loadLabel(pm).toString()
            val icon = try { appInfo.loadIcon(pm) } catch (_: Exception) { null }
            AppDisplayInfo(appInfo.packageName, name, icon)
        }
        .sortedBy { it.name.lowercase() } // Sort by app name, case-insensitive
}


@Composable
fun AppList(onAppClick: (packageName: String) -> Unit) {
    val context = LocalContext.current
    var searchText by remember { mutableStateOf("") }
    val allPackageNames = getInstalledPackageNames(context = context)

    val filteredApps = remember(searchText, allPackageNames) {
        if (searchText.isBlank()) {
            allPackageNames
        } else {
            allPackageNames.filter { app ->
                app.name.contains(searchText, ignoreCase = true)
                // You could also filter by package name if desired:
                // app.name.contains(searchText, ignoreCase = true) ||
                // app.packageName.contains(searchText, ignoreCase = true)
            }
        }
    }
    Column {
        OutlinedTextField(
            value = searchText,
            onValueChange = { searchText = it },
            label = { Text("Search Apps") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            singleLine = true,
            leadingIcon = {
                Icon(Icons.Filled.Search, contentDescription = "Search Icon")
            }
        )
        if (filteredApps.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f), // Takes remaining space
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (searchText.isBlank()) "No user-installed apps found."
                    else "No apps found matching \"$searchText\""
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f) // Takes remaining space
            ) {
                items(
                    items = filteredApps,
                    key = { app -> app.packageName }
                ) { app ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp) // Adjusted padding a bit
                            .clickable {
                                onAppClick(app.packageName)
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = app.icon,
                            contentDescription = "${app.name} icon",
                            modifier = Modifier.size(48.dp),
                            //error = painterResource(id = R.drawable.ic_android_black_24dp) // Replace with your placeholder
                        )
                        Spacer(Modifier.width(16.dp)) // Slightly more space
                        Text(
                            text = app.name,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewBlock(
    navController: NavController,
    modifier: Modifier = Modifier

) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Add App to Block List") },
                windowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) { // Standard way to go back
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            //Text("This is where you'll add apps to the block list.")
            AppList(
                onAppClick = { packageName ->
                    // This lambda is executed when an app is clicked in AppList
                    Log.d("MainScreen", "App clicked in AppList! Package: $packageName")
                    // Update our state with the clicked package name
                    scope.launch {
                        addAppToBlockList(context = context, packageName = packageName)
                    }
                    navController.popBackStack() // Go back to the previous screen
                }
            )
        }
    }
}
