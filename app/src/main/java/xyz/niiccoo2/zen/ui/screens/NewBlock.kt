package xyz.niiccoo2.zen.ui.screens

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerState
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import xyz.niiccoo2.zen.utils.AppSettings
import xyz.niiccoo2.zen.utils.TimeBlock
import xyz.niiccoo2.zen.utils.getAppNameAndIcon
import java.time.LocalTime
import java.util.Locale

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
fun TimePickerDialog( // Renamed for clarity, and it's now a full dialog
    onConfirm: (hour: Int, minute: Int) -> Unit,
    onDismiss: () -> Unit, // This will now be triggered by AlertDialog's onDismissRequest
    initialHour: Int? = null,
    initialMinute: Int? = null
) {
    val currentCalendar = java.util.Calendar.getInstance() // Using java.util.Calendar if not already imported

    val timePickerState: TimePickerState = rememberTimePickerState(
        initialHour = initialHour ?: currentCalendar.get(java.util.Calendar.HOUR_OF_DAY),
        initialMinute = initialMinute ?: currentCalendar.get(java.util.Calendar.MINUTE),
    )

    AlertDialog(
        onDismissRequest = onDismiss, // Call onDismiss when the dialog is dismissed (e.g., by back press or clicking outside)
        // You can customize title, shape, colors etc.
        // title = { Text("Select Time") },
        // shape = RoundedCornerShape(16.dp),
        text = { // The content of the dialog
            // TimePicker is the full clock face picker.
            // If you prefer the TimeInput fields, you can use that instead,
            // or use a TimePicker with a toggle for input mode.
            TimePicker(
                state = timePickerState,
                modifier = Modifier.fillMaxWidth() // Adjust as needed
            )
            // If using TimeInput instead of TimePicker:
            // TimeInput(state = timePickerState, modifier = Modifier.fillMaxWidth())
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(timePickerState.hour, timePickerState.minute)
                    // onDismiss() // Also call onDismiss to ensure the dialog closes after confirm
                }
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun NewBlockSettings (navController: NavController, selectedPackageName: String?) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // UI States
    var isAlwaysBlocked by remember(selectedPackageName) { mutableStateOf(true) }
    var selectedStartHour by remember(selectedPackageName) { mutableStateOf<Int?>(null) }
    var selectedStartMinute by remember(selectedPackageName) { mutableStateOf<Int?>(null) }
    var selectedEndHour by remember(selectedPackageName) { mutableStateOf<Int?>(null) }
    var selectedEndMinute by remember(selectedPackageName) { mutableStateOf<Int?>(null) }

    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    // Load initial settings
    LaunchedEffect(selectedPackageName) {
        if (selectedPackageName != null) {
            val currentSettings = AppSettings.getSpecificAppSetting(context, selectedPackageName)
            if (currentSettings != null) {
                isAlwaysBlocked = currentSettings.isEffectivelyAlwaysBlocked
                if (!isAlwaysBlocked && currentSettings.scheduledBlocks.isNotEmpty()) {
                    // Assuming the first block is the one set by this UI
                    val firstBlock = currentSettings.scheduledBlocks.first()
                    selectedStartHour = firstBlock.startTime.hour
                    selectedStartMinute = firstBlock.startTime.minute
                    selectedEndHour = firstBlock.endTime.hour
                    selectedEndMinute = firstBlock.endTime.minute
                } else {
                    // Reset times if always blocked or no schedule
                    selectedStartHour = null
                    selectedStartMinute = null
                    selectedEndHour = null
                    selectedEndMinute = null
                }
            } else {
                // Default to always blocked if no settings exist
                isAlwaysBlocked = true
                selectedStartHour = null
                selectedStartMinute = null
                selectedEndHour = null
                selectedEndMinute = null
            }
        }
    }

    if (selectedPackageName == null) {
        Log.w("NewBlockSettings", "selectedPackageName is null. Cannot display settings.")
        Text("No application selected.")
        return
    }

    val appInfo = getAppNameAndIcon(context = context, packageName = selectedPackageName)

    if (appInfo == null) {
        Log.e("NewBlockSettings", "Could not get app info for $selectedPackageName")
        Text("Error loading app details for $selectedPackageName.")
        return
    }

    val (nameFromAppInfo, iconFromAppInfo) = appInfo
    val name = remember(selectedPackageName) { nameFromAppInfo }
    val rememberedIcon = remember(selectedPackageName) { iconFromAppInfo }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Row for Icon and App Name (as before)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            AsyncImage(
                model = rememberedIcon,
                contentDescription = "$name icon",
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(text = name, style = MaterialTheme.typography.titleLarge)
        }

        Spacer(Modifier.height(16.dp))

        // Settings content area
        Column(modifier = Modifier.weight(1f)) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row (
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Always Blocked", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.weight(1f))
                    Switch(
                        checked = isAlwaysBlocked,
                        onCheckedChange = { isAlwaysBlocked = it }
                    )
                }

                if (!isAlwaysBlocked) {
                    HorizontalDivider(Modifier.padding(vertical = 8.dp), thickness = 1.dp, color = Color.Gray)
                    Row (
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            if (selectedStartHour != null && selectedStartMinute != null)
                                String.format(Locale.US, "%02d:%02d", selectedStartHour, selectedStartMinute)
                            else "00:00",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(text = " - ", style = MaterialTheme.typography.titleLarge)
                        Text(
                            if (selectedEndHour != null && selectedEndMinute != null)
                                String.format(Locale.US, "%02d:%02d", selectedEndHour, selectedEndMinute)
                            else "00:00",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Row (
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(onClick = { showStartPicker = true }) { Text("Pick Start Time") }
                        Spacer(Modifier.width(16.dp))
                        Button(onClick = { showEndPicker = true }) { Text("Pick End Time") }
                    }
                }
            }
        }

        // Save Button
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                coroutineScope.launch {
                    if (isAlwaysBlocked) {
                        AppSettings.setAppAsAlwaysBlocked(context, selectedPackageName)
                        Log.d("NewBlockSettings", "Saved $selectedPackageName as always blocked.")
                    } else {
                        // Ensure both start and end times are selected for a custom schedule
                        if (selectedStartHour != null && selectedStartMinute != null &&
                            selectedEndHour != null && selectedEndMinute != null
                        ) {
                            val startTime = LocalTime.of(selectedStartHour!!, selectedStartMinute!!)
                            val endTime = LocalTime.of(selectedEndHour!!, selectedEndMinute!!)
                            val customBlock = TimeBlock(startTime, endTime)

                            // Update existing settings or create new ones with this single block
                            AppSettings.updateSpecificAppSetting(context, selectedPackageName) { existingSettings ->
                                existingSettings.copy(
                                    scheduledBlocks = listOf(customBlock), // Replace with the new single block
                                    isOnBreak = false // Ensure break is off when setting schedule
                                )
                            }
                            Log.d("NewBlockSettings", "Saved custom schedule for $selectedPackageName: $customBlock")
                        } else {
                            // Handle case where times are not fully set for custom block
                            Log.w("NewBlockSettings", "Custom schedule not saved. Start or end time missing.")
                            // Optionally show a message to the user
                            // Toast.makeText(context, "Please select both start and end times.", Toast.LENGTH_LONG).show()
                            return@launch // Don't proceed with saving if times are incomplete
                        }
                    }
                    // Optionally navigate back or show a success message
                    navController.popBackStack()
                    // Toast.makeText(context, "Settings Saved for $name", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save Settings")
        }

        // Time Picker Dialogs
        if (showStartPicker) {
            TimePickerDialog(
                onConfirm = { hour, minute ->
                    selectedStartHour = hour
                    selectedStartMinute = minute
                    showStartPicker = false
                },
                onDismiss = { showStartPicker = false },
                initialHour = selectedStartHour,
                initialMinute = selectedStartMinute
            )
        }
        if (showEndPicker) {
            TimePickerDialog(
                onConfirm = { hour, minute ->
                    selectedEndHour = hour
                    selectedEndMinute = minute
                    showEndPicker = false
                },
                onDismiss = { showEndPicker = false },
                initialHour = selectedEndHour,
                initialMinute = selectedEndMinute
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewBlock(
    navController: NavController,
    modifier: Modifier = Modifier

) {
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
            contentAlignment = Alignment.TopCenter
        ) {
            var selectedPackageName by remember { mutableStateOf<String?>(null) }

            if (selectedPackageName == null) {
                AppList(
                    onAppClick = { packageName ->
                        Log.d("NewBlockScreen", "App clicked in AppList! Package: $packageName")
                        selectedPackageName = packageName // Update state to show settings
                        // Now NewBlockSettings will be shown for this package
                    }
                )
            } else {
                NewBlockSettings(navController,selectedPackageName)
            }
        }
    }
}
