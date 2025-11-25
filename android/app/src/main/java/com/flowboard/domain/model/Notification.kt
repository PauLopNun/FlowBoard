package com.flowboard.domain.model

/**
 * Types of notifications in the system
 */
enum class NotificationType {
    TASK_ASSIGNED,           // Someone assigned you a task
    TASK_COMPLETED,          // Task you created was completed
    TASK_DUE_SOON,          // Task deadline approaching
    TASK_OVERDUE,           // Task is overdue
    COMMENT_MENTION,        // Someone mentioned you in a comment
    COMMENT_REPLY,          // Someone replied to your comment
    PERMISSION_GRANTED,     // You were granted access to a resource
    PERMISSION_REVOKED,     // Your access was revoked
    DOCUMENT_SHARED,        // Document was shared with you
    DOCUMENT_UPDATED,       // Document you're watching was updated
    PROJECT_INVITATION,     // Invited to a project
    USER_JOINED,           // New user joined a project
    DEADLINE_REMINDER,     // Reminder for upcoming deadline
    SYSTEM_ANNOUNCEMENT    // System-wide announcement
}

/**
 * Priority level for notifications
 */
enum class NotificationPriority {
    LOW,
    MEDIUM,
    HIGH,
    URGENT
}

/**
 * Notification domain model
 */
data class Notification(
    val id: String,
    val userId: String,
    val type: NotificationType,
    val priority: NotificationPriority,
    val title: String,
    val message: String,
    val resourceId: String? = null,      // ID of related resource (task, doc, etc.)
    val resourceType: ResourceType? = null,
    val actionUserId: String? = null,    // User who triggered the notification
    val actionUserName: String? = null,
    val imageUrl: String? = null,
    val deepLink: String? = null,        // Deep link to open specific screen
    val metadata: Map<String, String> = emptyMap(),
    val isRead: Boolean = false,
    val createdAt: Long,
    val expiresAt: Long? = null
)

/**
 * Notification settings for a user
 */
data class NotificationSettings(
    val userId: String,
    val enablePushNotifications: Boolean = true,
    val enableEmailNotifications: Boolean = true,
    val enableInAppNotifications: Boolean = true,
    val enableSoundAlerts: Boolean = true,
    val enableVibration: Boolean = true,
    val quietHoursEnabled: Boolean = false,
    val quietHoursStart: Int = 22,  // 22:00 (10 PM)
    val quietHoursEnd: Int = 8,     // 08:00 (8 AM)
    val notificationTypes: Map<NotificationType, Boolean> = NotificationType.values()
        .associateWith { true }
)

/**
 * Notification group for display
 */
data class NotificationGroup(
    val type: NotificationType,
    val count: Int,
    val latestNotification: Notification,
    val notifications: List<Notification>
)

/**
 * Notification statistics
 */
data class NotificationStats(
    val totalCount: Int,
    val unreadCount: Int,
    val todayCount: Int,
    val byType: Map<NotificationType, Int>,
    val byPriority: Map<NotificationPriority, Int>
)
