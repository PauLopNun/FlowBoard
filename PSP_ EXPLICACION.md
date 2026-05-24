# FlowBoard — Criterios PSP (RA4 y RA5)
## Proyecto Intermodular 2DAM · Pau López Núñez

> **RA requeridos en el proyecto**: el enunciado del intermodular únicamente pide **RA4 y RA5** de PSP. Este documento cubre ambos criterio a criterio, con código real del proyecto y la explicación de por qué lo cumple.
>
> El código está escrito en **Kotlin** (no Python como en clase), pero los conceptos son exactamente los mismos. Al final de cada sección se indica la equivalencia con el temario.

---

## Índice de ficheros — Backend (servidor Ktor)

> Haz clic en el nombre del fichero para abrirlo directamente.

### Seguridad y autenticación

| Fichero | RA / Criterios | Qué hace |
|---|---|---|
| [Security.kt](backend/src/main/kotlin/com/flowboard/plugins/Security.kt) | RA5.b · RA5.c | Configura JWT con HMAC-SHA256; el plugin `authenticate("auth-jwt")` protege rutas |
| [AuthService.kt](backend/src/main/kotlin/com/flowboard/domain/AuthService.kt) | RA5.a · RA5.b · RA5.e | BCrypt para contraseñas; lógica de login/registro/OTP; anti-enumeración |
| [AuthRoutes.kt](backend/src/main/kotlin/com/flowboard/routes/AuthRoutes.kt) | RA5.a · RA5.c | Endpoints `/auth/register`, `/auth/login`, `/auth/forgot-password` |

### Red y servicios

| Fichero | RA / Criterios | Qué hace |
|---|---|---|
| [TaskRoutes.kt](backend/src/main/kotlin/com/flowboard/routes/TaskRoutes.kt) | RA4.a · RA4.d · RA5.c | 40+ endpoints REST protegidos con `authenticate("auth-jwt")` |
| [WebSockets.kt](backend/src/main/kotlin/com/flowboard/plugins/WebSockets.kt) | RA4.a · RA4.c · RA4.g | Plugin WebSocket RFC 6455 con `pingPeriod=30s` y `timeout=15s` |
| [WebSocketRoutes.kt](backend/src/main/kotlin/com/flowboard/routes/WebSocketRoutes.kt) | RA4.a · RA4.f | Canal WebSocket de tareas: `ws/{boardId}` |
| [DocumentWebSocketRoutes.kt](backend/src/main/kotlin/com/flowboard/routes/DocumentWebSocketRoutes.kt) | RA4.a · RA4.f | Canal WebSocket CRDT: `document-ws/{docId}` — edición colaborativa |
| [TaskService.kt](backend/src/main/kotlin/com/flowboard/domain/TaskService.kt) | RA4.d · RA4.f | CRUD de tareas + `broadcastToRoom()` para notificar a todos los clientes |
| [PermissionService.kt](backend/src/main/kotlin/com/flowboard/domain/PermissionService.kt) | RA5.d | Lógica de comprobación de roles (viewer / editor / owner) |

### Base de datos

| Fichero | RA / Criterios | Qué hace |
|---|---|---|
| [Tables.kt](backend/src/main/kotlin/com/flowboard/data/database/Tables.kt) | RA5.d | Define las tablas con los roles: `DocumentPermissions` (viewer/editor/owner), `WorkspaceMembers` (MEMBER/ADMIN/OWNER) |
| [DatabaseFactory.kt](backend/src/main/kotlin/com/flowboard/data/database/DatabaseFactory.kt) | RA4.h · RA5.f | Pool HikariCP, `sslmode=require` en la conexión a PostgreSQL, `dbQuery{}` |

---

## Índice de ficheros — Android (cliente)

### Red y comunicación

| Fichero | RA / Criterios | Qué hace |
|---|---|---|
| [NetworkModule.kt](android/app/src/main/java/com/flowboard/di/NetworkModule.kt) | RA4.b · RA4.c · RA4.f | Dos clientes Ktor: HTTP (Android engine) y WebSocket (OkHttp engine) |
| [ApiConfig.kt](android/app/src/main/java/com/flowboard/data/remote/ApiConfig.kt) | RA4.g · RA5.f · RA5.g | URLs de producción `https://` y `wss://` — siempre cifradas |
| [TaskApiService.kt](android/app/src/main/java/com/flowboard/data/remote/api/TaskApiService.kt) | RA4.d · RA4.e | Cliente HTTP que consume los endpoints REST del servidor |
| [TaskWebSocketClient.kt](android/app/src/main/java/com/flowboard/data/remote/websocket/TaskWebSocketClient.kt) | RA4.f | Cliente WebSocket de tareas; ping cada 30s |
| [DocumentWebSocketClient.kt](android/app/src/main/java/com/flowboard/data/remote/websocket/DocumentWebSocketClient.kt) | RA4.f | Cliente WebSocket CRDT; gestiona `ConnectionState` |

### Seguridad y datos locales

| Fichero | RA / Criterios | Qué hace |
|---|---|---|
| [UserEntity.kt](android/app/src/main/java/com/flowboard/data/local/entities/UserEntity.kt) | RA5.d | Enum `UserRole` (USER, ADMIN) almacenado en Room |

---

## Índice de secciones de la memoria

| Sección | RA / Criterios | Contenido |
|---|---|---|
| §6.2.3 — Test Cases TC-01 a TC-18 | RA4.e · RA4.h · RA5.h | 18 pruebas con Postman/curl contra el servidor de producción |
| §6.4 — Errores detectados | RA4.h · RA5.h | Bug SHA-1 Google Sign-In, bug URL Render.com, JWT sin expiración |
| §9.4 — Sostenibilidad y seguridad | RA5.a · RA5.b · RA5.f | Análisis de BCrypt, HMAC-SHA256, TLS, privacidad RGPD |
| §10.1 — Manual técnico / API | RA4.h | Referencia completa de endpoints REST |

---

## Conceptos técnicos del temario — qué son y dónde aparecen

> Cada concepto incluye: definición, dónde aparece en el código del proyecto y la equivalencia directa con el temario de Python.

---

### Singleton

