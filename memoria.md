# FlowBoard — Proyecto Intermodular (PIM)
### Ciclo Formativo de Grado Superior — Desarrollo de Aplicaciones Multiplataforma (DAM)

---

**Autor:** Pau López Núñez
**Tutor:** Isabel Martí Romeu
**Centro educativo:** IES La Sénia 
**Curso académico:** 2025–2026
**Fecha de entrega:** 17/05/2026
**Repositorio:** https://github.com/paulopnun/flowboard  
**Backend en producción:** https://flowboard-api-phrk.onrender.com  

---

## Resumen

FlowBoard es una aplicación Android de productividad colaborativa inspirada en Notion, desarrollada íntegramente en Kotlin. Combina un editor de documentos en tiempo real basado en CRDT (*Conflict-free Replicated Data Type*) con un gestor completo de tareas, proyectos, espacios de trabajo, chat y notificaciones. La arquitectura sigue el patrón MVVM + Clean Architecture con tres capas bien diferenciadas: presentación (Jetpack Compose + Material 3), dominio (casos de uso y repositorios) y datos (Room SQLite + API REST Ktor). El backend es un servidor Ktor (Kotlin) desplegado en Render.com con PostgreSQL como base de datos relacional, autenticación JWT HMAC-256, BCrypt para hashing de contraseñas y soporte de inicio de sesión con Google mediante Credential Manager. El sistema permite la edición colaborativa simultánea de documentos a través de WebSockets con transformación operacional (OT), CI/CD automatizado con GitHub Actions, y una interfaz adaptativa con soporte completo de modo oscuro/claro.

**Palabras clave:** Android, Kotlin, Jetpack Compose, CRDT, WebSocket, Ktor, PostgreSQL, MVVM, Clean Architecture, Material Design 3, colaboración en tiempo real.

---

## Abstract

FlowBoard is a collaborative productivity Android application inspired by Notion, written entirely in Kotlin. It combines a real-time document editor based on CRDT (Conflict-free Replicated Data Type) with a full task manager, projects, workspaces, chat, and notifications. The architecture follows the MVVM + Clean Architecture pattern with three distinct layers: presentation (Jetpack Compose + Material 3), domain (use cases and repositories) and data (Room SQLite + REST API Ktor). The backend is a Ktor (Kotlin) server deployed on Render.com with PostgreSQL as relational database, JWT HMAC-256 authentication, BCrypt for password hashing and Google Sign-In support via Credential Manager. The system enables simultaneous collaborative document editing via WebSockets with Operational Transformation (OT), automated CI/CD with GitHub Actions, and an adaptive UI with full dark/light mode support.

**Keywords:** Android, Kotlin, Jetpack Compose, CRDT, WebSocket, Ktor, PostgreSQL, MVVM, Clean Architecture, Material Design 3, real-time collaboration.

---

## Índice

