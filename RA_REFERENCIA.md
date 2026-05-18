# Resultados de Aprendizaje — Correspondencia con el código de FlowBoard

**Autor:** Pau López Núñez  
**Módulo:** Proyecto Intermodular — 2.º DAM  
**Repositorio:** https://github.com/paulopnun/flowboard

El presente documento recoge, para cada resultado de aprendizaje (RA) y criterio de evaluación del Proyecto Intermodular, la implementación concreta realizada en FlowBoard y la localización exacta en el código fuente. Los archivos se indican con su ruta relativa al repositorio y el número de línea donde comienza la implementación relevante.

---

## Glosario de términos técnicos

**PostgreSQL.** Base de datos relacional utilizada en el servidor. Almacena de forma persistente todos los datos de la aplicación (usuarios, tareas, documentos, workspaces) y está desplegada en Render.com.

**Room / SQLite.** Base de datos embebida en el cliente Android. Guarda una réplica local de los datos para permitir lectura sin conexión a red.

**Exposed ORM.** Librería de JetBrains que permite definir el esquema de la base de datos y escribir consultas en Kotlin, sin necesidad de SQL explícito en el código de la aplicación.

**HikariCP.** Gestor de pool de conexiones para PostgreSQL. Mantiene un número configurable de conexiones abiertas y las reutiliza entre peticiones, evitando el coste de abrir una conexión nueva en cada operación.

**DAO (Data Access Object).** Interfaz anotada de Room que agrupa las consultas a la base de datos local. Cada entidad tiene su DAO correspondiente: `TaskDao`, `DocumentDao`, etc.

**Trigger / procedimiento almacenado.** Fragmento de código PL/pgSQL que se ejecuta automáticamente en la base de datos ante un evento concreto. En FlowBoard, el trigger `set_updated_at()` actualiza el campo `updated_at` de forma automática cada vez que se modifica una tarea o documento.

**API REST.** Interfaz de comunicación entre el cliente Android y el servidor, basada en el protocolo HTTP. Las operaciones de lectura, creación, modificación y borrado se realizan mediante los verbos GET, POST, PUT y DELETE sobre más de 40 endpoints bajo `/api/v1/`.

**WebSocket.** Protocolo de comunicación que mantiene una conexión bidireccional persistente entre cliente y servidor. Se emplea para la sincronización de cambios en tiempo real en tareas y documentos colaborativos.

**JWT (JSON Web Token).** Estándar abierto (RFC 7519) para la transmisión segura de información de autenticación entre partes. El servidor emite un token firmado en el momento del login y el cliente lo adjunta en la cabecera `Authorization` de cada petición protegida.

**BCrypt.** Función de hash unidireccional con sal aleatoria, utilizada para almacenar las contraseñas de los usuarios de forma que no puedan recuperarse a partir del valor guardado en la base de datos.

**HMAC-SHA256.** Algoritmo de firma simétrica empleado para garantizar la integridad y autenticidad de los tokens JWT.

**TLS / HTTPS.** Protocolo de cifrado de capa de transporte. Toda la comunicación entre el cliente Android y el servidor se realiza sobre HTTPS, y la conexión entre el servidor Ktor y PostgreSQL incluye `sslmode=require`.

**OTP (One-Time Password).** Código numérico de seis dígitos de un solo uso, generado aleatoriamente y enviado por correo electrónico para el flujo de recuperación de contraseña. Su validez es de 15 minutos.

**Roles de acceso.** Sistema de permisos con distintos niveles: en documentos, `viewer` / `editor` / `owner`; en workspaces, `MEMBER` / `ADMIN` / `OWNER`.

**Jetpack Compose.** Framework declarativo de Google para construir interfaces de usuario en Android mediante código Kotlin, utilizado en la totalidad de las pantallas de FlowBoard.

**Material Design 3.** Sistema de diseño de Google que define la paleta de colores, tipografía, formas y componentes de la interfaz. FlowBoard implementa modo oscuro y modo claro mediante los tokens semánticos del sistema.

**Scaffold.** Componente de Compose que estructura una pantalla gestionando la barra superior (`TopAppBar`), la barra de navegación inferior y el área de contenido, garantizando el tratamiento correcto de los `innerPadding`.

**rememberSaveable.** Mecanismo de Compose que persiste el estado de un composable ante cambios de configuración del dispositivo, como la rotación de pantalla.

**ContentScale.Crop.** Propiedad de escala de imagen que recorta los bordes sobrantes para que la imagen ocupe exactamente el espacio asignado sin deformarse.

**Modifier.weight.** Modificador de Compose que distribuye el espacio disponible entre componentes de forma proporcional al peso asignado a cada uno.

**Animación Spring.** Modelo de animación basado en física de resorte. Produce transiciones con aceleración y deceleración naturales, con opción de rebote amortiguado al final del movimiento.

**StateFlow / SharedFlow.** Flujos reactivos de Kotlin Coroutines. Permiten que la interfaz de usuario se actualice automáticamente cuando cambia el estado subyacente, sin necesidad de refresco manual.

**Coil.** Librería para la carga asíncrona de imágenes desde URL en composables de Jetpack Compose.

**Hilt.** Framework de inyección de dependencias para Android, basado en Dagger. Gestiona el ciclo de vida de los objetos de la aplicación (repositorios, ViewModels, clientes de red) y su suministro a cada componente.

**ViewModel.** Clase de la arquitectura Android que mantiene el estado de la interfaz y la lógica de presentación, sobreviviendo a los cambios de configuración del dispositivo.

**MVVM + Clean Architecture.** Patrón arquitectónico empleado en el cliente Android. Separa el código en tres capas: datos (Room, API REST), dominio (repositorios, casos de uso) y presentación (composables, ViewModels).

**CRDT (Conflict-free Replicated Data Type).** Estructura de datos que permite la edición concurrente de un documento por múltiples usuarios sin necesidad de coordinación centralizada. La implementación en FlowBoard incorpora Transformación Operacional (OT) para resolver conflictos de forma determinista.

**Coroutines de Kotlin.** Mecanismo de concurrencia ligera que permite ejecutar operaciones asíncronas (acceso a red, base de datos) sin bloquear el hilo principal de la interfaz.

**Ktor.** Framework web para Kotlin desarrollado por JetBrains, utilizado como servidor HTTP y WebSocket en el backend de FlowBoard.

**Render.com.** Plataforma de alojamiento en la nube donde está desplegado el servidor Ktor y la base de datos PostgreSQL del proyecto.

**CI/CD (GitHub Actions).** Pipeline de integración y despliegue continuo configurado en el repositorio. Compila el APK de Android y publica una nueva versión en GitHub Releases de forma automática en cada push a la rama principal.

**Cloudinary.** Servicio de almacenamiento y distribución de imágenes en la nube, utilizado para guardar las imágenes adjuntas a documentos y perfiles de usuario.

**DataStore.** Mecanismo de almacenamiento de preferencias de usuario en Android. Persiste el token JWT, el identificador de usuario y la preferencia de modo oscuro/claro.

**ASG.** Acrónimo de los criterios de sostenibilidad empresarial: Ambiental, Social y de Gobernanza.

---

## Tabla de correspondencia RA — código fuente

Los archivos indicados son rutas relativas a la raíz del repositorio. El número tras el separador es la línea de inicio de la implementación en cuestión.

