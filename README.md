# FlowBoard 📋

**Gestión colaborativa de tareas en tiempo real con WebSockets**

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-blue.svg)](https://kotlinlang.org)
[![Ktor](https://img.shields.io/badge/Backend-Ktor-orange.svg)](https://ktor.io)
[![WebSockets](https://img.shields.io/badge/Real--Time-WebSockets-brightgreen.svg)](https://ktor.io/docs/websocket.html)
[![Status](https://img.shields.io/badge/Status-Production%20Ready-success.svg)]()

> **⚡ Nuevo:** 🎉 **Colaboración en Tiempo Real Implementada!** Lee la [Guía Completa](FINAL_IMPLEMENTATION_SUMMARY.md)
>
> **🚀 Quick Links:** [Resumen Final](FINAL_IMPLEMENTATION_SUMMARY.md) | [Deploy en Render](docs/deployment-guide-render.md) | [Publicar en Play Store](docs/play-store-publishing-guide.md) | [Arquitectura WebSocket](docs/websocket-architecture.md)

## 📋 Descripción

FlowBoard es una aplicación móvil de gestión colaborativa de tareas y eventos diseñada especialmente para estudiantes y equipos pequeños. Combina funcionalidades de organización personal con herramientas colaborativas, permitiendo una gestión eficiente tanto offline como online con sincronización automática.

## ✨ Colaboración en Tiempo Real (NUEVO)

FlowBoard ahora incluye **colaboración en tiempo real** mediante WebSockets, permitiendo:

- 🔄 **Sincronización instantánea** - Los cambios se reflejan en tiempo real en todos los dispositivos
- 👥 **Presencia de usuarios** - Ve quién está conectado y activo en cada board
- 🔔 **Notificaciones en vivo** - Recibe actualizaciones al instante cuando otros miembros hacen cambios
- 🌐 **Offline-first** - Funciona sin conexión y sincroniza automáticamente al reconectar
- 🔌 **Reconexión automática** - Manejo robusto de desconexiones con backoff exponencial
- 🔐 **Seguro** - Autenticación JWT sobre WebSocket

**Ver documentación completa:**
- 📖 [Arquitectura WebSocket](docs/websocket-architecture.md)
- 🔧 [Guía de Implementación](docs/websocket-implementation-guide.md)
- 📝 [Schema de Eventos](docs/websocket-events-schema.kt)

### 🎯 Objetivo

Mejorar la productividad y organización de estudiantes y equipos pequeños mediante una herramienta intuitiva que permita:
- Gestión individual y colaborativa de tareas
- Organización de eventos y calendario compartido
- Seguimiento del progreso con métricas detalladas
- Exportación de reportes profesionales

### 🌍 Impacto Social y ODS

FlowBoard contribuye directamente a los Objetivos de Desarrollo Sostenible:

- **ODS 4 - Educación de Calidad**: Mejora la organización académica de estudiantes
- **ODS 8 - Trabajo Decente y Crecimiento Económico**: Optimiza la productividad de equipos pequeños
- **Sostenibilidad Tecnológica**: Diseño eficiente, bajo consumo de datos, accesibilidad universal

## 👥 Usuarios Destinatarios

- **Estudiantes universitarios y de secundaria**
- **Equipos pequeños** (startups, ONGs, asociaciones)
- **Profesionales independientes**
- **Grupos de estudio y proyectos académicos**

## ⚡ Funcionalidades Principales

### 📱 Módulos Implementados

#### 💾 Acceso a Datos
- **Base de datos local**: Room/SQLite para funcionamiento offline
- **Base de datos remota**: PostgreSQL para sincronización
- **CRUD completo**: Crear, leer, actualizar, eliminar tareas/eventos
- **Import/Export**: Soporte JSON/XML para backup y migración

#### 🎨 Interfaz de Usuario
- **Tecnología**: Kotlin + Jetpack Compose
- **Pantallas principales**:
  - Login/Registro con autenticación JWT
  - Lista de tareas con filtros avanzados
  - Detalle de tareas/eventos
  - Calendario interactivo
  - Dashboard con métricas
  - Configuración y perfil

#### 🔧 Servicios y Procesos
- **API REST**: Backend Ktor con endpoints completos
- **Autenticación**: JWT con roles (admin/usuario)
- **Sincronización**: Hilos en background para datos
- **Notificaciones**: Push notifications para recordatorios

#### 📱 Multimedia y Móvil
- **Adjuntos**: Soporte para imágenes en tareas
- **Export PDF**: Generación de reportes profesionales
- **Notificaciones**: Sistema completo de alertas
- **Responsive**: Diseño adaptativo

#### 🏢 Gestión Empresarial
- **Control de usuarios**: Gestión de equipos y permisos
- **Reportes**: Métricas de productividad
- **Dashboard**: Visualización de estadísticas
- **Incidencias**: Sistema de seguimiento de problemas

#### ♻️ Sostenibilidad
- **Eficiencia**: Arquitectura optimizada
- **Modularidad**: Código reutilizable y mantenible
- **Escalabilidad**: Preparado para crecimiento

## 🏗️ Arquitectura del Sistema

```
┌─────────────────────────────────────────────────────────────────┐
│                        FRONTEND (Android)                       │
├─────────────────────────────────────────────────────────────────┤
│  Presentation Layer (Jetpack Compose + ViewModels)             │
│  ├── Auth Screens (Login/Register)                             │
│  ├── Task Management (List/Detail/Create)                      │
│  ├── Calendar & Events                                         │
│  └── Dashboard & Reports                                       │
├─────────────────────────────────────────────────────────────────┤
│  Domain Layer (Use Cases + Repositories)                       │
│  ├── Task Repository                                           │
│  ├── User Repository                                           │
│  └── Project Repository                                        │
├─────────────────────────────────────────────────────────────────┤
│  Data Layer                                                     │
│  ├── Local Database (Room/SQLite)                              │
│  ├── Remote API (Ktor Client)                                  │
│  └── Sync Manager                                              │
└─────────────────────────────────────────────────────────────────┘
                                    │
                                    │ HTTP/REST API
                                    │
┌─────────────────────────────────────────────────────────────────┐
│                         BACKEND (Ktor)                         │
├─────────────────────────────────────────────────────────────────┤
│  API Layer (REST Endpoints)                                    │
│  ├── /auth (Login/Register/Logout)                             │
│  ├── /tasks (CRUD + Query operations)                          │
│  ├── /users (Profile management)                               │
│  └── /projects (Team collaboration)                            │
├─────────────────────────────────────────────────────────────────┤
│  Business Logic Layer                                          │
│  ├── Authentication Service (JWT)                              │
│  ├── Task Service                                              │
│  └── User Service                                              │
├─────────────────────────────────────────────────────────────────┤
│  Data Access Layer                                             │
│  ├── Exposed ORM                                               │
│  └── PostgreSQL Database                                       │
└─────────────────────────────────────────────────────────────────┘
```

## 🛠️ Stack Tecnológico

### Frontend (Android)
- **Lenguaje**: Kotlin 1.9.22
- **UI Framework**: Jetpack Compose
- **Arquitectura**: MVVM + Clean Architecture
- **Inyección de dependencias**: Hilt
- **Base de datos local**: Room/SQLite
- **Networking**: Ktor Client
- **Navegación**: Navigation Compose
- **Gestión de estado**: StateFlow/Compose State

### Backend
- **Framework**: Ktor 2.3.7
- **Lenguaje**: Kotlin
- **Base de datos**: PostgreSQL
- **ORM**: Exposed
- **Autenticación**: JWT
- **Serialización**: Kotlinx Serialization

### DevOps y Testing
- **Build**: Gradle Kotlin DSL
- **Testing**: JUnit, Mockito, Coroutines Test
- **CI/CD**: GitHub Actions (configuración futura)

## 🚀 MVP (Mínimo Viable)

### ✅ Funcionalidades Básicas Implementadas
- [x] Base de datos local con CRUD completo
- [x] Pantallas esenciales en Jetpack Compose
- [x] Backend Ktor con autenticación y CRUD
- [x] Arquitectura Clean con Hilt
- [x] Sincronización básica offline/online
- [x] Gestión de tareas y eventos
- [x] Sistema de notificaciones
- [x] Export PDF básico

### 📊 Dashboard Simple
- Vista resumen de tareas
- Estadísticas básicas de productividad
- Filtros por estado y prioridad

## 🔮 Roadmap - Ampliación Profesional

### Fase 2: Colaboración Avanzada ✅ (COMPLETADO)
- [x] **Sincronización bidireccional en tiempo real con WebSockets** ✨
- [x] **Presencia de usuarios en tiempo real** ✨
- [x] **Broadcasting de eventos (crear/actualizar/eliminar)** ✨
- [x] **Reconexión automática robusta** ✨
- [x] **Arquitectura offline-first** ✨
- [ ] Roles y permisos granulares por proyecto
- [ ] Chat integrado en proyectos
- [ ] Plantillas de tareas/eventos
- [ ] Notificaciones push avanzadas

### Fase 3: Analytics y Business Intelligence
- [ ] Dashboard web para métricas avanzadas
- [ ] Reports automáticos por email
- [ ] Integración con herramientas externas (Slack, Trello)
- [ ] API pública para integraciones

### Fase 4: Escalabilidad Enterprise
- [ ] Vistas Kanban y Gantt
- [ ] Gestión de recursos y capacidad
- [ ] Auditoría y logs detallados
- [ ] Single Sign-On (SSO)
- [ ] Backup automático en la nube

## 🏃‍♂️ Inicio Rápido

### Prerrequisitos
- Android Studio Hedgehog o superior
- JDK 17+
- PostgreSQL 13+ (para backend)
- Git

### 🛠️ Instalación y Configuración

#### 1. Clonar el Repositorio
```bash
git clone https://github.com/tu-usuario/flowboard.git
cd flowboard
```

#### 2. Abrir el Proyecto en Android Studio

**Opción A: Abrir el módulo Android directamente (✅ Recomendado)**
```
Android Studio → File → Open → .../FlowBoard/android
```

**Opción B: Abrir desde la raíz (Composite Build)**
```
Android Studio → File → Open → .../FlowBoard
```

#### 3. Configurar Backend (Opcional)
```bash
cd backend

# Crear base de datos PostgreSQL
createdb flowboard

# Configurar variables de entorno
export DATABASE_URL="jdbc:postgresql://localhost:5432/flowboard"
export DATABASE_USER="tu_usuario"
export DATABASE_PASSWORD="tu_contraseña"
export JWT_SECRET="tu_secreto_jwt"

# Ejecutar backend
./gradlew run
```

#### 4. Compilar y Ejecutar Android App

**Desde Android Studio:**
- Click en Run ▶️ (Shift+F10)

**Desde línea de comandos:**
```bash
# Usando el script de utilidades (✅ Recomendado)
./flow.sh build      # Linux/Mac
flow.bat build       # Windows

# O directamente con Gradle
./gradlew -p android assembleDebug   # Desde raíz
cd android && ./gradlew assembleDebug # Desde android/
```

**💡 Script de Utilidades:**
```bash
# Ver todos los comandos disponibles
./flow.sh help      # Linux/Mac
flow.bat help       # Windows

# Comandos útiles:
./flow.sh build     # Compilar app
./flow.sh run       # Instalar y ejecutar
./flow.sh test      # Ejecutar tests
./flow.sh backend   # Iniciar backend
./flow.sh clean     # Limpiar builds
```

#### 5. Configurar Base de Datos Local
La app creará automáticamente la base de datos SQLite local en el primer arranque.

### 🧪 Ejecutar Tests
```bash
# Tests de Android (desde raíz)
./gradlew -p android test

# Tests de Backend (desde raíz)
./gradlew -p backend test

# O desde cada carpeta directamente
cd android && ./gradlew test
cd backend && ./gradlew test
```

### 📱 Usar la Aplicación

#### Credenciales de Demo
- **Email**: demo@flowboard.com
- **Password**: demo123

#### Flujo de Uso Básico
1. **Registro/Login**: Crear cuenta o usar credenciales demo
2. **Crear Tarea**: Tap en '+' para nueva tarea
3. **Gestionar**: Marcar completada, editar, eliminar
4. **Calendario**: Ver eventos en vista calendario
5. **Dashboard**: Revisar estadísticas de productividad
6. **Sync**: Pull para sincronizar con backend

## 📁 Estructura del Proyecto

```
FlowBoard/
├── android/                           # Aplicación Android
│   ├── app/src/main/java/com/flowboard/
│   │   ├── data/                      # Capa de datos
│   │   │   ├── local/                 # Room database
│   │   │   │   ├── dao/               # Data Access Objects
│   │   │   │   ├── entities/          # Entidades de base de datos
│   │   │   │   └── FlowBoardDatabase.kt
│   │   │   ├── remote/                # API remota
│   │   │   │   ├── api/               # Servicios API
│   │   │   │   └── dto/               # Data Transfer Objects
│   │   │   └── repository/            # Implementaciones de repositorios
│   │   ├── domain/                    # Capa de dominio
│   │   │   ├── model/                 # Modelos de dominio
│   │   │   ├── repository/            # Interfaces de repositorios
│   │   │   └── usecase/               # Casos de uso
│   │   ├── presentation/              # Capa de presentación
│   │   │   ├── ui/
│   │   │   │   ├── screens/           # Pantallas Compose
│   │   │   │   ├── components/        # Componentes reutilizables
│   │   │   │   └── theme/             # Tema y estilos
│   │   │   └── viewmodel/             # ViewModels
│   │   ├── di/                        # Módulos de inyección de dependencias
│   │   └── utils/                     # Utilidades y mappers
│   └── build.gradle                   # Configuración de build Android
├── backend/                           # Backend Ktor
│   ├── src/main/kotlin/com/flowboard/
│   │   ├── data/
│   │   │   ├── database/              # Configuración de base de datos
│   │   │   └── models/                # Modelos de datos
│   │   ├── domain/                    # Lógica de negocio
│   │   ├── plugins/                   # Plugins de Ktor
│   │   ├── routes/                    # Endpoints de API
│   │   └── Application.kt             # Punto de entrada
│   └── build.gradle.kts               # Configuración de build Backend
├── docs/                              # Documentación
├── .gitignore
└── README.md                          # Este archivo
```

## 🤝 Contribuir

### Proceso de Desarrollo
1. Fork del repositorio
2. Crear rama para feature (`git checkout -b feature/nueva-funcionalidad`)
3. Commit cambios (`git commit -am 'Agregar nueva funcionalidad'`)
4. Push a la rama (`git push origin feature/nueva-funcionalidad`)
5. Crear Pull Request

### Estándares de Código
- Seguir las convenciones de Kotlin
- Documentar funciones públicas
- Escribir tests para nuevas funcionalidades
- Usar Clean Architecture principles

## 📊 Métricas y Análisis

### KPIs del Proyecto
- **Productividad**: Tareas completadas por usuario/día
- **Engagement**: Tiempo promedio en la app
- **Colaboración**: Proyectos compartidos activos
- **Retención**: Usuarios activos mensuales

### Herramientas de Monitoreo (Futuro)
- Firebase Analytics
- Crashlytics para estabilidad
- Performance Monitoring

## 🔐 Seguridad

- **Autenticación**: JWT con refresh tokens
- **Datos**: Encriptación local con SQLCipher (futura implementación)
- **Comunicación**: HTTPS obligatorio
- **Validación**: Input sanitization en backend
- **Permisos**: Control granular de acceso

## 📝 Licencia

Este proyecto está licenciado bajo la Licencia MIT - ver el archivo [LICENSE](LICENSE) para detalles.

## 👨‍💻 Desarrollador

**Tu Nombre**
- Email: paulopeznunez@gmail.com
- LinkedIn: [Pau López Núñez](https://www.linkedin.com/in/paulopnun)
- GitHub: [@PauLopNun](https://github.com/PauLopNun)

## 🙏 Agradecimientos

- Comunidad de Kotlin y Android
- Equipo de Ktor por el excelente framework
- Contribuidores de bibliotecas open source utilizadas
- Beta testers y usuarios early adopters

## 📧 Soporte

¿Tienes preguntas o problemas?

- 🐛 **Bugs**: [GitHub Issues](https://github.com/PauLopNun/FlowBoard/issues)
- 💡 **Features**: [Feature Requests](https://github.com/PauLopNun/FlowBoard/discussions)
- 📧 **Email**: paulopeznunez@gmail.com
- 💼 **LinkedIn**: [Pau López Núñez](https://www.linkedin.com/in/paulopnun)

## 🚀 Deployment y Publicación

### Deploy del Backend en Render

FlowBoard backend puede desplegarse fácilmente en Render (plan gratuito disponible):

**Ver guía completa:** [docs/deployment-guide-render.md](docs/deployment-guide-render.md)

**Características:**
- ✅ Plan gratuito disponible
- ✅ PostgreSQL incluida
- ✅ Deploy automático desde Git
- ✅ HTTPS y WSS configurados
- ✅ Variables de entorno seguras

### Publicación en Google Play Store

FlowBoard está preparado para publicación en Play Store con:

**Ver guía completa:** [docs/play-store-publishing-guide.md](docs/play-store-publishing-guide.md)

**Incluye:**
- ✅ Configuración de keystore
- ✅ Build de release firmado
- ✅ Assets y screenshots
- ✅ Proceso completo paso a paso
- ✅ Políticas y privacidad
- ✅ Post-publicación y actualizaciones

**Costo:** $25 USD (pago único)

## 📚 Documentación Completa

### Implementación WebSocket
- [Resumen Final](FINAL_IMPLEMENTATION_SUMMARY.md) - Todo lo implementado
- [Arquitectura](docs/websocket-architecture.md) - Diseño completo del sistema
- [Guía de Implementación](docs/websocket-implementation-guide.md) - Paso a paso
- [Schema de Eventos](docs/websocket-events-schema.kt) - Modelos de mensajes

### Deployment
- [Guía Render](docs/deployment-guide-render.md) - Deploy del backend
- [Guía Play Store](docs/play-store-publishing-guide.md) - Publicación Android

### Código
Ver comentarios inline en el código para detalles de implementación.

---

**FlowBoard** - *Organizando el futuro, una tarea a la vez* 🚀✨

**Status:** Production Ready | **Versión:** 1.0.0 | **WebSockets:** ✅ Implementado