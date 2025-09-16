package com.flowboard.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

@Entity(tableName = "tasks")
@Serializable
data class TaskEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String,
    val isCompleted: Boolean = false,
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val dueDate: LocalDateTime? = null,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val assignedTo: String? = null,
    val projectId: String? = null,
    val tags: List<String> = emptyList(),
    val attachments: List<String> = emptyList(),
    val isEvent: Boolean = false,
    val eventStartTime: LocalDateTime? = null,
    val eventEndTime: LocalDateTime? = null,
    val location: String? = null,
    val isSync: Boolean = true,
    val lastSyncAt: LocalDateTime? = null
)

enum class TaskPriority {
    LOW, MEDIUM, HIGH, URGENT
}