| Módulo | RA | Criterio | Implementación en FlowBoard | Archivo : línea |
|---|---|---|---|---|
| Acceso a Datos | RA02 | a | Justificación de la elección de Room (Android) y Exposed con HikariCP (servidor) frente a conectores alternativos | [DatabaseFactory.kt](backend/src/main/kotlin/com/flowboard/data/database/DatabaseFactory.kt) : 84 |
| Acceso a Datos | RA02 | b | Dos gestores: SQLite embebido en el cliente (Room) y PostgreSQL independiente en la nube (Render) | [TaskHiveDatabase.kt](android/app/src/main/java/com/flowboard/data/local/TaskHiveDatabase.kt) : 26 |
| Acceso a Datos | RA02 | c | Room como conector para Android; Exposed ORM con driver JDBC 42.x y HikariCP para el servidor | [build.gradle (android)](android/app/build.gradle) |
| Acceso a Datos | RA02 | d | Apertura de conexión a PostgreSQL mediante `Database.connect(createHikariDataSource())` y a Room mediante `Room.databaseBuilder(…)` | [DatabaseFactory.kt](backend/src/main/kotlin/com/flowboard/data/database/DatabaseFactory.kt) : 26 |
| Acceso a Datos | RA02 | e | Definición de 13 tablas en el servidor (Exposed DSL) y 11 entidades en el cliente (Room) | [Tables.kt](backend/src/main/kotlin/com/flowboard/data/database/Tables.kt) |
| Acceso a Datos | RA02 | f | CRUD completo sobre tareas y documentos: endpoints REST en el servidor y métodos DAO en el cliente | [TaskRoutes.kt](backend/src/main/kotlin/com/flowboard/routes/TaskRoutes.kt) : 50 · [TaskDao.kt](android/app/src/main/java/com/flowboard/data/local/dao/TaskDao.kt) : 36 |
| Acceso a Datos | RA02 | g | Resultados de consulta mapeados a entidades Room, DTOs y modelos de dominio | [TaskEntity.kt](android/app/src/main/java/com/flowboard/data/local/entities/TaskEntity.kt) · [TaskDto.kt](android/app/src/main/java/com/flowboard/data/remote/dto/TaskDto.kt) |
| Acceso a Datos | RA02 | h | Consultas con `@Query` en Room: `getAllTasks()`, `getEventsBetweenDates()`, `getOverdueTasks()`, etc. | [TaskDao.kt](android/app/src/main/java/com/flowboard/data/local/dao/TaskDao.kt) : 10 |
| Acceso a Datos | RA02 | i | Liberación automática de la conexión al pool al finalizar cada bloque `dbQuery { }` | [DatabaseFactory.kt](backend/src/main/kotlin/com/flowboard/data/database/DatabaseFactory.kt) : 209 |
| Acceso a Datos | RA02 | j | Gestión de transacciones mediante `newSuspendedTransaction` con `isAutoCommit = false` y aislamiento `REPEATABLE_READ` | [DatabaseFactory.kt](backend/src/main/kotlin/com/flowboard/data/database/DatabaseFactory.kt) : 135 |
| Acceso a Datos | RA02 | k | Procedimiento almacenado `set_updated_at()` en PL/pgSQL con triggers sobre las tablas `tasks` y `documents` | [DatabaseFactory.kt](backend/src/main/kotlin/com/flowboard/data/database/DatabaseFactory.kt) : 182 |
| Des. Interfaces | RA01 | b | Veinticinco pantallas desarrolladas con Android Studio y Jetpack Compose mediante editor visual con vista previa en tiempo real | [screens/](android/app/src/main/java/com/flowboard/presentation/ui/screens) |
| Des. Interfaces | RA01 | e | Archivos de tema generados por el editor y adaptados manualmente: paleta de colores, escala tipográfica y radios de curvatura | [Theme.kt](android/app/src/main/java/com/flowboard/theme/Theme.kt) · [Color.kt](android/app/src/main/java/com/flowboard/theme/Color.kt) · [Type.kt](android/app/src/main/java/com/flowboard/theme/Type.kt) |
| Des. Interfaces | RA01 | g | Asociación de eventos a acciones mediante lambdas: `onClick`, `onValueChange`, `onCheckedChange`, etc. | [TaskListScreen.kt](android/app/src/main/java/com/flowboard/presentation/ui/screens/tasks/TaskListScreen.kt) : 35 |
| Des. Interfaces | RA02 | a | Identificación de herramientas de aprendizaje automático: asistente de IA en el editor (API de Claude) y Google Sign-In | [AiRoutes.kt](backend/src/main/kotlin/com/flowboard/routes/AiRoutes.kt) · [AiApiService.kt](android/app/src/main/java/com/flowboard/data/remote/api/AiApiService.kt) |
| Des. Interfaces | RA02 | b | Panel de IA integrado en el editor de documentos con procesamiento de lenguaje natural y acciones predefinidas | [CollaborativeDocumentScreenV2.kt](android/app/src/main/java/com/flowboard/presentation/ui/screens/documents/CollaborativeDocumentScreenV2.kt) |
| Des. Interfaces | RA02 | f | Representación en tiempo real de los cursores de otros colaboradores superpuestos sobre los bloques del editor | [CollaborativeDocumentScreenV2.kt](android/app/src/main/java/com/flowboard/presentation/ui/screens/documents/CollaborativeDocumentScreenV2.kt) : 556 |
| Des. Interfaces | RA03 | a | Herramientas de diseño y prueba: Figma para el prototipado previo, Android Studio para el desarrollo y `ComposeTestRule` para los tests instrumentados | [TaskListScreenTest.kt](android/app/src/androidTest/java/com/flowboard/TaskListScreenTest.kt) |
| Des. Interfaces | RA03 | c | Definición de parámetros con valores por defecto en composables y modelos de dominio | [TaskCard.kt](android/app/src/main/java/com/flowboard/presentation/ui/components/TaskCard.kt) : 29 |
| Des. Interfaces | RA05 | a | Estructura del informe PDF: título, bloques de contenido con tipografía diferenciada según tipo y paginación | [DocumentExporter.kt](android/app/src/main/java/com/flowboard/presentation/ui/screens/documents/DocumentExporter.kt) : 78 |
| Des. Interfaces | RA05 | b | Generación de informes en dos formatos a partir de la misma fuente de datos: PDF nativo y Markdown | [DocumentExporter.kt](android/app/src/main/java/com/flowboard/presentation/ui/screens/documents/DocumentExporter.kt) : 16 |
| Des. Interfaces | RA05 | c | Filtrado por tipo de bloque para aplicar formato específico en la exportación: encabezados, listas, bloques de código, citas | [DocumentExporter.kt](android/app/src/main/java/com/flowboard/presentation/ui/screens/documents/DocumentExporter.kt) : 20 |
| Des. Interfaces | RA06 | a | Atributo `contentDescription` en todos los componentes interactivos para compatibilidad con lectores de pantalla; manual de usuario en §7.4 de la memoria | [screens/](android/app/src/main/java/com/flowboard/presentation/ui/screens) |
| Des. Interfaces | RA06 | b | Documentación en formato Markdown; comentarios del código en inglés; bloques KDoc en las clases principales | [CRDTEngine.kt](android/app/src/main/java/com/flowboard/data/crdt/CRDTEngine.kt) : 1 |
| PMM | RA03 | a | Entornos de desarrollo multimedia utilizados: Android Studio, IntelliJ IDEA, Cloudinary y protocolo WebSocket | [build.gradle](android/app/build.gradle) · [build-apk.yml](.github/workflows/build-apk.yml) |
| PMM | RA03 | b | Captura de imagen desde URI del sistema, compresión JPEG y almacenamiento en Cloudinary mediante petición HTTP | [CloudinaryUploader.kt](android/app/src/main/java/com/flowboard/presentation/ui/util/CloudinaryUploader.kt) : 29 |
| PMM | RA03 | c | Conversión de URI local a JPEG comprimido para su subida; conversión de bloques de documento a PDF y a Markdown | [CloudinaryUploader.kt](android/app/src/main/java/com/flowboard/presentation/ui/util/CloudinaryUploader.kt) : 29 · [DocumentExporter.kt](android/app/src/main/java/com/flowboard/presentation/ui/screens/documents/DocumentExporter.kt) : 63 |
| PMM | RA03 | d | Motor CRDT que procesa en tiempo real las operaciones sobre bloques del documento: inserción, edición, movimiento y eliminación | [CRDTEngine.kt](android/app/src/main/java/com/flowboard/data/crdt/CRDTEngine.kt) : 51 |
| PMM | RA03 | e | Gestión de estados de conexión WebSocket mediante `ConnectionState`; captura y clasificación de excepciones en el flujo de autenticación con Google | [DocumentWebSocketClient.kt](android/app/src/main/java/com/flowboard/data/remote/websocket/DocumentWebSocketClient.kt) : 29 · [GoogleAuthManager.kt](android/app/src/main/java/com/flowboard/data/auth/GoogleAuthManager.kt) : 71 |
| PMM | RA03 | f | Animaciones con física de resorte en la expansión de tarjetas y en el indicador de cursor de colaborador | [TaskCard.kt](android/app/src/main/java/com/flowboard/presentation/ui/components/TaskCard.kt) : 82 |
| PMM | RA03 | g | Carga de imágenes desde URL mediante Coil; reproducción de contenido de vídeo y audio a través de bloques del editor | [DashboardScreen.kt](android/app/src/main/java/com/flowboard/presentation/ui/screens/dashboard/DashboardScreen.kt) : 386 |
| PMM | RA03 | h | Registro de eventos mediante `Log.d` / `Log.e`; nueve tests unitarios del ViewModel y cinco tests instrumentados de interfaz | [GoogleAuthManager.kt](android/app/src/main/java/com/flowboard/data/auth/GoogleAuthManager.kt) : 72 · [TaskViewModelTest.kt](android/app/src/test/java/com/flowboard/TaskViewModelTest.kt) |
| PMM | RA04 | — | Cubierto por el proyecto BIT (Unity 6): análisis de motores de juego 2D, arquitectura de escenas, componentes y FSM de enemigos | https://github.com/PauLopNun/BIT |
| PMM | RA05 | — | Cubierto por el proyecto BIT (Unity 6): juego completo con sistema de oleadas, tres personajes, audio y despliegue en dispositivo móvil | https://github.com/PauLopNun/BIT |
| Proc. y Servicios | RA04 | a | Dos protocolos estándar implementados: HTTP/HTTPS para la API REST y WebSocket (RFC 6455) para la comunicación en tiempo real | [TaskRoutes.kt](backend/src/main/kotlin/com/flowboard/routes/TaskRoutes.kt) · [WebSockets.kt](backend/src/main/kotlin/com/flowboard/plugins/WebSockets.kt) |
| Proc. y Servicios | RA04 | b | Análisis de ventajas: HTTP garantiza interoperabilidad con cualquier cliente; WebSocket elimina la necesidad de sondeo periódico | [memoria.md](memoria.md) §9.4 y §4.2 |
| Proc. y Servicios | RA04 | c | Ktor Server 2.3.7 como servidor; Ktor Client con motor OkHttp para WebSocket en Android | [NetworkModule.kt](android/app/src/main/java/com/flowboard/di/NetworkModule.kt) · [WebSockets.kt](backend/src/main/kotlin/com/flowboard/plugins/WebSockets.kt) |
| Proc. y Servicios | RA04 | d | Más de 40 endpoints REST desarrollados y probados; dos canales WebSocket: tareas y documentos CRDT | [TaskRoutes.kt](backend/src/main/kotlin/com/flowboard/routes/TaskRoutes.kt) : 15 · [DocumentWebSocketRoutes.kt](backend/src/main/kotlin/com/flowboard/routes/DocumentWebSocketRoutes.kt) |
| Proc. y Servicios | RA04 | e | Verificación del servicio con Postman, curl y DBeaver; 18 casos de prueba documentados contra el servidor de producción | [memoria.md](memoria.md) §6.2.3 |
| Proc. y Servicios | RA04 | f | Difusión de eventos a todos los clientes de una sala mediante `broadcastToRoom()`; cada sesión WebSocket se gestiona en una coroutine independiente | [TaskService.kt](backend/src/main/kotlin/com/flowboard/domain/TaskService.kt) : 82 |
| Proc. y Servicios | RA04 | g | Backend desplegado en Render.com con reinicio automático; ping WebSocket cada 30 segundos; caché Room para lectura sin conexión | [WebSockets.kt](backend/src/main/kotlin/com/flowboard/plugins/WebSockets.kt) : 13 |
| Proc. y Servicios | RA04 | h | Registro estructurado en el servidor; tabla de errores detectados y soluciones en §6.4; referencia completa de la API en §10.1 | [DatabaseFactory.kt](backend/src/main/kotlin/com/flowboard/data/database/DatabaseFactory.kt) : 25 · [memoria.md](memoria.md) §6.4 |
| Proc. y Servicios | RA05 | a | Principios aplicados: protección anti-enumeración en recuperación de contraseña; secretos únicamente en variables de entorno | [AuthService.kt](backend/src/main/kotlin/com/flowboard/domain/AuthService.kt) : 219 |
| Proc. y Servicios | RA05 | b | Tres técnicas criptográficas: BCrypt para contraseñas, HMAC-SHA256 para la firma JWT y TLS para el cifrado en tránsito | [AuthService.kt](backend/src/main/kotlin/com/flowboard/domain/AuthService.kt) : 31 · [Security.kt](backend/src/main/kotlin/com/flowboard/plugins/Security.kt) : 28 |
| Proc. y Servicios | RA05 | c | Todas las rutas protegidas con `authenticate("auth-jwt")`; respuesta HTTP 401 ante token ausente o inválido | [TaskRoutes.kt](backend/src/main/kotlin/com/flowboard/routes/TaskRoutes.kt) : 16 · [Security.kt](backend/src/main/kotlin/com/flowboard/plugins/Security.kt) : 9 |
| Proc. y Servicios | RA05 | d | Esquema de roles en documentos (viewer / editor / owner) y en workspaces (MEMBER / ADMIN / OWNER) | [Tables.kt](backend/src/main/kotlin/com/flowboard/data/database/Tables.kt) |
| Proc. y Servicios | RA05 | e | Contraseñas almacenadas como hash BCrypt; código OTP de seis dígitos con caducidad de 15 minutos | [AuthService.kt](backend/src/main/kotlin/com/flowboard/domain/AuthService.kt) : 31 |
| Proc. y Servicios | RA05 | f | Comunicación REST sobre HTTPS, WebSocket sobre WSS y conexión a la base de datos con `sslmode=require` | [ApiConfig.kt](android/app/src/main/java/com/flowboard/data/remote/ApiConfig.kt) |
| Proc. y Servicios | RA05 | g | La aplicación en producción emplea HTTPS y WSS en su totalidad; token JWT incluido en la cabecera de cada petición autenticada | [NetworkModule.kt](android/app/src/main/java/com/flowboard/di/NetworkModule.kt) |
| Proc. y Servicios | RA05 | h | Tests TC-04 y TC-06 verifican el rechazo ante credenciales incorrectas; §6.4 recoge el incidente del SHA-1 de Google Sign-In y su resolución | [memoria.md](memoria.md) §6.2.3 y §6.4 |
| Empleabilidad II | RA02 | a | Desarrollo del proyecto en paralelo al trabajo en GFT Technologies; reflexión sobre competencias demandadas en el sector | [memoria.md](memoria.md) §9.5 |
| Empleabilidad II | RA02 | b | Gestión del proyecto con metodología Scrum adaptada: backlog en GitHub Issues, sprints de dos semanas y retrospectivas | [memoria.md](memoria.md) §8.1 |
| Empleabilidad II | RA02 | c | Comunicación técnica y no técnica: memoria de 47 páginas con capturas comentadas, mapa de navegación y manual de usuario | [memoria.md](memoria.md) §5.4, §7.4 y §10.1 |
| Empleabilidad II | RA02 | d | Planificación en 15 semanas con dedicación estimada de 350 horas, compaginando empleo y estudios | [memoria.md](memoria.md) §8.2 |
| Empleabilidad II | RA02 | e | Gestión del incidente de Google Sign-In: diagnóstico de la causa raíz (SHA-1 no registrado) y resolución correcta en lugar de parcheo | [memoria.md](memoria.md) §9.5 y §6.4 |
| Empleabilidad II | RA02 | f | Previsión de dificultades técnicas en cada sprint; replanificación documentada ante imprevistos | [memoria.md](memoria.md) §9.2 y §9.3 |
| Empleabilidad II | RA02 | g | Profundización en el motor CRDT más allá del temario; resolución alternativa del bug de búsqueda mediante caché local | [memoria.md](memoria.md) §9.1 y §9.2 |
| Sostenibilidad | RA06 | a | Grupos de interés identificados: usuarios finales, equipo de desarrollo, proveedores de infraestructura y comunidad de software libre | [memoria.md](memoria.md) §9.4 |
| Sostenibilidad | RA06 | b | Aspectos ASG: eficiencia energética del pool de conexiones (A), privacidad y accesibilidad (S), stack de código abierto y reproducibilidad del build (G) | [memoria.md](memoria.md) §9.4 |
| Sostenibilidad | RA06 | c | Acciones propuestas: sustitución de Cloudinary por MinIO, implementación de FCM y sustitución del proceso de notificaciones actual | [memoria.md](memoria.md) §9.4 y §9.3 |
| Sostenibilidad | RA06 | d | Métricas definidas: tiempo de carga inferior a 2 segundos, disponibilidad del 99%, coste de licencias de 0€, cobertura de 32 casos de prueba | [memoria.md](memoria.md) §RNF-01 a §RNF-05 y §7.2 |
| Sostenibilidad | RA06 | e | Informe de sostenibilidad desarrollado en §9.4: tabla comparativa de tecnologías, reflexión sobre dependencia tecnológica y plan de formación | [memoria.md](memoria.md) §9.4 |
| Nube Pública | RA04 | a | Diferenciación entre tipos de almacenamiento: PostgreSQL gestionado (Render), object storage (Cloudinary) y almacenamiento local (DataStore) | [memoria.md](memoria.md) §7.2 y §4.3 |
| Nube Pública | RA04 | b | Configuración de PostgreSQL en Render.com: creación del servicio, 13 tablas, migraciones y restricción de acceso de red | [DatabaseFactory.kt](backend/src/main/kotlin/com/flowboard/data/database/DatabaseFactory.kt) : 18 |
| Nube Pública | RA04 | c | Problemas prácticos resueltos: conversión del formato de URL de Render, inicialización diferida durante el build de Docker | [DatabaseFactory.kt](backend/src/main/kotlin/com/flowboard/data/database/DatabaseFactory.kt) : 93 |
| Nube Pública | RA04 | d | Arquitectura sin estado en el servidor (JWT stateless), pool de conexiones, caché local en Room y pipeline CI/CD automatizado | [DatabaseFactory.kt](backend/src/main/kotlin/com/flowboard/data/database/DatabaseFactory.kt) : 130 |
| Nube Pública | RA04 | e | Herramientas de monitorización en producción: panel de Render.com, logs del proceso Ktor y métricas de PostgreSQL | [memoria.md](memoria.md) §7.2 |
| Nube Pública | RA04 | f | Análisis comparativo entre Render.com y AWS Academy; identificación de mejoras para versiones futuras | [memoria.md](memoria.md) §7.2, §9.3 y §9.4 |

