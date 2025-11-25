package com.flowboard.routes

import com.flowboard.data.models.CreateTaskRequest
import com.flowboard.data.models.UpdateTaskRequest
import com.flowboard.domain.TaskService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.*

fun Route.taskRoutes(taskService: TaskService) {
    authenticate("auth-jwt") {
        route("/tasks") {

            get {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized)

                val projectId = call.request.queryParameters["projectId"]
                val isCompleted = call.request.queryParameters["isCompleted"]?.toBoolean()

                val tasks = when {
                    projectId != null -> taskService.getTasksByProject(projectId)
                    isCompleted != null -> taskService.getTasksByStatus(isCompleted)
                    else -> taskService.getAllTasksForUser(userId)
                }

                call.respond(HttpStatusCode.OK, tasks)
            }

            get("/{id}") {
                val taskId = call.parameters["id"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    "Missing task ID"
                )

                val task = taskService.getTaskById(taskId)
                if (task != null) {
                    call.respond(HttpStatusCode.OK, task)
                } else {
                    call.respond(HttpStatusCode.NotFound, "Task not found")
                }
            }
            
            post {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString()
                    ?: return@post call.respond(HttpStatusCode.Unauthorized)
                
                val request = call.receive<CreateTaskRequest>()
                val task = taskService.createTask(request, userId)
                call.respond(HttpStatusCode.Created, task)
            }

            put("/{id}") {
                val taskId = call.parameters["id"] ?: return@put call.respond(
                    HttpStatusCode.BadRequest,
                    "Missing task ID"
                )

                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString()
                    ?: return@put call.respond(HttpStatusCode.Unauthorized)

                val request = call.receive<UpdateTaskRequest>()
                val task = taskService.updateTask(taskId, request, userId)
                
                if (task != null) {
                    call.respond(HttpStatusCode.OK, task)
                } else {
                    call.respond(HttpStatusCode.NotFound, "Task not found")
                }
            }
            
            delete("/{id}") {
                val taskId = call.parameters["id"] ?: return@delete call.respond(
                    HttpStatusCode.BadRequest, 
                    "Missing task ID"
                )
                
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString()
                    ?: return@delete call.respond(HttpStatusCode.Unauthorized)
                
                val deleted = taskService.deleteTask(taskId, userId)
                if (deleted) {
                    call.respond(HttpStatusCode.NoContent)
                } else {
                    call.respond(HttpStatusCode.NotFound, "Task not found")
                }
            }

            patch("/{id}/toggle") {
                val taskId = call.parameters["id"] ?: return@patch call.respond(
                    HttpStatusCode.BadRequest,
                    "Missing task ID"
                )

                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString()
                    ?: return@patch call.respond(HttpStatusCode.Unauthorized)

                val task = taskService.toggleTaskStatus(taskId, userId)
                if (task != null) {
                    call.respond(HttpStatusCode.OK, task)
                } else {
                    call.respond(HttpStatusCode.NotFound, "Task not found")
                }
            }
            
            get("/events") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized)
                
                val startDate = call.request.queryParameters["startDate"]
                val endDate = call.request.queryParameters["endDate"]
                
                if (startDate == null || endDate == null) {
                    return@get call.respond(
                        HttpStatusCode.BadRequest, 
                        "Start and end dates are required"
                    )
                }
                
                val events = taskService.getEventsBetweenDates(startDate, endDate, userId)
                call.respond(HttpStatusCode.OK, events)
            }
        }
    }
}