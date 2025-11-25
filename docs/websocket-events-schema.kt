/**
 * MODELOS DE EVENTOS WEBSOCKET - COMPARTIDOS ENTRE BACKEND Y ANDROID
 *
 * Este archivo documenta el schema de eventos WebSocket usado por FlowBoard.
 * Debe ser implementado idénticamente en:
 * - Backend: backend/src/main/kotlin/com/flowboard/data/models/WebSocketMessage.kt
 * - Android: android/app/src/main/java/com/flowboard/data/remote/dto/WebSocketMessage.kt
 *
 * Versión: 1.0.0
 */

package com.flowboard.websocket

import kotlinx.serialization.Serializable
import kotlinx.datetime.LocalDateTime

/**
 * Mensaje base para comunicación WebSocket
 */
@Serializable
sealed class WebSocketMessage {
    abstract val type: String
    abstract val timestamp: LocalDateTime
}

// ============================================================================
// CLIENT → SERVER MESSAGES
// ============================================================================

/**
 * Cliente solicita unirse a un room/board específico
 */
@Serializable
data class JoinRoomMessage(
    override val type: String = "JOIN_ROOM",
    override val timestamp: LocalDateTime,
    val boardId: String,
    val userId: String
) : WebSocketMessage()

/**
 * Cliente solicita salir de un room
 */
@Serializable
data class LeaveRoomMessage(
    override val type: String = "LEAVE_ROOM",
    override val timestamp: LocalDateTime,
    val boardId: String
) : WebSocketMessage()

/**
 * Cliente notifica que está escribiendo/editando una tarea
 */
@Serializable
data class TypingIndicatorMessage(
    override val type: String = "TYPING_INDICATOR",
    override val timestamp: LocalDateTime,
    val boardId: String,
    val taskId: String?,
    val userId: String,
    val isTyping: Boolean
) : WebSocketMessage()

/**
 * Ping para mantener conexión activa
 */
@Serializable
data class PingMessage(
    override val type: String = "PING",
    override val timestamp: LocalDateTime
) : WebSocketMessage()

// ============================================================================
// SERVER → CLIENT MESSAGES
// ============================================================================

/**
 * Confirmación de unión exitosa a un room
 */
@Serializable
data class RoomJoinedMessage(
    override val type: String = "ROOM_JOINED",
    override val timestamp: LocalDateTime,
    val boardId: String,
    val activeUsers: List<UserPresenceInfo>
) : WebSocketMessage()

/**
 * Notificación de nueva tarea creada
 */
@Serializable
data class TaskCreatedMessage(
    override val type: String = "TASK_CREATED",
    override val timestamp: LocalDateTime,
    val boardId: String,
    val task: TaskSnapshot,
    val createdBy: UserPresenceInfo
) : WebSocketMessage()

/**
 * Notificación de tarea actualizada
 */
@Serializable
data class TaskUpdatedMessage(
    override val type: String = "TASK_UPDATED",
    override val timestamp: LocalDateTime,
    val boardId: String,
    val taskId: String,
    val changes: Map<String, String>, // Campo → nuevo valor
    val updatedBy: UserPresenceInfo
) : WebSocketMessage()

/**
 * Notificación de tarea eliminada
 */
@Serializable
data class TaskDeletedMessage(
    override val type: String = "TASK_DELETED",
    override val timestamp: LocalDateTime,
    val boardId: String,
    val taskId: String,
    val deletedBy: UserPresenceInfo
) : WebSocketMessage()

/**
 * Notificación de tarea movida (cambiada de board/columna)
 */
@Serializable
data class TaskMovedMessage(
    override val type: String = "TASK_MOVED",
    override val timestamp: LocalDateTime,
    val taskId: String,
    val fromBoardId: String,
    val toBoardId: String,
    val movedBy: UserPresenceInfo
) : WebSocketMessage()

/**
 * Notificación de usuario que se unió al board
 */
@Serializable
data class UserJoinedMessage(
    override val type: String = "USER_JOINED",
    override val timestamp: LocalDateTime,
    val boardId: String,
    val user: UserPresenceInfo
) : WebSocketMessage()

/**
 * Notificación de usuario que salió del board
 */
@Serializable
data class UserLeftMessage(
    override val type: String = "USER_LEFT",
    override val timestamp: LocalDateTime,
    val boardId: String,
    val userId: String
) : WebSocketMessage()

/**
 * Notificación de usuario escribiendo
 */
@Serializable
data class UserTypingMessage(
    override val type: String = "USER_TYPING",
    override val timestamp: LocalDateTime,
    val boardId: String,
    val taskId: String?,
    val user: UserPresenceInfo,
    val isTyping: Boolean
) : WebSocketMessage()

/**
 * Respuesta Pong al Ping
 */
@Serializable
data class PongMessage(
    override val type: String = "PONG",
    override val timestamp: LocalDateTime
) : WebSocketMessage()

/**
 * Mensaje de error
 */
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

/**
 * Información de presencia de usuario
 */
@Serializable
data class UserPresenceInfo(
    val userId: String,
    val username: String,
    val fullName: String,
    val profileImageUrl: String?,
    val isOnline: Boolean = true,
    val lastActivity: LocalDateTime
)

/**
 * Snapshot simplificado de una tarea (para transmisión eficiente)
 */
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
// ESTRATEGIA DE SERIALIZACIÓN
// ============================================================================

/**
 * Para deserializar mensajes polimórficos, usar:
 *
 * Backend (Ktor):
 * ```kotlin
 * val json = Json {
 *     ignoreUnknownKeys = true
 *     classDiscriminator = "type"
 * }
 *
 * webSocket("/ws/boards") {
 *     for (frame in incoming) {
 *         val text = (frame as Frame.Text).readText()
 *         val message = json.decodeFromString<WebSocketMessage>(text)
 *         // Handle message
 *     }
 * }
 * ```
 *
 * Android (Ktor Client):
 * ```kotlin
 * val json = Json {
 *     ignoreUnknownKeys = true
 *     classDiscriminator = "type"
 * }
 *
 * webSocket("ws://server/ws/boards") {
 *     for (frame in incoming) {
 *         val text = (frame as Frame.Text).readText()
 *         val message = json.decodeFromString<WebSocketMessage>(text)
 *         // Handle message
 *     }
 * }
 * ```
 */