---
## 1. ACCESO A DATOS — RA02

> Se evalúa RA02 porque la aplicación usa **bases de datos relacionales**: PostgreSQL en el servidor y Room/SQLite en el cliente Android.

### a) Ventajas e inconvenientes de utilizar conectores

**En FlowBoard:**  
Se eligió **Exposed ORM + HikariCP** en el servidor y **Room** en Android en lugar de JDBC directo o conectores genéricos. La ventaja principal de Exposed es que el DSL tipado de Kotlin evita errores de compilación por SQL mal formado. La ventaja de HikariCP es la reutilización de conexiones (pool de 10). El inconveniente es que Exposed abstrae el SQL, dificultando queries muy específicas de PostgreSQL; en esos casos se usó `exec()` con SQL crudo.

**Código:**  
- `backend/src/main/kotlin/com/flowboard/data/database/DatabaseFactory.kt` — líneas 84–146 (creación del pool HikariCP)  
- Memoria §4.2 (tabla de frameworks) y §4.3 (justificación de tecnologías de BD)

---

### b) Gestores de bases de datos embebidos e independientes

**En FlowBoard:**  
Se usan **dos gestores**:
- **SQLite embebido** (Room, dentro del APK Android) → caché offline, sin servidor externo.
- **PostgreSQL independiente** (servidor externo en Render.com) → fuente de verdad principal.

**Código:**
- `android/app/src/main/java/com/flowboard/data/local/TaskHiveDatabase.kt` — Room @Database con 11 entidades, versión 10.
- `backend/src/main/kotlin/com/flowboard/data/database/DatabaseFactory.kt` — conexión a PostgreSQL vía HikariCP.
- Memoria §3.3 (diccionario de datos) y §4.3 (sección "Room / SQLite" y "PostgreSQL")

---

### c) Conector idóneo en la aplicación

**En FlowBoard:**  
- **Android → Room** (conector oficial de Jetpack para SQLite): integración nativa con StateFlow/LiveData, DAOs tipados, sin SQL manual en capas de presentación.
- **Servidor → Exposed ORM + Driver PostgreSQL JDBC 42.x + HikariCP 5.x**: Exposed genera SQL compatible con PostgreSQL y delega el pool de conexiones a HikariCP.

**Código:**
- `android/app/build.gradle` — dependencias Room.
- `backend/build.gradle.kts` — `implementation("org.jetbrains.exposed:exposed-core:…")`, `implementation("org.postgresql:postgresql:42.x")`.
- Memoria §4.2 tabla "Servidor Ktor"

---

### d) Establecer la conexión

**En FlowBoard:**

*Servidor (PostgreSQL):*
```kotlin
// DatabaseFactory.kt líneas 26–27
val database = Database.connect(createHikariDataSource())
```
La función `createHikariDataSource()` (líneas 84–147) parsea `DATABASE_URL` del entorno (formato Render.com `postgresql://user:pass@host/db`) y construye la URL JDBC con `sslmode=require`.

*Cliente (Room):*
```kotlin
// TaskHiveDatabase.kt líneas 61–73
Room.databaseBuilder(context, FlowBoardDatabase::class.java, "flowboard_database")
    .fallbackToDestructiveMigration()
    .build()
```

**Código:**
- `backend/src/main/kotlin/com/flowboard/data/database/DatabaseFactory.kt` líneas 18–78 (init) y 84–147 (createHikariDataSource)
- `android/app/src/main/java/com/flowboard/data/local/TaskHiveDatabase.kt` líneas 56–74
- `android/app/src/main/java/com/flowboard/di/DatabaseModule.kt` (inyección Hilt de la base de datos)

---

### e) Estructura de la base de datos

**En FlowBoard:**  
El servidor tiene **13 tablas** definidas con Exposed DSL en `Tables.kt`: `Users`, `Tasks`, `Projects`, `BoardPermissions`, `Documents`, `DocumentPermissions`, `Notifications`, `PasswordResetTokens`, `Workspaces`, `WorkspaceMembers`, `ChatRooms`, `ChatParticipants`, `Messages`.

