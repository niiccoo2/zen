package xyz.niiccoo2.zen.ui.screens

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
val OVERRIDE_ENABLED = booleanPreferencesKey("override_enabled")
val STAT_START_TIME = stringPreferencesKey("stat_start_time")
val Context.overrideEnabledFlow: Flow<Boolean>
    get() = dataStore.data
        .map { preferences ->
            preferences[OVERRIDE_ENABLED] ?: true  // default = true
        }
val Context.statStartTimeFlow: Flow<String>
    get() = dataStore.data.map { prefs ->
        prefs[STAT_START_TIME] ?: "00:00" // default = 00:00
    }


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val overrideEnabled by context.overrideEnabledFlow.collectAsState(initial = null)
    val statStartTime by context.statStartTimeFlow.collectAsState(initial = "")
    // State to track the expanded state and selected option
    var expanded by remember { mutableStateOf(false) }

    // List of options for the dropdown menu
    val options = listOf("00:00", "01:00", "02:00", "03:00", "04:00")

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box {
                Text(text="Settings:",
                    fontSize = 24.sp) // This is left justified
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row (
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text="Unblock for 5 minutes:")
                Spacer(Modifier.weight(1f)) // 1f makes it take up all available space
                overrideEnabled?.let { isEnabled ->
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = { checked ->
                            scope.launch {
                                context.dataStore.edit { prefs ->
                                    prefs[OVERRIDE_ENABLED] = checked
                                }
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(thickness = 2.dp)
            Spacer(modifier = Modifier.height(8.dp))

            Row (
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text="Stat reset time:")
                Spacer(Modifier.weight(1f)) // 1f makes it take up all available space
                Surface(
                    modifier = Modifier.width(400.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = !expanded }
                        ) {

                            TextField(
                                value = statStartTime,
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                                },
                                colors = ExposedDropdownMenuDefaults.textFieldColors(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            )

                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                options.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option, color = MaterialTheme.colorScheme.onSurface) },
                                        onClick = {
                                            scope.launch {
                                                context.dataStore.edit { prefs ->
                                                    prefs[STAT_START_TIME] = option
                                                }
                                            }
                                            expanded = false
                                        },
                                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                    )
                                }
                            }
                        }
                    }
                }

            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Thank you for downloading Zen, I hope it helps your life.",
                style = MaterialTheme.typography.titleSmall
            )

            Spacer(modifier = Modifier.height(8.dp))

            val websiteUrl = "https://niiccoo2.xyz/zen"
            Text(
                text = "Visit my Website",
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline
                ),
                modifier = Modifier
                    .clickable {
                        try {
                            uriHandler.openUri(websiteUrl)
                        } catch (e: Exception) {
                            println("Could not open URL: $websiteUrl. Error: ${e.message}")
                        }
                    }
                    .padding(8.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Made with ❤\uFE0F by Nico",
                style = MaterialTheme.typography.titleSmall
            )
        }
    }
}