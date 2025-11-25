package com.flowboard.data.remote.api

import com.flowboard.data.local.entities.TaskEntity
import com.flowboard.data.remote.dto.TaskDto
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskApiService @Inject constructor(
    private val httpClient: HttpClient
) {
    companion object {
        private const val BASE_URL = "http://10.0.2.2:8080/api/v1"
        private const val TASKS_ENDPOINT = "$BASE_URL/tasks"
    }
    
    suspend fun getAllTasks(): List<TaskEntity> {
        return httpClient.get(TASKS_ENDPOINT).body<List<TaskDto>>().map { it.toEntity() }
    }
    
    suspend fun getTaskById(id: String): TaskEntity {
        return httpClient.get("$TASKS_ENDPOINT/$id").body<TaskDto>().toEntity()
    }
    
    suspend fun createTask(task: TaskEntity): TaskEntity {
        return httpClient.post(TASKS_ENDPOINT) {
            contentType(ContentType.Application.Json)
            setBody(TaskDto.fromEntity(task))
        }.body<TaskDto>().toEntity()
    }
    
    suspend fun updateTask(id: String, task: TaskEntity): TaskEntity {
        return httpClient.put("$TASKS_ENDPOINT/$id") {
            contentType(ContentType.Application.Json)
            setBody(TaskDto.fromEntity(task))
        }.body<TaskDto>().toEntity()
    }
    
    suspend fun deleteTask(id: String) {
        httpClient.delete("$TASKS_ENDPOINT/$id")
    }
    
    suspend fun getTasksByProject(projectId: String): List<TaskEntity> {
        return httpClient.get("$TASKS_ENDPOINT/project/$projectId").body<List<TaskDto>>().map { it.toEntity() }
    }
}