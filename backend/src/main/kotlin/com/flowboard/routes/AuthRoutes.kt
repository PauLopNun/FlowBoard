package com.flowboard.routes

import com.flowboard.data.models.LoginRequest
import com.flowboard.data.models.RegisterRequest
import com.flowboard.domain.AuthService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.authRoutes() {
    route("/auth") {
        
        post("/register") {
            val request = call.receive<RegisterRequest>()

            try {
                val response = AuthService.register(request)
                call.respond(HttpStatusCode.Created, response)
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest, mapOf(
                    "error" to "Registration failed",
                    "message" to (e.message ?: "Invalid request")
                ))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf(
                    "error" to "Internal server error",
                    "message" to e.message
                ))
            }
        }
        
        post("/login") {
            val request = call.receive<LoginRequest>()

            try {
                val response = AuthService.login(request)
                if (response != null) {
                    call.respond(HttpStatusCode.OK, response)
                } else {
                    call.respond(HttpStatusCode.Unauthorized, mapOf(
                        "error" to "Invalid credentials",
                        "message" to "Email or password is incorrect"
                    ))
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf(
                    "error" to "Internal server error",
                    "message" to e.message
                ))
            }
        }
        
        post("/logout") {
            // In a JWT-based system, logout is typically handled client-side
            // by removing the token. Here we can just return success.
            call.respond(HttpStatusCode.OK, mapOf("message" to "Logged out successfully"))
        }
    }
}