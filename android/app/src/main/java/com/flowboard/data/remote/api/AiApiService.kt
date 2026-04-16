package com.flowboard.data.remote.api

import com.flowboard.data.remote.ApiConfig
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.Serializable

@Serializable
data class AiChatRequest(
    val prompt: String,
    val documentContext: String? = null
)

@Serializable
data class AiChatResponse(
    val reply: String
)

class AiApiService(private val httpClient: HttpClient) {

    suspend fun ask(
        prompt: String,
        documentContext: String? = null,
        token: String
    ): Result<String> = runCatching {
        val response: AiChatResponse = httpClient.post("${ApiConfig.API_BASE_URL}/ai/ask") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(AiChatRequest(prompt = prompt, documentContext = documentContext))
        }.body()
        response.reply
    }
}
