package com.flowboard.data.remote.api

import com.flowboard.data.remote.dto.*

interface PermissionApiService {
    /**
     * Grant permission to a user for a resource
     */
    suspend fun grantPermission(request: GrantPermissionRequestDto): Result<PermissionDto>

    /**
     * Update an existing permission level
     */
    suspend fun updatePermission(request: UpdatePermissionRequestDto): Result<PermissionDto>

    /**
     * Revoke a permission
     */
    suspend fun revokePermission(permissionId: String): Result<Unit>

    /**
     * Get all permissions for a resource
     */
    suspend fun getResourcePermissions(
        resourceId: String,
        resourceType: String
    ): Result<PermissionListResponseDto>

    /**
     * Get all resources a user has access to
     */
    suspend fun getUserPermissions(userId: String): Result<List<PermissionDto>>

    /**
     * Check if a user has a specific permission level on a resource
     */
    suspend fun checkPermission(
        resourceId: String,
        resourceType: String,
        userId: String,
        requiredLevel: String
    ): Result<Boolean>

    /**
     * Invite a user to a board (legacy method)
     */
    suspend fun inviteUser(
        boardId: String,
        inviteRequest: InviteRequest
    )
}
