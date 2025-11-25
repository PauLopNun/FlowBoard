package com.flowboard.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flowboard.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel para manejar el login y guardar auth data
 *
 * EJEMPLO DE USO:
 * 1. Usuario ingresa email y password en LoginScreen
 * 2. Se llama a login(email, password)
 * 3. Se hace request al backend para obtener JWT token
 * 4. Se guarda token, userId, username en AuthRepository
 * 5. TaskViewModel automáticamente carga estos valores
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val authApiService: com.flowboard.data.remote.api.AuthApiService
) : ViewModel() {

    companion object {
        private const val TAG = "LoginViewModel"
    }

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    init {
        checkLoginStatus()
    }

    private fun checkLoginStatus() {
        viewModelScope.launch {
            _isLoggedIn.value = authRepository.isLoggedIn()
            Log.d(TAG, "Initial login status: ${_isLoggedIn.value}")
        }
    }

    /**
     * Hace login con el backend y guarda el token
     *
     * @param email Email del usuario
     * @param password Password del usuario
     */
    fun login(email: String, password: String) {
        viewModelScope.launch {
            Log.d(TAG, "Login initiated for email: $email")
            _loginState.value = LoginState.Loading

            try {
                Log.d(TAG, "Calling authApiService.login...")
                // Llamada real al backend
                val result = authApiService.login(
                    com.flowboard.data.remote.api.LoginRequest(
                        email = email,
                        password = password
                    )
                )

                Log.d(TAG, "API call completed, processing result...")
                result.fold(
                    onSuccess = { response ->
                        Log.d(TAG, "Login response received - success: ${response.success}")
                        if (response.success) {
                            Log.d(TAG, "Saving auth data...")
                            // Guardar datos de auth
                            authRepository.saveAuth(
                                token = response.token,
                                userId = response.userId,
                                username = response.username
                            )

                            // Guardar board ID por defecto si el backend lo devuelve
                            response.defaultBoardId?.let { boardId ->
                                Log.d(TAG, "Saving default board ID: $boardId")
                                authRepository.saveBoardId(boardId)
                            } ?: run {
                                Log.d(TAG, "No default board ID, using 'default-board'")
                                // Si no hay board por defecto, usar uno genérico
                                authRepository.saveBoardId("default-board")
                            }

                            _loginState.value = LoginState.Success
                            _isLoggedIn.value = true
                            Log.d(TAG, "Login completed successfully")
                        } else {
                            Log.w(TAG, "Login failed: response.success = false")
                            _loginState.value = LoginState.Error("Login failed")
                        }
                    },
                    onFailure = { exception ->
                        Log.e(TAG, "Login failed with exception: ${exception.message}", exception)
                        _loginState.value = LoginState.Error(
                            exception.message ?: "Network error occurred"
                        )
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error during login: ${e.message}", e)
                _loginState.value = LoginState.Error(e.message ?: "Unknown error occurred")
            }
        }
    }

    /**
     * Logout - limpia todos los datos de auth
     */
    fun logout() {
        viewModelScope.launch {
            Log.d(TAG, "Logout initiated")
            authRepository.clearAuth()
            _isLoggedIn.value = false
            _loginState.value = LoginState.Idle
            Log.d(TAG, "Logout completed")
        }
    }

}

/**
 * Estados del login
 */
sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    object Success : LoginState()
    data class Error(val message: String) : LoginState()
}
