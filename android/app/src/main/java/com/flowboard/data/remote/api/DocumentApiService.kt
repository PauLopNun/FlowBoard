package com.flowboard.data.remote.api

import com.flowboard.data.local.entities.DocumentEntity
import com.flowboard.data.remote.dto.*
import com.flowboard.data.remote.ApiConfig
import com.flowboard.data.repository.AuthRepository
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DocumentApiService @Inject constructor(
    private val httpClient: HttpClient,
    private val authRepository: AuthRepository
) {
    companion object {
        private val DOCUMENTS_ENDPOINT = "${ApiConfig.API_BASE_URL}/documents"
    }

    private suspend fun getAuthToken(): String? {
        return authRepository.getToken()
    }

    suspend fun getAllDocuments(): DocumentListResponse {
        val token = getAuthToken() ?: throw Exception("Not authenticated")
        val response = httpClient.get(DOCUMENTS_ENDPOINT) {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        if (!response.status.isSuccess()) {
            throw Exception("Server error ${response.status.value}")
        }
        return response.body<DocumentListResponse>()
    }

    suspend fun getDocumentById(id: String): DocumentEntity {
        val token = getAuthToken() ?: throw Exception("Not authenticated")
        return httpClient.get("$DOCUMENTS_ENDPOINT/$id") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }.body<DocumentDto>().toEntity()
    }

    suspend fun createDocument(title: String, content: String = "", isPublic: Boolean = false, parentId: String? = null): DocumentEntity {
        val token = getAuthToken() ?: throw Exception("Not authenticated")
        return httpClient.post(DOCUMENTS_ENDPOINT) {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(CreateDocumentRequest(title, content, isPublic, parentId))
        }.body<DocumentDto>().toEntity()
    }

    suspend fun getChildDocuments(parentId: String): List<DocumentEntity> {
        val token = getAuthToken() ?: throw Exception("Not authenticated")
        return httpClient.get("$DOCUMENTS_ENDPOINT/$parentId/children") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }.body<List<DocumentDto>>().map { it.toEntity() }
    }

    suspend fun updateDocument(id: String, title: String? = null, content: String? = null, isPublic: Boolean? = null): DocumentEntity {
        val token = getAuthToken() ?: throw Exception("Not authenticated")
        return httpClient.put("$DOCUMENTS_ENDPOINT/$id") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(UpdateDocumentRequest(title, content, isPublic))
        }.body<DocumentDto>().toEntity()
    }

    suspend fun deleteDocument(id: String) {
        val token = getAuthToken() ?: throw Exception("Not authenticated")
        httpClient.delete("$DOCUMENTS_ENDPOINT/$id") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
    }

    suspend fun shareDocument(documentId: String, email: String, role: String) {
        val token = getAuthToken() ?: throw Exception("Not authenticated")
        httpClient.post("$DOCUMENTS_ENDPOINT/$documentId/share") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(ShareDocumentRequest(email, role))
        }
    }

    suspend fun getDocumentPermissions(documentId: String): List<DocumentPermissionDto> {
        val token = getAuthToken() ?: throw Exception("Not authenticated")
        return httpClient.get("$DOCUMENTS_ENDPOINT/$documentId/permissions") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }.body<List<DocumentPermissionDto>>()
    }
}