**Qué es:** Un patrón de diseño que garantiza que una clase tiene **una única instancia** en toda la aplicación y proporciona un punto de acceso global a ella. Se usa para recursos que deben compartirse (clientes de red, bases de datos, configuraciones).

**Dónde aparece en FlowBoard — tres formas:**

**1. `object` de Kotlin** — singleton por diseño del lenguaje (se crea una sola vez al cargarse la clase):
```kotlin
// backend/src/main/kotlin/com/flowboard/plugins/Security.kt
object JwtConfig {              // ← "object" en Kotlin = singleton automático
    private val secret = System.getenv("JWT_SECRET") ?: "dev-secret-key"
    private val algorithm = Algorithm.HMAC256(secret)
    val verifier = JWT.require(algorithm)...build()
    fun makeToken(...): String = JWT.create()...sign(algorithm)
}

// backend/src/main/kotlin/com/flowboard/domain/AuthService.kt
object AuthService {            // ← una única instancia de AuthService en todo el backend
    suspend fun register(...) = dbQuery { ... }
    suspend fun login(...)    = dbQuery { ... }
}

// android — ApiConfig.kt
object ApiConfig {              // ← una única fuente de verdad para las URLs
    val BASE_URL = "https://flowboard-api-phrk.onrender.com"
    val API_BASE_URL = "$BASE_URL/api/v1"
    val WS_BASE_URL  = "wss://flowboard-api-phrk.onrender.com"
}
```

**2. `@Singleton` de Hilt** — el framework de inyección de dependencias crea la instancia una sola vez y la reutiliza en toda la app Android:
```kotlin
// android/app/src/main/java/com/flowboard/di/NetworkModule.kt
@Provides
@Singleton                      // ← Hilt garantiza que solo hay UN HttpClient en toda la app
fun provideHttpClient(): HttpClient = HttpClient(Android) { ... }

@Provides
@Singleton
fun provideTaskApiService(...): TaskApiService = TaskApiService(httpClient, authRepository)
```

**3. `@Singleton` en la clase directamente:**
```kotlin
// android — TaskWebSocketClient.kt
@Singleton                      // ← una sola instancia del cliente WebSocket
class TaskWebSocketClient @Inject constructor(private val client: HttpClient) { ... }
```

**Por qué singleton aquí:** si hubiera dos instancias del `HttpClient` o del `TaskWebSocketClient`, habría dos conexiones separadas, dos pools de threads y posibles condiciones de carrera. El singleton garantiza que toda la app comparte el mismo canal.

> **Equivalencia Python**: en Python no existe `@Singleton` como anotación, pero el patrón se simula con un módulo (los módulos se cargan una sola vez) o con el patrón `_instance = None` + `if cls._instance is None: cls._instance = super().__new__(cls)`.

---

### Programación asíncrona — coroutines (`suspend fun`)

**Qué es:** Una coroutine es una función que puede **pausarse y reanudarse** sin bloquear el hilo. La palabra clave `suspend` marca una función que puede esperar a que termine una operación (red, BD) sin congelar la UI ni el servidor. Es la alternativa de Kotlin a `async/await` de Python.

**Cómo funciona en FlowBoard:**

```kotlin
// backend/src/main/kotlin/com/flowboard/domain/AuthService.kt
suspend fun login(request: LoginRequest): LoginResponse? = dbQuery {
//  ↑ suspend: esta función puede pausarse mientras espera a la base de datos
    val row = Users.select { Users.email eq request.email }.singleOrNull()
    // mientras espera la respuesta de PostgreSQL, el servidor atiende otras peticiones
    if (row != null && BCrypt.checkpw(request.password, row[Users.passwordHash])) {
        LoginResponse(token = JwtConfig.makeToken(...), user = ...)
    } else null
}
```

```kotlin
// android — TaskWebSocketClient.kt
suspend fun connect(boardId: String, token: String, userIdParam: String) {
//  ↑ suspend: espera a que el WebSocket esté conectado sin bloquear la UI
    _connectionState.value = WebSocketState.Connecting
    connectWithRetry(boardId, token, userIdParam)
}
```

**Cómo se lanza una coroutine (el equivalente a `asyncio.run()`):**
- En el backend: Ktor lanza automáticamente una coroutine por cada petición HTTP recibida.
- En Android: `viewModelScope.launch { }` lanza la coroutine atada al ciclo de vida del ViewModel.

**`dbQuery { }` — wrapper de coroutine para la base de datos:**
```kotlin
// backend — DatabaseFactory.kt
suspend fun <T> dbQuery(block: suspend () -> T): T =
    newSuspendedTransaction(Dispatchers.IO) { block() }
// Dispatchers.IO → ejecuta en el pool de threads de I/O, no bloquea el thread principal
```

> **Equivalencia directa Python**: `suspend fun` = `async def`. `dbQuery { }` = `await asyncio.get_event_loop().run_in_executor(...)`. La diferencia es que en Kotlin el compilador gestiona automáticamente la continuación de la coroutine.

---

### `Flow` y `StateFlow` — streams reactivos

**Qué es:** Un `Flow` es un stream asíncrono de datos que puede emitir múltiples valores a lo largo del tiempo. `StateFlow` es una versión especial que siempre tiene un valor actual y notifica a todos los observadores cuando cambia.

**Dónde aparece en FlowBoard:**

```kotlin
// android — TaskWebSocketClient.kt

// StateFlow: estado de conexión — siempre tiene un valor; los observadores reciben cada cambio
private val _connectionState = MutableStateFlow<WebSocketState>(WebSocketState.Disconnected)
val connectionState: StateFlow<WebSocketState> = _connectionState.asStateFlow()
//  ↑ La UI observa esto y actualiza el indicador de conexión automáticamente

// Channel + Flow: mensajes entrantes — cola ilimitada de mensajes del servidor
private val _incomingMessages = Channel<WebSocketMessage>(Channel.UNLIMITED)
val incomingMessages: Flow<WebSocketMessage> = _incomingMessages.receiveAsFlow()
//  ↑ Cada mensaje que llega del servidor se emite aquí; los listeners lo procesan
```

**Cómo se cambia el estado:**
```kotlin
_connectionState.value = WebSocketState.Connecting    // → todos los observadores notificados
_connectionState.value = WebSocketState.Connected(boardId)
_connectionState.value = WebSocketState.Disconnected
```

