package com.flowboard.data.repository

import com.flowboard.data.local.dao.NotificationDao
import com.flowboard.data.local.entities.NotificationEntity
import com.flowboard.data.remote.api.NotificationApiService
import com.flowboard.data.remote.api.RemoteNotificationDto
import com.flowboard.domain.model.*
import com.flowboard.domain.repository.NotificationRepository
import com.flowboard.utils.toDomain
import com.flowboard.utils.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepositoryImpl @Inject constructor(
    private val notificationDao: NotificationDao,
    private val notificationApiService: NotificationApiService,
    private val authRepository: AuthRepository
) : NotificationRepository {

    override fun getAllNotifications(userId: String): Flow<List<Notification>> {
        return notificationDao.getAllNotifications(userId)
            .map { entities -> entities.map { it.toDomain() } }
    }

    override fun getUnreadNotifications(userId: String): Flow<List<Notification>> {
        return notificationDao.getUnreadNotifications(userId)
            .map { entities -> entities.map { it.toDomain() } }
    }

    override fun getNotificationsByType(
        userId: String,
        type: NotificationType
    ): Flow<List<Notification>> {
        return notificationDao.getNotificationsByType(userId, type.name.lowercase())
            .map { entities -> entities.map { it.toDomain() } }
    }

    override fun getRecentNotifications(userId: String): Flow<List<Notification>> {
        val since = System.currentTimeMillis() - (24 * 60 * 60 * 1000) // Last 24 hours
        return notificationDao.getRecentNotifications(userId, since)
            .map { entities -> entities.map { it.toDomain() } }
    }

    override fun getUnreadCount(userId: String): Flow<Int> {
        return notificationDao.getUnreadCount(userId)
    }

    override suspend fun refreshNotifications(): Result<Unit> {
        return try {
            val remote = notificationApiService.getNotifications()
            notificationDao.insertNotifications(remote.map { it.toEntity() })
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun acceptInvitation(notificationId: String): Result<Unit> {
        return try {
            notificationApiService.acceptInvitation(notificationId)
            notificationDao.markAsRead(notificationId)
            refreshNotifications()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun declineInvitation(notificationId: String): Result<Unit> {
        return try {
            notificationApiService.declineInvitation(notificationId)
            notificationDao.markAsRead(notificationId)
            refreshNotifications()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getNotificationStats(userId: String): NotificationStats {
        val all = notificationDao.getAllNotifications(userId).first()
        val unread = notificationDao.getUnreadCount(userId).first()
        val todayStart = System.currentTimeMillis() - 24 * 60 * 60 * 1000L
        val todayCount = all.count { it.createdAt >= todayStart }
        val byType = all.groupBy { it.type }.mapNotNull { (key, value) ->
            val type = try { NotificationType.valueOf(key.uppercase()) } catch (_: Exception) { null }
            type?.let { it to value.size }
        }.toMap()
        val byPriority = all.groupBy { it.priority }.mapNotNull { (key, value) ->
            val priority = try { NotificationPriority.valueOf(key.uppercase()) } catch (_: Exception) { null }
            priority?.let { it to value.size }
        }.toMap()
        return NotificationStats(
            totalCount = all.size,
            unreadCount = unread,
            todayCount = todayCount,
            byType = byType,
            byPriority = byPriority
        )
    }

    override suspend fun markAsRead(notificationId: String) {
        runCatching { notificationApiService.markAsRead(notificationId) }
        notificationDao.markAsRead(notificationId)
    }

    override suspend fun markAllAsRead(userId: String) {
        runCatching { notificationApiService.markAllAsRead() }
        notificationDao.markAllAsRead(userId)
    }

    override suspend fun markTypeAsRead(userId: String, type: NotificationType) {
        notificationDao.markTypeAsRead(userId, type.name.lowercase())
    }

    override suspend fun deleteNotification(notificationId: String) {
        runCatching { notificationApiService.deleteNotification(notificationId) }
        notificationDao.deleteNotification(notificationId)
    }

    override suspend fun deleteAllNotifications(userId: String) {
        runCatching { notificationApiService.deleteAllNotifications() }
        notificationDao.deleteAllNotifications(userId)
    }

    override suspend fun deleteOldNotifications(userId: String) {
        val thirtyDaysAgo = System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000L)
        notificationDao.deleteOldNotifications(userId, thirtyDaysAgo)

        // Also delete expired notifications
        notificationDao.deleteExpiredNotifications(System.currentTimeMillis())
    }

    override suspend fun createNotification(notification: Notification) {
        notificationDao.insertNotification(notification.toEntity())
    }

    override suspend fun getNotificationSettings(userId: String): NotificationSettings {
        // TODO: Implement settings storage (could use DataStore)
        return NotificationSettings(userId = userId)
    }

    override suspend fun updateNotificationSettings(settings: NotificationSettings) {
        // TODO: Implement settings storage (could use DataStore)
    }

    private fun RemoteNotificationDto.toEntity(): NotificationEntity {
        return NotificationEntity(
            id = id,
            userId = userId,
            type = type.lowercase(),
            priority = when (type.uppercase()) {
                "DOCUMENT_SHARED", "WORKSPACE_INVITATION" -> NotificationPriority.HIGH.name.lowercase()
                "CHAT_MESSAGE" -> NotificationPriority.MEDIUM.name.lowercase()
                else -> NotificationPriority.MEDIUM.name.lowercase()
            },
            title = title,
            message = message,
            resourceId = resourceId,
            resourceType = resourceType,
            actionUserId = actionUserId,
            actionUserName = actionUserName,
            deepLink = deepLink,
            isRead = isRead,
            createdAt = createdAt.toInstant(TimeZone.UTC).toEpochMilliseconds(),
            expiresAt = expiresAt?.toInstant(TimeZone.UTC)?.toEpochMilliseconds()
        )
    }
}
