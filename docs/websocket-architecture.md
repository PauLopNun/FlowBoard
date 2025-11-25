# Arquitectura WebSocket - FlowBoard Real-Time Collaboration

## Índice
1. [Visión General](#visión-general)
2. [Componentes del Sistema](#componentes-del-sistema)
3. [Flujos de Comunicación](#flujos-de-comunicación)
4. [Estrategia de Sincronización](#estrategia-de-sincronización)
5. [Manejo de Conflictos](#manejo-de-conflictos)
6. [Reconexión y Resiliencia](#reconexión-y-resiliencia)
7. [Seguridad](#seguridad)
8. [Escalabilidad](#escalabilidad)

---

## Visión General

FlowBoard implementa colaboración en tiempo real usando **WebSockets** para permitir que múltiples usuarios trabajen simultáneamente en el mismo tablero de tareas con sincronización instantánea.

### Características Principales

- ✅ **Sincronización en tiempo real**: Cambios visibles inmediatamente para todos los usuarios
- ✅ **Presencia de usuarios**: Ver quién está conectado y activo en cada board
- ✅ **Indicadores de escritura**: Ver cuando alguien está editando una tarea
- ✅ **Offline-first**: Funciona sin conexión, sincroniza al reconectar
- ✅ **Reconexión automática**: Manejo robusto de desconexiones
- ✅ **Autenticación segura**: JWT sobre WebSocket
- ✅ **Rooms por board**: Aislamiento de eventos por tablero

### Stack Tecnológico

**Backend:**
- Ktor Server 2.3.7 con WebSockets plugin
- Kotlin Coroutines para manejo asíncrono
- kotlinx-serialization para JSON
- JWT para autenticación

**Android:**
- Ktor Client 2.3.7 con WebSockets
- Flow/StateFlow para streams reactivos
- Room para caché local
- Hilt para inyección de dependencias

---

## Componentes del Sistema

### Backend Components

```
backend/
├── routes/
│   └── WebSocketRoutes.kt         # Endpoint /ws/boards/{boardId}
├── services/
│   ├── WebSocketManager.kt        # Gestión de rooms y broadcasting
│   └── TaskService.kt             # Lógica de negocio + eventos WS
├── data/models/
│   └── WebSocketMessage.kt        # DTOs de mensajes
└── plugins/
    └── WebSockets.kt              # Configuración WebSocket plugin
```

#### 1. WebSocketManager (Singleton)

**Responsabilidades:**
- Mantener registro de sesiones WebSocket activas
- Gestionar rooms (board ID → sesiones)
- Broadcasting de mensajes a usuarios específicos
- Tracking de presencia de usuarios

**Estructura de datos:**
```kotlin
class WebSocketManager {
    // Board ID → Set de sesiones conectadas
    private val rooms = ConcurrentHashMap<String, MutableSet<WebSocketSession>>()

    // Session → información de usuario
    private val userSessions = ConcurrentHashMap<WebSocketSession, UserSessionInfo>()

    // User ID → Set de sesiones (multi-device)
    private val userConnections = ConcurrentHashMap<String, MutableSet<WebSocketSession>>()
}
```

#### 2. TaskService

**Modificado para emitir eventos:**
```kotlin
suspend fun createTask(task: Task): Result<Task> {
    // 1. Validar
    // 2. Guardar en DB
    val saved = taskRepository.save(task)

    // 3. Emitir evento WebSocket
    webSocketManager.broadcastToRoom(
        boardId = task.projectId,
        message = TaskCreatedMessage(...)
    )

    return Result.success(saved)
}
```

### Android Components

```
android/app/src/main/java/com/flowboard/
├── data/
│   ├── remote/
│   │   ├── websocket/
│   │   │   ├── TaskWebSocketClient.kt    # Cliente WebSocket
│   │   │   └── WebSocketState.kt         # Estados de conexión
│   │   └── dto/
│   │       └── WebSocketMessage.kt       # DTOs (idénticos a backend)
│   └── repository/
│       └── TaskRepositoryImpl.kt         # Integra WS + HTTP + Room
├── domain/
│   └── repository/
│       └── TaskRepository.kt             # Interface con Flow<Task>
└── presentation/
    └── viewmodel/
        └── TaskViewModel.kt              # Observa cambios en tiempo real
```

#### 1. TaskWebSocketClient

**Responsabilidades:**
- Establecer/mantener conexión WebSocket
- Enviar mensajes al servidor
- Recibir y parsear mensajes entrantes
- Manejo de reconexión automática
- Emitir eventos como Flow

**API:**
```kotlin
class TaskWebSocketClient {
    // Estado de conexión
    val connectionState: StateFlow<WebSocketState>

    // Stream de mensajes recibidos
    val incomingMessages: Flow<WebSocketMessage>

    // Métodos
    suspend fun connect(boardId: String)
    suspend fun disconnect()
    suspend fun send(message: WebSocketMessage)
}
```

#### 2. TaskRepositoryImpl (Actualizado)

**Fusiona 3 fuentes de datos:**
```kotlin
class TaskRepositoryImpl(
    private val localDataSource: TaskDao,
    private val remoteDataSource: TaskApiService,
    private val webSocketClient: TaskWebSocketClient
) : TaskRepository {

    override fun getAllTasks(): Flow<List<Task>> = combine(
        localDataSource.getAllTasks(),        // Room DB
        webSocketClient.incomingMessages      // WebSocket updates
    ) { localTasks, wsMessage ->
        when (wsMessage) {
            is TaskCreatedMessage -> localTasks + wsMessage.task
            is TaskUpdatedMessage -> localTasks.map {
                if (it.id == wsMessage.taskId) it.applyChanges(wsMessage.changes)
                else it
            }
            is TaskDeletedMessage -> localTasks.filter { it.id != wsMessage.taskId }
            else -> localTasks
        }
    }
}
```

---

## Flujos de Comunicación

### 1. Conexión Inicial

```
┌─────────┐                    ┌─────────┐
│ Android │                    │  Ktor   │
│ Client  │                    │ Server  │
└────┬────┘                    └────┬────┘
     │                              │
     │ 1. HTTP GET /api/v1/boards  │
     │────────────────────────────→│
     │                              │
     │ 2. Boards data + metadata   │
     │←────────────────────────────│
     │                              │
     │ 3. WS Connect /ws/boards    │
     │    Authorization: Bearer JWT│
     │────────────────────────────→│
     │                              │
     │ 4. Upgrade to WebSocket     │
     │←────────────────────────────│
     │                              │
     │ 5. Send JoinRoomMessage     │
     │    {boardId: "123"}         │
     │────────────────────────────→│
     │                              │
     │ 6. RoomJoinedMessage        │
     │    {activeUsers: [...]}     │
     │←────────────────────────────│
```

### 2. Sincronización de Cambios

**Escenario: Usuario A actualiza una tarea**

```
User A                Server              User B              User C
  │                     │                   │                   │
  │ 1. Update Task      │                   │                   │
  │    (local first)    │                   │                   │
  │─────────────────────│                   │                   │
  │                     │                   │                   │
  │ 2. HTTP PATCH       │                   │                   │
  │────────────────────→│                   │                   │
  │                     │ 3. Save to DB     │                   │
  │                     │                   │                   │
  │                     │ 4. Broadcast WS   │                   │
  │                     │──────────────────→│                   │
  │                     │──────────────────────────────────────→│
  │                     │                   │                   │
  │ 5. WS Confirmation  │                   │ 6. Update local   │
  │←────────────────────│                   │    & UI refresh   │
  │                     │                   │                   │
```

### 3. Manejo de Presencia

```
User A                Server              User B
  │                     │                   │
  │ JOIN_ROOM           │                   │
  │────────────────────→│                   │
  │                     │                   │
  │                     │ USER_JOINED       │
  │                     │──────────────────→│
  │                     │                   │
  │ TYPING_INDICATOR    │                   │
  │    {taskId: "xyz"}  │                   │
  │────────────────────→│                   │
  │                     │                   │
  │                     │ USER_TYPING       │
  │                     │──────────────────→│
  │                     │                   │
```

---

## Estrategia de Sincronización

### Patrón: Optimistic UI + Eventually Consistent

#### Fase 1: Cambio Local (Instantáneo)

```kotlin
// ViewModel
fun updateTask(task: Task) {
    viewModelScope.launch {
        // 1. Actualizar UI inmediatamente (optimistic)
        _tasks.value = _tasks.value.map {
            if (it.id == task.id) task else it
        }

        // 2. Guardar en Room
        repository.updateTaskLocal(task)

        // 3. Intentar sincronizar con servidor
        repository.updateTaskRemote(task)
            .onFailure {
                // Marcar como pendiente de sincronización
                repository.markAsUnsynced(task.id)
            }
    }
}
```

#### Fase 2: Sincronización Servidor

```kotlin
// TaskService (Backend)
suspend fun updateTask(id: String, updates: TaskUpdate): Task {
    // 1. Validar permisos
    // 2. Actualizar en PostgreSQL
    val updated = database.update(id, updates)

    // 3. Broadcast a todos excepto el autor
    webSocketManager.broadcastToRoomExcept(
        boardId = updated.projectId,
        exceptUserId = updates.updatedBy,
        message = TaskUpdatedMessage(...)
    )

    return updated
}
```

#### Fase 3: Recepción en Otros Clientes

```kotlin
// TaskRepositoryImpl
init {
    webSocketClient.incomingMessages
        .filterIsInstance<TaskUpdatedMessage>()
        .onEach { message ->
            // Actualizar Room DB
            taskDao.update(message.taskId, message.changes)

            // Flow automáticamente emite nuevo estado a UI
        }
        .launchIn(scope)
}
```

### Estrategia Offline-First

```kotlin
┌────────────────────────────────────────────┐
│         ANDROID CLIENT (Offline)           │
├────────────────────────────────────────────┤
│                                            │
│  1. User creates task                     │
│     ↓                                      │
│  2. Save to Room with isSync=false        │
│     ↓                                      │
│  3. Show in UI immediately                │
│     ↓                                      │
│  4. Queue for sync (WorkManager)          │
│                                            │
└────────────────────────────────────────────┘
            ↓
       (Reconnect)
            ↓
┌────────────────────────────────────────────┐
│  5. Detect online status                  │
│     ↓                                      │
│  6. Fetch unsynced tasks from Room        │
│     ↓                                      │
│  7. POST to server sequentially           │
│     ↓                                      │
│  8. Update local isSync=true              │
│                                            │
└────────────────────────────────────────────┘
```

---

## Manejo de Conflictos

### Estrategia: Last-Write-Wins con Timestamps

#### Conflicto: Dos usuarios editan la misma tarea offline

```
Scenario:
- User A (offline): Actualiza título a "Fix bug" @ T1
- User B (offline): Actualiza título a "Resolve issue" @ T2
- Ambos se conectan y sincronizan
```

**Resolución:**
```kotlin
// Backend
suspend fun resolveConflict(
    serverTask: Task,
    clientTask: Task
): Task {
    // Comparar timestamps
    return if (clientTask.updatedAt > serverTask.updatedAt) {
        // Cliente gana
        database.update(clientTask)
        clientTask
    } else {
        // Servidor gana, notificar al cliente
        serverTask
    }
}
```

**Cliente Android:**
```kotlin
// Si servidor rechaza por conflicto
webSocketClient.incomingMessages
    .filterIsInstance<TaskUpdatedMessage>()
    .filter { it.taskId in localPendingUpdates }
    .onEach { serverVersion ->
        // Mostrar diálogo al usuario
        showConflictDialog(
            localVersion = getLocalTask(serverVersion.taskId),
            serverVersion = serverVersion.task,
            onResolve = { chosenVersion ->
                if (chosenVersion == local) {
                    // Re-intentar enviar
                    repository.forceUpdateTask(localVersion)
                } else {
                    // Aceptar versión del servidor
                    repository.acceptServerVersion(serverVersion)
                }
            }
        )
    }
```

### Campos con Estrategias Diferentes

| Campo | Estrategia |
|-------|-----------|
| `title`, `description` | Last-Write-Wins |
| `isCompleted` | Last-Write-Wins |
| `tags` | Merge (union de sets) |
| `attachments` | Merge (union) |
| `assignedTo` | Last-Write-Wins |
| `dueDate` | Last-Write-Wins |

---

## Reconexión y Resiliencia

### Finite State Machine para Conexión

```
┌──────────────┐
│ DISCONNECTED │
└──────┬───────┘
       │ connect()
       ▼
┌──────────────┐
│  CONNECTING  │
└──┬───────┬───┘
   │       │ onFailure
   │       ▼
   │  ┌──────────────┐
   │  │  RECONNECTING│←───────┐
   │  └──────┬───────┘        │
   │         │ retry           │
   │         └─────────────────┘
   │ onSuccess
   ▼
┌──────────────┐
│  CONNECTED   │
└──────┬───────┘
       │ onDisconnect
       ▼
┌──────────────┐
│ DISCONNECTED │
└──────────────┘
```

### Implementación Reconexión

```kotlin
class TaskWebSocketClient {
    private val reconnectPolicy = ExponentialBackoff(
        initialDelay = 1000L,      // 1 segundo
        maxDelay = 30000L,         // 30 segundos
        factor = 2.0,
        maxAttempts = 10
    )

    suspend fun connectWithRetry(boardId: String) {
        reconnectPolicy.retry {
            try {
                connect(boardId)
                _connectionState.value = WebSocketState.Connected
            } catch (e: Exception) {
                _connectionState.value = WebSocketState.Reconnecting(
                    attempt = it.attemptNumber
                )
                throw e
            }
        }
    }
}
```

### Heartbeat / Ping-Pong

```kotlin
// Android Client
launch {
    while (isActive) {
        delay(30_000) // cada 30 segundos
        send(PingMessage(timestamp = Clock.System.now()))

        // Esperar pong en 5 segundos
        withTimeoutOrNull(5_000) {
            incoming.filterIsInstance<PongMessage>().first()
        } ?: run {
            // No response, reconectar
            reconnect()
        }
    }
}
```

---

## Seguridad

### Autenticación JWT sobre WebSocket

#### Paso 1: Login HTTP
```kotlin
// Android
POST /api/v1/auth/login
{
    "email": "user@example.com",
    "password": "password"
}

Response:
{
    "token": "eyJhbGc...",
    "refreshToken": "...",
    "expiresIn": 3600
}
```

#### Paso 2: WebSocket con JWT
```kotlin
// Android Client
client.webSocket(
    urlString = "ws://server/ws/boards",
    request = {
        header("Authorization", "Bearer $jwtToken")
    }
) {
    // Connected
}
```

#### Paso 3: Validación en Backend
```kotlin
// Backend WebSocketRoutes
webSocket("/ws/boards") {
    // Extraer token
    val token = call.request.headers["Authorization"]
        ?.removePrefix("Bearer ")
        ?: return@webSocket close(
            CloseReason(CloseReason.Codes.VIOLATED_POLICY, "No token")
        )

    // Validar JWT
    val userId = jwtConfig.validateToken(token)
        ?: return@webSocket close(
            CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Invalid token")
        )

    // Proceder con conexión
    webSocketManager.registerSession(this, userId)
}
```

### Autorización por Board

```kotlin
// Backend
suspend fun canAccessBoard(userId: String, boardId: String): Boolean {
    val board = boardRepository.findById(boardId)
    return board.ownerId == userId || userId in board.members
}

webSocket("/ws/boards/{boardId}") {
    val boardId = call.parameters["boardId"]!!
    val userId = extractUserId(call)

    if (!canAccessBoard(userId, boardId)) {
        close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Access denied"))
        return@webSocket
    }

    // Proceder
}
```

---

## Escalabilidad

### Arquitectura Multi-Instancia (Futuro)

```
┌─────────┐     ┌─────────┐     ┌─────────┐
│Client A │────→│ Ktor 1  │     │ Ktor 2  │←────│Client B │
└─────────┘     └────┬────┘     └────┬────┘     └─────────┘
                     │               │
                     ▼               ▼
                ┌────────────────────────┐
                │   Redis Pub/Sub        │
                │  (Message Broker)      │
                └────────────────────────┘
                          │
                          ▼
                ┌────────────────────┐
                │    PostgreSQL      │
                └────────────────────┘
```

**Con Redis Pub/Sub:**
```kotlin
// TaskService (Backend)
suspend fun createTask(task: Task) {
    database.save(task)

    // Publicar a Redis
    redisClient.publish(
        channel = "board:${task.projectId}",
        message = json.encodeToString(TaskCreatedMessage(...))
    )
}

// Cada instancia Ktor suscribe
init {
    redisClient.subscribe("board:*") { channel, message ->
        val boardId = channel.removePrefix("board:")
        val event = json.decodeFromString<WebSocketMessage>(message)

        // Broadcast solo a sesiones locales de esta instancia
        webSocketManager.broadcastToRoom(boardId, event)
    }
}
```

### Métricas de Rendimiento

| Métrica | Target | Notas |
|---------|--------|-------|
| Latencia mensaje | < 100ms | P95 |
| Conexiones concurrentes | > 10,000 | Por instancia |
| Throughput mensajes | > 1,000 msg/s | Por instancia |
| Tiempo reconexión | < 3s | P99 |
| Uso memoria | < 2GB | Por instancia |

---

## Testing

### Backend Tests

```kotlin
@Test
fun `test broadcast to room excludes sender`() = testApplication {
    val manager = WebSocketManager()

    // Setup
    val session1 = mockSession("user1")
    val session2 = mockSession("user2")

    manager.joinRoom(session1, "board123", "user1")
    manager.joinRoom(session2, "board123", "user2")

    // Act
    manager.broadcastToRoomExcept(
        boardId = "board123",
        exceptSession = session1,
        message = TaskCreatedMessage(...)
    )

    // Assert
    verify(session2).send(any())
    verify(session1, never()).send(any())
}
```

### Android Tests

```kotlin
@Test
fun `repository merges local and websocket updates`() = runTest {
    // Arrange
    val localTasks = flowOf(listOf(task1, task2))
    val wsMessages = flowOf(TaskUpdatedMessage(...))

    whenever(taskDao.getAllTasks()).thenReturn(localTasks)
    whenever(webSocketClient.incomingMessages).thenReturn(wsMessages)

    // Act
    val result = repository.getAllTasks().first()

    // Assert
    assertTrue(result.any { it.id == updatedTaskId })
    assertEquals("Updated Title", result.find { it.id == updatedTaskId }?.title)
}
```

---

## Referencias

- [Ktor WebSockets Documentation](https://ktor.io/docs/websocket.html)
- [Ktor Client WebSockets](https://ktor.io/docs/websocket-client.html)
- [kotlinx-serialization](https://github.com/Kotlin/kotlinx.serialization)
- [Room Database](https://developer.android.com/training/data-storage/room)

---

**Versión:** 1.0.0
**Última actualización:** 2025-11-25
**Autor:** FlowBoard Team
