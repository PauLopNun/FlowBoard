package com.flowboard.domain

import com.flowboard.data.database.DatabaseFactory.dbQuery
import com.flowboard.data.database.ChatRooms
import com.flowboard.data.database.ChatParticipants
import com.flowboard.data.database.Messages
import com.flowboard.data.database.Users
import com.flowboard.data.models.*
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.sql.*
import java.util.*

class ChatService {

    suspend fun createChatRoom(request: CreateChatRoomRequest, creatorId: String): ChatRoom {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        val chatRoomId = UUID.randomUUID()

        dbQuery {
            // Insert chat room
            ChatRooms.insert {
                it[ChatRooms.id] = chatRoomId
                it[ChatRooms.type] = request.type
                it[ChatRooms.name] = request.name
                it[ChatRooms.description] = request.description
                it[ChatRooms.resourceId] = request.resourceId?.let { UUID.fromString(it) }
                it[ChatRooms.resourceType] = request.resourceType
                it[ChatRooms.createdBy] = UUID.fromString(creatorId)
                it[ChatRooms.createdAt] = now
                it[ChatRooms.updatedAt] = now
                it[ChatRooms.isArchived] = false
            }

            // Add creator as owner
            ChatParticipants.insert {
                it[ChatParticipants.id] = UUID.randomUUID()
                it[ChatParticipants.chatRoomId] = chatRoomId
                it[ChatParticipants.userId] = UUID.fromString(creatorId)
                it[ChatParticipants.role] = "OWNER"
                it[ChatParticipants.joinedAt] = now
                it[ChatParticipants.isMuted] = false
            }

            // Add other participants
            request.participantIds.forEach { participantId ->
                if (participantId != creatorId) {
                    ChatParticipants.insert {
                        it[ChatParticipants.id] = UUID.randomUUID()
                        it[ChatParticipants.chatRoomId] = chatRoomId
                        it[ChatParticipants.userId] = UUID.fromString(participantId)
                        it[ChatParticipants.role] = "MEMBER"
                        it[ChatParticipants.joinedAt] = now
                        it[ChatParticipants.isMuted] = false
                    }
                }
            }
        }

        return getChatRoomById(chatRoomId.toString(), creatorId)
            ?: throw Exception("Failed to create chat room")
    }

    suspend fun getChatRoomById(chatRoomId: String, userId: String): ChatRoom? {
        return dbQuery {
            // Check if user is participant
            val isParticipant = ChatParticipants
                .select {
                    (ChatParticipants.chatRoomId eq UUID.fromString(chatRoomId)) and
                    (ChatParticipants.userId eq UUID.fromString(userId))
                }
                .count() > 0

            if (!isParticipant) return@dbQuery null

            val room = ChatRooms
                .select { ChatRooms.id eq UUID.fromString(chatRoomId) }
                .singleOrNull() ?: return@dbQuery null

            // Get participants
            val participants = (ChatParticipants innerJoin Users)
                .slice(
                    ChatParticipants.userId,
                    Users.username,
                    Users.email,
                    ChatParticipants.role,
                    ChatParticipants.joinedAt,
                    ChatParticipants.isMuted
                )
                .select { ChatParticipants.chatRoomId eq UUID.fromString(chatRoomId) }
                .map { row ->
                    ChatParticipant(
                        userId = row[ChatParticipants.userId].toString(),
                        userName = row[Users.username],
                        email = row[Users.email],
                        role = row[ChatParticipants.role],
                        isOnline = false, // TODO: Implement online status
                        joinedAt = row[ChatParticipants.joinedAt],
                        isMuted = row[ChatParticipants.isMuted]
                    )
                }

            // Get last message
            val lastMessage = (Messages innerJoin Users)
                .slice(
                    Messages.id,
                    Messages.chatRoomId,
                    Messages.senderId,
                    Users.username,
                    Messages.type,
                    Messages.content,
                    Messages.replyToId,
                    Messages.isEdited,
                    Messages.editedAt,
                    Messages.createdAt,
                    Messages.metadata
                )
                .select { Messages.chatRoomId eq UUID.fromString(chatRoomId) }
                .orderBy(Messages.createdAt to SortOrder.DESC)
                .limit(1)
                .singleOrNull()
                ?.let { row ->
                    Message(
                        id = row[Messages.id].toString(),
                        chatRoomId = row[Messages.chatRoomId].toString(),
                        senderId = row[Messages.senderId].toString(),
                        senderName = row[Users.username],
                        type = row[Messages.type],
                        content = row[Messages.content],
                        replyToId = row[Messages.replyToId]?.toString(),
                        isEdited = row[Messages.isEdited],
                        editedAt = row[Messages.editedAt],
                        createdAt = row[Messages.createdAt],
                        metadata = row[Messages.metadata]
                    )
                }

            ChatRoom(
                id = room[ChatRooms.id].toString(),
                type = room[ChatRooms.type],
                name = room[ChatRooms.name],
                description = room[ChatRooms.description],
                resourceId = room[ChatRooms.resourceId]?.toString(),
                resourceType = room[ChatRooms.resourceType],
                participants = participants,
                lastMessage = lastMessage,
                unreadCount = 0, // TODO: Implement unread count
                createdBy = room[ChatRooms.createdBy].toString(),
                createdAt = room[ChatRooms.createdAt],
                updatedAt = room[ChatRooms.updatedAt],
                isArchived = room[ChatRooms.isArchived]
            )
        }
    }

