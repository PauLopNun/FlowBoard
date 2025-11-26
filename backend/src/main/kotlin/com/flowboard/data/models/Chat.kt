package com.flowboard.data.models

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class ChatRoom(
    val id: String,
    val type: String, // DIRECT, GROUP, PROJECT, TASK_THREAD
    val name: String? = null,
    val description: String? = null,
    val resourceId: String? = null,
    val resourceType: String? = null,
    val participants: List<ChatParticipant> = emptyList(),
    val lastMessage: Message? = null,
    val unreadCount: Int = 0,
    val createdBy: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val isArchived: Boolean = false
)

@Serializable
data class ChatParticipant(
    val userId: String,
    val userName: String,
    val email: String,
    val role: String = "MEMBER", // OWNER, ADMIN, MEMBER
    val isOnline: Boolean = false,
    val joinedAt: LocalDateTime,
    val isMuted: Boolean = false
)

@Serializable
data class Message(
    val id: String,
    val chatRoomId: String,
    val senderId: String,
    val senderName: String,
    val type: String = "TEXT",
    val content: String,
    val replyToId: String? = null,
    val isEdited: Boolean = false,
    val editedAt: LocalDateTime? = null,
    val createdAt: LocalDateTime,
    val metadata: Map<String, String> = emptyMap()
)

@Serializable
data class CreateChatRoomRequest(
    val type: String,
    val name: String? = null,
    val description: String? = null,
    val participantIds: List<String>,
    val resourceId: String? = null,
    val resourceType: String? = null
)

@Serializable
data class SendMessageRequest(
    val content: String,
    val type: String = "TEXT",
    val replyToId: String? = null,
    val metadata: Map<String, String> = emptyMap()
)

@Serializable
data class UpdateMessageRequest(
    val content: String
)

@Serializable
data class ChatRoomListResponse(
    val chatRooms: List<ChatRoom>,
    val totalUnreadCount: Int
)
