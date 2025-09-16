package com.flowboard.domain.model

import kotlinx.datetime.LocalDateTime
import com.flowboard.data.local.entities.TaskPriority

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
    val location: String? = null
)