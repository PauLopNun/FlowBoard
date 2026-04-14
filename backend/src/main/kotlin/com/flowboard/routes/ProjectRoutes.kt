package com.flowboard.routes

import com.flowboard.data.models.CreateProjectRequest
import com.flowboard.data.models.UpdateProjectRequest
import com.flowboard.domain.ProjectService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.projectRoutes(projectService: ProjectService) {
    authenticate("auth-jwt") {
        route("/projects") {

            get {
                val userId = call.principal<JWTPrincipal>()
                    ?.payload?.getClaim("userId")?.asString()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized)
                val projects = projectService.getUserProjects(userId)
                call.respond(HttpStatusCode.OK, projects)
            }

            get("/{id}") {
                val userId = call.principal<JWTPrincipal>()
                    ?.payload?.getClaim("userId")?.asString()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized)
                val projectId = call.parameters["id"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing project ID")
                val project = projectService.getProjectById(projectId, userId)
                    ?: return@get call.respond(HttpStatusCode.NotFound, "Project not found")
                call.respond(HttpStatusCode.OK, project)
            }

            post {
                val userId = call.principal<JWTPrincipal>()
                    ?.payload?.getClaim("userId")?.asString()
                    ?: return@post call.respond(HttpStatusCode.Unauthorized)
                val req = call.receive<CreateProjectRequest>()
                val project = projectService.createProject(
                    name = req.name,
                    description = req.description,
                    color = req.color,
                    ownerId = userId,
                    deadline = req.deadline
                )
                call.respond(HttpStatusCode.Created, project)
            }

            put("/{id}") {
                val userId = call.principal<JWTPrincipal>()
                    ?.payload?.getClaim("userId")?.asString()
                    ?: return@put call.respond(HttpStatusCode.Unauthorized)
                val projectId = call.parameters["id"]
                    ?: return@put call.respond(HttpStatusCode.BadRequest, "Missing project ID")
                val req = call.receive<UpdateProjectRequest>()
                val updated = projectService.updateProject(
                    projectId = projectId,
                    ownerId = userId,
                    name = req.name,
                    description = req.description,
                    color = req.color,
                    isActive = req.isActive,
                    deadline = req.deadline
                ) ?: return@put call.respond(HttpStatusCode.NotFound, "Project not found or not authorized")
                call.respond(HttpStatusCode.OK, updated)
            }

            delete("/{id}") {
                val userId = call.principal<JWTPrincipal>()
                    ?.payload?.getClaim("userId")?.asString()
                    ?: return@delete call.respond(HttpStatusCode.Unauthorized)
                val projectId = call.parameters["id"]
                    ?: return@delete call.respond(HttpStatusCode.BadRequest, "Missing project ID")
                val deleted = projectService.deleteProject(projectId, userId)
                if (deleted) call.respond(HttpStatusCode.NoContent)
                else call.respond(HttpStatusCode.NotFound, "Project not found or not authorized")
            }
        }
    }
}
