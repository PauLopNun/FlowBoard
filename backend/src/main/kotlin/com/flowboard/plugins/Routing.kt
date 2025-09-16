package com.flowboard.plugins

import com.flowboard.routes.authRoutes
import com.flowboard.routes.taskRoutes
import com.flowboard.routes.userRoutes
import com.flowboard.routes.projectRoutes
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
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
    }
}