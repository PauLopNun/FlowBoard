# Arquitectura Offline-First de FlowBoard

## Resumen

FlowBoard implementa una **arquitectura offline-first** completa para el manejo de documentos y otras entidades. Esto significa que la aplicación funciona completamente sin conexión a internet, guardando todos los cambios localmente y sincronizándolos automáticamente con el servidor cuando hay conexión disponible.

## Componentes Principales

### 1. **Base de Datos Local (Room)**

#### DocumentEntity (`documents` table)
Almacena documentos localmente con los siguientes campos clave:
- `id`: ID del documento (puede ser temporal hasta sincronizar)
- `isSync`: Indica si el documento está sincronizado con el servidor
- `lastSyncAt`: Timestamp de la última sincronización exitosa
- `updatedAt`: Timestamp de la última modificación

#### PendingOperationEntity (`pending_operations` table)
Cola de operaciones pendientes de sincronizar:
- `operationType`: CREATE, UPDATE, DELETE
- `entityType`: DOCUMENT, TASK, PROJECT, etc.
- `entityId`: ID de la entidad afectada
- `attempts`: Contador de intentos de sincronización
- `lastError`: Último mensaje de error (si falló)

**Versión de BD**: 5

### 2. **DocumentSyncWorker**

Worker de WorkManager que ejecuta la sincronización en background:

#### Modos de Sincronización:
- **UPLOAD**: Solo sube cambios locales al servidor
- **DOWNLOAD**: Solo descarga cambios del servidor
- **BIDIRECTIONAL**: Sincronización completa (subida + descarga)

#### Flujo de Sincronización:

**SUBIDA (Upload)**:
1. Obtiene operaciones pendientes de la BD
2. Procesa cada operación según su tipo:
   - CREATE: Crea el documento en el servidor
   - UPDATE: Actualiza el documento en el servidor
   - DELETE: Elimina el documento del servidor
3. Marca las operaciones como completadas o incrementa contador de intentos

**DESCARGA (Download)**:
1. Obtiene todos los documentos del servidor
2. Compara con versiones locales
3. Resuelve conflictos usando estrategia **last-write-wins**
4. Actualiza documentos locales con versiones más recientes

#### Manejo de Conflictos:
- Si el documento local no está sincronizado (`isSync = false`), tiene prioridad
- Si ambas versiones están sincronizadas, gana la más reciente según `updatedAt`
- Estrategia: **last-write-wins** (el último en escribir gana)

### 3. **SyncManager**

Gestor centralizado de sincronización:

#### Responsabilidades:
1. **Sincronización Periódica**: Ejecuta sincronización cada 30 minutos
2. **NetworkCallback**: Detecta cuando se recupera la conexión y sincroniza automáticamente
3. **Sincronización Manual**: API para forzar sincronización inmediata

#### Características:
- Solo sincroniza si hay red disponible
- No sincroniza si la batería está baja (para sincronización periódica)
- Se inicializa automáticamente al arrancar la app
- Registra un NetworkCallback que detecta cambios de conectividad

### 4. **DocumentEditorViewModel**

ViewModel que implementa el patrón offline-first:

#### Flujo de Guardado:
```kotlin
saveDocument() {
    1. Guarda en BD local PRIMERO (siempre funciona)
    2. Registra operación pendiente (CREATE o UPDATE)
    3. Programa sincronización en background
    4. Actualiza UI con estado "Saved"
}
```

#### Flujo de Eliminación:
```kotlin
deleteDocument() {
    1. Registra operación DELETE pendiente
    2. Programa sincronización
    3. El worker eliminará local + remoto cuando procese
}
```

## Flujo Completo de Trabajo

### Escenario 1: Usuario Offline Guardando Documento

1. **Usuario edita documento** → UI actualiza en tiempo real
2. **Usuario guarda** → `saveDocument()` se ejecuta
3. **Guardado local** → Documento se guarda en Room DB con `isSync = false`
4. **Operación pendiente** → Se crea `PendingOperationEntity` de tipo UPDATE
5. **Programa Worker** → WorkManager programa sincronización (esperará conexión)
6. **UI muestra "Saved"** → Usuario ve confirmación inmediata

### Escenario 2: Usuario Recupera Conexión

1. **NetworkCallback detecta conexión** → `SyncManager.onAvailable()`
2. **Dispara sincronización** → `triggerImmediateSync()`
3. **Worker se ejecuta**:
   - Sube operaciones pendientes (documentos no sincronizados)
   - Descarga cambios del servidor
   - Resuelve conflictos