El cliente Room tiene **11 entidades**: `TaskEntity`, `UserEntity`, `ProjectEntity`, `NotificationEntity`, `ChatRoomEntity`, `MessageEntity`, `ChatParticipantEntity`, `TypingIndicatorEntity`, `DocumentEntity`, `PendingOperationEntity`, `WorkspaceEntity`.

El diagrama E/R y los diccionarios de datos completos están en la Memoria §3.3.

**Código:**
- `backend/src/main/kotlin/com/flowboard/data/database/Tables.kt` — definición de las 13 tablas con sus columnas, tipos y claves foráneas.
- `android/app/src/main/java/com/flowboard/data/local/entities/` — las 11 entidades Room.
- Memoria §3.3 (diagrama E/R + diccionarios de datos de todas las tablas)

---

### f) Aplicaciones que modifican el contenido de la base de datos

**En FlowBoard:**  
CRUD completo en ambas capas:

*Servidor — ejemplo tarea (INSERT/UPDATE/DELETE):*
- `backend/src/main/kotlin/com/flowboard/domain/TaskService.kt` — `createTask()`, `updateTask()`, `deleteTask()`, `toggleTaskStatus()`
- `backend/src/main/kotlin/com/flowboard/routes/TaskRoutes.kt` — endpoints POST, PUT, DELETE, PATCH `/tasks/{id}/toggle`

*Cliente Room — DAO:*
- `android/.../dao/TaskDao.kt` — `insertTask()` (línea 36), `updateTask()` (línea 40), `deleteTask()` (línea 43), `updateTaskStatus()` (línea 50)

*Documentos (INSERT/UPDATE/DELETE):*
- `backend/src/main/kotlin/com/flowboard/routes/DocumentRoutes.kt` — POST, PUT, DELETE `/documents`
- `android/.../dao/DocumentDao.kt` — DAO Room para documentos locales

**Código clave:**
- `android/app/src/main/java/com/flowboard/data/local/dao/TaskDao.kt` líneas 36–55
- `backend/src/main/kotlin/com/flowboard/routes/TaskRoutes.kt` — TaskRoutes completo (mostrado en Memoria §5.2)

---

### g) Objetos para almacenar el resultado de las consultas

**En FlowBoard:**  
Los resultados de las consultas se mapean a:
- **DTOs** en el servidor: `TaskDto`, `DocumentDto`, `WorkspaceDto` (en `android/.../remote/dto/`)
- **Entidades Room** en el cliente: `TaskEntity`, `DocumentEntity`, etc., retornadas como `Flow<List<T>>`
- **Modelos de dominio**: `Task`, `Document`, `User` (en `android/.../domain/model/`)

**Código:**
- `android/app/src/main/java/com/flowboard/data/remote/dto/TaskDto.kt`
- `android/app/src/main/java/com/flowboard/data/local/entities/TaskEntity.kt`
- `android/app/src/main/java/com/flowboard/domain/model/Task.kt`

---

### h) Aplicaciones que efectúan consultas

**En FlowBoard:**  
Consultas Room con `@Query`:
```kotlin
// TaskDao.kt líneas 10–33
@Query("SELECT * FROM tasks ORDER BY createdAt DESC")
fun getAllTasks(): Flow<List<TaskEntity>>

@Query("SELECT * FROM tasks WHERE isEvent = 1 AND eventStartTime >= :startDate ...")
fun getEventsBetweenDates(startDate: LocalDateTime, endDate: LocalDateTime): Flow<List<TaskEntity>>
```

Consultas Exposed en el servidor:
```kotlin
// AuthService.kt — consulta SELECT para login
Users.select { Users.email eq request.email }.firstOrNull()
```

**Código:**
- `android/app/src/main/java/com/flowboard/data/local/dao/TaskDao.kt` líneas 10–33 (todos los @Query)
- `android/app/src/main/java/com/flowboard/data/local/dao/DocumentDao.kt` (consultas de documentos)
- `backend/src/main/kotlin/com/flowboard/domain/AuthService.kt` (consultas Exposed)

---

### i) Eliminar objetos una vez finalizada su función

**En FlowBoard:**  
- `DatabaseFactory.dbQuery { … }` ejecuta el bloque en una transacción y libera la conexión al pool automáticamente al salir del scope (gestión por HikariCP).
- Room libera el cursor al finalizar el `Flow` (ciclo de vida gestionado por el ViewModel scope).
- El OTP de recuperación de contraseña se marca como `used = true` tras consumirse (tabla `password_reset_tokens`).

**Código:**
- `backend/src/main/kotlin/com/flowboard/data/database/DatabaseFactory.kt` línea 209: `suspend fun <T> dbQuery(block: suspend () -> T): T = newSuspendedTransaction(Dispatchers.IO) { block() }`
- `backend/src/main/kotlin/com/flowboard/domain/AuthService.kt` — `PasswordResetTokens.update { it[used] = true }` (invalidación de token)

---

### j) Gestión de transacciones

**En FlowBoard:**  
Toda operación de escritura al servidor pasa por `dbQuery { }` que a su vez usa `newSuspendedTransaction`. El pool está configurado con `isAutoCommit = false` y `transactionIsolation = "TRANSACTION_REPEATABLE_READ"`, por lo que cada bloque es una transacción explícita con commit/rollback automático.

```kotlin
// DatabaseFactory.kt líneas 135–137
maximumPoolSize = 10
isAutoCommit = false
transactionIsolation = "TRANSACTION_REPEATABLE_READ"
```

La inicialización del esquema (`SchemaUtils.create(...)`) también se ejecuta dentro de un `transaction(database) { }` explícito (líneas 28–67 de `DatabaseFactory.kt`).

**Código:**
- `backend/src/main/kotlin/com/flowboard/data/database/DatabaseFactory.kt` líneas 28–67 (transaction de arranque) y línea 209 (dbQuery)
- Memoria §5.2 sección "Pool de conexiones y esquema (backend)"

---

### k) Procedimientos almacenados en la base de datos

**En FlowBoard:**  
Se define y ejecuta el procedimiento almacenado `set_updated_at()` en PL/pgSQL, invocado mediante triggers en las tablas `tasks` y `documents`:

```sql
-- DatabaseFactory.kt líneas 182–206 (runCompatibilityMigrations)
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER tasks_set_updated_at
    BEFORE UPDATE ON tasks FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER documents_set_updated_at
    BEFORE UPDATE ON documents FOR EACH ROW EXECUTE FUNCTION set_updated_at();
```

**Código:**
- `backend/src/main/kotlin/com/flowboard/data/database/DatabaseFactory.kt` líneas 149–207 (`runCompatibilityMigrations()`)
- Memoria §3.3 sección "Procedimiento almacenado — trigger `set_updated_at`"

---

---

## 2. DESARROLLO DE INTERFACES — RA01

> Interfaces gráficos de usuario mediante editores visuales.

### b) Interfaz gráfico usando herramientas de un editor visual

**En FlowBoard:**  
La UI completa está construida con **Android Studio + Jetpack Compose**. Android Studio es el editor visual oficial de Android; el panel de preview de Compose permite visualizar las pantallas en tiempo real sin ejecutar la app. Se diseñaron **25 pantallas** principales (Login, Register, Dashboard, Tasks, Calendar, Chat, Workspaces, Documents, Editor, Notifications, Profile, Settings, Search, etc.).

**Código:**
- `android/app/src/main/java/com/flowboard/presentation/ui/screens/` — todas las pantallas Compose
- `android/app/src/main/java/com/flowboard/FlowBoardApp.kt` — NavHost con 20+ rutas declarativas
- Memoria §5.4 (25 capturas de pantalla comentadas)

---

### e) Análisis del código generado por el editor visual

**En FlowBoard:**  
Jetpack Compose genera código Kotlin declarativo (no XML). El análisis del código de Theme.kt, Type.kt y Shapes.kt (generados y luego personalizados manualmente) muestra cómo el editor traduce el sistema de diseño a tokens:

```kotlin
// Theme.kt — el editor visual generó la estructura base; se personalizó la paleta
MaterialTheme(colorScheme = colorScheme, typography = Typography, shapes = Shapes)
```

**Código:**
- `android/app/src/main/java/com/flowboard/theme/Theme.kt`
- `android/app/src/main/java/com/flowboard/theme/Color.kt`
- `android/app/src/main/java/com/flowboard/theme/Type.kt`
- `android/app/src/main/java/com/flowboard/theme/Shapes.kt`
- Memoria §3.4 sección "Sistema de temas Material 3"

---

### g) Eventos asociados a acciones correspondientes

**En FlowBoard:**  
Cada componente Compose tiene lambdas de evento asignadas:
- `onClick = { expanded = !expanded }` en TaskCard
- `onValueChange = { searchQuery = it }` en OutlinedTextField de búsqueda
- `onClick = { navController.navigate("create_task") }` en FAB
- `performClick()` en los tests de UI

**Código:**
- `android/.../screens/tasks/TaskListScreen.kt` — eventos onClick, onValueChange
- `android/.../screens/tasks/TaskCard.kt` — clickable y animación de expansión
- `android/app/src/test/…/TaskListScreenTest.kt` — tests de comportamiento de eventos (Memoria §6.2.2)

---

## 2. DESARROLLO DE INTERFACES — RA02

> Interfaces naturales de usuario con herramientas visuales.

### a) Herramientas para aprendizaje automático relacionadas con interfaces

**En FlowBoard:**  
Se identificaron y evaluaron dos herramientas AI para interfaz de usuario:
1. **Asistente IA integrado** (FAB "IA" en el editor): usa la API de Anthropic/Claude enviando el contenido del documento actual como contexto. El usuario puede solicitar "Summarize doc", "Rewrite doc" o escribir un prompt libre.
2. **Google Credential Manager + Google Sign-In**: reconocimiento de identidad mediante el ecosistema de IA de Google (reconocimiento de cuenta, selector inteligente de cuentas).

**Código:**
- `backend/src/main/kotlin/com/flowboard/routes/AiRoutes.kt` — endpoint de IA
- `android/.../data/remote/api/AiApiService.kt` — cliente del servicio IA
- Memoria §5.4 figura 5.19 (panel del agente de IA) y §9.5

---