    suspend fun getUserChatRooms(userId: String): List<ChatRoom> {
        return dbQuery {
            val participatingChatRoomIds = ChatParticipants
                .select { ChatParticipants.userId eq UUID.fromString(userId) }
                .map { it[ChatParticipants.chatRoomId] }

            participatingChatRoomIds.mapNotNull { chatRoomId ->
                getChatRoomById(chatRoomId.toString(), userId)
            }
        }
    }

    suspend fun sendMessage(chatRoomId: String, senderId: String, request: SendMessageRequest): Message {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        val messageId = UUID.randomUUID()

        dbQuery {
            // Insert message
            Messages.insert {
                it[Messages.id] = messageId
                it[Messages.chatRoomId] = UUID.fromString(chatRoomId)
                it[Messages.senderId] = UUID.fromString(senderId)
                it[Messages.type] = request.type
                it[Messages.content] = request.content
                it[Messages.replyToId] = request.replyToId?.let { UUID.fromString(it) }
                it[Messages.isEdited] = false
                it[Messages.editedAt] = null
                it[Messages.createdAt] = now
                it[Messages.metadata] = request.metadata
            }

            // Update chat room updatedAt
            ChatRooms.update({ ChatRooms.id eq UUID.fromString(chatRoomId) }) {
                it[ChatRooms.updatedAt] = now
            }
        }

        return getMessage(messageId.toString(), senderId)
            ?: throw Exception("Failed to send message")
    }

    suspend fun getMessage(messageId: String, userId: String): Message? {
        return dbQuery {
            (Messages innerJoin Users)
                .slice(
                    Messages.id,
                    Messages.chatRoomId,
                    Messages.senderId,
                    Users.username,
                    Messages.type,
                    Messages.content,
                    Messages.replyToId,
                    Messages.isEdited,
                    Messages.editedAt,
                    Messages.createdAt,
                    Messages.metadata
                )
                .select { Messages.id eq UUID.fromString(messageId) }
                .singleOrNull()
                ?.let { row ->
                    Message(
                        id = row[Messages.id].toString(),
                        chatRoomId = row[Messages.chatRoomId].toString(),
                        senderId = row[Messages.senderId].toString(),
                        senderName = row[Users.username],
                        type = row[Messages.type],
                        content = row[Messages.content],
                        replyToId = row[Messages.replyToId]?.toString(),
                        isEdited = row[Messages.isEdited],
                        editedAt = row[Messages.editedAt],
                        createdAt = row[Messages.createdAt],
                        metadata = row[Messages.metadata]
                    )
                }
        }
    }

