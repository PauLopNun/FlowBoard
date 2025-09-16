package com.flowboard.data.local.dao

import androidx.room.*
import com.flowboard.data.local.entities.TaskEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDateTime

@Dao
interface TaskDao {
    
    @Query("SELECT * FROM tasks ORDER BY createdAt DESC")
    fun getAllTasks(): Flow<List<TaskEntity>>
    
    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskById(id: String): TaskEntity?
    
    @Query("SELECT * FROM tasks WHERE projectId = :projectId ORDER BY createdAt DESC")
    fun getTasksByProject(projectId: String): Flow<List<TaskEntity>>
    
    @Query("SELECT * FROM tasks WHERE isCompleted = :isCompleted ORDER BY createdAt DESC")
    fun getTasksByStatus(isCompleted: Boolean): Flow<List<TaskEntity>>
    
    @Query("SELECT * FROM tasks WHERE isEvent = 1 AND eventStartTime >= :startDate AND eventStartTime <= :endDate ORDER BY eventStartTime ASC")
    fun getEventsBetweenDates(startDate: LocalDateTime, endDate: LocalDateTime): Flow<List<TaskEntity>>
    
    @Query("SELECT * FROM tasks WHERE assignedTo = :userId ORDER BY createdAt DESC")
    fun getTasksAssignedToUser(userId: String): Flow<List<TaskEntity>>
    
    @Query("SELECT * FROM tasks WHERE dueDate <= :date AND isCompleted = 0 ORDER BY dueDate ASC")
    fun getOverdueTasks(date: LocalDateTime): Flow<List<TaskEntity>>
    
    @Query("SELECT * FROM tasks WHERE isSync = 0")
    suspend fun getUnsyncedTasks(): List<TaskEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<TaskEntity>)
    
    @Update
    suspend fun updateTask(task: TaskEntity)
    
    @Delete
    suspend fun deleteTask(task: TaskEntity)
    
    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTaskById(id: String)
    
    @Query("UPDATE tasks SET isCompleted = :isCompleted, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateTaskStatus(id: String, isCompleted: Boolean, updatedAt: LocalDateTime)
    
    @Query("UPDATE tasks SET isSync = 1, lastSyncAt = :syncTime WHERE id = :id")
    suspend fun markTaskAsSynced(id: String, syncTime: LocalDateTime)
}