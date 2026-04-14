package com.flowboard.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workspaces")
data class WorkspaceEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String? = null,
    val ownerId: String,
    val ownerName: String? = null,
    val inviteCode: String,
    val memberCount: Int = 1,
    val createdAt: String,
    val updatedAt: String
)
