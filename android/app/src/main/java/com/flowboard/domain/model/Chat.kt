package com.flowboard.domain.model

/**
 * Types of chat rooms
 */
enum class ChatType {
    DIRECT,         // One-on-one chat
    GROUP,          // Group chat
    PROJECT,        // Project-based chat
    TASK_THREAD     // Thread for a specific task
}

/**
 * Message types
 */
enum class MessageType {
    TEXT,
    IMAGE,
    FILE,
    SYSTEM,         // System-generated messages
    TASK_UPDATE,    // Task status change
    MENTION         // Message with mentions
}

/**
 * Message status
 */
enum class MessageStatus {
    SENDING,
    SENT,
    DELIVERED,
    READ,
    FAILED
}

/**
 * Chat room model
 */
data class ChatRoom(
    val id: String,
    val type: ChatType,
    val name: String?,
    val description: String? = null,
    val participants: List<ChatParticipant>,
    val resourceId: String? = null,      // Task/Project/Document ID if applicable
    val resourceType: ResourceType? = null,
    val lastMessage: Message? = null,
    val unreadCount: Int = 0,
    val createdBy: String,
    val createdAt: Long,
    val updatedAt: Long,
    val isArchived: Boolean = false,
    val isMuted: Boolean = false
)

/**
 * Chat participant
 */
data class ChatParticipant(
    val userId: String,
    val userName: String,
    val email: String,
    val avatarUrl: String? = null,
    val role: ChatRole = ChatRole.MEMBER,
    val isOnline: Boolean = false,
    val lastSeen: Long? = null,
    val joinedAt: Long
)

/**
 * Chat role
 */
enum class ChatRole {
    OWNER,
    ADMIN,
    MEMBER
}

/**
 * Message model
 */
data class Message(
    val id: String,
    val chatRoomId: String,
    val senderId: String,
    val senderName: String,
    val type: MessageType,
    val content: String,
    val status: MessageStatus,
    val mentions: List<String> = emptyList(),  // User IDs mentioned
    val replyToId: String? = null,             // Message being replied to
    val attachments: List<MessageAttachment> = emptyList(),
    val reactions: Map<String, List<String>> = emptyMap(), // emoji -> [userIds]
    val metadata: Map<String, String> = emptyMap(),
    val isEdited: Boolean = false,
    val editedAt: Long? = null,
    val createdAt: Long,
    val deliveredAt: Long? = null,
    val readAt: Long? = null
)

/**
 * Message attachment
 */
data class MessageAttachment(
    val id: String,
    val type: AttachmentType,
    val url: String,
    val name: String,
    val size: Long,
    val mimeType: String,
    val thumbnailUrl: String? = null
)

enum class AttachmentType {
    IMAGE,
    VIDEO,
    DOCUMENT,
    AUDIO,
    OTHER
}

/**
 * Typing indicator
 */
data class TypingIndicator(
    val chatRoomId: String,
    val userId: String,
    val userName: String,
    val isTyping: Boolean,
    val timestamp: Long
)

/**
 * Message read receipt
 */
data class ReadReceipt(
    val messageId: String,
    val userId: String,
    val readAt: Long
)

/**
 * Chat statistics
 */
data class ChatStats(
    val totalMessages: Int,
    val unreadCount: Int,
    val participantCount: Int,
    val todayMessages: Int,
    val activeChats: Int
)

/**
 * Message search result
 */
data class MessageSearchResult(
    val message: Message,
    val chatRoom: ChatRoom,
    val matchedText: String,
    val relevanceScore: Float
)
