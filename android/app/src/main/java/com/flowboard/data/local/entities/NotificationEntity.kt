package com.flowboard.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.flowboard.data.local.converters.MapConverter

@Entity(tableName = "notifications")
@TypeConverters(MapConverter::class)
data class NotificationEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val type: String,
    val priority: String,
    val title: String,
    val message: String,
    val resourceId: String? = null,
    val resourceType: String? = null,
    val actionUserId: String? = null,
    val actionUserName: String? = null,
    val imageUrl: String? = null,
    val deepLink: String? = null,
    val metadata: Map<String, String> = emptyMap(),
    val isRead: Boolean = false,
    val createdAt: Long,
    val expiresAt: Long? = null
)
