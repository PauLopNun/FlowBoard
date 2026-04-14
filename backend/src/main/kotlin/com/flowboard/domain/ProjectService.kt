package com.flowboard.domain

import com.flowboard.data.database.DatabaseFactory.dbQuery
import com.flowboard.data.database.Projects
import com.flowboard.data.models.Project
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.util.*

class ProjectService {

    suspend fun getUserProjects(userId: String): List<Project> = dbQuery {
        Projects.select {
            (Projects.ownerId eq UUID.fromString(userId)) and (Projects.isActive eq true)
        }.orderBy(Projects.createdAt to SortOrder.DESC)
            .map { it.toProject() }
    }

    suspend fun getProjectById(projectId: String, userId: String): Project? = dbQuery {
        Projects.select { Projects.id eq UUID.fromString(projectId) }
            .singleOrNull()
            ?.toProject()
    }

    suspend fun createProject(
        name: String,
        description: String,
        color: String,
        ownerId: String,
        deadline: LocalDateTime?
    ): Project {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        val projectId = UUID.randomUUID()
        dbQuery {
            Projects.insert {
                it[Projects.id] = projectId
                it[Projects.name] = name
                it[Projects.description] = description
                it[Projects.color] = color
                it[Projects.ownerId] = UUID.fromString(ownerId)
                it[Projects.members] = emptyList()
                it[Projects.isActive] = true
                it[Projects.createdAt] = now
                it[Projects.updatedAt] = now
                it[Projects.deadline] = deadline
            }
        }
        return getProjectById(projectId.toString(), ownerId)!!
    }

    suspend fun updateProject(
        projectId: String,
        ownerId: String,
        name: String?,
        description: String?,
        color: String?,
        isActive: Boolean?,
        deadline: LocalDateTime?
    ): Project? {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        val updated = dbQuery {
            Projects.update({
                (Projects.id eq UUID.fromString(projectId)) and
                (Projects.ownerId eq UUID.fromString(ownerId))
            }) {
                if (name != null) it[Projects.name] = name
                if (description != null) it[Projects.description] = description
                if (color != null) it[Projects.color] = color
                if (isActive != null) it[Projects.isActive] = isActive
                if (deadline != null) it[Projects.deadline] = deadline
                it[Projects.updatedAt] = now
            }
        }
        if (updated == 0) return null
        return getProjectById(projectId, ownerId)
    }

    suspend fun deleteProject(projectId: String, ownerId: String): Boolean = dbQuery {
        Projects.deleteWhere {
            (Projects.id eq UUID.fromString(projectId)) and
            (Projects.ownerId eq UUID.fromString(ownerId))
        } > 0
    }

    private fun ResultRow.toProject() = Project(
        id = this[Projects.id].toString(),
        name = this[Projects.name],
        description = this[Projects.description],
        color = this[Projects.color],
        ownerId = this[Projects.ownerId].toString(),
        members = this[Projects.members],
        isActive = this[Projects.isActive],
        createdAt = this[Projects.createdAt],
        updatedAt = this[Projects.updatedAt],
        deadline = this[Projects.deadline]
    )
}
