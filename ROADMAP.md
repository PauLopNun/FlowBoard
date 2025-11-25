# FlowBoard - Roadmap de Desarrollo

## Estado Actual

### ✅ Funcionalidades Implementadas
- **Backend (Ktor + PostgreSQL)**
  - API REST funcional con JWT authentication
  - WebSocket para sincronización en tiempo real
  - Base de datos con usuarios y tareas
  - Deployment en Render con PostgreSQL

- **Android (Jetpack Compose)**
  - Login screen (funcional)
  - Task list screen con filtros (All, Pending, Completed, Overdue)
  - WebSocket client para real-time sync
  - Indicador de usuarios activos
  - Material 3 design system (básico)
  - Arquitectura MVVM con Hilt DI

### ❌ Funcionalidades Faltantes

#### Pantallas
1. **Register Screen** - No hay forma de registrarse como nuevo usuario
2. **Create/Edit Task Screen** - El botón FAB no hace nada
3. **Task Detail Screen** - No hay vista detallada de tareas
4. **Document Editor** - No existe un editor de documentos
5. **Settings Screen** - No hay configuración de usuario
6. **Profile Screen** - No hay perfil de usuario

#### Autenticación
- Login con Google (OAuth 2.0)
- Login con otros proveedores (Facebook, Apple)
- Recuperación de contraseña
- Verificación de email

#### Colaboración
- Sistema de invitaciones a documentos/tableros
- Permisos granulares (viewer, editor, admin)
- Cursores en tiempo real de otros usuarios
- Indicador de "quién está escribiendo"
- Historial de cambios (versioning)

#### Editor de Documentos
- Rich text editor (negritas, cursivas, títulos, listas)
- Bloques de contenido (párrafos, imágenes, código, tablas)
- Markdown support
- Exportar a PDF/DOCX
- Búsqueda dentro del documento

#### UI/UX Mejorada
- Animaciones con Material 3
- Dark mode / Light mode
- Diseño responsive y pulido
- Splash screen
- Onboarding para nuevos usuarios
- Empty states mejorados
- Loading states mejorados
- Error handling visual

#### Otras Funcionalidades
- Notificaciones push
- Búsqueda global
- Filtros avanzados
- Etiquetas/Tags para tareas
- Fechas de vencimiento
- Prioridades
- Comentarios en tareas
- Adjuntar archivos

---

## Propuesta de Plan de Desarrollo

### Fase 1: Fundamentos (2-3 semanas)
**Objetivo: Completar el flujo básico de usuario y mejorar la UI existente**

#### Week 1
- [ ] **Register Screen**
  - Diseñar UI con Material 3
  - Conectar con backend `/auth/register`
  - Validación de campos
  - Manejo de errores

- [ ] **Mejorar Login Screen**
  - Añadir "Forgot Password" link
  - Mejorar validación de campos
  - Mejor manejo de errores
  - Loading states

#### Week 2
- [ ] **Create Task Screen**
  - Diseñar formulario con Material 3
  - Campos: título, descripción, fecha, prioridad, tags
  - Validación
  - Conectar con backend

- [ ] **Edit Task Screen**
  - Reutilizar componentes de create
  - Pre-cargar datos de la tarea
  - Actualizar backend

#### Week 3
- [ ] **Task Detail Screen**
  - Vista completa de la tarea
  - Mostrar todos los campos
  - Botones de editar/eliminar
  - Historial de cambios (si aplica)

- [ ] **Mejorar UI Global**
  - Implementar theme switcher (dark/light)
  - Añadir animaciones
  - Mejorar spacing y padding
  - Pulir colores y tipografía

---

### Fase 2: Colaboración Básica (2-3 semanas)
**Objetivo: Implementar sistema de invitaciones y permisos**

#### Week 4
- [ ] **Sistema de Tableros/Workspaces**
  - Backend: tabla de Boards
  - Backend: relación User-Board con roles
  - API endpoints para boards
  - Android: lista de boards

#### Week 5
- [ ] **Invitaciones**
  - Backend: tabla de invitations
  - API para enviar/aceptar invitaciones
  - Android: UI para invitar usuarios
  - Android: UI para ver/aceptar invitaciones

