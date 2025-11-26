package com.flowboard.routes

import com.flowboard.domain.NotificationService
import com.flowboard.data.models.CreateNotificationRequest
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.notificationRoutes(notificationService: NotificationService) {
    authenticate("jwt") {
        route("/notifications") {
            // Get all user notifications
            get {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString()

                if (userId == null) {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Unauthorized"))
                    return@get
                }

                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 50
                val notifications = notificationService.getUserNotifications(userId, limit)

                call.respond(HttpStatusCode.OK, notifications)
            }

            // Get unread notifications
            get("/unread") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString()

                if (userId == null) {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Unauthorized"))
                    return@get
                }

                val notifications = notificationService.getUnreadNotifications(userId)
                call.respond(HttpStatusCode.OK, notifications)
            }

            // Get notification stats
            get("/stats") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString()

                if (userId == null) {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Unauthorized"))
                    return@get
                }

                val stats = notificationService.getNotificationStats(userId)
                call.respond(HttpStatusCode.OK, stats)
            }

            // Create notification (mostly for testing or admin)
            post {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString()

                if (userId == null) {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Unauthorized"))
                    return@post
                }

                val request = call.receive<CreateNotificationRequest>()
                val notification = notificationService.createNotification(request)

                call.respond(HttpStatusCode.Created, notification)
            }

            // Mark notification as read
            patch("/{id}/read") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString()

                if (userId == null) {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Unauthorized"))
                    return@patch
                }

                val notificationId = call.parameters["id"]
                if (notificationId == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing notification ID"))
                    return@patch
                }

                val marked = notificationService.markAsRead(notificationId, userId)
                if (!marked) {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Notification not found"))
                    return@patch
                }

                call.respond(HttpStatusCode.OK, mapOf("message" to "Notification marked as read"))
            }

            // Mark all notifications as read
            patch("/read-all") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString()

                if (userId == null) {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Unauthorized"))
                    return@patch
                }

                notificationService.markAllAsRead(userId)
                call.respond(HttpStatusCode.OK, mapOf("message" to "All notifications marked as read"))
            }

            // Delete notification
            delete("/{id}") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString()

                if (userId == null) {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Unauthorized"))
                    return@delete
                }

                val notificationId = call.parameters["id"]
                if (notificationId == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing notification ID"))
                    return@delete
                }

                val deleted = notificationService.deleteNotification(notificationId, userId)
                if (!deleted) {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Notification not found"))
                    return@delete
                }

                call.respond(HttpStatusCode.OK, mapOf("message" to "Notification deleted"))
            }

            // Delete all notifications
            delete {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString()

                if (userId == null) {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Unauthorized"))
                    return@delete
                }

                notificationService.deleteAllNotifications(userId)
                call.respond(HttpStatusCode.OK, mapOf("message" to "All notifications deleted"))
            }
        }
    }
}
