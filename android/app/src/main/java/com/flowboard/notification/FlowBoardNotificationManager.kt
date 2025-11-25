package com.flowboard.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.flowboard.MainActivity
import com.flowboard.R
import com.flowboard.domain.model.Notification
import com.flowboard.domain.model.NotificationPriority
import com.flowboard.domain.model.NotificationType
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FlowBoardNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val CHANNEL_ID_HIGH = "flowboard_high_priority"
        private const val CHANNEL_ID_DEFAULT = "flowboard_default"
        private const val CHANNEL_ID_LOW = "flowboard_low_priority"
        private const val CHANNEL_ID_MESSAGES = "flowboard_messages"
        private const val CHANNEL_NAME_HIGH = "High Priority Notifications"
        private const val CHANNEL_NAME_DEFAULT = "FlowBoard Notifications"
        private const val CHANNEL_NAME_LOW = "Low Priority Notifications"
        private const val CHANNEL_NAME_MESSAGES = "Messages"
    }

    private val notificationManager = NotificationManagerCompat.from(context)

    init {
        createNotificationChannels()
    }

    /**
     * Create notification channels (required for Android O+)
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // High priority channel
            val highChannel = NotificationChannel(
                CHANNEL_ID_HIGH,
                CHANNEL_NAME_HIGH,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Critical notifications that require immediate attention"
                enableVibration(true)
                enableLights(true)
            }

            // Default channel
            val defaultChannel = NotificationChannel(
                CHANNEL_ID_DEFAULT,
                CHANNEL_NAME_DEFAULT,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "General FlowBoard notifications"
                enableVibration(true)
            }

            // Low priority channel
            val lowChannel = NotificationChannel(
                CHANNEL_ID_LOW,
                CHANNEL_NAME_LOW,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Non-urgent notifications"
            }

            // Messages channel
            val messagesChannel = NotificationChannel(
                CHANNEL_ID_MESSAGES,
                CHANNEL_NAME_MESSAGES,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Chat messages and direct communications"
                enableVibration(true)
                enableLights(true)
            }

            notificationManager.createNotificationChannel(highChannel)
            notificationManager.createNotificationChannel(defaultChannel)
            notificationManager.createNotificationChannel(lowChannel)
            notificationManager.createNotificationChannel(messagesChannel)
        }
    }

    /**
     * Show a notification
     */
    fun showNotification(notification: Notification) {
        val channelId = getChannelId(notification.priority)
        val notificationId = notification.id.hashCode()

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            notification.deepLink?.let { putExtra("deep_link", it) }
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(getNotificationIcon(notification.type))
            .setContentTitle(notification.title)
            .setContentText(notification.message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notification.message))
            .setPriority(getNotificationPriority(notification.priority))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setGroup(getGroupKey(notification.type))

        // Add action user info if available
        notification.actionUserName?.let {
            builder.setSubText("From: $it")
        }

        // Set color based on type
        builder.color = getNotificationColor(notification.type)

        try {
            notificationManager.notify(notificationId, builder.build())
        } catch (e: SecurityException) {
            // Permission not granted, handle silently or log
        }
    }

    /**
     * Cancel a notification
     */
    fun cancelNotification(notificationId: String) {
        notificationManager.cancel(notificationId.hashCode())
    }

    /**
     * Cancel all notifications
     */
    fun cancelAllNotifications() {
        notificationManager.cancelAll()
    }

    /**
     * Get channel ID based on priority and type
     */
    private fun getChannelId(priority: NotificationPriority): String {
        return when (priority) {
            NotificationPriority.URGENT, NotificationPriority.HIGH -> CHANNEL_ID_HIGH
            NotificationPriority.MEDIUM -> CHANNEL_ID_DEFAULT
            NotificationPriority.LOW -> CHANNEL_ID_LOW
        }
    }

    /**
     * Get channel ID based on notification type
     */
    private fun getChannelIdForType(type: NotificationType): String {
        return when (type) {
            NotificationType.COMMENT_MENTION,
            NotificationType.COMMENT_REPLY -> CHANNEL_ID_MESSAGES
            else -> CHANNEL_ID_DEFAULT
        }
    }

    /**
     * Get notification priority for backwards compatibility
     */
    private fun getNotificationPriority(priority: NotificationPriority): Int {
        return when (priority) {
            NotificationPriority.URGENT, NotificationPriority.HIGH -> NotificationCompat.PRIORITY_HIGH
            NotificationPriority.MEDIUM -> NotificationCompat.PRIORITY_DEFAULT
            NotificationPriority.LOW -> NotificationCompat.PRIORITY_LOW
        }
    }

    /**
     * Get notification icon based on type
     */
    private fun getNotificationIcon(type: NotificationType): Int {
        return when (type) {
            NotificationType.TASK_ASSIGNED -> android.R.drawable.ic_menu_today
            NotificationType.COMMENT_MENTION,
            NotificationType.COMMENT_REPLY -> android.R.drawable.ic_menu_send
            NotificationType.PERMISSION_GRANTED -> android.R.drawable.ic_menu_share
            NotificationType.DOCUMENT_SHARED -> android.R.drawable.ic_menu_share
            NotificationType.TASK_DUE_SOON, NotificationType.TASK_OVERDUE -> android.R.drawable.ic_menu_recent_history
            else -> android.R.drawable.ic_dialog_info
        }
    }

    /**
     * Get notification color based on type
     */
    private fun getNotificationColor(type: NotificationType): Int {
        return when (type) {
            NotificationType.TASK_OVERDUE -> 0xFFD32F2F.toInt() // Red
            NotificationType.TASK_DUE_SOON -> 0xFFFFA000.toInt() // Orange
            NotificationType.TASK_ASSIGNED -> 0xFF1976D2.toInt() // Blue
            NotificationType.COMMENT_MENTION -> 0xFF388E3C.toInt() // Green
            NotificationType.PERMISSION_GRANTED -> 0xFF7B1FA2.toInt() // Purple
            else -> 0xFF1976D2.toInt() // Default blue
        }
    }

    /**
     * Get group key for notification grouping
     */
    private fun getGroupKey(type: NotificationType): String {
        return when (type) {
            NotificationType.TASK_ASSIGNED,
            NotificationType.TASK_COMPLETED,
            NotificationType.TASK_DUE_SOON,
            NotificationType.TASK_OVERDUE -> "tasks"

            NotificationType.COMMENT_MENTION,
            NotificationType.COMMENT_REPLY -> "messages"

            NotificationType.PERMISSION_GRANTED,
            NotificationType.PERMISSION_REVOKED -> "permissions"

            NotificationType.DOCUMENT_SHARED,
            NotificationType.DOCUMENT_UPDATED -> "documents"

            else -> "general"
        }
    }

    /**
     * Show a chat message notification with messaging-specific features
     */
    fun showChatNotification(
        chatRoomId: String,
        chatRoomName: String,
        senderName: String,
        message: String,
        timestamp: Long
    ) {
        val notificationId = chatRoomId.hashCode()

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("deep_link", "flowboard://chat/$chatRoomId")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID_MESSAGES)
            .setSmallIcon(android.R.drawable.ic_menu_send)
            .setContentTitle(chatRoomName)
            .setContentText(message)
            .setStyle(NotificationCompat.MessagingStyle(senderName)
                .addMessage(message, timestamp, senderName)
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setGroup("messages")
            .setColor(0xFF1976D2.toInt())

        try {
            notificationManager.notify(notificationId, builder.build())
        } catch (e: SecurityException) {
            // Permission not granted, handle silently or log
        }
    }
}
