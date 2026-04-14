package com.flowboard.data.repository

import com.flowboard.data.local.dao.NotificationDao
import com.flowboard.domain.model.*
import com.flowboard.domain.repository.NotificationRepository
import com.flowboard.utils.toDomain
import com.flowboard.utils.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepositoryImpl @Inject constructor(
    private val notificationDao: NotificationDao
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

    override suspend fun getNotificationStats(userId: String): NotificationStats {
        val all = notificationDao.getAllNotifications(userId).first()
        val unread = notificationDao.getUnreadCount(userId).first()
        val todayStart = System.currentTimeMillis() - 24 * 60 * 60 * 1000L
        val todayCount = all.count { it.createdAt >= todayStart }
        val byType = all.groupBy { it.type }.mapKeys { (key, _) ->
            try { NotificationType.valueOf(key.uppercase()) } catch (_: Exception) { NotificationType.SYSTEM }
        }.mapValues { it.value.size }
        val byPriority = all.groupBy { it.priority }.mapKeys { (key, _) ->
            try { NotificationPriority.valueOf(key.uppercase()) } catch (_: Exception) { NotificationPriority.NORMAL }
        }.mapValues { it.value.size }
        return NotificationStats(
            totalCount = all.size,
            unreadCount = unread,
            todayCount = todayCount,
            byType = byType,
            byPriority = byPriority
        )
    }

    override suspend fun markAsRead(notificationId: String) {
        notificationDao.markAsRead(notificationId)
    }

    override suspend fun markAllAsRead(userId: String) {
        notificationDao.markAllAsRead(userId)
    }

    override suspend fun markTypeAsRead(userId: String, type: NotificationType) {
        notificationDao.markTypeAsRead(userId, type.name.lowercase())
    }

    override suspend fun deleteNotification(notificationId: String) {
        notificationDao.deleteNotification(notificationId)
    }

    override suspend fun deleteAllNotifications(userId: String) {
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
}
