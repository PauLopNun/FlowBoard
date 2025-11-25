package com.flowboard.presentation.viewmodel

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
    private val authRepository: AuthRepository
    // TODO: Inject your API service to call backend login endpoint
    // private val authApiService: AuthApiService
) : ViewModel() {

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
            _loginState.value = LoginState.Loading

            try {
                // TODO: Replace with actual backend API call
                // Example:
                // val response = authApiService.login(LoginRequest(email, password))

                // SIMULACIÓN - Reemplaza con llamada real al backend
                val simulatedResponse = simulateBackendLogin(email, password)

                if (simulatedResponse.success) {
                    // Guardar datos de auth
                    authRepository.saveAuth(
                        token = simulatedResponse.token,
                        userId = simulatedResponse.userId,
                        username = simulatedResponse.username
                    )

                    // Guardar board ID por defecto (opcional)
                    authRepository.saveBoardId("default-board")

                    _loginState.value = LoginState.Success
                    _isLoggedIn.value = true
                } else {
                    _loginState.value = LoginState.Error(simulatedResponse.errorMessage ?: "Login failed")
                }
            } catch (e: Exception) {
                _loginState.value = LoginState.Error(e.message ?: "Unknown error occurred")
            }
        }
    }

    /**
     * Logout - limpia todos los datos de auth
     */
    fun logout() {
        viewModelScope.launch {
            authRepository.clearAuth()
            _isLoggedIn.value = false
            _loginState.value = LoginState.Idle
        }
    }

    /**
     * SIMULACIÓN de llamada al backend
     *
     * REEMPLAZA ESTO CON TU LLAMADA REAL AL BACKEND:
     *
     * suspend fun realBackendLogin(email: String, password: String): LoginResponse {
     *     val response = authApiService.login(LoginRequest(email, password))
     *     return LoginResponse(
     *         success = true,
     *         token = response.token,
     *         userId = response.userId,
     *         username = response.username
     *     )
     * }
     */
    private suspend fun simulateBackendLogin(email: String, password: String): LoginResponse {
        // Simulación de delay de red
        kotlinx.coroutines.delay(1000)

        // Simulación de validación simple
        return if (email.isNotEmpty() && password.length >= 6) {
            LoginResponse(
                success = true,
                token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VySWQiOiJ0ZXN0LXVzZXItMDAxIiwidXNlcm5hbWUiOiJ0ZXN0dXNlciIsImlhdCI6MTcwMDAwMDAwMCwiZXhwIjoxNzAwMDg2NDAwfQ.test-signature",
                userId = "test-user-001",
                username = email.substringBefore("@")
            )
        } else {
            LoginResponse(
                success = false,
                errorMessage = "Invalid credentials"
            )
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

/**
 * Response del backend (ejemplo)
 */
data class LoginResponse(
    val success: Boolean,
    val token: String = "",
    val userId: String = "",
    val username: String = "",
    val errorMessage: String? = null
)
