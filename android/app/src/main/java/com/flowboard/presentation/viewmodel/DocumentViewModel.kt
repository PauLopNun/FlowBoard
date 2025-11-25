package com.flowboard.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flowboard.data.remote.dto.UserPresenceInfo
import com.flowboard.data.remote.websocket.WebSocketState
import com.flowboard.domain.model.CollaborativeDocument
import com.flowboard.domain.model.ContentBlock
import com.flowboard.utils.toDomain
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for collaborative document editing
 * Manages real-time synchronization and user presence
 */
@HiltViewModel
class DocumentViewModel @Inject constructor(
    private val documentRepository: com.flowboard.domain.repository.DocumentRepository,
    private val permissionRepository: com.flowboard.domain.repository.PermissionRepository,
    private val authRepository: com.flowboard.data.repository.AuthRepository
) : ViewModel() {

    private val _documentState = MutableStateFlow(DocumentState())
    val documentState: StateFlow<DocumentState> = _documentState.asStateFlow()

    // WebSocket connection state
    val connectionState: StateFlow<WebSocketState> = documentRepository.getConnectionState()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = WebSocketState.Disconnected
        )

    // Active users collaborating on documents
    val activeUsers: StateFlow<List<UserPresenceInfo>> = documentRepository.activeUsers
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        // Monitor connection state
        viewModelScope.launch {
            connectionState.collect { state ->
                _documentState.update {
                    it.copy(isConnected = state is WebSocketState.Connected)
                }
            }
        }

        // Listen for incoming operations
        viewModelScope.launch {
            documentRepository.getOperations().collect { operation ->
                handleRemoteOperation(operation)
            }
        }

        // Listen for updated blocks
        viewModelScope.launch {
            documentRepository.getUpdatedBlock().collect { updatedBlock ->
                _documentState.update { state ->
                    val updatedBlocks = state.document?.blocks?.map {
                        if (it.id == updatedBlock.id) {
                            updatedBlock
                        } else {
                            it
                        }
                    }
                    state.copy(document = state.document?.copy(blocks = updatedBlocks ?: emptyList()))
                }
            }
        }
    }

    fun updateCursor(blockId: String, position: Int) {
        viewModelScope.launch {
            val operation = com.flowboard.data.models.crdt.CursorMoveOperation(
                operationId = java.util.UUID.randomUUID().toString(),
                boardId = _documentState.value.document?.id ?: "",
                userId = "", // TODO: get real user id
                blockId = blockId,
                position = position
            )
            documentRepository.sendOperation(operation)
        }
    }

    private fun handleRemoteOperation(operation: com.flowboard.data.models.crdt.DocumentOperation) {
        when (operation) {
            is com.flowboard.data.models.crdt.UpdateBlockContentOperation -> {
                _documentState.update { state ->
                    val updatedBlocks = state.document?.blocks?.map {
                        if (it.id == operation.blockId) {
                            it.copy(content = operation.content)
                        } else {
                            it
                        }
                    }
                    state.copy(document = state.document?.copy(blocks = updatedBlocks ?: emptyList()))
                }
            }
            is com.flowboard.data.models.crdt.AddBlockOperation -> {
                _documentState.update { state ->
                    val updatedBlocks = state.document?.blocks?.toMutableList()
                    val index = if (operation.afterBlockId == null) 0 else updatedBlocks?.indexOfFirst { it.id == operation.afterBlockId }?.plus(1) ?: 0
                    updatedBlocks?.add(index, operation.block.toDomain())
                    state.copy(document = state.document?.copy(blocks = updatedBlocks ?: emptyList()))
                }
            }
            is com.flowboard.data.models.crdt.DeleteBlockOperation -> {
                _documentState.update { state ->
                    val updatedBlocks = state.document?.blocks?.filter { it.id != operation.blockId }
                    state.copy(document = state.document?.copy(blocks = updatedBlocks ?: emptyList()))
                }
            }
            is com.flowboard.data.models.crdt.UpdateBlockFormattingOperation -> {
                _documentState.update { state ->
                    val updatedBlocks = state.document?.blocks?.map {
                        if (it.id == operation.blockId) {
                            it.copy(
                                fontWeight = operation.fontWeight ?: it.fontWeight,
                                fontStyle = operation.fontStyle ?: it.fontStyle,
                                textDecoration = operation.textDecoration ?: it.textDecoration,
                                fontSize = operation.fontSize ?: it.fontSize,
                                color = operation.color ?: it.color,
                                textAlign = operation.textAlign ?: it.textAlign
                            )
                        } else {
                            it
                        }
                    }
                    state.copy(document = state.document?.copy(blocks = updatedBlocks ?: emptyList()))
                }
            }
            is com.flowboard.data.models.crdt.UpdateBlockTypeOperation -> {
                _documentState.update { state ->
                    val updatedBlocks = state.document?.blocks?.map {
                        if (it.id == operation.blockId) {
                            it.copy(type = operation.newType)
                        } else {
                            it
                        }
                    }
                    state.copy(document = state.document?.copy(blocks = updatedBlocks ?: emptyList()))
                }
            }
            is com.flowboard.data.models.crdt.CursorMoveOperation -> {
                _documentState.update { state ->
                    val updatedCursors = state.userCursors.filter { it.userId != operation.userId } + UserCursor(
                        userId = operation.userId,
                        blockId = operation.blockId ?: "",
                        position = operation.position
                    )
                    state.copy(userCursors = updatedCursors)
                }
            }
            // TODO: Handle other operations
        }
    }

    /**
     * Load a document by ID
     */
    fun loadDocument(documentId: String) {
        viewModelScope.launch {
            _documentState.update { it.copy(isLoading = true) }
            documentRepository.getDocument(documentId)
                .catch { e ->
                    _documentState.update {
                        it.copy(
                            isLoading = false,
                            error = e.message ?: "Failed to load document"
                        )
                    }
                }
                .collect { document ->
                    _documentState.update {
                        it.copy(
                            document = document.toDomain(),
                            isLoading = false
                        )
                    }
                }
        }
    }

    /**
     * Update document title with real-time sync
     */
    fun updateTitle(title: String) {
        viewModelScope.launch {
//            _documentState.update { it.copy(title = title) }

            // TODO: Send update through WebSocket
            // taskRepository.sendDocumentUpdate(documentId, "title", title)
        }
    }

    /**
     * Update document content with real-time sync
     */
    fun updateContent(operation: com.flowboard.data.models.crdt.DocumentOperation) {
        // Apply the operation to the local state immediately
        handleRemoteOperation(operation)
        viewModelScope.launch {
            documentRepository.sendOperation(operation)
        }
    }

    fun updateFormatting(operation: com.flowboard.data.models.crdt.UpdateBlockFormattingOperation) {
        // Apply the operation to the local state immediately
        handleRemoteOperation(operation)
        viewModelScope.launch {
            documentRepository.sendOperation(operation)
        }
    }

    /**
     * Share document with another user
     */
    fun shareDocument(email: String, permissionLevel: com.flowboard.domain.model.PermissionLevel) {
        viewModelScope.launch {
            try {
                _documentState.value.document?.id?.let { documentId ->
                    val request = com.flowboard.domain.model.GrantPermissionRequest(
                        resourceId = documentId,
                        resourceType = com.flowboard.domain.model.ResourceType.DOCUMENT,
                        userEmail = email,
                        level = permissionLevel
                    )

                    permissionRepository.grantPermission(request)
                        .onSuccess { permission ->
                            _documentState.update {
                                it.copy(
                                    sharedWith = it.sharedWith + DocumentPermission(
                                        email,
                                        permissionLevel.name
                                    )
                                )
                            }
                        }
                        .onFailure { error ->
                            _documentState.update {
                                it.copy(error = error.message ?: "Failed to share document")
                            }
                        }
                }
            } catch (e: Exception) {
                _documentState.update {
                    it.copy(error = e.message ?: "Failed to share document")
                }
            }
        }
    }

    /**
     * Create a new document
     */
    fun createDocument(title: String, content: String = "") {
        viewModelScope.launch {
            _documentState.update { it.copy(isLoading = true) }

            try {
                // TODO: Implement document creation in repository
                val documentId = java.util.UUID.randomUUID().toString()

                _documentState.update {
                    it.copy(
                        document = CollaborativeDocument(
                            id = documentId,
                            blocks = listOf(
                                ContentBlock("1", "h1", title),
                                ContentBlock("2", "p", content)
                            )
                        ),
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _documentState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to create document"
                    )
                }
            }
        }
    }

    /**
     * Save document to local storage/backend
     */
    fun saveDocument() {
        viewModelScope.launch {
            try {
                val state = _documentState.value
                // TODO: Implement save logic
                // taskRepository.saveDocument(state.documentId, state.title, state.content)

                _documentState.update {
                    it.copy(lastSaved = System.currentTimeMillis())
                }
            } catch (e: Exception) {
                _documentState.update {
                    it.copy(error = e.message ?: "Failed to save document")
                }
            }
        }
    }

    /**
     * Handle incoming document updates from other users
     */
    private fun handleRemoteUpdate(update: com.flowboard.data.models.crdt.DocumentOperation) {
        viewModelScope.launch {
//            when (update.field) {
//                "title" -> _documentState.update { it.copy(title = update.value) }
//                "content" -> _documentState.update { it.copy(content = update.value) }
//            }
        }
    }

    fun clearError() {
        _documentState.update { it.copy(error = null) }
    }
}

/**
 * State for document editing
 */
data class DocumentState(
    val document: CollaborativeDocument? = null,
    val isLoading: Boolean = false,
    val isConnected: Boolean = false,
    val error: String? = null,
    val sharedWith: List<DocumentPermission> = emptyList(),
    val lastSaved: Long? = null,
    val userCursors: List<UserCursor> = emptyList()
)

data class UserCursor(
    val userId: String,
    val blockId: String,
    val position: Int
)


/**
 * Document permission info
 */
data class DocumentPermission(
    val email: String,
    val permission: String // "view" or "edit"
)