1. [Introducción](#1-introducción)
   - 1.1 [Descripción general del proyecto](#11-descripción-general-del-proyecto)
   - 1.2 [Objetivos](#12-objetivos)
   - 1.3 [Público objetivo](#13-público-objetivo)
   - 1.4 [Alcance y limitaciones](#14-alcance-y-limitaciones)
2. [Análisis del sistema](#2-análisis-del-sistema)
   - 2.1 [Requisitos funcionales](#21-requisitos-funcionales)
   - 2.2 [Requisitos no funcionales](#22-requisitos-no-funcionales)
   - 2.3 [Casos de uso](#23-casos-de-uso)
   - 2.4 [Diagrama de casos de uso](#24-diagrama-de-casos-de-uso)
3. [Diseño del sistema](#3-diseño-del-sistema)
   - 3.1 [Arquitectura del sistema](#31-arquitectura-del-sistema)
   - 3.2 [Componentes principales](#32-componentes-principales)
   - 3.3 [Modelo de base de datos](#33-modelo-de-base-de-datos)
   - 3.4 [Diseño de interfaz y navegación](#34-diseño-de-interfaz-y-navegación)
4. [Tecnologías y herramientas](#4-tecnologías-y-herramientas)
5. [Implementación](#5-implementación)
   - 5.1 [Estructura del proyecto](#51-estructura-del-proyecto)
   - 5.2 [Módulos principales](#52-módulos-principales)
   - 5.3 [Funcionalidades clave](#53-funcionalidades-clave)
6. [Pruebas](#6-pruebas)
7. [Despliegue](#7-despliegue)
8. [Planificación y gestión](#8-planificación-y-gestión)
9. [Conclusiones](#9-conclusiones)
10. [Anexos](#10-anexos)
11. [Bibliografía](#11-bibliografía)

---

# 1. Introducción

## 1.1 Descripción general del proyecto

FlowBoard es una aplicación Android nativa de productividad y colaboración diseñada para equipos de trabajo y estudiantes. La aplicación combina las funcionalidades de una herramienta de gestión de tareas estilo Kanban con un editor de documentos colaborativo en tiempo real, todo dentro de un único entorno integrado.

El proyecto surge de la necesidad de disponer de una herramienta todo-en-uno que permita a los equipos organizar su trabajo sin tener que alternar entre múltiples aplicaciones. A diferencia de soluciones como Notion (web-first) o Trello (solo tableros), FlowBoard está pensado desde el inicio como una aplicación móvil nativa para Android, aprovechando al máximo las capacidades del sistema operativo.

El sistema se compone de dos grandes partes:

- **Cliente Android:** Aplicación nativa desarrollada con Kotlin 1.9 y Jetpack Compose, siguiendo los principios de Material Design 3.
- **Servidor backend:** API REST y WebSocket desarrollada con Ktor (Kotlin), desplegada en Render.com con base de datos PostgreSQL.

La característica más innovadora es el motor CRDT (*Conflict-free Replicated Data Type*) con Transformación Operacional (OT) implementado en el cliente, que permite la edición simultánea de documentos por múltiples usuarios sin conflictos, de forma similar a Google Docs.

## 1.2 Objetivos

### Objetivo general

Desarrollar una aplicación Android completa de productividad colaborativa que permita a equipos gestionar tareas, proyectos y documentos en tiempo real, aplicando los conocimientos adquiridos en todos los módulos del ciclo de Desarrollo de Aplicaciones Multiplataforma (DAM).

### Objetivos específicos

1. **Acceso a Datos (AD — RA02):** Implementar una capa de persistencia dual que combine una base de datos relacional PostgreSQL en el servidor (con ORM Exposed) y una base de datos local SQLite mediante Room en el cliente Android, garantizando la sincronización offline/online.

2. **Desarrollo de Interfaces (DI — RA01/02/03/05/06):** Crear una interfaz de usuario completamente declarativa con Jetpack Compose y Material Design 3, con soporte para modo oscuro/claro dinámico, animaciones fluidas y un sistema tipográfico consistente basado en Inter.

3. **Programación Multimedia y Móviles (PMM — RA03/04/05):** Implementar comunicación en tiempo real mediante WebSockets, un motor CRDT para edición colaborativa de documentos, notificaciones del sistema y gestión de presencia de usuarios con cursores animados.

4. **Programación de Procesos y Servicios (PPS — RA04/05):** Desarrollar un backend asíncrono y escalable con Ktor y coroutines de Kotlin, exponiendo una API REST completa con autenticación JWT y soporte de WebSockets bidireccionales.

5. **Itinerario para la Empleabilidad II (RA02):** Integrar autenticación con Google Sign-In mediante Credential Manager, siguiendo estándares de la industria para aplicaciones Android modernas.

6. **Sostenibilidad (RA06):** Minimizar el uso de recursos del servidor mediante inicialización lazy de la base de datos, pooling de conexiones con HikariCP y tareas en segundo plano eficientes con WorkManager.

7. **Introducción a la Nube Pública (RA04):** Desplegar el backend en Render.com con CI/CD automatizado mediante GitHub Actions, generando releases del APK en cada push a master.

## 1.3 Público objetivo

FlowBoard está dirigido principalmente a:

- **Estudiantes universitarios y de FP** que trabajan en proyectos en grupo y necesitan coordinar tareas y compartir documentos.
- **Equipos de desarrollo de software** que buscan una herramienta integrada de gestión de proyectos y documentación técnica.
- **Pequeños equipos de trabajo** (5–20 personas) que necesitan una solución de productividad colaborativa sin los costes de licencia de herramientas enterprise.
- **Usuarios individuales** que desean un gestor de tareas personal con capacidades de documentación avanzadas.

## 1.4 Alcance y limitaciones

### Funcionalidades implementadas

- Autenticación completa: registro, login, Google Sign-In, recuperación de contraseña por OTP de 6 dígitos
- Gestión de tareas con prioridades (LOW/MEDIUM/HIGH/URGENT), fechas, etiquetas y asignación a usuarios
- Editor de documentos colaborativo con bloques enriquecidos: párrafo, H1–H6, listas, todo, código, cita, divisor, imagen, vídeo, audio
- Espacios de trabajo (Workspaces) con invitación por código único de 12 caracteres
- Proyectos con tablero Kanban
- Chat integrado: salas directas, grupos y canales de proyecto
- Notificaciones del sistema con deep links
- Sistema de permisos (viewer/editor/owner) para documentos
- Modo oscuro/claro persistido en DataStore
- Calendario de eventos
- Asistente IA integrado
- CI/CD con GitHub Actions: build automático + release de APK en cada push a master

### Limitaciones

- La aplicación está disponible únicamente para Android (API 26+, Android 8.0 Oreo o superior)
- El servidor utiliza el plan gratuito de Render.com, lo que implica arranque en frío de hasta 50 segundos tras inactividad prolongada
- La funcionalidad offline está limitada a lectura de datos cacheados en Room; la creación y edición requiere conexión
- No se implementa FCM (Firebase Cloud Messaging); las notificaciones push dependen del servidor de notificaciones de Android

---

# 2. Análisis del sistema

## 2.1 Requisitos funcionales

### RF-01: Autenticación y gestión de usuarios

- **RF-01.1:** El sistema permitirá el registro con email, nombre de usuario y contraseña.
- **RF-01.2:** El sistema permitirá el inicio de sesión con credenciales email/contraseña.
- **RF-01.3:** El sistema ofrecerá inicio de sesión con Google mediante Google Credential Manager.
- **RF-01.4:** El sistema permitirá la recuperación de contraseña mediante OTP de 6 dígitos enviado por email, con validez de 15 minutos.
- **RF-01.5:** Las contraseñas se cifrarán con BCrypt antes de almacenarse.
- **RF-01.6:** El sistema emitirá un token JWT HMAC-256 en cada inicio de sesión exitoso.

### RF-02: Gestión de tareas

- **RF-02.1:** Los usuarios podrán crear tareas con título, descripción, prioridad, fecha de vencimiento, etiquetas y asignación.
- **RF-02.2:** Las tareas podrán marcarse como completadas o pendientes.
- **RF-02.3:** Las tareas podrán asociarse a proyectos.
- **RF-02.4:** Las tareas podrán definirse como eventos de calendario con hora de inicio, hora de fin y ubicación.
- **RF-02.5:** Los cambios se propagarán en tiempo real a todos los usuarios del mismo proyecto mediante WebSocket.

### RF-03: Gestión de proyectos

- **RF-03.1:** Los usuarios podrán crear proyectos con nombre, descripción, color y fecha límite.
- **RF-03.2:** Los proyectos podrán tener múltiples miembros.
- **RF-03.3:** Cada proyecto dispondrá de un tablero Kanban.

### RF-04: Editor de documentos colaborativo

- **RF-04.1:** Los usuarios podrán crear y editar documentos con bloques de contenido enriquecido.
- **RF-04.2:** Los cambios se sincronizarán en tiempo real mediante WebSocket y el motor CRDT.
- **RF-04.3:** El sistema resolverá conflictos de edición concurrente mediante Transformación Operacional (OT).
- **RF-04.4:** Se mostrará la posición del cursor de cada editor activo.
- **RF-04.5:** Los documentos podrán ser privados, compartidos o asociados a un workspace.

### RF-05: Espacios de trabajo

- **RF-05.1:** Los usuarios podrán crear workspaces e invitar a otros mediante código único de 12 caracteres.
- **RF-05.2:** Los workspaces tendrán roles: OWNER, ADMIN y MEMBER.

### RF-06: Chat integrado

- **RF-06.1:** El sistema dispondrá de salas de chat directas (1:1) y grupales.
- **RF-06.2:** Los mensajes podrán responderse en hilo y contener adjuntos.

### RF-07: Notificaciones

- **RF-07.1:** El sistema generará notificaciones para asignaciones, menciones e invitaciones.
- **RF-07.2:** Las notificaciones incluirán deep links al recurso relacionado.

### RF-08: Configuración

- **RF-08.1:** El usuario podrá activar/desactivar el modo oscuro, persistido en DataStore.
- **RF-08.2:** El usuario podrá editar su perfil y cambiar su contraseña.

## 2.2 Requisitos no funcionales

### RNF-01: Rendimiento

- Carga inicial de lista de tareas < 2 segundos en condiciones normales de red.
- El motor CRDT aplicará operaciones locales de forma síncrona (< 16 ms) para garantizar 60 FPS.
- HikariCP mantendrá hasta 10 conexiones activas al servidor PostgreSQL.

### RNF-02: Seguridad

- Todas las comunicaciones usarán HTTPS (TLS 1.2+).
- Las contraseñas se almacenarán con BCrypt (factor de coste predeterminado de la librería jBCrypt).
- El servidor implementará protección anti-enumeración en el endpoint de recuperación de contraseña.
- Las rutas protegidas devolverán HTTP 401 ante token inválido o ausente.

### RNF-03: Disponibilidad

- Backend desplegado en Render.com con disponibilidad del 99% (plan gratuito con posible cold start).
- La aplicación funcionará en modo lectura offline gracias a la caché Room.

### RNF-04: Usabilidad

- La interfaz seguirá las guías de Material Design 3.
- Soporte completo de modo oscuro y claro con transición dinámica.
- Todas las transiciones de navegación tendrán duración de 280 ms.
- Compatible con Android API 26+.

### RNF-05: Mantenibilidad

- Patrón MVVM + Clean Architecture con separación estricta de capas.
- Inyección de dependencias con Hilt.
- API REST versionada en `/api/v1/`.

## 2.3 Casos de uso

### CU-01: Registrar usuario

- **Actor:** Usuario no autenticado
- **Precondición:** El usuario no tiene cuenta
- **Flujo principal:**
  1. El usuario introduce email, nombre de usuario, nombre completo y contraseña
  2. El sistema valida que no existan duplicados de email o username
  3. El sistema cifra la contraseña con BCrypt
  4. El sistema almacena el usuario y devuelve un token JWT
  5. El usuario accede al Dashboard
- **Flujo alternativo A:** Email o username ya existen → error 409 Conflict

### CU-02: Inicio de sesión con Google

- **Actor:** Usuario no autenticado
- **Flujo principal:**
  1. El usuario pulsa "Continuar con Google"
  2. Credential Manager muestra el selector de cuentas
  3. El sistema recibe el token de Google y hace upsert del usuario
  4. El sistema devuelve un token JWT propio

### CU-03: Crear y editar documento colaborativo

- **Actor:** Usuario autenticado con rol editor o superior
- **Flujo principal:**
  1. El usuario crea un nuevo documento
  2. El sistema abre el editor y establece conexión WebSocket
  3. El usuario añade bloques de contenido
  4. El motor CRDT genera operaciones y las envía al servidor
  5. El servidor retransmite las operaciones a todos los colaboradores
- **Flujo alternativo A:** Dos usuarios editan el mismo bloque → OT resuelve el conflicto de forma determinista

### CU-04: Gestionar tareas

- **Actor:** Usuario autenticado
- **Flujo principal:**
  1. El usuario accede a la lista de tareas
  2. Crea una tarea con título, descripción, prioridad y fecha opcional
  3. La tarea se persiste en la API REST y se replica en la caché Room
  4. Si pertenece a un proyecto, el servidor emite evento WebSocket TASK_CREATED

### CU-05: Recuperar contraseña

- **Actor:** Usuario no autenticado
- **Flujo principal:**
  1. El usuario introduce su email
  2. El sistema genera un OTP de 6 dígitos con validez de 15 minutos
  3. El sistema envía el OTP por email (Resend API)
  4. El usuario introduce el OTP y la nueva contraseña
  5. El sistema valida el OTP y actualiza el hash BCrypt

## 2.4 Diagrama de casos de uso

```
┌─────────────────────────────────────────────────────────────────────┐
│                         SISTEMA FLOWBOARD                           │
│                                                                     │
│  ┌────────────────┐   ┌────────────────────────────────────────┐   │
│  │  Usuario No    │──▶│ CU-01: Registrar usuario               │   │
│  │  Autenticado   │──▶│ CU-02: Login (email / Google)          │   │
│  └────────────────┘──▶│ CU-05: Recuperar contraseña (OTP)      │   │
│                        └────────────────────────────────────────┘   │
│                                                                     │
│  ┌────────────────┐   ┌────────────────────────────────────────┐   │
│  │  Usuario       │──▶│ CU-03: Editar documento colaborativo   │   │
│  │  Autenticado   │──▶│ CU-04: Gestionar tareas                │   │
│  │                │──▶│ CU-06: Gestionar proyectos             │   │
│  │                │──▶│ CU-07: Unirse a workspace (código)     │   │
│  │                │──▶│ CU-08: Chat (directo / grupo)          │   │
│  └────────────────┘──▶│ CU-09: Gestionar notificaciones        │   │
│                        └────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────┘
```

---

# 3. Diseño del sistema

## 3.1 Arquitectura del sistema

FlowBoard implementa una arquitectura cliente-servidor. El cliente Android sigue el patrón **MVVM + Clean Architecture** en tres capas y el servidor Ktor una arquitectura **por capas** (rutas → dominio → datos).

```
┌──────────────────────────────────────────────────────────────────┐
│                        CLIENTE ANDROID                           │
│                                                                  │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │  PRESENTACIÓN — Jetpack Compose · ViewModel · StateFlow    │ │
│  └───────────────────────────┬────────────────────────────────┘ │
│  ┌───────────────────────────▼────────────────────────────────┐ │
│  │  DOMINIO — Repositorios (interfaces) · Modelos             │ │
│  └───────────────────────────┬────────────────────────────────┘ │
│  ┌───────────────────────────▼────────────────────────────────┐ │
│  │  DATOS — Room · Ktor Client · WebSocket · CRDT Engine      │ │
│  └───────────────────────────┬────────────────────────────────┘ │
└──────────────────────────────┼───────────────────────────────────┘
                               │  HTTPS / WSS
┌──────────────────────────────▼───────────────────────────────────┐
│                       SERVIDOR KTOR                              │
│                                                                  │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │  RUTAS — authRoutes · taskRoutes · documentRoutes · ws...  │ │
│  └───────────────────────────┬────────────────────────────────┘ │
│  ┌───────────────────────────▼────────────────────────────────┐ │
│  │  DOMINIO — AuthService · TaskService · DocumentService...  │ │
│  └───────────────────────────┬────────────────────────────────┘ │
│  ┌───────────────────────────▼────────────────────────────────┐ │
│  │  DATOS — Exposed ORM · HikariCP · DatabaseFactory          │ │
│  └───────────────────────────┬────────────────────────────────┘ │
└──────────────────────────────┼───────────────────────────────────┘
                               │  JDBC / SSL
                     ┌─────────▼──────────┐
                     │  PostgreSQL         │
                     │  (Render.com)       │
                     └────────────────────┘
```

## 3.2 Componentes principales

### Cliente Android

| Componente | Tecnología | Responsabilidad |
|---|---|---|
| `FlowBoardApp.kt` | NavHost Compose | Host de navegación con 18 rutas |
| `TaskViewModel` | ViewModel + Hilt | Estado y lógica de tareas |
| `CollaborativeDocumentViewModel` | ViewModel + Hilt | Estado del editor colaborativo |
| `CRDTEngine` | Kotlin Singleton | Motor de Transformación Operacional |
| `FlowBoardDatabase` | Room | Base de datos local SQLite |
| `NetworkModule` | Hilt Module | Inyección de HttpClient + WebSocket |
| `TaskWebSocketClient` | Ktor WS Client | Canal WebSocket para tareas |
| `FlowBoardTheme` | Compose Theme | Tema Material 3 dinámico |

### Servidor Ktor

| Componente | Tecnología | Responsabilidad |
|---|---|---|
| `Application.kt` | Ktor | Entrada + cadena de plugins |
| `AuthService.kt` | Kotlin object | Registro, login, Google, OTP |
| `TaskService.kt` | Kotlin class | CRUD tareas + broadcast WS |
| `WebSocketManager` | Kotlin class | Gestión de salas WebSocket |
| `DatabaseFactory.kt` | HikariCP | Pool de conexiones PostgreSQL |
| `Tables.kt` | Exposed ORM | Definición de 13 tablas |
| `JwtConfig` | Auth0 JWT | Firma y verificación HMAC-256 |

## 3.3 Modelo de base de datos

### Diagrama Entidad-Relación

```
USERS (1) ──────────────────────────── (N) TASKS
  │                                           │
  ├── (1) ── WORKSPACES ── (N) ── WORKSPACE_MEMBERS
  │                │
  │                └── (N) ── DOCUMENTS ── (N) ── DOCUMENT_PERMISSIONS
  │
  ├── (1) ── PROJECTS ── (N) ── TASKS
  │
  ├── (1) ── NOTIFICATIONS
  │
  ├── (1) ── CHAT_ROOMS ── (N) ── CHAT_PARTICIPANTS
  │                                      └── (N) ── MESSAGES
  │
  └── (1) ── PASSWORD_RESET_TOKENS
```

### Definición de tablas principales (Exposed ORM)

```kotlin
// Tabla de usuarios
object Users : UUIDTable("users") {
    val email           = varchar("email", 255).uniqueIndex()
    val username        = varchar("username", 100).uniqueIndex()
    val fullName        = varchar("full_name", 255)
    val passwordHash    = varchar("password_hash", 255)
    val role            = enumeration("role", UserRole::class).default(UserRole.USER)
    val profileImageUrl = varchar("profile_image_url", 500).nullable()
    val isActive        = bool("is_active").default(true)
    val createdAt       = datetime("created_at")
    val lastLoginAt     = datetime("last_login_at").nullable()
}

// Tabla de tareas — soporta eventos de calendario
object Tasks : UUIDTable("tasks") {
    val title          = varchar("title", 255)
    val description    = text("description")
    val isCompleted    = bool("is_completed").default(false)
    val priority       = enumeration("priority", TaskPriority::class).default(TaskPriority.MEDIUM)
    val dueDate        = datetime("due_date").nullable()
    val createdAt      = datetime("created_at")
    val updatedAt      = datetime("updated_at")
    val assignedTo     = uuid("assigned_to").nullable()
    val projectId      = uuid("project_id").nullable()
    val tags           = json<List<String>>("tags", Json.Default).default(emptyList())
    val attachments    = json<List<String>>("attachments", Json.Default).default(emptyList())
    val isEvent        = bool("is_event").default(false)
    val eventStartTime = datetime("event_start_time").nullable()
    val eventEndTime   = datetime("event_end_time").nullable()
    val location       = varchar("location", 500).nullable()
    val createdBy      = uuid("created_by")
}

// Tabla de documentos colaborativos — jerarquía padre-hijo
object Documents : UUIDTable("documents") {
    val title       = varchar("title", 500)
    val content     = text("content")
    val ownerId     = uuid("owner_id")
    val parentId    = uuid("parent_id").nullable()
    val isPublic    = bool("is_public").default(false)
    val visibility  = varchar("visibility", 20).default("private")
    val workspaceId = uuid("workspace_id").nullable()
    val createdAt   = datetime("created_at")
    val updatedAt   = datetime("updated_at")
    val lastEditedBy = uuid("last_edited_by").nullable()
}

// Permisos de documento — roles viewer / editor / owner
object DocumentPermissions : UUIDTable("document_permissions") {
    val documentId = uuid("document_id")
    val userId     = uuid("user_id")
    val role       = varchar("role", 50).default("viewer")
    val grantedBy  = uuid("granted_by")
    val grantedAt  = datetime("granted_at")
}

// Workspaces — código de invitación único de 12 caracteres
object Workspaces : UUIDTable("workspaces") {
    val name        = varchar("name", 255)
    val description = text("description").nullable()
    val ownerId     = uuid("owner_id")
    val inviteCode  = varchar("invite_code", 12).uniqueIndex()
    val createdAt   = datetime("created_at")
    val updatedAt   = datetime("updated_at")
}

// OTP recuperación de contraseña — expira en 15 min
object PasswordResetTokens : UUIDTable("password_reset_tokens") {
    val email     = varchar("email", 255).index()
    val code      = varchar("code", 6)
    val expiresAt = datetime("expires_at")
    val used      = bool("used").default(false)
}
```

### Diccionario de datos — tabla `users`

| Campo | Tipo | Nulo | Descripción |
|---|---|---|---|
| id | UUID (PK) | No | Identificador único autogenerado |
| email | VARCHAR(255) | No | Email único del usuario |
| username | VARCHAR(100) | No | Nombre de usuario único |
| full_name | VARCHAR(255) | No | Nombre completo |
| password_hash | VARCHAR(255) | No | Hash BCrypt de la contraseña |
| role | ENUM | No | USER / ADMIN |
| profile_image_url | VARCHAR(500) | Sí | URL de la foto de perfil |
| is_active | BOOLEAN | No | Estado de la cuenta |
| created_at | DATETIME | No | Fecha de registro |
| last_login_at | DATETIME | Sí | Último inicio de sesión |

### Diccionario de datos — tabla `tasks`

| Campo | Tipo | Nulo | Descripción |
|---|---|---|---|
| id | UUID (PK) | No | Identificador único |
| title | VARCHAR(255) | No | Título de la tarea |
| description | TEXT | No | Descripción detallada |
| is_completed | BOOLEAN | No | Estado completado |
| priority | ENUM | No | LOW / MEDIUM / HIGH / URGENT |
| due_date | DATETIME | Sí | Fecha de vencimiento |
| assigned_to | UUID (FK→users) | Sí | Usuario asignado |
| project_id | UUID (FK→projects) | Sí | Proyecto al que pertenece |
| tags | JSON | No | Array de etiquetas |
| is_event | BOOLEAN | No | Si es evento de calendario |
| event_start_time | DATETIME | Sí | Inicio del evento |
| event_end_time | DATETIME | Sí | Fin del evento |
| location | VARCHAR(500) | Sí | Ubicación del evento |
| created_by | UUID (FK→users) | No | Usuario creador |

### Diccionario de datos — tabla `documents`

| Campo | Tipo | Nulo | Descripción |
|---|---|---|---|
| id | UUID (PK) | No | Identificador único |
| title | VARCHAR(500) | No | Título del documento |
| content | TEXT | No | Contenido serializado (JSON de bloques) |
| owner_id | UUID (FK→users) | No | Propietario |
| parent_id | UUID (FK→documents) | Sí | Documento padre |
| visibility | VARCHAR(20) | No | private / shared / workspace |
| workspace_id | UUID (FK→workspaces) | Sí | Workspace al que pertenece |
| last_edited_by | UUID (FK→users) | Sí | Último editor |

## 3.4 Diseño de interfaz y navegación

### Mapa de navegación (18 rutas)

```
app_start
├── login
├── register
├── forgot_password
└── (autenticado)
    ├── dashboard
    ├── task_list
    ├── task_detail/{taskId}
    ├── create_task
    ├── calendar
    ├── project_list
    ├── project_detail/{projectId}
    ├── my_documents
    ├── document_editor/{documentId}
    ├── collaborative_document/{documentId}
    ├── workspace_list
    ├── workspace_detail/{workspaceId}
    ├── workspace_documents/{workspaceId}
    ├── chat
    ├── chat_room/{chatRoomId}
    ├── notifications
    ├── profile
    └── settings
```

### Sistema de temas Material 3

**Paleta de colores (`Color.kt`):**

```kotlin
// Tema oscuro — fondo estilo Notion
private val DarkColorScheme = darkColorScheme(
    primary            = Color(0xFF3B82F6),
    onPrimary          = Color(0xFFFFFFFF),
    primaryContainer   = Color(0xFF1D3A6E),
    background         = Color(0xFF191919),
    surface            = Color(0xFF1F1F1F),
    onBackground       = Color(0xFFE8E8E8),
    onSurface          = Color(0xFFE8E8E8)
)

// Tema claro
private val LightColorScheme = lightColorScheme(
    primary            = Color(0xFF2563EB),
    onPrimary          = Color(0xFFFFFFFF),
    background         = Color(0xFFFFFFFF),
    surface            = Color(0xFFF8F9FA),
    onBackground       = Color(0xFF191919),
    onSurface          = Color(0xFF191919)
)

// Colores semánticos de prioridad
val PriorityLow    = Color(0xFF4CAF50)
val PriorityMedium = Color(0xFF2196F3)
val PriorityHigh   = Color(0xFFFF9800)
val PriorityUrgent = Color(0xFFF44336)

// Colores de cursor para colaboración en tiempo real
val CollabCursorColors = listOf(
    Color(0xFFEF4444), Color(0xFF3B82F6), Color(0xFF10B981),
    Color(0xFFF59E0B), Color(0xFF8B5CF6), Color(0xFFEC4899)
)
```

**Tipografía Inter (`Type.kt`):**

```kotlin
// Escala tipográfica M3 completa con Inter (Google Fonts)
val Typography = Typography(
    titleLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize   = 22.sp,
        lineHeight = 28.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize   = 16.sp,
        lineHeight = 24.sp
    ),
    labelSmall = TextStyle(
        fontFamily  = InterFontFamily,
        fontWeight  = FontWeight.Medium,
        fontSize    = 11.sp,
        lineHeight  = 16.sp,
        letterSpacing = 0.5.sp
    )
    // 13 estilos en total: displayLarge → labelSmall
)
```

**Tema dinámico (`Theme.kt`):**

```kotlin
@Composable
fun FlowBoardTheme(
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    content: @Composable () -> Unit
) {
    val darkModePreference by settingsViewModel.darkModeEnabled.collectAsState()
    val darkTheme = darkModePreference ?: isSystemInDarkTheme()
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography,
        content     = content
    )
}
```

---

# 4. Tecnologías y herramientas

## 4.1 Lenguajes de programación

| Lenguaje | Versión | Uso |
|---|---|---|
| Kotlin | 1.9.x | Cliente Android + servidor Ktor |
| SQL | PostgreSQL 15 | Base de datos relacional |
| YAML | — | CI/CD GitHub Actions |

## 4.2 Frameworks y librerías

### Cliente Android

| Librería | Versión | Propósito |
|---|---|---|
| Jetpack Compose | BOM 2024.x | UI declarativa |
| Material 3 | 1.2.x | Sistema de diseño |
| Hilt | 2.50 | Inyección de dependencias |
| Room | 2.6.x | Base de datos local SQLite |
| Ktor Client | 2.3.7 | HTTP + WebSocket |
| DataStore Preferences | 1.0.x | Persistencia de preferencias |
| Google Credential Manager | 1.2.x | Autenticación con Google |
| Navigation Compose | 2.7.x | Navegación entre pantallas |
| Kotlinx Coroutines | 1.7.x | Concurrencia asíncrona |
| Kotlinx Serialization | 1.6.x | Serialización JSON |
| Kotlinx DateTime | 0.5.x | Manejo de fechas |
| Coil Compose | 2.5.x | Carga de imágenes asíncrona |
| WorkManager | 2.9.x | Tareas en segundo plano |

### Servidor Ktor

| Librería | Versión | Propósito |
|---|---|---|
| Ktor Server (Netty) | 2.3.7 | Framework web + motor |
| Exposed ORM | 0.46.x | ORM para PostgreSQL |
| HikariCP | 5.x | Pool de conexiones |
| PostgreSQL Driver | 42.x | Driver JDBC |
| jBCrypt | 0.4 | Hash de contraseñas |
| Auth0 JWT | 4.x | Generación y verificación JWT |
| kotlinx-datetime | 0.5.x | Fechas en servidor |
| Logback | 1.4.x | Sistema de logging |

## 4.3 Base de datos

### PostgreSQL (servidor)

- **Proveedor:** Render.com (PostgreSQL 15, plan gratuito)
- **ORM:** JetBrains Exposed con DSL de tabla
- **Pool:** HikariCP, máx. 10 conexiones, TRANSACTION_REPEATABLE_READ
- **13 tablas:** Users, Tasks, Projects, BoardPermissions, Documents, DocumentPermissions, Notifications, PasswordResetTokens, Workspaces, WorkspaceMembers, ChatRooms, ChatParticipants, Messages

### Room / SQLite (cliente Android)

- **ORM:** Room 2.6.x con DAOs tipados
- **11 entidades:** TaskEntity, UserEntity, ProjectEntity, NotificationEntity, ChatRoomEntity, MessageEntity, ChatParticipantEntity, TypingIndicatorEntity, DocumentEntity, PendingOperationEntity, WorkspaceEntity
- **Versión del esquema:** 9

### DataStore Preferences

- **Uso:** Token JWT, userId, boardId, preferencia de modo oscuro
- **Implementación:** Preferences DataStore con claves tipadas

## 4.4 Herramientas de desarrollo

| Herramienta | Uso |
|---|---|
| Android Studio Iguana | IDE principal Android |
| IntelliJ IDEA | Desarrollo backend Ktor |
| Gradle 8.x | Sistema de build |
| Git / GitHub | Control de versiones + CI/CD |
| Render.com | Hosting backend + PostgreSQL |
| Postman | Pruebas de API REST |

## 4.5 Control de versiones y CI/CD

Pipeline de GitHub Actions (`.github/workflows/build-apk.yml`):

```yaml
name: Build APK

on:
  push:
    branches: [ master ]
  pull_request:
    branches: [ master ]
  workflow_dispatch:

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Cache Gradle
        uses: actions/cache@v4
        with:
          path: |
            ~/.gradle/caches
            ~/.gradle/wrapper
          key: gradle-${{ hashFiles('**/*.gradle*') }}

      - name: Build debug APK
        working-directory: android
        run: ./gradlew assembleDebug --no-daemon

      - name: Upload APK artifact
        uses: actions/upload-artifact@v4
        with:
          name: FlowBoard-debug-${{ github.sha }}
          path: android/app/build/outputs/apk/debug/app-debug.apk
          retention-days: 30

      - name: Create GitHub Release
        uses: softprops/action-gh-release@v2
        if: github.ref == 'refs/heads/master'
        with:
          tag_name: build-${{ github.run_number }}
          name: FlowBoard v1.0 (build ${{ github.run_number }})
          files: android/app/build/outputs/apk/debug/app-debug.apk
```

Cada push a `master` compila el APK y crea automáticamente un GitHub Release con el APK adjunto.

---

# 5. Implementación

## 5.1 Estructura del proyecto

```
FlowBoard/
├── android/
│   └── app/src/main/java/com/flowboard/
│       ├── data/
│       │   ├── auth/               # Google Auth Manager
│       │   ├── crdt/               # Motor CRDT
│       │   │   └── CRDTEngine.kt
│       │   ├── local/              # Room (SQLite)
│       │   │   ├── dao/
│       │   │   ├── entities/
│       │   │   └── FlowBoardDatabase.kt
│       │   ├── remote/             # Ktor Client
│       │   │   ├── api/
│       │   │   ├── dto/
│       │   │   └── websocket/
│       │   ├── repository/
│       │   └── workers/            # WorkManager
│       ├── di/                     # Módulos Hilt
│       │   ├── DatabaseModule.kt
│       │   ├── NetworkModule.kt
│       │   ├── RepositoryModule.kt
│       │   └── CRDTModule.kt
│       ├── domain/
│       │   ├── model/
│       │   └── repository/         # Interfaces
│       ├── presentation/
│       │   ├── ui/screens/
│       │   │   ├── auth/           # Login, Register, ForgotPassword
│       │   │   ├── tasks/          # TaskList, TaskDetail, CreateTask, Calendar
│       │   │   ├── documents/      # MyDocuments, CollaborativeDocumentV2
│       │   │   ├── projects/       # ProjectList
│       │   │   ├── profile/
│       │   │   └── settings/
│       │   └── viewmodel/          # 15+ ViewModels
│       ├── theme/
│       │   ├── Color.kt
│       │   ├── Theme.kt
│       │   └── Type.kt
│       ├── FlowBoardApp.kt         # NavHost principal
│       ├── FlowBoardApplication.kt # @HiltAndroidApp
│       └── MainActivity.kt
│
├── backend/
│   └── src/main/kotlin/com/flowboard/
│       ├── data/
│       │   ├── database/
│       │   │   ├── Tables.kt       # 13 tablas Exposed
│       │   │   └── DatabaseFactory.kt
│       │   └── models/
│       ├── domain/                 # Servicios de negocio
│       │   ├── AuthService.kt
│       │   ├── TaskService.kt
│       │   ├── DocumentService.kt
│       │   └── ...
│       ├── plugins/                # Configuración Ktor
│       │   ├── Security.kt
│       │   ├── Routing.kt
│       │   ├── Database.kt
│       │   └── WebSockets.kt
│       ├── routes/                 # Endpoints REST
│       └── Application.kt
│
├── docs/
└── .github/workflows/
    └── build-apk.yml
```

## 5.2 Módulos principales

### Autenticación (backend)

```kotlin
// AuthService.kt — Registro con BCrypt y JWT
suspend fun register(request: RegisterRequest): LoginResponse = dbQuery {
    val existing = Users
        .select { Users.email eq request.email or (Users.username eq request.username) }
        .firstOrNull()
    if (existing != null) throw IllegalArgumentException("User already exists")

    val hashedPassword = BCrypt.hashpw(request.password, BCrypt.gensalt())
    val userId = UUID.randomUUID()

    Users.insert {
        it[id]           = userId
        it[email]        = request.email
        it[username]     = request.username
        it[passwordHash] = hashedPassword
        it[createdAt]    = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    }

    LoginResponse(
        token = JwtConfig.makeToken(request.email, userId.toString(), request.username),
        user  = User(id = userId.toString(), email = request.email, ...)
    )
}

// Recuperación de contraseña — anti-enumeración de usuarios
suspend fun requestPasswordReset(email: String): Boolean = dbQuery {
    val expires = Clock.System.now().plus(15, DateTimeUnit.MINUTE).toLocalDateTime(TimeZone.UTC)
    val userExists = Users.select { Users.email eq email }.count() > 0

    if (userExists) {
        PasswordResetTokens.update({ PasswordResetTokens.email eq email }) {
            it[used] = true  // Invalida tokens anteriores
        }
        val code = (100000..999999).random().toString()
        PasswordResetTokens.insert {
            it[PasswordResetTokens.email]     = email
            it[PasswordResetTokens.code]      = code
            it[PasswordResetTokens.expiresAt] = expires
        }
        GlobalScope.launch { EmailService.sendPasswordResetEmail(email, code) }
    }
    true  // Siempre devuelve true (anti-enumeración)
}
```

### Seguridad JWT (backend)

```kotlin
// Security.kt — Plugin JWT con verificación HMAC-256
fun Application.configureSecurity() {
    install(Authentication) {
        jwt("auth-jwt") {
            realm = JwtConfig.realm
            verifier(JwtConfig.verifier)
            validate { credential ->
                val userId = credential.payload.getClaim("userId")?.asString()
                if (!userId.isNullOrEmpty()) JWTPrincipal(credential.payload) else null
            }
        }
    }
}

object JwtConfig {
    private val secret    = System.getenv("JWT_SECRET") ?: "dev-secret-key"
    private val algorithm = Algorithm.HMAC256(secret)

    val verifier = JWT.require(algorithm)
        .withIssuer(System.getenv("JWT_ISSUER") ?: "flowboard-api")
        .withAudience(System.getenv("JWT_AUDIENCE") ?: "flowboard-app")
        .build()

    fun makeToken(email: String, userId: String, username: String = ""): String =
        JWT.create()
            .withClaim("email",    email)
            .withClaim("userId",   userId)
            .withClaim("username", username)
            .sign(algorithm)
}
```

### Pool de conexiones y esquema (backend)

```kotlin
// DatabaseFactory.kt — HikariCP + parsing URL Render.com
object DatabaseFactory {
    fun init() {
        val database = Database.connect(createHikariDataSource())
        transaction(database) {
            SchemaUtils.create(
                Users, Tasks, Projects, BoardPermissions,
                Documents, DocumentPermissions, Notifications,
                PasswordResetTokens, Workspaces, WorkspaceMembers,
                ChatRooms, ChatParticipants, Messages
            )
        }
    }

    private fun createHikariDataSource(): HikariDataSource {
        val databaseUrl = System.getenv("DATABASE_URL")
        val config = HikariConfig().apply {
            driverClassName = "org.postgresql.Driver"

            if (databaseUrl?.startsWith("postgresql://") == true) {
                // Render.com entrega: postgresql://user:pass@host/db
                // JDBC requiere:       jdbc:postgresql://host/db
                val regex = Regex("postgresql://([^:]+):([^@]+)@([^/]+)/(.+)")
                val (user, pass, host, db) = regex.find(databaseUrl)!!.destructured
                val externalHost = if (host.startsWith("dpg-"))
                    "$host.oregon-postgres.render.com:5432" else "$host:5432"
                this.jdbcUrl  = "jdbc:postgresql://$externalHost/$db?sslmode=require"
                this.username = user
                this.password = pass
            } else {
                this.jdbcUrl  = databaseUrl ?: "jdbc:postgresql://localhost:5432/flowboard"
                this.username = System.getenv("DATABASE_USER") ?: "flowboard"
                this.password = System.getenv("DATABASE_PASSWORD") ?: "flowboard"
            }

            maximumPoolSize     = 10
            isAutoCommit        = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            connectionTimeout   = 30_000
            idleTimeout         = 600_000
            maxLifetime         = 1_800_000
        }
        return HikariDataSource(config)
    }

    suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}
```

### Endpoints REST de tareas (backend)

```kotlin
// TaskRoutes.kt — CRUD completo protegido con JWT
fun Route.taskRoutes(taskService: TaskService) {
    authenticate("auth-jwt") {
        route("/tasks") {

            get {  // GET /api/v1/tasks
                val userId = call.principal<JWTPrincipal>()
                    ?.payload?.getClaim("userId")?.asString()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized)
                call.respond(HttpStatusCode.OK, taskService.getAllTasksForUser(userId))
            }

            post {  // POST /api/v1/tasks
                val userId  = call.principal<JWTPrincipal>()
                    ?.payload?.getClaim("userId")?.asString()!!
                val request = call.receive<CreateTaskRequest>()
                val task    = taskService.createTask(request, userId)
                call.respond(HttpStatusCode.Created, task)
            }

            put("/{id}") {  // PUT /api/v1/tasks/{id}
                val taskId  = call.parameters["id"]!!
                val userId  = call.principal<JWTPrincipal>()
                    ?.payload?.getClaim("userId")?.asString()!!
                val request = call.receive<UpdateTaskRequest>()
                val task    = taskService.updateTask(taskId, request, userId)
                if (task != null) call.respond(HttpStatusCode.OK, task)
                else call.respond(HttpStatusCode.NotFound)
            }

            delete("/{id}") {  // DELETE /api/v1/tasks/{id}
                val taskId = call.parameters["id"]!!
                val userId = call.principal<JWTPrincipal>()
                    ?.payload?.getClaim("userId")?.asString()!!
                val deleted = taskService.deleteTask(taskId, userId)
                if (deleted) call.respond(HttpStatusCode.NoContent)
                else call.respond(HttpStatusCode.NotFound)
            }

            patch("/{id}/toggle") {  // PATCH /api/v1/tasks/{id}/toggle
                val taskId = call.parameters["id"]!!
                val userId = call.principal<JWTPrincipal>()
                    ?.payload?.getClaim("userId")?.asString()!!
                val task = taskService.toggleTaskStatus(taskId, userId)
                if (task != null) call.respond(HttpStatusCode.OK, task)
                else call.respond(HttpStatusCode.NotFound)
            }
        }
    }
}
```

### Base de datos Room (cliente Android)

```kotlin
// FlowBoardDatabase.kt — 11 entidades, versión 9
@Database(
    entities = [
        TaskEntity::class, UserEntity::class, ProjectEntity::class,
        NotificationEntity::class, ChatRoomEntity::class, MessageEntity::class,
        ChatParticipantEntity::class, TypingIndicatorEntity::class,
        DocumentEntity::class, PendingOperationEntity::class, WorkspaceEntity::class
    ],
    version = 9,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class FlowBoardDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun documentDao(): DocumentDao
    abstract fun pendingOperationDao(): PendingOperationDao
    abstract fun workspaceDao(): WorkspaceDao

    companion object {
        @Volatile private var INSTANCE: FlowBoardDatabase? = null

        fun getDatabase(context: Context): FlowBoardDatabase =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(context, FlowBoardDatabase::class.java, "flowboard_database")
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
```

### Inyección de dependencias Hilt (cliente Android)

```kotlin
// NetworkModule.kt — Hilt @Singleton para clientes HTTP y WebSocket
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides @Singleton
    fun provideHttpClient(): HttpClient = HttpClient(Android) {
        engine { connectTimeout = 30_000; socketTimeout = 30_000 }
        install(ContentNegotiation) {
            json(Json { isLenient = true; ignoreUnknownKeys = true })
        }
        install(Logging) { level = LogLevel.HEADERS }
        expectSuccess = false
    }

    // Cliente WebSocket con OkHttp — soporta WebSocket nativo de Android
    @Provides @Singleton @WebSocketClientQualifier
    fun provideWebSocketClient(): HttpClient = HttpClient(OkHttp) {
        install(WebSockets) {
            pingInterval = 30_000        // Mantener conexión activa
            maxFrameSize = Long.MAX_VALUE
        }
        install(Logging) { level = LogLevel.INFO }
    }

    @Provides @Singleton
    fun provideTaskApiService(
        @HttpClientQualifier httpClient: HttpClient,
        authRepository: AuthRepository
    ): TaskApiService = TaskApiService(httpClient, authRepository)
}
```

### ViewModel de tareas (cliente Android)

```kotlin
// TaskViewModel.kt — MVVM con StateFlow y coroutines
@HiltViewModel
class TaskViewModel @Inject constructor(
    private val taskRepository: TaskRepositoryImpl,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TaskUiState())
    val uiState: StateFlow<TaskUiState> = _uiState.asStateFlow()

    // Flujos desde Room — se actualizan automáticamente al cambiar la DB
    val allTasks = taskRepository.getAllTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingTasks = taskRepository.getTasksByStatus(false)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createTask(title: String, description: String, priority: TaskPriority = TaskPriority.MEDIUM, ...) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            taskRepository.createTask(task).fold(
                onSuccess = { _uiState.update { it.copy(isLoading = false, message = "Task created successfully") } },
                onFailure = { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
            )
        }
    }

    fun toggleTaskStatus(taskId: String) {
        viewModelScope.launch {
            taskRepository.toggleTaskStatus(taskId).fold(
                onSuccess = { _uiState.update { it.copy(message = "Task status updated") } },
                onFailure = { e -> _uiState.update { it.copy(error = e.message) } }
            )
        }
    }

    fun connectToBoard(boardId: String, token: String, userId: String) {
        viewModelScope.launch { taskRepository.connectToBoard(boardId, token, userId) }
    }

    fun disconnectFromBoard() {
        viewModelScope.launch { taskRepository.disconnectFromBoard() }
    }
}

data class TaskUiState(
    val isLoading: Boolean = false,
    val selectedTask: Task? = null,
    val error: String? = null,
    val message: String? = null
)
```

## 5.3 Funcionalidades clave

### Motor CRDT para edición colaborativa

```kotlin
// CRDTEngine.kt — Transformación Operacional para edición sin conflictos
@Singleton
class CRDTEngine @Inject constructor() {

    private val _document = MutableStateFlow<CollaborativeDocument?>(null)
    val document: StateFlow<CollaborativeDocument?> = _document.asStateFlow()

    private val vectorClock       = mutableMapOf<String, Long>()
    private val operationHistory  = mutableListOf<DocumentOperation>()
    private val appliedOperations = mutableSetOf<String>()  // Idempotencia

    fun applyOperation(operation: DocumentOperation): Boolean {
        if (appliedOperations.contains(operation.operationId)) return false

        val currentDoc = _document.value ?: return false
        val newBlocks = when (operation) {
            is AddBlockOperation          -> handleAddBlock(currentDoc.blocks, operation)
            is DeleteBlockOperation       -> handleDeleteBlock(currentDoc.blocks, operation)
            is UpdateBlockContentOperation -> handleUpdateContent(currentDoc.blocks, operation)
            is UpdateBlockFormattingOperation -> handleUpdateFormatting(currentDoc.blocks, operation)
            is MoveBlockOperation         -> handleMoveBlock(currentDoc.blocks, operation)
            is CursorMoveOperation        -> currentDoc.blocks  // Cursores no modifican el documento
            else                          -> currentDoc.blocks
        }

        _document.value = currentDoc.copy(blocks = newBlocks)
        appliedOperations.add(operation.operationId)
        operationHistory.add(operation)
        return true
    }

    // Transformación OT — ajusta la posición de operaciones concurrentes
    private fun transformContentOperations(
        op1: UpdateBlockContentOperation,
        op2: UpdateBlockContentOperation
    ): UpdateBlockContentOperation {
        if (op1.blockId != op2.blockId) return op1

        val newPosition = when {
            op2.position < op1.position ->
                op1.position + op2.content.length  // op2 insertó antes → ajustar
            op2.position == op1.position ->
                if (op1.operationId < op2.operationId) op1.position
                else op1.position + op2.content.length  // Desempate determinista por ID
            else -> op1.position
        }
        return op1.copy(position = newPosition)
    }

    fun mergeRemoteOperations(remoteOps: List<DocumentOperation>): List<DocumentOperation> {
        val localOps = getPendingOperations()
        return remoteOps
            .filter { !appliedOperations.contains(it.operationId) }
            .map { remoteOp ->
                transformOperation(remoteOp, localOps).also { applyOperation(it) }
            }
    }

    fun reset() {
        _document.value = null
        vectorClock.clear()
        operationHistory.clear()
        appliedOperations.clear()
    }
}
```

### Broadcast WebSocket en tareas (backend)

```kotlin
// TaskService.kt — Evento TASK_CREATED propagado a todos los miembros del tablero
suspend fun createTask(request: CreateTaskRequest, userId: String): Task {
    val task = dbQuery { /* inserción Exposed */ }

    if (webSocketManager != null && task.projectId != null) {
        webSocketManager.broadcastToRoom(
            boardId = task.projectId!!,
            message = TaskCreatedMessage(
                timestamp = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
                boardId   = task.projectId!!,
                task      = task.toSnapshot()
            )
        )
    }
    return task
}
```

### Animaciones (cliente Android)

```kotlin
// FlowBoardApp.kt — Transición uniforme en las 18 rutas (tween 280 ms)
composable(
    route = "task_list",
    enterTransition = { slideInHorizontally(tween(280)) { it } + fadeIn(tween(280)) },
    exitTransition  = { slideOutHorizontally(tween(280)) { -it } + fadeOut(tween(280)) }
)

// TaskListScreen.kt — Color animado al completar tarea
val backgroundColor by animateColorAsState(
    targetValue    = if (task.isCompleted) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                     else MaterialTheme.colorScheme.surface,
    animationSpec  = tween(300),
    label          = "task_bg_color"
)

// CollaborativeCursor.kt — Pulso infinito del cursor de colaboradores
val alpha by rememberInfiniteTransition(label = "cursor_pulse").animateFloat(
    initialValue   = 1f,
    targetValue    = 0.3f,
    animationSpec  = infiniteRepeatable(tween(800), RepeatMode.Reverse),
    label          = "cursor_alpha"
)

// TaskListScreen.kt — Animación de inserción/eliminación en LazyColumn
LazyColumn {
    items(tasks, key = { it.id }) { task ->
        TaskItem(task = task, modifier = Modifier.animateItem())
    }
}

// FlowBoardApp.kt — Splash screen con fundido de salida (1,4 s)
AnimatedVisibility(visible = showSplash, exit = fadeOut(tween(400))) {
    SplashScreen()
}
LaunchedEffect(Unit) { delay(1400); showSplash = false }
```

---

# 6. Pruebas

## 6.1 Plan de pruebas

| Nivel | Tipo | Herramienta | Alcance |
|---|---|---|---|
| Unitario | JUnit 4 + Mockito | Android JVM | ViewModels, lógica de dominio |
| Integración | Room in-memory | Android JVM | DAOs y repositorios |
| Manual | Postman + dispositivo | Manual | API REST + UI |

## 6.2 Pruebas unitarias del TaskViewModel

```kotlin
// TaskViewModelTest.kt — 9 casos con StandardTestDispatcher
@OptIn(ExperimentalCoroutinesApi::class)
class TaskViewModelTest {

    @Mock private lateinit var taskRepository: TaskRepository
    private lateinit var viewModel: TaskViewModel
    private val testDispatcher = StandardTestDispatcher()

    private val sampleTask = Task(
        id = "test-id", title = "Test Task",
        priority = TaskPriority.MEDIUM, isCompleted = false, ...
    )

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        whenever(taskRepository.getAllTasks()).thenReturn(flowOf(listOf(sampleTask)))
        viewModel = TaskViewModel(taskRepository)
    }

    @After fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `initial state is correct`() {
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.selectedTask)
        assertNull(state.error)
    }

    @Test
    fun `loadTaskById updates selected task`() = runTest {
        whenever(taskRepository.getTaskById("test-id")).thenReturn(sampleTask)
        viewModel.loadTaskById("test-id")
        advanceUntilIdle()
        assertEquals(sampleTask, viewModel.uiState.value.selectedTask)
    }

    @Test
    fun `createTask shows loading state`() = runTest {
        whenever(taskRepository.createTask(any())).thenReturn(Result.success(sampleTask))
        viewModel.createTask("New Task", "Description")
        assertTrue(viewModel.uiState.value.isLoading)
        advanceUntilIdle()
        assertEquals("Task created successfully", viewModel.uiState.value.message)
    }

    @Test
    fun `createTask handles failure`() = runTest {
        whenever(taskRepository.createTask(any()))
            .thenReturn(Result.failure(RuntimeException("Creation failed")))
        viewModel.createTask("New Task", "Description")
        advanceUntilIdle()
        assertEquals("Creation failed", viewModel.uiState.value.error)
    }

    @Test
    fun `toggleTaskStatus calls repository`() = runTest {
        whenever(taskRepository.toggleTaskStatus("test-id")).thenReturn(Result.success(sampleTask))
        viewModel.toggleTaskStatus("test-id")
        advanceUntilIdle()
        verify(taskRepository).toggleTaskStatus("test-id")
        assertEquals("Task status updated", viewModel.uiState.value.message)
    }

    @Test
    fun `deleteTask shows success message`() = runTest {
        whenever(taskRepository.deleteTask("test-id")).thenReturn(Result.success(Unit))
        viewModel.deleteTask("test-id")
        advanceUntilIdle()
        assertEquals("Task deleted successfully", viewModel.uiState.value.message)
    }

    @Test
    fun `syncTasks handles success`() = runTest {
        whenever(taskRepository.syncTasks()).thenReturn(Result.success(Unit))
        viewModel.syncTasks()
        advanceUntilIdle()
        assertEquals("Tasks synced successfully", viewModel.uiState.value.message)
    }

    @Test
    fun `clearError resets error state`() {
        viewModel.clearError()
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `clearMessage resets message state`() = runTest {
        whenever(taskRepository.createTask(any())).thenReturn(Result.success(sampleTask))
        viewModel.createTask("Test", "Description")
        advanceUntilIdle()
        viewModel.clearMessage()
        assertNull(viewModel.uiState.value.message)
    }
}
```

## 6.3 Pruebas manuales de la API REST

| ID | Endpoint | Método | Resultado esperado | Estado |
|---|---|---|---|---|
| TC-01 | /auth/register | POST | 201 + token JWT | ✅ |
| TC-02 | /auth/register (email duplicado) | POST | 409 Conflict | ✅ |
| TC-03 | /auth/login | POST | 200 + token JWT | ✅ |
| TC-04 | /auth/login (contraseña incorrecta) | POST | 401 Unauthorized | ✅ |
| TC-05 | /tasks (con token) | GET | 200 + lista | ✅ |
| TC-06 | /tasks (sin token) | GET | 401 Unauthorized | ✅ |
| TC-07 | /tasks | POST | 201 + tarea creada | ✅ |
| TC-08 | /tasks/{id} | PUT | 200 + tarea actualizada | ✅ |
| TC-09 | /tasks/{id} | DELETE | 204 No Content | ✅ |
| TC-10 | /tasks/{id}/toggle | PATCH | 200 + estado cambiado | ✅ |
| TC-11 | /auth/forgot-password | POST | 200 (siempre) | ✅ |
| TC-12 | /auth/reset-password (OTP válido) | POST | 200 OK | ✅ |
| TC-13 | /auth/reset-password (OTP expirado) | POST | 400 Bad Request | ✅ |

## 6.4 Errores encontrados y soluciones

| Error | Causa | Solución |
|---|---|---|
| `ClassCastException` en Room | Cambio de tipo de campo en entidad | Incrementar versión de esquema a 9 con `fallbackToDestructiveMigration()` |
| DB no disponible en Docker build | Inicialización en arranque | Diferir al evento `ApplicationStarted` de Ktor |
| URL Render.com no reconocida | Formato `postgresql://` vs `jdbc:postgresql://` | Parser con regex en `DatabaseFactory.kt` |
| WebSocket desconexión por inactividad | Sin ping/pong | `pingInterval = 30_000` en configuración Ktor WS |
| Conflicto de edición concurrente | Sin transformación OT | Motor CRDT con `transformContentOperations()` |

---

# 7. Despliegue

## 7.1 Requisitos de despliegue

### Servidor (Render.com)

Variables de entorno necesarias:

| Variable | Descripción |
|---|---|
| `DATABASE_URL` | URL de PostgreSQL: `postgresql://user:pass@host/db` |
| `JWT_SECRET` | Clave HMAC-256 (mínimo 32 caracteres) |
| `JWT_ISSUER` | Emisor del token (ej. `flowboard-api`) |
| `JWT_AUDIENCE` | Audiencia del token (ej. `flowboard-app`) |
| `RESEND_API_KEY` | Clave de API de Resend para emails |

### Cliente Android

- Android 8.0 (API 26) o superior
- Google Play Services instalado
- Acceso a internet

## 7.2 Proceso de despliegue

### Backend en Render.com

1. Hacer push del repositorio a GitHub
2. En Render.com → New Web Service → conectar repositorio → Runtime: Docker
3. Crear base de datos PostgreSQL en Render.com (región Oregon)
4. Copiar el Internal Database URL a la variable `DATABASE_URL`
5. Configurar el resto de variables de entorno
6. Deploy → Render construye la imagen Docker y arranca Ktor

### Build del APK (GitHub Actions)

Cada push a `master` genera automáticamente un APK y lo publica como GitHub Release. No requiere intervención manual.

## 7.3 Manual de usuario

### Primeros pasos

**Registro:** Abrir la app → "Crear cuenta" → introducir email, username y contraseña → "Registrarse"

**Login con Google:** Pantalla de login → "Continuar con Google" → seleccionar cuenta

**Recuperar contraseña:** Login → "¿Olvidaste tu contraseña?" → introducir email → revisar email con código OTP → introducir código + nueva contraseña

### Gestión de tareas

- **Crear tarea:** Botón "+" en pantalla de Tareas
- **Completar tarea:** Pulsar el checkbox de la tarea
- **Filtrar:** Pestañas "Todas / Pendientes / Completadas / Vencidas"
- **Vista calendario:** Sección Calendario → visualizar eventos

### Editor de documentos

- **Nuevo documento:** "+" en sección Documentos
- **Añadir bloque:** "/" al inicio de línea → menú de tipos de bloque
- **Tipos disponibles:** Texto, H1–H6, Lista, Todo, Código, Cita, Divisor, Imagen, Vídeo, Audio
- **Colaborar:** Menú "···" → Compartir → buscar usuario → asignar rol

### Workspaces

- **Crear:** Sección Workspaces → "+"
- **Unirse:** Workspaces → "Unirse" → código de 12 caracteres

---

# 8. Planificación y gestión

## 8.1 Metodología

El proyecto se ha desarrollado con una metodología **ágil iterativa** con sprints de dos semanas. El backlog se gestionó mediante GitHub Issues y Milestones.

## 8.2 Cronograma

| Semana | Fase | Tareas principales |
|---|---|---|
| 1–2 | Análisis y diseño | Requisitos, arquitectura, E-R, wireframes |
| 3–4 | Infraestructura | Setup Android (Hilt, Room, Compose), setup Ktor |
| 5 | Autenticación | BCrypt, JWT, Google Sign-In, OTP email |
| 6–7 | Tareas y proyectos | CRUD completo, tablero Kanban, WebSocket |
| 8–10 | Editor colaborativo | Motor CRDT, editor de bloques, WebSocket CRDT |
| 11–12 | Workspaces y chat | Workspaces, invitaciones, chat integrado |
| 13–14 | Pulido y pruebas | Animaciones M3, modo oscuro, CI/CD, tests |
| 15 | Documentación | Redacción del documento PIM |

**Dedicación total estimada:** 350 horas

---

# 9. Conclusiones

## 9.1 Objetivos alcanzados

El proyecto ha cumplido todos los objetivos planteados:

- **Acceso a Datos:** Capa dual de persistencia implementada con éxito: 13 tablas PostgreSQL con Exposed ORM + HikariCP en servidor, y 11 entidades Room + DataStore en cliente.
- **Desarrollo de Interfaces:** Interfaz declarativa profesional con Jetpack Compose, Material Design 3, modo oscuro/claro dinámico, tipografía Inter y animaciones fluidas en navegación y componentes.
- **Programación Multimedia y Móviles:** El motor CRDT con Transformación Operacional es el logro técnico más significativo: permite edición colaborativa sin conflictos en tiempo real mediante WebSocket.
- **Programación de Procesos y Servicios:** Servidor Ktor completamente asíncrono con coroutines, 40+ endpoints REST bajo `/api/v1/`, autenticación JWT en todas las rutas protegidas.
- **Empleabilidad:** Google Sign-In con Credential Manager siguiendo los estándares Android modernos.
- **Nube Pública:** Backend en Render.com con CI/CD automatizado que genera APK y release en cada push a master.

## 9.2 Dificultades encontradas

1. **Motor CRDT:** El caso de dos usuarios editando la misma posición del mismo bloque fue el más complejo. Solución: usar el ID de operación como desempate determinista.
2. **URL de Render.com:** El formato `postgresql://` debe convertirse a `jdbc:postgresql://` con hostname externo, ya que el plan gratuito no tiene red privada.
3. **Inicialización lazy de la DB:** Durante el build Docker la base de datos no es accesible, por lo que la inicialización se difirió al evento `ApplicationStarted`.
4. **Estado WebSocket:** Gestionar reconexiones automáticas y presencia de usuarios requirió un `StateFlow` dedicado para el estado de conexión.

## 9.3 Mejoras futuras

- **Exportación a PDF:** Usar `PrintManager` de Android para exportar documentos nativamente.
- **FCM (Firebase Cloud Messaging):** Notificaciones push reales con la app en segundo plano.
- **Modo offline completo:** Sincronizar operaciones pendientes (`PendingOperationEntity` ya creada en Room) al recuperar conexión.
- **Cliente web:** Compose Multiplatform o React para acceso desde navegador.
- **Expiración de JWT:** Implementar refresh tokens con ventana de expiración.
- **Cifrado E2E:** Cifrado de extremo a extremo para chat y documentos privados.

---

# 10. Anexos

## 10.1 Referencia completa de la API REST

**Base URL:** `https://flowboard-api-phrk.onrender.com/api/v1`

| Recurso | Método | Endpoint | Auth | Descripción |
|---|---|---|---|---|
| Auth | POST | /auth/register | ❌ | Registrar usuario |
| Auth | POST | /auth/login | ❌ | Login email/contraseña |
| Auth | POST | /auth/google | ❌ | Login con Google |
| Auth | POST | /auth/logout | ✅ | Cerrar sesión |
| Auth | POST | /auth/forgot-password | ❌ | Solicitar OTP |
| Auth | POST | /auth/reset-password | ❌ | Confirmar OTP + nueva contraseña |
| Tasks | GET | /tasks | ✅ | Listar tareas del usuario |
| Tasks | GET | /tasks/{id} | ✅ | Obtener tarea |
| Tasks | POST | /tasks | ✅ | Crear tarea |
| Tasks | PUT | /tasks/{id} | ✅ | Actualizar tarea |
| Tasks | DELETE | /tasks/{id} | ✅ | Eliminar tarea |
| Tasks | PATCH | /tasks/{id}/toggle | ✅ | Alternar completado |
| Tasks | GET | /tasks/events | ✅ | Eventos entre fechas |
| Projects | GET | /projects | ✅ | Listar proyectos |
| Projects | POST | /projects | ✅ | Crear proyecto |
| Projects | GET | /projects/{id} | ✅ | Obtener proyecto |
| Projects | PUT | /projects/{id} | ✅ | Actualizar proyecto |
| Projects | DELETE | /projects/{id} | ✅ | Eliminar proyecto |
| Documents | GET | /documents | ✅ | Listar documentos |
| Documents | POST | /documents | ✅ | Crear documento |
| Documents | GET | /documents/{id} | ✅ | Obtener documento |
| Documents | PUT | /documents/{id} | ✅ | Actualizar documento |
| Documents | DELETE | /documents/{id} | ✅ | Eliminar documento |
| Workspaces | GET | /workspaces | ✅ | Listar workspaces |
| Workspaces | POST | /workspaces | ✅ | Crear workspace |
| Workspaces | POST | /workspaces/join | ✅ | Unirse con código |
| Chat | GET | /chat/rooms | ✅ | Listar salas |
| Chat | POST | /chat/rooms | ✅ | Crear sala |
| Chat | GET | /chat/rooms/{id}/messages | ✅ | Mensajes de sala |
| Notifications | GET | /notifications | ✅ | Listar notificaciones |
| Notifications | PATCH | /notifications/{id}/read | ✅ | Marcar como leída |
| Permissions | POST | /permissions/document | ✅ | Compartir documento |
| WebSocket | WS | /ws/{boardId} | ✅ | Eventos de tareas en tiempo real |
| WebSocket | WS | /document-ws/{docId} | ✅ | CRDT de documentos |

## 10.2 Repositorio y recursos

- **Repositorio GitHub:** https://github.com/paulopnun/flowboard
- **Backend producción:** https://flowboard-api-phrk.onrender.com
- **APK:** Sección Releases del repositorio

## 10.3 Cumplimiento de Resultados de Aprendizaje (PIM)

| Módulo | RA | Funcionalidad en FlowBoard |
|---|---|---|
| Acceso a Datos | RA02 | PostgreSQL + Exposed ORM + HikariCP + Room SQLite + DataStore |
| Desarrollo de Interfaces | RA01 | Jetpack Compose + NavHost 18 rutas + Material 3 |
| Desarrollo de Interfaces | RA02 | Color.kt (paleta light/dark), Type.kt (Inter, 13 estilos) |
| Desarrollo de Interfaces | RA03 | tween(280ms), animateColorAsState, animateItem, infiniteTransition |
| Desarrollo de Interfaces | RA05 | strings.xml, soporte multi-idioma |
| Desarrollo de Interfaces | RA06 | contentDescription, semántica Compose |
| Prog. Multimedia y Móviles | RA03 | Motor CRDT + WebSocket en tiempo real |
| Prog. Multimedia y Móviles | RA04 | Notificaciones del sistema + deep links |
| Prog. Multimedia y Móviles | RA05 | Coil (imágenes), bloques imagen/vídeo/audio |
| Prog. Procesos y Servicios | RA04 | Ktor asíncrono + coroutines + 40+ endpoints REST |
| Prog. Procesos y Servicios | RA05 | JWT HMAC-256 + BCrypt + roles viewer/editor/owner |
| Empleabilidad II | RA02 | Google Sign-In con Credential Manager |
| Sostenibilidad | RA06 | HikariCP pooling + lazy init DB + WorkManager |
| Nube Pública | RA04 | Render.com (backend + PostgreSQL) + CI/CD GitHub Actions |

---

# 11. Bibliografía

## Documentación oficial

- Android Developers. (2024). *Jetpack Compose documentation*. Google. https://developer.android.com/jetpack/compose
- Android Developers. (2024). *Room Persistence Library*. Google. https://developer.android.com/training/data-storage/room
- Android Developers. (2024). *Hilt dependency injection*. Google. https://developer.android.com/training/dependency-injection/hilt-android
- Android Developers. (2024). *Material Design 3 for Android*. Google. https://m3.material.io/develop/android
- Android Developers. (2024). *Credential Manager API*. Google. https://developer.android.com/identity/sign-in/credential-manager
- JetBrains. (2024). *Ktor documentation*. JetBrains. https://ktor.io/docs
- JetBrains. (2024). *Exposed ORM documentation*. JetBrains. https://github.com/JetBrains/Exposed/wiki
- JetBrains. (2024). *Kotlin Coroutines Guide*. JetBrains. https://kotlinlang.org/docs/coroutines-guide.html
- JetBrains. (2024). *kotlinx.serialization*. JetBrains. https://kotlinlang.org/docs/serialization.html

## Artículos y recursos técnicos

- Shapiro, M., Preguiça, N., Baquero, C., & Zawirski, M. (2011). *Conflict-free Replicated Data Types*. INRIA. https://hal.inria.fr/inria-00609399
- Auth0. (2024). *Introduction to JSON Web Tokens*. https://jwt.io/introduction
- Render. (2024). *Deploying applications on Render*. https://render.com/docs
- Google. (2024). *Sign in with Google for Android*. https://developers.google.com/identity/sign-in/android
- Resend. (2024). *Resend Email API documentation*. https://resend.com/docs

## Librerías de terceros

- Zaxxer. (2024). *HikariCP — High-performance JDBC connection pool* [Software]. https://github.com/brettwooldridge/HikariCP
- Auth0. (2024). *java-jwt* [Software]. https://github.com/auth0/java-jwt
- mindrot. (2024). *jBCrypt* [Software]. https://www.mindrot.org/projects/jBCrypt/
- Coil. (2024). *Coil — Image loading for Android* [Software]. https://coil-kt.github.io/coil/
- Square. (2024). *OkHttp* [Software]. https://square.github.io/okhttp/

---

*Documento elaborado conforme a la rúbrica del Proyecto Intermodular (PIM) — 2.º DAM. Todo el contenido técnico está basado en el código real del repositorio https://github.com/paulopnun/flowboard.*