package com.flowboard.data.models

import com.flowboard.data.models.crdt.CollaborativeDocument
import com.flowboard.data.models.crdt.DocumentOperation
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

/**
 * WebSocket messages for document collaboration
 */
@Serializable
sealed class DocumentWebSocketMessage : WebSocketMessage() {
    abstract override val timestamp: LocalDateTime
    abstract override val type: String
}

/**
 * Client joins a document room
 */
@Serializable
data class JoinDocumentMessage(
    override val type: String = "JOIN_DOCUMENT",
    override val timestamp: LocalDateTime,
    val documentId: String,
    val userId: String,
    val userName: String
) : DocumentWebSocketMessage()

/**
 * Server confirms room joined
 */
@Serializable
data class DocumentJoinedMessage(
    override val type: String = "DOCUMENT_JOINED",
    override val timestamp: LocalDateTime,
    val documentId: String,
    val document: CollaborativeDocument,
    val activeUsers: List<DocumentUserPresence>
) : DocumentWebSocketMessage()

/**
 * Client sends an operation
 */
@Serializable
data class DocumentOperationMessage(
    override val type: String = "DOCUMENT_OPERATION",
    override val timestamp: LocalDateTime,
    val operation: DocumentOperation,
    val userId: String
) : DocumentWebSocketMessage()

/**
 * Server broadcasts operation to all clients
 */
@Serializable
data class DocumentOperationBroadcast(
    override val type: String = "DOCUMENT_OPERATION_BROADCAST",
    override val timestamp: LocalDateTime,
    val operation: DocumentOperation,
    val userId: String,
    val userName: String
) : DocumentWebSocketMessage()

/**
 * Client sends cursor position
 */
@Serializable
data class CursorUpdateMessage(
    override val type: String = "CURSOR_UPDATE",
    override val timestamp: LocalDateTime,
    val documentId: String,
    val userId: String,
    val userName: String,
    val blockId: String?,
    val position: Int,
    val selectionStart: Int? = null,
    val selectionEnd: Int? = null,
    val color: String
) : DocumentWebSocketMessage()

/**
 * User joined document
 */
@Serializable
data class UserJoinedDocumentMessage(
    override val type: String = "USER_JOINED_DOCUMENT",
    override val timestamp: LocalDateTime,
    val documentId: String,
    val user: DocumentUserPresence
) : DocumentWebSocketMessage()

/**
 * User left document
 */
@Serializable
data class UserLeftDocumentMessage(
    override val type: String = "USER_LEFT_DOCUMENT",
    override val timestamp: LocalDateTime,
    val documentId: String,
    val userId: String
) : DocumentWebSocketMessage()

/**
 * Request full document state
 */
@Serializable
data class RequestDocumentStateMessage(
    override val type: String = "REQUEST_DOCUMENT_STATE",
    override val timestamp: LocalDateTime,
    val documentId: String
) : DocumentWebSocketMessage()

/**
 * Server sends full document state
 */
@Serializable
data class DocumentStateMessage(
    override val type: String = "DOCUMENT_STATE",
    override val timestamp: LocalDateTime,
    val document: CollaborativeDocument,
    val activeUsers: List<DocumentUserPresence>
) : DocumentWebSocketMessage()

/**
 * Error message
 */
@Serializable
data class DocumentErrorMessage(
    override val type: String = "DOCUMENT_ERROR",
    override val timestamp: LocalDateTime,
    val error: String,
    val code: String
) : DocumentWebSocketMessage()

/**
 * Acknowledgment of operation
 */
@Serializable
data class OperationAckMessage(
    override val timestamp: LocalDateTime,
    val operationId: String,
    val success: Boolean,
    val error: String? = null
) : DocumentWebSocketMessage()

/**
 * Document user presence info with cursor
 * Note: Renamed to avoid conflict with UserPresenceInfo in WebSocketMessage
 */
@Serializable
data class DocumentUserPresence(
    val userId: String,
    val userName: String,
    val email: String? = null,
    val color: String,
    val cursor: CursorPosition? = null,
    val isOnline: Boolean = true
)

/**
 * Cursor position
 */
@Serializable
data class CursorPosition(
    val blockId: String?,
    val position: Int,
    val selectionStart: Int? = null,
    val selectionEnd: Int? = null
)
