package com.flowboard.data.remote.api

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * API service for authentication endpoints
 */
@Singleton
class AuthApiService @Inject constructor(
    private val httpClient: HttpClient
) {
    companion object {
        private const val BASE_URL = "http://10.0.2.2:8080/api/v1"
        private const val AUTH_ENDPOINT = "$BASE_URL/auth"
    }

    /**
     * Login with email and password
     * POST /api/v1/auth/login
     */
    suspend fun login(request: LoginRequest): Result<AuthResponse> {
        return try {
            val response: AuthResponse = httpClient.post("$AUTH_ENDPOINT/login") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Register new user
     * POST /api/v1/auth/register
     */
    suspend fun register(request: RegisterRequest): Result<AuthResponse> {
        return try {
            val response: AuthResponse = httpClient.post("$AUTH_ENDPOINT/register") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Verify JWT token is still valid
     * GET /api/v1/auth/verify
     */
    suspend fun verifyToken(token: String): Result<VerifyTokenResponse> {
        return try {
            val response: VerifyTokenResponse = httpClient.get("$AUTH_ENDPOINT/verify") {
                header("Authorization", "Bearer $token")
            }.body()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Refresh JWT token
     * POST /api/v1/auth/refresh
     */
    suspend fun refreshToken(token: String): Result<AuthResponse> {
        return try {
            val response: AuthResponse = httpClient.post("$AUTH_ENDPOINT/refresh") {
                header("Authorization", "Bearer $token")
            }.body()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Logout (invalidate token)
     * POST /api/v1/auth/logout
     */
    suspend fun logout(token: String): Result<LogoutResponse> {
        return try {
            val response: LogoutResponse = httpClient.post("$AUTH_ENDPOINT/logout") {
                header("Authorization", "Bearer $token")
            }.body()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

// ============================================================================
// REQUEST MODELS
// ============================================================================

/**
 * Login request body
 */
data class LoginRequest(
    val email: String,
    val password: String
)

/**
 * Register request body
 */
data class RegisterRequest(
    val email: String,
    val password: String,
    val username: String,
    val fullName: String? = null
)

// ============================================================================
// RESPONSE MODELS
// ============================================================================

/**
 * Auth response (login/register/refresh)
 */
data class AuthResponse(
    val success: Boolean,
    val token: String,
    val userId: String,
    val username: String,
    val email: String,
    val fullName: String? = null,
    val defaultBoardId: String? = null,
    val expiresAt: Long? = null,
    val message: String? = null
)

/**
 * Token verification response
 */
data class VerifyTokenResponse(
    val valid: Boolean,
    val userId: String? = null,
    val expiresAt: Long? = null,
    val message: String? = null
)

/**
 * Logout response
 */
data class LogoutResponse(
    val success: Boolean,
    val message: String? = null
)
