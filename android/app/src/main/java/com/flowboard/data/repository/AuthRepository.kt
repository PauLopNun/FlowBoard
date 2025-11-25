package com.flowboard.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing authentication data
 *
 * Stores JWT token, userId, and boardId in DataStore
 * Use this to provide auth data to WebSocket connection
 */
@Singleton
class AuthRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        private val TOKEN_KEY = stringPreferencesKey("jwt_token")
        private val USER_ID_KEY = stringPreferencesKey("user_id")
        private val BOARD_ID_KEY = stringPreferencesKey("board_id")
        private val USERNAME_KEY = stringPreferencesKey("username")
    }

    /**
     * Get JWT token
     * Returns null if not logged in
     */
    suspend fun getToken(): String? {
        return dataStore.data.map { preferences ->
            preferences[TOKEN_KEY]
        }.first()
    }

    /**
     * Get user ID
     * Returns null if not logged in
     */
    suspend fun getUserId(): String? {
        return dataStore.data.map { preferences ->
            preferences[USER_ID_KEY]
        }.first()
    }

    /**
     * Get current board ID
     * Returns null if not selected
     */
    suspend fun getBoardId(): String? {
        return dataStore.data.map { preferences ->
            preferences[BOARD_ID_KEY]
        }.first()
    }

    /**
     * Get username
     * Returns null if not logged in
     */
    suspend fun getUsername(): String? {
        return dataStore.data.map { preferences ->
            preferences[USERNAME_KEY]
        }.first()
    }

    /**
     * Save auth data after successful login
     */
    suspend fun saveAuth(token: String, userId: String, username: String) {
        dataStore.edit { preferences ->
            preferences[TOKEN_KEY] = token
            preferences[USER_ID_KEY] = userId
            preferences[USERNAME_KEY] = username
        }
    }

    /**
     * Save current board ID
     */
    suspend fun saveBoardId(boardId: String) {
        dataStore.edit { preferences ->
            preferences[BOARD_ID_KEY] = boardId
        }
    }

    /**
     * Clear all auth data (logout)
     */
    suspend fun clearAuth() {
        dataStore.edit { preferences ->
            preferences.remove(TOKEN_KEY)
            preferences.remove(USER_ID_KEY)
            preferences.remove(BOARD_ID_KEY)
            preferences.remove(USERNAME_KEY)
        }
    }

    /**
     * Check if user is logged in
     */
    suspend fun isLoggedIn(): Boolean {
        return getToken() != null && getUserId() != null
    }

    /**
     * Observe auth state
     */
    fun observeAuthState(): Flow<Boolean> {
        return dataStore.data.map { preferences ->
            preferences[TOKEN_KEY] != null && preferences[USER_ID_KEY] != null
        }
    }
}
