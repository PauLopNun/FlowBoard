package com.flowboard.plugins

import com.flowboard.routes.*
import com.flowboard.domain.*
import com.flowboard.services.WebSocketManager
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    // Singleton services para toda la aplicación
    val webSocketManager = WebSocketManager()
    val documentService = InMemoryDocumentService(webSocketManager)
    val documentPersistenceService = DocumentPersistenceService()
    val notificationService = NotificationService()
    val chatService = ChatService()
    val permissionService = PermissionService()
    val taskService = TaskService(webSocketManager)

    routing {
        get("/") {
            call.respondText("FlowBoard API is running!")
        }

        route("/api/v1") {
            // Auth routes
            authRoutes()

            // Task routes
            taskRoutes(taskService)

            // User routes
            userRoutes()

            // Project routes
            projectRoutes()

            // Document routes (with persistence)
            documentRoutes(documentPersistenceService, notificationService)

            // Notification routes
            notificationRoutes(notificationService)

            // Chat routes
            chatRoutes(chatService)

            // Permission routes (legacy)
            permissionRoutes(permissionService)
        }

        // Rutas WebSocket (sin prefijo /api/v1)
        webSocketRoutes(webSocketManager, documentService)
    }
}