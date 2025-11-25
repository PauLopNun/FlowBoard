package com.flowboard.domain.repository

import com.flowboard.domain.model.*
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing permissions
 */
interface PermissionRepository {
    /**
     * Grant permission to a user
     */
    suspend fun grantPermission(request: GrantPermissionRequest): Result<Permission>

    /**
     * Update an existing permission
     */
    suspend fun updatePermission(request: UpdatePermissionRequest): Result<Permission>

    /**
     * Revoke a permission
     */
    suspend fun revokePermission(permissionId: String): Result<Unit>

    /**
     * Get all permissions for a resource
     */
    suspend fun getResourcePermissions(
        resourceId: String,
        resourceType: ResourceType
    ): Result<PermissionListResponse>

    /**
     * Get all resources a user has access to
     */
    suspend fun getUserPermissions(userId: String): Result<List<Permission>>

    /**
     * Check if a user has the required permission level
     */
    suspend fun hasPermission(
        resourceId: String,
        resourceType: ResourceType,
        userId: String,
        requiredLevel: PermissionLevel
    ): Result<Boolean>

    /**
     * Observe permissions for a resource in real-time
     */
    fun observeResourcePermissions(
        resourceId: String,
        resourceType: ResourceType
    ): Flow<PermissionListResponse>
}
