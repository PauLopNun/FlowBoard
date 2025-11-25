package com.flowboard.data.remote.api

import com.flowboard.data.remote.dto.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import javax.inject.Inject

class PermissionApiServiceImpl @Inject constructor(
    private val client: HttpClient
) : PermissionApiService {

    private val baseUrl = "http://10.0.2.2:8080" // Android emulator localhost

    override suspend fun grantPermission(request: GrantPermissionRequestDto): Result<PermissionDto> {
        return try {
            val response: HttpResponse = client.post("$baseUrl/api/permissions/grant") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            if (response.status.isSuccess()) {
                Result.success(response.body<PermissionDto>())
            } else {
                Result.failure(Exception("Failed to grant permission: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updatePermission(request: UpdatePermissionRequestDto): Result<PermissionDto> {
        return try {
            val response: HttpResponse = client.put("$baseUrl/api/permissions/update") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            if (response.status.isSuccess()) {
                Result.success(response.body<PermissionDto>())
            } else {
                Result.failure(Exception("Failed to update permission: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun revokePermission(permissionId: String): Result<Unit> {
        return try {
            val response: HttpResponse = client.delete("$baseUrl/api/permissions/$permissionId")

            if (response.status.isSuccess()) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to revoke permission: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getResourcePermissions(
        resourceId: String,
        resourceType: String
    ): Result<PermissionListResponseDto> {
        return try {
            val response: HttpResponse = client.get("$baseUrl/api/permissions/resource/$resourceType/$resourceId")

            if (response.status.isSuccess()) {
                Result.success(response.body<PermissionListResponseDto>())
            } else {
                Result.failure(Exception("Failed to get resource permissions: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getUserPermissions(userId: String): Result<List<PermissionDto>> {
        return try {
            val response: HttpResponse = client.get("$baseUrl/api/permissions/user/$userId")

            if (response.status.isSuccess()) {
                Result.success(response.body<List<PermissionDto>>())
            } else {
                Result.failure(Exception("Failed to get user permissions: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun checkPermission(
        resourceId: String,
        resourceType: String,
        userId: String,
        requiredLevel: String
    ): Result<Boolean> {
        return try {
            val response: HttpResponse = client.get("$baseUrl/api/permissions/check") {
                parameter("resourceId", resourceId)
                parameter("resourceType", resourceType)
                parameter("userId", userId)
                parameter("requiredLevel", requiredLevel)
            }

            if (response.status.isSuccess()) {
                Result.success(response.body<Boolean>())
            } else {
                Result.failure(Exception("Failed to check permission: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun inviteUser(boardId: String, inviteRequest: InviteRequest) {
        client.post("$baseUrl/boards/$boardId/invite") {
            contentType(ContentType.Application.Json)
            setBody(inviteRequest)
        }
    }
}
