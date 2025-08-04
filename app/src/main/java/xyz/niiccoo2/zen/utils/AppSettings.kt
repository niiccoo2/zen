package xyz.niiccoo2.zen.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Create a DataStore instance (usually as a singleton or extension property on Context)
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "zen_settings")

object AppSettings {
    val BLOCKED_APPS_KEY = stringSetPreferencesKey("blocked_app_packages")

    // Function to save the set of blocked apps
    suspend fun saveBlockedApps(context: Context, blockedApps: Set<String>) {
        context.dataStore.edit { settings ->
            settings[BLOCKED_APPS_KEY] = blockedApps
        }
    }

    // Flow to observe the set of blocked apps
    fun getBlockedApps(context: Context): Flow<Set<String>> {
        return context.dataStore.data.map { preferences ->
            preferences[BLOCKED_APPS_KEY] ?: emptySet()
        }
    }

    // --- Example for storing app-specific settings ---
    fun getAppBlockingModeKey(packageName: String) = stringSetPreferencesKey("${packageName}_blocking_mode")
    // You'd have similar get/set functions for these

}