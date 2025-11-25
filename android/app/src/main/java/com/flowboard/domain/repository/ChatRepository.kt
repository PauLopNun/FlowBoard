package com.flowboard.domain.repository

import com.flowboard.domain.model.*
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for chat functionality
 */
interface ChatRepository {

    // ==================== Chat Rooms ====================

    /**
     * Get all chat rooms
     */
    fun getAllChatRooms(): Flow<List<ChatRoom>>

    /**
     * Get a specific chat room
     */
    fun getChatRoom(chatRoomId: String): Flow<ChatRoom?>

    /**
     * Get archived chat rooms
     */
    fun getArchivedChatRooms(): Flow<List<ChatRoom>>

    /**
     * Get total unread message count
     */
    fun getTotalUnreadCount(): Flow<Int>

    /**
     * Create a new chat room
     */
    suspend fun createChatRoom(
        type: ChatType,
        name: String?,
        participantIds: List<String>,
        resourceId: String? = null,
        resourceType: ResourceType? = null
    ): Result<ChatRoom>

    /**
     * Archive/unarchive chat room
     */
    suspend fun archiveChatRoom(chatRoomId: String, isArchived: Boolean = true)

    /**
     * Mute/unmute chat room
     */
    suspend fun muteChatRoom(chatRoomId: String, isMuted: Boolean = true)

    /**
     * Clear unread count
     */
    suspend fun clearUnreadCount(chatRoomId: String)

    /**
     * Delete chat room
     */
    suspend fun deleteChatRoom(chatRoomId: String)

    // ==================== Messages ====================

    /**
     * Get messages for a chat room
     */
    fun getMessages(chatRoomId: String): Flow<List<Message>>

    /**
     * Send a message
     */
    suspend fun sendMessage(
        chatRoomId: String,
        content: String,
        type: MessageType = MessageType.TEXT,
        replyToId: String? = null,
        mentions: List<String> = emptyList()
    ): Result<Message>

    /**
     * Edit a message
     */
    suspend fun editMessage(messageId: String, newContent: String): Result<Message>

    /**
     * Delete a message
     */
    suspend fun deleteMessage(messageId: String)

    /**
     * Mark messages as read
     */
    suspend fun markMessagesAsRead(chatRoomId: String)

    /**
     * Add reaction to message
     */
    suspend fun addReaction(messageId: String, emoji: String)

    /**
     * Remove reaction from message
     */
    suspend fun removeReaction(messageId: String, emoji: String)

    /**
     * Search messages
     */
    suspend fun searchMessages(query: String): List<MessageSearchResult>

    // ==================== Participants ====================

    /**
     * Get chat participants
     */
    fun getChatParticipants(chatRoomId: String): Flow<List<ChatParticipant>>

    /**
     * Add participant to chat
     */
    suspend fun addParticipant(chatRoomId: String, userId: String): Result<Unit>

    /**
     * Remove participant from chat
     */
    suspend fun removeParticipant(chatRoomId: String, userId: String): Result<Unit>

    // ==================== Typing Indicators ====================

    /**
     * Get typing indicators for a chat
     */
    fun getTypingIndicators(chatRoomId: String): Flow<List<TypingIndicator>>

    /**
     * Send typing indicator
     */
    suspend fun sendTypingIndicator(chatRoomId: String, isTyping: Boolean)

    // ==================== Real-time Updates ====================

    /**
     * Connect to chat WebSocket
     */
    suspend fun connectToChat(chatRoomId: String)

    /**
     * Disconnect from chat WebSocket
     */
    suspend fun disconnectFromChat(chatRoomId: String)
}