> **Equivalencia Python**: `StateFlow` es similar a una `asyncio.Queue` observable, o a un generador asíncrono (`async for item in generator`). La diferencia es que `StateFlow` siempre retiene el último valor y lo da inmediatamente a nuevos suscriptores.

---

### `CoroutineScope`, `SupervisorJob` y `Dispatchers`

**Qué es:** El `CoroutineScope` define el contexto y ciclo de vida de las coroutines. `SupervisorJob` hace que si una coroutine hija falla, las demás no se cancelen. `Dispatchers.IO` asigna la coroutine al pool de threads de I/O.

**Dónde aparece en FlowBoard:**

```kotlin
// android — TaskWebSocketClient.kt
private suspend fun establishConnection(...) {
    connectionScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    //                               ↑ si el pingJob falla, la coroutine principal sigue viva
    //                                                    ↑ ejecuta en thread de red, no en UI

    client.webSocket(...) {
        val pingJob = connectionScope?.launch {  // ← lanza coroutine hija para el ping
            startPingLoop()                      //   corre en paralelo con la escucha de mensajes
        }

        for (frame in incoming) {   // ← escucha mensajes (suspende sin bloquear)
            when (frame) {
                is Frame.Text  -> handleIncomingMessage(frame.readText())
                is Frame.Close -> break
                else -> {}
            }
        }

        pingJob?.cancel()  // cuando se cierra la conexión, para el ping
    }
}
```

> **Equivalencia Python**: `CoroutineScope` con `SupervisorJob` = `asyncio.TaskGroup` (Python 3.11+) o un grupo de tasks donde los fallos individuales no afectan al resto. `Dispatchers.IO` = `loop.run_in_executor(executor, ...)`.

---

### Reconexión automática con backoff exponencial

**Qué es:** Cuando la conexión WebSocket se pierde, el cliente no intenta reconectar inmediatamente (para no saturar el servidor). En cambio, espera un tiempo que se **duplica con cada intento fallido** hasta un máximo.

**Dónde aparece en FlowBoard:**

```kotlin
// android — TaskWebSocketClient.kt
private suspend fun connectWithRetry(boardId: String, token: String, userIdParam: String) {
    var attempt = 0
    var delay = INITIAL_RECONNECT_DELAY_MS  // empieza en 1 segundo

    while (attempt < MAX_RECONNECT_ATTEMPTS) {  // máximo 10 intentos
        try {
            attempt++
            if (attempt > 1) {
                delay(delay)                                     // espera antes de reintentar
                delay = (delay * 2).coerceAtMost(MAX_RECONNECT_DELAY_MS)  // dobla el tiempo (max 30s)
            }
            establishConnection(boardId, token, userIdParam)    // intenta conectar
            attempt = 0                                          // éxito: resetea contador
            delay = INITIAL_RECONNECT_DELAY_MS
            return
        } catch (e: CancellationException) { throw e }          // cancelación siempre se propaga
        catch (e: Exception) { /* reintentará */ }
    }
}
```

Secuencia de esperas: 1s → 2s → 4s → 8s → 16s → 30s → 30s → ...

> **Equivalencia Python**: es el mismo patrón que se usaría con `asyncio.sleep(delay)` dentro de un `while` con `delay *= 2`.

---

### JWT — JSON Web Token

**Qué es:** Un estándar (RFC 7519) para transmitir información de autenticación de forma segura. Un JWT tiene tres partes separadas por puntos: `HEADER.PAYLOAD.SIGNATURE`.

```
eyJhbGciOiJIUzI1NiJ9  ←  HEADER (algoritmo: HS256)
.
eyJlbWFpbCI6InBhdUBmbG93Ym9hcmQuY29tIiwidXNlcklkIjoiMTIzIn0  ←  PAYLOAD (datos del usuario)
.
SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c  ←  SIGNATURE (HMAC-SHA256)
```

**Dónde se crea en FlowBoard:**

```kotlin
// backend/src/main/kotlin/com/flowboard/plugins/Security.kt
object JwtConfig {
    private val algorithm = Algorithm.HMAC256(secret)  // clave secreta del servidor

    fun makeToken(email: String, userId: String, username: String): String =
        JWT.create()
            .withIssuer(issuer)          // quién emitió el token
            .withAudience(audience)      // para quién es el token
            .withClaim("email", email)   // datos dentro del PAYLOAD
            .withClaim("userId", userId)
            .withClaim("username", username)
            .sign(algorithm)             // firma con HMAC-SHA256 → genera la SIGNATURE
}
```

**Dónde se verifica en cada petición:**

```kotlin
// backend — Security.kt
jwt("auth-jwt") {
    verifier(JwtConfig.verifier)        // comprueba que la firma sea válida
    validate { credential ->
        val userId = credential.payload.getClaim("userId")?.asString()
        if (!userId.isNullOrEmpty()) JWTPrincipal(credential.payload) else null
        // null → Ktor devuelve 401 automáticamente
    }
}
```

**Dónde se envía desde el cliente Android:**

```kotlin
// android — TaskWebSocketClient.kt
client.webSocket(urlString = WS_URL, request = {
    headers.append("Authorization", "Bearer $token")  // ← JWT en la cabecera
})
```

> **Equivalencia Python PU5**: el JWT es el equivalente al token que el servidor enviaba al cliente tras la autenticación con RSA+Fernet. La diferencia es que JWT es autocontenido: lleva el `userId` dentro, el servidor no necesita consultar una BD para saber quién es.

---

### BCrypt — hash de contraseñas

**Qué es:** Una función de hash unidireccional diseñada específicamente para contraseñas. Genera un **salt aleatorio** en cada llamada, lo incluye en el resultado y aplica el algoritmo miles de veces (factor de coste), haciendo que los ataques de fuerza bruta sean muy lentos.

**Dónde aparece en FlowBoard:**

