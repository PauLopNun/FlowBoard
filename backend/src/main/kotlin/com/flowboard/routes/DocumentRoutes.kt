package com.flowboard.routes

import com.flowboard.domain.DocumentPersistenceService
import com.flowboard.domain.NotificationService
import com.flowboard.data.models.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.documentRoutes(
    documentService: DocumentPersistenceService,
    notificationService: NotificationService
) {
    authenticate("auth-jwt") {
        route("/documents") {
            // Create document
            post {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString()

                if (userId == null) {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Unauthorized"))
                    return@post
                }

                val request = call.receive<CreateDocumentRequest>()
                val document = documentService.createDocument(
                    title = request.title,
                    content = request.content,
                    ownerId = userId,
                    isPublic = request.isPublic
                )

                call.respond(HttpStatusCode.Created, document)
            }

            // Get all user documents (owned + shared)
            get {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString()

                if (userId == null) {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Unauthorized"))
                    return@get
                }

                val documents = documentService.getUserDocuments(userId)
                call.respond(HttpStatusCode.OK, documents)
            }

            // Get document by ID
            get("/{id}") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString()

                if (userId == null) {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Unauthorized"))
                    return@get
                }

                val documentId = call.parameters["id"]
                if (documentId == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing document ID"))
                    return@get
                }

                val document = documentService.getDocumentById(documentId, userId)
                if (document == null) {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Document not found or access denied"))
                    return@get
                }

                call.respond(HttpStatusCode.OK, document)
            }

            // Update document
            put("/{id}") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString()

                if (userId == null) {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Unauthorized"))
                    return@put
                }

                val documentId = call.parameters["id"]
                if (documentId == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing document ID"))
                    return@put
                }

                val request = call.receive<UpdateDocumentRequest>()
                val document = documentService.updateDocument(
                    documentId = documentId,
                    userId = userId,
                    title = request.title,
                    content = request.content,
                    isPublic = request.isPublic
                )

                if (document == null) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "No permission to update document"))
                    return@put
                }

                call.respond(HttpStatusCode.OK, document)
            }

            // Delete document
            delete("/{id}") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString()

                if (userId == null) {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Unauthorized"))
                    return@delete
                }

                val documentId = call.parameters["id"]
                if (documentId == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing document ID"))
                    return@delete
                }

                val deleted = documentService.deleteDocument(documentId, userId)
                if (!deleted) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Only owner can delete document"))
                    return@delete
                }

                call.respond(HttpStatusCode.OK, mapOf("message" to "Document deleted successfully"))
            }

            // Share document with user by email
            post("/{id}/share") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString()
                val userName = principal?.payload?.getClaim("username")?.asString()

                if (userId == null) {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Unauthorized"))
                    return@post
                }

                val documentId = call.parameters["id"]
                if (documentId == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing document ID"))
                    return@post
                }

                val request = call.receive<ShareDocumentRequest>()

                // Validate role
                if (request.role !in listOf("viewer", "editor")) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid role. Must be 'viewer' or 'editor'"))
                    return@post
                }

                val response = documentService.shareDocument(
                    documentId = documentId,
                    ownerId = userId,
                    targetEmail = request.email,
                    role = request.role
                )

                if (!response.success) {
                    call.respond(HttpStatusCode.BadRequest, response)
                    return@post
                }

                // Send notification to the user
                if (response.permission != null) {
                    val document = documentService.getDocumentById(documentId, userId)
                    if (document != null) {
                        notificationService.sendDocumentSharedNotification(
                            recipientId = response.permission.userId,
                            senderName = userName ?: "Someone",
                            documentTitle = document.title,
                            documentId = documentId
                        )
                    }
                }

                call.respond(HttpStatusCode.OK, response)
            }

            // Get document permissions
            get("/{id}/permissions") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString()

                if (userId == null) {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Unauthorized"))
                    return@get
                }

                val documentId = call.parameters["id"]
                if (documentId == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing document ID"))
                    return@get
                }

                val permissions = documentService.getDocumentPermissions(documentId, userId)
                if (permissions == null) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Access denied"))
                    return@get
                }

                call.respond(HttpStatusCode.OK, permissions)
            }

            // Update permission role
            put("/{id}/permissions/{userId}") {
                val principal = call.principal<JWTPrincipal>()
                val ownerId = principal?.payload?.getClaim("userId")?.asString()

                if (ownerId == null) {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Unauthorized"))
                    return@put
                }

                val documentId = call.parameters["id"]
                val targetUserId = call.parameters["userId"]

                if (documentId == null || targetUserId == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing parameters"))
                    return@put
                }

                val request = call.receive<UpdatePermissionRequest>()

                // Validate role
                if (request.role !in listOf("viewer", "editor")) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid role. Must be 'viewer' or 'editor'"))
                    return@put
                }

                val updated = documentService.updatePermission(documentId, ownerId, targetUserId, request.role)
                if (!updated) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Cannot update permission"))
                    return@put
                }

                call.respond(HttpStatusCode.OK, mapOf("message" to "Permission updated successfully"))
            }

            // Remove permission
            delete("/{id}/permissions/{userId}") {
                val principal = call.principal<JWTPrincipal>()
                val ownerId = principal?.payload?.getClaim("userId")?.asString()

                if (ownerId == null) {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Unauthorized"))
                    return@delete
                }

                val documentId = call.parameters["id"]
                val targetUserId = call.parameters["userId"]

                if (documentId == null || targetUserId == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing parameters"))
                    return@delete
                }

                val removed = documentService.removePermission(documentId, ownerId, targetUserId)
                if (!removed) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Cannot remove permission"))
                    return@delete
                }

                call.respond(HttpStatusCode.OK, mapOf("message" to "Permission removed successfully"))
            }
        }
    }
}
