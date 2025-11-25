package com.flowboard.domain

import com.flowboard.data.database.BoardPermissions
import com.flowboard.data.database.DatabaseFactory.dbQuery
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import java.util.*

class PermissionService {
    suspend fun grantPermission(boardId: String, userId: String) {
        dbQuery {
            BoardPermissions.insert {
                it[BoardPermissions.boardId] = UUID.fromString(boardId)
                it[BoardPermissions.userId] = UUID.fromString(userId)
            }
        }
    }

    suspend fun hasPermission(boardId: String, userId: String): Boolean {
        return dbQuery {
            BoardPermissions.select {
                (BoardPermissions.boardId eq UUID.fromString(boardId)) and (BoardPermissions.userId eq UUID.fromString(userId))
            }.count() > 0
        }
    }
}