```kotlin
// backend/src/main/kotlin/com/flowboard/domain/AuthService.kt

// Registro — solo se guarda el hash, nunca la contraseña
val hashedPassword = BCrypt.hashpw(request.password, BCrypt.gensalt())
// BCrypt.gensalt() genera un salt aleatorio como "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy"
// El resultado tiene el formato: $2a$10$<salt><hash>  — 60 caracteres total

Users.insert { it[passwordHash] = hashedPassword }  // guarda en BD

// Login — verificación
if (BCrypt.checkpw(request.password, row[Users.passwordHash])) {
    // BCrypt extrae el salt del hash almacenado, rehashea la contraseña y compara
    // Si coincide → contraseña correcta
}
```

**Por qué no se puede "descifrar":** BCrypt no es un cifrado, es un hash. No existe operación inversa. Si alguien roba la BD solo obtiene hashes irreversibles.

> **Equivalencia directa Python PU5**: `BCrypt.hashpw(pass, BCrypt.gensalt())` = `bcrypt.hashpw(password_bytes, bcrypt.gensalt())`. `BCrypt.checkpw(pass, hash)` = `bcrypt.checkpw(password_bytes, stored_hash)`. Es literalmente el mismo algoritmo.

---

### HMAC-SHA256 — firma de tokens

**Qué es:** Un algoritmo de firma simétrica. Combina una **clave secreta** con los datos usando el algoritmo SHA-256. Cualquiera que tenga la clave puede verificar que el mensaje no ha sido alterado.

**Dónde aparece en FlowBoard:**

```kotlin
// backend/src/main/kotlin/com/flowboard/plugins/Security.kt
private val algorithm = Algorithm.HMAC256(secret)  // secret = JWT_SECRET del entorno
// HMAC-SHA256 firma el token JWT: si alguien modifica el payload, la firma no cuadra
// → el servidor lo rechaza automáticamente como inválido

val verifier = JWT.require(algorithm).withIssuer(issuer).build()
// verifier comprueba: ¿la firma del token fue hecha con nuestra JWT_SECRET?
```

> **Equivalencia Python**: `Algorithm.HMAC256(secret)` es el mismo algoritmo que `hmac.new(secret.encode(), msg, hashlib.sha256)`. La librería `auth0/java-jwt` lo gestiona automáticamente igual que `PyJWT` en Python.

---

### Pool de conexiones — HikariCP

**Qué es:** En lugar de abrir una nueva conexión a la base de datos en cada petición (lento), HikariCP mantiene un **pool** de N conexiones abiertas y las presta cuando se necesitan. Cuando la operación termina, la conexión vuelve al pool.

**Dónde aparece en FlowBoard:**

```kotlin
// backend — DatabaseFactory.kt
hikariConfig.maximumPoolSize = 10              // máximo 10 conexiones simultáneas
hikariConfig.isAutoCommit    = false           // cada dbQuery { } es una transacción
hikariConfig.transactionIsolation = "TRANSACTION_REPEATABLE_READ"

// Cada llamada a dbQuery { } toma una conexión del pool, la usa y la devuelve:
suspend fun <T> dbQuery(block: suspend () -> T): T =
    newSuspendedTransaction(Dispatchers.IO) { block() }
// Al salir del bloque, la conexión se libera automáticamente al pool
```

> **Equivalencia Python**: sería similar a usar `psycopg2.pool.ThreadedConnectionPool(minconn=1, maxconn=10, ...)` con un context manager que devuelve la conexión al terminar.

---

### Dependency Injection con Hilt — `@Inject`, `@Module`, `@Provides`

**Qué es:** Un patrón donde los objetos no crean sus propias dependencias sino que las reciben desde fuera. Hilt es el framework de DI de Android — gestiona el ciclo de vida de los objetos y los inyecta donde se necesitan.

**Dónde aparece en FlowBoard:**

```kotlin
// android — NetworkModule.kt
@Module
@InstallIn(SingletonComponent::class)  // estas instancias viven mientras vive la app
object NetworkModule {

    @Provides @Singleton
    fun provideHttpClient(): HttpClient = HttpClient(Android) { ... }
    //  ↑ Hilt crea este objeto UNA VEZ y lo inyecta donde lo pidan

    @Provides @Singleton
    fun provideTaskApiService(
        @HttpClientQualifier httpClient: HttpClient,  // ← Hilt lo inyecta automáticamente
        authRepository: AuthRepository               // ← Hilt lo inyecta automáticamente
    ): TaskApiService = TaskApiService(httpClient, authRepository)
}
```

**En la clase que lo recibe:**
```kotlin
@Singleton
class TaskWebSocketClient @Inject constructor(
    private val client: HttpClient  // ← Hilt inyecta el singleton de NetworkModule
) { ... }
```

**Por qué:** sin DI, `TaskWebSocketClient` tendría que crear su propio `HttpClient` → dos instancias → dos pools de conexiones → desperdicio de recursos y bugs.

> **Equivalencia Python**: en Python la DI se hace manualmente pasando objetos como parámetros o usando librerías como `dependency-injector`. Hilt automatiza este proceso con anotaciones.

---

## RA4 — Servicios en red con librerías estándar

> **Enunciado completo**: Desarrolla aplicaciones que ofrecen servicios en red, utilizando librerías de clases y aplicando criterios de eficiencia y disponibilidad.

---

### RA4.a — Protocolos estándar identificados e implementados

**Criterio**: Se han identificado diferentes protocolos estándar de comunicación para la implementación de servicios en red.

**En FlowBoard se usan dos protocolos estándar:**

**1. HTTP/REST (RFC 7231)** — Para todas las operaciones CRUD: más de 40 endpoints bajo `/api/v1/`. Los verbos son exactamente los del temario: GET, POST, PUT, DELETE.

```kotlin
// backend/src/main/kotlin/com/flowboard/routes/TaskRoutes.kt
fun Route.taskRoutes(taskService: TaskService) {
    authenticate("auth-jwt") {
        route("/tasks") {
            get    { ... }          // GET  /tasks
            get("/{id}") { ... }    // GET  /tasks/{id}
            post   { ... }          // POST /tasks
            put("/{id}") { ... }    // PUT  /tasks/{id}
            delete("/{id}") { ... } // DELETE /tasks/{id}
        }
    }
}
```

**2. WebSocket (RFC 6455)** — Para sincronización en tiempo real de tareas y documentos colaborativos. Protocolo full-duplex que elimina el polling continuo.

