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

sealed class ForgotPasswordState {
    object Idle : ForgotPasswordState()
    object Loading : ForgotPasswordState()
    object CodeSent : ForgotPasswordState()
    object Success : ForgotPasswordState()
    data class Error(val message: String, val afterCodeSent: Boolean = false) : ForgotPasswordState()
}

@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow<ForgotPasswordState>(ForgotPasswordState.Idle)
    val state: StateFlow<ForgotPasswordState> = _state.asStateFlow()

    fun requestReset(email: String) {
        if (email.isBlank()) {
            _state.value = ForgotPasswordState.Error("Please enter your email address")
            return
        }
        viewModelScope.launch {
            _state.value = ForgotPasswordState.Loading
            authRepository.forgotPassword(email)
                .onSuccess { _state.value = ForgotPasswordState.CodeSent }
                .onFailure { _state.value = ForgotPasswordState.Error(it.message ?: "Failed to send reset code") }
        }
    }

    fun confirmReset(email: String, code: String, newPassword: String) {
        viewModelScope.launch {
            _state.value = ForgotPasswordState.Loading
            authRepository.resetPassword(email, code, newPassword)
                .onSuccess { _state.value = ForgotPasswordState.Success }
                .onFailure { _state.value = ForgotPasswordState.Error(it.message ?: "Invalid or expired code", afterCodeSent = true) }
        }
    }
}
