package com.flowboard.data.repository

import com.flowboard.data.remote.api.PermissionApiService
import com.flowboard.domain.model.*
import com.flowboard.domain.repository.PermissionRepository
import com.flowboard.utils.toDomain
import com.flowboard.utils.toDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PermissionRepositoryImpl @Inject constructor(
    private val permissionApiService: PermissionApiService
) : PermissionRepository {

    // Cache for permission list responses
    private val permissionsCache = mutableMapOf<String, MutableStateFlow<PermissionListResponse?>>()

    override suspend fun grantPermission(request: GrantPermissionRequest): Result<Permission> {
        return permissionApiService.grantPermission(request.toDto())
            .map { it.toDomain() }
            .also { result ->
                // Update cache if successful
                if (result.isSuccess) {
                    refreshResourcePermissions(request.resourceId, request.resourceType)
                }
            }
    }

    override suspend fun updatePermission(request: UpdatePermissionRequest): Result<Permission> {
        return permissionApiService.updatePermission(request.toDto())
            .map { it.toDomain() }
    }

    override suspend fun revokePermission(permissionId: String): Result<Unit> {
        return permissionApiService.revokePermission(permissionId)
    }

    override suspend fun getResourcePermissions(
        resourceId: String,
        resourceType: ResourceType
    ): Result<PermissionListResponse> {
        return permissionApiService.getResourcePermissions(
            resourceId = resourceId,
            resourceType = resourceType.name.lowercase()
        ).map { it.toDomain() }
            .also { result ->
                // Update cache
                if (result.isSuccess) {
                    val key = getCacheKey(resourceId, resourceType)
                    permissionsCache.getOrPut(key) {
                        MutableStateFlow(null)
                    }.value = result.getOrNull()
                }
            }
    }

    override suspend fun getUserPermissions(userId: String): Result<List<Permission>> {
        return permissionApiService.getUserPermissions(userId)
            .map { list -> list.map { it.toDomain() } }
    }

    override suspend fun hasPermission(
        resourceId: String,
        resourceType: ResourceType,
        userId: String,
        requiredLevel: PermissionLevel
    ): Result<Boolean> {
        return permissionApiService.checkPermission(
            resourceId = resourceId,
            resourceType = resourceType.name.lowercase(),
            userId = userId,
            requiredLevel = requiredLevel.name.lowercase()
        )
    }

    override fun observeResourcePermissions(
        resourceId: String,
        resourceType: ResourceType
    ): Flow<PermissionListResponse> {
        val key = getCacheKey(resourceId, resourceType)
        val flow = permissionsCache.getOrPut(key) {
            MutableStateFlow(null)
        }

        // TODO: Implement WebSocket-based real-time updates

        return flow.asStateFlow() as Flow<PermissionListResponse>
    }

    private suspend fun refreshResourcePermissions(
        resourceId: String,
        resourceType: ResourceType
    ) {
        getResourcePermissions(resourceId, resourceType)
    }

    private fun getCacheKey(resourceId: String, resourceType: ResourceType): String {
        return "${resourceType.name}:$resourceId"
    }
}
