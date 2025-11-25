package com.flowboard.domain.model

/**
 * Represents different permission levels in the system
 */
enum class PermissionLevel {
    NONE,       // No access
    VIEWER,     // Can only view
    COMMENTER,  // Can view and comment
    EDITOR,     // Can view, comment, and edit
    ADMIN,      // Full access including sharing and deletion
    OWNER       // Full access + ownership transfer
}

/**
 * Represents a permission granted to a user for a specific resource
 */
data class Permission(
    val id: String,
    val resourceId: String,      // ID of the document, task, or project
    val resourceType: ResourceType,
    val userId: String,
    val userEmail: String,
    val userName: String,
    val level: PermissionLevel,
    val grantedBy: String,       // User ID who granted this permission
    val grantedAt: Long,
    val expiresAt: Long? = null  // Optional expiration timestamp
)

/**
 * Type of resource the permission applies to
 */
enum class ResourceType {
    DOCUMENT,
    TASK,
    PROJECT,
    BOARD
}

/**
 * Request to grant a permission
 */
data class GrantPermissionRequest(
    val resourceId: String,
    val resourceType: ResourceType,
    val userEmail: String,
    val level: PermissionLevel,
    val expiresAt: Long? = null
)

/**
 * Request to update an existing permission
 */
data class UpdatePermissionRequest(
    val permissionId: String,
    val newLevel: PermissionLevel
)

/**
 * Response when listing permissions for a resource
 */
data class PermissionListResponse(
    val resourceId: String,
    val resourceType: ResourceType,
    val owner: UserPermissionInfo,
    val collaborators: List<UserPermissionInfo>
)

/**
 * User permission information
 */
data class UserPermissionInfo(
    val userId: String,
    val email: String,
    val userName: String,
    val permissionLevel: PermissionLevel,
    val grantedAt: Long,
    val isOnline: Boolean = false
)
