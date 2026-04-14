package com.flowboard.presentation.viewmodel

import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flowboard.data.crdt.CRDTEngine
import com.flowboard.data.models.*
import com.flowboard.data.models.crdt.*
import com.flowboard.data.remote.api.DocumentApiService
import com.flowboard.data.remote.websocket.ConnectionState
import com.flowboard.data.remote.websocket.DocumentWebSocketClient
import com.flowboard.data.repository.AuthRepository
import com.flowboard.presentation.ui.components.RemoteCursor
import com.flowboard.presentation.ui.components.getUserColor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

private const val TAG = "CollaborativeDocViewModel"

/**
 * ViewModel for collaborative document editing with CRDT and real-time sync
 */
@HiltViewModel
class CollaborativeDocumentViewModel @Inject constructor(
    private val crdtEngine: CRDTEngine,
    private val webSocketClient: DocumentWebSocketClient,
    private val authRepository: AuthRepository,
    private val documentApiService: DocumentApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(CollaborativeDocumentUiState())
    val uiState: StateFlow<CollaborativeDocumentUiState> = _uiState.asStateFlow()

    // CRDT document state
    val document: StateFlow<CollaborativeDocument?> = crdtEngine.document
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    // WebSocket connection state
    val connectionState: StateFlow<ConnectionState> = webSocketClient.connectionState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ConnectionState.Disconnected
        )

    // Active users
    val activeUsers: StateFlow<List<DocumentUserPresence>> = webSocketClient.activeUsers
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Remote cursors
    private val _remoteCursors = MutableStateFlow<Map<String, RemoteCursor>>(emptyMap())
    val remoteCursors: StateFlow<Map<String, RemoteCursor>> = _remoteCursors.asStateFlow()

    // Auto-save debounce (save 5 seconds after the last change)
    private var autoSaveJob: Job? = null

    init {
        // Listen for incoming operations from other users
        viewModelScope.launch {
            webSocketClient.incomingOperations.collect { broadcast ->
                Log.d(TAG, "Received operation from ${broadcast.userName}")
                handleRemoteOperation(broadcast)
            }
        }

        // Listen for cursor updates
        viewModelScope.launch {
            webSocketClient.cursorUpdates.collect { cursorUpdate ->
                updateRemoteCursor(cursorUpdate)
            }
        }

        // Listen for errors
        viewModelScope.launch {
            webSocketClient.errors.collect { error ->
                _uiState.update { it.copy(error = error) }
            }
        }

        // Sync CRDT document with WebSocket state
        viewModelScope.launch {
            webSocketClient.documentState.collect { wsDocument ->
                wsDocument?.let {
                    crdtEngine.initDocument(it.id, it.blocks)
                }
            }
        }
    }

    /**
     * Connect to a document for real-time collaboration
     */
    fun connectToDocument(documentId: String) {
        viewModelScope.launch {
            try {
                val userId = authRepository.getUserId() ?: run {
                    _uiState.update { it.copy(error = "User not authenticated") }
                    return@launch
                }

                val userName = authRepository.getUserName() ?: "Anonymous"
                val token = authRepository.getToken() ?: run {
                    _uiState.update { it.copy(error = "No authentication token") }
                    return@launch
                }

                Log.d(TAG, "Connecting to document $documentId as $userName")

                // Initialize CRDT engine
                crdtEngine.initDocument(documentId, emptyList())

                // Connect WebSocket
                webSocketClient.connect(
                    documentId = documentId,
                    userId = userId,
                    userName = userName,
                    token = token
                )

                _uiState.update {
                    it.copy(
                        currentDocumentId = documentId,
                        currentUserId = userId,
                        currentUserName = userName
                    )
                }

                // Load breadcrumb chain (parent hierarchy) in the background
                loadBreadcrumbs(documentId)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to connect to document", e)
                _uiState.update { it.copy(error = "Connection failed: ${e.message}") }
            }
        }
    }

    /**
     * Build a breadcrumb trail by walking up the parent chain.
     * Each entry is Pair(documentId, title).
     */
    private fun loadBreadcrumbs(documentId: String) {
        viewModelScope.launch {
            try {
                val chain = mutableListOf<Pair<String, String>>()
                var currentId: String? = documentId
                val visited = mutableSetOf<String>()

                while (currentId != null && visited.add(currentId)) {
                    val entity = try {
                        documentApiService.getDocumentById(currentId)
                    } catch (_: Exception) { break }

                    chain.add(0, currentId to entity.title)
                    currentId = entity.parentId
                }

                _uiState.update { it.copy(breadcrumbs = chain) }
            } catch (e: Exception) {
                Log.w(TAG, "Could not load breadcrumbs: ${e.message}")
            }
        }
    }

    /**
     * Disconnect from document
     */
    fun disconnect() {
        webSocketClient.disconnect()
        crdtEngine.reset()
        _remoteCursors.value = emptyMap()
    }

    /**
     * Schedule an auto-save 5 seconds after the last change.
     * Any new change resets the timer.
     */
    private fun scheduleAutoSave() {
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(5_000)
            saveDocument()
        }
    }

    /**
     * Insert text at position
     */
    fun insertText(blockId: String, text: String, position: Int) {
        val documentId = _uiState.value.currentDocumentId ?: return
        val userId = _uiState.value.currentUserId ?: return

        val operation = UpdateBlockContentOperation(
            operationId = UUID.randomUUID().toString(),
            boardId = documentId,
            blockId = blockId,
            content = text,
            position = position
        )

        // Apply locally via CRDT
        crdtEngine.applyOperation(operation)

        // Send to server
        viewModelScope.launch {
            webSocketClient.sendOperation(operation, userId)
        }

        scheduleAutoSave()
    }

    /**
     * Delete text range
     */
    fun deleteText(blockId: String, start: Int, end: Int) {
        val documentId = _uiState.value.currentDocumentId ?: return
        val userId = _uiState.value.currentUserId ?: return

        // For now, replace with empty string
        val operation = UpdateBlockContentOperation(
            operationId = UUID.randomUUID().toString(),
            boardId = documentId,
            blockId = blockId,
            content = "",
            position = start
        )

        crdtEngine.applyOperation(operation)

        viewModelScope.launch {
            webSocketClient.sendOperation(operation, userId)
        }
    }

    /**
     * Add new block
     */
    fun addBlock(block: ContentBlock, afterBlockId: String? = null) {
        val documentId = _uiState.value.currentDocumentId ?: return
        val userId = _uiState.value.currentUserId ?: return

        val operation = AddBlockOperation(
            operationId = UUID.randomUUID().toString(),
            boardId = documentId,
            block = block,
            afterBlockId = afterBlockId
        )

        crdtEngine.applyOperation(operation)

        viewModelScope.launch {
            webSocketClient.sendOperation(operation, userId)
        }

        scheduleAutoSave()
    }

    /**
     * Delete block
     */
    fun deleteBlock(blockId: String) {
        val documentId = _uiState.value.currentDocumentId ?: return
        val userId = _uiState.value.currentUserId ?: return

        val operation = DeleteBlockOperation(
            operationId = UUID.randomUUID().toString(),
            boardId = documentId,
            blockId = blockId
        )

        crdtEngine.applyOperation(operation)

        viewModelScope.launch {
            webSocketClient.sendOperation(operation, userId)
        }

        scheduleAutoSave()
    }

    /**
     * Update block formatting
     */
    fun updateFormatting(
        blockId: String,
        fontWeight: String? = null,
        fontStyle: String? = null,
        textDecoration: String? = null,
        fontSize: Int? = null,
        color: String? = null,
        textAlign: String? = null
    ) {
        val documentId = _uiState.value.currentDocumentId ?: return
        val userId = _uiState.value.currentUserId ?: return

        val operation = UpdateBlockFormattingOperation(
            operationId = UUID.randomUUID().toString(),
            boardId = documentId,
            blockId = blockId,
            fontWeight = fontWeight,
            fontStyle = fontStyle,
            textDecoration = textDecoration,
            fontSize = fontSize,
            color = color,
            textAlign = textAlign
        )

        crdtEngine.applyOperation(operation)

        viewModelScope.launch {
            webSocketClient.sendOperation(operation, userId)
        }
    }

    /**
     * Update block type
     */
    fun updateBlockType(blockId: String, newType: String) {
        val documentId = _uiState.value.currentDocumentId ?: return
        val userId = _uiState.value.currentUserId ?: return

        val operation = UpdateBlockTypeOperation(
            operationId = UUID.randomUUID().toString(),
            boardId = documentId,
            blockId = blockId,
            newType = newType
        )

        crdtEngine.applyOperation(operation)

        viewModelScope.launch {
            webSocketClient.sendOperation(operation, userId)
        }
    }

    /**
     * Toggle the checked state of a todo block
     */
    fun toggleTodo(blockId: String, isChecked: Boolean) {
        val documentId = _uiState.value.currentDocumentId ?: return
        val userId = _uiState.value.currentUserId ?: return

        val operation = ToggleTodoOperation(
            operationId = UUID.randomUUID().toString(),
            boardId = documentId,
            blockId = blockId,
            isChecked = isChecked
        )

        crdtEngine.applyOperation(operation)

        viewModelScope.launch {
            webSocketClient.sendOperation(operation, userId)
        }

        scheduleAutoSave()
    }

    /**
     * Update cursor position
     */
    fun updateCursorPosition(
        blockId: String?,
        position: Int,
        selectionStart: Int? = null,
        selectionEnd: Int? = null
    ) {
        val documentId = _uiState.value.currentDocumentId ?: return
        val userId = _uiState.value.currentUserId ?: return
        val userName = _uiState.value.currentUserName ?: return

        viewModelScope.launch {
            webSocketClient.sendCursorUpdate(
                documentId = documentId,
                userId = userId,
                userName = userName,
                blockId = blockId,
                position = position,
                selectionStart = selectionStart,
                selectionEnd = selectionEnd,
                color = getUserColor(userId).value.toString(16)
            )
        }
    }

    /**
     * Handle remote operation from another user
     */
    private fun handleRemoteOperation(broadcast: DocumentOperationBroadcast) {
        val currentUserId = _uiState.value.currentUserId

        // Don't apply our own operations again
        if (broadcast.userId == currentUserId) {
            return
        }

        // Transform operation against pending local operations
        val localOps = crdtEngine.getPendingOperations()
        val transformedOp = crdtEngine.transformOperation(broadcast.operation, localOps)

        // Apply transformed operation
        val applied = crdtEngine.applyOperation(transformedOp)

        if (applied) {
            Log.d(TAG, "Applied operation from ${broadcast.userName}")
        }
    }

    /**
     * Update remote cursor position
     */
    private fun updateRemoteCursor(cursorUpdate: CursorUpdateMessage) {
        val currentCursors = _remoteCursors.value.toMutableMap()

        val cursor = RemoteCursor(
            userId = cursorUpdate.userId,
            userName = cursorUpdate.userName,
            color = Color(android.graphics.Color.parseColor("#${cursorUpdate.color}")),
            position = CursorPosition(
                blockId = cursorUpdate.blockId,
                position = cursorUpdate.position,
                selectionStart = cursorUpdate.selectionStart,
                selectionEnd = cursorUpdate.selectionEnd
            ),
            lastUpdate = System.currentTimeMillis()
        )

        currentCursors[cursorUpdate.userId] = cursor
        _remoteCursors.value = currentCursors

        Log.d(TAG, "Updated cursor for ${cursorUpdate.userName} at block ${cursorUpdate.blockId}")
    }

    /**
     * Request full document state from server
     */
    fun requestDocumentState() {
        val documentId = _uiState.value.currentDocumentId ?: return

        viewModelScope.launch {
            webSocketClient.requestDocumentState(documentId)
        }
    }

    /**
     * Save the current document content via HTTP (fallback when WebSocket is offline)
     */
    fun saveDocument() {
        val documentId = _uiState.value.currentDocumentId ?: return
        val blocks = document.value?.blocks ?: return

        val title = blocks.firstOrNull { it.type == "h1" }?.content
            ?: blocks.firstOrNull()?.content
            ?: "Untitled"
        val content = blocks.joinToString("\n") { it.content }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                documentApiService.updateDocument(documentId, title = title, content = content)
                _uiState.update { it.copy(isSaving = false, shareSuccessMessage = "Document saved") }
            } catch (e: kotlinx.coroutines.CancellationException) {
                Log.d(TAG, "Auto-save cancelled (navigation)")
                _uiState.update { it.copy(isSaving = false) }
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Save failed", e)
                _uiState.update { it.copy(isSaving = false, error = "Save failed: ${e.message}") }
            }
        }
    }

    /**
     * Share document with another user by email
     */
    fun shareDocument(email: String, role: String) {
        val documentId = _uiState.value.currentDocumentId ?: return
        viewModelScope.launch {
            try {
                val roleStr = if (role.equals("editor", ignoreCase = true)) "editor" else "viewer"
                documentApiService.shareDocument(documentId, email, roleStr)
                _uiState.update { it.copy(shareSuccessMessage = "Shared with $email") }
            } catch (e: kotlinx.coroutines.CancellationException) {
                // ViewModel scope cancelled (user navigated away) — not an error
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Share document failed", e)
                _uiState.update { it.copy(error = "Share failed: ${e.message}") }
            }
        }
    }

    /**
     * Create a sub-page under this document and insert an inline reference block.
     * @param afterBlockId insert the reference block after this block (null = append to end)
     */
    fun createSubPage(parentId: String, title: String, afterBlockId: String? = null) {
        viewModelScope.launch {
            try {
                val subPage = documentApiService.createDocument(title = title, parentId = parentId)
                // Insert an inline subpage-link block so it appears inside the current document
                addBlock(
                    ContentBlock(
                        id = UUID.randomUUID().toString(),
                        type = "subpage",
                        content = "${subPage.id}||${subPage.title}"
                    ),
                    afterBlockId ?: document.value?.blocks?.lastOrNull()?.id
                )
                _uiState.update { it.copy(shareSuccessMessage = "Sub-page '${subPage.title}' created") }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to create sub-page: ${e.message}") }
            }
        }
    }

    /**
     * Clear error
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearShareSuccess() {
        _uiState.update { it.copy(shareSuccessMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        disconnect()
    }
}

/**
 * UI State for collaborative document
 */
data class CollaborativeDocumentUiState(
    val currentDocumentId: String? = null,
    val currentUserId: String? = null,
    val currentUserName: String? = null,
    val error: String? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val shareSuccessMessage: String? = null,
    /** Breadcrumb trail: list of (documentId, title) from root to current page */
    val breadcrumbs: List<Pair<String, String>> = emptyList()
)