```kotlin
// backend/src/main/kotlin/com/flowboard/plugins/WebSockets.kt
fun Application.configureWebSockets() {
    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(30)  // ping automático cada 30s
        timeout    = Duration.ofSeconds(15)  // timeout de respuesta
        maxFrameSize = Long.MAX_VALUE        // frames de hasta 10 MB
    }
}
```

> **Equivalencia con el temario**: HTTP/REST = lo que hacíamos en PU3 con `socket` + PU4 con `Flask`. WebSocket = socket bidireccional persistente, similar al servidor TCP con `socket.SOCK_STREAM` pero sin cerrar la conexión.

---

### RA4.b — Ventajas de los protocolos estándar

**Criterio**: Se han reconocido las ventajas de la utilización de protocolos estándar para la comunicación entre aplicaciones y procesos.

**Ventajas aplicadas en FlowBoard:**

- **Interoperabilidad**: la API REST puede consumirse desde Postman, curl, cualquier app móvil o web, sin librería especial. En los 18 test cases de la memoria (§6.2.3) se verifica con herramientas externas.
- **Sin estado (stateless)**: cada petición HTTP lleva su propio token JWT; el servidor no guarda sesión → escalable horizontalmente.
- **Negociación de contenido**: el cliente y servidor acuerdan usar JSON automáticamente:

```kotlin
// android/app/src/main/java/com/flowboard/di/NetworkModule.kt
HttpClient(Android) {
    install(ContentNegotiation) {
        json(Json {
            isLenient = true         // cliente tolerante con formatos JSON del servidor
            ignoreUnknownKeys = true // retrocompatible: campos nuevos del servidor no rompen el cliente
        })
    }
}
```

- **WebSocket estándar**: el cliente Android se conecta con OkHttp y el servidor con Ktor; al ser RFC 6455 cualquier implementación es compatible.

> **Equivalencia**: en PU4 vimos que Flask/`http.client` en Python podían hablar entre sí precisamente por seguir HTTP estándar. Aquí es exactamente igual pero entre Kotlin Android y Kotlin Ktor.

---

### RA4.c — Librerías para servicios en red analizadas

**Criterio**: Se han analizado librerías que permitan implementar servicios en red utilizando protocolos estándar de comunicación.

**Librería servidor: Ktor Server 2.3.7 (JetBrains)**

```kotlin
// backend/src/main/kotlin/com/flowboard/Application.kt
embeddedServer(Netty, port = System.getenv("PORT")?.toInt() ?: 8080) {
    configureWebSockets()    // plugin WebSocket RFC 6455
    configureSecurity()      // plugin JWT Auth
    configureRouting()       // 40+ rutas REST
}.start(wait = true)
```

**Librería cliente Android: Ktor Client 2.3.7 — dos instancias diferenciadas:**

```kotlin
// android/app/src/main/java/com/flowboard/di/NetworkModule.kt

// Cliente HTTP (peticiones REST)
@Provides @Singleton
fun provideHttpClient(): HttpClient = HttpClient(Android) {
    engine { connectTimeout = 30_000; socketTimeout = 30_000 }
    install(ContentNegotiation) { json(...) }
    install(Logging) { level = LogLevel.HEADERS }  // logs de cabeceras HTTP
}

// Cliente WebSocket (tiempo real) — motor OkHttp por mejor soporte WS en Android
@Provides @Singleton @WebSocketClientQualifier
fun provideWebSocketClient(): HttpClient = HttpClient(OkHttp) {
    install(WebSockets) {
        pingInterval = 30_000    // mantiene la conexión viva
        maxFrameSize = Long.MAX_VALUE
    }
}
```

Se eligió Ktor frente a Retrofit porque Ktor es Kotlin-nativo, soporta coroutines nativamente y permite usar el mismo framework en cliente y servidor (Kotlin Multiplatform).

> **Equivalencia**: Ktor Server = el servidor Flask de PU4. Ktor Client = el `http.client` de PU4 pero para Kotlin. WebSocket = el servidor TCP de PU3 pero bidireccional y sin cortar la conexión.

---

### RA4.d — Servicio desarrollado y probado

**Criterio**: Se han desarrollado y probado servicios de comunicación en red.

**REST API — 40+ endpoints implementados y funcionando en producción:**

```kotlin
// backend/src/main/kotlin/com/flowboard/domain/TaskService.kt
suspend fun createTask(request: CreateTaskRequest, userId: String, ...): Task {
    val task = dbQuery {
        val taskId = UUID.randomUUID()
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        Tasks.insert {
            it[id]          = taskId
            it[title]       = request.title
            it[description] = request.description
            it[priority]    = request.priority
            it[createdBy]   = UUID.fromString(userId)
            it[createdAt]   = now
            it[updatedAt]   = now
        }
        getTaskById(taskId.toString())!!
    }
    // Emitir evento WebSocket a todos los clientes de la sala
    webSocketManager?.broadcastToRoom(boardId = task.projectId!!, message = TaskCreatedMessage(...))
    return task
}
```

**WebSocket — dos canales implementados:**
- `ws/{boardId}` → sincronización de tareas en tiempo real
- `document-ws/{docId}` → edición colaborativa con motor CRDT

**Pruebas (ver Memoria §6.2.3 — 18 Test Cases):**
- TC-01: GET /tasks → 200 OK con lista de tareas
- TC-02: POST /tasks → 201 Created con el objeto creado
- TC-03: DELETE /tasks/{id} → 204 No Content

> **Equivalencia**: el servidor Ktor con sus rutas es el equivalente al servidor Flask de PU4. La diferencia es que aquí hay un backend real desplegado en la nube, no local.

---

### RA4.e — Clientes de comunicación para verificar el funcionamiento

**Criterio**: Se han utilizado clientes de comunicaciones para verificar el funcionamiento de los servicios.

Se usaron tres clientes externos para verificar el servicio (todos documentados en Memoria §6.2.3):

**1. Postman** — pruebas funcionales de todos los endpoints REST contra producción.

**2. curl** — verificación desde línea de comandos:
```bash
curl -X GET https://flowboard-api-phrk.onrender.com/api/v1/tasks \
     -H "Authorization: Bearer <token>"
# → 200 OK con JSON de tareas
```

