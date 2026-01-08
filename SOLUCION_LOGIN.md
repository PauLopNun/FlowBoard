# 🔧 Solución al Problema de Inicio de Sesión

## ✅ Cambios Realizados

He implementado las siguientes soluciones para resolver el problema de inicio de sesión:

### 1. **Mejor Manejo de Errores** ✨
- Mensajes de error más claros y específicos
- Detección de problemas de red (sin internet, servidor no responde, timeout)
- Logs detallados para debugging

### 2. **Timeouts Aumentados** ⏱️
- Timeout de conexión: 30 segundos (antes: default ~10s)
- Timeout de socket: 30 segundos
- Esto da tiempo al servidor de Render para "despertar" si estaba inactivo

### 3. **Interfaz Mejorada** 🎨
- Botón para ver información del servidor actual
- Botón para auto-rellenar credenciales de prueba
- Credenciales de demostración actualizadas

### 4. **Debugging Facilitado** 🔍
- Información de la URL del servidor visible
- Sugerencias de troubleshooting en pantalla
- Logs más detallados en Logcat

---

## 🧪 Cómo Probar

### Opción 1: Usar Credenciales de Prueba

1. Abre la app
2. En la pantalla de login, toca **"Usar credenciales de prueba"**
3. Toca **"Sign In"**
4. **Espera hasta 30 segundos** (el servidor puede estar "durmiendo")

Las credenciales de prueba son:
```
Email: test@flowboard.com
Password: password123
```

### Opción 2: Registrar Nueva Cuenta

1. En la pantalla de login, toca **"Sign Up"**
2. Completa el formulario
3. Toca **"Register"**

---

## 🐛 Troubleshooting

### Problema: "No se puede conectar al servidor"

**Causas posibles:**
1. ❌ No hay conexión a internet
2. ❌ El servidor de Render está caído
3. ❌ Firewall bloqueando la conexión

**Soluciones:**
1. ✅ Verifica tu conexión WiFi/datos móviles
2. ✅ Abre https://flowboard-api-phrk.onrender.com/api/v1/auth en tu navegador
   - Si carga, el servidor funciona
   - Si no carga, espera unos minutos y reintenta
3. ✅ Intenta con otra red (WiFi diferente o datos móviles)

### Problema: "El servidor no responde"

**Causa:** El servidor de Render se "duerme" después de 15 minutos sin uso

**Solución:**
1. ✅ **Espera 30-60 segundos** después de tocar "Sign In"
2. ✅ El servidor despertará y procesará tu login
3. ✅ Las siguientes peticiones serán instantáneas

### Problema: "Credenciales incorrectas"

**Soluciones:**
1. ✅ Verifica que estés usando:
   - Email: `test@flowboard.com`
   - Password: `password123`
2. ✅ O crea una cuenta nueva con "Sign Up"

### Problema: "Usuario no encontrado"

**Causa:** No existe ningún usuario con ese email en la base de datos

**Solución:**
1. ✅ Usa las credenciales de prueba: `test@flowboard.com` / `password123`
2. ✅ O registra una cuenta nueva tocando "Sign Up"

---

## 🔍 Ver Logs de Debugging

Para ver qué está pasando internamente:

1. Conecta tu dispositivo/emulador a Android Studio
2. Abre la ventana **Logcat**
3. Filtra por: `LoginViewModel` o `AuthApiService`
4. Intenta hacer login
5. Verás logs como:
   ```
   LoginViewModel: Login initiated for email: test@flowboard.com
   AuthApiService: Attempting login for email: test@flowboard.com
   AuthApiService: Login URL: https://flowboard-api-phrk.onrender.com/api/v1/auth/login
   AuthApiService: Response status: 200
   LoginViewModel: Login successful for user: test
   ```

---

## 📡 Verificar el Backend

### Método 1: Navegador Web

Abre en tu navegador:
```
https://flowboard-api-phrk.onrender.com/api/v1/auth
```

Si ves algo (incluso un error 404), el servidor funciona.

### Método 2: cURL (Terminal)

```bash
# Test de registro
curl -X POST https://flowboard-api-phrk.onrender.com/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@flowboard.com",
    "password": "password123",
    "username": "testuser",
    "fullName": "Test User"
  }'

# Test de login
curl -X POST https://flowboard-api-phrk.onrender.com/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@flowboard.com",
    "password": "password123"
  }'
```

---

## 🔄 Cambiar a Servidor Local (Para Desarrollo)

Si quieres usar un backend local en tu máquina:

1. Abre: `android/app/src/main/java/com/flowboard/data/remote/ApiConfig.kt`

2. Cambia la línea 13:
   ```kotlin
   // ANTES:
   private const val USE_PRODUCTION = true
   
   // DESPUÉS:
   private const val USE_PRODUCTION = false
   ```

3. Asegúrate de que el backend esté corriendo en `localhost:8080`

4. Recompila la app

**Nota:** Para emulador Android, `10.0.2.2` = `localhost` de tu PC

---

## 📝 Crear Usuario de Prueba en el Backend

Si el usuario `test@flowboard.com` no existe, créalo manualmente:

### Opción A: Desde la App (Recomendado)

1. En la pantalla de login, toca **"Sign Up"**
2. Completa:
   - Email: `test@flowboard.com`
   - Username: `testuser`
   - Full Name: `Test User`
   - Password: `password123`
   - Confirm Password: `password123`
3. Toca **"Register"**

### Opción B: Desde cURL

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

---

## 🎯 Resumen de los Archivos Modificados

1. **AuthApiService.kt** - Mejor manejo de errores HTTP
2. **LoginViewModel.kt** - Mensajes de error más claros
3. **NetworkModule.kt** - Timeouts aumentados (30s)
4. **LoginScreen.kt** - UI mejorada con debugging info
5. **local.properties** - SDK configurado

---

## ✨ Próximos Pasos

Después de que el login funcione:

1. ✅ Prueba Google Sign-In (requiere configuración adicional en Google Cloud Console)
2. ✅ Verifica que la navegación al Dashboard funcione
3. ✅ Prueba crear tareas y documentos
4. ✅ Verifica la sincronización en tiempo real

---

## 🆘 Si Nada Funciona

1. Verifica los logs en Logcat
2. Comprueba que el servidor esté online: https://flowboard-api-phrk.onrender.com
3. Intenta registrar una cuenta nueva en lugar de usar credenciales de prueba
4. Espera al menos 30-60 segundos después de tocar "Sign In" (servidor puede estar despertando)

---

**Última actualización:** 2026-01-08

