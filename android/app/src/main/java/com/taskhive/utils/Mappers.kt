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