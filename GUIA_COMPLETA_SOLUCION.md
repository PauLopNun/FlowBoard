# 🚀 GUÍA COMPLETA: Solución de Problemas de Login y Backend

## ✅ Resumen de lo que se ha Solucionado

### 1. **Problema de Inicio de Sesión en Android** ✨
- ✅ Mejorado manejo de errores con mensajes claros en español
- ✅ Aumentados los timeouts de red (30 segundos)
- ✅ Agregada información de debugging en la pantalla de login
- ✅ Botón para auto-rellenar credenciales de prueba
- ✅ Credenciales de demostración actualizadas

### 2. **Errores de Compilación del Backend** 🔧
- ✅ Eliminadas redeclaraciones de clases
- ✅ Corregida jerarquía de herencia de WebSocket messages
- ✅ Eliminado campo inexistente `synkLastModified`
- ✅ Agregado import faltante para operador `eq`
- ✅ Corregidos parámetros en WebSocketRoutes

---

## 📝 PASO A PASO: Qué Hacer Ahora

### Paso 1: Hacer Commit y Push de los Cambios 🔄

```bash
cd /home/paulopnun/AndroidStudioProjects/FlowBoard

# Verificar cambios
git status

# Agregar todos los cambios
git add backend/
git add android/
git add *.md
git add verify-backend.sh

# Hacer commit
git commit -m "Fix: Resolver errores de compilación del backend y mejorar login en Android

- Eliminadas redeclaraciones de clases WebSocket
- Corregida jerarquía de DocumentWebSocketMessage
- Agregados campos type a todas las clases de documento
- Mejorado manejo de errores en login con mensajes en español
- Aumentados timeouts de red a 30 segundos
- Agregada UI de debugging en pantalla de login
- Eliminado campo inexistente synkLastModified
- Agregado import faltante para operador eq SQL
- Corregidos parámetros en WebSocketRoutes"

# Push a Render
git push origin main
```

### Paso 2: Monitorear el Deployment en Render 👀

1. Abre tu navegador
2. Ve a: https://dashboard.render.com
3. Busca tu servicio "flowboard-api" o similar
4. Observa el log del deployment
5. **Espera a que diga "Deploy live"** (puede tardar 2-5 minutos)

### Paso 3: Verificar que el Backend Funciona 🧪

Opción A - Usando el script automático:
```bash
chmod +x verify-backend.sh
./verify-backend.sh
```

Opción B - Verificación manual:
```bash
# Test básico
curl https://flowboard-api-phrk.onrender.com/api/v1/auth/login \
  -X POST \
  -H "Content-Type: application/json" \
  -d '{"email":"test@flowboard.com","password":"password123"}'

# Si ves un token en la respuesta, ¡funciona! ✅
```

### Paso 4: Probar el Login en la App Android 📱

#### 4.1 Recompilar la App

Desde Android Studio:
```bash
cd android
./gradlew clean assembleDebug
```

O desde la raíz:
```bash
./compile-android.sh
```

#### 4.2 Instalar en el Dispositivo

```bash
adb install -r android/app/build/outputs/apk/debug/app-debug.apk
```

#### 4.3 Abrir la App y Probar

1. **Abre FlowBoard** en tu dispositivo/emulador
2. En la pantalla de login, toca **"Ver info de servidor ▼"**
3. Verifica que aparece: `https://flowboard-api-phrk.onrender.com`
4. Toca **"Usar credenciales de prueba"** (auto-rellena email y password)
5. Toca **"Sign In"**
6. **⏱️ ESPERA hasta 30-60 segundos** (el servidor puede estar "despertando")
7. Si ves errores claros, lee el mensaje - ahora son informativos

---

## 🐛 Troubleshooting

### Error: "No se puede conectar al servidor"

**Causa:** Sin internet o servidor caído

**Solución:**
1. ✅ Verifica WiFi/datos móviles
2. ✅ Ejecuta `./verify-backend.sh` para comprobar el servidor
3. ✅ Espera 30-60 segundos (servidor puede estar despertando)

### Error: "El servidor no responde"

**Causa:** Servidor de Render en modo "sleep"

**Solución:**
1. ✅ **Espera 60 segundos** después de tocar "Sign In"
2. ✅ El servidor se despertará automáticamente
3. ✅ Las siguientes peticiones serán instantáneas

### Error: "Usuario no encontrado"

**Causa:** No existe el usuario en la base de datos

