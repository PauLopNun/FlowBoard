package com.flowboard.routes

import com.flowboard.data.models.*
import com.flowboard.data.models.crdt.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

/**
 * WebSocket routes for collaborative document editing
 */
fun Route.documentWebSocketRoutes(json: Json) {
    val documentSessions = ConcurrentHashMap<String, MutableMap<String, DocumentWebSocketSession>>()

    authenticate("auth-jwt") {
        webSocket("/ws/documents/{documentId}") {
            val documentId = call.parameters["documentId"] ?: run {
                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Missing document ID"))
                return@webSocket
            }

            val principal = call.principal<JWTPrincipal>()
            val userId = principal?.payload?.getClaim("userId")?.asString() ?: run {
                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Unauthorized"))
                return@webSocket
            }

            val userName = principal?.payload?.getClaim("username")?.asString() ?: "Anonymous"

            // Create session
            val session = DocumentWebSocketSession(
                id = userId,
                userName = userName,
                webSocketSession = this
            )

            // Add to document sessions
            documentSessions.computeIfAbsent(documentId) { ConcurrentHashMap() }[userId] = session

            // Get all sessions for this document
            val sessions = documentSessions[documentId]!!

            try {
                // Send join confirmation
                sendJoinedMessage(session, documentId, sessions, json)

                // Broadcast user joined
                broadcastToOthers(
                    sessions = sessions,
                    excludeUserId = userId,
                    message = UserJoinedDocumentMessage(
                        timestamp = Clock.System.now().toLocalDateTime(TimeZone.UTC),
                        documentId = documentId,
                        user = DocumentUserPresence(
                            userId = userId,
                            userName = userName,
                            color = getUserColor(userId),
                            isOnline = true
                        )
                    ),
                    json = json
                )

                // Listen for messages
                for (frame in incoming) {
                    if (frame is Frame.Text) {
                        val text = frame.readText()
                        handleIncomingMessage(
                            text = text,
                            session = session,
                            documentId = documentId,
                            sessions = sessions,
                            json = json
                        )
                    }
                }
            } catch (e: ClosedReceiveChannelException) {
                println("WebSocket closed for user $userId in document $documentId")
            } catch (e: Exception) {
                println("Error in WebSocket for user $userId: ${e.message}")
                e.printStackTrace()
            } finally {
                // Remove session
                sessions.remove(userId)
                if (sessions.isEmpty()) {
                    documentSessions.remove(documentId)
                }

                // Broadcast user left
                broadcastToAll(
                    sessions = sessions,
                    message = UserLeftDocumentMessage(
                        timestamp = Clock.System.now().toLocalDateTime(TimeZone.UTC),
                        documentId = documentId,
                        userId = userId
                    ),
                    json = json
                )
            }
        }
    }
}

/**
 * Send joined message with document state
 */
private suspend fun sendJoinedMessage(
    session: DocumentWebSocketSession,
    documentId: String,
    sessions: Map<String, DocumentWebSocketSession>,
    json: Json
) {
    // TODO: Load actual document from database
    val document = CollaborativeDocument(
        id = documentId,
        blocks = listOf(
            ContentBlock(
                id = "block-1",
                type = "h1",
                content = "Welcome to FlowBoard!"
            ),
            ContentBlock(
                id = "block-2",
                type = "p",
                content = "Start collaborating in real-time with your team."
            )
        )
    )

    val activeUsers = sessions.values.map { s ->
        DocumentUserPresence(
            userId = s.id,
            userName = s.userName,
            color = getUserColor(s.id),
            isOnline = true
        )
    }

    val message = DocumentJoinedMessage(
        timestamp = Clock.System.now().toLocalDateTime(TimeZone.UTC),
        documentId = documentId,
        document = document,
        activeUsers = activeUsers
    )

    session.send(json.encodeToString<DocumentWebSocketMessage>(message))
}

/**
 * Handle incoming message
 */
