# Guía de Implementación WebSocket - FlowBoard

## 📋 Índice
1. [Resumen de lo Implementado](#resumen-de-lo-implementado)
2. [Integración en Repository](#integración-en-repository)
3. [Integración en ViewModel](#integración-en-viewmodel)
4. [Integración en UI](#integración-en-ui)
5. [Manejo de Conflictos](#manejo-de-conflictos)
6. [Testing](#testing)
7. [Troubleshooting](#troubleshooting)

---

## ✅ Resumen de lo Implementado

### Backend (100% Completo)

| Componente | Ubicación | Estado |
|-----------|-----------|--------|
| Modelos WebSocket | `backend/.../WebSocketMessage.kt` | ✅ |
| WebSocketManager | `backend/.../WebSocketManager.kt` | ✅ |
| WebSocket Plugin | `backend/.../plugins/WebSockets.kt` | ✅ |
| WebSocket Routes | `backend/.../routes/WebSocketRoutes.kt` | ✅ |
| TaskService con eventos | `backend/.../domain/TaskService.kt` | ✅ |
| Autenticación JWT | Integrado en WebSocketRoutes | ✅ |

**Endpoints disponibles:**
- `ws://localhost:8080/ws/boards` - Conexión WebSocket
- `GET /ws/stats` - Estadísticas de debugging

### Android (80% Completo)

| Componente | Ubicación | Estado |
|-----------|-----------|--------|
| Modelos DTO WebSocket | `android/.../dto/WebSocketMessage.kt` | ✅ |
| WebSocketState | `android/.../websocket/WebSocketState.kt` | ✅ |
| TaskWebSocketClient | `android/.../websocket/TaskWebSocketClient.kt` | ✅ |
| NetworkModule | `android/.../di/NetworkModule.kt` | ✅ |
| Repository Integration | - | ⏳ Pendiente |
| ViewModel Integration | - | ⏳ Pendiente |
| UI Components | - | ⏳ Pendiente |

---

## 🔧 Integración en Repository

### Paso 1: Actualizar TaskRepositoryImpl

**Ubicación:** `android/app/src/main/java/com/flowboard/data/repository/TaskRepositoryImpl.kt`

```kotlin
package com.flowboard.data.repository

import com.flowboard.data.local.dao.TaskDao
import com.flowboard.data.local.entities.TaskEntity
import com.flowboard.data.remote.api.TaskApiService
import com.flowboard.data.remote.dto.*
import com.flowboard.data.remote.websocket.TaskWebSocketClient
import com.flowboard.data.remote.websocket.WebSocketState
import com.flowboard.domain.model.Task
import com.flowboard.domain.repository.TaskRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepositoryImpl @Inject constructor(
    private val taskDao: TaskDao,
    private val taskApiService: TaskApiService,
    private val webSocketClient: TaskWebSocketClient
) : TaskRepository {

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        // Escuchar eventos WebSocket y actualizar base de datos local
        repositoryScope.launch {
            webSocketClient.incomingMessages.collect { message ->
                handleWebSocketMessage(message)
            }
        }
    }

    /**
     * Obtiene todas las tareas, combinando datos locales y actualizaciones en tiempo real
     */
    override fun getAllTasks(): Flow<List<Task>> {
        return taskDao.getAllTasks()
            .map { entities ->
                entities.map { it.toDomainModel() }
            }
    }

    /**
     * Conecta al WebSocket para un board específico
     */
    suspend fun connectToBoard(boardId: String, token: String, userId: String) {
        webSocketClient.connect(boardId, token, userId)
    }

    /**
     * Desconecta del WebSocket
     */
    suspend fun disconnectFromBoard() {
        webSocketClient.disconnect()
    }

    /**
     * Obtiene el estado de conexión WebSocket
     */
    fun getConnectionState(): StateFlow<WebSocketState> {
        return webSocketClient.connectionState
    }

    /**
     * Crea una tarea (optimistic update + sync)
     */
    override suspend fun createTask(task: Task): Result<Task> {
        return try {
            // 1. Guardar localmente primero (optimistic update)
            val entity = task.toEntity().copy(isSync = false)
            taskDao.insertTask(entity)

            // 2. Enviar al servidor
            val createdTask = taskApiService.createTask(entity)

            // 3. Actualizar local con datos del servidor
            taskDao.insertTask(createdTask.copy(isSync = true))

            // 4. WebSocket broadcasting será manejado automáticamente por el servidor
            // No necesitamos hacer nada más aquí

            Result.success(createdTask.toDomainModel())
        } catch (e: Exception) {
            // Si falla, la tarea quedará marcada como no sincronizada
            Result.failure(e)
        }
    }

    /**
     * Actualiza una tarea
     */
    override suspend fun updateTask(task: Task): Result<Task> {
        return try {
            // 1. Actualizar localmente (optimistic)
            taskDao.updateTask(task.toEntity().copy(isSync = false))

            // 2. Sincronizar con servidor
            val updatedTask = taskApiService.updateTask(task.id, task.toEntity())

            // 3. Actualizar con datos del servidor
            taskDao.updateTask(updatedTask.copy(isSync = true))

            Result.success(updatedTask.toDomainModel())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Elimina una tarea
     */
    override suspend fun deleteTask(id: String): Result<Unit> {
        return try {
            // 1. Eliminar localmente
            taskDao.deleteTask(id)

            // 2. Eliminar en servidor
            taskApiService.deleteTask(id)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Maneja mensajes WebSocket y actualiza la base de datos local
     */
    private suspend fun handleWebSocketMessage(message: WebSocketMessage) {
        when (message) {
            is TaskCreatedMessage -> {
                // Otra persona creó una tarea
                val task = message.task.toDomainTask()
                taskDao.insertTask(task.toEntity().copy(isSync = true))
            }

            is TaskUpdatedMessage -> {
                // Otra persona actualizó una tarea
                val existingTask = taskDao.getTaskById(message.taskId)
                if (existingTask != null) {
                    val updatedTask = applyChanges(existingTask, message.changes)
                    taskDao.updateTask(updatedTask.copy(isSync = true))
                }
            }

            is TaskDeletedMessage -> {
                // Otra persona eliminó una tarea
                taskDao.deleteTask(message.taskId)
            }

            is TaskMovedMessage -> {
                // Tarea movida a otro board
                // Actualizar projectId
                val task = taskDao.getTaskById(message.taskId)
                if (task != null) {
                    taskDao.updateTask(task.copy(projectId = message.toBoardId))
                }
            }

            // Otros mensajes (USER_JOINED, USER_LEFT, etc.) pueden ser manejados
            // por un StateFlow separado para presencia de usuarios
            else -> { /* Ignorar */ }
        }
    }

    /**
     * Aplica cambios incrementales a una tarea
     */
    private fun applyChanges(task: TaskEntity, changes: Map<String, String>): TaskEntity {
        var updatedTask = task

        changes.forEach { (field, value) ->
            updatedTask = when (field) {
                "title" -> updatedTask.copy(title = value)
                "description" -> updatedTask.copy(description = value)
                "isCompleted" -> updatedTask.copy(isCompleted = value.toBoolean())
                "priority" -> updatedTask.copy(priority = TaskPriority.valueOf(value))
                // Agregar más campos según necesites
                else -> updatedTask
            }
        }

        return updatedTask
    }

    /**
     * Sincroniza tareas no sincronizadas (offline-first)
     */
    override suspend fun syncTasks(): Result<Unit> {
        return try {
            val unsyncedTasks = taskDao.getUnsyncedTasks()

            unsyncedTasks.forEach { task ->
                try {
                    val synced = taskApiService.createTask(task)
                    taskDao.markTaskAsSynced(task.id, Clock.System.now())
                } catch (e: Exception) {
                    // Log error pero continuar con otras tareas
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ... otros métodos del repository ...
}
```

### Paso 2: Añadir Flujo de Presencia de Usuarios (Opcional)

```kotlin
// En TaskRepositoryImpl

private val _activeUsers = MutableStateFlow<List<UserPresenceInfo>>(emptyList())
val activeUsers: StateFlow<List<UserPresenceInfo>> = _activeUsers.asStateFlow()

init {
    repositoryScope.launch {
        webSocketClient.incomingMessages.collect { message ->
            when (message) {
                is RoomJoinedMessage -> {
                    _activeUsers.value = message.activeUsers
                }
                is UserJoinedMessage -> {
                    _activeUsers.value = _activeUsers.value + message.user
                }
                is UserLeftMessage -> {
                    _activeUsers.value = _activeUsers.value.filter { it.userId != message.userId }
                }
                // ... manejar tareas ...
            }
        }
    }
}
```

---

## 🎨 Integración en ViewModel

### Paso 1: Actualizar TaskViewModel

**Ubicación:** `android/app/src/main/java/com/flowboard/presentation/viewmodel/TaskViewModel.kt`

```kotlin
package com.flowboard.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flowboard.data.remote.websocket.WebSocketState
import com.flowboard.data.repository.TaskRepositoryImpl
import com.flowboard.domain.model.Task
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TaskViewModel @Inject constructor(
    private val repository: TaskRepositoryImpl
) : ViewModel() {

    // Estado de conexión WebSocket
    val connectionState: StateFlow<WebSocketState> = repository.getConnectionState()

    // Tareas observables (actualizadas en tiempo real)
    val tasks: StateFlow<List<Task>> = repository.getAllTasks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Usuarios activos en el board
    val activeUsers = repository.activeUsers
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Estado UI
    private val _uiState = MutableStateFlow(TaskUiState())
    val uiState: StateFlow<TaskUiState> = _uiState.asStateFlow()

    /**
     * Conecta al WebSocket para un board específico
     */
    fun connectToBoard(boardId: String, token: String, userId: String) {
        viewModelScope.launch {
            repository.connectToBoard(boardId, token, userId)
        }
    }

    /**
     * Desconecta del WebSocket
     */
    fun disconnectFromBoard() {
        viewModelScope.launch {
            repository.disconnectFromBoard()
        }
    }

    /**
     * Crea una tarea
     */
    fun createTask(task: Task) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            repository.createTask(task)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message
                    )
                }
        }
    }

    /**
     * Actualiza una tarea
     */
    fun updateTask(task: Task) {
        viewModelScope.launch {
            repository.updateTask(task)
        }
    }

    /**
     * Elimina una tarea
     */
    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            repository.deleteTask(taskId)
        }
    }

    /**
     * Toggle estado completado
     */
    fun toggleTaskStatus(taskId: String) {
        viewModelScope.launch {
            val task = tasks.value.find { it.id == taskId }
            task?.let {
                updateTask(it.copy(isCompleted = !it.isCompleted))
            }
        }
    }

    /**
     * Reconectar manualmente al WebSocket
     */
    fun reconnect() {
        viewModelScope.launch {
            // Implementar lógica de reconexión
            // Necesitarás guardar boardId, token, userId en ViewModel o recuperarlos
        }
    }
}

data class TaskUiState(
    val isLoading: Boolean = false,
    val error: String? = null
)
```

---

## 🖼️ Integración en UI

### Paso 1: Mostrar Estado de Conexión

**Ubicación:** `android/app/src/main/java/com/flowboard/presentation/ui/components/ConnectionStatusBanner.kt`

```kotlin
package com.flowboard.presentation.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.flowboard.data.remote.websocket.WebSocketState

@Composable
fun ConnectionStatusBanner(
    connectionState: WebSocketState,
    onReconnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = connectionState !is WebSocketState.Connected,
        enter = slideInVertically() + fadeIn(),
        exit = slideOutVertically() + fadeOut()
    ) {
        when (connectionState) {
            is WebSocketState.Connecting -> {
                StatusBanner(
                    message = "Conectando...",
                    icon = Icons.Default.CloudSync,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            is WebSocketState.Reconnecting -> {
                StatusBanner(
                    message = "Reconectando (${connectionState.attempt}/${connectionState.maxAttempts})...",
                    icon = Icons.Default.CloudSync,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            is WebSocketState.Disconnected -> {
                StatusBanner(
                    message = "Sin conexión",
                    icon = Icons.Default.CloudOff,
                    color = MaterialTheme.colorScheme.error,
                    action = {
                        TextButton(onClick = onReconnect) {
                            Text("Reconectar")
                        }
                    }
                )
            }

            is WebSocketState.Error -> {
                StatusBanner(
                    message = "Error: ${connectionState.message}",
                    icon = Icons.Default.Error,
                    color = MaterialTheme.colorScheme.error,
                    action = if (connectionState.isRecoverable) {
                        {
                            TextButton(onClick = onReconnect) {
                                Text("Reintentar")
                            }
                        }
                    } else null
                )
            }

            else -> { /* Connected - no mostrar */ }
        }
    }
}

@Composable
private fun StatusBanner(
    message: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    action: @Composable (() -> Unit)? = null
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = color.copy(alpha = 0.9f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }

            action?.invoke()
        }
    }
}
```

### Paso 2: Mostrar Usuarios Activos

```kotlin
@Composable
fun ActiveUsersList(
    users: List<UserPresenceInfo>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy((-8).dp)
    ) {
        users.take(5).forEach { user ->
            UserAvatar(user = user)
        }

        if (users.size > 5) {
            Text(
                text = "+${users.size - 5}",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = CircleShape
                    )
                    .padding(8.dp)
            )
        }
    }
}

@Composable
fun UserAvatar(user: UserPresenceInfo) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(
                color = MaterialTheme.colorScheme.primary,
                shape = CircleShape
            )
            .padding(2.dp)
    ) {
        // Si hay imagen de perfil, usar AsyncImage de Coil
        // Caso contrario, mostrar iniciales
        Text(
            text = user.fullName.take(2).uppercase(),
            color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.align(Alignment.Center)
        )

        // Indicador de online
        if (user.isOnline) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(Color.Green, CircleShape)
                    .align(Alignment.BottomEnd)
            )
        }
    }
}
```

### Paso 3: Integrar en TaskListScreen

```kotlin
@Composable
fun TaskListScreen(
    viewModel: TaskViewModel = hiltViewModel(),
    boardId: String,
    token: String,
    userId: String
) {
    val tasks by viewModel.tasks.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val activeUsers by viewModel.activeUsers.collectAsState()

    // Conectar al board cuando la pantalla se monta
    LaunchedEffect(boardId) {
        viewModel.connectToBoard(boardId, token, userId)
    }

    // Desconectar cuando la pantalla se desmonta
    DisposableEffect(Unit) {
        onDispose {
            viewModel.disconnectFromBoard()
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("FlowBoard") },
                    actions = {
                        ActiveUsersList(users = activeUsers)
                    }
                )

                // Banner de estado de conexión
                ConnectionStatusBanner(
                    connectionState = connectionState,
                    onReconnect = { viewModel.reconnect() }
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            items(tasks) { task ->
                TaskCard(
                    task = task,
                    onToggleComplete = { viewModel.toggleTaskStatus(task.id) },
                    onDelete = { viewModel.deleteTask(task.id) }
                )
            }
        }
    }
}
```

---

## ⚔️ Manejo de Conflictos

### Estrategia: Last-Write-Wins con Timestamps

```kotlin
// En TaskRepositoryImpl

private suspend fun handleConflict(
    localTask: TaskEntity,
    serverTask: TaskEntity
): TaskEntity {
    return if (localTask.updatedAt > serverTask.updatedAt) {
        // Local es más reciente, enviar al servidor
        try {
            val updated = taskApiService.updateTask(localTask.id, localTask)
            updated
        } catch (e: Exception) {
            // Si falla, mantener local
            localTask
        }
    } else {
        // Servidor es más reciente, aceptar cambios
        serverTask
    }
}
```

### Resolución Manual de Conflictos

```kotlin
@Composable
fun ConflictResolutionDialog(
    localTask: Task,
    serverTask: Task,
    onResolve: (Task) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Conflicto Detectado") },
        text = {
            Column {
                Text("Esta tarea fue modificada por otro usuario mientras estabas offline.")
                Spacer(modifier = Modifier.height(16.dp))

                Text("Versión Local:", style = MaterialTheme.typography.labelMedium)
                TaskPreview(task = localTask)

                Spacer(modifier = Modifier.height(8.dp))

                Text("Versión del Servidor:", style = MaterialTheme.typography.labelMedium)
                TaskPreview(task = serverTask)
            }
        },
        confirmButton = {
            TextButton(onClick = { onResolve(localTask) }) {
                Text("Usar Mía")
            }
        },
        dismissButton = {
            TextButton(onClick = { onResolve(serverTask) }) {
                Text("Usar del Servidor")
            }
        }
    )
}
```

---

## 🧪 Testing

### Backend Tests

**Ubicación:** `backend/src/test/kotlin/com/flowboard/WebSocketTest.kt`

```kotlin
class WebSocketTest {

    @Test
    fun `test WebSocketManager handles multiple users in same room`() = runTest {
        val manager = WebSocketManager()

        val session1 = mockWebSocketSession()
        val session2 = mockWebSocketSession()

        val user1 = UserPresenceInfo(...)
        val user2 = UserPresenceInfo(...)

        manager.joinRoom(session1, "board123", user1)
        manager.joinRoom(session2, "board123", user2)

        assertEquals(2, manager.getRoomSize("board123"))
        assertEquals(2, manager.getActiveUsersInRoom("board123").size)
    }

    @Test
    fun `test broadcast excludes sender`() = runTest {
        val manager = WebSocketManager()

        val session1 = mockWebSocketSession()
        val session2 = mockWebSocketSession()

        manager.joinRoom(session1, "board123", user1)
        manager.joinRoom(session2, "board123", user2)

        manager.broadcastToRoomExcept(
            boardId = "board123",
            exceptSession = session1,
            message = TaskCreatedMessage(...)
        )

        // Verificar que session2 recibió el mensaje pero session1 no
        verify(session2).send(any())
        verify(session1, never()).send(any())
    }
}
```

### Android Tests

**Ubicación:** `android/app/src/test/kotlin/com/flowboard/TaskWebSocketClientTest.kt`

```kotlin
class TaskWebSocketClientTest {

    @Test
    fun `test connection state changes`() = runTest {
        val client = TaskWebSocketClient(mockHttpClient())

        val states = mutableListOf<WebSocketState>()
        val job = launch {
            client.connectionState.collect { states.add(it) }
        }

        client.connect("board123", "token", "user123")

        delay(1000)

        // Verificar transiciones de estado
        assertEquals(WebSocketState.Disconnected, states[0])
        assertEquals(WebSocketState.Connecting, states[1])
        assertTrue(states.last() is WebSocketState.Connected)

        job.cancel()
    }

    @Test
    fun `test automatic reconnection on disconnect`() = runTest {
        // Implementar test de reconexión automática
    }
}
```

---

## 🔍 Troubleshooting

### Problema: WebSocket no conecta

**Diagnóstico:**
1. Verificar que el backend esté corriendo en `localhost:8080`
2. Para Android emulator, usar `10.0.2.2` en lugar de `localhost`
3. Verificar logs de autenticación JWT

**Solución:**
```kotlin
// En TaskWebSocketClient.kt
private const val WS_URL = "ws://10.0.2.2:8080/ws/boards" // Emulator
// private const val WS_URL = "ws://192.168.1.X:8080/ws/boards" // Dispositivo físico
```

### Problema: Mensajes no se reciben

**Diagnóstico:**
1. Verificar que el cliente envió `JOIN_ROOM`
2. Verificar que el `boardId` es correcto
3. Verificar logs del backend

**Solución:**
- Revisar `WebSocketRoutes.kt` logs
- Verificar que `WebSocketManager` tiene la sesión registrada

### Problema: Reconexión fallida

**Diagnóstico:**
1. Verificar que el token JWT no haya expirado
2. Verificar conectividad de red

**Solución:**
- Implementar refresh de token automático
- Aumentar `MAX_RECONNECT_ATTEMPTS`

---

## 📚 Recursos Adicionales

- [Documentación Ktor WebSockets](https://ktor.io/docs/websocket.html)
- [Documentación Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html)
- [Clean Architecture](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)

---

**Versión:** 1.0.0
**Última actualización:** 2025-11-25
**Próximos pasos:**
1. Integrar en Repository ✅ (guía proporcionada)
2. Integrar en ViewModel ✅ (guía proporcionada)
3. Crear componentes UI ✅ (guía proporcionada)
4. Implementar tests ✅ (guía proporcionada)
5. Deployment a producción ⏳
