package com.flowboard.data.repository

import com.flowboard.data.local.dao.TaskDao
import com.flowboard.data.local.entities.TaskEntity
import com.flowboard.data.remote.api.TaskApiService
import com.flowboard.domain.model.Task
import com.flowboard.domain.repository.TaskRepository
import com.flowboard.utils.toEntity
import com.flowboard.utils.toDomain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepositoryImpl @Inject constructor(
    private val taskDao: TaskDao,
    private val taskApiService: TaskApiService
) : TaskRepository {
    
    override fun getAllTasks(): Flow<List<Task>> {
        return taskDao.getAllTasks().map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override fun getTasksByProject(projectId: String): Flow<List<Task>> {
        return taskDao.getTasksByProject(projectId).map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override fun getTasksByStatus(isCompleted: Boolean): Flow<List<Task>> {
        return taskDao.getTasksByStatus(isCompleted).map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override fun getEventsBetweenDates(startDate: LocalDateTime, endDate: LocalDateTime): Flow<List<Task>> {
        return taskDao.getEventsBetweenDates(startDate, endDate).map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override fun getTasksAssignedToUser(userId: String): Flow<List<Task>> {
        return taskDao.getTasksAssignedToUser(userId).map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override fun getOverdueTasks(): Flow<List<Task>> {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        return taskDao.getOverdueTasks(now).map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override suspend fun getTaskById(id: String): Task? {
        return taskDao.getTaskById(id)?.toDomain()
    }
    
    override suspend fun createTask(task: Task): Result<Task> {
        return try {
            val entity = task.toEntity().copy(
                isSync = false,
                lastSyncAt = null
            )
            taskDao.insertTask(entity)
            
            // Try to sync with backend
            try {
                taskApiService.createTask(entity)
                taskDao.markTaskAsSynced(
                    entity.id,
                    Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                )
            } catch (e: Exception) {
                // Backend sync failed, task remains unsynced
            }
            
            Result.success(task)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun updateTask(task: Task): Result<Task> {
        return try {
            val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            val entity = task.toEntity().copy(
                updatedAt = now,
                isSync = false,
                lastSyncAt = null
            )
            taskDao.updateTask(entity)
            
            // Try to sync with backend
            try {
                taskApiService.updateTask(entity.id, entity)
                taskDao.markTaskAsSynced(entity.id, now)
            } catch (e: Exception) {
                // Backend sync failed, task remains unsynced
            }
            
            Result.success(task.copy(updatedAt = now))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun deleteTask(id: String): Result<Unit> {
        return try {
            taskDao.deleteTaskById(id)
            
            // Try to sync with backend
            try {
                taskApiService.deleteTask(id)
            } catch (e: Exception) {
                // Backend sync failed, but local deletion succeeded
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun toggleTaskStatus(id: String): Result<Task> {
        return try {
            val task = taskDao.getTaskById(id) ?: return Result.failure(Exception("Task not found"))
            val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            val updatedTask = task.copy(
                isCompleted = !task.isCompleted,
                updatedAt = now,
                isSync = false
            )
            
            taskDao.updateTask(updatedTask)
            
            // Try to sync with backend
            try {
                taskApiService.updateTask(updatedTask.id, updatedTask)
                taskDao.markTaskAsSynced(updatedTask.id, now)
            } catch (e: Exception) {
                // Backend sync failed, task remains unsynced
            }
            
            Result.success(updatedTask.toDomain())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun syncTasks(): Result<Unit> {
        return try {
            // Get unsynced local tasks
            val unsyncedTasks = taskDao.getUnsyncedTasks()
            
            // Sync each unsynced task
            unsyncedTasks.forEach { task ->
                try {
                    if (task.createdAt == task.updatedAt) {
                        // New task
                        taskApiService.createTask(task)
                    } else {
                        // Updated task
                        taskApiService.updateTask(task.id, task)
                    }
                    taskDao.markTaskAsSynced(
                        task.id,
                        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                    )
                } catch (e: Exception) {
                    // Skip this task, will retry later
                }
            }
            
            // Fetch latest tasks from backend
            try {
                val remoteTasks = taskApiService.getAllTasks()
                taskDao.insertTasks(remoteTasks)
            } catch (e: Exception) {
                // Remote fetch failed, but local sync may have succeeded
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}