package com.flowboard.domain

import com.flowboard.data.database.DatabaseFactory.dbQuery
import com.flowboard.data.database.Documents
import com.flowboard.data.database.DocumentPermissions
import com.flowboard.data.database.Users
import com.flowboard.data.models.*
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.util.*

class DocumentPersistenceService {

    suspend fun createDocument(title: String, content: String, ownerId: String, isPublic: Boolean): Document {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        val documentId = UUID.randomUUID()

        dbQuery {
            Documents.insert {
                it[Documents.id] = documentId
                it[Documents.title] = title
                it[Documents.content] = content
                it[Documents.ownerId] = UUID.fromString(ownerId)
                it[Documents.isPublic] = isPublic
                it[Documents.createdAt] = now
                it[Documents.updatedAt] = now
            }

            // Grant owner permission
            DocumentPermissions.insert {
                it[DocumentPermissions.id] = UUID.randomUUID()
                it[DocumentPermissions.documentId] = documentId
                it[DocumentPermissions.userId] = UUID.fromString(ownerId)
                it[DocumentPermissions.role] = "owner"
                it[DocumentPermissions.grantedBy] = UUID.fromString(ownerId)
                it[DocumentPermissions.grantedAt] = now
            }
        }

        return getDocumentById(documentId.toString(), ownerId)
            ?: throw Exception("Failed to create document")
    }

    suspend fun getDocumentById(documentId: String, requesterId: String): Document? {
        return dbQuery {
            val docQuery = Documents
                .leftJoin(Users, { Documents.ownerId }, { Users.id })
                .select { Documents.id eq UUID.fromString(documentId) }
                .singleOrNull()

            if (docQuery == null) return@dbQuery null

            // Check permission
            val hasPermission = DocumentPermissions
                .select {
                    (DocumentPermissions.documentId eq UUID.fromString(documentId)) and
                    (DocumentPermissions.userId eq UUID.fromString(requesterId))
                }
                .count() > 0

            val isOwner = docQuery[Documents.ownerId].toString() == requesterId

            if (!hasPermission && !isOwner && !docQuery[Documents.isPublic]) {
                return@dbQuery null
            }

            // Get permissions
            val permissions = DocumentPermissions
                .leftJoin(Users, { DocumentPermissions.userId }, { Users.id })
                .select { DocumentPermissions.documentId eq UUID.fromString(documentId) }
                .map { row ->
                    DocumentPermissionResponse(
                        id = row[DocumentPermissions.id].toString(),
                        documentId = row[DocumentPermissions.documentId].toString(),
                        userId = row[DocumentPermissions.userId].toString(),
                        userName = row[Users.username],
                        userEmail = row[Users.email],
                        role = row[DocumentPermissions.role],
                        grantedBy = row[DocumentPermissions.grantedBy].toString(),
                        grantedAt = row[DocumentPermissions.grantedAt]
                    )
                }

            Document(
                id = docQuery[Documents.id].toString(),
                title = docQuery[Documents.title],
                content = docQuery[Documents.content],
                ownerId = docQuery[Documents.ownerId].toString(),
                ownerName = docQuery[Users.username],
                isPublic = docQuery[Documents.isPublic],
                createdAt = docQuery[Documents.createdAt],
                updatedAt = docQuery[Documents.updatedAt],
                lastEditedBy = docQuery[Documents.lastEditedBy]?.toString(),
                permissions = permissions
            )
        }
    }

    suspend fun updateDocument(documentId: String, userId: String, title: String?, content: String?, isPublic: Boolean?): Document? {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)

        // Check if user has editor or owner permission
        val hasPermission = dbQuery {
            DocumentPermissions
                .select {
                    (DocumentPermissions.documentId eq UUID.fromString(documentId)) and
                    (DocumentPermissions.userId eq UUID.fromString(userId)) and
                    (DocumentPermissions.role inList listOf("editor", "owner"))
                }
                .count() > 0
        }

        if (!hasPermission) return null

        dbQuery {
            Documents.update({ Documents.id eq UUID.fromString(documentId) }) {
                if (title != null) it[Documents.title] = title
                if (content != null) it[Documents.content] = content
                if (isPublic != null) it[Documents.isPublic] = isPublic
                it[Documents.updatedAt] = now
                it[Documents.lastEditedBy] = UUID.fromString(userId)
            }
        }

