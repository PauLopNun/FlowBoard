package com.flowboard.domain.model

import kotlinx.datetime.LocalDateTime
import com.flowboard.data.local.entities.UserRole
import com.flowboard.data.local.entities.UserPreferences

data class User(
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