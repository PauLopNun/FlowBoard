package com.flowboard.utils

import com.flowboard.domain.model.PermissionLevel
import com.flowboard.domain.model.PermissionListResponse
import com.flowboard.domain.model.ResourceType
import com.flowboard.domain.repository.PermissionRepository

/**
 * Utility class for checking permissions
 */
class PermissionChecker(
    private val permissionRepository: PermissionRepository
) {

    /**
     * Check if a user can edit a resource
     */
    suspend fun canEdit(
        resourceId: String,
        resourceType: ResourceType,
        userId: String
    ): Boolean {
        return permissionRepository.hasPermission(
            resourceId = resourceId,
            resourceType = resourceType,
            userId = userId,
            requiredLevel = PermissionLevel.EDITOR
        ).getOrDefault(false)
    }

    /**
     * Check if a user can view a resource
     */
    suspend fun canView(
        resourceId: String,
        resourceType: ResourceType,
        userId: String
    ): Boolean {
        return permissionRepository.hasPermission(
            resourceId = resourceId,
            resourceType = resourceType,
            userId = userId,
            requiredLevel = PermissionLevel.VIEWER
        ).getOrDefault(false)
    }

    /**
     * Check if a user can admin a resource
     */
    suspend fun canAdmin(
        resourceId: String,
        resourceType: ResourceType,
        userId: String
    ): Boolean {
        return permissionRepository.hasPermission(
            resourceId = resourceId,
            resourceType = resourceType,
            userId = userId,
            requiredLevel = PermissionLevel.ADMIN
        ).getOrDefault(false)
    }

    /**
     * Check if a user is the owner of a resource
     */
    fun isOwner(permissionsList: PermissionListResponse?, userId: String): Boolean {
        return permissionsList?.owner?.userId == userId
    }

    /**
     * Get the user's permission level for a resource
     */
    fun getUserPermissionLevel(
        permissionsList: PermissionListResponse?,
        userId: String
    ): PermissionLevel {
        if (permissionsList == null) return PermissionLevel.NONE

        // Check if owner
        if (permissionsList.owner.userId == userId) {
            return permissionsList.owner.permissionLevel
        }

        // Check collaborators
        val collaborator = permissionsList.collaborators.find { it.userId == userId }
        return collaborator?.permissionLevel ?: PermissionLevel.NONE
    }

    /**
     * Check if a permission level is sufficient
     */
    fun hasPermissionLevel(
        userLevel: PermissionLevel,
        requiredLevel: PermissionLevel
    ): Boolean {
        val levels = listOf(
            PermissionLevel.NONE,
            PermissionLevel.VIEWER,
            PermissionLevel.COMMENTER,
            PermissionLevel.EDITOR,
            PermissionLevel.ADMIN,
            PermissionLevel.OWNER
        )

        val userIndex = levels.indexOf(userLevel)
        val requiredIndex = levels.indexOf(requiredLevel)

        return userIndex >= requiredIndex
    }
}

/**
 * Extension functions for permission checking
 */
fun PermissionLevel.canView(): Boolean = this != PermissionLevel.NONE

fun PermissionLevel.canComment(): Boolean =
    hasPermissionLevel(this, PermissionLevel.COMMENTER)

fun PermissionLevel.canEdit(): Boolean =
    hasPermissionLevel(this, PermissionLevel.EDITOR)

fun PermissionLevel.canAdmin(): Boolean =
    hasPermissionLevel(this, PermissionLevel.ADMIN)

fun PermissionLevel.isOwner(): Boolean = this == PermissionLevel.OWNER

private fun hasPermissionLevel(
    userLevel: PermissionLevel,
    requiredLevel: PermissionLevel
): Boolean {
    val levels = listOf(
        PermissionLevel.NONE,
        PermissionLevel.VIEWER,
        PermissionLevel.COMMENTER,
        PermissionLevel.EDITOR,
        PermissionLevel.ADMIN,
        PermissionLevel.OWNER
    )

    val userIndex = levels.indexOf(userLevel)
    val requiredIndex = levels.indexOf(requiredLevel)

    return userIndex >= requiredIndex
}