### b) Interfaz natural de usuario creada con las herramientas disponibles

**En FlowBoard:**  
El **asistente IA** (FAB "(IA)") es la interfaz natural: el usuario escribe en lenguaje natural, el sistema procesa el prompt y genera o modifica bloques del documento. La interfaz incluye chips de acciones rápidas ("Summarize doc", "Rewrite doc", "Outline") y un botón de varita mágica para mejorar el prompt automáticamente.

**Código:**
- `android/.../screens/documents/CollaborativeDocumentScreenV2.kt` — integración del panel IA
- `android/.../data/remote/api/AiApiService.kt`
- Memoria §5.4 figura 5.19 y §1.4 ("Asistente IA integrado")

---

### f) Integración de realidad aumentada en interfaces

**En FlowBoard:**  
La edición colaborativa en tiempo real con **cursores de otros usuarios visibles** en el editor de documentos cumple el espíritu de este criterio: se superpone información de presencia (quién está editando dónde) sobre la interfaz principal, como una capa de AR social. Los cursores de colaboradores aparecen animados (pulso infinito) con colores identificativos sobre los bloques que están editando.

**Código:**
- `android/.../screens/documents/CollaborativeDocumentScreenV2.kt` — renderizado de cursores remotos
- `android/app/src/main/java/com/flowboard/data/remote/websocket/DocumentWebSocketClient.kt` líneas 41–44 (`_cursorUpdates`)
- `android/app/src/main/java/com/flowboard/theme/Color.kt` líneas de `CollabCursorColors`
- Memoria §5.3 sección "Animaciones" (cursor pulse) y §5.4 figura 5.18

---

## 2. DESARROLLO DE INTERFACES — RA03

> Componentes visuales con herramientas específicas.

### a) Herramientas para diseño y prueba de componentes

**En FlowBoard:**
- **Diseño:** Figma (prototipado de alta fidelidad antes de la implementación — Memoria §3.4, figuras 3.1 y 3.2)
- **Implementación:** Android Studio Iguana con preview de Compose en tiempo real
- **Prueba:** `createComposeRule()` para tests instrumentados de UI (Memoria §6.2.2)

**Código:**
- `android/app/src/androidTest/…/TaskListScreenTest.kt` — test con `ComposeTestRule`
- Memoria §3.4 (Figma wireframes) y §6.2.2 (UI tests)

---

### c) Métodos y propiedades con asignación de valores por defecto

**En FlowBoard:**  
Todos los Composables clave tienen parámetros con valores por defecto:
```kotlin
// DocumentCoverImage.kt
@Composable
fun DocumentCoverImage(imageUrl: String?, modifier: Modifier = Modifier)

// TaskCard.kt
@Composable
fun TaskCard(task: Task, onClick: () -> Unit, modifier: Modifier = Modifier)
```

Los modelos de dominio también tienen defaults:
```kotlin
// Task.kt
data class Task(id: String, title: String, isCompleted: Boolean = false,
                priority: TaskPriority = TaskPriority.MEDIUM, dueDate: LocalDateTime? = null, ...)
```

**Código:**
- `android/.../presentation/ui/screens/tasks/TaskCard.kt`
- `android/.../domain/model/Task.kt`
- `android/.../domain/model/ContentBlock.kt`
- Memoria §5.2 (TaskCard con parámetros Modifier = Modifier) y §5.3 (ContentScale.Crop)

---

## 2. DESARROLLO DE INTERFACES — RA05

> Informes con herramientas gráficas.

### a) Estructura del informe

**En FlowBoard:**  
`DocumentExporter.kt` genera PDFs con estructura definida: título del documento como cabecera, cada bloque de contenido con su tipo (H1, H2, párrafo, lista, código, cita) renderizado con fuente y tamaño correspondiente, usando `android.graphics.pdf.PdfDocument` con coordenadas explícitas en puntos.

**Código:**
- `android/app/src/main/java/com/flowboard/presentation/ui/util/DocumentExporter.kt`
- Memoria §5.3 sección "Exportación de documentos a PDF y Markdown"

---

### b) Informes básicos a partir de diferentes fuentes de datos mediante asistentes

**En FlowBoard:**  
Se generan dos tipos de informe desde la misma fuente (lista de `ContentBlock`):
1. **PDF nativo** (`exportToPdf()`): `android.graphics.pdf.PdfDocument` con `PdfDocument.Page`.
2. **Markdown** (`exportToMarkdown()`): texto plano con sintaxis Markdown, compartido via Intent Android.

Ambos se ofrecen desde el menú contextual del editor con un selector de formato.

**Código:**
- `android/.../util/DocumentExporter.kt` — `exportToPdf()` y `exportToMarkdown()`
- Memoria §5.3 (código completo de ambas funciones) y §1.4 (RF-12)

---

### c) Filtros sobre los valores a presentar en los informes

**En FlowBoard:**  
La exportación a Markdown filtra por tipo de bloque (`when (block.type)`) para aplicar la sintaxis correcta: `"h1"` → `"# "`, `"bullet"` → `"- "`, `"code"` → bloque de código con backticks, `"todo"` → `"- [ ] "`. Los bloques de tipo `"divider"` o `"image"` se omiten o se representan como separadores.

El PDF usa el mismo filtrado para aplicar el estilo tipográfico correcto a cada bloque.

**Código:**
- `android/.../util/DocumentExporter.kt` — bloque `when (block.type)` en `exportToMarkdown()`
- Memoria §5.3 (código fuente completo de `exportToMarkdown`)

---

## 2. DESARROLLO DE INTERFACES — RA06

> Documenta aplicaciones con herramientas específicas.

### a) Sistemas de generación de ayudas

**En FlowBoard:**
- `contentDescription` en todos los iconos interactivos de la UI para accesibilidad (lectores de pantalla TalkBack).
- Semántica Compose (`Modifier.semantics { }`) en componentes personalizados.
- Manual de usuario integrado en la Memoria §7.4.
- README técnico en el repositorio GitHub.

**Código:**
- Todos los `Icon()` en Compose tienen `contentDescription` definido (p.ej. `Icon(Icons.Default.Add, contentDescription = "New task")`)
- Memoria §7.4 (Manual básico de usuario) y §10.1 (Manual técnico)

---

### b) Ayudas en los formatos habituales

**En FlowBoard:**
- **Markdown:** Este documento de memoria está escrito en Markdown con Pandoc (`.md` → PDF con LaTeX).
- **Código documentado en inglés:** todos los comentarios del código están en inglés.
- **README.md** en el repositorio.
- **KDoc/Javadoc:** comentarios en `CRDTEngine.kt` y `GoogleAuthManager.kt` documentando los métodos públicos.

**Código:**
- `android/.../data/crdt/CRDTEngine.kt` líneas 1–22 (bloque KDoc con descripción de la clase)
- `android/.../data/auth/GoogleAuthManager.kt` — comentarios inline explicando el flujo de dos pasos
- Memoria §10.1 (Manual técnico completo) y §7.4 (Manual de usuario)

---

---

## 3. PROGRAMACIÓN MULTIMEDIA Y DISPOSITIVOS MÓVILES — RA03

> Programas que integran contenidos multimedia.

### a) Entornos de desarrollo multimedia

**En FlowBoard:**
- **Android Studio Iguana** como IDE principal para el cliente multimedia.
- **Ktor + IntelliJ IDEA** para el backend que sirve el contenido multimedia.
- **Cloudinary** como CDN para almacenamiento y entrega de imágenes.
- **WebSocket RFC 6455** como protocolo de sincronización en tiempo real.

**Código:**
- Memoria §4.4 (tabla de entornos de desarrollo) y §4.2 (librerías multimedia)

---

### b) Clases para captura, procesamiento y almacenamiento de datos multimedia

**En FlowBoard:**
- **Captura de imágenes:** `CloudinaryUploader.kt` captura la URI de una imagen seleccionada del sistema, la comprime y la sube.
- **Almacenamiento:** Cloudinary (imágenes remotas), Room `DocumentEntity` (referencia local a URL de bloque de imagen/vídeo/audio).
- **Coil:** clase `AsyncImage` para carga y caché de imágenes remotas.

```kotlin
// CloudinaryUploader.kt
suspend fun uploadImage(context: Context, uri: Uri, maxSide: Int = 512, quality: Int = 82): String?
```

**Código:**
- `android/.../presentation/ui/util/CloudinaryUploader.kt`
- `android/.../data/local/entities/DocumentEntity.kt` — campo `content` almacena JSON de bloques incluyendo URLs de imagen/vídeo/audio
- Memoria §5.3 sección "Carga de imágenes a Cloudinary"

---

### c) Conversión de datos multimedia de un formato a otro

**En FlowBoard:**
- Un documento se convierte de **lista de bloques** (objetos Kotlin) a **PDF** (`android.graphics.pdf.PdfDocument`) y a **Markdown** (texto plano).
- Las imágenes se convierten de **URI local** a **JPEG comprimido** (`Bitmap.compress`) antes de subir a Cloudinary.
- Los bloques de tipo `"image"`, `"video"`, `"audio"` almacenan URLs que Coil/MediaPlayer pueden consumir directamente.

**Código:**
- `android/.../util/DocumentExporter.kt` — conversión bloques → PDF y bloques → Markdown
- `android/.../util/CloudinaryUploader.kt` — conversión URI → JPEG comprimido → URL Cloudinary
- Memoria §5.3 sección "Exportación de documentos a PDF y Markdown"

---

### d) Clases para procesar datos multimedia

**En FlowBoard:**
- `CRDTEngine.kt` procesa operaciones de documento en tiempo real (añadir, editar, mover, eliminar bloques de imagen/vídeo/audio).
- `DocumentSyncService.kt` coordina la sincronización de operaciones CRDT con el servidor.
- `ContentBlock` es la clase que modela cada bloque multimedia (tipo, contenido, spans de formato).

**Código:**
- `android/.../data/crdt/CRDTEngine.kt` — `applyOperation()` líneas 51–79
- `android/.../data/models/crdt/ContentBlock.kt`
- `android/.../data/remote/websocket/DocumentSyncService.kt`

---

### e) Clases para control de eventos, tipos de media y excepciones

