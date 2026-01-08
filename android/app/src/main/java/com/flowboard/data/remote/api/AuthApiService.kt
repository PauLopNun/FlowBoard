package com.flowboard.data.remote.api

import android.util.Log
import com.flowboard.data.remote.ApiConfig
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
        private const val TAG = "AuthApiService"
        private val AUTH_ENDPOINT = "${ApiConfig.API_BASE_URL}/auth"
    }

    /**
     * Login with email and password
     * POST /api/v1/auth/login
     */
    suspend fun login(request: LoginRequest): Result<AuthResponse> {
        return try {
            Log.d(TAG, "Attempting login for email: ${request.email}")
            Log.d(TAG, "Login URL: $AUTH_ENDPOINT/login")
            Log.d(TAG, "Full URL: ${ApiConfig.BASE_URL}")

            val httpResponse = httpClient.post("$AUTH_ENDPOINT/login") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            Log.d(TAG, "Response status: ${httpResponse.status}")
            Log.d(TAG, "Response content type: ${httpResponse.contentType()}")

            if (httpResponse.status.value == 200) {
                val response: AuthResponse = httpResponse.body()
                Log.d(TAG, "Login successful: ${response.success}")
                Result.success(response)
            } else {
                try {
                    val errorBody = httpResponse.body<String>()
                    Log.e(TAG, "Login failed with status ${httpResponse.status.value}")
                    Log.e(TAG, "Error response body: $errorBody")

                    // Extraer mensaje de error más amigable
                    val errorMessage = when (httpResponse.status.value) {
                        400 -> "Email o contraseña inválidos"
                        401 -> "Credenciales incorrectas"
                        404 -> "Usuario no encontrado"
                        500 -> "Error del servidor. Intenta de nuevo más tarde"
                        else -> "Error de conexión: ${httpResponse.status.value}"
                    }
                    Result.failure(Exception(errorMessage))
                } catch (e: Exception) {
                    Result.failure(Exception("Error al procesar respuesta del servidor"))
                }
            }
        } catch (e: java.net.UnknownHostException) {
            Log.e(TAG, "Network error - Unknown host: ${e.message}", e)
            Result.failure(Exception("No se puede conectar al servidor. Verifica tu conexión a internet"))
        } catch (e: java.net.SocketTimeoutException) {
            Log.e(TAG, "Network error - Timeout: ${e.message}", e)
            Result.failure(Exception("El servidor no responde. Intenta de nuevo más tarde"))
        } catch (e: java.net.ConnectException) {
            Log.e(TAG, "Network error - Connection refused: ${e.message}", e)
            Result.failure(Exception("No se puede conectar al servidor. Verifica tu conexión"))
        } catch (e: Exception) {
            Log.e(TAG, "Login failed with exception: ${e.message}", e)
            Log.e(TAG, "Exception type: ${e.javaClass.simpleName}")
            Result.failure(Exception("Error de red: ${e.message ?: "Conexión fallida"}"))
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

    suspend fun searchUserByEmail(email: String): Result<UserData> {
        return try {
            val response: UserData = httpClient.get("${ApiConfig.API_BASE_URL}/users/search") {
                parameter("email", email)
            }.body()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get current user profile
     * GET /api/v1/users/me
     */
    suspend fun getCurrentUser(token: String): Result<UserData> {
        return try {
            val response: UserData = httpClient.get("${ApiConfig.API_BASE_URL}/users/me") {
                header("Authorization", "Bearer $token")
            }.body()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update user profile
     * PUT /api/v1/users/me
     */
    suspend fun updateProfile(token: String, request: UpdateProfileRequest): Result<UserData> {
        return try {
            val response: UserData = httpClient.put("${ApiConfig.API_BASE_URL}/users/me") {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update password
     * PUT /api/v1/users/me/password
     */
    suspend fun updatePassword(token: String, request: UpdatePasswordRequest): Result<UpdatePasswordResponse> {
        return try {
            val response: UpdatePasswordResponse = httpClient.put("${ApiConfig.API_BASE_URL}/users/me/password") {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Sign in with Google
     * POST /api/v1/auth/google
     */
    suspend fun googleSignIn(request: GoogleSignInRequest): Result<AuthResponse> {
        return try {
            Log.d(TAG, "Attempting Google Sign-In for email: ${request.email}")

            val httpResponse = httpClient.post("$AUTH_ENDPOINT/google") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            if (httpResponse.status.value == 200) {
                val response: AuthResponse = httpResponse.body()
                Log.d(TAG, "Google Sign-In successful")
                Result.success(response)
            } else {
                val errorBody = httpResponse.body<String>()
                Log.e(TAG, "Google Sign-In failed with status ${httpResponse.status.value}")
                Result.failure(Exception("Google Sign-In failed: $errorBody"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Google Sign-In failed with exception: ${e.message}", e)
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
@kotlinx.serialization.Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

/**
 * Register request body
 */
@kotlinx.serialization.Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
    val username: String,
    val fullName: String? = null
)

/**
 * Update profile request body
 */
@kotlinx.serialization.Serializable
data class UpdateProfileRequest(
    val fullName: String? = null,
    val profileImageUrl: String? = null
)

/**
 * Update password request body
 */
@kotlinx.serialization.Serializable
data class UpdatePasswordRequest(
    val oldPassword: String,
    val newPassword: String
)

/**
 * Google Sign-In request body
 */
@kotlinx.serialization.Serializable
data class GoogleSignInRequest(
    val idToken: String,
    val email: String,
    val displayName: String? = null,
    val profilePictureUrl: String? = null
)

// ============================================================================
// RESPONSE MODELS
// ============================================================================

/**
 * Auth response from backend (login/register)
 */
@kotlinx.serialization.Serializable
data class AuthResponse(
    val token: String,
    val user: UserData
) {
    // Computed properties for compatibility
    val success: Boolean get() = token.isNotEmpty()
    val userId: String get() = user.id
    val username: String get() = user.username
    val email: String get() = user.email
    val fullName: String? get() = user.fullName
    val defaultBoardId: String? get() = "default-board" // Default board for all users
}

/**
 * User data from backend
 */
@kotlinx.serialization.Serializable
data class UserData(
    val id: String,
    val email: String,
    val username: String,
    val fullName: String,
    val role: String = "USER",
    val profileImageUrl: String? = null,
    val isActive: Boolean = true,
    val createdAt: String? = null,
    val lastLoginAt: String? = null
)

/**
 * Token verification response
 */
@kotlinx.serialization.Serializable
data class VerifyTokenResponse(
    val valid: Boolean,
    val userId: String? = null,
    val expiresAt: Long? = null,
    val message: String? = null
)

/**
 * Logout response
 */
@kotlinx.serialization.Serializable
data class LogoutResponse(
    val success: Boolean,
    val message: String? = null
)

/**
 * Update password response
 */
@kotlinx.serialization.Serializable
data class UpdatePasswordResponse(
    val message: String
)
