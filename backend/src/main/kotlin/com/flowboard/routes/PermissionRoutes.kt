package com.flowboard.routes

import com.flowboard.domain.PermissionService
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.permissionRoutes(permissionService: PermissionService) {
    route("/boards/{boardId}/invite") {
        post {
            val boardId = call.parameters["boardId"] ?: return@post call.respondText("Missing boardId", status = io.ktor.http.HttpStatusCode.BadRequest)
            val request = call.receive<InviteRequest>()
            permissionService.grantPermission(boardId, request.userId)
            call.respondText("User invited successfully")
        }
    }
}

@kotlinx.serialization.Serializable
data class InviteRequest(
    val userId: String
)
