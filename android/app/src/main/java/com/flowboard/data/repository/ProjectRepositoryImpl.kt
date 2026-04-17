package com.flowboard.data.repository

import com.flowboard.data.local.dao.ProjectDao
import com.flowboard.data.local.entities.ProjectEntity
import com.flowboard.data.remote.api.ProjectApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProjectRepositoryImpl @Inject constructor(
    private val projectApiService: ProjectApiService,
    private val projectDao: ProjectDao,
    private val authRepository: AuthRepository
) {
    fun getProjects(): Flow<List<ProjectEntity>> = projectDao.getAllActiveProjects()

    suspend fun refresh(): Result<Unit> = runCatching {
        val projects = projectApiService.getProjects()
        projectDao.insertProjects(projects)
    }

    suspend fun createProject(name: String, description: String, color: String): Result<ProjectEntity> =
        runCatching {
            val project = projectApiService.createProject(name, description, color)
            projectDao.insertProject(project)
            project
        }

    suspend fun deleteProject(id: String): Result<Unit> = runCatching {
        projectApiService.deleteProject(id)
        projectDao.deleteProjectById(id)
    }

    suspend fun updateProject(id: String, name: String? = null, description: String? = null, color: String? = null): Result<ProjectEntity> =
        runCatching {
            val updated = projectApiService.updateProject(id, name, description, color)
            projectDao.insertProject(updated)
            updated
        }

    suspend fun getCurrentUserId(): String? = authRepository.getUserId()
}
