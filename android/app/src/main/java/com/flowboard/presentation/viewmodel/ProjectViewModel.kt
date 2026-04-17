package com.flowboard.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flowboard.data.local.entities.ProjectEntity
import com.flowboard.data.repository.ProjectRepositoryImpl
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProjectListUiState(
    val projects: List<ProjectEntity> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentUserId: String? = null
)

@HiltViewModel
class ProjectViewModel @Inject constructor(
    private val repository: ProjectRepositoryImpl
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProjectListUiState())
    val uiState: StateFlow<ProjectListUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _uiState.update { it.copy(currentUserId = repository.getCurrentUserId()) }
        }
        observeProjects()
        refresh()
    }

    private fun observeProjects() {
        viewModelScope.launch {
            repository.getProjects().collect { projects ->
                _uiState.update { it.copy(projects = projects) }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.refresh()
                .onSuccess { _uiState.update { it.copy(isLoading = false, error = null) } }
                .onFailure { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
        }
    }

    fun createProject(name: String, description: String, color: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.createProject(name, description, color)
                .onSuccess { _uiState.update { it.copy(isLoading = false, error = null) } }
                .onFailure { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
        }
    }

    fun deleteProject(id: String) {
        viewModelScope.launch {
            repository.deleteProject(id)
                .onFailure { e -> _uiState.update { it.copy(error = e.message) } }
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }
}
