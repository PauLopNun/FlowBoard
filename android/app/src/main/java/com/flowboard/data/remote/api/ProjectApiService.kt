package com.flowboard.data.remote.api

import com.flowboard.data.local.entities.ProjectEntity
import com.flowboard.data.remote.ApiConfig
import com.flowboard.data.repository.AuthRepository
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
private data class ProjectDto(
    val id: String,
    val name: String,
    val description: String,
    val color: String = "#2196F3",
    val ownerId: String,
    val members: List<String> = emptyList(),
    val isActive: Boolean = true,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val deadline: LocalDateTime? = null
) {
    fun toEntity() = ProjectEntity(
        id = id,
        name = name,
        description = description,
        color = color,
        ownerId = ownerId,
        members = members,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deadline = deadline,
        isSync = true,
        lastSyncAt = updatedAt
    )
}

@Serializable
private data class CreateProjectRequest(
    val name: String,
    val description: String = "",
    val color: String = "#2196F3",
    val deadline: LocalDateTime? = null
)

@Serializable
private data class UpdateProjectRequest(
    val name: String? = null,
    val description: String? = null,
    val color: String? = null,
    val isActive: Boolean? = null,
    val deadline: LocalDateTime? = null
)

@Singleton
class ProjectApiService @Inject constructor(
    private val httpClient: HttpClient,
    private val authRepository: AuthRepository
) {
    companion object {
        private val PROJECTS_ENDPOINT = "${ApiConfig.API_BASE_URL}/projects"
    }

    private suspend fun authHeader() =
        authRepository.getToken()?.let { "Bearer $it" } ?: throw Exception("Not authenticated")

    suspend fun getProjects(): List<ProjectEntity> =
        httpClient.get(PROJECTS_ENDPOINT) {
            header(HttpHeaders.Authorization, authHeader())
        }.body<List<ProjectDto>>().map { it.toEntity() }

    suspend fun createProject(name: String, description: String, color: String, deadline: LocalDateTime? = null): ProjectEntity =
        httpClient.post(PROJECTS_ENDPOINT) {
            header(HttpHeaders.Authorization, authHeader())
            contentType(ContentType.Application.Json)
            setBody(CreateProjectRequest(name, description, color, deadline))
        }.body<ProjectDto>().toEntity()

    suspend fun updateProject(id: String, name: String? = null, description: String? = null, color: String? = null, isActive: Boolean? = null): ProjectEntity =
        httpClient.put("$PROJECTS_ENDPOINT/$id") {
            header(HttpHeaders.Authorization, authHeader())
            contentType(ContentType.Application.Json)
            setBody(UpdateProjectRequest(name, description, color, isActive))
        }.body<ProjectDto>().toEntity()

    suspend fun deleteProject(id: String) {
        httpClient.delete("$PROJECTS_ENDPOINT/$id") {
            header(HttpHeaders.Authorization, authHeader())
        }
    }
}
