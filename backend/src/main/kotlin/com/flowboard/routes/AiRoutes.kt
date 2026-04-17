package com.flowboard.routes

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.HttpTimeout
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
        install(HttpTimeout) {
            requestTimeoutMillis = 45_000
            connectTimeoutMillis = 15_000
            socketTimeoutMillis = 45_000
        }
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
                putJsonObject("systemInstruction") {
                    putJsonArray("parts") {
                        addJsonObject { put("text", systemPrompt) }
                    }
                }
                putJsonArray("contents") {
                    addJsonObject {
                        put("role", "user")
                        putJsonArray("parts") {
                            addJsonObject { put("text", request.prompt) }
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
                    setBody(geminiBody)
                }

                val responseText = response.bodyAsText()

                if (!response.status.isSuccess()) {
                    val errorText = runCatching {
                        Json.parseToJsonElement(responseText).jsonObject["error"]?.jsonObject?.get("message")?.jsonPrimitive?.content
                    }.getOrNull() ?: "Upstream AI provider error"

                    val status = when (response.status) {
                        HttpStatusCode.Unauthorized, HttpStatusCode.Forbidden -> HttpStatusCode.BadGateway
                        HttpStatusCode.TooManyRequests -> HttpStatusCode.TooManyRequests
                        HttpStatusCode.RequestTimeout, HttpStatusCode.GatewayTimeout -> HttpStatusCode.GatewayTimeout
                        else -> HttpStatusCode.BadGateway
                    }

                    call.respond(status, mapOf("error" to errorText))
                    return@post
                }

                val json = Json { ignoreUnknownKeys = true }
                val parsed = json.parseToJsonElement(responseText).jsonObject
                val reply = parsed["candidates"]
                    ?.jsonArray
                    ?.mapNotNull { it.jsonObject["content"]?.jsonObject }
                    ?.flatMap { content ->
                        content["parts"]
                            ?.jsonArray
                            ?.mapNotNull { part -> part.jsonObject["text"]?.jsonPrimitive?.contentOrNull }
                            ?: emptyList()
                    }
                    ?.joinToString("\n")
                    ?.trim()
                    .orEmpty()

                if (reply.isNotBlank()) {
                    call.respond(AiResponse(reply = reply))
                    return@post
                }

                val blockReason = parsed["promptFeedback"]
                    ?.jsonObject
                    ?.get("blockReason")
                    ?.jsonPrimitive
                    ?.contentOrNull

                val finishReason = parsed["candidates"]
                    ?.jsonArray
                    ?.firstOrNull()
                    ?.jsonObject
                    ?.get("finishReason")
                    ?.jsonPrimitive
                    ?.contentOrNull

                val emptyReplyError = when {
                    !blockReason.isNullOrBlank() -> "AI blocked the request: $blockReason"
                    !finishReason.isNullOrBlank() -> "AI returned no text (finishReason=$finishReason)"
                    else -> "AI returned an empty response"
                }

                application.log.warn("AI empty response: $emptyReplyError")
                call.respond(HttpStatusCode.BadGateway, mapOf("error" to emptyReplyError))
                return@post
            } catch (e: Exception) {
                application.log.error("AI request failed", e)
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (e.message ?: "AI request failed")))
            }
        }
    }
}
