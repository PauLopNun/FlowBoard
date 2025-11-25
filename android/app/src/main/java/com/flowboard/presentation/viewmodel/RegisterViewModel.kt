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

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _registerState = MutableStateFlow<RegisterState>(RegisterState.Idle)
    val registerState: StateFlow<RegisterState> = _registerState.asStateFlow()

    fun register(email: String, password: String, username: String, fullName: String) {
        viewModelScope.launch {
            _registerState.value = RegisterState.Loading

            // Validate inputs
            if (email.isBlank() || password.isBlank() || username.isBlank()) {
                _registerState.value = RegisterState.Error("Please fill all required fields")
                return@launch
            }

            if (!isValidEmail(email)) {
                _registerState.value = RegisterState.Error("Invalid email format")
                return@launch
            }

            if (password.length < 6) {
                _registerState.value = RegisterState.Error("Password must be at least 6 characters")
                return@launch
            }

            if (username.length < 3) {
                _registerState.value = RegisterState.Error("Username must be at least 3 characters")
                return@launch
            }

            try {
                val result = authRepository.register(email, password, username, fullName)

                result.fold(
                    onSuccess = { authResponse ->
                        _registerState.value = RegisterState.Success(authResponse.user.username)
                    },
                    onFailure = { error ->
                        _registerState.value = RegisterState.Error(
                            error.message ?: "Registration failed"
                        )
                    }
                )
            } catch (e: Exception) {
                _registerState.value = RegisterState.Error(
                    e.message ?: "An unexpected error occurred"
                )
            }
        }
    }

    fun clearError() {
        if (_registerState.value is RegisterState.Error) {
            _registerState.value = RegisterState.Idle
        }
    }

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
}

sealed class RegisterState {
    object Idle : RegisterState()
    object Loading : RegisterState()
    data class Success(val username: String) : RegisterState()
    data class Error(val message: String) : RegisterState()
}
