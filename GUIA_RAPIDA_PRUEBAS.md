# 🚀 Guía Rápida de Pruebas - FlowBoard

## ⚡ Inicio Rápido

### 1. Compilar el Proyecto

```bash
# Backend
cd backend
./gradlew build

# Android (opción 1: desde raíz)
cd ../
./gradlew -p android assembleDebug

# Android (opción 2: desde carpeta android)
cd android
./gradlew assembleDebug
```

### 2. Iniciar el Backend

```bash
cd backend
./gradlew run
```

El backend estará disponible en: `http://localhost:8080`

### 3. Instalar y Ejecutar Android

```bash
# Desde Android Studio: Run ▶️
# O desde terminal:
cd android
./gradlew installDebug
adb shell am start -n com.flowboard/.MainActivity
```

---

## 🧪 Escenarios de Prueba

### 📝 Escenario 1: Colaboración en Documentos (PRINCIPAL)

#### **Objetivo:** Dos usuarios colaborando en el mismo documento

**Pasos:**

1. **Usuario A - Crear y compartir:**
   ```
   - Abrir app
   - Login/Register como usuario_a@test.com
   - Ir a Documents (menú ⋮)
   - Crear nuevo documento
   - Escribir contenido
   - Click en botón "Share" 🔗
   - Ingresar: usuario_b@test.com
   - Seleccionar rol: "Editor"
   - Click "Share"
   - ✅ Ver confirmación de éxito
   ```

2. **Usuario B - Recibir y colaborar:**
   ```
   - Login como usuario_b@test.com
   - Click en icono 🔔 (notificaciones)
   - ✅ Ver: "User A shared 'Document Title' with you"
   - Click en la notificación
   - ✅ Abre el documento automáticamente
   - Editar el contenido
   - ✅ Usuario A ve los cambios en tiempo real
   ```

**Resultado esperado:**
- ✅ Documento compartido exitosamente
- ✅ Notificación recibida
- ✅ Deep link funciona
- ✅ Ambos usuarios ven cambios en vivo
- ✅ Avatares de usuarios activos visibles

---

### 💬 Escenario 2: Chat en Tiempo Real

**Pasos:**

1. **Usuario A - Crear chat:**
   ```
   - Click en icono 💬 (chat)
   - Click en botón "+"
   - Seleccionar tipo: "Direct"
   - Buscar usuario: usuario_b@test.com
   - Click "Create Chat"
   ```

2. **Usuario A - Enviar mensaje:**
   ```
   - Escribir: "Hola! ¿Viste el documento?"
   - Click "Send"
   - ✅ Mensaje aparece instantáneamente
   ```

3. **Usuario B - Responder:**
   ```
   - Click en icono 💬
   - ✅ Ver chat con Usuario A
   - Abrir chat
   - ✅ Ver mensaje de Usuario A
   - Responder: "Sí, lo estoy revisando"
   - ✅ Usuario A lo ve inmediatamente
   ```

**Resultado esperado:**
- ✅ Chat creado exitosamente
- ✅ Mensajes instantáneos
- ✅ Ambos usuarios sincronizados

---

### 🔔 Escenario 3: Gestión de Notificaciones

**Pasos:**

1. **Ver notificaciones:**
   ```
   - Click en icono 🔔
   - ✅ Ver lista de notificaciones
   - ✅ Contador de no leídas
   ```

2. **Marcar como leída:**
   ```
   - Click en una notificación
   - ✅ Marca automáticamente como leída
   - ✅ Contador disminuye
   ```

3. **Filtrar por tipo:**
   ```
   - Click en dropdown de filtros
   - Seleccionar: "Document Shared"
   - ✅ Solo notificaciones de documentos
   ```

4. **Marcar todas como leídas:**
   ```
   - Click en menú ⋮
   - Click "Mark all as read"
   - ✅ Todas las notificaciones marcadas
   - ✅ Contador = 0
   ```

---

### ✏️ Escenario 4: Edición Colaborativa en Tiempo Real

**Pasos:**

1. **Usuario A y B en el mismo documento:**
   ```
   Usuario A:
   - Abrir documento compartido
   - Escribir en párrafo 1

   Usuario B:
   - Abrir mismo documento
   - ✅ Ver avatar de Usuario A
   - ✅ Ver banner: "User A is editing"
   - Escribir en párrafo 2

   Usuario A:
   - ✅ Ver cambios de Usuario B inmediatamente
   - ✅ Sin conflictos
   ```

2. **Aplicar formato:**
   ```
   Usuario A:
   - Seleccionar texto
   - Click en "B" (negrita)
   - ✅ Usuario B ve el formato aplicado

   Usuario B:
   - Aplicar cursiva a su texto
   - ✅ Usuario A lo ve inmediatamente
   ```

**Resultado esperado:**
- ✅ Presencia de usuarios visible
- ✅ Cambios sincronizados < 1 segundo
- ✅ No hay pérdida de datos
- ✅ Formato preservado

---

### 🔐 Escenario 5: Permisos y Seguridad

**Pasos:**

1. **Compartir como Viewer:**
   ```
   Usuario A:
   - Compartir documento con Usuario C
   - Rol: "Viewer"

   Usuario C:
   - Abrir documento
   - ✅ Puede ver contenido
   - ❌ NO puede editar
   - ✅ Toolbar de formato deshabilitado
   ```

2. **Cambiar a Editor:**
   ```
   Usuario A:
   - Ir a permisos del documento
   - Cambiar Usuario C a "Editor"

   Usuario C:
   - Refrescar documento
   - ✅ Ahora puede editar
   - ✅ Toolbar habilitado
   ```

