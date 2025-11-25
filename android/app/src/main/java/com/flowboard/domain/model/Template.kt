package com.flowboard.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Template types
 */
enum class TemplateType {
    TASK,           // Task template
    EVENT,          // Event template
    PROJECT,        // Project template
    CHECKLIST       // Checklist template
}

/**
 * Template category for organization
 */
enum class TemplateCategory {
    PERSONAL,
    WORK,
    MEETING,
    PROJECT_PLANNING,
    DEVELOPMENT,
    MARKETING,
    DESIGN,
    SALES,
    SUPPORT,
    ONBOARDING,
    REVIEW,
    CUSTOM
}

/**
 * Recurrence pattern for templates
 */
enum class RecurrencePattern {
    NONE,
    DAILY,
    WEEKLY,
    BIWEEKLY,
    MONTHLY,
    QUARTERLY,
    YEARLY
}

/**
 * Task template model
 */
@Parcelize
data class TaskTemplate(
    val id: String,
    val name: String,
    val description: String?,
    val type: TemplateType,
    val category: TemplateCategory,
    val icon: String?,
    val color: String?,

    // Task details
    val priority: String?,           // high, medium, low
    val estimatedDuration: Int?,     // in minutes
    val tags: List<String>,

    // Subtasks
    val subtasks: List<SubtaskTemplate>,

    // Fields
    val customFields: List<CustomFieldTemplate>,

    // Recurrence
    val recurrencePattern: RecurrencePattern,
    val recurrenceInterval: Int?,    // e.g., every N days/weeks

    // Metadata
    val isPublic: Boolean,           // Shared with team
    val usageCount: Int,             // Times used
    val createdBy: String,
    val createdAt: Long,
    val updatedAt: Long,
    val lastUsedAt: Long?
) : Parcelable

/**
 * Subtask template
 */
@Parcelize
data class SubtaskTemplate(
    val id: String,
    val title: String,
    val description: String?,
    val estimatedDuration: Int?,
    val order: Int
) : Parcelable

/**
 * Custom field template
 */
@Parcelize
data class CustomFieldTemplate(
    val id: String,
    val name: String,
    val type: FieldType,
    val defaultValue: String?,
    val options: List<String>?,      // For select/multiselect fields
    val isRequired: Boolean,
    val order: Int
) : Parcelable

/**
 * Field types for custom fields
 */
enum class FieldType {
    TEXT,
    NUMBER,
    DATE,
    TIME,
    DATETIME,
    SELECT,
    MULTISELECT,
    CHECKBOX,
    URL,
    EMAIL,
    PHONE
}

/**
 * Template application result
 */
data class TemplateApplicationResult(
    val taskId: String?,
    val success: Boolean,
    val message: String
)

/**
 * Template statistics
 */
data class TemplateStats(
    val templateId: String,
    val usageCount: Int,
    val averageCompletionTime: Long?,
    val successRate: Float,
    val lastUsedAt: Long?,
    val popularityScore: Float
)

/**
 * Template filter options
 */
data class TemplateFilter(
    val type: TemplateType? = null,
    val category: TemplateCategory? = null,
    val searchQuery: String? = null,
    val isPublic: Boolean? = null,
    val createdBy: String? = null,
    val sortBy: TemplateSortOption = TemplateSortOption.RECENT
)

/**
 * Template sort options
 */
enum class TemplateSortOption {
    RECENT,         // Recently updated
    POPULAR,        // Most used
    NAME,           // Alphabetical
    CREATED_DATE,   // Creation date
    LAST_USED       // Last used date
}

/**
 * Template share options
 */
data class TemplateShareOptions(
    val isPublic: Boolean,
    val sharedWithUserIds: List<String>,
    val sharedWithTeamIds: List<String>,
    val canEdit: Boolean,
    val canCopy: Boolean
)

/**
 * Template import/export
 */
@Parcelize
data class TemplateExport(
    val template: TaskTemplate,
    val version: String,
    val exportedAt: Long,
    val exportedBy: String
) : Parcelable
