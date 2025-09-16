package com.flowboard.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

@Entity(tableName = "users")
@Serializable
data class UserEntity(
    @PrimaryKey
    val id: String,
    val email: String,
    val username: String,
    val fullName: String,
    val role: UserRole = UserRole.USER,
    val profileImageUrl: String? = null,
    val isActive: Boolean = true,
    val createdAt: LocalDateTime,
    val lastLoginAt: LocalDateTime? = null,
    val preferences: UserPreferences = UserPreferences()
)

enum class UserRole {
    USER, ADMIN, PROJECT_MANAGER
}

@Serializable
data class UserPreferences(
    val theme: String = "AUTO",
    val notificationsEnabled: Boolean = true,
    val emailNotifications: Boolean = true,
    val defaultView: String = "LIST",
    val language: String = "es"
)