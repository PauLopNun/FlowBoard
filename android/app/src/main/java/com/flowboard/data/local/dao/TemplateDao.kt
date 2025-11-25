package com.flowboard.data.local.dao

import androidx.room.*
import com.flowboard.data.local.entities.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TemplateDao {

    // ==================== Task Templates ====================

    @Query("SELECT * FROM task_templates ORDER BY updatedAt DESC")
    fun getAllTemplates(): Flow<List<TaskTemplateEntity>>

    @Query("SELECT * FROM task_templates WHERE id = :templateId")
    fun getTemplate(templateId: String): Flow<TaskTemplateEntity?>

    @Query("SELECT * FROM task_templates WHERE id = :templateId")
    suspend fun getTemplateSync(templateId: String): TaskTemplateEntity?

    @Query("SELECT * FROM task_templates WHERE type = :type ORDER BY updatedAt DESC")
    fun getTemplatesByType(type: String): Flow<List<TaskTemplateEntity>>

    @Query("SELECT * FROM task_templates WHERE category = :category ORDER BY updatedAt DESC")
    fun getTemplatesByCategory(category: String): Flow<List<TaskTemplateEntity>>

    @Query("SELECT * FROM task_templates WHERE createdBy = :userId ORDER BY updatedAt DESC")
    fun getUserTemplates(userId: String): Flow<List<TaskTemplateEntity>>

    @Query("SELECT * FROM task_templates WHERE isPublic = 1 ORDER BY usageCount DESC")
    fun getPublicTemplates(): Flow<List<TaskTemplateEntity>>

    @Query("""
        SELECT * FROM task_templates
        WHERE name LIKE '%' || :query || '%'
        OR description LIKE '%' || :query || '%'
        ORDER BY usageCount DESC
    """)
    suspend fun searchTemplates(query: String): List<TaskTemplateEntity>

    @Query("""
        SELECT * FROM task_templates
        WHERE (:type IS NULL OR type = :type)
        AND (:category IS NULL OR category = :category)
        AND (:isPublic IS NULL OR isPublic = :isPublic)
        AND (:createdBy IS NULL OR createdBy = :createdBy)
        ORDER BY
            CASE WHEN :sortBy = 'RECENT' THEN updatedAt END DESC,
            CASE WHEN :sortBy = 'POPULAR' THEN usageCount END DESC,
            CASE WHEN :sortBy = 'NAME' THEN name END ASC,
            CASE WHEN :sortBy = 'CREATED_DATE' THEN createdAt END DESC,
            CASE WHEN :sortBy = 'LAST_USED' THEN lastUsedAt END DESC
    """)
    fun getFilteredTemplates(
        type: String?,
        category: String?,
        isPublic: Boolean?,
        createdBy: String?,
        sortBy: String
    ): Flow<List<TaskTemplateEntity>>

    @Query("SELECT * FROM task_templates ORDER BY usageCount DESC LIMIT :limit")
    fun getPopularTemplates(limit: Int = 10): Flow<List<TaskTemplateEntity>>

    @Query("SELECT * FROM task_templates WHERE lastUsedAt IS NOT NULL ORDER BY lastUsedAt DESC LIMIT :limit")
    fun getRecentlyUsedTemplates(limit: Int = 10): Flow<List<TaskTemplateEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: TaskTemplateEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplates(templates: List<TaskTemplateEntity>)

    @Update
    suspend fun updateTemplate(template: TaskTemplateEntity)

    @Query("UPDATE task_templates SET usageCount = usageCount + 1, lastUsedAt = :timestamp WHERE id = :templateId")
    suspend fun incrementUsageCount(templateId: String, timestamp: Long)

    @Query("UPDATE task_templates SET isPublic = :isPublic WHERE id = :templateId")
    suspend fun updateTemplateVisibility(templateId: String, isPublic: Boolean)

    @Delete
    suspend fun deleteTemplate(template: TaskTemplateEntity)

    @Query("DELETE FROM task_templates WHERE id = :templateId")
    suspend fun deleteTemplateById(templateId: String)

    // ==================== Subtask Templates ====================

    @Query("SELECT * FROM subtask_templates WHERE templateId = :templateId ORDER BY `order` ASC")
    fun getSubtasks(templateId: String): Flow<List<SubtaskTemplateEntity>>

    @Query("SELECT * FROM subtask_templates WHERE templateId = :templateId ORDER BY `order` ASC")
    suspend fun getSubtasksSync(templateId: String): List<SubtaskTemplateEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubtask(subtask: SubtaskTemplateEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubtasks(subtasks: List<SubtaskTemplateEntity>)

    @Update
    suspend fun updateSubtask(subtask: SubtaskTemplateEntity)

    @Delete
    suspend fun deleteSubtask(subtask: SubtaskTemplateEntity)

    @Query("DELETE FROM subtask_templates WHERE templateId = :templateId")
    suspend fun deleteSubtasksByTemplate(templateId: String)

    // ==================== Custom Field Templates ====================

    @Query("SELECT * FROM custom_field_templates WHERE templateId = :templateId ORDER BY `order` ASC")
    fun getCustomFields(templateId: String): Flow<List<CustomFieldTemplateEntity>>

    @Query("SELECT * FROM custom_field_templates WHERE templateId = :templateId ORDER BY `order` ASC")
    suspend fun getCustomFieldsSync(templateId: String): List<CustomFieldTemplateEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomField(field: CustomFieldTemplateEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomFields(fields: List<CustomFieldTemplateEntity>)

    @Update
    suspend fun updateCustomField(field: CustomFieldTemplateEntity)

    @Delete
    suspend fun deleteCustomField(field: CustomFieldTemplateEntity)

    @Query("DELETE FROM custom_field_templates WHERE templateId = :templateId")
    suspend fun deleteCustomFieldsByTemplate(templateId: String)

    // ==================== Template Usage ====================

    @Query("SELECT * FROM template_usage WHERE templateId = :templateId ORDER BY usedAt DESC")
    fun getTemplateUsage(templateId: String): Flow<List<TemplateUsageEntity>>

    @Query("SELECT * FROM template_usage WHERE userId = :userId ORDER BY usedAt DESC LIMIT :limit")
    fun getUserTemplateUsage(userId: String, limit: Int = 50): Flow<List<TemplateUsageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsage(usage: TemplateUsageEntity)

    @Query("DELETE FROM template_usage WHERE templateId = :templateId")
    suspend fun deleteUsageByTemplate(templateId: String)

    @Query("DELETE FROM template_usage WHERE usedAt < :timestamp")
    suspend fun cleanupOldUsage(timestamp: Long)

    // ==================== Template Stats ====================

    @Query("SELECT * FROM template_stats WHERE templateId = :templateId")
    fun getTemplateStats(templateId: String): Flow<TemplateStatsEntity?>

    @Query("SELECT * FROM template_stats WHERE templateId = :templateId")
    suspend fun getTemplateStatsSync(templateId: String): TemplateStatsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStats(stats: TemplateStatsEntity)

    @Query("DELETE FROM template_stats WHERE templateId = :templateId")
    suspend fun deleteStatsByTemplate(templateId: String)

    @Query("DELETE FROM template_stats WHERE updatedAt < :timestamp")
    suspend fun cleanupOldStats(timestamp: Long)

    // ==================== Batch Operations ====================

    @Transaction
    suspend fun insertTemplateWithDetails(
        template: TaskTemplateEntity,
        subtasks: List<SubtaskTemplateEntity>,
        customFields: List<CustomFieldTemplateEntity>
    ) {
        insertTemplate(template)
        if (subtasks.isNotEmpty()) {
            insertSubtasks(subtasks)
        }
        if (customFields.isNotEmpty()) {
            insertCustomFields(customFields)
        }
    }

    @Transaction
    suspend fun deleteTemplateWithDetails(templateId: String) {
        deleteTemplateById(templateId)
        deleteSubtasksByTemplate(templateId)
        deleteCustomFieldsByTemplate(templateId)
        deleteUsageByTemplate(templateId)
        deleteStatsByTemplate(templateId)
    }

    @Transaction
    suspend fun duplicateTemplate(
        sourceTemplateId: String,
        newTemplateId: String,
        newName: String,
        userId: String,
        timestamp: Long
    ) {
        val sourceTemplate = getTemplateSync(sourceTemplateId) ?: return
        val sourceSubtasks = getSubtasksSync(sourceTemplateId)
        val sourceFields = getCustomFieldsSync(sourceTemplateId)

        // Create new template
        val newTemplate = sourceTemplate.copy(
            id = newTemplateId,
            name = newName,
            createdBy = userId,
            createdAt = timestamp,
            updatedAt = timestamp,
            usageCount = 0,
            lastUsedAt = null
        )
        insertTemplate(newTemplate)

        // Duplicate subtasks
        val newSubtasks = sourceSubtasks.map { subtask ->
            subtask.copy(
                id = "${newTemplateId}_subtask_${subtask.order}",
                templateId = newTemplateId
            )
        }
        if (newSubtasks.isNotEmpty()) {
            insertSubtasks(newSubtasks)
        }

        // Duplicate custom fields
        val newFields = sourceFields.map { field ->
            field.copy(
                id = "${newTemplateId}_field_${field.order}",
                templateId = newTemplateId
            )
        }
        if (newFields.isNotEmpty()) {
            insertCustomFields(newFields)
        }
    }
}
