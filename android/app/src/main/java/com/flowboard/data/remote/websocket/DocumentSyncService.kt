package com.flowboard.data.remote.websocket

import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Servicio de sincronización de documentos en tiempo real usando WebSockets
 */
@Singleton
class DocumentSyncService @Inject constructor(
    private val client: HttpClient,
    private val json: Json
) {
    private var session: DefaultClientWebSocketSession? = null
    private val _documentUpdates = MutableSharedFlow<DocumentUpdate>(replay = 1)
    val documentUpdates: SharedFlow<DocumentUpdate> = _documentUpdates.asSharedFlow()

    private val _userPresence = MutableStateFlow<List<UserPresence>>(emptyList())
    val userPresence: StateFlow<List<UserPresence>> = _userPresence.asStateFlow()

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    /**
     * Conectar al documento para sincronización
     */
    suspend fun connectToDocument(
        documentId: String,
        userId: String,
        token: String
    ) {
        try {
            _connectionState.value = ConnectionState.Connecting

            session = client.webSocketSession(
                urlString = "wss://your-server.com/ws/document/$documentId?userId=$userId&token=$token"
            )

            _connectionState.value = ConnectionState.Connected

            // Escuchar mensajes del servidor
            session?.incoming?.receiveAsFlow()?.collect { frame ->
                if (frame is Frame.Text) {
                    val message = frame.readText()
                    handleIncomingMessage(message)
                }
            }

        } catch (e: Exception) {
            _connectionState.value = ConnectionState.Error("Connection failed: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * Desconectar del documento
     */
    suspend fun disconnect() {
        session?.close()
        session = null
        _connectionState.value = ConnectionState.Disconnected
    }

    /**
     * Enviar actualización de contenido
     */
    suspend fun sendContentUpdate(
        documentId: String,
        content: String,
        cursorPosition: Int
    ) {
        val message = DocumentMessage.ContentUpdate(
            documentId = documentId,
            content = content,
            timestamp = System.currentTimeMillis(),
            cursorPosition = cursorPosition
        )
        sendMessage(message)
    }

    /**
     * Enviar posición del cursor
     */
    suspend fun sendCursorPosition(
        documentId: String,
        position: Int
    ) {
        val message = DocumentMessage.CursorPosition(
            documentId = documentId,
            position = position,
            timestamp = System.currentTimeMillis()
        )
        sendMessage(message)
    }

    /**
     * Enviar presencia de usuario
     */
    suspend fun sendPresence(
        documentId: String,
        isActive: Boolean
    ) {
        val message = DocumentMessage.UserPresence(
            documentId = documentId,
            isActive = isActive,
            timestamp = System.currentTimeMillis()
        )
        sendMessage(message)
    }

    /**
     * Invitar usuario a colaborar
     */
    suspend fun inviteUser(
        documentId: String,
        userIdOrEmail: String,
        permission: String
    ) {
        val message = DocumentMessage.InviteUser(
            documentId = documentId,
            userIdOrEmail = userIdOrEmail,
            permission = permission
        )
        sendMessage(message)
    }

    /**
     * Manejar mensajes entrantes del servidor
     */
    private suspend fun handleIncomingMessage(message: String) {
        try {
            val response = json.decodeFromString<DocumentMessage>(message)

            when (response) {
                is DocumentMessage.ContentUpdate -> {
                    _documentUpdates.emit(
                        DocumentUpdate.ContentChanged(
                            content = response.content,
                            timestamp = response.timestamp
                        )
                    )
                }
                is DocumentMessage.CursorPosition -> {
                    _documentUpdates.emit(
                        DocumentUpdate.CursorMoved(
                            userId = response.documentId, // En producción usar el userId real
                            position = response.position
                        )
                    )
                }
                is DocumentMessage.UserPresence -> {
                    // Actualizar lista de usuarios presentes
                    // En producción, el servidor enviaría la lista completa
                }
                is DocumentMessage.InviteUser -> {
                    // Manejar invitación recibida
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Enviar mensaje al servidor
     */
    private suspend fun sendMessage(message: DocumentMessage) {
        try {
            val jsonMessage = json.encodeToString(message)
            session?.send(Frame.Text(jsonMessage))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}


/**
 * Tipos de actualizaciones de documento
 */
sealed class DocumentUpdate {
    data class ContentChanged(
        val content: String,
        val timestamp: Long
    ) : DocumentUpdate()

    data class CursorMoved(
        val userId: String,
        val position: Int
    ) : DocumentUpdate()

    data class UserJoined(
        val userId: String,
        val username: String
    ) : DocumentUpdate()

    data class UserLeft(
        val userId: String
    ) : DocumentUpdate()
}

/**
 * Presencia de usuario
 */
@Serializable
data class UserPresence(
    val userId: String,
    val username: String,
    val isActive: Boolean,
    val lastSeen: Long
)

/**
 * Mensajes de WebSocket
 */
@Serializable
sealed class DocumentMessage {
    @Serializable
    data class ContentUpdate(
        val documentId: String,
        val content: String,
        val timestamp: Long,
        val cursorPosition: Int
    ) : DocumentMessage()

    @Serializable
    data class CursorPosition(
        val documentId: String,
        val position: Int,
        val timestamp: Long
    ) : DocumentMessage()

    @Serializable
    data class UserPresence(
        val documentId: String,
        val isActive: Boolean,
        val timestamp: Long
    ) : DocumentMessage()

    @Serializable
    data class InviteUser(
        val documentId: String,
        val userIdOrEmail: String,
        val permission: String
    ) : DocumentMessage()
}
