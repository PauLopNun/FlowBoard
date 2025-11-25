# 🎯 Pasos Manuales - FlowBoard

## ✅ Lo que YA está implementado (código listo)

- ✅ Backend WebSocket completo
- ✅ Cliente Android WebSocket
- ✅ Componentes UI integrados en TaskListScreen
- ✅ Repository AuthRepository para manejar tokens

---

## 📝 LO QUE TÚ NECESITAS HACER

### **Paso 1: Configurar valores de autenticación** ⏱️ 5 minutos

En el archivo:
```
android/app/src/main/java/com/flowboard/presentation/ui/screens/tasks/TaskListScreen.kt
```

**Líneas 49-51**, reemplaza estos valores hardcodeados:

```kotlin
val boardId = "board-123"  // ← Cámbialo por el ID real del board
val token = "your-jwt-token"  // ← Cámbialo por el token JWT del backend
val userId = "user-456"  // ← Cámbialo por el ID del usuario logueado
```

**Opciones para obtener estos valores:**

#### **Opción A: Valores hardcodeados para pruebas rápidas**
```kotlin
val boardId = "test-board-001"
val token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." // Token de prueba del backend
val userId = "test-user-001"
```

#### **Opción B: Desde AuthRepository (recomendado)**
```kotlin
val authRepository: AuthRepository = hiltViewModel()
val token = authRepository.getToken() ?: ""
val userId = authRepository.getUserId() ?: ""
val boardId = authRepository.getBoardId() ?: "default-board"
```

---

### **Paso 2: Iniciar el backend** ⏱️ 2 minutos

Abre una terminal en `C:\Users\paulo\Desktop\FlowBoard`:

```bash
flow backend
```

O manualmente:
```bash
cd backend
gradlew.bat run
```

**Deberías ver:**
```
Server started at http://0.0.0.0:8080
```

---

### **Paso 3: Compilar la app Android** ⏱️ 3-5 minutos

En otra terminal:

```bash
flow build
```

O manualmente:
```bash
cd android
gradlew.bat assembleDebug
```

**APK generado en:**
```
android/app/build/outputs/apk/debug/app-debug.apk
```

---

### **Paso 4: Ejecutar en emulador/dispositivo** ⏱️ 2 minutos

**Opción A: Desde terminal**
```bash
flow run
```

**Opción B: Desde Android Studio**
1. Abre el proyecto en Android Studio
2. Selecciona un emulador o dispositivo
3. Presiona Run ▶️

---

### **Paso 5: Probar colaboración en tiempo real** ⏱️ 5 minutos

1. **Abre la app en 2 emuladores/dispositivos diferentes**
2. **Login con usuarios diferentes** (si tienes auth implementado)
3. **Abre el mismo board en ambos**
4. **Crea una tarea en uno** → Debería aparecer instantáneamente en el otro ✨
5. **Verifica:**
   - ✅ Banner de conexión (verde = conectado)
   - ✅ Avatares de usuarios activos en el TopBar
   - ✅ Sincronización en tiempo real

---

## 🔧 Troubleshooting Común

### ❌ "WebSocket no conecta"

**Causa:** URL incorrecta para emulador

**Solución:** Verifica que en `TaskWebSocketClient.kt` uses:
```kotlin
private const val WS_URL = "ws://10.0.2.2:8080/ws/boards"  // Para emulador
// Para dispositivo físico usa: ws://TU_IP_LOCAL:8080/ws/boards
```

---

### ❌ "Cannot resolve symbol 'ActiveUsersList'"

**Causa:** Componentes UI no compilados

**Solución:**
```bash
cd android
gradlew.bat clean build
```

Luego en Android Studio:
```
File → Invalidate Caches → Invalidate and Restart
```

---

### ❌ "Backend crashea al iniciar"

**Causa:** PostgreSQL no configurado o no corriendo

**Solución:**
1. Instala PostgreSQL
2. O configura H2 en memoria (más simple para desarrollo)

En `backend/src/main/resources/application.conf`:
```hocon
ktor {
    deployment {
        port = 8080
    }
}
```

---

### ❌ "JWT token inválido"

**Causa:** Token expirado o incorrecto

**Solución:**
1. Genera un token nuevo desde el backend
2. Verifica que `JWT_SECRET` sea el mismo en backend y cliente
3. Para pruebas, usa un token de prueba con expiración larga

---

## 📊 Checklist de Verificación

Marca cada item cuando funcione:

- [ ] Backend corriendo en `localhost:8080`
- [ ] Android app compila sin errores
- [ ] TaskListScreen muestra componentes WebSocket
- [ ] Banner de conexión aparece (verde = conectado)
- [ ] Usuarios activos se muestran en TopBar
- [ ] Crear tarea en un dispositivo aparece en otro
- [ ] Editar tarea sincroniza en tiempo real
- [ ] Eliminar tarea sincroniza en tiempo real

---

## 🚀 Siguiente Nivel (Opcional)

### **Deployment en Producción** ⏱️ 1-2 horas

Ver guía completa: `docs/deployment-guide-render.md`

**Resumen rápido:**
1. Crea cuenta en [Render.com](https://render.com) (gratis)
2. Conecta tu repo GitHub
3. Crea PostgreSQL Database (gratis)
4. Crea Web Service con:
   - Build: `./gradlew clean build`
   - Start: `java -Xmx512m -jar build/libs/backend-all.jar`
5. Configura variables de entorno (JWT_SECRET, DATABASE_URL)
6. Actualiza URLs en Android a `wss://tu-app.onrender.com/ws/boards`

---

### **Publicar en Play Store** ⏱️ 2-4 horas + 1-7 días revisión

Ver guía completa: `docs/play-store-publishing-guide.md`

**Requisitos:**
- $25 USD cuenta de desarrollador
- Keystore de firma (generar con keytool)
- Assets: ícono 512x512, screenshots, feature graphic
- Política de privacidad (URL pública)

---

## 💡 Tips Finales

1. **Para desarrollo local:** Usa valores hardcodeados primero
2. **Para testing:** Abre 2 emuladores y prueba sincronización
3. **Para producción:** Implementa AuthRepository completo
4. **Monitorea logs:** Android Studio Logcat filtrado por "TaskWebSocketClient"

---

## 📞 Documentación Completa

- **ACCION_REQUERIDA.md** - Guía detallada en español
- **FINAL_IMPLEMENTATION_SUMMARY.md** - Resumen técnico completo
- **docs/websocket-architecture.md** - Arquitectura del sistema
- **docs/deployment-guide-render.md** - Deploy en Render
- **docs/play-store-publishing-guide.md** - Publicación Play Store

---

**Tiempo estimado total:** 15-30 minutos para tener colaboración en tiempo real funcionando localmente.

**¡Éxito! 🎉**

---

**Creado:** 2025-11-25
**Versión:** 1.0.0
