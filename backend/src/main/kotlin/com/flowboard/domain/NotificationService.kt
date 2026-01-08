package com.flowboard.domain

import com.flowboard.data.database.DatabaseFactory.dbQuery
import com.flowboard.data.database.Notifications
import com.flowboard.data.models.Notification
import com.flowboard.data.models.CreateNotificationRequest
import com.flowboard.data.models.NotificationStats
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.util.*

class NotificationService {

    suspend fun createNotification(request: CreateNotificationRequest): Notification {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        val notificationId = UUID.randomUUID()

        dbQuery {
            Notifications.insert {
                it[Notifications.id] = notificationId
                it[Notifications.userId] = UUID.fromString(request.userId)
                it[Notifications.type] = request.type
                it[Notifications.title] = request.title
                it[Notifications.message] = request.message
                it[Notifications.resourceId] = request.resourceId?.let { UUID.fromString(it) }
                it[Notifications.resourceType] = request.resourceType
                it[Notifications.actionUserId] = request.actionUserId?.let { UUID.fromString(it) }
                it[Notifications.actionUserName] = request.actionUserName
                it[Notifications.deepLink] = request.deepLink
                it[Notifications.isRead] = false
                it[Notifications.createdAt] = now
                it[Notifications.expiresAt] = null
            }
        }

        return Notification(
            id = notificationId.toString(),
            userId = request.userId,
            type = request.type,
            title = request.title,
            message = request.message,
            resourceId = request.resourceId,
            resourceType = request.resourceType,
            actionUserId = request.actionUserId,
            actionUserName = request.actionUserName,
            deepLink = request.deepLink,
            isRead = false,
            createdAt = now,
            expiresAt = null
        )
    }

    suspend fun getUserNotifications(userId: String, limit: Int = 50): List<Notification> {
        return dbQuery {
            Notifications
                .select { Notifications.userId eq UUID.fromString(userId) }
                .orderBy(Notifications.createdAt to SortOrder.DESC)
                .limit(limit)
                .map { row ->
                    Notification(
                        id = row[Notifications.id].toString(),
                        userId = row[Notifications.userId].toString(),
                        type = row[Notifications.type],
                        title = row[Notifications.title],
                        message = row[Notifications.message],
                        resourceId = row[Notifications.resourceId]?.toString(),
                        resourceType = row[Notifications.resourceType],
                        actionUserId = row[Notifications.actionUserId]?.toString(),
                        actionUserName = row[Notifications.actionUserName],
                        deepLink = row[Notifications.deepLink],
                        isRead = row[Notifications.isRead],
                        createdAt = row[Notifications.createdAt],
                        expiresAt = row[Notifications.expiresAt]
                    )
                }
        }
    }

    suspend fun getUnreadNotifications(userId: String): List<Notification> {
        return dbQuery {
            Notifications
                .select {
                    (Notifications.userId eq UUID.fromString(userId)) and
                    (Notifications.isRead eq false)
                }
                .orderBy(Notifications.createdAt to SortOrder.DESC)
                .map { row ->
                    Notification(
                        id = row[Notifications.id].toString(),
                        userId = row[Notifications.userId].toString(),
                        type = row[Notifications.type],
                        title = row[Notifications.title],
                        message = row[Notifications.message],
                        resourceId = row[Notifications.resourceId]?.toString(),
                        resourceType = row[Notifications.resourceType],
                        actionUserId = row[Notifications.actionUserId]?.toString(),
                        actionUserName = row[Notifications.actionUserName],
                        deepLink = row[Notifications.deepLink],
                        isRead = row[Notifications.isRead],
                        createdAt = row[Notifications.createdAt],
                        expiresAt = row[Notifications.expiresAt]
                    )
                }
        }
    }

    suspend fun markAsRead(notificationId: String, userId: String): Boolean {
        return dbQuery {
            Notifications.update({
                (Notifications.id eq UUID.fromString(notificationId)) and
                (Notifications.userId eq UUID.fromString(userId))
            }) {
                it[Notifications.isRead] = true
            } > 0
        }
    }

    suspend fun markAllAsRead(userId: String): Boolean {
        return dbQuery {
            Notifications.update({
                (Notifications.userId eq UUID.fromString(userId)) and
                (Notifications.isRead eq false)
            }) {
                it[Notifications.isRead] = true
            } > 0
        }
    }

    suspend fun deleteNotification(notificationId: String, userId: String): Boolean {
        return dbQuery {
            Notifications.deleteWhere {
                (Notifications.id eq UUID.fromString(notificationId)) and
                (Notifications.userId eq UUID.fromString(userId))
            } > 0
        }
    }

    suspend fun deleteAllNotifications(userId: String): Boolean {
        return dbQuery {
            Notifications.deleteWhere {
                Notifications.userId eq UUID.fromString(userId)
            } > 0
        }
    }

    suspend fun getNotificationStats(userId: String): NotificationStats {
        return dbQuery {
            val total = Notifications
                .select { Notifications.userId eq UUID.fromString(userId) }
                .count()
                .toInt()

            val unread = Notifications
                .select {
                    (Notifications.userId eq UUID.fromString(userId)) and
                    (Notifications.isRead eq false)
                }
                .count()
                .toInt()

            NotificationStats(
                total = total,
                unread = unread
            )
        }
    }

    // Helper method to send a document shared notification
    suspend fun sendDocumentSharedNotification(
        recipientId: String,
        senderName: String,
        documentTitle: String,
        documentId: String
    ) {
        createNotification(
            CreateNotificationRequest(
                userId = recipientId,
                type = "DOCUMENT_SHARED",
                title = "Document Shared",
                message = "$senderName shared \"$documentTitle\" with you",
                resourceId = documentId,
                resourceType = "document",
                actionUserName = senderName,
                deepLink = "/document_edit/$documentId"
            )
        )
    }
}
