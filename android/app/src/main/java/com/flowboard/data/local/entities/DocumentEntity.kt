package com.flowboard.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val content: String,
    val ownerId: String,
    val ownerName: String? = null,
    val isPublic: Boolean = false,
    val createdAt: String,
    val updatedAt: String,
    val lastEditedBy: String? = null,
    val lastEditedByName: String? = null,
    val isSync: Boolean = false,
    val lastSyncAt: String? = null
)
