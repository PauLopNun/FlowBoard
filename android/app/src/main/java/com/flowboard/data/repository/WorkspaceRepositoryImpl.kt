package com.flowboard.data.repository

import com.flowboard.data.local.dao.WorkspaceDao
import com.flowboard.data.local.entities.WorkspaceEntity
import com.flowboard.data.remote.api.WorkspaceApiService
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkspaceRepositoryImpl @Inject constructor(
    private val workspaceApiService: WorkspaceApiService,
    private val workspaceDao: WorkspaceDao
) {
    fun getAllWorkspaces(): Flow<List<WorkspaceEntity>> = workspaceDao.getAllWorkspaces()

    suspend fun fetchWorkspaces(): Result<List<WorkspaceEntity>> {
        return try {
            workspaceDao.deleteAll()
            val response = workspaceApiService.getWorkspaces()
            val all = (response.owned + response.member).map { it.toEntity() }
            workspaceDao.insertAll(all)
            Result.success(all)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createWorkspace(name: String, description: String?, imageUrl: String? = null): Result<WorkspaceEntity> {
        return try {
            val dto = workspaceApiService.createWorkspace(name, description, imageUrl)
            val entity = dto.toEntity()
            workspaceDao.insert(entity)
            Result.success(entity)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateWorkspace(id: String, name: String?, description: String?, imageUrl: String?): Result<WorkspaceEntity> {
        return try {
            val dto = workspaceApiService.updateWorkspace(id, name, description, imageUrl)
            val entity = dto.toEntity()
            workspaceDao.insert(entity)
            Result.success(entity)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun joinWorkspace(inviteCode: String): Result<WorkspaceEntity> {
        return try {
            val dto = workspaceApiService.joinWorkspace(inviteCode)
            val entity = dto.toEntity()
            workspaceDao.insert(entity)
            Result.success(entity)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun inviteMember(workspaceId: String, email: String): Result<String> {
        return try {
            val response = workspaceApiService.inviteMember(workspaceId, email)
            if (response.success) {
                Result.success(response.message)
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteWorkspace(id: String): Result<Unit> {
        return try {
            workspaceApiService.deleteWorkspace(id)
            workspaceDao.deleteById(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
