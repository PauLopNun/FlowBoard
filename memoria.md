# FlowBoard — Proyecto Intermodular (PIM)
### Ciclo Formativo de Grado Superior — Desarrollo de Aplicaciones Multiplataforma (DAM)

---

**Autor:** [Tu nombre completo]  
**Tutor:** [Nombre del tutor]  
**Centro educativo:** [Nombre del centro]  
**Curso académico:** 2024–2025  
**Fecha de entrega:** [Fecha]  
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
| `Application.