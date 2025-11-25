package com.flowboard.plugins

import com.flowboard.routes.authRoutes
import com.flowboard.routes.taskRoutes
import com.flowboard.routes.userRoutes
import com.flowboard.routes.projectRoutes
import com.flowboard.routes.webSocketRoutes
import com.flowboard.services.WebSocketManager
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    // Singleton WebSocketManager para toda la aplicación
    val webSocketManager = WebSocketManager()

    routing {
        get("/") {
            call.respondText("FlowBoard API is running!")
        }

        route("/api/v1") {
            authRoutes()
            taskRoutes()
            userRoutes()
            projectRoutes()
        }

        // Rutas WebSocket (sin prefijo /api/v1)
        webSocketRoutes(webSocketManager)
    }
}