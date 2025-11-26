package com.flowboard.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.flowboard.data.remote.api.AuthApiService
import com.flowboard.data.remote.api.AuthResponse
import com.flowboard.data.remote.api.LoginRequest
import com.flowboard.data.remote.api.RegisterRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
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
    private val dataStore: DataStore<Preferences>,
    private val authApiService: AuthApiService
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
     * Get user name (alias for getUsername)
     */
    suspend fun getUserName(): String? = getUsername()

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

    /**
     * Login with email and password
     */
    suspend fun login(email: String, password: String): Result<AuthResponse> {
        val result = authApiService.login(LoginRequest(email, password))

        result.onSuccess { authResponse ->
            saveAuth(authResponse.token, authResponse.userId, authResponse.username)
            authResponse.defaultBoardId?.let { saveBoardId(it) }
        }

        return result
    }

    /**
     * Register new user
     */
    suspend fun register(
        email: String,
        password: String,
        username: String,
        fullName: String
    ): Result<AuthResponse> {
        val result = authApiService.register(
            RegisterRequest(email, password, username, fullName)
        )

        result.onSuccess { authResponse ->
            saveAuth(authResponse.token, authResponse.userId, authResponse.username)
            authResponse.defaultBoardId?.let { saveBoardId(it) }
        }

        return result
    }

    /**
     * Logout user
     */
    suspend fun logout() {
        clearAuth()
    }

    suspend fun searchUserByEmail(email: String): Result<com.flowboard.data.remote.api.UserData> {
        return authApiService.searchUserByEmail(email)
    }

    /**
     * Get current user profile
     */
    suspend fun getCurrentUser(): com.flowboard.domain.model.User? {
        val token = getToken() ?: return null
        val result = authApiService.getCurrentUser(token)

        return result.getOrNull()?.let { userData ->
            com.flowboard.domain.model.User(
                id = userData.id,
                email = userData.email,
                username = userData.username,
                fullName = userData.fullName,
                role = com.flowboard.data.local.entities.UserRole.USER,
                profileImageUrl = userData.profileImageUrl,
                isActive = userData.isActive,
                createdAt = Clock.System.now().toLocalDateTime(TimeZone.UTC),
                lastLoginAt = null
            )
        }
    }

    /**
     * Update user profile
     */
    suspend fun updateProfile(fullName: String?, profileImageUrl: String?): com.flowboard.domain.model.User? {
        val token = getToken() ?: return null
        val request = com.flowboard.data.remote.api.UpdateProfileRequest(fullName, profileImageUrl)
        val result = authApiService.updateProfile(token, request)

        return result.getOrNull()?.let { userData ->
            com.flowboard.domain.model.User(
                id = userData.id,
                email = userData.email,
                username = userData.username,
                fullName = userData.fullName,
                role = com.flowboard.data.local.entities.UserRole.USER,
                profileImageUrl = userData.profileImageUrl,
                isActive = userData.isActive,
                createdAt = Clock.System.now().toLocalDateTime(TimeZone.UTC),
                lastLoginAt = null
            )
        }
    }

    /**
     * Update password
     */
    suspend fun updatePassword(oldPassword: String, newPassword: String): Boolean {
        val token = getToken() ?: return false
        val request = com.flowboard.data.remote.api.UpdatePasswordRequest(oldPassword, newPassword)
        val result = authApiService.updatePassword(token, request)
        return result.isSuccess
    }
}
