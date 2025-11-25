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
