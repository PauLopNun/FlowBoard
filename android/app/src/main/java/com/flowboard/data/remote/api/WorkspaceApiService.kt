package com.flowboard.data.remote.api

import android.util.Log
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

    suspend fun createWorkspace(name: String, description: String?, imageUrl: String? = null): WorkspaceDto {
        return httpClient.post(endpoint) {
            header(HttpHeaders.Authorization, "Bearer ${token()}")
            contentType(ContentType.Application.Json)
            setBody(CreateWorkspaceRequest(name, description, imageUrl))
        }.body()
    }

    suspend fun updateWorkspace(id: String, name: String?, description: String?, imageUrl: String?): WorkspaceDto {
        Log.d("WorkspaceApi", "updateWorkspace id=$id imageUrl=$imageUrl")
        val resp = httpClient.put("$endpoint/$id") {
            header(HttpHeaders.Authorization, "Bearer ${token()}")
            contentType(ContentType.Application.Json)
            setBody(UpdateWorkspaceRequest(name, description, imageUrl))
        }
        Log.d("WorkspaceApi", "updateWorkspace status=${resp.status}")
        if (!resp.status.isSuccess()) {
            val body = try { resp.body<String>() } catch (_: Exception) { "" }
            Log.e("WorkspaceApi", "updateWorkspace error ${resp.status.value}: $body")
            throw Exception("Server error ${resp.status.value}: ${body.take(120)}")
        }
        return resp.body()
    }

    suspend fun joinWorkspace(inviteCode: String): WorkspaceDto {
        return httpClient.post("$endpoint/join") {
            header(HttpHeaders.Authorization, "Bearer ${token()}")
            contentType(ContentType.Application.Json)
            setBody(JoinWorkspaceRequest(inviteCode))
        }.body()
    }

    suspend fun inviteMember(workspaceId: String, email: String): InviteWorkspaceResponseDto {
        return httpClient.post("$endpoint/$workspaceId/invite") {
            header(HttpHeaders.Authorization, "Bearer ${token()}")
            contentType(ContentType.Application.Json)
            setBody(InviteWorkspaceRequest(email))
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
