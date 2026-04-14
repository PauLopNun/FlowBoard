package com.flowboard.plugins

import com.flowboard.routes.*
import com.flowboard.domain.*
import com.flowboard.services.WebSocketManager
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json

fun Application.configureRouting() {
    val webSocketManager = WebSocketManager()
    val documentService = InMemoryDocumentService(webSocketManager)
    val documentPersistenceService = DocumentPersistenceService()
    val notificationService = NotificationService()
    val chatService = ChatService()
    val permissionService = PermissionService()
    val taskService = TaskService(webSocketManager)
    val workspaceService = com.flowboard.domain.WorkspaceService()
    val projectService = ProjectService()

    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    routing {
        get("/") {
            call.respondText("FlowBoard API is running!")
        }

        route("/api/v1") {
            authRoutes()
            taskRoutes(taskService)
            userRoutes()
            projectRoutes(projectService)
            documentRoutes(documentPersistenceService, notificationService)
            notificationRoutes(notificationService)
            chatRoutes(chatService)
            permissionRoutes(permissionService)
            workspaceRoutes(workspaceService)
        }

        webSocketRoutes(webSocketManager, documentService)
        documentWebSocketRoutes(json, documentPersistenceService, documentService)
    }
}
