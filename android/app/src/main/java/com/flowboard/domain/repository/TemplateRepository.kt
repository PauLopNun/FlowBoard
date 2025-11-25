package com.flowboard.domain.repository

import com.flowboard.domain.model.*
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for template functionality
 */
interface TemplateRepository {

    // ==================== Template CRUD ====================

    /**
     * Get all templates
     */
    fun getAllTemplates(): Flow<List<TaskTemplate>>

    /**
     * Get a specific template
     */
    fun getTemplate(templateId: String): Flow<TaskTemplate?>

    /**
     * Get templates by type
     */
    fun getTemplatesByType(type: TemplateType): Flow<List<TaskTemplate>>

    /**
     * Get templates by category
     */
    fun getTemplatesByCategory(category: TemplateCategory): Flow<List<TaskTemplate>>

    /**
     * Get user's templates
     */
    fun getUserTemplates(userId: String): Flow<List<TaskTemplate>>

    /**
     * Get public templates
     */
    fun getPublicTemplates(): Flow<List<TaskTemplate>>

    /**
     * Get popular templates
     */
    fun getPopularTemplates(limit: Int = 10): Flow<List<TaskTemplate>>

    /**
     * Get recently used templates
     */
    fun getRecentlyUsedTemplates(limit: Int = 10): Flow<List<TaskTemplate>>

    /**
     * Search templates
     */
    suspend fun searchTemplates(query: String): List<TaskTemplate>

    /**
     * Get filtered templates
     */
    fun getFilteredTemplates(filter: TemplateFilter): Flow<List<TaskTemplate>>

    /**
     * Create a new template
     */
    suspend fun createTemplate(
        name: String,
        description: String?,
        type: TemplateType,
        category: TemplateCategory,
        icon: String?,
        color: String?,
        priority: String?,
        estimatedDuration: Int?,
        tags: List<String>,
        subtasks: List<SubtaskTemplate>,
        customFields: List<CustomFieldTemplate>,
        recurrencePattern: RecurrencePattern,
        recurrenceInterval: Int?,
        isPublic: Boolean
    ): Result<TaskTemplate>

    /**
     * Update an existing template
     */
    suspend fun updateTemplate(template: TaskTemplate): Result<TaskTemplate>

    /**
     * Delete a template
     */
    suspend fun deleteTemplate(templateId: String)

    /**
     * Duplicate a template
     */
    suspend fun duplicateTemplate(templateId: String, newName: String): Result<TaskTemplate>

    // ==================== Template Application ====================

    /**
     * Apply template to create a task
     */
    suspend fun applyTemplate(
        templateId: String,
        customValues: Map<String, String> = emptyMap(),
        assignees: List<String> = emptyList(),
        dueDate: Long? = null
    ): Result<TemplateApplicationResult>

    /**
     * Apply template with recurrence
     */
    suspend fun applyTemplateWithRecurrence(
        templateId: String,
        startDate: Long,
        endDate: Long?,
        customValues: Map<String, String> = emptyMap()
    ): Result<List<TemplateApplicationResult>>

    // ==================== Template Sharing ====================

    /**
     * Update template visibility
     */
    suspend fun updateTemplateVisibility(templateId: String, isPublic: Boolean)

    /**
     * Share template with users
     */
    suspend fun shareTemplate(
        templateId: String,
        userIds: List<String>,
        canEdit: Boolean,
        canCopy: Boolean
    ): Result<Unit>

    /**
     * Share template with teams
     */
    suspend fun shareTemplateWithTeam(
        templateId: String,
        teamIds: List<String>,
        canEdit: Boolean,
        canCopy: Boolean
    ): Result<Unit>

    // ==================== Template Stats ====================

    /**
     * Get template statistics
     */
    fun getTemplateStats(templateId: String): Flow<TemplateStats?>

    /**
     * Record template usage
     */
    suspend fun recordTemplateUsage(
        templateId: String,
        taskId: String?,
        wasSuccessful: Boolean
    )

    /**
     * Update template stats cache
     */
    suspend fun updateTemplateStats(templateId: String)

    // ==================== Import/Export ====================

    /**
     * Export template
     */
    suspend fun exportTemplate(templateId: String): Result<TemplateExport>

    /**
     * Import template
     */
    suspend fun importTemplate(templateExport: TemplateExport): Result<TaskTemplate>

    /**
     * Export template as JSON
     */
    suspend fun exportTemplateAsJson(templateId: String): Result<String>

    /**
     * Import template from JSON
     */
    suspend fun importTemplateFromJson(json: String): Result<TaskTemplate>
}
