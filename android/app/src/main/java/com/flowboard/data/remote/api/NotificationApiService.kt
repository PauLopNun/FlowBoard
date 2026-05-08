package com.flowboard.data.remote.api

import com.flowboard.data.remote.ApiConfig
import com.flowboard.data.repository.AuthRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.http.HttpHeaders
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class RemoteNotificationDto(
    val id: String,
    val userId: String,
    val type: String,
    val title: String,
    val message: String,
    val resourceId: String? = null,
    val resourceType: String? = null,
    val actionUserId: String? = null,
    val actionUserName: String? = null,
    val deepLink: String? = null,
    val isRead: Boolean = false,
    val createdAt: LocalDateTime,
    val expiresAt: LocalDateTime? = null
)

@Singleton
class NotificationApiService @Inject constructor(
    private val httpClient: HttpClient,
    private val authRepository: AuthRepository
) {
    private val endpoint = "${ApiConfig.API_BASE_URL}/notifications"

    private suspend fun token() = authRepository.getToken() ?: throw Exception("Not authenticated")

    suspend fun getNotifications(): List<RemoteNotificationDto> {
        return httpClient.get(endpoint) {
            header(HttpHeaders.Authorization, "Bearer ${token()}")
        }.body()
    }

    suspend fun markAsRead(notificationId: String) {
        httpClient.patch("$endpoint/$notificationId/read") {
            header(HttpHeaders.Authorization, "Bearer ${token()}")
        }
    }

    suspend fun markAllAsRead() {
        httpClient.patch("$endpoint/read-all") {
            header(HttpHeaders.Authorization, "Bearer ${token()}")
        }
    }

    suspend fun acceptInvitation(notificationId: String) {
        httpClient.post("$endpoint/$notificationId/accept") {
            header(HttpHeaders.Authorization, "Bearer ${token()}")
        }
    }

    suspend fun declineInvitation(notificationId: String) {
        httpClient.post("$endpoint/$notificationId/decline") {
            header(HttpHeaders.Authorization, "Bearer ${token()}")
        }
    }

    suspend fun deleteNotification(notificationId: String) {
        httpClient.delete("$endpoint/$notificationId") {
            header(HttpHeaders.Authorization, "Bearer ${token()}")
        }
    }

    suspend fun deleteAllNotifications() {
        httpClient.delete(endpoint) {
            header(HttpHeaders.Authorization, "Bearer ${token()}")
        }
    }
}
