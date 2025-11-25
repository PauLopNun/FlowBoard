package com.flowboard.services

import com.flowboard.data.models.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Gestiona todas las conexiones WebSocket activas y el broadcasting de mensajes
 *
 * Responsabilidades:
 * - Mantener registro de sesiones por board (rooms)
 * - Tracking de presencia de usuarios
 * - Broadcasting de mensajes a usuarios específicos o rooms completos
 * - Limpieza de sesiones desconectadas
 */
class WebSocketManager {
    private val logger = LoggerFactory.getLogger(WebSocketManager::class.java)

    // Board ID → Set de sesiones WebSocket conectadas
    private val rooms = ConcurrentHashMap<String, MutableSet<WebSocketSession>>()

    // Session → información de usuario y board
    private val sessionInfo = ConcurrentHashMap<WebSocketSession, UserSessionInfo>()

    // User ID → Set de sesiones (permite múltiples dispositivos por usuario)
    private val userSessions = ConcurrentHashMap<String, MutableSet<WebSocketSession>>()

    // JSON serializer
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = false
    }

    /**
     * Registra una sesión WebSocket y la une a un board específico
     */
    suspend fun joinRoom(
        session: WebSocketSession,
        boardId: String,
        user: UserPresenceInfo
    ) {
        // Registrar en room
        rooms.getOrPut(boardId) { mutableSetOf() }.add(session)

        // Guardar info de sesión
        sessionInfo[session] = UserSessionInfo(
            userId = user.userId,
            boardId = boardId,
            user = user
        )

        // Registrar sesión por usuario (multi-device)
        userSessions.getOrPut(user.userId) { mutableSetOf() }.add(session)

        logger.info("User ${user.username} joined board $boardId (${getRoomSize(boardId)} users in room)")

        // Notificar a otros usuarios en el room
        broadcastToRoomExcept(
            boardId = boardId,
            exceptSession = session,
            message = UserJoinedMessage(
                timestamp = Clock.System.now().toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault()),
                boardId = boardId,
                user = user
            )
        )

        // Enviar confirmación al usuario que se unió
        sendToSession(
            session = session,
            message = RoomJoinedMessage(
                timestamp = Clock.System.now().toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault()),
                boardId = boardId,
                activeUsers = getActiveUsersInRoom(boardId)
            )
        )
    }

    /**
     * Desconecta una sesión y limpia todos los registros
     */
    suspend fun leaveRoom(session: WebSocketSession) {
        val info = sessionInfo.remove(session) ?: return

        // Remover de room
        rooms[info.boardId]?.remove(session)
        if (rooms[info.boardId]?.isEmpty() == true) {
            rooms.remove(info.boardId)
        }

        // Remover de sesiones de usuario
        userSessions[info.userId]?.remove(session)
        if (userSessions[info.userId]?.isEmpty() == true) {
            userSessions.remove(info.userId)
        }

        logger.info("User ${info.user.username} left board ${info.boardId}")

        // Notificar a otros usuarios
        broadcastToRoom(
            boardId = info.boardId,
            message = UserLeftMessage(
                timestamp = Clock.System.now().toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault()),
                boardId = info.boardId,
                userId = info.userId
            )
        )
    }

    /**
     * Envía un mensaje a todas las sesiones en un board específico
     */
    suspend fun broadcastToRoom(boardId: String, message: WebSocketMessage) {
        val sessions = rooms[boardId] ?: return
        val jsonMessage = json.encodeToString(message)

        sessions.forEach { session ->
            sendToSession(session, jsonMessage)
        }

        logger.debug("Broadcast to room $boardId: ${message.type} (${sessions.size} recipients)")
    }

    /**
     * Envía un mensaje a todas las sesiones en un board EXCEPTO una específica
     * Útil para no enviar el mismo mensaje al usuario que lo originó
     */
    suspend fun broadcastToRoomExcept(
        boardId: String,
        exceptSession: WebSocketSession,
        message: WebSocketMessage
    ) {
        val sessions = rooms[boardId] ?: return
        val jsonMessage = json.encodeToString(message)

        sessions.filter { it != exceptSession }.forEach { session ->
            sendToSession(session, jsonMessage)
        }

        logger.debug("Broadcast to room $boardId (except one): ${message.type} (${sessions.size - 1} recipients)")
    }

    /**
     * Envía un mensaje a todas las sesiones de un usuario específico (todos sus dispositivos)
     */
    suspend fun sendToUser(userId: String, message: WebSocketMessage) {
        val sessions = userSessions[userId] ?: return
        val jsonMessage = json.encodeToString(message)

        sessions.forEach { session ->
            sendToSession(session, jsonMessage)
        }

        logger.debug("Sent to user $userId: ${message.type} (${sessions.size} devices)")
    }

    /**
     * Envía un mensaje a una sesión específica
     */
    private suspend fun sendToSession(session: WebSocketSession, message: WebSocketMessage) {
        sendToSession(session, json.encodeToString(message))
    }

    /**
     * Envía un mensaje (string JSON) a una sesión específica
     */
    private suspend fun sendToSession(session: WebSocketSession, jsonMessage: String) {
        try {
            session.send(Frame.Text(jsonMessage))
        } catch (e: ClosedSendChannelException) {
            logger.warn("Failed to send message: session closed")
            // La sesión está cerrada, limpiar
            leaveRoom(session)
        } catch (e: Exception) {
            logger.error("Error sending message to session", e)
        }
    }

    /**
     * Obtiene la lista de usuarios activos en un board
     */
    fun getActiveUsersInRoom(boardId: String): List<UserPresenceInfo> {
        val sessions = rooms[boardId] ?: return emptyList()
        return sessions.mapNotNull { sessionInfo[it]?.user }.distinctBy { it.userId }
    }

    /**
     * Obtiene el número de sesiones conectadas en un board
     */
    fun getRoomSize(boardId: String): Int {
        return rooms[boardId]?.size ?: 0
    }

    /**
     * Verifica si un usuario está conectado a un board específico
     */
    fun isUserInRoom(userId: String, boardId: String): Boolean {
        val sessions = userSessions[userId] ?: return false
        return sessions.any { sessionInfo[it]?.boardId == boardId }
    }

    /**
     * Obtiene todas las sesiones activas (para debugging/monitoring)
     */
    fun getActiveSessions(): Int {
        return sessionInfo.size
    }

    /**
     * Obtiene todos los rooms activos (para debugging/monitoring)
     */
    fun getActiveRooms(): Int {
        return rooms.size
    }

    /**
     * Limpia sesiones huérfanas (por seguridad)
     */
    fun cleanup() {
        val closedSessions = sessionInfo.keys.filter { session ->
            try {
                session.outgoing.isClosedForSend
            } catch (e: Exception) {
                true
            }
        }
        closedSessions.forEach { session ->
            sessionInfo.remove(session)
            rooms.values.forEach { it.remove(session) }
            val userId = sessionInfo[session]?.userId
            userId?.let { userSessions[it]?.remove(session) }
        }

        if (closedSessions.isNotEmpty()) {
            logger.info("Cleaned up ${closedSessions.size} closed sessions")
        }
    }
}

/**
 * Información de sesión de usuario
 */
data class UserSessionInfo(
    val userId: String,
    val boardId: String,
    val user: UserPresenceInfo
)
