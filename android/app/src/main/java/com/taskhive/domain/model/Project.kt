package com.flowboard.domain.model

import kotlinx.datetime.LocalDateTime

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