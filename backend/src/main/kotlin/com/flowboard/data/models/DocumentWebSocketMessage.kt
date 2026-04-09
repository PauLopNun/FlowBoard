package com.flowboard.data.models

import com.flowboard.data.models.crdt.CollaborativeDocument
import com.flowboard.data.models.crdt.DocumentOperation
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * WebSocket messages for document collaboration.
 * @SerialName values are the JSON discriminator values — must match the Android client exactly.
 */
@Serializable
sealed class DocumentWebSocketMessage {
    abstract val timestamp: LocalDateTime
}

@Serializable
@SerialName("JOIN_DOCUMENT")
data class JoinDocumentMessage(
    override val timestamp: LocalDateTime,
    val documentId: String,
    val userId: String,
    val userName: String
) : DocumentWebSocketMessage()

@Serializable
@SerialName("DOCUMENT_JOINED")
data class DocumentJoinedMessage(
    override val timestamp: LocalDateTime,
    val documentId: String,
    val document: CollaborativeDocument,
    val activeUsers: List<DocumentUserPresence>
) : DocumentWebSocketMessage()

@Serializable
@SerialName("DOCUMENT_OPERATION")
data class DocumentOperationMessage(
    override val timestamp: LocalDateTime,
    val operation: DocumentOperation,
    val userId: String
) : DocumentWebSocketMessage()

@Serializable
@SerialName("DOCUMENT_OPERATION_BROADCAST")
data class DocumentOperationBroadcast(
    override val timestamp: LocalDateTime,
    val operation: DocumentOperation,
    val userId: String,
    val userName: String
) : DocumentWebSocketMessage()

@Serializable
@SerialName("CURSOR_UPDATE")
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

@Serializable
@SerialName("USER_JOINED_DOCUMENT")
data class UserJoinedDocumentMessage(
    override val timestamp: LocalDateTime,
    val documentId: String,
    val user: DocumentUserPresence
) : DocumentWebSocketMessage()

@Serializable
@SerialName("USER_LEFT_DOCUMENT")
data class UserLeftDocumentMessage(
    override val timestamp: LocalDateTime,
    val documentId: String,
    val userId: String
) : DocumentWebSocketMessage()

@Serializable
@SerialName("REQUEST_DOCUMENT_STATE")
data class RequestDocumentStateMessage(
    override val timestamp: LocalDateTime,
    val documentId: String
) : DocumentWebSocketMessage()

@Serializable
@SerialName("DOCUMENT_STATE")
data class DocumentStateMessage(
    override val timestamp: LocalDateTime,
    val document: CollaborativeDocument,
    val activeUsers: List<DocumentUserPresence>
) : DocumentWebSocketMessage()

@Serializable
@SerialName("DOCUMENT_ERROR")
data class DocumentErrorMessage(
    override val timestamp: LocalDateTime,
    val error: String,
    val code: String
) : DocumentWebSocketMessage()

@Serializable
@SerialName("OPERATION_ACK")
data class OperationAckMessage(
    override val timestamp: LocalDateTime,
    val operationId: String,
    val success: Boolean,
    val error: String? = null
) : DocumentWebSocketMessage()

@Serializable
data class DocumentUserPresence(
    val userId: String,
    val userName: String,
    val email: String? = null,
    val color: String,
    val cursor: CursorPosition? = null,
    val isOnline: Boolean = true
)

@Serializable
data class CursorPosition(
    val blockId: String?,
    val position: Int,
    val selectionStart: Int? = null,
    val selectionEnd: Int? = null
)
