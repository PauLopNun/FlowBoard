package com.flowboard.data.remote.api

import com.flowboard.data.local.entities.TaskEntity
import com.flowboard.data.remote.dto.TaskDto
import com.flowboard.data.remote.ApiConfig
import com.flowboard.data.repository.AuthRepository
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskApiService @Inject constructor(
    private val httpClient: HttpClient,
    private val authRepository: AuthRepository
) {
    companion object {
        private val TASKS_ENDPOINT = "${ApiConfig.API_BASE_URL}/tasks"
    }

    private suspend fun authHeader() =
        authRepository.getToken()?.let { "Bearer $it" } ?: throw Exception("Not authenticated")

    suspend fun getAllTasks(): List<TaskEntity> =
        httpClient.get(TASKS_ENDPOINT) {
            header(HttpHeaders.Authorization, authHeader())
        }.body<List<TaskDto>>().map { it.toEntity() }

    suspend fun getTaskById(id: String): TaskEntity =
        httpClient.get("$TASKS_ENDPOINT/$id") {
            header(HttpHeaders.Authorization, authHeader())
        }.body<TaskDto>().toEntity()

    suspend fun createTask(task: TaskEntity): TaskEntity =
        httpClient.post(TASKS_ENDPOINT) {
            header(HttpHeaders.Authorization, authHeader())
            contentType(ContentType.Application.Json)
            setBody(TaskDto.fromEntity(task))
        }.body<TaskDto>().toEntity()

    suspend fun updateTask(id: String, task: TaskEntity): TaskEntity =
        httpClient.put("$TASKS_ENDPOINT/$id") {
            header(HttpHeaders.Authorization, authHeader())
            contentType(ContentType.Application.Json)
            setBody(TaskDto.fromEntity(task))
        }.body<TaskDto>().toEntity()

    suspend fun deleteTask(id: String) {
        httpClient.delete("$TASKS_ENDPOINT/$id") {
            header(HttpHeaders.Authorization, authHeader())
        }
    }

    suspend fun getTasksByProject(projectId: String): List<TaskEntity> =
        httpClient.get("$TASKS_ENDPOINT/project/$projectId") {
            header(HttpHeaders.Authorization, authHeader())
        }.body<List<TaskDto>>().map { it.toEntity() }
}
