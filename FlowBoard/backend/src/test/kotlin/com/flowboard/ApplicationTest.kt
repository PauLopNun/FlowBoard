package com.flowboard

import com.flowboard.module
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlin.test.*

class ApplicationTest {
    
    @Test
    fun `test root endpoint returns welcome message`() = testApplication {
        application {
            module()
        }
        
        val response = client.get("/")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("FlowBoard API is running!", response.bodyAsText())
    }
    
    @Test
    fun `test api endpoints are accessible`() = testApplication {
        application {
            module()
        }
        
        // Test auth endpoints exist (should return method not allowed or unauthorized, not 404)
        val authResponse = client.get("/api/v1/auth/login")
        assertNotEquals(HttpStatusCode.NotFound, authResponse.status)
        
        // Test tasks endpoints exist
        val tasksResponse = client.get("/api/v1/tasks")
        assertNotEquals(HttpStatusCode.NotFound, tasksResponse.status)
    }
}