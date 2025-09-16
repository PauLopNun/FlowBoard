package com.flowboard.routes

import com.flowboard.data.models.*
import com.flowboard.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlin.test.*
import kotlinx.serialization.json.Json

class TaskRoutesTest {

    @Test
    fun `test get tasks without authentication returns unauthorized`() = testApplication {
        // Given
        application {
            configureRouting()
            configureSerialization()
        }

        // When
        val response = client.get("/api/v1/tasks")

        // Then
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `test create task endpoint structure`() = testApplication {
        // Given
        application {
            configureRouting()
            configureSerialization()
        }

        val createTaskRequest = CreateTaskRequest(
            title = "Test Task",
            description = "Test Description",
            priority = TaskPriority.HIGH
        )

        // When
        val response = client.post("/api/v1/tasks") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(CreateTaskRequest.serializer(), createTaskRequest))
        }

        // Then
        // Should return Unauthorized due to missing JWT token
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `test get task by id endpoint structure`() = testApplication {
        // Given
        application {
            configureRouting()
            configureSerialization()
        }

        // When
        val response = client.get("/api/v1/tasks/test-id")

        // Then
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `test update task endpoint structure`() = testApplication {
        // Given
        application {
            configureRouting()
            configureSerialization()
        }

        val updateRequest = UpdateTaskRequest(
            title = "Updated Title",
            isCompleted = true
        )

        // When
        val response = client.put("/api/v1/tasks/test-id") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(UpdateTaskRequest.serializer(), updateRequest))
        }

        // Then
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `test delete task endpoint structure`() = testApplication {
        // Given
        application {
            configureRouting()
            configureSerialization()
        }

        // When
        val response = client.delete("/api/v1/tasks/test-id")

        // Then
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `test toggle task status endpoint structure`() = testApplication {
        // Given
        application {
            configureRouting()
            configureSerialization()
        }

        // When
        val response = client.patch("/api/v1/tasks/test-id/toggle")

        // Then
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `test get events endpoint structure`() = testApplication {
        // Given
        application {
            configureRouting()
            configureSerialization()
        }

        // When
        val response = client.get("/api/v1/tasks/events") {
            parameter("startDate", "2024-01-01T00:00:00")
            parameter("endDate", "2024-01-31T23:59:59")
        }

        // Then
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }
}