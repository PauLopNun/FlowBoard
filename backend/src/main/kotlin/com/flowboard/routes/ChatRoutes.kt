package com.flowboard.routes

import com.flowboard.domain.ChatService
import com.flowboard.data.models.CreateChatRoomRequest
import com.flowboard.data.models.SendMessageRequest
import com.flowboard.data.models.UpdateMessageRequest
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.chatRoutes(chatService: ChatService) {
    authenticate("jwt") {
        route("/chat") {
            // Create chat room
            post("/rooms") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString()

                if (userId == null) {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Unauthorized"))
                    return@post
                }

                val request = call.receive<CreateChatRoomRequest>()
                val chatRoom = chatService.createChatRoom(request, userId)

                call.respond(HttpStatusCode.Created, chatRoom)
            }

            // Get user's chat rooms
            get("/rooms") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString()

                if (userId == null) {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Unauthorized"))
                    return@get
                }

                val chatRooms = chatService.getUserChatRooms(userId)
                call.respond(HttpStatusCode.OK, chatRooms)
            }

            // Get specific chat room
            get("/rooms/{id}") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString()

                if (userId == null) {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Unauthorized"))
                    return@get
                }

                val chatRoomId = call.parameters["id"]
                if (chatRoomId == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing chat room ID"))
                    return@get
                }

                val chatRoom = chatService.getChatRoomById(chatRoomId, userId)
                if (chatRoom == null) {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Chat room not found or access denied"))
                    return@get
                }

                call.respond(HttpStatusCode.OK, chatRoom)
            }

            // Send message
            post("/rooms/{id}/messages") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString()

                if (userId == null) {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Unauthorized"))
                    return@post
                }

                val chatRoomId = call.parameters["id"]
                if (chatRoomId == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing chat room ID"))
                    return@post
                }

                val request = call.receive<SendMessageRequest>()
                val message = chatService.sendMessage(chatRoomId, userId, request)

                call.respond(HttpStatusCode.Created, message)
            }

            // Get messages
            get("/rooms/{id}/messages") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString()

                if (userId == null) {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Unauthorized"))
                    return@get
                }

                val chatRoomId = call.parameters["id"]
                if (chatRoomId == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing chat room ID"))
                    return@get
                }

                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 50
                val offset = call.request.queryParameters["offset"]?.toIntOrNull() ?: 0

                val messages = chatService.getChatMessages(chatRoomId, userId, limit, offset)
                call.respond(HttpStatusCode.OK, messages)
            }

            // Update message
            put("/messages/{id}") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString()

                if (userId == null) {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Unauthorized"))
                    return@put
                }

                val messageId = call.parameters["id"]
                if (messageId == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing message ID"))
                    return@put
                }

                val request = call.receive<UpdateMessageRequest>()
                val message = chatService.updateMessage(messageId, userId, request.content)

                if (message == null) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Cannot update message"))
                    return@put
                }

                call.respond(HttpStatusCode.OK, message)
            }

            // Delete message
            delete("/messages/{id}") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString()

                if (userId == null) {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Unauthorized"))
                    return@delete
                }

                val messageId = call.parameters["id"]
                if (messageId == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing message ID"))
                    return@delete
                }

                val deleted = chatService.deleteMessage(messageId, userId)
                if (!deleted) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Cannot delete message"))
                    return@delete
                }

                call.respond(HttpStatusCode.OK, mapOf("message" to "Message deleted"))
            }

            // Add participant to chat room
            post("/rooms/{id}/participants") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString()

                if (userId == null) {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Unauthorized"))
                    return@post
                }

                val chatRoomId = call.parameters["id"]
                if (chatRoomId == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing chat room ID"))
                    return@post
                }

                val request = call.receive<Map<String, String>>()
                val targetUserId = request["userId"]

                if (targetUserId == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing user ID"))
                    return@post
                }

                val added = chatService.addParticipant(chatRoomId, userId, targetUserId)
                if (!added) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Cannot add participant"))
                    return@post
                }

                call.respond(HttpStatusCode.OK, mapOf("message" to "Participant added"))
            }

            // Remove participant from chat room
            delete("/rooms/{id}/participants/{userId}") {
                val principal = call.principal<JWTPrincipal>()
                val requesterId = principal?.payload?.getClaim("userId")?.asString()

                if (requesterId == null) {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Unauthorized"))
                    return@delete
                }

                val chatRoomId = call.parameters["id"]
                val targetUserId = call.parameters["userId"]

                if (chatRoomId == null || targetUserId == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing parameters"))
                    return@delete
                }

                val removed = chatService.removeParticipant(chatRoomId, requesterId, targetUserId)
                if (!removed) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Cannot remove participant"))
                    return@delete
                }

                call.respond(HttpStatusCode.OK, mapOf("message" to "Participant removed"))
            }
        }
    }
}