3. **Revocar acceso:**
   ```
   Usuario A:
   - Ir a permisos
   - Click en "Remove" junto a Usuario C

   Usuario C:
   - Intentar abrir documento
   - ✅ Error: "Access denied"
   ```

---

### 📋 Escenario 6: Tareas con Colaboración

**Pasos:**

1. **Crear tarea:**
   ```
   - Click en botón "+"
   - Título: "Revisar documento colaborativo"
   - Prioridad: HIGH
   - Modo: Event
   - Fecha: Mañana
   - Click "Create"
   ```

2. **Ver en tiempo real:**
   ```
   Usuario A:
   - ✅ Tarea aparece inmediatamente

   Usuario B (en otra sesión):
   - ✅ Tarea aparece sin refrescar
   - ✅ WebSocket sincronizando
   ```

3. **Completar tarea:**
   ```
   - Click en checkbox de tarea
   - ✅ Marca como completada
   - ✅ Filtro "Completed" la muestra
   - ✅ Usuario B ve el cambio
   ```

---

## 🐛 Problemas Comunes

### 1. Backend no inicia

**Error:** "Port 8080 already in use"

**Solución:**
```bash
# Windows
netstat -ano | findstr :8080
taskkill /PID <PID> /F

# Linux/Mac
lsof -ti:8080 | xargs kill -9
```

### 2. Android no compila

**Error:** "SDK location not found"

**Solución:**
```bash
# Crear android/local.properties
echo "sdk.dir=C:\\Users\\<usuario>\\AppData\\Local\\Android\\Sdk" > android/local.properties

# Linux/Mac
echo "sdk.dir=/home/<usuario>/Android/Sdk" > android/local.properties
```

### 3. WebSocket no conecta

**Síntomas:**
- Banner rojo "Disconnected"
- No hay cambios en tiempo real

**Solución:**
1. Verificar que backend esté corriendo
2. Verificar URL en `ApiConfig.kt`: `ws://10.0.2.2:8080/ws` (emulador) o `ws://TU_IP:8080/ws` (dispositivo físico)
3. Revisar logs del backend para ver conexión

### 4. Notificaciones no aparecen

**Solución:**
1. Verificar que el documento se compartió exitosamente
2. Revisar logs del backend: `Notification sent to user X`
3. Refresh la pantalla de notificaciones (pull to refresh)

---

## 📊 Checklist de Funcionalidades

### ✅ Autenticación
- [ ] Login funcional
- [ ] Register funcional
- [ ] Logout funcional
- [ ] JWT token válido

### ✅ Tareas
- [ ] Crear tarea
- [ ] Editar tarea
- [ ] Eliminar tarea
- [ ] Marcar como completada
- [ ] Filtros funcionan
- [ ] Sincronización real-time

### ✅ Documentos
- [ ] Crear documento
- [ ] Editar contenido
- [ ] Aplicar formato (negrita, cursiva, listas)
- [ ] Auto-guardado funciona
- [ ] Ver historial de versiones

### ✅ Colaboración
- [ ] Compartir documento por email
- [ ] Roles (viewer/editor) funcionan
- [ ] Ver usuarios activos
- [ ] Edición simultánea sin conflictos
- [ ] Revocar acceso funciona

### ✅ Notificaciones
- [ ] Recibir notificación al compartir
- [ ] Contador de no leídas
- [ ] Marcar como leída funciona
- [ ] Deep link a documento funciona
- [ ] Filtros por tipo funcionan

### ✅ Chat
- [ ] Crear chat directo
- [ ] Crear chat grupal
- [ ] Enviar mensajes
- [ ] Recibir mensajes en tiempo real
- [ ] Editar mensaje propio
- [ ] Eliminar mensaje propio

---

## 🎯 Test de Estrés

### Múltiples usuarios simultáneos:

```
1. Abrir 3 instancias (emulador + 2 dispositivos)
2. Todos abriendo el mismo documento
3. Todos editando al mismo tiempo
4. ✅ Verificar que no hay pérdida de datos
5. ✅ Verificar que la sincronización funciona
```

### Muchas notificaciones:

```
1. Compartir 20 documentos con un usuario
2. Ese usuario debe tener 20 notificaciones
3. ✅ Marcar todas como leídas funciona
4. ✅ Eliminar todas funciona
5. ✅ Paginación funciona (si más de 50)
```

---

## 📈 Métricas de Success

**Rendimiento:**
- ⏱️ Login: < 2 segundos
- ⏱️ Crear documento: < 1 segundo
- ⏱️ Compartir documento: < 2 segundos
- ⏱️ Sincronización WebSocket: < 500ms
- ⏱️ Notificación enviada: < 1 segundo

**Estabilidad:**
- 🔄 Reconexión automática funciona
- 🔄 Offline-first: cambios se guardan localmente
- 🔄 No hay crashes al navegar

**UX:**
- ✨ Animaciones fluidas
- ✨ Loading states claros
- ✨ Errores informativos
- ✨ Deep links funcionan

---

## 🎉 Checklist Final de Demostración

Para una demo exitosa, asegúrate de probar:

1. [ ] Login con 2 usuarios diferentes
2. [ ] Crear documento
3. [ ] Compartir por email
4. [ ] Recibir notificación
5. [ ] Abrir desde deep link
6. [ ] Editar simultáneamente
7. [ ] Ver avatares de usuarios activos
8. [ ] Enviar mensaje de chat
9. [ ] Revocar acceso
10. [ ] Verificar que el acceso fue revocado

---

**¡Todo listo para probar! 🚀**

Si encuentras algún problema, revisa los logs del backend y del dispositivo Android.
