package com.flowboard.domain

import com.flowboard.data.database.DatabaseFactory.dbQuery
import com.flowboard.data.database.Tasks
import com.flowboard.data.models.*
import com.flowboard.services.WebSocketManager
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.slf4j.LoggerFactory
import java.util.*

class TaskService(private val webSocketManager: WebSocketManager? = null) {

    private val logger = LoggerFactory.getLogger(TaskService::class.java)
    
    suspend fun getAllTasksForUser(userId: String): List<Task> = dbQuery {
        Tasks.select { Tasks.createdBy eq UUID.fromString(userId) or (Tasks.assignedTo eq UUID.fromString(userId)) }
            .map { rowToTask(it) }
    }
    
    suspend fun getTaskById(id: String): Task? = dbQuery {
        Tasks.select { Tasks.id eq UUID.fromString(id) }
            .map { rowToTask(it) }
            .singleOrNull()
    }
    
    suspend fun getTasksByProject(projectId: String): List<Task> = dbQuery {
        Tasks.select { Tasks.projectId eq UUID.fromString(projectId) }
            .map { rowToTask(it) }
    }
    
    suspend fun getTasksByStatus(isCompleted: Boolean): List<Task> = dbQuery {
        Tasks.select { Tasks.isCompleted eq isCompleted }
            .map { rowToTask(it) }
    }
    
    suspend fun getEventsBetweenDates(startDate: String, endDate: String, userId: String): List<Task> = dbQuery {
        val start = LocalDateTime.parse(startDate)
        val end = LocalDateTime.parse(endDate)
        
        Tasks.select { 
            (Tasks.isEvent eq true) and 
            (Tasks.eventStartTime greaterEq start) and 
            (Tasks.eventStartTime lessEq end) and
            (Tasks.createdBy eq UUID.fromString(userId) or (Tasks.assignedTo eq UUID.fromString(userId)))
        }.map { rowToTask(it) }
    }
    
    suspend fun createTask(request: CreateTaskRequest, userId: String, userInfo: UserPresenceInfo? = null): Task {
        val task = dbQuery {
            val taskId = UUID.randomUUID()
            val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

            Tasks.insert {
                it[id] = taskId
                it[title] = request.title
                it[description] = request.description
                it[priority] = request.priority
                it[dueDate] = request.dueDate
                it[createdAt] = now
                it[updatedAt] = now
                it[assignedTo] = request.assignedTo?.let { assignedToId -> UUID.fromString(assignedToId) }
                it[projectId] = request.projectId?.let { projId -> UUID.fromString(projId) }
                it[tags] = request.tags
                it[isEvent] = request.isEvent
                it[eventStartTime] = request.eventStartTime
                it[eventEndTime] = request.eventEndTime
                it[location] = request.location
                it[createdBy] = UUID.fromString(userId)
            }

            getTaskById(taskId.toString())!!
        }

        // Emitir evento WebSocket si está habilitado y tiene projectId (boardId)
        if (webSocketManager != null && task.projectId != null && userInfo != null) {
            try {
                webSocketManager.broadcastToRoom(
                    boardId = task.projectId!!,
                    message = TaskCreatedMessage(
                        timestamp = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
                        boardId = task.projectId!!,
                        task = task.toSnapshot(),
                        createdBy = userInfo
                    )
                )
                logger.debug("Broadcasted TASK_CREATED event for task ${task.id} to board ${task.projectId}")
            } catch (e: Exception) {
                logger.error("Failed to broadcast TASK_CREATED event", e)
            }
        }

        return task
    }
    
