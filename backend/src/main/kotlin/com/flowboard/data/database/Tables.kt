package com.flowboard.data.database

import kotlinx.serialization.json.Json
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.kotlin.datetime.datetime
import org.jetbrains.exposed.sql.json.json

object Users : UUIDTable("users") {
    val email = varchar("email", 255).uniqueIndex()
    val username = varchar("username", 100).uniqueIndex()
    val fullName = varchar("full_name", 255)
    val passwordHash = varchar("password_hash", 255)
    val role = enumeration("role", com.flowboard.data.models.UserRole::class).default(com.flowboard.data.models.UserRole.USER)
    val profileImageUrl = varchar("profile_image_url", 500).nullable()
    val isActive = bool("is_active").default(true)
    val createdAt = datetime("created_at")
    val lastLoginAt = datetime("last_login_at").nullable()
}

object Tasks : UUIDTable("tasks") {
    val title = varchar("title", 255)
    val description = text("description")
    val isCompleted = bool("is_completed").default(false)
    val priority = enumeration("priority", com.flowboard.data.models.TaskPriority::class).default(com.flowboard.data.models.TaskPriority.MEDIUM)
    val dueDate = datetime("due_date").nullable()
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at")
    val assignedTo = uuid("assigned_to").nullable()
    val projectId = uuid("project_id").nullable()
    val tags = json<List<String>>("tags", Json.Default).default(emptyList())
    val attachments = json<List<String>>("attachments", Json.Default).default(emptyList())
    val isEvent = bool("is_event").default(false)
    val eventStartTime = datetime("event_start_time").nullable()
    val eventEndTime = datetime("event_end_time").nullable()
    val location = varchar("location", 500).nullable()
    val createdBy = uuid("created_by")
}

object Projects : UUIDTable("projects") {

    val name = varchar("name", 255)

    val description = text("description")

    val color = varchar("color", 7).default("#2196F3")

    val ownerId = uuid("owner_id")

    val members = json<List<String>>("members", Json.Default).default(emptyList())

    val isActive = bool("is_active").default(true)

    val createdAt = datetime("created_at")

    val updatedAt = datetime("updated_at")

    val deadline = datetime("deadline").nullable()

}



object BoardPermissions : UUIDTable("board_permissions") {
    val boardId = uuid("board_id")
    val userId = uuid("user_id")
}

// Documents table for collaborative documents
object Documents : UUIDTable("documents") {
    val title = varchar("title", 500)
    val content = text("content")
    val ownerId = uuid("owner_id")
    val isPublic = bool("is_public").default(false)
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at")
    val lastEditedBy = uuid("last_edited_by").nullable()
}

// Document permissions with roles (viewer/editor)
object DocumentPermissions : UUIDTable("document_permissions") {
    val documentId = uuid("document_id")
    val userId = uuid("user_id")
    val role = varchar("role", 50).default("viewer") // viewer, editor, owner
    val grantedBy = uuid("granted_by")
    val grantedAt = datetime("granted_at")
}

// Notifications table
object Notifications : UUIDTable("notifications") {
    val userId = uuid("user_id")
    val type = varchar("type", 100)
    val title = varchar("title", 255)
    val message = text("message")
    val resourceId = uuid("resource_id").nullable()
    val resourceType = varchar("resource_type", 50).nullable()
    val actionUserId = uuid("action_user_id").nullable()
    val actionUserName = varchar("action_user_name", 255).nullable()
    val deepLink = varchar("deep_link", 500).nullable()
    val isRead = bool("is_read").default(false)
    val createdAt = datetime("created_at")
    val expiresAt = datetime("expires_at").nullable()
}

// Chat rooms table
object ChatRooms : UUIDTable("chat_rooms") {
    val type = varchar("type", 50) // DIRECT, GROUP, PROJECT, TASK_THREAD
    val name = varchar("name", 255).nullable()
    val description = text("description").nullable()
    val resourceId = uuid("resource_id").nullable()
    val resourceType = varchar("resource_type", 50).nullable()
    val createdBy = uuid("created_by")
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at")
    val isArchived = bool("is_archived").default(false)
}

// Chat participants table
object ChatParticipants : UUIDTable("chat_participants") {
    val chatRoomId = uuid("chat_room_id")
    val userId = uuid("user_id")
    val role = varchar("role", 50).default("MEMBER") // OWNER, ADMIN, MEMBER
    val joinedAt = datetime("joined_at")
    val isMuted = bool("is_muted").default(false)
}

// Messages table
object Messages : UUIDTable("messages") {
    val chatRoomId = uuid("chat_room_id")
    val senderId = uuid("sender_id")
    val type = varchar("type", 50).default("TEXT")
    val content = text("content")
    val replyToId = uuid("reply_to_id").nullable()
    val isEdited = bool("is_edited").default(false)
    val editedAt = datetime("edited_at").nullable()
    val createdAt = datetime("created_at")
    val metadata = json<Map<String, String>>("metadata", Json.Default).default(emptyMap())
}
