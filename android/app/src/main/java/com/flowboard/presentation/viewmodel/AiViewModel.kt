package com.flowboard.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flowboard.data.remote.api.AiApiService
import com.flowboard.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AiMessage(
    val role: String, // "user" | "assistant"
    val text: String
)

data class AiUiState(
    val messages: List<AiMessage> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class AiViewModel @Inject constructor(
    private val aiApiService: AiApiService,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiUiState())
    val uiState: StateFlow<AiUiState> = _uiState.asStateFlow()

    fun ask(prompt: String, documentContext: String? = null) {
        if (prompt.isBlank()) return

        _uiState.update { it.copy(
            messages = it.messages + AiMessage("user", prompt),
            isLoading = true,
            error = null
        ) }

        viewModelScope.launch {
            val token = authRepository.getToken() ?: run {
                _uiState.update { it.copy(isLoading = false, error = "Not authenticated") }
                return@launch
            }

            aiApiService.ask(prompt, documentContext, token).fold(
                onSuccess = { reply ->
                    _uiState.update { it.copy(
                        messages = it.messages + AiMessage("assistant", reply),
                        isLoading = false
                    ) }
                },
                onFailure = { err ->
                    _uiState.update { it.copy(
                        isLoading = false,
                        error = err.message ?: "AI request failed"
                    ) }
                }
            )
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }
    fun clearMessages() = _uiState.update { it.copy(messages = emptyList()) }
}
