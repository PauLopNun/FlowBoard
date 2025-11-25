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
    private val authRepository: AuthRepository
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

    fun login(email: String, password: String) {
        viewModelScope.launch {
            Log.d(TAG, "Login initiated for email: $email")
            _loginState.value = LoginState.Loading

            try {
                val result = authRepository.login(email, password)

                result.fold(
                    onSuccess = { response ->
                        Log.d(TAG, "Login successful for user: ${response.username}")
                        _loginState.value = LoginState.Success
                        _isLoggedIn.value = true
                    },
                    onFailure = { exception ->
                        Log.e(TAG, "Login failed: ${exception.message}", exception)
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

    fun logout() {
        viewModelScope.launch {
            Log.d(TAG, "Logout initiated")
            authRepository.logout()
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
