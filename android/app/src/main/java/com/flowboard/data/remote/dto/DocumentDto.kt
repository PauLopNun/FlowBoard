package com.flowboard.data.remote.dto

import com.flowboard.data.local.entities.DocumentEntity
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class DocumentDto(
    val id: String,
    val title: String,
    val content: String,
    val ownerId: String,
    val ownerName: String? = null,
    val isPublic: Boolean = false,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val lastEditedBy: String? = null,
    val lastEditedByName: String? = null,
    val permissions: List<DocumentPermissionDto>? = null
) {
    fun toEntity(): DocumentEntity {
        return DocumentEntity(
            id = id,
            title = title,
            content = content,
            ownerId = ownerId,
            ownerName = ownerName,
            isPublic = isPublic,
            createdAt = createdAt.toString(),
            updatedAt = updatedAt.toString(),
            lastEditedBy = lastEditedBy,
            lastEditedByName = lastEditedByName,
            isSync = true,
            lastSyncAt = updatedAt.toString()
        )
    }

    companion object {
        fun fromEntity(entity: DocumentEntity): DocumentDto {
            return DocumentDto(
                id = entity.id,
                title = entity.title,
                content = entity.content,
                ownerId = entity.ownerId,
                ownerName = entity.ownerName,
                isPublic = entity.isPublic,
                createdAt = LocalDateTime.parse(entity.createdAt),
                updatedAt = LocalDateTime.parse(entity.updatedAt),
                lastEditedBy = entity.lastEditedBy,
                lastEditedByName = entity.lastEditedByName
            )
        }
    }
}

@Serializable
data class DocumentPermissionDto(
    val id: String,
    val documentId: String,
    val userId: String,
    val userName: String? = null,
    val userEmail: String? = null,
    val role: String,
    val grantedBy: String,
    val grantedAt: LocalDateTime
)

@Serializable
data class CreateDocumentRequest(
    val title: String,
    val content: String = "",
    val isPublic: Boolean = false
)

@Serializable
data class UpdateDocumentRequest(
    val title: String? = null,
    val content: String? = null,
    val isPublic: Boolean? = null
)

@Serializable
data class ShareDocumentRequest(
    val email: String,
    val role: String
)

@Serializable
data class DocumentListResponse(
    val ownedDocuments: List<DocumentDto>,
    val sharedWithMe: List<DocumentDto>
)
