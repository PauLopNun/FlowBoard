package com.flowboard.data.models

import kotlinx.serialization.Serializable
import kotlinx.datetime.LocalDateTime

/**
 * Mensajes WebSocket para comunicación en tiempo real
 * Idéntico al schema definido en docs/websocket-events-schema.kt
 */

@Serializable
sealed class WebSocketMessage {
    abstract val type: String
    abstract val timestamp: LocalDateTime
}

// ============================================================================
// CLIENT → SERVER MESSAGES
// ============================================================================

@Serializable
data class JoinRoomMessage(
    override val type: String = "JOIN_ROOM",
    override val timestamp: LocalDateTime,
    val boardId: String,
    val userId: String
) : WebSocketMessage()

@Serializable
data class LeaveRoomMessage(
    override val type: String = "LEAVE_ROOM",
    override val timestamp: LocalDateTime,
    val boardId: String
) : WebSocketMessage()

@Serializable
data class TypingIndicatorMessage(
    override val type: String = "TYPING_INDICATOR",
    override val timestamp: LocalDateTime,
    val boardId: String,
    val taskId: String?,
    val userId: String,
    val isTyping: Boolean
) : WebSocketMessage()

@Serializable
data class PingMessage(
    override val type: String = "PING",
    override val timestamp: LocalDateTime
) : WebSocketMessage()

// ============================================================================
// SERVER → CLIENT MESSAGES
// ============================================================================

@Serializable
data class RoomJoinedMessage(
    override val type: String = "ROOM_JOINED",
    override val timestamp: LocalDateTime,
    val boardId: String,
    val activeUsers: List<UserPresenceInfo>
) : WebSocketMessage()

@Serializable
data class TaskCreatedMessage(
    override val type: String = "TASK_CREATED",
    override val timestamp: LocalDateTime,
    val boardId: String,
    val task: TaskSnapshot,
    val createdBy: UserPresenceInfo
) : WebSocketMessage()

@Serializable
data class TaskUpdatedMessage(
    override val type: String = "TASK_UPDATED",
    override val timestamp: LocalDateTime,
    val boardId: String,
    val taskId: String,
    val changes: Map<String, String>,
    val updatedBy: UserPresenceInfo
) : WebSocketMessage()

@Serializable
data class TaskDeletedMessage(
    override val type: String = "TASK_DELETED",
    override val timestamp: LocalDateTime,
    val boardId: String,
    val taskId: String,
    val deletedBy: UserPresenceInfo
) : WebSocketMessage()

@Serializable
data class TaskMovedMessage(
    override val type: String = "TASK_MOVED",
    override val timestamp: LocalDateTime,
    val taskId: String,
    val fromBoardId: String,
    val toBoardId: String,
    val movedBy: UserPresenceInfo
) : WebSocketMessage()

@Serializable
data class UserJoinedMessage(
    override val type: String = "USER_JOINED",
    override val timestamp: LocalDateTime,
    val boardId: String,
    val user: UserPresenceInfo
) : WebSocketMessage()

@Serializable
data class UserLeftMessage(
    override val type: String = "USER_LEFT",
    override val timestamp: LocalDateTime,
    val boardId: String,
    val userId: String
) : WebSocketMessage()

@Serializable
data class UserTypingMessage(
    override val type: String = "USER_TYPING",
    override val timestamp: LocalDateTime,
    val boardId: String,
    val taskId: String?,
    val user: UserPresenceInfo,
    val isTyping: Boolean
) : WebSocketMessage()

@Serializable
data class PongMessage(
    override val type: String = "PONG",
    override val timestamp: LocalDateTime
) : WebSocketMessage()

@Serializable
data class ErrorMessage(
    override val type: String = "ERROR",
    override val timestamp: LocalDateTime,
    val code: String,
    val message: String,
    val details: String? = null
) : WebSocketMessage()

// ============================================================================
// DATA CLASSES AUXILIARES
// ============================================================================

@Serializable
data class UserPresenceInfo(
    val userId: String,
    val username: String,
    val fullName: String,
    val profileImageUrl: String?,
    val isOnline: Boolean = true,
    val lastActivity: LocalDateTime
)

@Serializable
data class TaskSnapshot(
    val id: String,
    val title: String,
    val description: String,
    val isCompleted: Boolean,
    val priority: String, // "HIGH", "MEDIUM", "LOW"
    val dueDate: LocalDateTime?,
    val assignedTo: String?,
    val projectId: String?,
    val tags: List<String>,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

// ============================================================================
// CÓDIGOS DE ERROR
// ============================================================================

object WebSocketErrorCodes {
    const val UNAUTHORIZED = "UNAUTHORIZED"
    const val INVALID_MESSAGE = "INVALID_MESSAGE"
    const val ROOM_NOT_FOUND = "ROOM_NOT_FOUND"
    const val PERMISSION_DENIED = "PERMISSION_DENIED"
    const val RATE_LIMIT_EXCEEDED = "RATE_LIMIT_EXCEEDED"
    const val INTERNAL_ERROR = "INTERNAL_ERROR"
}

// ============================================================================
// EXTENSION FUNCTIONS
// ============================================================================

/**
 * Convierte un Task completo a un TaskSnapshot para transmisión
 */
fun Task.toSnapshot(): TaskSnapshot {
    return TaskSnapshot(
        id = this.id,
        title = this.title,
        description = this.description,
        isCompleted = this.isCompleted,
        priority = this.priority.name,
        dueDate = this.dueDate,
        assignedTo = this.assignedTo,
        projectId = this.projectId,
        tags = this.tags,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt
    )
}
