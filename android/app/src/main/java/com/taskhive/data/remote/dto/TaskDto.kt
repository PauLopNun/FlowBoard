package com.flowboard.data.remote.dto

import com.flowboard.data.local.entities.TaskEntity
import com.flowboard.data.local.entities.TaskPriority
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class TaskDto(
    val id: String,
    val title: String,
    val description: String,
    val isCompleted: Boolean = false,
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val dueDate: String? = null,
    val createdAt: String,
    val updatedAt: String,
    val assignedTo: String? = null,
    val projectId: String? = null,
    val tags: List<String> = emptyList(),
    val attachments: List<String> = emptyList(),
    val isEvent: Boolean = false,
    val eventStartTime: String? = null,
    val eventEndTime: String? = null,
    val location: String? = null
) {
    fun toEntity(): TaskEntity {
        return TaskEntity(
            id = id,
            title = title,
            description = description,
            isCompleted = isCompleted,
            priority = priority,
            dueDate = dueDate?.let { LocalDateTime.parse(it) },
            createdAt = LocalDateTime.parse(createdAt),
            updatedAt = LocalDateTime.parse(updatedAt),
            assignedTo = assignedTo,
            projectId = projectId,
            tags = tags,
            attachments = attachments,
            isEvent = isEvent,
            eventStartTime = eventStartTime?.let { LocalDateTime.parse(it) },
            eventEndTime = eventEndTime?.let { LocalDateTime.parse(it) },
            location = location,
            isSync = true,
            lastSyncAt = LocalDateTime.parse(updatedAt)
        )
    }
    
    companion object {
        fun fromEntity(entity: TaskEntity): TaskDto {
            return TaskDto(
                id = entity.id,
                title = entity.title,
                description = entity.description,
                isCompleted = entity.isCompleted,
                priority = entity.priority,
                dueDate = entity.dueDate?.toString(),
                createdAt = entity.createdAt.toString(),
                updatedAt = entity.updatedAt.toString(),
                assignedTo = entity.assignedTo,
                projectId = entity.projectId,
                tags = entity.tags,
                attachments = entity.attachments,
                isEvent = entity.isEvent,
                eventStartTime = entity.eventStartTime?.toString(),
                eventEndTime = entity.eventEndTime?.toString(),
                location = entity.location
            )
        }
    }
}