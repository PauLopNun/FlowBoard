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

        // Request a password reset — sends a 6-digit OTP to the user's email
        post("/forgot-password") {
            @Serializable data class ForgotPasswordRequest(val email: String)
            try {
                val req = call.receive<ForgotPasswordRequest>()
                AuthService.requestPasswordReset(req.email)
                // Always return 200 — no user enumeration
                call.respond(HttpStatusCode.OK,
                    mapOf("message" to "If that email is registered you will receive a reset code"))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
            }
        }

        // Confirm password reset with OTP + new password
        post("/reset-password") {
            @Serializable data class ResetPasswordRequest(val email: String, val code: String, val newPassword: String)
            try {
                val req = call.receive<ResetPasswordRequest>()
                val success = AuthService.confirmPasswordReset(req.email, req.code, req.newPassword)
                if (success) {
                    call.respond(HttpStatusCode.OK, mapOf("message" to "Password updated successfully"))
                } else {
                    call.respond(HttpStatusCode.BadRequest,
                        mapOf("error" to "Invalid or expired reset code"))
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
            }
        }
    }
}