**Solución:**
1. ✅ Toca **"Sign Up"** para crear una cuenta nueva
2. ✅ O usa las credenciales de prueba (si ya están en el backend):
   - Email: `test@flowboard.com`
   - Password: `password123`

### El Backend No Compila en Render

**Solución:**
1. ✅ Verifica que hiciste commit y push de TODOS los archivos
2. ✅ Revisa los logs en Render Dashboard
3. ✅ Asegúrate de que no hay más errores de Kotlin
4. ✅ Si falla, copia el error y avísame

---

## 📂 Archivos Modificados (Resumen)

### Backend (para deployment)
- ✅ `backend/src/main/kotlin/com/flowboard/data/models/WebSocketMessage.kt`
- ✅ `backend/src/main/kotlin/com/flowboard/data/models/DocumentWebSocketMessage.kt`
- ✅ `backend/src/main/kotlin/com/flowboard/domain/DocumentService.kt`
- ✅ `backend/src/main/kotlin/com/flowboard/domain/NotificationService.kt`
- ✅ `backend/src/main/kotlin/com/flowboard/routes/WebSocketRoutes.kt`

### Android (para login mejorado)
- ✅ `android/app/src/main/java/com/flowboard/data/remote/api/AuthApiService.kt`
- ✅ `android/app/src/main/java/com/flowboard/presentation/viewmodel/LoginViewModel.kt`
- ✅ `android/app/src/main/java/com/flowboard/presentation/viewmodel/RegisterViewModel.kt`
- ✅ `android/app/src/main/java/com/flowboard/presentation/ui/screens/auth/LoginScreen.kt`
- ✅ `android/app/src/main/java/com/flowboard/di/NetworkModule.kt`
- ✅ `android/local.properties` (configurado SDK)

### Documentación
- ✅ `SOLUCION_LOGIN.md` - Guía detallada de solución de login
- ✅ `BACKEND_ERRORS_FIXED.md` - Documentación de errores corregidos
- ✅ `verify-backend.sh` - Script de verificación

---

## 🎯 Checklist Final

Antes de probar, asegúrate de:

- [ ] Hiciste commit de todos los cambios
- [ ] Hiciste push a `origin main`
- [ ] El deployment en Render está "live" (verde)
- [ ] Ejecutaste `./verify-backend.sh` y el servidor responde
- [ ] Recompilaste la app Android
- [ ] Instalaste la nueva APK

---

## 📞 Si Necesitas Más Ayuda

### Opción 1: Revisar Logs

**Backend (Render):**
- Dashboard → Tu servicio → Logs tab
- Busca errores en color rojo

**Android (Logcat):**
```bash
adb logcat | grep -E "LoginViewModel|AuthApiService"
```

### Opción 2: Verificar URLs

**Backend debe responder en:**
- https://flowboard-api-phrk.onrender.com/api/v1/auth/login

**Puedes verificar en el navegador:**
- https://flowboard-api-phrk.onrender.com
  - Si ves algo (incluso 404), el servidor funciona

### Opción 3: Crear Usuario Manualmente

Si nada funciona, crea un usuario de prueba directamente:

```bash
curl -X POST https://flowboard-api-phrk.onrender.com/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@flowboard.com",
    "password": "password123",
    "username": "testuser",
    "fullName": "Test User"
  }'
```

Luego intenta hacer login con:
- Email: `test@flowboard.com`
- Password: `password123`

---

## 🎉 ¿Qué Esperar Cuando Funcione?

1. **En la app:**
   - Toca "Sign In" → Spinner de carga
   - Después de unos segundos → Navegación al Dashboard
   - ¡Ya estás dentro!

2. **En los logs (Logcat):**
   ```
   LoginViewModel: Login initiated for email: test@flowboard.com
   AuthApiService: Attempting login...
   AuthApiService: Response status: 200
   LoginViewModel: Login successful for user: testuser
   ```

---

## 📚 Documentos de Referencia

- `SOLUCION_LOGIN.md` - Troubleshooting detallado de login
- `BACKEND_ERRORS_FIXED.md` - Errores del backend corregidos
- `GOOGLE_SIGNIN_SETUP.md` - Configuración de Google Sign-In (futuro)
- `COMO_OBTENER_AUTH_DATA.md` - Guía de autenticación

---

**Última actualización:** 2026-01-08  
**Estado:** ✅ Todo listo para deployment y pruebas

**¡Buena suerte! 🚀**

