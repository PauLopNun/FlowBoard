package com.flowboard.domain

import com.flowboard.data.database.DatabaseFactory.dbQuery
import com.flowboard.data.database.Documents
import com.flowboard.data.database.DocumentPermissions
import com.flowboard.data.database.Users
import com.flowboard.data.database.WorkspaceMembers
import com.flowboard.data.models.*
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.util.*

class DocumentPersistenceService {

    suspend fun createDocument(
        title: String,
        content: String,
        ownerId: String,
        isPublic: Boolean,
        visibility: String = "private",
        workspaceId: String? = null,
        parentId: String? = null
    ): Document {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        val documentId = UUID.randomUUID()

        dbQuery {
            if (visibility == "workspace") {
                val targetWorkspaceId = workspaceId?.let { UUID.fromString(it) }
                    ?: throw IllegalArgumentException("Workspace document requires a workspaceId")
                val isMember = WorkspaceMembers.select {
                    (WorkspaceMembers.workspaceId eq targetWorkspaceId) and
                    (WorkspaceMembers.userId eq UUID.fromString(ownerId))
                }.count() > 0
                if (!isMember) {
                    throw IllegalArgumentException("Only workspace members can create workspace documents")
                }
            }

            Documents.insert {
                it[Documents.id] = documentId
                it[Documents.title] = title
                it[Documents.content] = content
                it[Documents.ownerId] = UUID.fromString(ownerId)
                it[Documents.parentId] = parentId?.let { p -> UUID.fromString(p) }
                it[Documents.isPublic] = false
                it[Documents.visibility] = visibility
                it[Documents.workspaceId] = workspaceId?.let { w -> UUID.fromString(w) }
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

            val hasWorkspaceAccess = docQuery[Documents.visibility] == "workspace" &&
                docQuery[Documents.workspaceId] != null &&
                WorkspaceMembers.select {
                    (WorkspaceMembers.workspaceId eq docQuery[Documents.workspaceId]!!) and
                    (WorkspaceMembers.userId eq UUID.fromString(requesterId))
                }.count() > 0

            if (!hasPermission && !isOwner && !hasWorkspaceAccess && !docQuery[Documents.isPublic]) {
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
                visibility = docQuery[Documents.visibility],
                workspaceId = docQuery[Documents.workspaceId]?.toString(),
                parentId = docQuery[Documents.parentId]?.toString(),
                createdAt = docQuery[Documents.createdAt],
                updatedAt = docQuery[Documents.updatedAt],
                lastEditedBy = docQuery[Documents.lastEditedBy]?.toString(),
                permissions = permissions
            )
        }
    }

    /**
     * Server-side save that skips permission checks (called by WebSocket handler on session end).
     */
    suspend fun saveDocumentContent(documentId: String, title: String, content: String) {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        dbQuery {
            Documents.update({ Documents.id eq UUID.fromString(documentId) }) {
                it[Documents.title] = title
                it[Documents.content] = content
                it[Documents.updatedAt] = now
            }
        }
    }

    suspend fun updateDocument(
        documentId: String,
        userId: String,
        title: String?,
        content: String?,
        isPublic: Boolean?,
        visibility: String? = null,
        workspaceId: String? = null
    ): Document? {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        val isOwner = dbQuery {
            Documents.select {
                (Documents.id eq UUID.fromString(documentId)) and
                    (Documents.ownerId eq UUID.fromString(userId))
            }.count() > 0
        }

        val hasPermission = dbQuery {
            val directPermission = DocumentPermissions
                .select {
                    (DocumentPermissions.documentId eq UUID.fromString(documentId)) and
                    (DocumentPermissions.userId eq UUID.fromString(userId)) and
                    (DocumentPermissions.role inList listOf("editor", "owner"))
                }
                .count() > 0

            if (directPermission) {
                true
            } else {
                val document = Documents.select { Documents.id eq UUID.fromString(documentId) }.singleOrNull()
                    ?: return@dbQuery false
                document[Documents.visibility] == "workspace" &&
                    document[Documents.workspaceId] != null &&
                    WorkspaceMembers.select {
                        (WorkspaceMembers.workspaceId eq document[Documents.workspaceId]!!) and
                        (WorkspaceMembers.userId eq UUID.fromString(userId))
                    }.count() > 0
            }
        }

        if ((visibility != null || workspaceId != null) && !isOwner) return null
        if (!hasPermission && !isOwner) return null

        dbQuery {
            val nextWorkspaceId = when {
                visibility == null -> workspaceId?.let { UUID.fromString(it) }
                visibility == "workspace" -> {
                    val rawWorkspaceId = workspaceId ?: Documents
                        .select { Documents.id eq UUID.fromString(documentId) }
                        .singleOrNull()
                        ?.get(Documents.workspaceId)
                        ?.toString()
                    val parsedWorkspaceId = rawWorkspaceId?.let { UUID.fromString(it) }
                        ?: throw IllegalArgumentException("Workspace visibility requires a workspaceId")
                    val isMember = WorkspaceMembers.select {
                        (WorkspaceMembers.workspaceId eq parsedWorkspaceId) and
                        (WorkspaceMembers.userId eq UUID.fromString(userId))
                    }.count() > 0
                    if (!isMember) {
                        throw IllegalArgumentException("Only workspace members can move documents into this workspace")
                    }
                    parsedWorkspaceId
                }
                else -> null
            }

            Documents.update({ Documents.id eq UUID.fromString(documentId) }) {
                if (title != null) it[Documents.title] = title
                if (content != null) it[Documents.content] = content
                if (isPublic != null) it[Documents.isPublic] = false
                if (visibility != null) it[Documents.visibility] = visibility
                if (visibility != null || workspaceId != null) it[Documents.workspaceId] = nextWorkspaceId
                it[Documents.updatedAt] = now
                it[Documents.lastEditedBy] = UUID.fromString(userId)
            }
        }

        return getDocumentById(documentId, userId)
    }

    suspend fun getWorkspaceDocuments(workspaceId: String, requesterId: String): List<Document>? {
        return dbQuery {
            // Verify requester is a member of the workspace
            val isMember = WorkspaceMembers
                .select {
                    (WorkspaceMembers.workspaceId eq UUID.fromString(workspaceId)) and
                    (WorkspaceMembers.userId eq UUID.fromString(requesterId))
                }
                .count() > 0
            if (!isMember) return@dbQuery null

            Documents
                .leftJoin(Users, { Documents.ownerId }, { Users.id })
                .select {
                    (Documents.workspaceId eq UUID.fromString(workspaceId)) and
                    (Documents.visibility eq "workspace")
                }
                .orderBy(Documents.updatedAt, SortOrder.DESC)
                .map { row -> row.toDocument() }
        }
    }

    private fun ResultRow.toDocument() = Document(
        id = this[Documents.id].toString(),
        title = this[Documents.title],
        content = this[Documents.content],
        ownerId = this[Documents.ownerId].toString(),
        ownerName = try { this[Users.username] } catch (_: Exception) { null },
        isPublic = this[Documents.isPublic],
        visibility = this[Documents.visibility],
        workspaceId = this[Documents.workspaceId]?.toString(),
        parentId = this[Documents.parentId]?.toString(),
        createdAt = this[Documents.createdAt],
        updatedAt = this[Documents.updatedAt],
        lastEditedBy = this[Documents.lastEditedBy]?.toString()
    )

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
            val ownedRaw = Documents
                .leftJoin(Users, { Documents.ownerId }, { Users.id })
                .select {
                    (Documents.ownerId eq UUID.fromString(userId)) and
                    (Documents.visibility neq "workspace")
                }
                .map { row -> row.toDocument() }

            val ownedDocumentIds = ownedRaw.map { UUID.fromString(it.id) }
            val sharedOwnedDocumentIds = if (ownedDocumentIds.isEmpty()) {
                emptySet()
            } else {
                DocumentPermissions
                    .select {
                        (DocumentPermissions.documentId inList ownedDocumentIds) and
                        (DocumentPermissions.role neq "owner")
                    }
                    .map { row -> row[DocumentPermissions.documentId].toString() }
                    .toSet()
            }
            val owned = ownedRaw.map { document ->
                if (document.id in sharedOwnedDocumentIds && document.visibility == "private") {
                    document.copy(visibility = "shared")
                } else {
                    document
                }
            }

            // Get shared documents (explicit permission grant, not workspace)
            val shared = DocumentPermissions
                .join(
                    Documents,
                    JoinType.INNER,
                    onColumn = DocumentPermissions.documentId,
                    otherColumn = Documents.id
                )
                .leftJoin(Users, { Documents.ownerId }, { Users.id })
                .select {
                    (DocumentPermissions.userId eq UUID.fromString(userId)) and
                    (Documents.ownerId neq UUID.fromString(userId)) and
                    (DocumentPermissions.role neq "owner") and
                    (Documents.visibility neq "workspace")
                }
                .map { row -> row.toDocument() }

            DocumentListResponse(
                ownedDocuments = owned,
                sharedWithMe = shared
            )
        }
    }

    suspend fun shareDocument(documentId: String, ownerId: String, targetEmail: String, role: String): ShareDocumentResponse {
        return dbQuery {
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

            val targetUser = Users
                .select { Users.email eq targetEmail }
                .singleOrNull()

            if (targetUser == null) {
                return@dbQuery ShareDocumentResponse(
                    success = true,
                    message = "Invitation email sent to $targetEmail (they must create a FlowBoard account to access the document)"
                )
            }

            val targetUserId = targetUser[Users.id].toString()
            val existing = DocumentPermissions
                .select {
                    (DocumentPermissions.documentId eq UUID.fromString(documentId)) and
                    (DocumentPermissions.userId eq UUID.fromString(targetUserId))
                }
                .singleOrNull()

            if (existing != null) {
                if (existing[DocumentPermissions.role] == "owner") {
                    return@dbQuery ShareDocumentResponse(
                        success = false,
                        message = "Document owner already has access"
                    )
                }

                val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
                val permissionId = existing[DocumentPermissions.id].value
                DocumentPermissions.update({ DocumentPermissions.id eq permissionId }) {
                    it[DocumentPermissions.role] = role
                    it[DocumentPermissions.grantedAt] = now
                }
                Documents.update({
                    (Documents.id eq UUID.fromString(documentId)) and
                    (Documents.visibility neq "workspace")
                }) {
                    it[Documents.visibility] = "shared"
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

                return@dbQuery ShareDocumentResponse(
                    success = true,
                    message = "Access updated successfully",
                    permission = permission,
                    targetUserId = targetUserId,
                    targetUserName = targetUser[Users.username],
                    targetUserEmail = targetUser[Users.email],
                    role = role
                )
            }

            ShareDocumentResponse(
                success = true,
                message = "Invitation sent",
                targetUserId = targetUserId,
                targetUserName = targetUser[Users.username],
                targetUserEmail = targetUser[Users.email],
                role = role
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

            if (updated > 0) {
                Documents.update({
                    (Documents.id eq UUID.fromString(documentId)) and
                    (Documents.visibility neq "workspace")
                }) {
                    it[Documents.visibility] = "shared"
                }
            }

            updated > 0
        }
    }

    suspend fun getChildDocuments(parentId: String, requesterId: String): List<Document> {
        val parent = getDocumentById(parentId, requesterId) ?: return emptyList()
        val parentWorkspaceId = parent.workspaceId?.let { UUID.fromString(it) }
        return dbQuery {
            Documents
                .leftJoin(Users, { Documents.ownerId }, { Users.id })
                .select {
                    val base = Documents.parentId eq UUID.fromString(parentId)
                    val ownerAccess = Documents.ownerId eq UUID.fromString(requesterId)
                    val workspaceAccess = if (parentWorkspaceId != null) {
                        (Documents.workspaceId eq parentWorkspaceId) and (Documents.visibility eq "workspace")
                    } else {
                        Op.FALSE
                    }
                    base and (ownerAccess or workspaceAccess)
                }
                .map { row -> row.toDocument() }
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

            val removed = DocumentPermissions.deleteWhere {
                (DocumentPermissions.documentId eq UUID.fromString(documentId)) and
                (DocumentPermissions.userId eq UUID.fromString(targetUserId))
            } > 0

            if (removed) {
                val remainingShares = DocumentPermissions.select {
                    (DocumentPermissions.documentId eq UUID.fromString(documentId)) and
                    (DocumentPermissions.role neq "owner")
                }.count()

                if (remainingShares == 0L) {
                    Documents.update({
                        (Documents.id eq UUID.fromString(documentId)) and
                        (Documents.visibility neq "workspace")
                    }) {
                        it[Documents.visibility] = "private"
                    }
                }
            }

            removed
        }
    }
}
