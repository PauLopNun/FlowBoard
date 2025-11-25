package com.flowboard.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class PermissionDto(
    val id: String,
    val resourceId: String,
    val resourceType: String,
    val userId: String,
    val userEmail: String,
    val userName: String,
    val level: String,
    val grantedBy: String,
    val grantedAt: Long,
    val expiresAt: Long? = null
)

@Serializable
data class GrantPermissionRequestDto(
    val resourceId: String,
    val resourceType: String,
    val userEmail: String,
    val level: String,
    val expiresAt: Long? = null
)

@Serializable
data class UpdatePermissionRequestDto(
    val permissionId: String,
    val newLevel: String
)

@Serializable
data class PermissionListResponseDto(
    val resourceId: String,
    val resourceType: String,
    val owner: UserPermissionInfoDto,
    val collaborators: List<UserPermissionInfoDto>
)

@Serializable
data class UserPermissionInfoDto(
    val userId: String,
    val email: String,
    val userName: String,
    val permissionLevel: String,
    val grantedAt: Long,
    val isOnline: Boolean = false
)

@Serializable
data class RevokePermissionRequest(
    val permissionId: String
)
