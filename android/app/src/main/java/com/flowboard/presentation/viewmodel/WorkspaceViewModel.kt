package com.flowboard.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flowboard.data.local.entities.WorkspaceEntity
import com.flowboard.data.repository.WorkspaceRepositoryImpl
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WorkspaceUiState(
    val workspaces: List<WorkspaceEntity> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val message: String? = null
)

@HiltViewModel
class WorkspaceViewModel @Inject constructor(
    private val workspaceRepository: WorkspaceRepositoryImpl
) : ViewModel() {

    private val _uiState = MutableStateFlow(WorkspaceUiState())
    val uiState: StateFlow<WorkspaceUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            workspaceRepository.getAllWorkspaces().collect { list ->
                _uiState.update { it.copy(workspaces = list) }
            }
        }
        fetchWorkspaces()
    }

    fun fetchWorkspaces() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            workspaceRepository.fetchWorkspaces()
                .onSuccess { _uiState.update { it.copy(isLoading = false, error = null) } }
                .onFailure { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
        }
    }

    fun createWorkspace(name: String, description: String?, onSuccess: (WorkspaceEntity) -> Unit = {}) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            workspaceRepository.createWorkspace(name, description)
                .onSuccess { ws ->
                    _uiState.update { it.copy(isLoading = false, message = "Workspace created!") }
                    onSuccess(ws)
                }
                .onFailure { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
        }
    }

    fun joinWorkspace(inviteCode: String, onSuccess: (WorkspaceEntity) -> Unit = {}) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            workspaceRepository.joinWorkspace(inviteCode)
                .onSuccess { ws ->
                    _uiState.update { it.copy(isLoading = false, message = "Joined workspace!") }
                    onSuccess(ws)
                }
                .onFailure { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
        }
    }

    fun deleteWorkspace(id: String) {
        viewModelScope.launch {
            workspaceRepository.deleteWorkspace(id)
                .onSuccess { _uiState.update { it.copy(message = "Workspace deleted") } }
                .onFailure { e -> _uiState.update { it.copy(error = e.message) } }
        }
    }

    fun clearMessage() = _uiState.update { it.copy(message = null) }
    fun clearError() = _uiState.update { it.copy(error = null) }
}
