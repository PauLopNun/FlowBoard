package com.flowboard.domain.repository

import com.flowboard.domain.model.Task
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDateTime

interface TaskRepository {
    fun getAllTasks(): Flow<List<Task>>
    fun getTasksByProject(projectId: String): Flow<List<Task>>
    fun getTasksByStatus(isCompleted: Boolean): Flow<List<Task>>
    fun getEventsBetweenDates(startDate: LocalDateTime, endDate: LocalDateTime): Flow<List<Task>>
    fun getTasksAssignedToUser(userId: String): Flow<List<Task>>
    fun getOverdueTasks(): Flow<List<Task>>
    
    suspend fun getTaskById(id: String): Task?
    suspend fun createTask(task: Task): Result<Task>
    suspend fun updateTask(task: Task): Result<Task>
    suspend fun deleteTask(id: String): Result<Unit>
    suspend fun toggleTaskStatus(id: String): Result<Task>
    suspend fun syncTasks(): Result<Unit>
}