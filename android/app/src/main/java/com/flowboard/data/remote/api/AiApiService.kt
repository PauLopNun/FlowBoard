package com.flowboard.data.remote.api

import com.flowboard.data.remote.ApiConfig
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Serializable
data class AiChatRequest(
    val prompt: String,
    val documentContext: String? = null,
    val structuredJson: Boolean = false
)

@Serializable
data class AiChatResponse(
    val reply: String
)

class AiApiService(private val httpClient: HttpClient) {

    suspend fun ask(
        prompt: String,
        documentContext: String? = null,
        token: String,
        structuredJson: Boolean = false
    ): Result<String> = runCatching {
        // Strip JSON-invalid control characters that document blocks may contain
        val safeContext = documentContext
            ?.replace(Regex("[\u0000-\u0008\u000B\u000C\u000E-\u001F\u007F]"), "")
            ?.takeIf { it.isNotBlank() }
        val response = httpClient.post("${ApiConfig.API_BASE_URL}/ai/ask") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(
                AiChatRequest(
                    prompt = prompt,
                    documentContext = safeContext,
                    structuredJson = structuredJson
                )
            )
        }

        if (!response.status.isSuccess()) {
            val body = response.bodyAsText()
            val backendMessage = runCatching {
                Json.parseToJsonElement(body).jsonObject["error"]?.jsonPrimitive?.content
            }.getOrNull()

            val message = backendMessage ?: when (response.status) {
                HttpStatusCode.Unauthorized -> "Session expired. Please sign in again."
                HttpStatusCode.ServiceUnavailable -> "AI service is not configured on the server."
                HttpStatusCode.GatewayTimeout -> "AI request timed out. Please try again."
                else -> "AI request failed (${response.status.value})"
            }
            throw Exception(message)
        }

        val body = response.bodyAsText()
        runCatching {
            Json.decodeFromString(AiChatResponse.serializer(), body).reply
        }.getOrElse {
            body.ifBlank { throw Exception("AI returned an empty response") }
        }
    }
}
