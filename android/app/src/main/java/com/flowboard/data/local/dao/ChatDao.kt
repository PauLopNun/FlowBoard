package com.flowboard.data.local.dao

import androidx.room.*
import com.flowboard.data.local.entities.ChatParticipantEntity
import com.flowboard.data.local.entities.ChatRoomEntity
import com.flowboard.data.local.entities.MessageEntity
import com.flowboard.data.local.entities.TypingIndicatorEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {

    // ==================== Chat Rooms ====================

    @Query("SELECT * FROM chat_rooms WHERE isArchived = 0 ORDER BY updatedAt DESC")
    fun getAllChatRooms(): Flow<List<ChatRoomEntity>>

    @Query("SELECT * FROM chat_rooms WHERE id = :chatRoomId")
    fun getChatRoom(chatRoomId: String): Flow<ChatRoomEntity?>

    @Query("SELECT * FROM chat_rooms WHERE id = :chatRoomId")
    suspend fun getChatRoomSync(chatRoomId: String): ChatRoomEntity?

    @Query("SELECT * FROM chat_rooms WHERE isArchived = 1 ORDER BY updatedAt DESC")
    fun getArchivedChatRooms(): Flow<List<ChatRoomEntity>>

    @Query("SELECT SUM(unreadCount) FROM chat_rooms WHERE isArchived = 0")
    fun getTotalUnreadCount(): Flow<Int?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatRoom(chatRoom: ChatRoomEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatRooms(chatRooms: List<ChatRoomEntity>)

    @Update
    suspend fun updateChatRoom(chatRoom: ChatRoomEntity)

    @Query("UPDATE chat_rooms SET unreadCount = 0 WHERE id = :chatRoomId")
    suspend fun clearUnreadCount(chatRoomId: String)

    @Query("UPDATE chat_rooms SET isArchived = :isArchived WHERE id = :chatRoomId")
    suspend fun archiveChatRoom(chatRoomId: String, isArchived: Boolean = true)

    @Query("UPDATE chat_rooms SET isMuted = :isMuted WHERE id = :chatRoomId")
    suspend fun muteChatRoom(chatRoomId: String, isMuted: Boolean = true)

    @Delete
    suspend fun deleteChatRoom(chatRoom: ChatRoomEntity)

    // ==================== Messages ====================

    @Query("SELECT * FROM messages WHERE chatRoomId = :chatRoomId ORDER BY createdAt ASC")
    fun getMessages(chatRoomId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE chatRoomId = :chatRoomId ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getRecentMessages(chatRoomId: String, limit: Int = 50): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE id = :messageId")
    suspend fun getMessage(messageId: String): MessageEntity?

    @Query("SELECT * FROM messages WHERE chatRoomId = :chatRoomId AND createdAt > :since ORDER BY createdAt ASC")
    fun getMessagesSince(chatRoomId: String, since: Long): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE content LIKE '%' || :query || '%' ORDER BY createdAt DESC")
    suspend fun searchMessages(query: String): List<MessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageEntity>)

    @Update
    suspend fun updateMessage(message: MessageEntity)

    @Query("UPDATE messages SET status = :status WHERE id = :messageId")
    suspend fun updateMessageStatus(messageId: String, status: String)

    @Query("UPDATE messages SET readAt = :readAt, status = 'read' WHERE chatRoomId = :chatRoomId AND senderId != :currentUserId AND readAt IS NULL")
    suspend fun markMessagesAsRead(chatRoomId: String, currentUserId: String, readAt: Long)

    @Delete
    suspend fun deleteMessage(message: MessageEntity)

    @Query("DELETE FROM messages WHERE chatRoomId = :chatRoomId")
    suspend fun deleteAllMessages(chatRoomId: String)

    // ==================== Participants ====================

    @Query("SELECT * FROM chat_participants WHERE chatRoomId = :chatRoomId")
    fun getChatParticipants(chatRoomId: String): Flow<List<ChatParticipantEntity>>

    @Query("SELECT * FROM chat_participants WHERE chatRoomId = :chatRoomId")
    suspend fun getChatParticipantsSync(chatRoomId: String): List<ChatParticipantEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParticipant(participant: ChatParticipantEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParticipants(participants: List<ChatParticipantEntity>)

    @Query("DELETE FROM chat_participants WHERE chatRoomId = :chatRoomId AND userId = :userId")
    suspend fun removeParticipant(chatRoomId: String, userId: String)

    @Query("DELETE FROM chat_participants WHERE chatRoomId = :chatRoomId")
    suspend fun removeAllParticipants(chatRoomId: String)

    // ==================== Typing Indicators ====================

    @Query("SELECT * FROM typing_indicators WHERE chatRoomId = :chatRoomId AND isTyping = 1")
    fun getTypingIndicators(chatRoomId: String): Flow<List<TypingIndicatorEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTypingIndicator(indicator: TypingIndicatorEntity)

    @Query("DELETE FROM typing_indicators WHERE chatRoomId = :chatRoomId AND userId = :userId")
    suspend fun removeTypingIndicator(chatRoomId: String, userId: String)

    @Query("DELETE FROM typing_indicators WHERE timestamp < :threshold")
    suspend fun cleanupOldTypingIndicators(threshold: Long)
}
