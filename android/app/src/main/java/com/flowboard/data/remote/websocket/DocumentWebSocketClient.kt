package com.flowboard.data.remote.websocket

import android.util.Log
import com.flowboard.data.models.*
import com.flowboard.data.models.crdt.CollaborativeDocument
import com.flowboard.data.models.crdt.DocumentOperation
import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "DocumentWebSocketClient"

@Singleton
class DocumentWebSocketClient @Inject constructor(
    private val client: HttpClient,
    private val json: Json
) {
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _incomingOperations = MutableSharedFlow<DocumentOperationBroadcast>()
    val incomingOperations: SharedFlow<DocumentOperationBroadcast> = _incomingOperations.asSharedFlow()

    private val _documentState = MutableStateFlow<CollaborativeDocument?>(null)
    val documentState: StateFlow<CollaborativeDocument?> = _documentState.asStateFlow()

    private val _activeUsers = MutableStateFlow<List<DocumentUserPresence>>(emptyList())
    val activeUsers: StateFlow<List<DocumentUserPresence>> = _activeUsers.asStateFlow()

    private val _cursorUpdates = MutableSharedFlow<CursorUpdateMessage>()
    val cursorUpdates: SharedFlow<CursorUpdateMessage> = _cursorUpdates.asSharedFlow()

    private val _errors = MutableSharedFlow<String>()
    val errors: SharedFlow<String> = _errors.asSharedFlow()

    private var session: DefaultClientWebSocketSession? = null
    private var connectionJob: Job? = null
    private var currentDocumentId: String? = null
    private var currentUserId: String? = null
    private var currentUserName: String? = null

    /**
     * Connect to document WebSocket
     */
    suspend fun connect(
        documentId: String,
        userId: String,
        userName: String,
        token: String,
        host: String = "localhost",
        port: Int = 8080
    ) {
        if (session != null && session?.isActive == true) {
            Log.d(TAG, "Already connected")
            return
        }

        currentDocumentId = documentId
        currentUserId = userId
        currentUserName = userName

        _connectionState.value = ConnectionState.Connecting

        connectionJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                client.webSocket(
                    method = HttpMethod.Get,
                    host = host,
                    port = port,
                    path = "/ws/documents/$documentId",
                    request = {
                        header("Authorization", "Bearer $token")
                    }
                ) {
                    session = this
                    _connectionState.value = ConnectionState.Connected

                    Log.d(TAG, "Connected to document WebSocket: $documentId")

                    // Send join message
                    sendJoinMessage(documentId, userId, userName)

                    // Listen for messages
                    try {
                        for (frame in incoming) {
                            if (frame is Frame.Text) {
                                val text = frame.readText()
                                handleIncomingMessage(text)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error receiving messages", e)
                        _errors.emit("Connection error: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "WebSocket connection failed", e)
                _connectionState.value = ConnectionState.Error(e.message ?: "Connection failed")
                _errors.emit("Failed to connect: ${e.message}")
            } finally {
                session = null
                _connectionState.value = ConnectionState.Disconnected
                Log.d(TAG, "Disconnected from document WebSocket")
            }
        }
    }

    /**
     * Send join message
     */
    private suspend fun sendJoinMessage(documentId: String, userId: String, userName: String) {
        val message = JoinDocumentMessage(
            timestamp = Clock.System.now().toLocalDateTime(TimeZone.UTC),
            documentId = documentId,
            userId = userId,
            userName = userName
        )
        sendMessage(message)
    }

    /**
     * Send operation to server
     */
    suspend fun sendOperation(operation: DocumentOperation, userId: String) {
        val message = DocumentOperationMessage(
            timestamp = Clock.System.now().toLocalDateTime(TimeZone.UTC),
            operation = operation,
            userId = userId
        )
        sendMessage(message)
    }

    /**
     * Send cursor update
     */
    suspend fun sendCursorUpdate(
        documentId: String,
        userId: String,
        userName: String,
        blockId: String?,
        position: Int,
        selectionStart: Int? = null,
        selectionEnd: Int? = null,
        color: String
    ) {
        val message = CursorUpdateMessage(
            timestamp = Clock.System.now().toLocalDateTime(TimeZone.UTC),
            documentId = documentId,
            userId = userId,
            userName = userName,
            blockId = blockId,
            position = position,
            selectionStart = selectionStart,
            selectionEnd = selectionEnd,
            color = color
        )
        sendMessage(message)
    }

    /**
     * Request full document state
     */
    suspend fun requestDocumentState(documentId: String) {
        val message = RequestDocumentStateMessage(
            timestamp = Clock.System.now().toLocalDateTime(TimeZone.UTC),
            documentId = documentId
        )
        sendMessage(message)
    }

    /**
     * Send message to server
     */
    private suspend fun sendMessage(message: DocumentWebSocketMessage) {
        try {
            val json = json.encodeToString<DocumentWebSocketMessage>(message)
            session?.send(Frame.Text(json))
            Log.d(TAG, "Sent message: ${message::class.simpleName}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send message", e)
            _errors.emit("Failed to send message: ${e.message}")
        }
    }

    /**
     * Handle incoming message
     */
    private suspend fun handleIncomingMessage(text: String) {
        try {
            val message = json.decodeFromString<DocumentWebSocketMessage>(text)
            Log.d(TAG, "Received message: ${message::class.simpleName}")

            when (message) {
                is DocumentJoinedMessage -> {
                    _documentState.value = message.document
                    _activeUsers.value = message.activeUsers
                    Log.d(TAG, "Joined document with ${message.activeUsers.size} users")
                }

                is DocumentOperationBroadcast -> {
                    // Only emit if it's not from current user
                    if (message.userId != currentUserId) {
                        _incomingOperations.emit(message)
                        Log.d(TAG, "Received operation from ${message.userName}")
                    }
                }

                is CursorUpdateMessage -> {
                    // Only emit if it's not from current user
                    if (message.userId != currentUserId) {
                        _cursorUpdates.emit(message)
                    }
                }

                is UserJoinedDocumentMessage -> {
                    val users = _activeUsers.value.toMutableList()
                    if (!users.any { it.userId == message.user.userId }) {
                        users.add(message.user)
                        _activeUsers.value = users
                    }
                    Log.d(TAG, "User joined: ${message.user.userName}")
                }

                is UserLeftDocumentMessage -> {
                    val users = _activeUsers.value.filter { it.userId != message.userId }
                    _activeUsers.value = users
                    Log.d(TAG, "User left: ${message.userId}")
                }

                is DocumentStateMessage -> {
                    _documentState.value = message.document
                    _activeUsers.value = message.activeUsers
                    Log.d(TAG, "Received document state update")
                }

                is DocumentErrorMessage -> {
                    Log.e(TAG, "Server error: ${message.error}")
                    _errors.emit(message.error)
                }

                is OperationAckMessage -> {
                    if (!message.success) {
                        Log.e(TAG, "Operation failed: ${message.error}")
                        _errors.emit("Operation failed: ${message.error}")
                    }
                }

                else -> {
                    Log.w(TAG, "Unknown message type: ${message::class.simpleName}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse message: $text", e)
            _errors.emit("Failed to parse message: ${e.message}")
        }
    }

    /**
     * Disconnect from WebSocket
     */
    fun disconnect() {
        try {
            connectionJob?.cancel()
            session?.cancel()
            session = null
            currentDocumentId = null
            currentUserId = null
            currentUserName = null
            _connectionState.value = ConnectionState.Disconnected
            Log.d(TAG, "Disconnected")
        } catch (e: Exception) {
            Log.e(TAG, "Error disconnecting", e)
        }
    }

    /**
     * Check if connected
     */
    fun isConnected(): Boolean {
        return session?.isActive == true && _connectionState.value is ConnectionState.Connected
    }
}

/**
 * Connection state
 */
sealed class ConnectionState {
    object Disconnected : ConnectionState()
    object Connecting : ConnectionState()
    object Connected : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}