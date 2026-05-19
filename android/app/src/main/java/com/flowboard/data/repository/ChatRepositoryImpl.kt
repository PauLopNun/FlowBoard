package com.flowboard.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.flowboard.data.local.dao.ChatDao
import com.flowboard.data.local.entities.*
import com.flowboard.data.remote.api.ChatApiService
import com.flowboard.data.remote.api.ChatParticipantDto
import com.flowboard.domain.model.*
import com.flowboard.domain.repository.ChatRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val chatDao: ChatDao,
    private val authRepository: AuthRepository,
    private val chatApiService: ChatApiService,
    private val dataStore: DataStore<Preferences>
) : ChatRepository {

    companion object {
        private val DELETED_CHAT_IDS = stringSetPreferencesKey("deleted_chat_room_ids")
    }

    private suspend fun deletedIds(): Set<String> =
        dataStore.data.first()[DELETED_CHAT_IDS] ?: emptySet()

    private suspend fun markDeleted(chatRoomId: String) {
        dataStore.edit { prefs ->
            val current = prefs[DELETED_CHAT_IDS] ?: emptySet()
            prefs[DELETED_CHAT_IDS] = current + chatRoomId
        }
    }

    override fun getAllChatRooms(): Flow<List<ChatRoom>> {
        return combine(
            chatDao.getAllChatRooms(),
            chatDao.getAllParticipants()
        ) { rooms, participants ->
            val participantsByRoom = participants.groupBy { it.chatRoomId }
            rooms.map { roomEntity ->
                roomEntity.toChatRoom(participantsByRoom[roomEntity.id].orEmpty())
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
        return combine(
            chatDao.getArchivedChatRooms(),
            chatDao.getAllParticipants()
        ) { rooms, participants ->
            val participantsByRoom = participants.groupBy { it.chatRoomId }
            rooms.map { roomEntity ->
                roomEntity.toChatRoom(participantsByRoom[roomEntity.id].orEmpty())
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
    ): Result<ChatRoom> = runCatching {
        val currentUserId = authRepository.getUserId() ?: throw Exception("Not authenticated")
        val dto = chatApiService.createChatRoom(
            type = type.name.lowercase(),
            name = name,
            participantIds = participantIds,
            resourceId = resourceId,
            resourceType = resourceType?.name?.lowercase()
        )
        val entity = dto.toEntity()
        val participants = dto.participants.map { it.toEntity(dto.id) }
        chatDao.insertChatRoom(entity)
        chatDao.removeAllParticipants(entity.id)
        if (participants.isNotEmpty()) {
            chatDao.insertParticipants(participants)
        }
        entity.toChatRoom(participants)
    }

    suspend fun refreshChatRooms() {
        val deleted = deletedIds()
        val rooms = chatApiService.getChatRooms().filter { it.id !in deleted }
        val roomEntities = rooms.map { it.toEntity() }
        val participantEntities = rooms.flatMap { dto ->
            dto.participants.map { it.toEntity(dto.id) }
        }
        chatDao.replaceChatRooms(roomEntities, participantEntities)
    }

    suspend fun refreshMessages(chatRoomId: String) {
        val messages = chatApiService.getMessages(chatRoomId)
        messages.forEach { chatDao.insertMessage(it.toMessageEntity()) }
    }

    suspend fun updateOwnParticipantAvatar(userId: String, avatarUrl: String?) {
        chatDao.updateParticipantAvatar(userId, avatarUrl)
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
        markDeleted(chatRoomId)
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
            val currentUserName = authRepository.getUserName() ?: "Anonymous"

            val messageId = UUID.randomUUID().toString()
            val now = System.currentTimeMillis()

            val messageEntity = MessageEntity(
                id = messageId,
                chatRoomId = chatRoomId,
                senderId = currentUserId,
                senderName = currentUserName,
                type = type.name.lowercase(),
                content = content,
                status = MessageStatus.SENT.name.lowercase(),
                mentions = mentions,
                replyToId = replyToId,
                createdAt = now,
                deliveredAt = now
            )

            chatDao.insertMessage(messageEntity)

            // Update chat room's last message preview locally
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

            // Send to backend and replace with server-assigned message
            val serverMsg = chatApiService.sendMessage(chatRoomId, content, replyToId, mentions)
            val serverEntity = serverMsg.toMessageEntity()
            chatDao.deleteMessage(messageEntity)
            chatDao.insertMessage(serverEntity)

            Result.success(serverEntity.toMessage())
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
        val currentUserName = authRepository.getUserName() ?: "Anonymous"

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
            lastMessage = if (!lastMessagePreview.isNullOrBlank()) Message(
                id = lastMessageId ?: "",
                chatRoomId = id,
                senderId = "",
                senderName = "",
                type = MessageType.TEXT,
                content = lastMessagePreview,
                status = MessageStatus.SENT,
                createdAt = lastMessageTimestamp ?: updatedAt
            ) else null,
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

private fun com.flowboard.data.remote.api.ChatRoomDto.toEntity() = ChatRoomEntity(
    id = id,
    type = type,
    name = name,
    description = description,
    participantIds = participantIds.ifEmpty { participants.map { it.userId } },
    resourceId = resourceId,
    resourceType = resourceType,
    lastMessageId = null,
    lastMessagePreview = lastMessagePreview ?: lastMessage?.content,
    lastMessageTimestamp = lastMessageTimestamp ?: lastMessage?.createdAt?.toEpochMillis(),
    unreadCount = unreadCount,
    createdBy = createdBy,
    createdAt = createdAt.toEpochMillis(),
    updatedAt = updatedAt.toEpochMillis(),
    isArchived = isArchived,
    isMuted = isMuted
)

private fun ChatParticipantDto.toEntity(chatRoomId: String) = ChatParticipantEntity(
    chatRoomId = chatRoomId,
    userId = userId,
    userName = userName,
    email = email,
    avatarUrl = avatarUrl,
    role = role,
    joinedAt = joinedAt.toEpochMillis(),
    lastSeen = lastSeen?.toEpochMillis()
)

private fun com.flowboard.data.remote.api.MessageDto.toMessageEntity() = MessageEntity(
    id = id,
    chatRoomId = chatRoomId,
    senderId = senderId,
    senderName = senderName,
    type = type,
    content = content,
    status = status,
    mentions = mentions,
    replyToId = replyToId,
    isEdited = isEdited,
    editedAt = editedAt?.toEpochMillis(),
    createdAt = createdAt.toEpochMillis(),
    deliveredAt = deliveredAt?.toEpochMillis(),
    readAt = readAt?.toEpochMillis()
)

private fun LocalDateTime.toEpochMillis(): Long =
    toInstant(TimeZone.UTC).toEpochMilliseconds()
