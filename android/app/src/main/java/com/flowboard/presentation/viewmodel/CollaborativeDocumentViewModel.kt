package com.flowboard.presentation.viewmodel

import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flowboard.data.crdt.CRDTEngine
import com.flowboard.data.models.*
import com.flowboard.data.models.crdt.*
import com.flowboard.data.remote.websocket.ConnectionState
import com.flowboard.data.remote.websocket.DocumentWebSocketClient
import com.flowboard.data.repository.AuthRepository
import com.flowboard.presentation.ui.components.RemoteCursor
import com.flowboard.presentation.ui.components.getUserColor
import dagger.hilt.android.lifecycle.HiltViewModel
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
    private val authRepository: AuthRepository
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
    val activeUsers: StateFlow<List<UserPresenceInfo>> = webSocketClient.activeUsers
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Remote cursors
    private val _remoteCursors = MutableStateFlow<Map<String, RemoteCursor>>(emptyMap())
    val remoteCursors: StateFlow<Map<String, RemoteCursor>> = _remoteCursors.asStateFlow()

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
            } catch (e: Exception) {
                Log.e(TAG, "Failed to connect to document", e)
                _uiState.update { it.copy(error = "Connection failed: ${e.message}") }
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
     * Clear error
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
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
    val isLoading: Boolean = false
)