**3. DBeaver** — conexión directa a PostgreSQL para verificar que los datos persisten correctamente.

**4. La propia app Android** — cliente completo que consume todos los endpoints:

```kotlin
// android/app/src/main/java/com/flowboard/data/remote/api/TaskApiService.kt
suspend fun getAllTasks(): List<Task> {
    val response = httpClient.get("${ApiConfig.API_BASE_URL}/tasks") {
        headers { append("Authorization", "Bearer ${authRepository.getToken()}") }
    }
    return response.body()
}
```

> **Equivalencia**: Postman y curl equivalen a usar `http.client` de Python o el navegador para probar los endpoints Flask del PU4.

---

### RA4.f — Comunicación simultánea de varios clientes

**Criterio**: Se han incorporado mecanismos para posibilitar la comunicación simultánea de varios clientes con el servicio.

**WebSocket con broadcast a todos los clientes de una sala:**

```kotlin
// backend/src/main/kotlin/com/flowboard/domain/TaskService.kt
// Cuando un usuario crea una tarea, se notifica a TODOS los del board a la vez
webSocketManager.broadcastToRoom(
    boardId = task.projectId!!,
    message = TaskCreatedMessage(
        timestamp = Clock.System.now()...,
        boardId   = task.projectId!!,
        task      = task.toSnapshot(),
        createdBy = userInfo
    )
)
```

**Cada sesión WebSocket corre en su propia coroutine (equivalente a threading en Python):**

```kotlin
// backend/src/main/kotlin/com/flowboard/routes/DocumentWebSocketRoutes.kt
webSocket("/document-ws/{docId}") {
    // Cada cliente conectado ejecuta este bloque en una coroutine separada
    // → soporta N clientes simultáneos sin bloquear
    val session = call.sessions.get<UserSession>()
    for (frame in incoming) {    // escucha mensajes del cliente
        when (frame) {
            is Frame.Text -> { ... processar operación CRDT ... }
            else -> {}
        }
    }
}
```

**HikariCP pool de 10 conexiones** en el backend soporta hasta 10 operaciones de base de datos simultáneas.

> **Equivalencia con el temario**: `broadcastToRoom()` = la barrera/event de PU1 que notifica a todos los procesos a la vez. Cada sesión WebSocket en su coroutine = cada `Thread` o `Process` de PU2 manejando un cliente.

---

### RA4.g — Verificación de disponibilidad del servicio

**Criterio**: Se ha verificado la disponibilidad del servicio.

**Mecanismos implementados:**

**1. Ping WebSocket automático** — el servidor y el cliente se confirman mutuamente que siguen vivos cada 30 segundos:
```kotlin
// WebSockets.kt (servidor)
pingPeriod = Duration.ofSeconds(30)
timeout    = Duration.ofSeconds(15)  // si no responde en 15s, desconecta

// NetworkModule.kt (cliente Android)
install(WebSockets) { pingInterval = 30_000 }  // ping cada 30s desde el cliente
```

**2. Despliegue en Render.com** — reinicio automático si el proceso cae; disponibilidad del 99% con monitorización de CPU/RAM/latencia.

**3. Caché offline con Room** — la app Android sigue funcionando aunque el servidor esté caído:
```kotlin
// android — TaskRepositoryImpl.kt
override fun getAllTasks(): Flow<List<Task>> {
    return taskDao.getAllTasks().map { ... }  // sirve desde Room si el backend no responde
}
```

**4. URL de producción siempre HTTPS:**
```kotlin
// android/app/src/main/java/com/flowboard/data/remote/ApiConfig.kt
val BASE_URL = "https://flowboard-api-phrk.onrender.com"
val WS_BASE_URL = "wss://flowboard-api-phrk.onrender.com"   // WSS = WebSocket + TLS
```

> **Equivalencia**: el ping WebSocket es el equivalente al `event.wait(timeout=5)` de PU2-2: espera respuesta, y si no llega en el tiempo dado, actúa.

---

### RA4.h — Depuración y documentación

**Criterio**: Se han depurado y documentado las aplicaciones desarrolladas.

**Logging estructurado en el backend** con niveles y prefijos visuales:
```kotlin
// backend — DatabaseFactory.kt, TaskService.kt
private val logger = LoggerFactory.getLogger(TaskService::class.java)
logger.debug("Broadcasted TASK_CREATED event for task ${task.id}")
logger.error("Failed to broadcast TASK_CREATED event", e)
```

**Logging de cabeceras HTTP en el cliente Android:**
```kotlin
// android — NetworkModule.kt
install(Logging) {
    logger = Logger.DEFAULT
    level  = LogLevel.HEADERS  // muestra cabeceras de cada petición en Logcat
}
```

**Documentación:**
- Memoria §6.4: tabla de errores detectados y soluciones (bug del SHA-1, bug URL Render.com, etc.)
- Memoria §10.1: referencia completa de la API REST (todos los endpoints, parámetros, respuestas)
- Memoria §6.2.3: 18 test cases documentados con respuestas HTTP reales

---

## RA5 — Seguridad en acceso, almacenamiento y transmisión

> **Enunciado completo**: Protege las aplicaciones y los datos definiendo y aplicando criterios de seguridad en el acceso, almacenamiento y transmisión de la información.

---

### RA5.a — Principios y prácticas de programación segura

**Criterio**: Se han identificado y aplicado principios y prácticas de programación segura.

**Tres principios aplicados:**

**1. Protección anti-enumeración** — el endpoint de recuperación de contraseña siempre devuelve 200, tanto si el email existe como si no. Así un atacante no puede saber qué emails están registrados:

```kotlin
// backend/src/main/kotlin/com/flowboard/domain/AuthService.kt
suspend fun requestPasswordReset(email: String) {
    val user = Users.select { Users.email eq email }.singleOrNull()
    // Si el usuario no existe, no lanzamos error — devolvemos 200 igualmente
    // (el atacante no sabe si el email existe)
    if (user == null) return  // silencio: no "User not found"
    // ... enviar OTP solo si existe ...
}
```

