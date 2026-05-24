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

// [PSP - RA4.d] taskRoutes define todos los endpoints REST del recurso "tasks".
// Recibe una instancia de TaskService (inyectada desde Application.kt).
fun Route.taskRoutes(taskService: TaskService) {

    // [PSP - RA5.c] authenticate("auth-jwt") es el middleware de seguridad.
    // TODAS las rutas dentro de este bloque requieren un token JWT válido.
    // El plugin de Security.kt intercepta la petición y verifica la firma HMAC-SHA256.
    // Si el token falta, ha expirado o la firma es inválida → Ktor responde 401 Unauthorized
    // automáticamente, sin llegar al código de los handlers.
    // Equivalente Python PU5: if cmd not in ACL[role]: resp = "[DENIED]"
    authenticate("auth-jwt") {
        route("/tasks") {

            // [PSP - RA4.a] GET /tasks — leer todas las tareas del usuario autenticado
            // Protocolo HTTP/REST, verbo GET (solo lectura, sin cuerpo en la petición)
            get {
                // [PSP - RA5.c] Extraer el userId del PAYLOAD del JWT verificado.
                // call.principal<JWTPrincipal>() devuelve los datos del token si es válido.
                // No necesitamos consultar la BD para saber quién es — está en el propio token.
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized)

                // Filtros opcionales por query parameter: ?projectId=xxx  o  ?isCompleted=true
                val projectId   = call.request.queryParameters["projectId"]
                val isCompleted = call.request.queryParameters["isCompleted"]?.toBoolean()

                val tasks = when {
                    projectId   != null -> taskService.getTasksByProject(projectId)
                    isCompleted != null -> taskService.getTasksByStatus(isCompleted)
                    else                -> taskService.getAllTasksForUser(userId)
                }

                // [PSP - RA4.a] HTTP 200 OK — respuesta estándar para GET con éxito
                // El objeto 'tasks' se serializa automáticamente a JSON (ContentNegotiation)
                call.respond(HttpStatusCode.OK, tasks)
            }

            // [PSP - RA4.a] GET /tasks/{id} — leer una tarea por su identificador
            get("/{id}") {
                val taskId = call.parameters["id"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest, "Missing task ID"
                )
                val task = taskService.getTaskById(taskId)
                if (task != null) {
                    call.respond(HttpStatusCode.OK, task)         // 200 OK — tarea encontrada
                } else {
                    call.respond(HttpStatusCode.NotFound, "Task not found")  // 404 Not Found
                }
            }

            // [PSP - RA4.a] POST /tasks — crear una nueva tarea
            // El cliente envía el cuerpo en JSON; call.receive<>() lo deserializa automáticamente
            post {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString()
                    ?: return@post call.respond(HttpStatusCode.Unauthorized)

                // [PSP - RA4.b] call.receive<CreateTaskRequest>() deserializa el JSON del cuerpo.
                // ContentNegotiation (configurado en NetworkModule) gestiona la serialización/deserialización.
                // Si el JSON es inválido o faltan campos obligatorios → 400 Bad Request automático.
                val request = call.receive<CreateTaskRequest>()
                val task    = taskService.createTask(request, userId)

                // [PSP - RA4.a] HTTP 201 Created — código estándar para POST con recurso creado
                call.respond(HttpStatusCode.Created, task)
            }

            // [PSP - RA4.a] PUT /tasks/{id} — actualizar una tarea existente (reemplaza el recurso)
            put("/{id}") {
                val taskId = call.parameters["id"] ?: return@put call.respond(
                    HttpStatusCode.BadRequest, "Missing task ID"
                )
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString()
                    ?: return@put call.respond(HttpStatusCode.Unauthorized)

                val request = call.receive<UpdateTaskRequest>()
                val task    = taskService.updateTask(taskId, request, userId)

                if (task != null) {
                    call.respond(HttpStatusCode.OK, task)                         // 200 OK
                } else {
                    call.respond(HttpStatusCode.NotFound, "Task not found")       // 404 Not Found
                }
            }

            // [PSP - RA4.a] DELETE /tasks/{id} — eliminar una tarea
            delete("/{id}") {
                val taskId = call.parameters["id"] ?: return@delete call.respond(
                    HttpStatusCode.BadRequest, "Missing task ID"
                )
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString()
                    ?: return@delete call.respond(HttpStatusCode.Unauthorized)

                val deleted = taskService.deleteTask(taskId, userId)
                if (deleted) {
                    // [PSP - RA4.a] HTTP 204 No Content — estándar para DELETE con éxito (sin cuerpo de respuesta)
                    call.respond(HttpStatusCode.NoContent)
                } else {
                    call.respond(HttpStatusCode.NotFound, "Task not found")
                }
            }

            // [PSP - RA4.a] PATCH /tasks/{id}/toggle — actualización parcial (cambiar solo el estado)
            // PATCH = modificación parcial (a diferencia de PUT que reemplaza el recurso completo)
            patch("/{id}/toggle") {
                val taskId = call.parameters["id"] ?: return@patch call.respond(
                    HttpStatusCode.BadRequest, "Missing task ID"
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

            // [PSP - RA4.a] GET /tasks/events — filtrar tareas que son eventos por rango de fechas
            get("/events") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized)

                val startDate = call.request.queryParameters["startDate"]
                val endDate   = call.request.queryParameters["endDate"]

                if (startDate == null || endDate == null) {
                    return@get call.respond(
                        HttpStatusCode.BadRequest, "Start and end dates are required"
                    )
                }

                val events = taskService.getEventsBetweenDates(startDate, endDate, userId)
                call.respond(HttpStatusCode.OK, events)
            }
        }
    }
}
