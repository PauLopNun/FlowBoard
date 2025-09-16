package com.flowboard.data.models

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class Task(
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
    val createdBy: String
)

@Serializable
enum class TaskPriority {
    LOW, MEDIUM, HIGH, URGENT
}

@Serializable
data class CreateTaskRequest(
    val title: String,
    val description: String,
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val dueDate: LocalDateTime? = null,
    val assignedTo: String? = null,
    val projectId: String? = null,
    val tags: List<String> = emptyList(),
    val isEvent: Boolean = false,
    val eventStartTime: LocalDateTime? = null,
    val eventEndTime: LocalDateTime? = null,
    val location: String? = null
)

@Serializable
data class UpdateTaskRequest(
    val title: String? = null,
    val description: String? = null,
    val isCompleted: Boolean? = null,
    val priority: TaskPriority? = null,
    val dueDate: LocalDateTime? = null,
    val assignedTo: String? = null,
    val projectId: String? = null,
    val tags: List<String>? = null,
    val attachments: List<String>? = null,
    val isEvent: Boolean? = null,
    val eventStartTime: LocalDateTime? = null,
    val eventEndTime: LocalDateTime? = null,
    val location: String? = null
)