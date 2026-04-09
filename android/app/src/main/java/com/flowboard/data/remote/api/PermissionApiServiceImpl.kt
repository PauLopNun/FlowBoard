package com.flowboard.data.remote.api

import com.flowboard.data.remote.ApiConfig
import com.flowboard.data.remote.dto.*
import com.flowboard.data.repository.AuthRepository
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import javax.inject.Inject

class PermissionApiServiceImpl @Inject constructor(
    private val client: HttpClient,
    private val authRepository: AuthRepository
) : PermissionApiService {

    private val baseUrl = "${ApiConfig.API_BASE_URL}/permissions"

    private suspend fun authHeader() =
        authRepository.getToken()?.let { "Bearer $it" } ?: throw Exception("Not authenticated")

    override suspend fun grantPermission(request: GrantPermissionRequestDto): Result<PermissionDto> =
        runCatching {
            val response: HttpResponse = client.post("$baseUrl/grant") {
                header(HttpHeaders.Authorization, authHeader())
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            if (response.status.isSuccess()) response.body()
            else throw Exception("Failed to grant permission: ${response.status}")
        }

    override suspend fun updatePermission(request: UpdatePermissionRequestDto): Result<PermissionDto> =
        runCatching {
            val response: HttpResponse = client.put("$baseUrl/update") {
                header(HttpHeaders.Authorization, authHeader())
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            if (response.status.isSuccess()) response.body()
            else throw Exception("Failed to update permission: ${response.status}")
        }

    override suspend fun revokePermission(permissionId: String): Result<Unit> =
        runCatching {
            val response: HttpResponse = client.delete("$baseUrl/$permissionId") {
                header(HttpHeaders.Authorization, authHeader())
            }
            if (!response.status.isSuccess()) throw Exception("Failed to revoke permission: ${response.status}")
        }

    override suspend fun getResourcePermissions(
        resourceId: String,
        resourceType: String
    ): Result<PermissionListResponseDto> =
        runCatching {
            val response: HttpResponse = client.get("$baseUrl/resource/$resourceType/$resourceId") {
                header(HttpHeaders.Authorization, authHeader())
            }
            if (response.status.isSuccess()) response.body()
            else throw Exception("Failed to get permissions: ${response.status}")
        }

    override suspend fun getUserPermissions(userId: String): Result<List<PermissionDto>> =
        runCatching {
            val response: HttpResponse = client.get("$baseUrl/user/$userId") {
                header(HttpHeaders.Authorization, authHeader())
            }
            if (response.status.isSuccess()) response.body()
            else throw Exception("Failed to get user permissions: ${response.status}")
        }

    override suspend fun checkPermission(
        resourceId: String,
        resourceType: String,
        userId: String,
        requiredLevel: String
    ): Result<Boolean> =
        runCatching {
            val response: HttpResponse = client.get("$baseUrl/check") {
                header(HttpHeaders.Authorization, authHeader())
                parameter("resourceId", resourceId)
                parameter("resourceType", resourceType)
                parameter("userId", userId)
                parameter("requiredLevel", requiredLevel)
            }
            if (response.status.isSuccess()) response.body()
            else throw Exception("Permission check failed: ${response.status}")
        }

    override suspend fun inviteUser(boardId: String, inviteRequest: InviteRequest) {
        client.post("${ApiConfig.API_BASE_URL}/boards/$boardId/invite") {
            header(HttpHeaders.Authorization, authHeader())
            contentType(ContentType.Application.Json)
            setBody(inviteRequest)
        }
    }
}