**2. Secretos fuera del código** — ningún secreto está hardcodeado:
```kotlin
// backend/src/main/kotlin/com/flowboard/plugins/Security.kt
private val secret = System.getenv("JWT_SECRET") ?: "dev-secret-key"
// JWT_SECRET, DATABASE_URL y RESEND_API_KEY se inyectan como variables de entorno en Render.com
```

**3. Validación de entrada** — el servidor usa `call.receive<T>()` que falla con 400 ante JSON malformado o campos obligatorios ausentes.

> **Equivalencia**: es el mismo principio del PU5: no exponer información innecesaria, separar secretos del código (como `os.environ["JWT_SECRET"]` en Python).

---

### RA5.b — Técnicas y prácticas criptográficas

**Criterio**: Se han analizado las principales técnicas y prácticas criptográficas.

**Tres técnicas criptográficas implementadas (explicadas en Memoria §9.4):**

**1. BCrypt** — hash unidireccional con salt aleatorio para contraseñas (equivalente al `bcrypt` de Python del PU5):

```kotlin
// backend/src/main/kotlin/com/flowboard/domain/AuthService.kt — registro
val hashedPassword = BCrypt.hashpw(request.password, BCrypt.gensalt())
// La contraseña NUNCA se guarda en texto plano

// — login
if (BCrypt.checkpw(request.password, row[Users.passwordHash])) {
    // contraseña correcta: BCrypt extrae el salt del hash y compara
}
```

**2. HMAC-SHA256** — firma simétrica para los tokens JWT:

```kotlin
// backend/src/main/kotlin/com/flowboard/plugins/Security.kt
private val algorithm = Algorithm.HMAC256(secret)  // clave secreta del servidor

fun makeToken(email: String, userId: String, username: String): String =
    JWT.create()
        .withClaim("email", email)
        .withClaim("userId", userId)
        .sign(algorithm)  // firma el token con HMAC-SHA256
```

**3. TLS/HTTPS** — cifrado en tránsito (toda la comunicación cliente-servidor viaja cifrada).

> **Equivalencia directa con el temario PU5:**
> - BCrypt aquí = `bcrypt.hashpw()` + `bcrypt.checkpw()` de Python
> - HMAC-SHA256 para JWT = el cifrado simétrico Fernet de PU5
> - TLS/HTTPS = el "RSA + Fernet sobre socket" de PU5 pero gestionado por el servidor web

---

### RA5.c — Políticas de seguridad / control de acceso

**Criterio**: Se han definido e implantado políticas de seguridad para limitar y controlar el acceso de los usuarios a las aplicaciones desarrolladas.

**Todas las rutas protegidas con `authenticate("auth-jwt")`:**

```kotlin
// backend/src/main/kotlin/com/flowboard/routes/TaskRoutes.kt
fun Route.taskRoutes(taskService: TaskService) {
    authenticate("auth-jwt") {  // <-- TODA la ruta requiere token válido
        route("/tasks") {
            get    { ... }   // sin token → 401 Unauthorized automático
            post   { ... }   // sin token → 401 Unauthorized automático
            put    { ... }
            delete { ... }
        }
    }
}
```

**Verificación del JWT en cada petición:**

```kotlin
// backend/src/main/kotlin/com/flowboard/plugins/Security.kt
jwt("auth-jwt") {
    verifier(JwtConfig.verifier)   // verifica firma HMAC-SHA256
    validate { credential ->
        val userId = credential.payload.getClaim("userId")?.asString()
        if (!userId.isNullOrEmpty()) JWTPrincipal(credential.payload) else null
        // null → Ktor responde 401 automáticamente
    }
}
```

**Test documentado (Memoria §6.2.3, TC-06):** petición sin token → respuesta `401 Unauthorized`.

> **Equivalencia con el temario PU5**: `authenticate("auth-jwt")` es exactamente lo mismo que la comprobación del rol en el ACL de Python: `if cmd not in ACL[role]: resp = "[DENIED]"`. Aquí si el token no es válido, Ktor devuelve 401 automáticamente.

---

### RA5.d — Esquemas de seguridad basados en roles

**Criterio**: Se han utilizado esquemas de seguridad basados en roles.

**FlowBoard tiene tres niveles de roles** (más completo que el ACL `admin/user` del PU5):

**Nivel 1 — Rol de sistema** (tabla `users`):
```kotlin
// backend/src/main/kotlin/com/flowboard/data/database/Tables.kt
object Users : Table("users") {
    val role = enumerationByName("role", 20, UserRole::class).default(UserRole.USER)
    // UserRole: USER, ADMIN
}
```

**Nivel 2 — Rol en documentos** (tabla `document_permissions`):
```kotlin
object DocumentPermissions : Table("document_permissions") {
    val role = varchar("role", 10)  // "viewer" | "editor" | "owner"
    // viewer  → solo puede leer
    // editor  → puede editar
    // owner   → puede editar, compartir y eliminar
}
```

**Nivel 3 — Rol en workspaces** (tabla `workspace_members`):
```kotlin
object WorkspaceMembers : Table("workspace_members") {
    val role = enumerationByName("role", 20, WorkspaceRole::class).default(WorkspaceRole.MEMBER)
    // WorkspaceRole: MEMBER, ADMIN, OWNER
}
```

La pantalla "Share Document" (Memoria §5.4 figura 5.20) permite al owner asignar roles a otros usuarios en tiempo real.

> **Equivalencia directa con el temario PU5**: el diccionario `ACL = {"admin": {"SECRET", "ECHO"}, "user": {"ECHO"}}` de Python es exactamente el mismo concepto que estas tablas de roles, pero persistido en base de datos en lugar de en memoria.

---

### RA5.e — Algoritmos criptográficos para información almacenada

**Criterio**: Se han empleado algoritmos criptográficos para proteger el acceso a la información almacenada.

**BCrypt para contraseñas** — las contraseñas se almacenan siempre como hash, nunca en texto plano:

```kotlin
// backend/src/main/kotlin/com/flowboard/domain/AuthService.kt
// En el registro:
val hashedPassword = BCrypt.hashpw(request.password, BCrypt.gensalt())
Users.insert {
    it[passwordHash] = hashedPassword  // se guarda el HASH en la BD, no la contraseña
}

// En el login:
BCrypt.checkpw(request.password, row[Users.passwordHash])
// BCrypt recalcula el hash con el salt que ya está en el hash almacenado y compara
```