**En FlowBoard:**
- **Eventos WebSocket:** `DocumentWebSocketClient.kt` gestiona `ConnectionState` (Disconnected, Connecting, Connected, Error) y emite eventos via `SharedFlow`.
- **Tipos de media:** `ContentBlock.type` puede ser "paragraph", "h1"–"h6", "image", "video", "audio", "code", "quote", "todo", "bullet", "divider".
- **Excepciones:** `GoogleAuthManager.kt` captura `GetCredentialCancellationException`, `NoCredentialException`, `GetCredentialUnknownException`, `GetCredentialException` y `Exception` genérica con manejo diferenciado.

**Código:**
- `android/.../data/remote/websocket/DocumentWebSocketClient.kt` líneas 29–46 (StateFlow/SharedFlow para eventos)
- `android/.../data/remote/websocket/ConnectionState.kt`
- `android/.../data/auth/GoogleAuthManager.kt` líneas 71–100 (bloque try/catch con tipos de excepción)
- Memoria §6.4 (tabla de errores detectados y soluciones)

---

### f) Clases para creación y control de animaciones

**En FlowBoard:**  
Múltiples animaciones implementadas:
- **Spring** (física de muelle): `animateDpAsState(animationSpec = spring(DampingRatioMediumBouncy, StiffnessLow))` en TaskCard
- **AnimatedVisibility** con `expandVertically(spring(...)) + fadeIn(spring(...))` para expansión de cards
- **Cursor collaborativo:** `rememberInfiniteTransition + animateFloat(tween(800), RepeatMode.Reverse)` — pulso infinito
- **Pantalla splash:** `AnimatedVisibility(exit = fadeOut(tween(400)))` con `LaunchedEffect(delay(1400))`
- **Transiciones de ruta:** `slideInHorizontally(tween(280)) + fadeIn(tween(280))` en toda la navegación

**Código:**
- `android/.../screens/tasks/TaskCard.kt` — animateDpAsState con Spring
- `android/.../FlowBoardApp.kt` — transiciones de navegación tween(280) y splash fadeOut
- `android/.../screens/documents/CollaborativeDocumentScreenV2.kt` — cursor pulse animation
- Memoria §5.2 (TaskCard completo) y §5.3 sección "Animaciones"

---

### g) Clases para reproducir contenidos multimedia

**En FlowBoard:**  
- **Imágenes:** `AsyncImage` de Coil Compose con `ContentScale.Crop` para portadas y avatares.
- **Vídeo/Audio:** los bloques de tipo "video" y "audio" del editor almacenan URLs en `ContentBlock.content`; la reproducción se invoca via Android `MediaPlayer` / Intent desde los bloques del editor.
- **Exportación multimedia:** `android.graphics.pdf.PdfDocument` renderiza el contenido del documento como PDF visualizable.

**Código:**
- `android/.../screens/documents/DocumentCoverImage.kt` — `AsyncImage` con ContentScale.Crop
- `android/.../screens/documents/CollaborativeDocumentScreenV2.kt` — renderizado de bloques imagen/vídeo/audio
- Memoria §5.3 sección "ContentScale.Crop en imágenes de portada de documentos"

---

### h) Depuración y documentación de programas desarrollados

**En FlowBoard:**
- Logs estructurados con prefijos (`[OK]`, `[ERROR]`) en el backend.
- Logcat en Android con `Log.d(TAG, …)` / `Log.e(TAG, …)` en `GoogleAuthManager`, `DocumentWebSocketClient`, etc.
- 9 tests unitarios con JUnit 4 + Mockito (TaskViewModel) y 5 tests de UI con ComposeTestRule.
- Documentación KDoc en `CRDTEngine.kt` y `GoogleAuthManager.kt`.

**Código:**
- `android/.../data/auth/GoogleAuthManager.kt` líneas 4–5 (import Log) y líneas 72–100 (Log.d, Log.e, Log.w)
- `android/.../data/remote/websocket/DocumentWebSocketClient.kt` — TAG = "DocumentWebSocketClient"
- `android/app/src/test/…/TaskViewModelTest.kt` — 9 tests unitarios
- Memoria §6.2 (casos de prueba) y §6.3 (resultados)

---

## 3. PROGRAMACIÓN MULTIMEDIA Y DISPOSITIVOS MÓVILES — RA04 y RA05

> **Estos RAs se cubren con el proyecto BIT** (juego 2D Unity), desarrollado en paralelo como parte del mismo módulo.
>
> Repositorio BIT: https://github.com/PauLopNun/BIT
>
> - **RA04** — Análisis de motores de juegos: Unity 6, arquitectura de juego 2D top-down, componentes (Rigidbody2D, Collider2D, Animator, Camera), FSM de enemigos, ScriptableObjects.
> - **RA05** — Juego 2D completo: sistema de oleadas, 3 personajes jugables, 5 tipos de enemigos con IA (FSM), audio BGM + 5 SFX, ranking JSON local, despliegue en dispositivo móvil.
>
> FlowBoard cubre RA03 del mismo módulo (ver apartado anterior).

---

---

## 4. PROGRAMACIÓN DE PROCESOS Y SERVICIOS — RA04

> Servicios en red con librerías de clases.

### a) Protocolos estándar de comunicación para servicios en red

**En FlowBoard:**  
Se implementan dos protocolos estándar:
- **HTTP/HTTPS (REST):** 40+ endpoints bajo `/api/v1/` con verbos GET, POST, PUT, DELETE, PATCH. Referencia: RFC 7231.
- **WebSocket (RFC 6455):** endpoints `ws://…/ws/{boardId}` (tareas en tiempo real) y `ws://…/document-ws/{docId}` (CRDT de documentos).

**Código:**
- `backend/src/main/kotlin/com/flowboard/routes/` — todos los archivos de rutas REST
- `backend/src/main/kotlin/com/flowboard/routes/DocumentWebSocketRoutes.kt` — WebSocket de documentos
- `backend/src/main/kotlin/com/flowboard/plugins/WebSockets.kt` — configuración del plugin WebSocket
- Memoria §10.1 (tabla completa de endpoints REST)

---

### b) Ventajas de protocolos estándar para comunicación entre aplicaciones

**En FlowBoard:**  
La memoria §9.4 analiza esta elección. Las ventajas concretas usadas:
- **HTTP REST:** interoperabilidad total (Postman, curl, cualquier cliente HTTP puede consumir la API sin librería especial).
- **WebSocket:** canal full-duplex persistente que evita polling, esencial para la edición colaborativa en tiempo real.
- **JWT (RFC 7519):** estándar abierto de autenticación sin estado, compatible con cualquier cliente.

**Código:**
- Memoria §9.4 (análisis de tecnologías y alternativas libres) y §4.2 (justificación de librerías)

---

### c) Librerías que implementan servicios en red con protocolos estándar

**En FlowBoard:**
- **Servidor:** `Ktor Server (Netty) 2.3.7` — HTTP + WebSocket server.
- **Cliente Android:** `Ktor Client 2.3.7` con engine `Android` para HTTP y engine `OkHttp` para WebSocket (mejor soporte de WebSocket nativo en Android).

```kotlin
// NetworkModule.kt
@Provides @Singleton @WebSocketClientQualifier
fun provideWebSocketClient(): HttpClient = HttpClient(OkHttp) {
    install(WebSockets) { pingInterval = 30_000; maxFrameSize = Long.MAX_VALUE }
}
```

**Código:**
- `android/.../di/NetworkModule.kt` — proveedores Hilt de HttpClient y WebSocket client
- `backend/src/main/kotlin/com/flowboard/plugins/WebSockets.kt` — configuración WebSocket servidor
- Memoria §4.2 (tablas de librerías cliente y servidor)

---

### d) Servicios de comunicación en red desarrollados y probados

**En FlowBoard:**  
Se desarrollaron y probaron (18 test cases en TC-01 a TC-18):
- **API REST** con 40+ endpoints
- **WebSocket de tareas:** `TaskWebSocketClient.kt` + `ws/{boardId}`
- **WebSocket de documentos CRDT:** `DocumentWebSocketClient.kt` + `document-ws/{docId}`

**Código:**
- `android/.../data/remote/websocket/TaskWebSocketClient.kt`
- `android/.../data/remote/websocket/DocumentWebSocketClient.kt`
- `backend/src/main/kotlin/com/flowboard/routes/DocumentWebSocketRoutes.kt`
- Memoria §6.2.3 (18 test cases REST contra producción)

---

### e) Clientes de comunicaciones para verificar funcionamiento

**En FlowBoard:**
- **Postman:** pruebas de todos los endpoints REST (evidencias en Memoria §6.2.3 y §7.2)
- **curl:** comandos contra `https://flowboard-api-phrk.onrender.com/api/v1` (incluidos en Memoria §6.2.3)
- **DBeaver:** conexión remota a PostgreSQL para verificar tablas y FKs (evidencia en Memoria §7.2)
- **La propia app Android:** cliente funcional completo

**Código:**
- Memoria §6.2.3 (TC-01 a TC-18 con comandos curl y respuestas JSON reales) y §7.2

---

### f) Comunicación simultánea de varios clientes con el servicio

**En FlowBoard:**  
El servidor gestiona múltiples conexiones simultáneas mediante:
- **WebSocket rooms:** `webSocketManager.broadcastToRoom(boardId, message)` emite a todos los clientes de la sala.
- **Coroutines Kotlin:** cada sesión WebSocket corre en su propia coroutine, no hay bloqueo de threads.
- **HikariCP pool de 10 conexiones:** soporta hasta 10 transacciones de BD simultáneas.
- **CRDT + OT:** el motor de Transformación Operacional resuelve conflictos cuando ≥2 usuarios editan el mismo documento simultáneamente.

```kotlin
// TaskService.kt
webSocketManager.broadcastToRoom(boardId = task.projectId!!, message = TaskCreatedMessage(...))
```

**Código:**
- `backend/src/main/kotlin/com/flowboard/domain/TaskService.kt` — broadcast WebSocket
- `backend/src/main/kotlin/com/flowboard/routes/DocumentWebSocketRoutes.kt` — gestión de sesiones concurrentes
- `android/.../data/crdt/CRDTEngine.kt` — resolución de conflictos OT
- Memoria §5.3 sección "Motor CRDT" y §5.3 sección "Broadcast WebSocket en tareas"

---

### g) Disponibilidad del servicio

