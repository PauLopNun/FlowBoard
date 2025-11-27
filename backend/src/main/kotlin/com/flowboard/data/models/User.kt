package com.flowboard.data.models

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: String,
    val email: String,
    val username: String,
    val fullName: String,
    val role: UserRole = UserRole.USER,
    val profileImageUrl: String? = null,
    val isActive: Boolean = true,
    val createdAt: LocalDateTime,
    val lastLoginAt: LocalDateTime? = null
)

@Serializable
enum class UserRole {
    USER, ADMIN, PROJECT_MANAGER
}

@Serializable
data class RegisterRequest(
    val email: String,
    val username: String,
    val fullName: String,
    val password: String
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class GoogleSignInRequest(
    val idToken: String,
    val email: String,
    val displayName: String? = null,
    val profilePictureUrl: String? = null
)

@Serializable
data class LoginResponse(
    val token: String,
    val user: User
)

@Serializable
data class UpdateUserRequest(
    val fullName: String? = null,
    val profileImageUrl: String? = null
)