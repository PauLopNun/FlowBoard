package com.flowboard.utils

import com.flowboard.data.local.entities.TaskEntity
import com.flowboard.data.local.entities.UserEntity
import com.flowboard.data.local.entities.ProjectEntity
import com.flowboard.domain.model.Task
import com.flowboard.domain.model.User
import com.flowboard.domain.model.Project

fun TaskEntity.toDomain(): Task {
    return Task(
        id = id,
        title = title,
        description = description,
        isCompleted = isCompleted,
        priority = priority,
        dueDate = dueDate,
        createdAt = createdAt,
        updatedAt = updatedAt,
        assignedTo = assignedTo,
        projectId = projectId,
        tags = tags,
        attachments = attachments,
        isEvent = isEvent,
        eventStartTime = eventStartTime,
        eventEndTime = eventEndTime,
        location = location
    )
}

fun Task.toEntity(): TaskEntity {
    return TaskEntity(
        id = id,
        title = title,
        description = description,
        isCompleted = isCompleted,
        priority = priority,
        dueDate = dueDate,
        createdAt = createdAt,
        updatedAt = updatedAt,
        assignedTo = assignedTo,
        projectId = projectId,
        tags = tags,
        attachments = attachments,
        isEvent = isEvent,
        eventStartTime = eventStartTime,
        eventEndTime = eventEndTime,
        location = location,
        isSync = true,
        lastSyncAt = updatedAt
    )
}

fun com.flowboard.data.models.crdt.CollaborativeDocument.toDomain(): com.flowboard.domain.model.CollaborativeDocument {
    return com.flowboard.domain.model.CollaborativeDocument(
        id = id,
        blocks = blocks.map { it.toDomain() }
    )
}

fun com.flowboard.data.models.crdt.ContentBlock.toDomain(): com.flowboard.domain.model.ContentBlock {
    return com.flowboard.domain.model.ContentBlock(
        id = id,
        type = type,
        content = content,
        fontWeight = fontWeight,
        fontStyle = fontStyle,
        textDecoration = textDecoration,
        fontSize = fontSize,
        color = color,
        textAlign = textAlign
    )
}

fun UserEntity.toDomain(): User {
    return User(
        id = id,
        email = email,
        username = username,
        fullName = fullName,
        role = role,
        profileImageUrl = profileImageUrl,
        isActive = isActive,
        createdAt = createdAt,
        lastLoginAt = lastLoginAt,
        preferences = preferences
    )
}

fun User.toEntity(): UserEntity {
    return UserEntity(
        id = id,
        email = email,
        username = username,
        fullName = fullName,
        role = role,
        profileImageUrl = profileImageUrl,
        isActive = isActive,
        createdAt = createdAt,
        lastLoginAt = lastLoginAt,
        preferences = preferences
    )
}

fun ProjectEntity.toDomain(): Project {
    return Project(
        id = id,
        name = name,
        description = description,
        color = color,
        ownerId = ownerId,
        members = members,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deadline = deadline
    )
}

fun Project.toEntity(): ProjectEntity {
    return ProjectEntity(
        id = id,
        name = name,
        description = description,
        color = color,
        ownerId = ownerId,
        members = members,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deadline = deadline,
        isSync = true,
        lastSyncAt = updatedAt
    )
}

// Permission Mappers
fun com.flowboard.data.remote.dto.PermissionDto.toDomain(): com.flowboard.domain.model.Permission {
    return com.flowboard.domain.model.Permission(
        id = id,
        resourceId = resourceId,
        resourceType = com.flowboard.domain.model.ResourceType.valueOf(resourceType.uppercase()),
        userId = userId,
        userEmail = userEmail,
        userName = userName,
        level = com.flowboard.domain.model.PermissionLevel.valueOf(level.uppercase()),
        grantedBy = grantedBy,
        grantedAt = grantedAt,
        expiresAt = expiresAt
    )
}

fun com.flowboard.data.remote.dto.UserPermissionInfoDto.toDomain(): com.flowboard.domain.model.UserPermissionInfo {
    return com.flowboard.domain.model.UserPermissionInfo(
        userId = userId,
        email = email,
        userName = userName,
        permissionLevel = com.flowboard.domain.model.PermissionLevel.valueOf(permissionLevel.uppercase()),
        grantedAt = grantedAt,
        isOnline = isOnline
    )
}

fun com.flowboard.data.remote.dto.PermissionListResponseDto.toDomain(): com.flowboard.domain.model.PermissionListResponse {
    return com.flowboard.domain.model.PermissionListResponse(
        resourceId = resourceId,
        resourceType = com.flowboard.domain.model.ResourceType.valueOf(resourceType.uppercase()),
        owner = owner.toDomain(),
        collaborators = collaborators.map { it.toDomain() }
    )
}

fun com.flowboard.domain.model.GrantPermissionRequest.toDto(): com.flowboard.data.remote.dto.GrantPermissionRequestDto {
    return com.flowboard.data.remote.dto.GrantPermissionRequestDto(
        resourceId = resourceId,
        resourceType = resourceType.name.lowercase(),
        userEmail = userEmail,
        level = level.name.lowercase(),
        expiresAt = expiresAt
    )
}

fun com.flowboard.domain.model.UpdatePermissionRequest.toDto(): com.flowboard.data.remote.dto.UpdatePermissionRequestDto {
    return com.flowboard.data.remote.dto.UpdatePermissionRequestDto(
        permissionId = permissionId,
        newLevel = newLevel.name.lowercase()
    )
}

// Notification Mappers
fun com.flowboard.data.local.entities.NotificationEntity.toDomain(): com.flowboard.domain.model.Notification {
    return com.flowboard.domain.model.Notification(
        id = id,
        userId = userId,
        type = com.flowboard.domain.model.NotificationType.valueOf(type.uppercase()),
        priority = com.flowboard.domain.model.NotificationPriority.valueOf(priority.uppercase()),
        title = title,
        message = message,
        resourceId = resourceId,
        resourceType = resourceType?.let { com.flowboard.domain.model.ResourceType.valueOf(it.uppercase()) },
        actionUserId = actionUserId,
        actionUserName = actionUserName,
        imageUrl = imageUrl,
        deepLink = deepLink,
        metadata = metadata,
        isRead = isRead,
        createdAt = createdAt,
        expiresAt = expiresAt
    )
}

fun com.flowboard.domain.model.Notification.toEntity(): com.flowboard.data.local.entities.NotificationEntity {
    return com.flowboard.data.local.entities.NotificationEntity(
        id = id,
        userId = userId,
        type = type.name.lowercase(),
        priority = priority.name.lowercase(),
        title = title,
        message = message,
        resourceId = resourceId,
        resourceType = resourceType?.name?.lowercase(),
        actionUserId = actionUserId,
        actionUserName = actionUserName,
        imageUrl = imageUrl,
        deepLink = deepLink,
        metadata = metadata,
        isRead = isRead,
        createdAt = createdAt,
        expiresAt = expiresAt
    )
}