package com.flowboard.data.models

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class Workspace(
    val id: String,
    val name: String,
    val description: String? = null,
    val ownerId: String,
    val ownerName: String? = null,
    val inviteCode: String,
    val members: List<WorkspaceMember> = emptyList(),
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

@Serializable
data class WorkspaceMember(
    val userId: String,
    val userName: String? = null,
    val userEmail: String? = null,
    val role: String,
    val joinedAt: LocalDateTime
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

@Serializable
data class WorkspaceListResponse(
    val owned: List<Workspace>,
    val member: List<Workspace>
)
