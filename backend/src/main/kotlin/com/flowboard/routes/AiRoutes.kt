package com.flowboard.routes

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
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
    val documentContext: String? = null,
    val structuredJson: Boolean = false
)

@Serializable
data class AiResponse(
    val reply: String
)

fun Route.aiRoutes() {
    val apiKey = System.getenv("GEMINI_API_KEY") ?: ""
    val configuredModel = System.getenv("GEMINI_MODEL")
        ?.trim()
        ?.removePrefix("models/")
        ?.takeIf { it.isNotBlank() && !it.contains("gemini-1.5", ignoreCase = true) }
    val modelCandidates = listOfNotNull(
        "gemini-2.5-flash",
        configuredModel,
        "gemini-2.0-flash"
    ).distinct()

    val httpClient = HttpClient(CIO) {
        // No ContentNegotiation — JSON body is built and parsed manually
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

            val promptText = "$systemPrompt\n\nUser request:\n${request.prompt}"

            val geminiBody = buildJsonObject {
                putJsonArray("contents") {
                    addJsonObject {
                        put("role", "user")
                        putJsonArray("parts") {
                            addJsonObject { put("text", promptText) }
                        }
                    }
                }
                putJsonObject("generationConfig") {
                    put("maxOutputTokens", 1024)
                    put("temperature", if (request.structuredJson) 0.3 else 0.7)
                    if (request.structuredJson) {
                        put("responseMimeType", "application/json")
                        putJsonObject("responseSchema") {
                            put("type", "OBJECT")
                            putJsonObject("properties") {
                                putJsonObject("text") {
                                    put("type", "STRING")
                                    put("description", "Replacement text for selection or block edits.")
                                }
                                putJsonObject("blocks") {
                                    put("type", "ARRAY")
                                    put("description", "Revised FlowBoard document blocks for document-level edits.")
                                    putJsonObject("items") {
                                        put("type", "OBJECT")
                                        putJsonObject("properties") {
                                            putJsonObject("type") {
                                                put("type", "STRING")
                                                put("description", "One of h1, h2, h3, p, bullet, numbered, todo, quote, callout, code, divider.")
                                            }
                                            putJsonObject("content") {
                                                put("type", "STRING")
                                                put("description", "Plain block content without markdown markers.")
                                            }
                                        }
                                        putJsonArray("required") {
                                            add("type")
                                            add("content")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            try {
                val json = Json { ignoreUnknownKeys = true }
                var parsedResponse: JsonObject? = null
                var lastProviderError = "Upstream AI provider error"
                var lastProviderStatus = HttpStatusCode.BadGateway

                for (candidateModel in modelCandidates) {
                    val response: HttpResponse = httpClient.post(
                        "https://generativelanguage.googleapis.com/v1beta/models/$candidateModel:generateContent"
                    ) {
                        parameter("key", apiKey)
                        contentType(ContentType.Application.Json)
                        setBody(Json.encodeToString(JsonObject.serializer(), geminiBody))
                    }

                    val responseText = response.bodyAsText()

                    if (response.status.isSuccess()) {
                        parsedResponse = json.parseToJsonElement(responseText).jsonObject
                        break
                    }

                    val errorText = runCatching {
                        json.parseToJsonElement(responseText).jsonObject["error"]?.jsonObject?.get("message")?.jsonPrimitive?.content
                    }.getOrNull() ?: "Upstream AI provider error"

                    lastProviderError = errorText
                    lastProviderStatus = when (response.status) {
                        HttpStatusCode.Unauthorized, HttpStatusCode.Forbidden -> HttpStatusCode.BadGateway
                        HttpStatusCode.TooManyRequests -> HttpStatusCode.TooManyRequests
                        HttpStatusCode.RequestTimeout, HttpStatusCode.GatewayTimeout -> HttpStatusCode.GatewayTimeout
                        else -> HttpStatusCode.BadGateway
                    }

                    val canRetryWithFallback =
                        candidateModel != modelCandidates.last() &&
                        (response.status == HttpStatusCode.NotFound ||
                            errorText.contains("not found", ignoreCase = true) ||
                            errorText.contains("not supported", ignoreCase = true))

                    if (canRetryWithFallback) {
                        application.log.warn("Gemini model $candidateModel failed: $errorText. Trying fallback model.")
                        continue
                    }

                    call.respond(lastProviderStatus, mapOf("error" to errorText))
                    return@post
                }

                val parsed = parsedResponse
                if (parsed == null) {
                    call.respond(lastProviderStatus, mapOf("error" to lastProviderError))
                    return@post
                }

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
