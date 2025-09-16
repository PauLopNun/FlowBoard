package com.flowboard.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

@Entity(tableName = "projects")
@Serializable
data class ProjectEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String,
    val color: String = "#2196F3",
    val ownerId: String,
    val members: List<String> = emptyList(),
    val isActive: Boolean = true,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val deadline: LocalDateTime? = null,
    val isSync: Boolean = true,
    val lastSyncAt: LocalDateTime? = null
)