package com.flowboard.data.models

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class Project(
    val id: String,
    val name: String,
    val description: String,
    val color: String = "#2196F3",
    val ownerId: String,
    val members: List<String> = emptyList(),
    val isActive: Boolean = true,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val deadline: LocalDateTime? = null
)

@Serializable
data class CreateProjectRequest(
    val name: String,
    val description: String = "",
    val color: String = "#2196F3",
    val deadline: LocalDateTime? = null
)

@Serializable
data class UpdateProjectRequest(
    val name: String? = null,
    val description: String? = null,
    val color: String? = null,
    val isActive: Boolean? = null,
    val deadline: LocalDateTime? = null
)