#### Week 6
- [ ] **Permisos**
  - Backend: implementar roles (viewer, editor, admin)
  - Backend: validación de permisos en endpoints
  - Android: UI condicional según permisos
  - Android: indicadores visuales de rol

---

### Fase 3: Editor de Documentos (3-4 semanas)
**Objetivo: Crear un editor rico de documentos con sync real-time**

#### Week 7-8
- [ ] **Rich Text Editor Básico**
  - Investigar librerías (Compose Rich Editor, Markwon, etc.)
  - Implementar formatting básico (bold, italic, underline)
  - Implementar listas (bullets, numbered)
  - Implementar headings (H1, H2, H3)

#### Week 9
- [ ] **Bloques de Contenido**
  - Sistema de bloques (paragraphs, images, code, quotes)
  - Drag & drop para reordenar bloques
  - Añadir/eliminar bloques

#### Week 10
- [ ] **Real-Time Sync**
  - Backend: Operational Transform o CRDT
  - WebSocket messages para cambios de documento
  - Conflict resolution
  - Cursores de otros usuarios

---

### Fase 4: Features Avanzadas (2-3 semanas)
**Objetivo: Añadir funcionalidades que mejoren la experiencia**

#### Week 11
- [ ] **Búsqueda y Filtros**
  - Backend: full-text search
  - Android: barra de búsqueda global
  - Filtros avanzados (por fecha, usuario, tag, etc.)

#### Week 12
- [ ] **Notificaciones**
  - Backend: Firebase Cloud Messaging
  - Android: implementar push notifications
  - Notificaciones de invitaciones, menciones, cambios

#### Week 13
- [ ] **Exportar y Compartir**
  - Exportar documentos a PDF
  - Exportar documentos a Markdown
  - Compartir links públicos (read-only)

---

### Fase 5: Autenticación Avanzada (1-2 semanas)
**Objetivo: OAuth providers y seguridad**

#### Week 14
- [ ] **Google Sign-In**
  - Backend: OAuth 2.0 con Google
  - Android: Google Sign-In SDK
  - Vincular cuentas existentes

#### Week 15 (opcional)
- [ ] **Otros Providers**
  - Facebook login
  - Apple Sign In
  - GitHub login

---

### Fase 6: Polish y Testing (2 semanas)
**Objetivo: Pulir la app para producción**

#### Week 16
- [ ] **Testing**
  - Unit tests para ViewModels
  - Integration tests para repositories
  - UI tests para flows críticos
  - Backend: tests de API endpoints

#### Week 17
- [ ] **Polish Final**
  - Fix bugs conocidos
  - Optimizar performance
  - Mejorar accesibilidad
  - Documentación completa
  - Preparar para Play Store

---

## Librerías Recomendadas

### Android
- **Compose Material 3** - Ya está incluido ✅
- **Accompanist** - Utilidades para Compose (system UI controller, navigation animations)
- **Compose Rich Editor** - Editor de texto rico
- **Coil** - Carga de imágenes (ya incluido ✅)
- **Lottie** - Animaciones JSON
- **Google Sign-In** - Autenticación con Google
- **Firebase Cloud Messaging** - Push notifications

### Backend
- **Firebase Admin SDK** - Para push notifications
- **Google OAuth Library** - Para Google Sign-In
- **Bcrypt** - Password hashing (ya incluido ✅)
- **Exposed** - ORM para Kotlin (ya incluido ✅)
- **ShareDB o Y.js** - CRDT para real-time collaboration

---

## Prioridades Inmediatas

Para tener una app funcional mínima, recomiendo enfocarse en:

1. **Register Screen** (1-2 días)
2. **Create/Edit Task Screen** (2-3 días)
3. **Mejorar UI con Material 3** (2-3 días)
4. **Dark Mode** (1 día)
5. **Google Sign-In** (2-3 días)

Esto daría una experiencia completa de gestión de tareas antes de avanzar al editor de documentos complejo.

---

## Notas

- El editor de documentos con colaboración real-time es la feature más compleja y tomará más tiempo
- Se recomienda usar una librería existente para CRDT (Conflict-free Replicated Data Type) como Y.js
- Para el editor rico, evaluar si usar componentes nativos de Compose o WebView con editor JavaScript
- Considerar el costo de mantener el backend 24/7 en Render (plan Starter $7/mes recomendado)