        return getDocumentById(documentId, userId)
    }

    suspend fun deleteDocument(documentId: String, userId: String): Boolean {
        return dbQuery {
            // Only owner can delete
            val isOwner = Documents
                .select {
                    (Documents.id eq UUID.fromString(documentId)) and
                    (Documents.ownerId eq UUID.fromString(userId))
                }
                .count() > 0

            if (!isOwner) return@dbQuery false

            // Delete permissions first
            DocumentPermissions.deleteWhere { DocumentPermissions.documentId eq UUID.fromString(documentId) }

            // Delete document
            Documents.deleteWhere { Documents.id eq UUID.fromString(documentId) } > 0
        }
    }

    suspend fun getUserDocuments(userId: String): DocumentListResponse {
        return dbQuery {
            // Get owned documents
            val owned = Documents
                .leftJoin(Users, { Documents.ownerId }, { Users.id })
                .select { Documents.ownerId eq UUID.fromString(userId) }
                .map { row ->
                    Document(
                        id = row[Documents.id].toString(),
                        title = row[Documents.title],
                        content = row[Documents.content],
                        ownerId = row[Documents.ownerId].toString(),
                        ownerName = row[Users.username],
                        isPublic = row[Documents.isPublic],
                        createdAt = row[Documents.createdAt],
                        updatedAt = row[Documents.updatedAt],
                        lastEditedBy = row[Documents.lastEditedBy]?.toString()
                    )
                }

            // Get shared documents
            val shared = (DocumentPermissions innerJoin Documents innerJoin Users)
                .slice(
                    Documents.id,
                    Documents.title,
                    Documents.content,
                    Documents.ownerId,
                    Users.username,
                    Documents.isPublic,
                    Documents.createdAt,
                    Documents.updatedAt,
                    Documents.lastEditedBy
                )
                .select {
                    (DocumentPermissions.userId eq UUID.fromString(userId)) and
                    (Documents.ownerId neq UUID.fromString(userId)) and
                    (DocumentPermissions.role neq "owner")
                }
                .map { row ->
                    Document(
                        id = row[Documents.id].toString(),
                        title = row[Documents.title],
                        content = row[Documents.content],
                        ownerId = row[Documents.ownerId].toString(),
                        ownerName = row[Users.username],
                        isPublic = row[Documents.isPublic],
                        createdAt = row[Documents.createdAt],
                        updatedAt = row[Documents.updatedAt],
                        lastEditedBy = row[Documents.lastEditedBy]?.toString()
                    )
                }

            DocumentListResponse(
                ownedDocuments = owned,
                sharedWithMe = shared
            )
        }
    }

    suspend fun shareDocument(documentId: String, ownerId: String, targetEmail: String, role: String): ShareDocumentResponse {
        return dbQuery {
            // Verify requester is owner
            val isOwner = Documents
                .select {
                    (Documents.id eq UUID.fromString(documentId)) and
                    (Documents.ownerId eq UUID.fromString(ownerId))
                }
                .count() > 0

            if (!isOwner) {
                return@dbQuery ShareDocumentResponse(
                    success = false,
                    message = "Only the document owner can share it"
                )
            }

            // Find user by email
            val targetUser = Users
                .select { Users.email eq targetEmail }
                .singleOrNull()

            if (targetUser == null) {
                return@dbQuery ShareDocumentResponse(
                    success = false,
                    message = "User with email $targetEmail not found"
                )
            }

            val targetUserId = targetUser[Users.id].toString()

            // Check if already shared
            val existing = DocumentPermissions
                .select {
                    (DocumentPermissions.documentId eq UUID.fromString(documentId)) and
                    (DocumentPermissions.userId eq UUID.fromString(targetUserId))
                }
                .singleOrNull()

            val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
            val permissionId: UUID

            if (existing != null) {
                // Update existing permission
                permissionId = existing[DocumentPermissions.id].value
                DocumentPermissions.update({
                    DocumentPermissions.id eq permissionId
                }) {
                    it[DocumentPermissions.role] = role
                    it[DocumentPermissions.grantedAt] = now
                }
            } else {
                // Create new permission
                permissionId = UUID.randomUUID()
                DocumentPermissions.insert {
                    it[DocumentPermissions.id] = permissionId
                    it[DocumentPermissions.documentId] = UUID.fromString(documentId)
                    it[DocumentPermissions.userId] = UUID.fromString(targetUserId)
                    it[DocumentPermissions.role] = role
                    it[DocumentPermissions.grantedBy] = UUID.fromString(ownerId)
                    it[DocumentPermissions.grantedAt] = now
                }
            }

            val permission = DocumentPermissionResponse(
                id = permissionId.toString(),
                documentId = documentId,
                userId = targetUserId,
                userName = targetUser[Users.username],
                userEmail = targetUser[Users.email],
                role = role,
                grantedBy = ownerId,
                grantedAt = now
            )

            ShareDocumentResponse(
                success = true,
                message = "Document shared successfully",
                permission = permission
            )
        }
    }

    suspend fun getDocumentPermissions(documentId: String, requesterId: String): List<DocumentPermissionResponse>? {
        return dbQuery {
            // Check if requester has access to the document
            val hasPermission = DocumentPermissions
                .select {
                    (DocumentPermissions.documentId eq UUID.fromString(documentId)) and
                    (DocumentPermissions.userId eq UUID.fromString(requesterId))
                }
                .count() > 0

            if (!hasPermission) return@dbQuery null

            // Get all permissions for this document
            DocumentPermissions
                .leftJoin(Users, { DocumentPermissions.userId }, { Users.id })
                .select { DocumentPermissions.documentId eq UUID.fromString(documentId) }
                .map { row ->
                    DocumentPermissionResponse(
                        id = row[DocumentPermissions.id].toString(),
                        documentId = row[DocumentPermissions.documentId].toString(),
                        userId = row[DocumentPermissions.userId].toString(),
                        userName = row[Users.username],
                        userEmail = row[Users.email],
                        role = row[DocumentPermissions.role],
                        grantedBy = row[DocumentPermissions.grantedBy].toString(),
                        grantedAt = row[DocumentPermissions.grantedAt]
                    )
                }
        }
    }

    suspend fun updatePermission(documentId: String, ownerId: String, targetUserId: String, newRole: String): Boolean {
        return dbQuery {
            // Verify requester is owner
            val isOwner = Documents
                .select {
                    (Documents.id eq UUID.fromString(documentId)) and
                    (Documents.ownerId eq UUID.fromString(ownerId))
                }
                .count() > 0

            if (!isOwner) return@dbQuery false

            // Don't allow changing owner permission
            val permission = DocumentPermissions
                .select {
                    (DocumentPermissions.documentId eq UUID.fromString(documentId)) and
                    (DocumentPermissions.userId eq UUID.fromString(targetUserId))
                }
                .singleOrNull()

            if (permission?.get(DocumentPermissions.role) == "owner") {
                return@dbQuery false
            }

            // Update permission
            val updated = DocumentPermissions.update({
                (DocumentPermissions.documentId eq UUID.fromString(documentId)) and
                (DocumentPermissions.userId eq UUID.fromString(targetUserId))
            }) {
                it[DocumentPermissions.role] = newRole
                it[DocumentPermissions.grantedAt] = Clock.System.now().toLocalDateTime(TimeZone.UTC)
            }

            updated > 0
        }
    }

    suspend fun removePermission(documentId: String, ownerId: String, targetUserId: String): Boolean {
        return dbQuery {
            // Verify requester is owner
            val isOwner = Documents
                .select {
                    (Documents.id eq UUID.fromString(documentId)) and
                    (Documents.ownerId eq UUID.fromString(ownerId))
                }
                .count() > 0

            if (!isOwner) return@dbQuery false

            // Don't allow removing owner permission
            val permission = DocumentPermissions
                .select {
                    (DocumentPermissions.documentId eq UUID.fromString(documentId)) and
                    (DocumentPermissions.userId eq UUID.fromString(targetUserId))
                }
                .singleOrNull()

            if (permission?.get(DocumentPermissions.role) == "owner") {
                return@dbQuery false
            }

            DocumentPermissions.deleteWhere {
                (DocumentPermissions.documentId eq UUID.fromString(documentId)) and
                (DocumentPermissions.userId eq UUID.fromString(targetUserId))
            } > 0
        }
    }
}
