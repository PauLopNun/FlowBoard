package com.flowboard.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.projectRoutes() {
    authenticate("auth-jwt") {
        route("/projects") {
            
            get {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized)
                
                // TODO: Implement project service
                call.respond(HttpStatusCode.OK, emptyList<Any>())
            }
            
            get("/{id}") {
                val projectId = call.parameters["id"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    "Missing project ID"
                )
                
                // TODO: Implement get project by id
                call.respond(HttpStatusCode.NotImplemented, "Project routes not implemented yet")
            }
            
            post {
                // TODO: Implement create project
                call.respond(HttpStatusCode.NotImplemented, "Project creation not implemented yet")
            }
            
            put("/{id}") {
                // TODO: Implement update project
                call.respond(HttpStatusCode.NotImplemented, "Project update not implemented yet")
            }
            
            delete("/{id}") {
                // TODO: Implement delete project
                call.respond(HttpStatusCode.NotImplemented, "Project deletion not implemented yet")
            }
        }
    }
}