**En FlowBoard:**
- **Backend:** desplegado en Render.com (disponibilidad 99%, restart automático ante caídas, notificación por email).
- **Monitorización:** Render Dashboard (CPU/RAM/latencia), Render Logs (stdout/stderr), GitHub Actions (estado CI/CD).
- **Ping WebSocket:** `pingPeriod = Duration.ofSeconds(30)` y `timeout = Duration.ofSeconds(15)` en el servidor; `pingInterval = 30_000` en el cliente para mantener la conexión activa.
- **Caché offline:** Room SQLite permite lectura de datos aunque el backend esté caído.

**Código:**
- `backend/src/main/kotlin/com/flowboard/plugins/WebSockets.kt` líneas 13–14 (pingPeriod/timeout)
- `android/.../di/NetworkModule.kt` — `pingInterval = 30_000` en WebSocket client
- Memoria §7.2 sección "Monitorización y observabilidad" y §RNF-03

---

### h) Depuración y documentación

**En FlowBoard:**
- Logs estructurados en el servidor con emojis de estado (✅, ❌, ⚠️, 🔍) para facilitar depuración en Render Logs.
- Tabla de errores detectados y soluciones en Memoria §6.4.
- 18 test cases documentados con HTTP real de producción.
- Referencia completa de API REST en Memoria §10.1.

**Código:**
- `backend/src/main/kotlin/com/flowboard/data/database/DatabaseFactory.kt` líneas 25–77 (println con emojis)
- Memoria §6.4 (errores y soluciones) y §10.1 (referencia API)

---

## 4. PROGRAMACIÓN DE PROCESOS Y SERVICIOS — RA05

> Protege aplicaciones y datos con criterios de seguridad.

### a) Principios y prácticas de programación segura

**En FlowBoard:**
- **Validación de entrada:** el servidor valida el cuerpo de cada petición con `call.receive<T>()` y devuelve HTTP 400 ante datos inválidos.
- **Anti-enumeración:** el endpoint `/auth/forgot-password` siempre devuelve HTTP 200 tanto si el email existe como si no (Memoria §5.2 y TC-11).
- **Sin secretos en código:** `JWT_SECRET`, `DATABASE_URL`, `RESEND_API_KEY` se inyectan como variables de entorno.
- **HTTPS obligatorio:** `sslmode=require` en la cadena JDBC; el backend solo es accesible por HTTPS en producción.

**Código:**
- `backend/src/main/kotlin/com/flowboard/domain/AuthService.kt` — `requestPasswordReset()` con anti-enumeración
- `backend/src/main/kotlin/com/flowboard/data/database/DatabaseFactory.kt` línea 115 (`sslmode=require`)
- Memoria §RNF-02 (seguridad) y §9.4 (cifrado y privacidad)

---

### b) Técnicas y prácticas criptográficas

**En FlowBoard:**  
Tres técnicas implementadas y explicadas en Memoria §9.4:
1. **BCrypt** (hashing unidireccional con salt): contraseñas almacenadas como hash BCrypt, nunca en texto plano.
2. **HMAC-SHA256** (cifrado simétrico): firma de tokens JWT.
3. **TLS/HTTPS** (cifrado asimétrico + simétrico en tránsito): toda comunicación cliente-servidor cifrada.

```kotlin
// AuthService.kt
val hashedPassword = BCrypt.hashpw(request.password, BCrypt.gensalt())

// Security.kt / JwtConfig
private val algorithm = Algorithm.HMAC256(secret)
```

**Código:**
- `backend/src/main/kotlin/com/flowboard/domain/AuthService.kt` — `BCrypt.hashpw()` y `BCrypt.checkpw()`
- `backend/src/main/kotlin/com/flowboard/plugins/Security.kt` — `Algorithm.HMAC256(secret)` y verificación JWT
- Memoria §9.4 sección "Cifrado, privacidad y protección de datos"

---

### c) Políticas de seguridad para limitar acceso de usuarios

**En FlowBoard:**
- Todas las rutas protegidas están envueltas en `authenticate("auth-jwt") { … }`.
- Un token inválido o ausente devuelve HTTP 401 automáticamente (ver TC-06).
- Los documentos tienen política de acceso: `visibility` = private/shared/workspace + tabla `document_permissions`.
- Los workspaces tienen roles: OWNER, ADMIN, MEMBER.

**Código:**
- `backend/src/main/kotlin/com/flowboard/routes/TaskRoutes.kt` — `authenticate("auth-jwt") { … }`
- `backend/src/main/kotlin/com/flowboard/plugins/Security.kt` — configuración del plugin JWT
- `backend/src/main/kotlin/com/flowboard/data/database/Tables.kt` — tablas `DocumentPermissions` y `WorkspaceMembers`
- Memoria §RNF-02 y TC-06 (HTTP 401 sin token)

---

### d) Esquemas de seguridad basados en roles

**En FlowBoard:**  
Tres niveles de roles implementados:
1. **Rol de usuario en el sistema:** `UserRole.USER` / `UserRole.ADMIN` (tabla `users`)
2. **Rol en documentos:** `viewer` / `editor` / `owner` (tabla `document_permissions`) — pantalla "Share Document" (Figura 5.20)
3. **Rol en workspace:** `OWNER` / `ADMIN` / `MEMBER` (tabla `workspace_members`)

**Código:**
- `backend/src/main/kotlin/com/flowboard/data/database/Tables.kt` — columnas `role` en `DocumentPermissions` y `WorkspaceMembers`
- `backend/src/main/kotlin/com/flowboard/domain/PermissionService.kt` — lógica de permisos de documento
- `android/.../data/remote/api/PermissionApiService.kt` — endpoint `POST /permissions/document`
- Memoria §3.3 (diccionarios de datos de `document_permissions` y `workspace_members`) y §5.4 figura 5.20

---

### e) Algoritmos criptográficos para proteger información almacenada

**En FlowBoard:**  
- **BCrypt:** las contraseñas se almacenan como hash BCrypt en la columna `password_hash` de la tabla `users`. BCrypt usa salt aleatorio y factor de coste configurable.
- El OTP de recuperación de contraseña (6 dígitos) se almacena en `password_reset_tokens.code` con expiración de 15 minutos y se invalida tras su uso.

**Código:**
- `backend/src/main/kotlin/com/flowboard/domain/AuthService.kt` — `BCrypt.hashpw()` en `register()` y `BCrypt.checkpw()` en `login()`
- `backend/src/main/kotlin/com/flowboard/data/database/Tables.kt` — tabla `PasswordResetTokens`
- Memoria §5.2 sección "Autenticación (backend)" y §9.4

---

### f) Métodos para asegurar la información transmitida

**En FlowBoard:**
- **HTTPS con TLS:** certificado gestionado por Render.com vía Let's Encrypt. Todas las peticiones al backend usan `https://`.
- **JWT en cabecera Authorization:** los tokens no viajan en URL sino en `Authorization: Bearer <token>`.
- **WebSocket sobre WSS:** las conexiones WebSocket en producción son `wss://` (sobre TLS).
- **sslmode=require en JDBC:** la conexión BD→servidor está cifrada.

**Código:**
- `android/.../data/remote/ApiConfig.kt` — `BASE_URL = "https://flowboard-api-phrk.onrender.com/api/v1/"`
- `backend/src/main/kotlin/com/flowboard/data/database/DatabaseFactory.kt` línea 115
- Memoria §RNF-02 y §9.4

---

### g) Aplicaciones que utilizan comunicaciones seguras

**En FlowBoard:**  
Toda la aplicación usa comunicaciones seguras en producción:
- HTTPS para REST API
- WSS para WebSocket (tareas y documentos CRDT)
- TLS en la conexión BD (Ktor ↔ PostgreSQL)
- JWT como capa de autenticación en cada petición

**Código:**
- `android/.../di/NetworkModule.kt` — HttpClient configurado para HTTPS
- `android/.../data/remote/ApiConfig.kt` — URL de producción con `https://`
- Memoria §7.1 (variables de entorno JWT) y §7.2 (HTTPS y seguridad de red en Render.com)

---

### h) Depuración y documentación

**En FlowBoard:**
- Test TC-04 (HTTP 401 con contraseña incorrecta) y TC-06 (HTTP 401 sin token) documentan el comportamiento de seguridad.
- Test TC-11 documenta la protección anti-enumeración.
- Tabla de errores §6.4 incluye el bug del SHA-1 de Google Sign-In y su solución.

**Código:**
- Memoria §6.2.3 (TC-04, TC-06, TC-11, TC-12, TC-13)
- Memoria §6.4 (errores de seguridad detectados: SHA-1, JWT sin expiración)

---

---

## 5. ITINERARIO PARA LA EMPLEABILIDAD II — RA02

> Competencias personales, sociales y emocionales para el empleo.

### a) Importancia de las competencias personales y sociales en la empleabilidad

FlowBoard se desarrolló mientras el autor estaba trabajando en **GFT Technologies** (consultora de transformación digital para el sector financiero). La reflexión directa está en Memoria §9.5: contraste entre las prácticas del ciclo y las del entorno laboral real (revisiones de código, arquitectura Clean, testing no opcional). La experiencia previa en Economía (Universidad de Valencia) se traduce en decisiones técnicas con argumentación económica (análisis de TCO, coste de oportunidad, efecto red de tecnologías).

**Referencia:** Memoria §9.5 completo.

---

### b) Objetivos del equipo, decisiones compartidas, responsabilidad

FlowBoard es un proyecto individual tratado como si tuviera un cliente real: requisitos redactados como historias de usuario, criterios de aceptación concretos, backlog gestionado en GitHub Issues, sprints de dos semanas con retrospectiva. Se describen decisiones tomadas de forma autónoma y asumidas con responsabilidad: cuándo mover una funcionalidad al siguiente sprint, cuándo resolver un bug raíz en lugar de parchear.

**Referencia:** Memoria §8.1 (Metodología Scrum unipersonal) y §9.5.

---

### c) Técnicas de presentación y comunicación

La [memoria.md](memoria.md) (47 páginas, formato académico IEEE) es el ejercicio de comunicación escrita. Las 25 capturas de pantalla comentadas, los diagramas E/R, el mapa de navegación y el manual de usuario demuestran la adaptación del mensaje a diferentes audiencias (técnica / no técnica).

