package com.flowboard.data.models

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class Document(
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
    val permissions: List<DocumentPermissionResponse>? = null
)

@Serializable
data class DocumentPermissionResponse(
    val id: String,
    val documentId: String,
    val userId: String,
    val userName: String? = null,
    val userEmail: String? = null,
    val role: String, // viewer, editor, owner
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
    val role: String // viewer or editor
)

@Serializable
data class ShareDocumentResponse(
    val success: Boolean,
    val message: String,
    val permission: DocumentPermissionResponse? = null
)

@Serializable
data class DocumentListResponse(
    val ownedDocuments: List<Document>,
    val sharedWithMe: List<Document>
)
