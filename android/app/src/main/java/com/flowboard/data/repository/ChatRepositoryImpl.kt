package com.flowboard.data.repository

import com.flowboard.data.local.dao.ChatDao
import com.flowboard.domain.model.*
import com.flowboard.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton
import com.flowboard.data.local.entities.*
import java.util.UUID

@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val chatDao: ChatDao,
    private val authRepository: AuthRepository
) : ChatRepository {

    override fun getAllChatRooms(): Flow<List<ChatRoom>> {
        return combine(
            chatDao.getAllChatRooms(),
            chatDao.getAllChatRooms().map { it.map { room -> room.id } }
        ) { rooms, ids ->
            rooms.map { roomEntity ->
                val participants = chatDao.getChatParticipantsSync(roomEntity.id)
                roomEntity.toChatRoom(participants)
            }
        }
    }

    override fun getChatRoom(chatRoomId: String): Flow<ChatRoom?> {
        return combine(
            chatDao.getChatRoom(chatRoomId),
            chatDao.getChatParticipants(chatRoomId)
        ) { room, participants ->
            room?.toChatRoom(participants)
        }
    }

    override fun getArchivedChatRooms(): Flow<List<ChatRoom>> {
        return chatDao.getArchivedChatRooms().map { rooms ->
            rooms.map { roomEntity ->
                val participants = chatDao.getChatParticipantsSync(roomEntity.id)
                roomEntity.toChatRoom(participants)
            }
        }
    }

    override fun getTotalUnreadCount(): Flow<Int> {
        return chatDao.getTotalUnreadCount().map { it ?: 0 }
    }

    override suspend fun createChatRoom(
        type: ChatType,
        name: String?,
        participantIds: List<String>,
        resourceId: String?,
        resourceType: ResourceType?
    ): Result<ChatRoom> {
        return try {
            val currentUserId = authRepository.getUserId() ?: return Result.failure(Exception("Not authenticated"))
            val chatRoomId = UUID.randomUUID().toString()
            val now = System.currentTimeMillis()

            val chatRoomEntity = ChatRoomEntity(
                id = chatRoomId,
                type = type.name.lowercase(),
                name = name,
                description = null,
                participantIds = participantIds,
                resourceId = resourceId,
                resourceType = resourceType?.name?.lowercase(),
                lastMessageId = null,
                lastMessagePreview = null,
                lastMessageTimestamp = null,
                unreadCount = 0,
                createdBy = currentUserId,
                createdAt = now,
                updatedAt = now,
                isArchived = false,
                isMuted = false
            )

            chatDao.insertChatRoom(chatRoomEntity)

            // TODO: Add participants from user data
            val chatRoom = ChatRoom(
                id = chatRoomId,
                type = type,
                name = name,
                participants = emptyList(),
                resourceId = resourceId,
                resourceType = resourceType,
                createdBy = currentUserId,
                createdAt = now,
                updatedAt = now
            )

            Result.success(chatRoom)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun archiveChatRoom(chatRoomId: String, isArchived: Boolean) {
        chatDao.archiveChatRoom(chatRoomId, isArchived)
    }

    override suspend fun muteChatRoom(chatRoomId: String, isMuted: Boolean) {
        chatDao.muteChatRoom(chatRoomId, isMuted)
    }

    override suspend fun clearUnreadCount(chatRoomId: String) {
        chatDao.clearUnreadCount(chatRoomId)
    }

    override suspend fun deleteChatRoom(chatRoomId: String) {
        chatDao.getChatRoomSync(chatRoomId)?.let {
            chatDao.deleteChatRoom(it)
            chatDao.deleteAllMessages(chatRoomId)
            chatDao.removeAllParticipants(chatRoomId)
        }
    }

    override fun getMessages(chatRoomId: String): Flow<List<Message>> {
        return chatDao.getMessages(chatRoomId).map { messages ->
            messages.map { it.toMessage() }
        }
    }

    override suspend fun sendMessage(
        chatRoomId: String,
        content: String,
        type: MessageType,
        replyToId: String?,
        mentions: List<String>
    ): Result<Message> {
        return try {
            val currentUserId = authRepository.getUserId() ?: return Result.failure(Exception("Not authenticated"))
            val currentUserName = "Current User" // TODO: Get from auth

            val messageId = UUID.randomUUID().toString()
            val now = System.currentTimeMillis()

            val messageEntity = MessageEntity(
                id = messageId,
                chatRoomId = chatRoomId,
                senderId = currentUserId,
                senderName = currentUserName,
                type = type.name.lowercase(),
                content = content,
                status = MessageStatus.SENDING.name.lowercase(),
                mentions = mentions,
                replyToId = replyToId,
                createdAt = now
            )

            chatDao.insertMessage(messageEntity)

            // Update chat room's last message
            chatDao.getChatRoomSync(chatRoomId)?.let { room ->
                chatDao.updateChatRoom(
                    room.copy(
                        lastMessageId = messageId,
                        lastMessagePreview = content.take(100),
                        lastMessageTimestamp = now,
                        updatedAt = now
                    )
                )
            }

            // TODO: Send via WebSocket

            Result.success(messageEntity.toMessage())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun editMessage(messageId: String, newContent: String): Result<Message> {
        return try {
            chatDao.getMessage(messageId)?.let { message ->
                val updated = message.copy(
                    content = newContent,
                    isEdited = true,
                    editedAt = System.currentTimeMillis()
                )
                chatDao.updateMessage(updated)
                Result.success(updated.toMessage())
            } ?: Result.failure(Exception("Message not found"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteMessage(messageId: String) {
        chatDao.getMessage(messageId)?.let {
            chatDao.deleteMessage(it)
        }
    }

    override suspend fun markMessagesAsRead(chatRoomId: String) {
        val currentUserId = authRepository.getUserId() ?: return
        chatDao.markMessagesAsRead(chatRoomId, currentUserId, System.currentTimeMillis())
        chatDao.clearUnreadCount(chatRoomId)
    }

    override suspend fun addReaction(messageId: String, emoji: String) {
        // TODO: Implement reactions
    }

    override suspend fun removeReaction(messageId: String, emoji: String) {
        // TODO: Implement reactions
    }

    override suspend fun searchMessages(query: String): List<MessageSearchResult> {
        // TODO: Implement search with chat room info
        return emptyList()
    }

    override fun getChatParticipants(chatRoomId: String): Flow<List<ChatParticipant>> {
        return chatDao.getChatParticipants(chatRoomId).map { participants ->
            participants.map { it.toParticipant() }
        }
    }

    override suspend fun addParticipant(chatRoomId: String, userId: String): Result<Unit> {
        // TODO: Implement with user data
        return Result.success(Unit)
    }

    override suspend fun removeParticipant(chatRoomId: String, userId: String): Result<Unit> {
        return try {
            chatDao.removeParticipant(chatRoomId, userId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getTypingIndicators(chatRoomId: String): Flow<List<TypingIndicator>> {
        return chatDao.getTypingIndicators(chatRoomId).map { indicators ->
            indicators.map { it.toTypingIndicator() }
        }
    }

    override suspend fun sendTypingIndicator(chatRoomId: String, isTyping: Boolean) {
        val currentUserId = authRepository.getUserId() ?: return
        val currentUserName = "Current User" // TODO: Get from auth

        if (isTyping) {
            val indicator = TypingIndicatorEntity(
                id = "${chatRoomId}_$currentUserId",
                chatRoomId = chatRoomId,
                userId = currentUserId,
                userName = currentUserName,
                isTyping = true,
                timestamp = System.currentTimeMillis()
            )
            chatDao.insertTypingIndicator(indicator)
        } else {
            chatDao.removeTypingIndicator(chatRoomId, currentUserId)
        }

        // TODO: Send via WebSocket
    }

    override suspend fun connectToChat(chatRoomId: String) {
        // TODO: Implement WebSocket connection
    }

    override suspend fun disconnectFromChat(chatRoomId: String) {
        // TODO: Implement WebSocket disconnection
    }

    // Helper extension functions
    private fun ChatRoomEntity.toChatRoom(participants: List<ChatParticipantEntity>): ChatRoom {
        return ChatRoom(
            id = id,
            type = ChatType.valueOf(type.uppercase()),
            name = name,
            description = description,
            participants = participants.map { it.toParticipant() },
            resourceId = resourceId,
            resourceType = resourceType?.let { ResourceType.valueOf(it.uppercase()) },
            lastMessage = null, // TODO: Load from lastMessageId
            unreadCount = unreadCount,
            createdBy = createdBy,
            createdAt = createdAt,
            updatedAt = updatedAt,
            isArchived = isArchived,
            isMuted = isMuted
        )
    }

    private fun MessageEntity.toMessage(): Message {
        return Message(
            id = id,
            chatRoomId = chatRoomId,
            senderId = senderId,
            senderName = senderName,
            type = MessageType.valueOf(type.uppercase()),
            content = content,
            status = MessageStatus.valueOf(status.uppercase()),
            mentions = mentions,
            replyToId = replyToId,
            metadata = metadata,
            isEdited = isEdited,
            editedAt = editedAt,
            createdAt = createdAt,
            deliveredAt = deliveredAt,
            readAt = readAt
        )
    }

    private fun ChatParticipantEntity.toParticipant(): ChatParticipant {
        return ChatParticipant(
            userId = userId,
            userName = userName,
            email = email,
            avatarUrl = avatarUrl,
            role = ChatRole.valueOf(role.uppercase()),
            joinedAt = joinedAt,
            lastSeen = lastSeen
        )
    }

    private fun TypingIndicatorEntity.toTypingIndicator(): TypingIndicator {
        return TypingIndicator(
            chatRoomId = chatRoomId,
            userId = userId,
            userName = userName,
            isTyping = isTyping,
            timestamp = timestamp
        )
    }
}
