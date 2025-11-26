package com.flowboard.routes

import com.flowboard.domain.AuthService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

@Serializable
data class UpdateProfileRequest(
    val fullName: String? = null,
    val profileImageUrl: String? = null
)

@Serializable
data class UpdatePasswordRequest(
    val oldPassword: String,
    val newPassword: String
)

fun Route.userRoutes() {
    authenticate("auth-jwt") {
        route("/users") {

            get("/me") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized)

                val user = AuthService.getUserById(userId)
                if (user != null) {
                    call.respond(HttpStatusCode.OK, user)
                } else {
                    call.respond(HttpStatusCode.NotFound, "User not found")
                }
            }

            // Update own profile
            put("/me") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString()
                    ?: return@put call.respond(HttpStatusCode.Unauthorized)

                val request = call.receive<UpdateProfileRequest>()
                val updatedUser = AuthService.updateProfile(userId, request.fullName, request.profileImageUrl)

                if (updatedUser != null) {
                    call.respond(HttpStatusCode.OK, updatedUser)
                } else {
                    call.respond(HttpStatusCode.NotFound, "User not found")
                }
            }

            // Change password
            put("/me/password") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString()
                    ?: return@put call.respond(HttpStatusCode.Unauthorized)

                val request = call.receive<UpdatePasswordRequest>()
                val success = AuthService.updatePassword(userId, request.oldPassword, request.newPassword)

                if (success) {
                    call.respond(HttpStatusCode.OK, mapOf("message" to "Password updated successfully"))
                } else {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid old password"))
                }
            }

            get("/search") {
                val email = call.request.queryParameters["email"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    "Missing email query parameter"
                )

                val user = AuthService.getUserByEmail(email)
                if (user != null) {
                    call.respond(HttpStatusCode.OK, user)
                } else {
                    call.respond(HttpStatusCode.NotFound, "User not found")
                }
            }

            get("/{id}") {
                val userId = call.parameters["id"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    "Missing user ID"
                )

                val user = AuthService.getUserById(userId)
                if (user != null) {
                    call.respond(HttpStatusCode.OK, user)
                } else {
                    call.respond(HttpStatusCode.NotFound, "User not found")
                }
            }
        }
    }
}