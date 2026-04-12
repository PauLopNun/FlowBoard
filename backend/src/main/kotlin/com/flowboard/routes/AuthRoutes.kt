package com.flowboard.routes

import com.flowboard.data.models.GoogleSignInRequest
import com.flowboard.data.models.LoginRequest
import com.flowboard.data.models.RegisterRequest
import com.flowboard.domain.AuthService
import kotlinx.serialization.Serializable
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.authRoutes() {
    route("/auth") {
        post("/register") {
            try {
                val response = AuthService.register(call.receive<RegisterRequest>())
                call.respond(HttpStatusCode.Created, response)
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest,
                    mapOf("error" to (e.message ?: "Invalid request")))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError,
                    mapOf("error" to e.message))
            }
        }

        post("/login") {
            try {
                val response = AuthService.login(call.receive<LoginRequest>())
                if (response != null) {
                    call.respond(HttpStatusCode.OK, response)
                } else {
                    call.respond(HttpStatusCode.Unauthorized,
                        mapOf("error" to "Email or password is incorrect"))
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError,
                    mapOf("error" to e.message))
            }
        }

        post("/google") {
            try {
                val response = AuthService.googleSignIn(call.receive<GoogleSignInRequest>())
                call.respond(HttpStatusCode.OK, response)
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest,
                    mapOf("error" to (e.message ?: "Invalid Google token")))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError,
                    mapOf("error" to e.message))
            }
        }

        post("/logout") {
            call.respond(HttpStatusCode.OK, mapOf("message" to "Logged out successfully"))
        }

        post("/forgot-password") {
            try {
                val body = call.receive<ForgotPasswordBody>()
                AuthService.requestPasswordReset(body.email)
                call.respond(HttpStatusCode.OK, mapOf("message" to "If this email is registered, a reset code has been sent"))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.OK, mapOf("message" to "If this email is registered, a reset code has been sent"))
            }
        }

        post("/reset-password") {
            try {
                val body = call.receive<ResetPasswordBody>()
                val success = AuthService.confirmPasswordReset(body.email, body.code, body.newPassword)
                if (success) {
                    call.respond(HttpStatusCode.OK, mapOf("message" to "Password reset successfully"))
                } else {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid or expired code"))
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
            }
        }
    }
}

@Serializable
private data class ForgotPasswordBody(val email: String)

@Serializable
private data class ResetPasswordBody(
    val email: String,
    val code: String,
    val newPassword: String
)