**Referencia:** Memoria §5.4 (capturas comentadas), §7.4 (manual de usuario) y §10.1 (manual técnico).

---

### d) Gestión del tiempo para alcanzar objetivos individuales

Cronograma de 15 semanas detallado en Memoria §8.2 con dedicación estimada de 350 horas. Gestión de sprints en paralelo a jornada laboral en GFT. Uso de GitHub Milestones para agrupar issues por sprint.

**Referencia:** Memoria §8.2 (cronograma) y §9.5 sección "Gestión del tiempo y planificación".

---

### e) Estrategias para canalizar emociones con actitud flexible

El episodio del Google Sign-In en dispositivo físico (semanas de debugging, `GetCredentialUnknownException` código 10, SHA-1 no registrado) es el caso concreto: en lugar de abandonar la funcionalidad, se diagnosticó la causa raíz y se resolvió correctamente. Reflexión honesta sobre tendencias personales de mejora (TDD, pedir ayuda antes).

**Referencia:** Memoria §9.5 sección "Gestión emocional ante dificultades técnicas" y §6.4 (bug del SHA-1).

---

### f) Programación de actividades con organización eficiente y previsión de dificultades

Los sprints del proyecto previeron dificultades (§9.2): URL de Render.com, inicialización lazy de BD, reconexión WebSocket, conflictos CRDT. Cada uno tiene su solución documentada. El backlog de mejoras futuras (§9.3) refleja la previsión de lo que no cabía en el alcance.

**Referencia:** Memoria §9.2 (dificultades encontradas) y §9.3 (mejoras futuras).

---

### g) Reacción positiva ante conflictos y situaciones nuevas

Dos ejemplos concretos de reacción positiva ante lo inesperado:
1. El motor CRDT no estaba planificado inicialmente con esa profundidad; se investigó más allá del temario para implementarlo correctamente.
2. La búsqueda de texto completo (el endpoint backend tenía un bug de routing) se resolvió con búsqueda client-side sobre la caché Room como solución provisional documentada.

**Referencia:** Memoria §9.1 (objetivos alcanzados), §9.2 y §6.4.

---

---

## 6. SOSTENIBILIDAD — RA06

> Plan de sostenibilidad con grupos de interés, aspectos ASG y métricas.

### a) Grupos de interés de la empresa

**En FlowBoard (hipotético despliegue real):**
- Usuarios finales (estudiantes, equipos de desarrollo, pequeños equipos)
- Desarrolladores / mantenedores del código
- Proveedores de infraestructura (Render.com, Cloudinary, Google)
- Comunidad open source (dependencias Apache 2.0 y BSD)

**Referencia:** Memoria §9.4 sección "Tecnologías utilizadas y sus alternativas en software libre".

---

### b) Aspectos ASG materiales y expectativas de los grupos de interés

**En FlowBoard:**
- **Ambiental (E):** eficiencia energética del backend — pool HikariCP (reutilización de conexiones, no se abre una nueva por petición), arranque lazy de la BD (no consume recursos antes de ser necesario), WorkManager para diferir operaciones costosas.
- **Social (S):** accesibilidad (contentDescription en iconos, semántica Compose para TalkBack), privacidad de datos (RGPD, BCrypt, HTTPS, sin telemetría de terceros).
- **Gobernanza (G):** stack de código abierto (Apache 2.0, BSD), sin vendor lock-in absoluto (backend dockerizado, datos exportables), CI/CD automatizado (reproducibilidad del build).

**Referencia:** Memoria §9.4 completo.

---

### c) Acciones para minimizar impactos negativos y aprovechar oportunidades ASG

**En FlowBoard:**
- Sustituir Cloudinary (SaaS propietario) por **MinIO** o **Nextcloud** (open source, auto-alojable) como mejora futura.
- Dockerizar completamente el backend (ya hecho) para garantizar portabilidad y reducir dependencia de Render.com.
- Implementar FCM en lugar del servidor de notificaciones actual para mejorar la eficiencia energética (batch de notificaciones vs. polling).

**Referencia:** Memoria §9.4 sección "Reflexión sobre el uso de grandes tecnológicas" y §9.3 (mejoras futuras).

---

### d) Métricas de evaluación del desempeño según estándares de sostenibilidad

**En FlowBoard:**
- **Rendimiento técnico:** tiempo de carga < 2s (RNF-01), 60 FPS con CRDT síncrono < 16ms, pool de 10 conexiones simultáneas.
- **Disponibilidad:** 99% en Render.com (monitorizados por Render Dashboard + Render Postgres Metrics).
- **Coste de licencias:** 0€ (stack 100% open source / gratuito para el MVP).
- **Cobertura de tests:** 9 unitarios + 5 UI + 18 REST = 32 casos documentados.

**Referencia:** Memoria §RNF-01 a §RNF-05 y §7.2 sección "Monitorización y observabilidad".

---

### e) Informe de sostenibilidad con plan e indicadores

El análisis de sostenibilidad completo de FlowBoard es la Memoria §9.4 (3 páginas), que incluye:
- Tabla de tecnologías vs. alternativas libres con justificación
- Reflexión sobre dependencia de grandes tecnológicas y riesgo de vendor lock-in
- Análisis de los tres tipos de criptografía usados (BCrypt, HMAC-SHA256, TLS) con implicaciones de privacidad y RGPD
- Plan de formación de usuarios (Memoria §9.4 subsección "Plan de formación")

**Referencia:** Memoria §9.4 completo (páginas ~35–38 del documento impreso).

---

---

## 7. INTRODUCCIÓN A LA NUBE PÚBLICA — RA04

> Gestiona almacenamiento y bases de datos en la nube con arquitecturas escalables.

### a) Diferenciación entre tecnologías de almacenamiento en la nube

**En FlowBoard:**  
Se usan y comparan tres tecnologías de almacenamiento en la nube:
1. **Render PostgreSQL** (BD relacional gestionada) — fuente de verdad principal.
2. **Cloudinary** (Object Storage / CDN) — almacenamiento de imágenes con entrega optimizada.
3. **DataStore Preferences** (almacenamiento local cifrado en Android) — token JWT y preferencias.

La tabla comparativa de Render.com vs. AWS Academy (Memoria §7.2) explica las diferencias entre un proveedor gestionado y una infraestructura IaaS.

**Referencia:** Memoria §7.2 tabla "Render.com vs AWS Academy" y §4.3 sección "Base de datos".

---

### b) Configuración y gestión de base de datos en entorno de nube

**En FlowBoard:**  
PostgreSQL desplegado en Render.com (región Oregon, US West):
- BD creada en el panel de Render.com como servicio gestionado.
- URL de conexión inyectada como variable de entorno `DATABASE_URL` al servicio Ktor.
- 13 tablas creadas automáticamente por `SchemaUtils.create()` en el primer arranque.
- Migraciones de esquema aplicadas con `runCompatibilityMigrations()` (ALTER TABLE).
- Seguridad de red: solo accesible desde el servicio Ktor del mismo entorno Render; ningún puerto público expuesto.

**Código:**
- `backend/src/main/kotlin/com/flowboard/data/database/DatabaseFactory.kt` líneas 18–78 y 84–147
- Memoria §7.2 secciones "Despliegue del backend en Render.com" y "Evidencia de funcionamiento"

---

### c) Resolución de problemas prácticos sobre almacenamiento y bases de datos

**En FlowBoard:**  
Problemas reales resueltos documentados en Memoria §6.4 y §9.2:
1. **URL Render.com no reconocida:** el formato `postgresql://` tuvo que parsearse a `jdbc:postgresql://` con hostname externo (plan gratuito sin red privada). Solución: regex en `DatabaseFactory.kt`.
2. **BD no disponible en Docker build:** la inicialización se difirió al evento `ApplicationStarted` de Ktor (arranque lazy, `initialized = false` por defecto).
3. **Pérdida de conexión por inactividad:** `pingPeriod = 30s` en WebSocket y `idleTimeout = 600000` en HikariCP.

**Código:**
- `backend/src/main/kotlin/com/flowboard/data/database/DatabaseFactory.kt` líneas 93–116 (parser regex URL) y líneas 69–77 (arranque sin lanzar excepción)
- Memoria §6.4 y §9.2

---

### d) Arquitecturas escalables y resilientes basadas en mejores prácticas

**En FlowBoard:**
- **Stateless backend:** el servidor Ktor no almacena estado de sesión (JWT stateless) → escalable horizontalmente.
- **Pool de conexiones HikariCP:** max 10 conexiones, `TRANSACTION_REPEATABLE_READ`, reutilización eficiente.
- **Caché local Room:** resiliencia ante caídas del backend (lectura offline garantizada).
- **CI/CD automatizado:** cada push a `master` genera y despliega automáticamente (GitHub Actions → Render.com).
- **Arranque resiliente:** el backend arranca aunque la BD no esté disponible (lazy init), evitando crashloops en Docker.

**Código:**
- `backend/src/main/kotlin/com/flowboard/data/database/DatabaseFactory.kt` líneas 130–143 (config HikariCP) y líneas 69–77 (resilient init)
- `.github/workflows/build-apk.yml` — pipeline CI/CD
- Memoria §7.2 y §4.5

---

### e) Herramientas de monitoreo y optimización

**En FlowBoard:**  
Herramientas de monitoreo activas en producción:
| Herramienta | Qué monitoriza |
|---|---|
| Render.com Dashboard | CPU, RAM, latencia del servicio Ktor |
| Render Logs | stdout/stderr del proceso, errores de arranque |
| Render Postgres Metrics | Conexiones activas, tamaño de BD, queries |
| GitHub Actions | Estado del pipeline CI/CD, logs de build |

**Referencia:** Memoria §7.2 sección "Monitorización y observabilidad" (tabla completa).

---

### f) Análisis y mejora de arquitecturas existentes

**En FlowBoard:**
- La tabla comparativa Render.com vs. AWS Academy (Memoria §7.2) es el ejercicio de análisis de arquitecturas alternativas.
- Las mejoras futuras §9.3 (FCM para notificaciones, MinIO para imágenes, JWT con refresh tokens) son el análisis crítico de las limitaciones de la arquitectura actual.
- La sección §9.4 analiza el riesgo de vendor lock-in y propone la migración a infraestructura más independiente.

**Referencia:** Memoria §7.2, §9.3 y §9.4.

---

