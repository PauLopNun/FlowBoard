package com.flowboard.data.local.entities

import androidx.room.*
import com.flowboard.data.local.converters.StringListConverter

@Entity(tableName = "chat_rooms")
@TypeConverters(StringListConverter::class)
data class ChatRoomEntity(
    @PrimaryKey
    val id: String,
    val type: String,
    val name: String?,
    val description: String?,
    val participantIds: List<String>,
    val resourceId: String?,
    val resourceType: String?,
    val lastMessageId: String?,
    val lastMessagePreview: String?,
    val lastMessageTimestamp: Long?,
    val unreadCount: Int = 0,
    val createdBy: String,
    val createdAt: Long,
    val updatedAt: Long,
    val isArchived: Boolean = false,
    val isMuted: Boolean = false
)

@Entity(
    tableName = "messages",
    indices = [
        Index(value = ["chatRoomId", "createdAt"]),
        Index(value = ["senderId"])
    ]
)
@TypeConverters(StringListConverter::class, com.flowboard.data.local.converters.MapConverter::class)
data class MessageEntity(
    @PrimaryKey
    val id: String,
    val chatRoomId: String,
    val senderId: String,
    val senderName: String,
    val type: String,
    val content: String,
    val status: String,
    val mentions: List<String> = emptyList(),
    val replyToId: String? = null,
    val metadata: Map<String, String> = emptyMap(),
    val isEdited: Boolean = false,
    val editedAt: Long? = null,
    val createdAt: Long,
    val deliveredAt: Long? = null,
    val readAt: Long? = null
)

@Entity(tableName = "chat_participants")
data class ChatParticipantEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val chatRoomId: String,
    val userId: String,
    val userName: String,
    val email: String,
    val avatarUrl: String?,
    val role: String,
    val joinedAt: Long,
    val lastSeen: Long?
)

@Entity(tableName = "typing_indicators")
data class TypingIndicatorEntity(
    @PrimaryKey
    val id: String, // chatRoomId_userId
    val chatRoomId: String,
    val userId: String,
    val userName: String,
    val isTyping: Boolean,
    val timestamp: Long
)
