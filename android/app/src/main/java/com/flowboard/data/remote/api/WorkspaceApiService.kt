package com.flowboard.data.remote.api

import com.flowboard.data.remote.ApiConfig
import com.flowboard.data.remote.dto.*
import com.flowboard.data.repository.AuthRepository
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkspaceApiService @Inject constructor(
    private val httpClient: HttpClient,
    private val authRepository: AuthRepository
) {
    private val endpoint = "${ApiConfig.API_BASE_URL}/workspaces"
    private suspend fun token() = authRepository.getToken() ?: throw Exception("Not authenticated")

    suspend fun getWorkspaces(): WorkspaceListResponseDto {
        return httpClient.get(endpoint) {
            header(HttpHeaders.Authorization, "Bearer ${token()}")
        }.body()
    }

    suspend fun getWorkspace(id: String): WorkspaceDto {
        return httpClient.get("$endpoint/$id") {
            header(HttpHeaders.Authorization, "Bearer ${token()}")
        }.body()
    }

    suspend fun createWorkspace(name: String, description: String?): WorkspaceDto {
        return httpClient.post(endpoint) {
            header(HttpHeaders.Authorization, "Bearer ${token()}")
            contentType(ContentType.Application.Json)
            setBody(CreateWorkspaceRequest(name, description))
        }.body()
    }

    suspend fun joinWorkspace(inviteCode: String): WorkspaceDto {
        return httpClient.post("$endpoint/join") {
            header(HttpHeaders.Authorization, "Bearer ${token()}")
            contentType(ContentType.Application.Json)
            setBody(JoinWorkspaceRequest(inviteCode))
        }.body()
    }

    suspend fun deleteWorkspace(id: String) {
        httpClient.delete("$endpoint/$id") {
            header(HttpHeaders.Authorization, "Bearer ${token()}")
        }
    }

    suspend fun removeMember(workspaceId: String, userId: String) {
        httpClient.delete("$endpoint/$workspaceId/members/$userId") {
            header(HttpHeaders.Authorization, "Bearer ${token()}")
        }
    }
}
