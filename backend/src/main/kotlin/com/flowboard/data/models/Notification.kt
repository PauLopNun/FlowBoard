package com.flowboard.data.models

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class Notification(
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

@Serializable
data class CreateNotificationRequest(
    val userId: String,
    val type: String,
    val title: String,
    val message: String,
    val resourceId: String? = null,
    val resourceType: String? = null,
    val actionUserId: String? = null,
    val actionUserName: String? = null,
    val deepLink: String? = null
)

@Serializable
data class NotificationStats(
    val total: Int,
    val unread: Int
)
