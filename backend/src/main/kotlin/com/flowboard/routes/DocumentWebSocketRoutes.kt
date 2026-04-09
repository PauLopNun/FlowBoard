package com.flowboard.routes

import com.flowboard.data.models.*
import com.flowboard.data.models.crdt.*
import com.flowboard.domain.DocumentPersistenceService
import com.flowboard.domain.DocumentService
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap

/**
 * WebSocket routes for collaborative document editing.
 * Loads initial state from DB, maintains shared in-memory CRDT state,
 * and persists back to DB when all users leave.
 */
fun Route.documentWebSocketRoutes(
    json: Json,
    documentPersistenceService: DocumentPersistenceService,
    documentService: DocumentService
) {
    // Map of documentId → (userId → session)
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
            val userName = principal?.payload?.getClaim("username")?.asString()
                ?.takeIf { it.isNotBlank() }
                ?: principal?.payload?.getClaim("email")?.asString()?.substringBefore("@")
                ?: "Anonymous"

            val session = DocumentWebSocketSession(
                id = userId,
                userName = userName,
                webSocketSession = this
            )

            // Add to sessions map for this document
            documentSessions.computeIfAbsent(documentId) { ConcurrentHashMap() }[userId] = session
            val sessions = documentSessions[documentId]!!

            try {
                // Load document from DB if not yet in memory
                if (!documentService.hasDocument(documentId)) {
                    val dbDoc = try {
                        documentPersistenceService.getDocumentById(documentId, userId)
                    } catch (e: Exception) {
                        println("Could not load document $documentId from DB: ${e.message}")
                        null
                    }

                    val initialBlocks = if (dbDoc != null) {
                        parseBlocksFromContent(dbDoc.content, documentId)
                    } else {
                        listOf(
                            ContentBlock(id = "block-1", type = "h1", content = "Untitled Document"),
                            ContentBlock(id = "block-2", type = "p", content = "")
                        )
                    }
                    documentService.initializeDocument(documentId, initialBlocks)
                }

                // Send join confirmation with current document state
                sendJoinedMessage(session, documentId, sessions, documentService, json)

                // Broadcast that a new user joined
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

                // Listen for incoming messages
                for (frame in incoming) {
                    if (frame is Frame.Text) {
                        handleIncomingMessage(
                            text = frame.readText(),
                            session = session,
                            documentId = documentId,
                            sessions = sessions,
                            documentService = documentService,
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
                sessions.remove(userId)

                // If last user left, persist document to DB
                if (sessions.isEmpty()) {
                    documentSessions.remove(documentId)
                    persistDocumentToDb(documentId, documentService, documentPersistenceService)
                }

                // Notify remaining users
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
 * Serialize blocks to JSON string for DB storage
 */
private fun serializeBlocks(blocks: List<ContentBlock>, json: Json): String {
    return try {
        json.encodeToString(blocks)
    } catch (e: Exception) {
        "[]"
    }
}

/**
 * Parse blocks from DB content field.
 * Content may be a JSON array of ContentBlock or plain text.
 */
private fun parseBlocksFromContent(content: String, documentId: String): List<ContentBlock> {
    if (content.isBlank()) {
        return listOf(ContentBlock(id = "block-1", type = "p", content = ""))
    }
    return try {
        val trimmed = content.trim()
        if (trimmed.startsWith("[")) {
            Json { ignoreUnknownKeys = true }.decodeFromString<List<ContentBlock>>(trimmed)
        } else {
            // Legacy plain text content — wrap in a paragraph block
            val lines = content.split("\n").filter { it.isNotBlank() }
            if (lines.isEmpty()) {
                listOf(ContentBlock(id = "block-1", type = "p", content = content))
            } else {
                lines.mapIndexed { idx, line ->
                    ContentBlock(id = "block-${idx + 1}", type = "p", content = line)
                }
            }
        }
    } catch (e: Exception) {
        listOf(ContentBlock(id = "block-1", type = "p", content = content))
    }
}

/**
 * Persist document content back to DB (server-side, no permission check).
 */
private suspend fun persistDocumentToDb(
    documentId: String,
    documentService: DocumentService,
    documentPersistenceService: DocumentPersistenceService
) {
    try {
        val doc = documentService.getDocument(documentId)
        val json = Json { encodeDefaults = true }
        val contentJson = serializeBlocks(doc.blocks, json)
        val title = doc.blocks.firstOrNull { it.type == "h1" }?.content?.takeIf { it.isNotBlank() }
            ?: "Untitled Document"
        documentPersistenceService.saveDocumentContent(documentId, title, contentJson)
        println("Persisted document $documentId to DB (${doc.blocks.size} blocks)")
    } catch (e: Exception) {
        println("Failed to persist document $documentId: ${e.message}")
    }
}

/**
 * Send join confirmation with current document state
 */
private suspend fun sendJoinedMessage(
    session: DocumentWebSocketSession,
    documentId: String,
    sessions: Map<String, DocumentWebSocketSession>,
    documentService: DocumentService,
    json: Json
) {
    val document = documentService.getDocument(documentId)

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
 * Handle an incoming message from a client
 */
private suspend fun handleIncomingMessage(
    text: String,
    session: DocumentWebSocketSession,
    documentId: String,
    sessions: Map<String, DocumentWebSocketSession>,
    documentService: DocumentService,
    json: Json
) {
    try {
        val message = json.decodeFromString<DocumentWebSocketMessage>(text)

        when (message) {
            is DocumentOperationMessage -> {
                // Apply operation to the shared in-memory document
                val updatedDoc = documentService.applyOperation(message.operation)

                // Broadcast to all other users
                broadcastToOthers(
                    sessions, session.id,
                    DocumentOperationBroadcast(
                        timestamp = Clock.System.now().toLocalDateTime(TimeZone.UTC),
                        operation = message.operation,
                        userId = message.userId,
                        userName = session.userName
                    ),
                    json
                )

                // Acknowledge to sender
                session.send(
                    json.encodeToString<DocumentWebSocketMessage>(
                        OperationAckMessage(
                            timestamp = Clock.System.now().toLocalDateTime(TimeZone.UTC),
                            operationId = message.operation.operationId,
                            success = true
                        )
                    )
                )
            }

            is CursorUpdateMessage -> {
                // Broadcast cursor position to all other users
                broadcastToOthers(sessions, session.id, message, json)
            }

            is RequestDocumentStateMessage -> {
                val document = documentService.getDocument(documentId)
                val activeUsers = sessions.values.map { s ->
                    DocumentUserPresence(
                        userId = s.id,
                        userName = s.userName,
                        color = getUserColor(s.id),
                        isOnline = true
                    )
                }
                session.send(
                    json.encodeToString<DocumentWebSocketMessage>(
                        DocumentStateMessage(
                            timestamp = Clock.System.now().toLocalDateTime(TimeZone.UTC),
                            document = document,
                            activeUsers = activeUsers
                        )
                    )
                )
            }

            else -> {
                println("Unhandled message type: ${message::class.simpleName}")
            }
        }
    } catch (e: Exception) {
        println("Failed to handle message: ${e.message}")
        try {
            session.send(
                json.encodeToString<DocumentWebSocketMessage>(
                    DocumentErrorMessage(
                        timestamp = Clock.System.now().toLocalDateTime(TimeZone.UTC),
                        error = "Failed to process message: ${e.message}",
                        code = "PROCESSING_ERROR"
                    )
                )
            )
        } catch (_: Exception) {}
    }
}

private suspend fun broadcastToAll(
    sessions: Map<String, DocumentWebSocketSession>,
    message: DocumentWebSocketMessage,
    json: Json
) {
    val serialized = json.encodeToString<DocumentWebSocketMessage>(message)
    sessions.values.forEach { s ->
        try { s.send(serialized) } catch (_: Exception) {}
    }
}

private suspend fun broadcastToOthers(
    sessions: Map<String, DocumentWebSocketSession>,
    excludeUserId: String,
    message: DocumentWebSocketMessage,
    json: Json
) {
    val serialized = json.encodeToString<DocumentWebSocketMessage>(message)
    sessions.values.filter { it.id != excludeUserId }.forEach { s ->
        try { s.send(serialized) } catch (_: Exception) {}
    }
}

private fun getUserColor(userId: String): String {
    val colors = listOf(
        "EF4444", "F97316", "FBBF24", "10B981",
        "14B8A6", "3B82F6", "6366F1", "8B5CF6", "EC4899"
    )
    val index = (userId.hashCode() and Int.MAX_VALUE) % colors.size
    return colors[index]
}

data class DocumentWebSocketSession(
    val id: String,
    val userName: String,
    val webSocketSession: DefaultWebSocketServerSession
) {
    suspend fun send(message: String) {
        webSocketSession.send(Frame.Text(message))
    }
}
