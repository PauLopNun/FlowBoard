# FlowBoard

## Descripción del proyecto

FlowBoard es una aplicación multiplataforma (Android con posibilidad de expandirse a otras plataformas) diseñada para la **gestión colaborativa de tareas y eventos**. La app permite a estudiantes y equipos pequeños crear, asignar y organizar tareas, eventos y proyectos de manera eficiente, visualizando el progreso mediante dashboards y métricas. Incluye funcionalidades como adjuntar notas e imágenes, exportación de reportes en PDF y notificaciones push.

FlowBoard combina **sincronización local** mediante Room/SQLite con un **backend centralizado** en Ktor para gestionar usuarios remotos y colaboración entre dispositivos, simulando un entorno realista que podría manejar una empresa.

---

## Objetivos y ODS

* **ODS 4: Educación de calidad** – Mejora la organización y colaboración de estudiantes y equipos educativos.
* **ODS 8: Trabajo decente y crecimiento económico** – Facilita la planificación y seguimiento de tareas y proyectos para equipos pequeños.
* Promueve **sostenibilidad tecnológica**, eficiencia en el uso de recursos y modularidad del software.

---

## Usuarios destinatarios

* Estudiantes y grupos de estudio.
* Equipos pequeños de startups, ONGs o asociaciones.
* Usuarios individuales que buscan gestionar tareas y eventos de manera profesional.

---

## Módulos y funcionalidades

1. **Acceso a datos**

   * Base de datos local Room/SQLite para cada usuario.
   * Base de datos central PostgreSQL para usuarios remotos y sincronización.
   * CRUD completo para Tareas, Eventos, Usuarios y Colaboraciones.
   * Importación y exportación en formatos JSON/XML.

2. **Interfaz**

   * Kotlin + Jetpack Compose para una experiencia moderna y fluida.
   * Pantallas principales: Login/Registro, Lista de Tareas, Detalle de Tarea/Evento, Calendario y Dashboard.
   * Formularios validados y menús coherentes.

3. **Servicios y procesos**

   * Backend Ktor con API REST para gestión de usuarios, tareas y eventos.
   * Autenticación JWT y roles de usuario (admin/usuario).
   * Hilos en background para sincronización y notificaciones periódicas.

4. **Multimedia y móvil**

   * Adjuntar imágenes a tareas y eventos.
   * Exportación de reportes en PDF.
   * Notificaciones push para recordatorios.

5. **Gestión empresarial**

   * Control de usuarios y roles.
   * Gestión de incidencias y reportes de actividad.
   * Dashboard con métricas de progreso y tareas completadas.

6. **Sostenibilidad**

   * Código modular y escalable.
   * Optimización en el uso de recursos.
   * Funcionalidad offline parcial para ahorro de datos y continuidad de uso.

---

## MVP (Mínimo Viable)

* Base de datos local con CRUD.
* Pantallas esenciales en Jetpack Compose.
* Backend Ktor básico con endpoints CRUD y autenticación JWT.
* Dashboard básico con métricas.
* Notificaciones push y exportación PDF.
* Funcionalidad offline parcial y sincronización básica con backend.

---

## Ampliación avanzada / profesional

* Sincronización bidireccional completa con backend central.
* Roles avanzados y permisos por proyecto.
* Vistas adicionales: Kanban, calendario, lista.
* Plantillas de tareas y eventos reutilizables.
* Dashboard web para métricas y seguimiento avanzado.
* Gestión de usuarios remotos que descargan la app desde Play Store.

---

## Stack tecnológico

* **Frontend móvil:** Kotlin + Jetpack Compose
* **Arquitectura:** MVVM + Hilt (Inyección de dependencias)
* **Base de datos local:** Room/SQLite
* **Backend y API REST:** Ktor + PostgreSQL
* **Cliente HTTP:** Retrofit
* **Notificaciones push**
* **Exportación PDF**

---

## Diagrama de arquitectura (textual)

```
[App Android (Jetpack Compose)]
        |
        | Retrofit/HTTP
        v
[Backend Ktor API REST] --- PostgreSQL remoto
        |
        | Sincronización
        v
[Room/SQLite local en dispositivo]
```

