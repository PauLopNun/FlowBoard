package com.flowboard.data.repository

import android.util.Log
import com.flowboard.data.local.dao.TaskDao
import com.flowboard.data.local.entities.TaskEntity
import com.flowboard.data.local.entities.TaskPriority
import com.flowboard.data.remote.api.TaskApiService
import com.flowboard.data.remote.dto.*
import com.flowboard.data.remote.websocket.TaskWebSocketClient
import com.flowboard.data.remote.websocket.WebSocketState
import com.flowboard.domain.model.Task
import com.flowboard.domain.repository.TaskRepository
import com.flowboard.utils.toEntity
import com.flowboard.utils.toDomain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepositoryImpl @Inject constructor(
    private val taskDao: TaskDao,
    private val taskApiService: TaskApiService,
    private val webSocketClient: TaskWebSocketClient
) : TaskRepository {

    private val TAG = "TaskRepositoryImpl"
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Usuarios activos en el board actual
    private val _activeUsers = MutableStateFlow<List<UserPresenceInfo>>(emptyList())
    val activeUsers: StateFlow<List<UserPresenceInfo>> = _activeUsers.asStateFlow()

    init {
        // Escuchar mensajes WebSocket y actualizar base de datos local
        repositoryScope.launch {
            webSocketClient.incomingMessages.collect { message ->
                handleWebSocketMessage(message)
            }
        }
    }
    
    override fun getAllTasks(): Flow<List<Task>> {
        return taskDao.getAllTasks().map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override fun getTasksByProject(projectId: String): Flow<List<Task>> {
        return taskDao.getTasksByProject(projectId).map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override fun getTasksByStatus(isCompleted: Boolean): Flow<List<Task>> {
        return taskDao.getTasksByStatus(isCompleted).map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override fun getEventsBetweenDates(startDate: LocalDateTime, endDate: LocalDateTime): Flow<List<Task>> {
        return taskDao.getEventsBetweenDates(startDate, endDate).map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override fun getTasksAssignedToUser(userId: String): Flow<List<Task>> {
        return taskDao.getTasksAssignedToUser(userId).map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override fun getOverdueTasks(): Flow<List<Task>> {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        return taskDao.getOverdueTasks(now).map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override suspend fun getTaskById(id: String): Task? {
        return taskDao.getTaskById(id)?.toDomain()
    }
    
    override suspend fun createTask(task: Task): Result<Task> {
        return try {
            val entity = task.toEntity().copy(
                isSync = false,
                lastSyncAt = null
            )
            taskDao.insertTask(entity)
            
            // Try to sync with backend
            try {
                taskApiService.createTask(entity)
                taskDao.markTaskAsSynced(
                    entity.id,
                    Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                )
            } catch (e: Exception) {
                // Backend sync failed, task remains unsynced
            }
            
            Result.success(task)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun updateTask(task: Task): Result<Task> {
        return try {
            val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            val entity = task.toEntity().copy(
                updatedAt = now,
                isSync = false,
                lastSyncAt = null
            )
            taskDao.updateTask(entity)
            
            // Try to sync with backend
            try {
                taskApiService.updateTask(entity.id, entity)
                taskDao.markTaskAsSynced(entity.id, now)
            } catch (e: Exception) {
                // Backend sync failed, task remains unsynced
            }
            
            Result.success(task.copy(updatedAt = now))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun deleteTask(id: String): Result<Unit> {
        return try {
            taskDao.deleteTaskById(id)
            
            // Try to sync with backend
            try {
                taskApiService.deleteTask(id)
            } catch (e: Exception) {
                // Backend sync failed, but local deletion succeeded
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun toggleTaskStatus(id: String): Result<Task> {
        return try {
            val task = taskDao.getTaskById(id) ?: return Result.failure(Exception("Task not found"))
            val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            val updatedTask = task.copy(
                isCompleted = !task.isCompleted,
                updatedAt = now,
                isSync = false
            )
            
            taskDao.updateTask(updatedTask)
            
            // Try to sync with backend
            try {
                taskApiService.updateTask(updatedTask.id, updatedTask)
                taskDao.markTaskAsSynced(updatedTask.id, now)
            } catch (e: Exception) {
                // Backend sync failed, task remains unsynced
            }
            
            Result.success(updatedTask.toDomain())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun syncTasks(): Result<Unit> {
        return try {
            // Get unsynced local tasks
            val unsyncedTasks = taskDao.getUnsyncedTasks()

            // Sync each unsynced task
            unsyncedTasks.forEach { task ->
                try {
                    if (task.createdAt == task.updatedAt) {
                        // New task
                        taskApiService.createTask(task)
                    } else {
                        // Updated task
                        taskApiService.updateTask(task.id, task)
                    }
                    taskDao.markTaskAsSynced(
                        task.id,
                        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                    )
                } catch (e: Exception) {
                    // Skip this task, will retry later
                }
            }

            // Fetch latest tasks from backend
            try {
                val remoteTasks = taskApiService.getAllTasks()
                taskDao.insertTasks(remoteTasks)
            } catch (e: Exception) {
                // Remote fetch failed, but local sync may have succeeded
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ============================================================================
    // WEBSOCKET METHODS
    // ============================================================================

    /**
     * Conecta al WebSocket para un board específico
     */
    suspend fun connectToBoard(boardId: String, token: String, userId: String) {
        Log.d(TAG, "Connecting to board: $boardId")
        webSocketClient.connect(boardId, token, userId)
    }

    /**
     * Desconecta del WebSocket
     */
    suspend fun disconnectFromBoard() {
        Log.d(TAG, "Disconnecting from board")
        webSocketClient.disconnect()
    }

    /**
     * Obtiene el estado de conexión WebSocket
     */
    fun getConnectionState(): StateFlow<WebSocketState> {
        return webSocketClient.connectionState
    }

    /**
     * Envía indicador de escritura
     */
    suspend fun sendTypingIndicator(boardId: String, taskId: String?, isTyping: Boolean) {
        webSocketClient.sendTypingIndicator(boardId, taskId, isTyping)
    }

    /**
     * Maneja mensajes WebSocket entrantes y actualiza la base de datos local
     */
    private suspend fun handleWebSocketMessage(message: WebSocketMessage) {
        Log.d(TAG, "Handling WebSocket message: ${message.type}")

        when (message) {
            is RoomJoinedMessage -> {
                // Usuario se unió exitosamente al room
                _activeUsers.value = message.activeUsers
                Log.d(TAG, "Joined room with ${message.activeUsers.size} active users")
            }

            is TaskCreatedMessage -> {
                // Otra persona creó una tarea
                try {
                    val task = message.task.toDomainTask()
                    val entity = task.toEntity().copy(
                        isSync = true,
                        lastSyncAt = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                    )
                    taskDao.insertTask(entity)
                    Log.d(TAG, "Task created via WebSocket: ${task.id}")
                } catch (e: Exception) {
                    Log.e(TAG, "Error handling TaskCreatedMessage", e)
                }
            }

            is TaskUpdatedMessage -> {
                // Otra persona actualizó una tarea
                try {
                    val existingTask = taskDao.getTaskById(message.taskId)
                    if (existingTask != null) {
                        val updatedTask = applyChanges(existingTask, message.changes)
                        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                        taskDao.updateTask(updatedTask.copy(
                            isSync = true,
                            lastSyncAt = now
                        ))
                        Log.d(TAG, "Task updated via WebSocket: ${message.taskId}")
                    } else {
                        Log.w(TAG, "Task ${message.taskId} not found locally for update")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error handling TaskUpdatedMessage", e)
                }
            }

            is TaskDeletedMessage -> {
                // Otra persona eliminó una tarea
                try {
                    taskDao.deleteTaskById(message.taskId)
                    Log.d(TAG, "Task deleted via WebSocket: ${message.taskId}")
                } catch (e: Exception) {
                    Log.e(TAG, "Error handling TaskDeletedMessage", e)
                }
            }

            is TaskMovedMessage -> {
                // Tarea movida a otro board
                try {
                    val task = taskDao.getTaskById(message.taskId)
                    if (task != null) {
                        taskDao.updateTask(task.copy(projectId = message.toBoardId))
                        Log.d(TAG, "Task moved via WebSocket: ${message.taskId} to ${message.toBoardId}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error handling TaskMovedMessage", e)
                }
            }

            is UserJoinedMessage -> {
                // Usuario se unió al board
                _activeUsers.value = _activeUsers.value + message.user
                Log.d(TAG, "User joined: ${message.user.username}")
            }

            is UserLeftMessage -> {
                // Usuario salió del board
                _activeUsers.value = _activeUsers.value.filter { it.userId != message.userId }
                Log.d(TAG, "User left: ${message.userId}")
            }

            is UserTypingMessage -> {
                // Usuario está escribiendo (se puede manejar en UI)
                Log.d(TAG, "User ${message.user.username} is typing")
            }

            is ErrorMessage -> {
                Log.e(TAG, "WebSocket error: ${message.message}")
            }

            else -> {
                Log.d(TAG, "Unhandled message type: ${message.type}")
            }
        }
    }

    /**
     * Aplica cambios incrementales a una tarea existente
     */
    private fun applyChanges(task: TaskEntity, changes: Map<String, String>): TaskEntity {
        var updatedTask = task

        changes.forEach { (field, value) ->
            updatedTask = when (field) {
                "title" -> updatedTask.copy(title = value)
                "description" -> updatedTask.copy(description = value)
                "isCompleted" -> updatedTask.copy(isCompleted = value.toBoolean())
                "priority" -> updatedTask.copy(priority = TaskPriority.valueOf(value))
                "assignedTo" -> updatedTask.copy(assignedTo = value.takeIf { it.isNotBlank() })
                "dueDate" -> {
                    try {
                        updatedTask.copy(dueDate = LocalDateTime.parse(value))
                    } catch (e: Exception) {
                        updatedTask
                    }
                }
                else -> updatedTask
            }
        }

        return updatedTask.copy(
            updatedAt = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        )
    }
}