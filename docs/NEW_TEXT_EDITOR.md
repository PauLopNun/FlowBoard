# Nuevo Editor de Texto - FlowBoard

## Resumen de Cambios

Se ha implementado un **nuevo editor de texto enriquecido** usando la librería [compose-rich-editor](https://github.com/MohamedRejeb/compose-rich-editor) con funcionalidad de **colaboración en tiempo real**.

## Características Principales

### 🎨 Editor de Texto Enriquecido

El nuevo editor (`ComposeRichTextEditor.kt`) incluye:

- **Formato de texto completo**:
  - Negrita, cursiva, subrayado, tachado
  - Títulos (H1, H2, H3)
  - Alineación de texto (izquierda, centro, derecha)
  - Listas con viñetas y numeradas
  - Bloques de código
  - Colores de texto personalizables

- **Undo/Redo**: Soporte completo para deshacer y rehacer cambios

- **Exportación**: El contenido se guarda en formato HTML, permitiendo exportación a PDF y otros formatos

- **Auto-guardado**: El contenido se guarda automáticamente mientras escribes

### 👥 Colaboración en Tiempo Real

#### Sistema de Invitación de Usuarios

- **Diálogo de invitación** (`UserInvitationDialog.kt`):
  - Búsqueda de usuarios por email o username
  - Selección de niveles de permiso:
    - **View only**: Solo lectura
    - **Can comment**: Lectura y comentarios
    - **Can edit**: Lectura y edición
    - **Admin**: Control total incluido compartir
  - Lista de usuarios sugeridos
  - Visualización de colaboradores actuales con estado online/offline

#### Sincronización en Tiempo Real

- **WebSocket Service** (`DocumentSyncService.kt`):
  - Conexión WebSocket para sincronización instantánea
  - Transmisión de cambios de contenido en tiempo real
  - Sincronización de posición del cursor
  - Presencia de usuarios (online/offline)
  - Estados de conexión (Disconnected, Connecting, Connected, Error)

#### Indicadores de Presencia

- **Avatares de usuarios activos**: Muestra los usuarios que están editando el documento actualmente
- **Estados online/offline**: Indicadores visuales de la presencia de colaboradores
- **Contador de usuarios**: Muestra el número total de colaboradores activos

## Arquitectura

### Componentes Principales

```
┌─────────────────────────────────────────┐
│     DocumentEditorScreen.kt             │
│  (Pantalla principal de edición)       │
└────────────┬────────────────────────────┘
             │
             ├─────────────────────────────┐
             │                             │
    ┌────────▼─────────┐        ┌─────────▼──────────┐
    │ComposeRichText   │        │UserInvitation      │
    │Editor.kt         │        │Dialog.kt           │
    │(Editor principal)│        │(Invitar usuarios)  │
    └────────┬─────────┘        └────────────────────┘
             │
    ┌────────▼──────────────────────────────┐
    │ DocumentEditorViewModel.kt            │
    │ (Lógica de negocio y estado)          │
    └────────┬──────────────────────────────┘
             │
    ┌────────▼──────────────────────────────┐
    │ DocumentSyncService.kt                │
    │ (Sincronización WebSocket)            │
    └───────────────────────────────────────┘
```

### Flujo de Datos

1. **Edición Local**:
   ```
   Usuario escribe → ComposeRichTextEditor → ViewModel →
   → SharedPreferences (guardado local)
   → DocumentSyncService → WebSocket → Servidor
   ```

2. **Recepción de Cambios Remotos**:
   ```
   Servidor → WebSocket → DocumentSyncService →
   → ViewModel → ComposeRichTextEditor → UI actualizada
   ```

## Uso del Editor

### Inicialización Básica

```kotlin
ComposeRichTextEditor(
    initialHtml = documentContent,
    onContentChange = { htmlContent ->
        // Manejar cambios de contenido
        saveDocument(htmlContent)
    },
    activeUsers = listOf(
        UserPresenceInfo("user1", "John Doe", true, timestamp)
    ),
    onInviteUser = {
        // Mostrar diálogo de invitación
    },
    placeholder = "Start typing..."
)
```

### Integración con ViewModel

```kotlin
// En el ViewModel
class DocumentEditorViewModel @Inject constructor(
    private val documentSyncService: DocumentSyncService
) : ViewModel() {

    // Conectar a documento para colaboración
    fun connectToDocument(documentId: String, userId: String, token: String) {
        viewModelScope.launch {
            documentSyncService.connectToDocument(documentId, userId, token)
        }
    }

    // Enviar actualización de contenido
    fun sendContentUpdate(documentId: String, content: String, cursorPosition: Int) {
        viewModelScope.launch {
            documentSyncService.sendContentUpdate(documentId, content, cursorPosition)
        }
    }

    // Invitar usuario
    fun inviteUser(documentId: String, userIdOrEmail: String, permission: String) {
        viewModelScope.launch {
            documentSyncService.inviteUser(documentId, userIdOrEmail, permission)
        }
    }
}
```

## Dependencias Agregadas

```gradle
// Compose Rich Editor
implementation 'com.mohamedrejeb.richeditor:richeditor-compose:1.0.0-rc13'
```

## Formato de Datos

### Contenido HTML

El contenido del documento se guarda en formato HTML:

```html
<h1>Título del Documento</h1>
<p>Este es un párrafo con <strong>texto en negrita</strong> y <em>cursiva</em>.</p>
<ul>
    <li>Item de lista 1</li>
    <li>Item de lista 2</li>
</ul>
```

### Mensajes WebSocket

```json
{
  "type": "ContentUpdate",
  "documentId": "doc-123",
  "content": "<p>Contenido actualizado</p>",
  "timestamp": 1234567890,
  "cursorPosition": 45
}
```

## Próximos Pasos

### Backend (A implementar)

1. **Servidor WebSocket**:
   - Implementar endpoint WebSocket en el backend
   - Manejar conexiones y desconexiones de usuarios
   - Broadcast de cambios a todos los colaboradores

2. **Sistema de Permisos**:
   - Implementar verificación de permisos en el servidor
   - Manejar invitaciones de usuarios
   - Sistema de tokens de acceso

3. **Persistencia**:
   - Guardar documentos en base de datos
   - Mantener historial de versiones
   - Sistema de backup automático

### Frontend (Mejoras futuras)

1. **Cursores colaborativos**: Mostrar la posición del cursor de otros usuarios en tiempo real
2. **Comentarios en línea**: Permitir comentarios en partes específicas del documento
3. **Sugerencias de cambios**: Sistema de track changes estilo Google Docs
4. **Chat integrado**: Chat en tiempo real entre colaboradores
5. **Historial de versiones**: Ver y restaurar versiones anteriores del documento

## Archivos Eliminados

Los siguientes archivos del editor antiguo fueron eliminados:

- `CollaborativeRichTextEditor.kt` - Reemplazado por `ComposeRichTextEditor.kt`
- `AdvancedRichTextEditor.kt` - Reemplazado por `ComposeRichTextEditor.kt`
- `FluidDocumentEditor.kt` - Reemplazado por `ComposeRichTextEditor.kt`
- `EditorToolbar.kt` - Integrado en `ComposeRichTextEditor.kt`

## Archivos Nuevos

- ✅ `ComposeRichTextEditor.kt` - Componente principal del editor
- ✅ `UserInvitationDialog.kt` - Diálogo de invitación de usuarios
- ✅ `DocumentSyncService.kt` - Servicio de sincronización WebSocket
- ✅ Actualizaciones en `DocumentEditorViewModel.kt` - Lógica de sincronización
- ✅ Actualizaciones en `DocumentEditorScreen.kt` - Integración del nuevo editor

## Referencias

- [compose-rich-editor](https://github.com/MohamedRejeb/compose-rich-editor) - Librería del editor
- [OpenNote-Compose](https://github.com/YangDai2003/OpenNote-Compose) - Referencia de arquitectura
- [nowinandroid](https://github.com/android/nowinandroid) - Mejores prácticas de Android

## Soporte

Para reportar problemas o sugerir mejoras, crea un issue en el repositorio de GitHub.
