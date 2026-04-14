package com.flowboard.data.remote.dto

import com.flowboard.data.local.entities.WorkspaceEntity
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class WorkspaceDto(
    val id: String,
    val name: String,
    val description: String? = null,
    val ownerId: String,
    val ownerName: String? = null,
    val inviteCode: String,
    val members: List<WorkspaceMemberDto> = emptyList(),
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    fun toEntity() = WorkspaceEntity(
        id = id,
        name = name,
        description = description,
        ownerId = ownerId,
        ownerName = ownerName,
        inviteCode = inviteCode,
        memberCount = members.size,
        createdAt = createdAt.toString(),
        updatedAt = updatedAt.toString()
    )
}

@Serializable
data class WorkspaceMemberDto(
    val userId: String,
    val userName: String? = null,
    val userEmail: String? = null,
    val role: String,
    val joinedAt: LocalDateTime
)

@Serializable
data class WorkspaceListResponseDto(
    val owned: List<WorkspaceDto>,
    val member: List<WorkspaceDto>
)

@Serializable
data class CreateWorkspaceRequest(
    val name: String,
    val description: String? = null
)

@Serializable
data class JoinWorkspaceRequest(
    val inviteCode: String
)