    suspend fun updateTask(id: String, request: UpdateTaskRequest, userId: String, userInfo: UserPresenceInfo? = null): Task? {
        val updatedTask = dbQuery {
            val taskId = UUID.fromString(id)
            val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

            val updateCount = Tasks.update({ Tasks.id eq taskId }) {
                request.title?.let { title -> it[Tasks.title] = title }
                request.description?.let { desc -> it[Tasks.description] = desc }
                request.isCompleted?.let { completed -> it[Tasks.isCompleted] = completed }
                request.priority?.let { priority -> it[Tasks.priority] = priority }
                request.dueDate?.let { dueDate -> it[Tasks.dueDate] = dueDate }
                request.assignedTo?.let { assignedTo -> it[Tasks.assignedTo] = UUID.fromString(assignedTo) }
                request.projectId?.let { projectId -> it[Tasks.projectId] = UUID.fromString(projectId) }
                request.tags?.let { tags -> it[Tasks.tags] = tags }
                request.attachments?.let { attachments -> it[Tasks.attachments] = attachments }
                request.isEvent?.let { isEvent -> it[Tasks.isEvent] = isEvent }
                request.eventStartTime?.let { startTime -> it[Tasks.eventStartTime] = startTime }
                request.eventEndTime?.let { endTime -> it[Tasks.eventEndTime] = endTime }
                request.location?.let { location -> it[Tasks.location] = location }
                it[Tasks.updatedAt] = now
            }

            if (updateCount > 0) getTaskById(id) else null
        }

        // Emitir evento WebSocket si se actualizó exitosamente
        if (webSocketManager != null && updatedTask != null && updatedTask.projectId != null && userInfo != null) {
            try {
                // Construir mapa de cambios
                val changes = mutableMapOf<String, String>()
                request.title?.let { changes["title"] = it }
                request.description?.let { changes["description"] = it }
                request.isCompleted?.let { changes["isCompleted"] = it.toString() }
                request.priority?.let { changes["priority"] = it.name }
                request.dueDate?.let { changes["dueDate"] = it.toString() }
                request.assignedTo?.let { changes["assignedTo"] = it }

                webSocketManager.broadcastToRoom(
                    boardId = updatedTask.projectId!!,
                    message = TaskUpdatedMessage(
                        timestamp = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
                        boardId = updatedTask.projectId!!,
                        taskId = updatedTask.id,
                        changes = changes,
                        updatedBy = userInfo
                    )
                )
                logger.debug("Broadcasted TASK_UPDATED event for task ${updatedTask.id}")
            } catch (e: Exception) {
                logger.error("Failed to broadcast TASK_UPDATED event", e)
            }
        }

        return updatedTask
    }
    
    suspend fun deleteTask(id: String, userId: String, userInfo: UserPresenceInfo? = null): Boolean {
        // Obtener la tarea antes de eliminarla para tener su projectId
        val task = getTaskById(id)

        val deleted = dbQuery {
            val taskId = UUID.fromString(id)
            val deleteCount = Tasks.deleteWhere {
                (Tasks.id eq taskId) and (Tasks.createdBy eq UUID.fromString(userId))
            }
            deleteCount > 0
        }

        // Emitir evento WebSocket si se eliminó exitosamente
        if (webSocketManager != null && deleted && task != null && task.projectId != null && userInfo != null) {
            try {
                webSocketManager.broadcastToRoom(
                    boardId = task.projectId!!,
                    message = TaskDeletedMessage(
                        timestamp = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
                        boardId = task.projectId!!,
                        taskId = id,
                        deletedBy = userInfo
                    )
                )
                logger.debug("Broadcasted TASK_DELETED event for task $id")
            } catch (e: Exception) {
                logger.error("Failed to broadcast TASK_DELETED event", e)
            }
        }

        return deleted
    }
    
    suspend fun toggleTaskStatus(id: String, userId: String, userInfo: UserPresenceInfo? = null): Task? {
        val task = getTaskById(id)
        return if (task != null) {
            val updateRequest = UpdateTaskRequest(isCompleted = !task.isCompleted)
            updateTask(id, updateRequest, userId, userInfo)
        } else {
            null
        }
    }
    
    private fun rowToTask(row: ResultRow): Task {
        return Task(
            id = row[Tasks.id].toString(),
            title = row[Tasks.title],
            description = row[Tasks.description],
            isCompleted = row[Tasks.isCompleted],
            priority = row[Tasks.priority],
            dueDate = row[Tasks.dueDate],
            createdAt = row[Tasks.createdAt],
            updatedAt = row[Tasks.updatedAt],
            assignedTo = row[Tasks.assignedTo]?.toString(),
            projectId = row[Tasks.projectId]?.toString(),
            tags = row[Tasks.tags],
            attachments = row[Tasks.attachments],
            isEvent = row[Tasks.isEvent],
            eventStartTime = row[Tasks.eventStartTime],
            eventEndTime = row[Tasks.eventEndTime],
            location = row[Tasks.location],
            createdBy = row[Tasks.createdBy].toString()
        )
    }
}