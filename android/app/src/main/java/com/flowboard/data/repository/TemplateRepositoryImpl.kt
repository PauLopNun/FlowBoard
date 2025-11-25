package com.flowboard.data.repository

import com.flowboard.data.local.dao.TemplateDao
import com.flowboard.data.local.entities.*
import com.flowboard.domain.model.*
import com.flowboard.domain.repository.TemplateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton
import java.util.UUID
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

@Singleton
class TemplateRepositoryImpl @Inject constructor(
    private val templateDao: TemplateDao,
    private val authRepository: AuthRepository,
    private val json: Json
) : TemplateRepository {

    override fun getAllTemplates(): Flow<List<TaskTemplate>> {
        return templateDao.getAllTemplates().map { templates ->
            templates.map { template ->
                val subtasks = templateDao.getSubtasksSync(template.id)
                val customFields = templateDao.getCustomFieldsSync(template.id)
                template.toTaskTemplate(subtasks, customFields)
            }
        }
    }

    override fun getTemplate(templateId: String): Flow<TaskTemplate?> {
        return combine(
            templateDao.getTemplate(templateId),
            templateDao.getSubtasks(templateId),
            templateDao.getCustomFields(templateId)
        ) { template, subtasks, customFields ->
            template?.toTaskTemplate(subtasks, customFields)
        }
    }

    override fun getTemplatesByType(type: TemplateType): Flow<List<TaskTemplate>> {
        return templateDao.getTemplatesByType(type.name.lowercase()).map { templates ->
            templates.map { template ->
                val subtasks = templateDao.getSubtasksSync(template.id)
                val customFields = templateDao.getCustomFieldsSync(template.id)
                template.toTaskTemplate(subtasks, customFields)
            }
        }
    }

    override fun getTemplatesByCategory(category: TemplateCategory): Flow<List<TaskTemplate>> {
        return templateDao.getTemplatesByCategory(category.name.lowercase()).map { templates ->
            templates.map { template ->
                val subtasks = templateDao.getSubtasksSync(template.id)
                val customFields = templateDao.getCustomFieldsSync(template.id)
                template.toTaskTemplate(subtasks, customFields)
            }
        }
    }

    override fun getUserTemplates(userId: String): Flow<List<TaskTemplate>> {
        return templateDao.getUserTemplates(userId).map { templates ->
            templates.map { template ->
                val subtasks = templateDao.getSubtasksSync(template.id)
                val customFields = templateDao.getCustomFieldsSync(template.id)
                template.toTaskTemplate(subtasks, customFields)
            }
        }
    }

    override fun getPublicTemplates(): Flow<List<TaskTemplate>> {
        return templateDao.getPublicTemplates().map { templates ->
            templates.map { template ->
                val subtasks = templateDao.getSubtasksSync(template.id)
                val customFields = templateDao.getCustomFieldsSync(template.id)
                template.toTaskTemplate(subtasks, customFields)
            }
        }
    }

    override fun getPopularTemplates(limit: Int): Flow<List<TaskTemplate>> {
        return templateDao.getPopularTemplates(limit).map { templates ->
            templates.map { template ->
                val subtasks = templateDao.getSubtasksSync(template.id)
                val customFields = templateDao.getCustomFieldsSync(template.id)
                template.toTaskTemplate(subtasks, customFields)
            }
        }
    }

    override fun getRecentlyUsedTemplates(limit: Int): Flow<List<TaskTemplate>> {
        return templateDao.getRecentlyUsedTemplates(limit).map { templates ->
            templates.map { template ->
                val subtasks = templateDao.getSubtasksSync(template.id)
                val customFields = templateDao.getCustomFieldsSync(template.id)
                template.toTaskTemplate(subtasks, customFields)
            }
        }
    }

    override suspend fun searchTemplates(query: String): List<TaskTemplate> {
        val templates = templateDao.searchTemplates(query)
        return templates.map { template ->
            val subtasks = templateDao.getSubtasksSync(template.id)
            val customFields = templateDao.getCustomFieldsSync(template.id)
            template.toTaskTemplate(subtasks, customFields)
        }
    }

    override fun getFilteredTemplates(filter: TemplateFilter): Flow<List<TaskTemplate>> {
        return templateDao.getFilteredTemplates(
            type = filter.type?.name?.lowercase(),
            category = filter.category?.name?.lowercase(),
            isPublic = filter.isPublic,
            createdBy = filter.createdBy,
            sortBy = filter.sortBy.name
        ).map { templates ->
            templates.map { template ->
                val subtasks = templateDao.getSubtasksSync(template.id)
                val customFields = templateDao.getCustomFieldsSync(template.id)
                template.toTaskTemplate(subtasks, customFields)
            }
        }
    }

    override suspend fun createTemplate(
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
    ): Result<TaskTemplate> {
        return try {
            val currentUserId = authRepository.getUserId() ?: return Result.failure(Exception("Not authenticated"))
            val templateId = UUID.randomUUID().toString()
            val now = System.currentTimeMillis()

            val templateEntity = TaskTemplateEntity(
                id = templateId,
                name = name,
                description = description,
                type = type.name.lowercase(),
                category = category.name.lowercase(),
                icon = icon,
                color = color,
                priority = priority,
                estimatedDuration = estimatedDuration,
                tags = tags,
                recurrencePattern = recurrencePattern.name.lowercase(),
                recurrenceInterval = recurrenceInterval,
                isPublic = isPublic,
                sharedWithUserIds = emptyList(),
                sharedWithTeamIds = emptyList(),
                canEdit = true,
                canCopy = true,
                usageCount = 0,
                createdBy = currentUserId,
                createdAt = now,
                updatedAt = now,
                lastUsedAt = null
            )

            val subtaskEntities = subtasks.mapIndexed { index, subtask ->
                SubtaskTemplateEntity(
                    id = "${templateId}_subtask_$index",
                    templateId = templateId,
                    title = subtask.title,
                    description = subtask.description,
                    estimatedDuration = subtask.estimatedDuration,
                    order = index
                )
            }

            val customFieldEntities = customFields.mapIndexed { index, field ->
                CustomFieldTemplateEntity(
                    id = "${templateId}_field_$index",
                    templateId = templateId,
                    name = field.name,
                    type = field.type.name.lowercase(),
                    defaultValue = field.defaultValue,
                    options = field.options,
                    isRequired = field.isRequired,
                    order = index
                )
            }

            templateDao.insertTemplateWithDetails(templateEntity, subtaskEntities, customFieldEntities)

            val template = TaskTemplate(
                id = templateId,
                name = name,
                description = description,
                type = type,
                category = category,
                icon = icon,
                color = color,
                priority = priority,
                estimatedDuration = estimatedDuration,
                tags = tags,
                subtasks = subtasks,
                customFields = customFields,
                recurrencePattern = recurrencePattern,
                recurrenceInterval = recurrenceInterval,
                isPublic = isPublic,
                usageCount = 0,
                createdBy = currentUserId,
                createdAt = now,
                updatedAt = now,
                lastUsedAt = null
            )

            Result.success(template)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateTemplate(template: TaskTemplate): Result<TaskTemplate> {
        return try {
            val now = System.currentTimeMillis()

            val templateEntity = TaskTemplateEntity(
                id = template.id,
                name = template.name,
                description = template.description,
                type = template.type.name.lowercase(),
                category = template.category.name.lowercase(),
                icon = template.icon,
                color = template.color,
                priority = template.priority,
                estimatedDuration = template.estimatedDuration,
                tags = template.tags,
                recurrencePattern = template.recurrencePattern.name.lowercase(),
                recurrenceInterval = template.recurrenceInterval,
                isPublic = template.isPublic,
                sharedWithUserIds = emptyList(),
                sharedWithTeamIds = emptyList(),
                canEdit = true,
                canCopy = true,
                usageCount = template.usageCount,
                createdBy = template.createdBy,
                createdAt = template.createdAt,
                updatedAt = now,
                lastUsedAt = template.lastUsedAt
            )

            // Delete old subtasks and fields
            templateDao.deleteSubtasksByTemplate(template.id)
            templateDao.deleteCustomFieldsByTemplate(template.id)

            // Insert updated subtasks and fields
            val subtaskEntities = template.subtasks.mapIndexed { index, subtask ->
                SubtaskTemplateEntity(
                    id = "${template.id}_subtask_$index",
                    templateId = template.id,
                    title = subtask.title,
                    description = subtask.description,
                    estimatedDuration = subtask.estimatedDuration,
                    order = index
                )
            }

            val customFieldEntities = template.customFields.mapIndexed { index, field ->
                CustomFieldTemplateEntity(
                    id = "${template.id}_field_$index",
                    templateId = template.id,
                    name = field.name,
                    type = field.type.name.lowercase(),
                    defaultValue = field.defaultValue,
                    options = field.options,
                    isRequired = field.isRequired,
                    order = index
                )
            }

            templateDao.insertTemplateWithDetails(templateEntity, subtaskEntities, customFieldEntities)

            Result.success(template.copy(updatedAt = now))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteTemplate(templateId: String) {
        templateDao.deleteTemplateWithDetails(templateId)
    }

    override suspend fun duplicateTemplate(templateId: String, newName: String): Result<TaskTemplate> {
        return try {
            val currentUserId = authRepository.getUserId() ?: return Result.failure(Exception("Not authenticated"))
            val newTemplateId = UUID.randomUUID().toString()
            val now = System.currentTimeMillis()

            templateDao.duplicateTemplate(templateId, newTemplateId, newName, currentUserId, now)

            val newTemplate = templateDao.getTemplateSync(newTemplateId)
            if (newTemplate != null) {
                val subtasks = templateDao.getSubtasksSync(newTemplateId)
                val customFields = templateDao.getCustomFieldsSync(newTemplateId)
                Result.success(newTemplate.toTaskTemplate(subtasks, customFields))
            } else {
                Result.failure(Exception("Failed to duplicate template"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun applyTemplate(
        templateId: String,
        customValues: Map<String, String>,
        assignees: List<String>,
        dueDate: Long?
    ): Result<TemplateApplicationResult> {
        return try {
            // TODO: Implement task creation from template
            // This would integrate with TaskRepository to create actual tasks

            val now = System.currentTimeMillis()
            templateDao.incrementUsageCount(templateId, now)

            val usageId = UUID.randomUUID().toString()
            val currentUserId = authRepository.getUserId() ?: "unknown"

            val usage = TemplateUsageEntity(
                id = usageId,
                templateId = templateId,
                userId = currentUserId,
                taskId = null, // TODO: Set actual task ID
                usedAt = now,
                completedAt = null,
                wasSuccessful = true
            )
            templateDao.insertUsage(usage)

            Result.success(
                TemplateApplicationResult(
                    taskId = null,
                    success = true,
                    message = "Template applied successfully"
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun applyTemplateWithRecurrence(
        templateId: String,
        startDate: Long,
        endDate: Long?,
        customValues: Map<String, String>
    ): Result<List<TemplateApplicationResult>> {
        // TODO: Implement recurrence logic
        return Result.success(emptyList())
    }

    override suspend fun updateTemplateVisibility(templateId: String, isPublic: Boolean) {
        templateDao.updateTemplateVisibility(templateId, isPublic)
    }

    override suspend fun shareTemplate(
        templateId: String,
        userIds: List<String>,
        canEdit: Boolean,
        canCopy: Boolean
    ): Result<Unit> {
        // TODO: Implement sharing logic with API
        return Result.success(Unit)
    }

    override suspend fun shareTemplateWithTeam(
        templateId: String,
        teamIds: List<String>,
        canEdit: Boolean,
        canCopy: Boolean
    ): Result<Unit> {
        // TODO: Implement team sharing logic with API
        return Result.success(Unit)
    }

    override fun getTemplateStats(templateId: String): Flow<TemplateStats?> {
        return templateDao.getTemplateStats(templateId).map { it?.toTemplateStats() }
    }

    override suspend fun recordTemplateUsage(
        templateId: String,
        taskId: String?,
        wasSuccessful: Boolean
    ) {
        val usageId = UUID.randomUUID().toString()
        val currentUserId = authRepository.getUserId() ?: "unknown"
        val now = System.currentTimeMillis()

        val usage = TemplateUsageEntity(
            id = usageId,
            templateId = templateId,
            userId = currentUserId,
            taskId = taskId,
            usedAt = now,
            completedAt = null,
            wasSuccessful = wasSuccessful
        )
        templateDao.insertUsage(usage)
        templateDao.incrementUsageCount(templateId, now)
    }

    override suspend fun updateTemplateStats(templateId: String) {
        // TODO: Calculate actual stats from usage data
        val stats = TemplateStatsEntity(
            templateId = templateId,
            usageCount = 0,
            averageCompletionTime = null,
            successRate = 0f,
            lastUsedAt = null,
            popularityScore = 0f,
            updatedAt = System.currentTimeMillis()
        )
        templateDao.insertStats(stats)
    }

    override suspend fun exportTemplate(templateId: String): Result<TemplateExport> {
        return try {
            val template = templateDao.getTemplateSync(templateId)
            val subtasks = templateDao.getSubtasksSync(templateId)
            val customFields = templateDao.getCustomFieldsSync(templateId)

            if (template != null) {
                val currentUserId = authRepository.getUserId() ?: "unknown"
                val taskTemplate = template.toTaskTemplate(subtasks, customFields)

                val export = TemplateExport(
                    template = taskTemplate,
                    version = "1.0",
                    exportedAt = System.currentTimeMillis(),
                    exportedBy = currentUserId
                )
                Result.success(export)
            } else {
                Result.failure(Exception("Template not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun importTemplate(templateExport: TemplateExport): Result<TaskTemplate> {
        return try {
            val currentUserId = authRepository.getUserId() ?: return Result.failure(Exception("Not authenticated"))
            val newTemplateId = UUID.randomUUID().toString()
            val now = System.currentTimeMillis()

            val template = templateExport.template.copy(
                id = newTemplateId,
                createdBy = currentUserId,
                createdAt = now,
                updatedAt = now,
                usageCount = 0,
                lastUsedAt = null
            )

            createTemplate(
                name = template.name,
                description = template.description,
                type = template.type,
                category = template.category,
                icon = template.icon,
                color = template.color,
                priority = template.priority,
                estimatedDuration = template.estimatedDuration,
                tags = template.tags,
                subtasks = template.subtasks,
                customFields = template.customFields,
                recurrencePattern = template.recurrencePattern,
                recurrenceInterval = template.recurrenceInterval,
                isPublic = false
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun exportTemplateAsJson(templateId: String): Result<String> {
        return try {
            val exportResult = exportTemplate(templateId)
            exportResult.fold(
                onSuccess = { export ->
                    val jsonString = json.encodeToString(export)
                    Result.success(jsonString)
                },
                onFailure = { Result.failure(it) }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun importTemplateFromJson(jsonString: String): Result<TaskTemplate> {
        return try {
            val export = json.decodeFromString<TemplateExport>(jsonString)
            importTemplate(export)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== Helper Extension Functions ====================

    private fun TaskTemplateEntity.toTaskTemplate(
        subtasks: List<SubtaskTemplateEntity>,
        customFields: List<CustomFieldTemplateEntity>
    ): TaskTemplate {
        return TaskTemplate(
            id = id,
            name = name,
            description = description,
            type = TemplateType.valueOf(type.uppercase()),
            category = TemplateCategory.valueOf(category.uppercase()),
            icon = icon,
            color = color,
            priority = priority,
            estimatedDuration = estimatedDuration,
            tags = tags,
            subtasks = subtasks.map { it.toSubtaskTemplate() },
            customFields = customFields.map { it.toCustomFieldTemplate() },
            recurrencePattern = RecurrencePattern.valueOf(recurrencePattern.uppercase()),
            recurrenceInterval = recurrenceInterval,
            isPublic = isPublic,
            usageCount = usageCount,
            createdBy = createdBy,
            createdAt = createdAt,
            updatedAt = updatedAt,
            lastUsedAt = lastUsedAt
        )
    }

    private fun SubtaskTemplateEntity.toSubtaskTemplate(): SubtaskTemplate {
        return SubtaskTemplate(
            id = id,
            title = title,
            description = description,
            estimatedDuration = estimatedDuration,
            order = order
        )
    }

    private fun CustomFieldTemplateEntity.toCustomFieldTemplate(): CustomFieldTemplate {
        return CustomFieldTemplate(
            id = id,
            name = name,
            type = FieldType.valueOf(type.uppercase()),
            defaultValue = defaultValue,
            options = options,
            isRequired = isRequired,
            order = order
        )
    }

    private fun TemplateStatsEntity.toTemplateStats(): TemplateStats {
        return TemplateStats(
            templateId = templateId,
            usageCount = usageCount,
            averageCompletionTime = averageCompletionTime,
            successRate = successRate,
            lastUsedAt = lastUsedAt,
            popularityScore = popularityScore
        )
    }
}
