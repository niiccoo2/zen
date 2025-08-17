package xyz.niiccoo2.zen.ui.screens

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
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
import androidx.compose.material3.CircularProgressIndicator
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import xyz.niiccoo2.zen.utils.AppSettings
import xyz.niiccoo2.zen.utils.TimeBlock
import xyz.niiccoo2.zen.utils.getAppNameAndIcon
import java.time.LocalTime
import java.util.Locale

data class AppDisplayInfo(
    val packageName: String,
    val name: String,
    val icon: Drawable?
)

fun getInstalledPackageNames(context: Context): List<AppDisplayInfo> {
    val pm = context.packageManager
    val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)

    return packages
        .filter { appInfo ->
            (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) == 0 || (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
        }
        .mapNotNull { appInfo ->
            val name = appInfo.loadLabel(pm).toString()
            val icon = try {
                appInfo.loadIcon(pm)
            } catch (_: Exception) {
                null
            }
            AppDisplayInfo(appInfo.packageName, name, icon)
        }
        .sortedBy { it.name.lowercase(Locale.getDefault()) }
}


@Composable
fun AppList(
    allApps: List<AppDisplayInfo>,
    onAppClick: (packageName: String) -> Unit
) {
    var searchText by remember { mutableStateOf("") }

    val filteredApps = remember(searchText, allApps) {
        if (searchText.isBlank()) {
            allApps
        } else {
            allApps.filter { app ->
                app.name.contains(searchText, ignoreCase = true)
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
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (searchText.isBlank() && allApps.isEmpty()) "No user-installed apps found."
                    else if (searchText.isBlank() && allApps.isNotEmpty()) "No apps to display."
                    else "No apps found matching \"$searchText\""
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
            ) {
                items(
                    items = filteredApps,
                    key = { app -> app.packageName }
                ) { app ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .clickable {
                                onAppClick(app.packageName)
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = app.icon,
                            contentDescription = "${app.name} icon",
                            modifier = Modifier.size(48.dp),
                        )
                        Spacer(Modifier.width(16.dp))
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
fun TimePickerDialog(
    onConfirm: (hour: Int, minute: Int) -> Unit,
    onDismiss: () -> Unit,
    initialHour: Int? = null,
    initialMinute: Int? = null
) {
    val currentCalendar = java.util.Calendar.getInstance()

    val timePickerState: TimePickerState = rememberTimePickerState(
        initialHour = initialHour ?: currentCalendar.get(java.util.Calendar.HOUR_OF_DAY),
        initialMinute = initialMinute ?: currentCalendar.get(java.util.Calendar.MINUTE),
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        text = {
            TimePicker(
                state = timePickerState,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(timePickerState.hour, timePickerState.minute)
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

    var isAlwaysBlocked by remember(selectedPackageName) { mutableStateOf(true) }
    var selectedStartHour by remember(selectedPackageName) { mutableStateOf<Int?>(null) }
    var selectedStartMinute by remember(selectedPackageName) { mutableStateOf<Int?>(null) }
    var selectedEndHour by remember(selectedPackageName) { mutableStateOf<Int?>(null) }
    var selectedEndMinute by remember(selectedPackageName) { mutableStateOf<Int?>(null) }

    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    var appName by remember(selectedPackageName) { mutableStateOf<String?>(null) }
    var appIcon by remember(selectedPackageName) { mutableStateOf<Drawable?>(null) }
    var isLoadingAppDetails by remember(selectedPackageName) { mutableStateOf(true) }


    LaunchedEffect(selectedPackageName) {
        if (selectedPackageName != null) {
            isLoadingAppDetails = true
            withContext(Dispatchers.IO) {
                val appInfoResult = getAppNameAndIcon(context, selectedPackageName)
                appName = appInfoResult?.first
                appIcon = appInfoResult?.second

                val currentSettings = AppSettings.getSpecificAppSetting(context, selectedPackageName)
                if (currentSettings != null) {
                    isAlwaysBlocked = currentSettings.isEffectivelyAlwaysBlocked
                    if (!isAlwaysBlocked && currentSettings.scheduledBlocks.isNotEmpty()) {
                        val firstBlock = currentSettings.scheduledBlocks.first()
                        selectedStartHour = firstBlock.startTime.hour
                        selectedStartMinute = firstBlock.startTime.minute
                        selectedEndHour = firstBlock.endTime.hour
                        selectedEndMinute = firstBlock.endTime.minute
                    } else {
                        selectedStartHour = null
                        selectedStartMinute = null
                        selectedEndHour = null
                        selectedEndMinute = null
                    }
                } else {
                    isAlwaysBlocked = true
                    selectedStartHour = null
                    selectedStartMinute = null
                    selectedEndHour = null
                    selectedEndMinute = null
                }
            }
            isLoadingAppDetails = false
        }
    }

    if (selectedPackageName == null) {
        Text("No application selected.")
        return
    }

    if (isLoadingAppDetails) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (appName == null) {
        Text("Error loading app details for $selectedPackageName.")
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            AsyncImage(
                model = appIcon,
                contentDescription = "$appName icon",
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(text = appName ?: "App", style = MaterialTheme.typography.titleLarge)
        }

        Spacer(Modifier.height(16.dp))

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
                    Spacer(Modifier.weight(1f)) // 1f makes it take up all available space
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

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                coroutineScope.launch {
                    val finalPackageName = selectedPackageName

                    if (isAlwaysBlocked) {
                        AppSettings.setAppAsAlwaysBlocked(context, finalPackageName)
                    } else {
                        if (selectedStartHour != null && selectedStartMinute != null &&
                            selectedEndHour != null && selectedEndMinute != null
                        ) {
                            val startTime = LocalTime.of(selectedStartHour!!, selectedStartMinute!!)
                            val endTime = LocalTime.of(selectedEndHour!!, selectedEndMinute!!)
                            val customBlock = TimeBlock(startTime, endTime)

                            AppSettings.updateSpecificAppSetting(context, finalPackageName) { existingSettings ->
                                existingSettings.copy(
                                    scheduledBlocks = listOf(customBlock),
                                    isOnBreak = false
                                )
                            }
                        } else {
                            return@launch
                        }
                    }
                    navController.popBackStack()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save Settings")
        }

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
    val context = LocalContext.current
    var allInstalledApps by remember { mutableStateOf<List<AppDisplayInfo>>(emptyList()) }
    var isLoadingApps by remember { mutableStateOf(true) }
    var selectedPackageName by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        isLoadingApps = true
        allInstalledApps = withContext(Dispatchers.IO) {
            getInstalledPackageNames(context)
        }
        isLoadingApps = false
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(if (selectedPackageName == null) "Add App to Block List" else "Configure Block") },
                windowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
                navigationIcon = {
                    IconButton(onClick = {
                        if (selectedPackageName != null) {
                            selectedPackageName = null
                        } else {
                            navController.popBackStack()
                        }
                    }) {
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
            if (selectedPackageName == null) {
                if (isLoadingApps) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                        Text("Loading apps...", modifier = Modifier.padding(top = 70.dp))
                    }
                } else {
                    AppList(
                        allApps = allInstalledApps,
                        onAppClick = { packageName ->
                            selectedPackageName = packageName
                        }
                    )
                }
            } else {
                NewBlockSettings(navController,selectedPackageName)
            }
        }
    }
}