private suspend fun handleIncomingMessage(
    text: String,
    session: DocumentWebSocketSession,
    documentId: String,
    sessions: Map<String, DocumentWebSocketSession>,
    json: Json
) {
    try {
        val message = json.decodeFromString<DocumentWebSocketMessage>(text)

        when (message) {
            is DocumentOperationMessage -> {
                // Broadcast operation to all other users
                val broadcast = DocumentOperationBroadcast(
                    timestamp = Clock.System.now().toLocalDateTime(TimeZone.UTC),
                    operation = message.operation,
                    userId = message.userId,
                    userName = session.userName
                )

                broadcastToOthers(sessions, session.id, broadcast, json)

                // Send acknowledgment
                val ack = OperationAckMessage(
                    timestamp = Clock.System.now().toLocalDateTime(TimeZone.UTC),
                    operationId = message.operation.operationId,
                    success = true
                )
                session.send(json.encodeToString<DocumentWebSocketMessage>(ack))
            }

            is CursorUpdateMessage -> {
                // Broadcast cursor position to all other users
                broadcastToOthers(sessions, session.id, message, json)
            }

            is RequestDocumentStateMessage -> {
                // Send current document state
                // TODO: Load from database
                val document = CollaborativeDocument(
                    id = documentId,
                    blocks = emptyList()
                )

                val activeUsers = sessions.values.map { s ->
                    DocumentUserPresence(
                        userId = s.id,
                        userName = s.userName,
                        color = getUserColor(s.id),
                        isOnline = true
                    )
                }

                val stateMessage = DocumentStateMessage(
                    timestamp = Clock.System.now().toLocalDateTime(TimeZone.UTC),
                    document = document,
                    activeUsers = activeUsers
                )

                session.send(json.encodeToString<DocumentWebSocketMessage>(stateMessage))
            }

            else -> {
                println("Unknown message type: ${message::class.simpleName}")
            }
        }
    } catch (e: Exception) {
        println("Failed to handle message: ${e.message}")
        e.printStackTrace()

        // Send error to client
        val error = DocumentErrorMessage(
            timestamp = Clock.System.now().toLocalDateTime(TimeZone.UTC),
            error = "Failed to process message: ${e.message}",
            code = "PROCESSING_ERROR"
        )
        session.send(json.encodeToString<DocumentWebSocketMessage>(error))
    }
}

/**
 * Broadcast message to all sessions
 */
private suspend fun broadcastToAll(
    sessions: Map<String, DocumentWebSocketSession>,
    message: DocumentWebSocketMessage,
    json: Json
) {
    val serialized = json.encodeToString<DocumentWebSocketMessage>(message)
    sessions.values.forEach { session ->
        try {
            session.send(serialized)
        } catch (e: Exception) {
            println("Failed to send to ${session.userName}: ${e.message}")
        }
    }
}

/**
 * Broadcast message to all except one user
 */
private suspend fun broadcastToOthers(
    sessions: Map<String, DocumentWebSocketSession>,
    excludeUserId: String,
    message: DocumentWebSocketMessage,
    json: Json
) {
    val serialized = json.encodeToString<DocumentWebSocketMessage>(message)
    sessions.values
        .filter { it.id != excludeUserId }
        .forEach { session ->
            try {
                session.send(serialized)
            } catch (e: Exception) {
                println("Failed to send to ${session.userName}: ${e.message}")
            }
        }
}

/**
 * Get consistent color for user
 */
private fun getUserColor(userId: String): String {
    val colors = listOf(
        "EF4444", "F97316", "FBBF24", "10B981",
        "14B8A6", "3B82F6", "6366F1", "8B5CF6", "EC4899"
    )
    val hash = userId.hashCode()
    val index = (hash and Int.MAX_VALUE) % colors.size
    return colors[index]
}

/**
 * Document WebSocket session
 */
data class DocumentWebSocketSession(
    val id: String,
    val userName: String,
    val webSocketSession: DefaultWebSocketServerSession
) {
    suspend fun send(message: String) {
        webSocketSession.send(Frame.Text(message))
    }
}
