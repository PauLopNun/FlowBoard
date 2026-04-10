package com.flowboard.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : ViewModel() {

    companion object {
        private val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")
        private val NOTIFICATIONS_ENABLED_KEY = booleanPreferencesKey("notifications_enabled")
        private val NOTIF_DOCS_KEY = booleanPreferencesKey("notif_docs")
        private val NOTIF_CHAT_KEY = booleanPreferencesKey("notif_chat")
        private val NOTIF_TASKS_KEY = booleanPreferencesKey("notif_tasks")
    }

    val darkModeEnabled: StateFlow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[DARK_MODE_KEY] ?: false
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    val notificationsEnabled: StateFlow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[NOTIFICATIONS_ENABLED_KEY] ?: true
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    val notifDocsEnabled: StateFlow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[NOTIF_DOCS_KEY] ?: true
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    val notifChatEnabled: StateFlow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[NOTIF_CHAT_KEY] ?: true
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    val notifTasksEnabled: StateFlow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[NOTIF_TASKS_KEY] ?: true
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[DARK_MODE_KEY] = enabled
            }
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[NOTIFICATIONS_ENABLED_KEY] = enabled
            }
        }
    }

    fun setNotifDocsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[NOTIF_DOCS_KEY] = enabled
            }
        }
    }

    fun setNotifChatEnabled(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[NOTIF_CHAT_KEY] = enabled
            }
        }
    }

    fun setNotifTasksEnabled(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[NOTIF_TASKS_KEY] = enabled
            }
        }
    }
}
