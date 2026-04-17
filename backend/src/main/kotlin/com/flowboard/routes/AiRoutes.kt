package com.flowboard.routes

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

@Serializable
data class AiRequest(
    val prompt: String,
    val documentContext: String? = null
)

@Serializable
data class AiResponse(
    val reply: String
)

fun Route.aiRoutes() {
    val apiKey = System.getenv("GEMINI_API_KEY") ?: ""

    val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) { json() }
    }

    authenticate("auth-jwt") {
        post("/ai/ask") {
            if (apiKey.isBlank()) {
                call.respond(HttpStatusCode.ServiceUnavailable, mapOf("error" to "AI assistant not configured"))
                return@post
            }

            val request = call.receive<AiRequest>()

            val systemPrompt = if (!request.documentContext.isNullOrBlank())
                "You are a helpful writing assistant integrated in FlowBoard, a Notion-like app. " +
                "The user is working on a document with the following content:\n\n${request.documentContext.take(3000)}\n\n" +
                "Help them with their request. Be concise and direct."
            else
                "You are a helpful writing assistant integrated in FlowBoard, a Notion-like app. Be concise and direct."

            val geminiBody = buildJsonObject {
                putJsonArray("contents") {
                    addJsonObject {
                        putJsonArray("parts") {
                            addJsonObject { put("text", "$systemPrompt\n\nUser: ${request.prompt}") }
                        }
                    }
                }
                putJsonObject("generationConfig") {
                    put("maxOutputTokens", 1024)
                    put("temperature", 0.7)
                }
            }

            try {
                val response: HttpResponse = httpClient.post(
                    "https://generativelanguage.googleapis.com/v1/models/gemini-2.0-flash:generateContent"
                ) {
                    parameter("key", apiKey)
                    contentType(ContentType.Application.Json)
                    setBody(geminiBody.toString())
                }

                val responseText = response.bodyAsText()
                val json = Json { ignoreUnknownKeys = true }
                val parsed = json.parseToJsonElement(responseText).jsonObject
                val reply = parsed["candidates"]
                    ?.jsonArray?.firstOrNull()?.jsonObject
                    ?.get("content")?.jsonObject
                    ?.get("parts")?.jsonArray?.firstOrNull()?.jsonObject
                    ?.get("text")?.jsonPrimitive?.content
                    ?: "Sorry, I couldn't generate a response."

                call.respond(AiResponse(reply = reply))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (e.message ?: "AI request failed")))
            }
        }
    }
}