    suspend fun getChatMessages(chatRoomId: String, userId: String, limit: Int = 50, offset: Int = 0): List<Message> {
        return dbQuery {
            // Check if user is participant
            val isParticipant = ChatParticipants
                .select {
                    (ChatParticipants.chatRoomId eq UUID.fromString(chatRoomId)) and
                    (ChatParticipants.userId eq UUID.fromString(userId))
                }
                .count() > 0

            if (!isParticipant) return@dbQuery emptyList()

            (Messages innerJoin Users)
                .slice(
                    Messages.id,
                    Messages.chatRoomId,
                    Messages.senderId,
                    Users.username,
                    Messages.type,
                    Messages.content,
                    Messages.replyToId,
                    Messages.isEdited,
                    Messages.editedAt,
                    Messages.createdAt,
                    Messages.metadata
                )
                .select { Messages.chatRoomId eq UUID.fromString(chatRoomId) }
                .orderBy(Messages.createdAt to SortOrder.DESC)
                .limit(limit, offset.toLong())
                .map { row ->
                    Message(
                        id = row[Messages.id].toString(),
                        chatRoomId = row[Messages.chatRoomId].toString(),
                        senderId = row[Messages.senderId].toString(),
                        senderName = row[Users.username],
                        type = row[Messages.type],
                        content = row[Messages.content],
                        replyToId = row[Messages.replyToId]?.toString(),
                        isEdited = row[Messages.isEdited],
                        editedAt = row[Messages.editedAt],
                        createdAt = row[Messages.createdAt],
                        metadata = row[Messages.metadata]
                    )
                }
                .reversed() // Return in chronological order
        }
    }

    suspend fun updateMessage(messageId: String, senderId: String, content: String): Message? {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)

        val updated = dbQuery {
            Messages.update({
                (Messages.id eq UUID.fromString(messageId)) and
                (Messages.senderId eq UUID.fromString(senderId))
            }) {
                it[Messages.content] = content
                it[Messages.isEdited] = true
                it[Messages.editedAt] = now
            } > 0
        }

        return if (updated) getMessage(messageId, senderId) else null
    }

    suspend fun deleteMessage(messageId: String, senderId: String): Boolean {
        return dbQuery {
            Messages.deleteWhere {
                (Messages.id eq UUID.fromString(messageId)) and
                (Messages.senderId eq UUID.fromString(senderId))
            } > 0
        }
    }

    suspend fun addParticipant(chatRoomId: String, userId: String, targetUserId: String): Boolean {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)

        return dbQuery {
            // Check if requester is admin or owner
            val requesterRole = ChatParticipants
                .select {
                    (ChatParticipants.chatRoomId eq UUID.fromString(chatRoomId)) and
                    (ChatParticipants.userId eq UUID.fromString(userId))
                }
                .singleOrNull()
                ?.get(ChatParticipants.role)

            if (requesterRole != "OWNER" && requesterRole != "ADMIN") {
                return@dbQuery false
            }

            // Add participant
            ChatParticipants.insert {
                it[ChatParticipants.id] = UUID.randomUUID()
                it[ChatParticipants.chatRoomId] = UUID.fromString(chatRoomId)
                it[ChatParticipants.userId] = UUID.fromString(targetUserId)
                it[ChatParticipants.role] = "MEMBER"
                it[ChatParticipants.joinedAt] = now
                it[ChatParticipants.isMuted] = false
            }

            true
        }
    }

    suspend fun removeParticipant(chatRoomId: String, userId: String, targetUserId: String): Boolean {
        return dbQuery {
            // Check if requester is owner
            val requesterRole = ChatParticipants
                .select {
                    (ChatParticipants.chatRoomId eq UUID.fromString(chatRoomId)) and
                    (ChatParticipants.userId eq UUID.fromString(userId))
                }
                .singleOrNull()
                ?.get(ChatParticipants.role)

            if (requesterRole != "OWNER") {
                return@dbQuery false
            }

            // Don't allow removing the owner
            val targetRole = ChatParticipants
                .select {
                    (ChatParticipants.chatRoomId eq UUID.fromString(chatRoomId)) and
                    (ChatParticipants.userId eq UUID.fromString(targetUserId))
                }
                .singleOrNull()
                ?.get(ChatParticipants.role)

            if (targetRole == "OWNER") {
                return@dbQuery false
            }

            ChatParticipants.deleteWhere {
                (ChatParticipants.chatRoomId eq UUID.fromString(chatRoomId)) and
                (ChatParticipants.userId eq UUID.fromString(targetUserId))
            } > 0
        }
    }
}
