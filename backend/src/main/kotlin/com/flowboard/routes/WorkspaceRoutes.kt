package com.flowboard.routes

import com.flowboard.data.models.*
import com.flowboard.domain.WorkspaceService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.workspaceRoutes(workspaceService: WorkspaceService) {
    authenticate("auth-jwt") {
        route("/workspaces") {

            // Create a new workspace
            post {
                val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asString()
                    ?: return@post call.respond(HttpStatusCode.Unauthorized)

                val request = call.receive<CreateWorkspaceRequest>()
                if (request.name.isBlank()) {
                    return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Name is required"))
                }

                val workspace = workspaceService.createWorkspace(request.name, request.description, userId)
                call.respond(HttpStatusCode.Created, workspace)
            }

            // List workspaces I belong to
            get {
                val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asString()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized)

                val workspaces = workspaceService.getUserWorkspaces(userId)
                call.respond(HttpStatusCode.OK, workspaces)
            }

            // Join workspace by invite code
            post("/join") {
                val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asString()
                    ?: return@post call.respond(HttpStatusCode.Unauthorized)

                val request = call.receive<JoinWorkspaceRequest>()
                val workspace = workspaceService.joinWorkspace(request.inviteCode, userId)
                    ?: return@post call.respond(HttpStatusCode.NotFound, mapOf("error" to "Invalid invite code"))

                call.respond(HttpStatusCode.OK, workspace)
            }

            // Get workspace by ID
            get("/{id}") {
                val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asString()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized)

                val workspaceId = call.parameters["id"] ?: return@get
                val workspace = workspaceService.getWorkspaceById(workspaceId, userId)
                    ?: return@get call.respond(HttpStatusCode.NotFound, mapOf("error" to "Workspace not found"))

                call.respond(HttpStatusCode.OK, workspace)
            }

            // Remove a member
            delete("/{id}/members/{userId}") {
                val ownerId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asString()
                    ?: return@delete call.respond(HttpStatusCode.Unauthorized)

                val workspaceId = call.parameters["id"] ?: return@delete
                val targetUserId = call.parameters["userId"] ?: return@delete

                val removed = workspaceService.removeMember(workspaceId, ownerId, targetUserId)
                if (!removed) {
                    return@delete call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Not authorized"))
                }
                call.respond(HttpStatusCode.OK, mapOf("message" to "Member removed"))
            }

            // Delete workspace
            delete("/{id}") {
                val ownerId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asString()
                    ?: return@delete call.respond(HttpStatusCode.Unauthorized)

                val workspaceId = call.parameters["id"] ?: return@delete
                val deleted = workspaceService.deleteWorkspace(workspaceId, ownerId)
                if (!deleted) {
                    return@delete call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Not authorized"))
                }
                call.respond(HttpStatusCode.OK, mapOf("message" to "Workspace deleted"))
            }
        }
    }
}