4. **Actualiza estados** → Marca documentos como `isSync = true`
5. **Elimina operaciones** → Borra operaciones completadas de la cola

### Escenario 3: Sincronización Periódica

1. **Cada 30 minutos** → PeriodicWorkRequest se ejecuta
2. **Si hay conexión** → Ejecuta sincronización bidireccional
3. **Si no hay conexión** → WorkManager reintenta automáticamente

## Características Implementadas

✅ **Guardado offline-first**: Guarda local primero, sincroniza después
✅ **Cola de operaciones pendientes**: Sistema robusto de tracking
✅ **Sincronización bidireccional**: Sube y descarga cambios
✅ **Manejo de conflictos**: Estrategia last-write-wins
✅ **Sincronización periódica**: Cada 30 minutos automáticamente
✅ **Detección de red**: Sincroniza al recuperar conexión
✅ **Reintentos automáticos**: WorkManager reintenta si falla
✅ **Límite de reintentos**: Máximo 5 intentos por operación

## APIs Públicas

### SyncManager

```kotlin
// Forzar sincronización inmediata (bidireccional)
syncManager.triggerImmediateSync()

// Subir un documento específico
syncManager.triggerUploadSync(documentId)

// Solo descargar cambios del servidor
syncManager.triggerDownloadSync()

// Cancelar todas las sincronizaciones
syncManager.cancelAllSync()
```

### DocumentEditorViewModel

```kotlin
// Guardar documento (offline-first)
viewModel.saveDocument(id, title, content, isManualSave = true)

// Eliminar documento (offline-first)
viewModel.deleteDocument(documentId)

// Forzar sincronización de todos los documentos
viewModel.syncAllDocuments()

// Observar estado de guardado
viewModel.saveStatus.collect { status ->
    when (status) {
        SaveStatus.Saving -> showLoading()
        SaveStatus.Saved -> showSuccess()
        SaveStatus.Error -> showError()
    }
}
```

## Configuración

### WorkManager
- **Sincronización periódica**: 30 minutos
- **Reintentos**: Máximo 3 por trabajo
- **Constraints**: Requiere red y batería no baja (periódica)

### Base de Datos
- **Nombre**: `flowboard_database`
- **Versión**: 5
- **Estrategia de migración**: `fallbackToDestructiveMigration`

## Próximas Mejoras

- [ ] Implementar CRDTs para resolución avanzada de conflictos
- [ ] Caché de imágenes y assets offline
- [ ] Sincronización de tareas y proyectos
- [ ] Indicadores visuales de estado de sincronización en UI
- [ ] Compresión de datos para sincronización
- [ ] Sincronización diferencial (solo cambios)
- [ ] Notificaciones de éxito/fallo de sincronización

## Logs y Debugging

Todos los componentes logean con tags específicos:
- `DocumentSyncWorker`: Logs de sincronización
- `SyncManager`: Logs de gestión de sincronización
- `DocumentEditorViewModel`: Logs de guardado/edición

Usar LogCat con filtros:
```
adb logcat | grep -E "(DocumentSyncWorker|SyncManager)"
```

## Dependencias

```kotlin
// Room
implementation "androidx.room:room-runtime:2.6.1"
implementation "androidx.room:room-ktx:2.6.1"
kapt "androidx.room:room-compiler:2.6.1"

// WorkManager
implementation "androidx.work:work-runtime-ktx:2.9.0"

// Hilt + WorkManager
implementation "androidx.hilt:hilt-work:1.1.0"
kapt "androidx.hilt:hilt-compiler:1.1.0"

// Kotlinx DateTime
implementation "org.jetbrains.kotlinx:kotlinx-datetime:0.5.0"

// Kotlinx Serialization
implementation "org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2"
```

## Notas de Implementación

1. **Todas las operaciones de guardado deben pasar por el ViewModel** para garantizar que se registren en la cola de operaciones pendientes.

2. **No eliminar directamente de la BD**: Siempre usar `deleteDocument()` del ViewModel para registrar la operación pendiente.

3. **El NetworkCallback se registra automáticamente**: No es necesario registrarlo manualmente.

4. **Las operaciones se procesan en orden FIFO**: Primera operación creada, primera en procesarse.

5. **Verificar estado de sincronización**: Usar `isSync` para saber si un documento tiene cambios pendientes.
