package com.flowboard.domain.repository

import com.flowboard.domain.model.*
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing notifications
 */
interface NotificationRepository {

    /**
     * Get all notifications for a user
     */
    fun getAllNotifications(userId: String): Flow<List<Notification>>

    /**
     * Get unread notifications
     */
    fun getUnreadNotifications(userId: String): Flow<List<Notification>>

    /**
     * Get notifications by type
     */
    fun getNotificationsByType(userId: String, type: NotificationType): Flow<List<Notification>>

    /**
     * Get recent notifications (last 24 hours)
     */
    fun getRecentNotifications(userId: String): Flow<List<Notification>>

    /**
     * Get unread count
     */
    fun getUnreadCount(userId: String): Flow<Int>

    /**
     * Get notification statistics
     */
    suspend fun getNotificationStats(userId: String): NotificationStats

    /**
     * Mark notification as read
     */
    suspend fun markAsRead(notificationId: String)

    /**
     * Mark all notifications as read
     */
    suspend fun markAllAsRead(userId: String)

    /**
     * Mark all notifications of a type as read
     */
    suspend fun markTypeAsRead(userId: String, type: NotificationType)

    /**
     * Delete notification
     */
    suspend fun deleteNotification(notificationId: String)

    /**
     * Delete all notifications
     */
    suspend fun deleteAllNotifications(userId: String)

    /**
     * Delete old notifications (older than 30 days)
     */
    suspend fun deleteOldNotifications(userId: String)

    /**
     * Create a new notification
     */
    suspend fun createNotification(notification: Notification)

    /**
     * Get notification settings
     */
    suspend fun getNotificationSettings(userId: String): NotificationSettings

    /**
     * Update notification settings
     */
    suspend fun updateNotificationSettings(settings: NotificationSettings)
}
