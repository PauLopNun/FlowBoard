package com.flowboard.data.local.dao

import androidx.room.*
import com.flowboard.data.local.entities.ProjectEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    
    @Query("SELECT * FROM projects WHERE isActive = 1 ORDER BY createdAt DESC")
    fun getAllActiveProjects(): Flow<List<ProjectEntity>>
    
    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun getProjectById(id: String): ProjectEntity?
    
    @Query("SELECT * FROM projects WHERE ownerId = :ownerId AND isActive = 1 ORDER BY createdAt DESC")
    fun getProjectsByOwner(ownerId: String): Flow<List<ProjectEntity>>
    
    @Query("SELECT * FROM projects WHERE ownerId = :userId OR members LIKE '%' || :userId || '%' AND isActive = 1 ORDER BY createdAt DESC")
    fun getProjectsByMember(userId: String): Flow<List<ProjectEntity>>
    
    @Query("SELECT * FROM projects WHERE isSync = 0")
    suspend fun getUnsyncedProjects(): List<ProjectEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProjects(projects: List<ProjectEntity>)
    
    @Update
    suspend fun updateProject(project: ProjectEntity)
    
    @Delete
    suspend fun deleteProject(project: ProjectEntity)
    
    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteProjectById(id: String)
    
    @Query("UPDATE projects SET isSync = 1, lastSyncAt = :syncTime WHERE id = :id")
    suspend fun markProjectAsSynced(id: String, syncTime: kotlinx.datetime.LocalDateTime)
}