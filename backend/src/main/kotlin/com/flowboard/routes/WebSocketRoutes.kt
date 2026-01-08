package com.flowboard.routes

import com.flowboard.data.models.*
import com.flowboard.data.models.crdt.*
import com.flowboard.plugins.JwtConfig
import com.flowboard.services.WebSocketManager
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import org.slf4j.LoggerFactory

/**
 * Configura las rutas WebSocket para colaboración en tiempo real
 */
fun Route.webSocketRoutes(
    webSocketManager: WebSocketManager,
    documentService: com.flowboard.domain.DocumentService
) {
    val logger = LoggerFactory.getLogger("WebSocketRoutes")

    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Endpoint de WebSocket: ws://server/ws/boards
     * Requiere autenticación JWT mediante header Authorization
     */
    webSocket("/ws/boards") {
        logger.info("New WebSocket connection attempt")

        // 1. AUTENTICACIÓN: Extraer y validar JWT token
        val token = call.request.headers["Authorization"]
            ?.removePrefix("Bearer ")
            ?.trim()

        if (token.isNullOrEmpty()) {
            logger.warn("WebSocket connection rejected: No token provided")
            close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Authentication required"))
            return@webSocket
        }

        // Validar token JWT
        val decodedJWT = try {
            JwtConfig.verifier.verify(token)
        } catch (e: Exception) {
            logger.warn("WebSocket connection rejected: Invalid token - ${e.message}")
            close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Invalid token"))
            return@webSocket
        }

        val userId = decodedJWT.getClaim("userId").asString()
        val email = decodedJWT.getClaim("email").asString()

        if (userId.isNullOrEmpty()) {
            logger.warn("WebSocket connection rejected: No userId in token")
            close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Invalid token claims"))
            return@webSocket
        }

        logger.info("User $userId ($email) authenticated successfully")

        // 2. ESTADO: Variables para tracking de sesión
        var currentBoardId: String? = null
        var userPresence: UserPresenceInfo? = null

        try {
            // 3. LOOP DE MENSAJES: Escuchar mensajes del cliente
            for (frame in incoming) {
                when (frame) {
                    is Frame.Text -> {
                        val receivedText = frame.readText()
                        logger.debug("Received message from $userId: $receivedText")

                        try {
                            // Parse mensaje base para obtener el tipo
                            val messageType = json.decodeFromString<Map<String, String>>(receivedText)["type"]

                            when (messageType) {
                                // Cliente solicita unirse a un board
                                "JOIN_ROOM" -> {
                                    val message = json.decodeFromString<JoinRoomMessage>(receivedText)

                                    // TODO: Validar que el usuario tenga permiso para acceder al board
                                    // val hasAccess = boardService.canUserAccessBoard(userId, message.boardId)
                                    // if (!hasAccess) { ... }

                                    currentBoardId = message.boardId

                                    // Crear info de presencia del usuario
                                    // TODO: Obtener datos reales del usuario desde DB
                                    userPresence = UserPresenceInfo(
                                        userId = userId,
                                        username = email.substringBefore("@"),
                                        fullName = email.substringBefore("@"), // TODO: nombre real
                                        profileImageUrl = null,
                                        isOnline = true,
                                        lastActivity = Clock.System.now().toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault())
                                    )

                                    // Unir al room
                                    webSocketManager.joinRoom(
                                        session = this,
                                        boardId = message.boardId,
                                        user = userPresence!!
                                    )

                                    // Send the current document state to the user
                                    val document = documentService.getDocument(message.boardId)
                                    val documentStateMessage = DocumentStateMessage(
                                        timestamp = Clock.System.now().toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault()),
                                        document = document,
                                        activeUsers = webSocketManager.getActiveUsersInRoom(message.boardId)
                                    )
                                    send(Frame.Text(json.encodeToString(documentStateMessage)))


                                    logger.info("User $userId joined board ${message.boardId}")
                                }

                                // Cliente solicita salir del board
                                "LEAVE_ROOM" -> {
                                    val message = json.decodeFromString<LeaveRoomMessage>(receivedText)

                                    if (currentBoardId == message.boardId) {
                                        webSocketManager.leaveRoom(this)
                                        currentBoardId = null
                                        logger.info("User $userId left board ${message.boardId}")
                                    }
                                }

                                // Cliente envía indicador de escritura
                                "TYPING_INDICATOR" -> {
                                    val message = json.decodeFromString<TypingIndicatorMessage>(receivedText)

                                    if (currentBoardId == message.boardId && userPresence != null) {
                                        webSocketManager.broadcastToRoomExcept(
                                            boardId = message.boardId,
                                            exceptSession = this,
                                            message = UserTypingMessage(
                                                timestamp = Clock.System.now().toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault()),
                                                boardId = message.boardId,
                                                taskId = message.taskId,
                                                user = userPresence!!,
                                                isTyping = message.isTyping
                                            )
                                        )
                                    }
                                }

                                // Ping para keep-alive
                                "PING" -> {
                                    send(Frame.Text(json.encodeToString(
                                        PongMessage(timestamp = Clock.System.now().toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault()))
                                    )))
                                }

                                "DOCUMENT_OPERATION" -> {
                                    val message = json.decodeFromString<DocumentOperationMessage>(receivedText)
                                    if (currentBoardId == message.operation.boardId) {
                                        // Apply the operation to the document
                                        documentService.applyOperation(message.operation)
                                        // Broadcast the operation to other users
                                        webSocketManager.broadcastToRoomExcept(
                                            boardId = message.operation.boardId,
                                            exceptSession = this,
                                            message = message as WebSocketMessage
                                        )
                                    }
                                }

                                "CURSOR_UPDATE" -> {
                                    val message = json.decodeFromString<CursorUpdateMessage>(receivedText)
                                    // Extract boardId from documentId (assuming documentId contains boardId info)
                                    // For now, use currentBoardId
                                    if (currentBoardId != null) {
                                        webSocketManager.broadcastToRoomExcept(
                                            boardId = currentBoardId!!,
                                            exceptSession = this,
                                            message = message as WebSocketMessage
                                        )
                                    }
                                }

                                else -> {
                                    logger.warn("Unknown message type: $messageType from user $userId")
                                    send(Frame.Text(json.encodeToString(
                                        ErrorMessage(
                                            timestamp = Clock.System.now().toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault()),
                                            code = WebSocketErrorCodes.INVALID_MESSAGE,
                                            message = "Unknown message type: $messageType"
                                        )
                                    )))
                                }
                            }
                        } catch (e: Exception) {
                            logger.error("Error processing message from $userId: ${e.message}", e)
                            send(Frame.Text(json.encodeToString(
                                ErrorMessage(
                                    timestamp = Clock.System.now().toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault()),
                                    code = WebSocketErrorCodes.INTERNAL_ERROR,
                                    message = "Error processing message",
                                    details = e.message
                                )
                            )))
                        }
                    }

                    is Frame.Binary -> {
                        logger.warn("Binary frames not supported")
                    }

                    is Frame.Close -> {
                        logger.info("User $userId closed connection")
                    }

                    is Frame.Ping -> {
                        send(Frame.Pong(frame.data))
                    }

                    is Frame.Pong -> {
                        // Keep-alive response
                    }
                }
            }
        } catch (e: ClosedReceiveChannelException) {
            logger.info("User $userId connection closed normally")
        } catch (e: Exception) {
            logger.error("WebSocket error for user $userId: ${e.message}", e)
        } finally {
            // 4. LIMPIEZA: Asegurar que la sesión se elimine al desconectar
            webSocketManager.leaveRoom(this)
            logger.info("User $userId disconnected from WebSocket")
        }
    }

    /**
     * Endpoint REST para obtener estadísticas de WebSocket (debugging)
     */
    get("/ws/stats") {
        call.respond(
            HttpStatusCode.OK,
            mapOf(
                "activeSessions" to webSocketManager.getActiveSessions(),
                "activeRooms" to webSocketManager.getActiveRooms(),
                "timestamp" to Clock.System.now().toString()
            )
        )
    }
}