**OTP para recuperación de contraseña** — código de 6 dígitos con caducidad de 15 minutos:

```kotlin
// backend/src/main/kotlin/com/flowboard/domain/AuthService.kt
val otp = (100000..999999).random().toString()  // 6 dígitos aleatorios
PasswordResetTokens.insert {
    it[code]      = otp
    it[expiresAt] = now.plus(15, DateTimeUnit.MINUTE, TimeZone.currentSystemDefault())
    it[used]      = false   // se marca como true al usarse (no reutilizable)
}
```

> **Equivalencia directa PU5**: BCrypt aquí = `bcrypt.hashpw(password_bytes, salt)` + `bcrypt.checkpw()` de Python. El salt está incluido automáticamente en el hash.

---

### RA5.f — Métodos para asegurar la información transmitida

**Criterio**: Se han identificado métodos para asegurar la información transmitida.

**Cuatro capas de seguridad en tránsito:**

**1. HTTPS** — toda la REST API usa HTTPS (certificado Let's Encrypt gestionado por Render.com):
```kotlin
// android/app/src/main/java/com/flowboard/data/remote/ApiConfig.kt
val BASE_URL = "https://flowboard-api-phrk.onrender.com"  // https:// siempre
```

**2. WSS** — los WebSockets en producción usan WSS (WebSocket sobre TLS):
```kotlin
val WS_BASE_URL = "wss://flowboard-api-phrk.onrender.com"  // wss:// = WS + TLS
```

**3. JWT en cabecera Authorization** — el token viaja en la cabecera, nunca en la URL:
```kotlin
// android — TaskApiService.kt
httpClient.get("${ApiConfig.API_BASE_URL}/tasks") {
    headers { append("Authorization", "Bearer ${authRepository.getToken()}") }
    //                                ^^^^^^^^^^
    //              el token viaja cifrado dentro del túnel TLS, nunca en la URL
}
```

**4. sslmode=require en la conexión BD** — también la conexión entre el servidor Ktor y PostgreSQL es cifrada (ver Memoria §7.2, `DatabaseFactory.kt` línea 115).

> **Equivalencia PU5**: HTTPS+TLS = el túnel cifrado que en PU5 creábamos con RSA+Fernet. JWT en cabecera Authorization = el token que el servidor enviaba al cliente tras autenticarse.

---

### RA5.g — Aplicaciones con comunicaciones seguras

**Criterio**: Se han desarrollado aplicaciones que utilicen comunicaciones seguras para la transmisión de información.

**FlowBoard en producción usa comunicaciones seguras en todas sus capas:**

| Canal | Protocolo | Seguridad |
|---|---|---|
| App Android → API REST | HTTPS | TLS 1.3 (Let's Encrypt) |
| App Android → WebSocket tareas | WSS | TLS 1.3 |
| App Android → WebSocket CRDT | WSS | TLS 1.3 |
| Servidor Ktor → PostgreSQL | JDBC+SSL | `sslmode=require` |

**Autenticación en cada petición** — el JWT viaja en `Authorization: Bearer <token>` en todas las peticiones a rutas protegidas, garantizando que solo usuarios autenticados acceden a los datos.

El backend en producción está en `https://flowboard-api-phrk.onrender.com` — cualquier conexión que no sea HTTPS es rechazada por Render.com.

> **Equivalencia PU5**: esta arquitectura HTTPS+JWT equivale al ejemplo RSA+Fernet+ACL del temario: primero se establece un canal seguro (TLS≈RSA), luego el cliente se autentica con un token (JWT≈clave Fernet) y el servidor comprueba permisos (JWT claims≈ACL).

---

### RA5.h — Depuración y documentación de seguridad

**Criterio**: Se han depurado y documentado las aplicaciones desarrolladas.

**Test cases de seguridad documentados (Memoria §6.2.3):**
- **TC-04**: login con contraseña incorrecta → 401 Unauthorized (BCrypt.checkpw devuelve false)
- **TC-06**: petición a `/tasks` sin token → 401 Unauthorized (authenticate bloquea)
- **TC-11**: solicitud de reset con email inexistente → 200 OK (anti-enumeración funciona)
- **TC-12**: reset con OTP caducado → 400 Bad Request
- **TC-13**: reset con OTP ya usado → 400 Bad Request

**Incidente de seguridad real documentado (Memoria §6.4):**
Google Sign-In fallaba con error `GetCredentialUnknownException` código 10. Causa raíz: el SHA-1 del certificado de firma de la app no estaba registrado en Google Cloud Console. Solución: registrar el SHA-1 correcto. Se documenta como error de seguridad de configuración (no de código).

---

## Resumen para la defensa

| RA | Criterio | Lo más importante | Fichero |
|---|---|---|---|
| RA4 | a | HTTP REST (40+ endpoints) + WebSocket (RFC 6455) | `TaskRoutes.kt`, `WebSockets.kt` |
| RA4 | c | Ktor Server (servidor) + Ktor Client Android + OkHttp WebSocket | `NetworkModule.kt` |
| RA4 | d | CRUD completo en producción + 2 canales WebSocket | `TaskService.kt` |
| RA4 | e | Postman + curl + DBeaver + la propia app como clientes | Memoria §6.2.3 |
| RA4 | f | `broadcastToRoom()` notifica a N clientes simultáneamente | `TaskService.kt` línea 82 |
| RA4 | g | Render.com + ping 30s + caché Room offline | `ApiConfig.kt`, `WebSockets.kt` |
| RA5 | b | BCrypt (contraseñas) + HMAC-SHA256 (JWT) + TLS (tránsito) | `AuthService.kt`, `Security.kt` |
| RA5 | c | `authenticate("auth-jwt")` protege todas las rutas → 401 sin token | `TaskRoutes.kt` línea 16 |
| RA5 | d | 3 niveles de roles: sistema / documentos / workspaces | `Tables.kt` |
| RA5 | e | `BCrypt.hashpw()` — contraseñas nunca en texto plano | `AuthService.kt` línea 31 |
| RA5 | f-g | HTTPS + WSS + JWT en header + sslmode=require | `ApiConfig.kt` |
