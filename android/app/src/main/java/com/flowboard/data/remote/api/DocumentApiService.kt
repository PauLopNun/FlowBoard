package com.flowboard.data.remote.api

import com.flowboard.data.local.entities.DocumentEntity
import com.flowboard.data.remote.dto.*
import com.flowboard.data.remote.ApiConfig
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DocumentApiService @Inject constructor(
    private val httpClient: HttpClient
) {
    companion object {
        private val DOCUMENTS_ENDPOINT = "${ApiConfig.API_BASE_URL}/documents"
    }

    suspend fun getAllDocuments(): DocumentListResponse {
        return httpClient.get(DOCUMENTS_ENDPOINT).body<DocumentListResponse>()
    }

    suspend fun getDocumentById(id: String): DocumentEntity {
        return httpClient.get("$DOCUMENTS_ENDPOINT/$id").body<DocumentDto>().toEntity()
    }

    suspend fun createDocument(title: String, content: String = "", isPublic: Boolean = false): DocumentEntity {
        return httpClient.post(DOCUMENTS_ENDPOINT) {
            contentType(ContentType.Application.Json)
            setBody(CreateDocumentRequest(title, content, isPublic))
        }.body<DocumentDto>().toEntity()
    }

    suspend fun updateDocument(id: String, title: String? = null, content: String? = null, isPublic: Boolean? = null): DocumentEntity {
        return httpClient.put("$DOCUMENTS_ENDPOINT/$id") {
            contentType(ContentType.Application.Json)
            setBody(UpdateDocumentRequest(title, content, isPublic))
        }.body<DocumentDto>().toEntity()
    }

    suspend fun deleteDocument(id: String) {
        httpClient.delete("$DOCUMENTS_ENDPOINT/$id")
    }

    suspend fun shareDocument(documentId: String, email: String, role: String) {
        httpClient.post("$DOCUMENTS_ENDPOINT/$documentId/share") {
            contentType(ContentType.Application.Json)
            setBody(ShareDocumentRequest(email, role))
        }
    }

    suspend fun getDocumentPermissions(documentId: String): List<DocumentPermissionDto> {
        return httpClient.get("$DOCUMENTS_ENDPOINT/$documentId/permissions").body<List<DocumentPermissionDto>>()
    }
}
