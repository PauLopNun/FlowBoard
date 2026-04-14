package com.flowboard.domain

import com.flowboard.data.database.DatabaseFactory.dbQuery
import com.flowboard.data.database.Users
import com.flowboard.data.database.WorkspaceMembers
import com.flowboard.data.database.Workspaces
import com.flowboard.data.models.*
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.util.*

class WorkspaceService {

    private fun generateInviteCode(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..8).map { chars[chars.indices.random()] }.joinToString("")
    }

    suspend fun createWorkspace(name: String, description: String?, ownerId: String): Workspace {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        val workspaceId = UUID.randomUUID()
        val inviteCode = generateInviteCode()

        dbQuery {
            Workspaces.insert {
                it[Workspaces.id] = workspaceId
                it[Workspaces.name] = name
                it[Workspaces.description] = description
                it[Workspaces.ownerId] = UUID.fromString(ownerId)
                it[Workspaces.inviteCode] = inviteCode
                it[Workspaces.createdAt] = now
                it[Workspaces.updatedAt] = now
            }
            WorkspaceMembers.insert {
                it[WorkspaceMembers.id] = UUID.randomUUID()
                it[WorkspaceMembers.workspaceId] = workspaceId
                it[WorkspaceMembers.userId] = UUID.fromString(ownerId)
                it[WorkspaceMembers.role] = "OWNER"
                it[WorkspaceMembers.joinedAt] = now
            }
        }

        return getWorkspaceById(workspaceId.toString(), ownerId)!!
    }

    suspend fun getWorkspaceById(workspaceId: String, requesterId: String): Workspace? {
        return dbQuery {
            val row = Workspaces
                .leftJoin(Users, { Workspaces.ownerId }, { Users.id })
                .select { Workspaces.id eq UUID.fromString(workspaceId) }
                .singleOrNull() ?: return@dbQuery null

            val isMember = WorkspaceMembers
                .select {
                    (WorkspaceMembers.workspaceId eq UUID.fromString(workspaceId)) and
                    (WorkspaceMembers.userId eq UUID.fromString(requesterId))
                }.count() > 0

            if (!isMember) return@dbQuery null

            val members = WorkspaceMembers
                .leftJoin(Users, { WorkspaceMembers.userId }, { Users.id })
                .select { WorkspaceMembers.workspaceId eq UUID.fromString(workspaceId) }
                .map { m ->
                    WorkspaceMember(
                        userId = m[WorkspaceMembers.userId].toString(),
                        userName = m[Users.username],
                        userEmail = m[Users.email],
                        role = m[WorkspaceMembers.role],
                        joinedAt = m[WorkspaceMembers.joinedAt]
                    )
                }

            Workspace(
                id = row[Workspaces.id].toString(),
                name = row[Workspaces.name],
                description = row[Workspaces.description],
                ownerId = row[Workspaces.ownerId].toString(),
                ownerName = row[Users.username],
                inviteCode = row[Workspaces.inviteCode],
                members = members,
                createdAt = row[Workspaces.createdAt],
                updatedAt = row[Workspaces.updatedAt]
            )
        }
    }

    suspend fun getUserWorkspaces(userId: String): WorkspaceListResponse {
        return dbQuery {
            val memberWorkspaceIds = WorkspaceMembers
                .select { WorkspaceMembers.userId eq UUID.fromString(userId) }
                .map { it[WorkspaceMembers.workspaceId].toString() }

            val allWorkspaces = Workspaces
                .leftJoin(Users, { Workspaces.ownerId }, { Users.id })
                .select { Workspaces.id inList memberWorkspaceIds.map { UUID.fromString(it) } }
                .map { row ->
                    Workspace(
                        id = row[Workspaces.id].toString(),
                        name = row[Workspaces.name],
                        description = row[Workspaces.description],
                        ownerId = row[Workspaces.ownerId].toString(),
                        ownerName = row[Users.username],
                        inviteCode = row[Workspaces.inviteCode],
                        createdAt = row[Workspaces.createdAt],
                        updatedAt = row[Workspaces.updatedAt]
                    )
                }

            WorkspaceListResponse(
                owned = allWorkspaces.filter { it.ownerId == userId },
                member = allWorkspaces.filter { it.ownerId != userId }
            )
        }
    }

    suspend fun joinWorkspace(inviteCode: String, userId: String): Workspace? {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        val workspaceId = dbQuery {
            val workspace = Workspaces
                .select { Workspaces.inviteCode eq inviteCode }
                .singleOrNull() ?: return@dbQuery null

            val alreadyMember = WorkspaceMembers
                .select {
                    (WorkspaceMembers.workspaceId eq workspace[Workspaces.id].value) and
                    (WorkspaceMembers.userId eq UUID.fromString(userId))
                }.count() > 0

            if (!alreadyMember) {
                WorkspaceMembers.insert {
                    it[WorkspaceMembers.id] = UUID.randomUUID()
                    it[WorkspaceMembers.workspaceId] = workspace[Workspaces.id].value
                    it[WorkspaceMembers.userId] = UUID.fromString(userId)
                    it[WorkspaceMembers.role] = "MEMBER"
                    it[WorkspaceMembers.joinedAt] = now
                }
            }
            workspace[Workspaces.id].toString()
        } ?: return null

        return getWorkspaceById(workspaceId, userId)
    }

    suspend fun removeMember(workspaceId: String, ownerId: String, targetUserId: String): Boolean {
        return dbQuery {
            val isOwner = Workspaces
                .select {
                    (Workspaces.id eq UUID.fromString(workspaceId)) and
                    (Workspaces.ownerId eq UUID.fromString(ownerId))
                }.count() > 0

            if (!isOwner) return@dbQuery false

            WorkspaceMembers.deleteWhere {
                (WorkspaceMembers.workspaceId eq UUID.fromString(workspaceId)) and
                (WorkspaceMembers.userId eq UUID.fromString(targetUserId))
            } > 0
        }
    }

    suspend fun deleteWorkspace(workspaceId: String, ownerId: String): Boolean {
        return dbQuery {
            val isOwner = Workspaces
                .select {
                    (Workspaces.id eq UUID.fromString(workspaceId)) and
                    (Workspaces.ownerId eq UUID.fromString(ownerId))
                }.count() > 0

            if (!isOwner) return@dbQuery false

            WorkspaceMembers.deleteWhere { WorkspaceMembers.workspaceId eq UUID.fromString(workspaceId) }
            Workspaces.deleteWhere { Workspaces.id eq UUID.fromString(workspaceId) } > 0
        }
    }
}
