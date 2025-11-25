package com.flowboard.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

/**
 * Task template entity for Room database
 */
@Entity(
    tableName = "task_templates",
    indices = [
        Index(value = ["createdBy"]),
        Index(value = ["category"]),
        Index(value = ["type"]),
        Index(value = ["isPublic"]),
        Index(value = ["usageCount"])
    ]
)
data class TaskTemplateEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String?,
    val type: String,                // TemplateType as string
    val category: String,            // TemplateCategory as string
    val icon: String?,
    val color: String?,

    // Task details
    val priority: String?,
    val estimatedDuration: Int?,
    val tags: List<String>,

    // Recurrence
    val recurrencePattern: String,   // RecurrencePattern as string
    val recurrenceInterval: Int?,

    // Sharing
    val isPublic: Boolean,
    val sharedWithUserIds: List<String>,
    val sharedWithTeamIds: List<String>,
    val canEdit: Boolean,
    val canCopy: Boolean,

    // Metadata
    val usageCount: Int,
    val createdBy: String,
    val createdAt: Long,
    val updatedAt: Long,
    val lastUsedAt: Long?
)

/**
 * Subtask template entity
 */
@Entity(
    tableName = "subtask_templates",
    indices = [
        Index(value = ["templateId"]),
        Index(value = ["order"])
    ]
)
data class SubtaskTemplateEntity(
    @PrimaryKey
    val id: String,
    val templateId: String,          // Foreign key to task_templates
    val title: String,
    val description: String?,
    val estimatedDuration: Int?,
    val order: Int
)

/**
 * Custom field template entity
 */
@Entity(
    tableName = "custom_field_templates",
    indices = [
        Index(value = ["templateId"]),
        Index(value = ["order"])
    ]
)
data class CustomFieldTemplateEntity(
    @PrimaryKey
    val id: String,
    val templateId: String,          // Foreign key to task_templates
    val name: String,
    val type: String,                // FieldType as string
    val defaultValue: String?,
    val options: List<String>?,      // For select/multiselect
    val isRequired: Boolean,
    val order: Int
)

/**
 * Template usage tracking
 */
@Entity(
    tableName = "template_usage",
    indices = [
        Index(value = ["templateId"]),
        Index(value = ["userId"]),
        Index(value = ["usedAt"])
    ]
)
data class TemplateUsageEntity(
    @PrimaryKey
    val id: String,
    val templateId: String,
    val userId: String,
    val taskId: String?,             // Created task ID
    val usedAt: Long,
    val completedAt: Long?,
    val wasSuccessful: Boolean
)

/**
 * Template stats cache
 */
@Entity(
    tableName = "template_stats",
    indices = [
        Index(value = ["templateId"], unique = true)
    ]
)
data class TemplateStatsEntity(
    @PrimaryKey
    val templateId: String,
    val usageCount: Int,
    val averageCompletionTime: Long?,
    val successRate: Float,
    val lastUsedAt: Long?,
    val popularityScore: Float,
    val updatedAt: Long
)
