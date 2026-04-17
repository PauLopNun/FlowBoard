package com.flowboard.data.remote.api

import com.flowboard.data.remote.ApiConfig
import com.flowboard.data.repository.AuthRepository
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class ChatRoomDto(
    val id: String,
    val type: String,
    val name: String? = null,
    val description: String? = null,
    val participantIds: List<String> = emptyList(),
    val resourceId: String? = null,
    val resourceType: String? = null,
    val lastMessagePreview: String? = null,
    val lastMessageTimestamp: Long? = null,
    val unreadCount: Int = 0,
    val createdBy: String,
    val createdAt: Long,
    val updatedAt: Long,
    val isArchived: Boolean = false,
    val isMuted: Boolean = false
)

@Serializable
data class MessageDto(
    val id: String,
    val chatRoomId: String,
    val senderId: String,
    val senderName: String,
    val type: String = "text",
    val content: String,
    val status: String = "sent",
    val mentions: List<String> = emptyList(),
    val replyToId: String? = null,
    val isEdited: Boolean = false,
    val editedAt: Long? = null,
    val createdAt: Long,
    val deliveredAt: Long? = null,
    val readAt: Long? = null
)

@Serializable
private data class CreateChatRoomRequest(
    val type: String,
    val name: String? = null,
    val participantIds: List<String> = emptyList(),
    val resourceId: String? = null,
    val resourceType: String? = null
)

@Serializable
private data class SendMessageRequest(
    val content: String,
    val type: String = "text",
    val replyToId: String? = null,
    val mentions: List<String> = emptyList()
)

@Singleton
class ChatApiService @Inject constructor(
    private val httpClient: HttpClient,
    private val authRepository: AuthRepository
) {
    companion object {
        private val CHAT_ENDPOINT = "${ApiConfig.API_BASE_URL}/chat"
    }

    private suspend fun authHeader() =
        authRepository.getToken()?.let { "Bearer $it" } ?: throw Exception("Not authenticated")

    suspend fun getChatRooms(): List<ChatRoomDto> =
        httpClient.get("$CHAT_ENDPOINT/rooms") {
            header(HttpHeaders.Authorization, authHeader())
        }.body()

    suspend fun getChatRoom(id: String): ChatRoomDto =
        httpClient.get("$CHAT_ENDPOINT/rooms/$id") {
            header(HttpHeaders.Authorization, authHeader())
        }.body()

    suspend fun createChatRoom(type: String, name: String?, participantIds: List<String>, resourceId: String? = null, resourceType: String? = null): ChatRoomDto =
        httpClient.post("$CHAT_ENDPOINT/rooms") {
            header(HttpHeaders.Authorization, authHeader())
            contentType(ContentType.Application.Json)
            setBody(CreateChatRoomRequest(type, name, participantIds, resourceId, resourceType))
        }.body()

    suspend fun getMessages(chatRoomId: String, limit: Int = 50, offset: Int = 0): List<MessageDto> =
        httpClient.get("$CHAT_ENDPOINT/rooms/$chatRoomId/messages") {
            header(HttpHeaders.Authorization, authHeader())
            parameter("limit", limit)
            parameter("offset", offset)
        }.body()

    suspend fun sendMessage(chatRoomId: String, content: String, replyToId: String? = null, mentions: List<String> = emptyList()): MessageDto =
        httpClient.post("$CHAT_ENDPOINT/rooms/$chatRoomId/messages") {
            header(HttpHeaders.Authorization, authHeader())
            contentType(ContentType.Application.Json)
            setBody(SendMessageRequest(content, replyToId = replyToId, mentions = mentions))
        }.body()

    suspend fun deleteMessage(messageId: String) {
        httpClient.delete("$CHAT_ENDPOINT/messages/$messageId") {
            header(HttpHeaders.Authorization, authHeader())
        }
    }
}
