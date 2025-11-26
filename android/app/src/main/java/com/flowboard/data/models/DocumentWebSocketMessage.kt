package com.flowboard.data.models

import com.flowboard.data.models.crdt.CollaborativeDocument
import com.flowboard.data.models.crdt.DocumentOperation
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

/**
 * WebSocket messages for document collaboration
 */
@Serializable
sealed class DocumentWebSocketMessage {
    abstract val timestamp: LocalDateTime
}

/**
 * Client joins a document room
 */
@Serializable
data class JoinDocumentMessage(
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
    override val timestamp: LocalDateTime,
    val documentId: String,
    val document: CollaborativeDocument,
    val activeUsers: List<UserPresenceInfo>
) : DocumentWebSocketMessage()

/**
 * Client sends an operation
 */
@Serializable
data class DocumentOperationMessage(
    override val timestamp: LocalDateTime,
    val operation: DocumentOperation,
    val userId: String
) : DocumentWebSocketMessage()

/**
 * Server broadcasts operation to all clients
 */
@Serializable
data class DocumentOperationBroadcast(
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
    override val timestamp: LocalDateTime,
    val documentId: String,
    val user: UserPresenceInfo
) : DocumentWebSocketMessage()

/**
 * User left document
 */
@Serializable
data class UserLeftDocumentMessage(
    override val timestamp: LocalDateTime,
    val documentId: String,
    val userId: String
) : DocumentWebSocketMessage()

/**
 * Request full document state
 */
@Serializable
data class RequestDocumentStateMessage(
    override val timestamp: LocalDateTime,
    val documentId: String
) : DocumentWebSocketMessage()

/**
 * Server sends full document state
 */
@Serializable
data class DocumentStateMessage(
    override val timestamp: LocalDateTime,
    val document: CollaborativeDocument,
    val activeUsers: List<UserPresenceInfo>
) : DocumentWebSocketMessage()

/**
 * Error message
 */
@Serializable
data class DocumentErrorMessage(
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
 * User presence info with cursor
 */
@Serializable
data class UserPresenceInfo(
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