---

## Estructura inicial del proyecto

```
FlowBoard/
│
├── app/                            # Código Android/Kotlin
│   ├── build.gradle                # Configuración de Gradle para módulo app
│   ├── src/
│   │   ├── main/
│   │   │   ├── AndroidManifest.xml
│   │   │   ├── java/com/example/flowboard/
│   │   │   │   ├── MainActivity.kt
│   │   │   │   ├── ui/
│   │   │   │   │   ├── LoginScreen.kt
│   │   │   │   │   ├── RegisterScreen.kt
│   │   │   │   │   ├── TaskListScreen.kt
│   │   │   │   │   ├── TaskDetailScreen.kt
│   │   │   │   │   ├── CalendarScreen.kt
│   │   │   │   │   └── DashboardScreen.kt
│   │   │   │   ├── viewmodel/
│   │   │   │   │   ├── TaskViewModel.kt
│   │   │   │   │   ├── UserViewModel.kt
│   │   │   │   │   └── EventViewModel.kt
│   │   │   │   ├── repository/
│   │   │   │   │   ├── TaskRepository.kt
│   │   │   │   │   ├── UserRepository.kt
│   │   │   │   │   └── EventRepository.kt
│   │   │   │   ├── model/
│   │   │   │   │   ├── Task.kt
│   │   │   │   │   ├── User.kt
│   │   │   │   │   └── Event.kt
│   │   │   │   └── database/
│   │   │   │       ├── AppDatabase.kt
│   │   │   │       ├── TaskDao.kt
│   │   │   │       ├── UserDao.kt
│   │   │   │       └── EventDao.kt
│   │   │   └── res/
│   │   │       ├── layout/
│   │   │       ├── values/
│   │   │       └── drawable/
│   │   └── test/
│   │       ├── TaskViewModelTest.kt
│   │       └── UserRepositoryTest.kt
│   └── proguard-rules.pro
│
├── backend/                               # Backend Ktor
│   ├── build.gradle
│   ├── src/
│   │   ├── main/kotlin/com/example/flowboard/server/
│   │   │   ├── Application.kt
│   │   │   ├── routes/
│   │   │   │   ├── TaskRoutes.kt
│   │   │   │   ├── UserRoutes.kt
│   │   │   │   └── EventRoutes.kt
│   │   │   ├── model/
│   │   │   │   ├── Task.kt
│   │   │   │   ├── User.kt
│   │   │   │   └── Event.kt
│   │   │   └── service/
│   │   │       ├── TaskService.kt
│   │   │       ├── UserService.kt
│   │   │       └── EventService.kt
│   │   └── resources/
│   │       └── application.conf
│
├── README.md                              # Documentación inicial
├── settings.gradle
└── gradle/                                 # Configuración de Gradle wrapper
```

---

## Cómo clonar y ejecutar la app

1. Clonar el repositorio:

```bash
git clone https://github.com/tu_usuario/FlowBoard.git
```

2. Abrir en Android Studio y sincronizar Gradle.

3. Configurar backend:

   * Ejecutar el servidor Ktor.
   * Configurar PostgreSQL local o remoto.
   * Actualizar `application.conf` con credenciales de base de datos.

4. Ejecutar la app en un emulador o dispositivo Android.

5. Registrar usuarios, crear tareas y eventos para probar la funcionalidad.

---

## Futuras ampliaciones

* Aplicación multiplataforma (iOS y web) con Jetpack Compose Multiplatform.
* Dashboard web completo para administración avanzada.
* Integración con calendarios externos y servicios de productividad.
* Optimización de notificaciones push con recordatorios inteligentes.

**FlowBoard** está diseñado para ser un proyecto profesional, escalable y modular, demostrando competencias avanzadas en Kotlin, Jetpack Compose, MVVM, Hilt, Room y desarrollo de backend con Ktor, ideal para un TFG y atractivo para empresas que busquen un desarrollador junior con experiencia en stack moderno.

```
```
