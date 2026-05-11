package com.flowboard.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flowboard.data.local.entities.DocumentEntity
import com.flowboard.data.local.entities.WorkspaceEntity
import com.flowboard.data.local.dao.DocumentDao
import com.flowboard.data.local.dao.WorkspaceDao
import com.flowboard.data.repository.DocumentRepositoryImpl
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WorkspaceDocumentsUiState(
    val documents: List<DocumentEntity> = emptyList(),
    val workspaces: List<WorkspaceEntity> = emptyList(),
    val workspaceName: String = "",
    val workspaceImageUrl: String? = null,
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

    init {
        viewModelScope.launch {
            workspaceDao.getAllWorkspaces().collect { workspaces ->
                _uiState.update { state ->
                    val currentWorkspace = currentWorkspaceId?.let { id -> workspaces.firstOrNull { it.id == id } }
                    state.copy(
                        workspaces = workspaces,
                        workspaceName = currentWorkspace?.name ?: state.workspaceName,
                        workspaceImageUrl = currentWorkspace?.imageUrl ?: state.workspaceImageUrl
                    )
                }
            }
        }
    }

    fun load(workspaceId: String) {
        if (currentWorkspaceId == workspaceId) return
        currentWorkspaceId = workspaceId

        // Load workspace name
        viewModelScope.launch {
            workspaceDao.getById(workspaceId)?.let { ws ->
                _uiState.update { it.copy(workspaceName = ws.name, workspaceImageUrl = ws.imageUrl) }
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

    fun refresh(workspaceId: String? = currentWorkspaceId) {
        if (workspaceId == null) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            documentRepository.fetchWorkspaceDocuments(workspaceId)
                .onSuccess { docs ->
                    documentDao.deleteWorkspaceDocuments(workspaceId)
                    docs.forEach { documentDao.insertDocument(it.copy(workspaceId = workspaceId, visibility = "workspace")) }
                    _uiState.update { it.copy(isLoading = false, error = null) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
        }
    }

    fun moveToPrivate(documentId: String) {
        viewModelScope.launch {
            documentRepository.updateDocumentVisibility(documentId, "private", null)
                .onSuccess { updated ->
                    documentDao.insertDocument(updated)
                    _uiState.update { state ->
                        state.copy(documents = state.documents.filter { it.id != documentId })
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(error = e.message ?: "Failed to move document") }
                }
        }
    }

    fun moveToWorkspace(documentId: String, targetWorkspaceId: String) {
        viewModelScope.launch {
            documentRepository.updateDocumentVisibility(documentId, "workspace", targetWorkspaceId)
                .onSuccess { updated ->
                    documentDao.insertDocument(updated)
                    if (targetWorkspaceId != currentWorkspaceId) {
                        _uiState.update { state ->
                            state.copy(documents = state.documents.filter { it.id != documentId })
                        }
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(error = e.message ?: "Failed to move document") }
                }
        }
    }

    fun deleteDocument(documentId: String) {
        viewModelScope.launch {
            val now = kotlinx.datetime.Clock.System.now()
                .toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault()).toString()
            documentDao.softDeleteDocument(documentId, now)
            _uiState.update { state ->
                state.copy(documents = state.documents.filter { it.id != documentId })
            }
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }
}
