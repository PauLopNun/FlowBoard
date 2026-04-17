package com.flowboard.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flowboard.data.local.entities.DocumentEntity
import com.flowboard.data.local.dao.DocumentDao
import com.flowboard.data.local.dao.WorkspaceDao
import com.flowboard.data.repository.DocumentRepositoryImpl
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WorkspaceDocumentsUiState(
    val documents: List<DocumentEntity> = emptyList(),
    val workspaceName: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class WorkspaceDocumentsViewModel @Inject constructor(
    private val documentRepository: DocumentRepositoryImpl,
    private val documentDao: DocumentDao,
    private val workspaceDao: WorkspaceDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(WorkspaceDocumentsUiState())
    val uiState: StateFlow<WorkspaceDocumentsUiState> = _uiState.asStateFlow()

    private var currentWorkspaceId: String? = null

    fun load(workspaceId: String) {
        if (currentWorkspaceId == workspaceId) return
        currentWorkspaceId = workspaceId

        // Load workspace name
        viewModelScope.launch {
            workspaceDao.getById(workspaceId)?.let { ws ->
                _uiState.update { it.copy(workspaceName = ws.name) }
            }
        }

        // Observe local workspace documents
        viewModelScope.launch {
            documentDao.getWorkspaceDocuments(workspaceId).collect { docs ->
                _uiState.update { it.copy(documents = docs) }
            }
        }

        // Fetch fresh from server
        refresh(workspaceId)
    }

    fun refresh(workspaceId: String = currentWorkspaceId ?: return) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            documentRepository.fetchWorkspaceDocuments(workspaceId)
                .onSuccess { docs ->
                    // Upsert into local DB so the Flow above emits
                    docs.forEach { documentDao.insertDocument(it.copy(workspaceId = workspaceId, visibility = "workspace")) }
                    _uiState.update { it.copy(isLoading = false, error = null) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }
}
