# Configuración de Google Sign-In para FlowBoard

## 🔑 Información de tu App

**Nombre del paquete:** `com.flowboard`

**SHA-1 Fingerprint (Debug):**
```
91:1B:D1:91:D4:AE:C4:CB:5F:1F:3D:D6:44:27:8B:38:F8:AD:42:2B
```

**SHA-256 Fingerprint (Debug):**
```
D3:AA:81:62:E0:DA:66:22:5C:64:E3:5B:AF:D9:C5:93:37:DB:BF:D9:F7:96:7B:6D:88:6D:B0:46:B8:D7:16:52
```

---

## 📋 Pasos de Configuración

### 1️⃣ Acceder a Google Cloud Console

1. Ve a: https://console.cloud.google.com/
2. Inicia sesión con tu cuenta de Google

### 2️⃣ Crear o Seleccionar Proyecto

**Opción A - Si ya tienes un proyecto:**
- Selecciona el proyecto existente desde el menú desplegable superior

**Opción B - Crear nuevo proyecto:**
1. Click en el selector de proyectos (arriba a la izquierda)
2. Click en "Nuevo Proyecto"
3. Nombre: `FlowBoard` (o el que prefieras)
4. Click en "Crear"

### 3️⃣ Configurar Pantalla de Consentimiento OAuth

1. Ve a: https://console.cloud.google.com/apis/credentials/consent
2. Selecciona **"Externo"** (para testing con cuentas personales)
3. Click en "Crear"
4. Completa la información:
   - **Nombre de la app:** FlowBoard
   - **Correo de asistencia:** tu-email@gmail.com
   - **Logo de la app:** (opcional)
   - **Dominio de la app:** (dejar vacío por ahora)
   - **Correo de contacto del desarrollador:** tu-email@gmail.com
5. Click en "Guardar y Continuar"
6. En **"Alcances"**: Click en "Guardar y Continuar" (no agregar alcances adicionales)
7. En **"Usuarios de prueba"**:
   - Click en "Add Users"
   - Agrega tu email personal para testing
   - Click en "Guardar y Continuar"
8. Revisa y click en "Volver al Panel"

### 4️⃣ Crear Credenciales OAuth 2.0

#### A. Credencial Android (REQUERIDO)

1. Ve a: https://console.cloud.google.com/apis/credentials
2. Click en "+ CREAR CREDENCIALES" → "ID de cliente de OAuth 2.0"
3. Tipo de aplicación: **"Android"**
4. Completa:
   - **Nombre:** FlowBoard Android
   - **Nombre del paquete:** `com.flowboard`
   - **SHA-1 de certificado de firma:**
     ```
     91:1B:D1:91:D4:AE:C4:CB:5F:1F:3D:D6:44:27:8B:38:F8:AD:42:2B
     ```
5. Click en "Crear"

#### B. Credencial Web (REQUERIDO - Para Credential Manager)

1. En la misma página de credenciales
2. Click en "+ CREAR CREDENCIALES" → "ID de cliente de OAuth 2.0"
3. Tipo de aplicación: **"Aplicación web"**
4. Completa:
   - **Nombre:** FlowBoard Web Client
   - **Orígenes autorizados:** (dejar vacío)
   - **URIs de redireccionamiento:** (dejar vacío)
5. Click en "Crear"
6. **IMPORTANTE:** Copia el "Client ID" que se muestra (lo necesitarás en el paso 6)

### 5️⃣ Habilitar APIs Necesarias

1. Ve a: https://console.cloud.google.com/apis/library
2. Busca y habilita estas APIs:
   - **Google Sign-In API**
   - **Identity Toolkit API**

Para habilitarlas:
- Click en cada API
- Click en "Habilitar"

### 6️⃣ Actualizar el Web Client ID en el Código

El Web Client ID actual en el código es:
```
387871911602-3ps8i85m95609nepmoboaaqcf7n40kos.apps.googleusercontent.com
```

**DEBES REEMPLAZARLO** con el Client ID que obtuviste en el paso 4B.

Edita el archivo:
```
android/app/src/main/java/com/flowboard/data/auth/GoogleAuthManager.kt
```

Línea 27:
```kotlin
private val webClientId = "TU-NUEVO-WEB-CLIENT-ID-AQUI.apps.googleusercontent.com"
```

### 7️⃣ Verificar configuración del Backend

Si tu backend necesita verificar el ID token de Google, asegúrate de:

1. El endpoint `/api/v1/auth/google` esté implementado
2. Usa la biblioteca de Google para verificar el token
3. El backend usa el mismo Web Client ID

---

## 🧪 Probar Google Sign-In

### Opción 1: Recompilar y Reinstalar

```bash
cd android
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Opción 2: Ejecutar directamente desde Android Studio

1. Abre el proyecto en Android Studio
2. Click en "Run" (▶️)
3. Selecciona un emulador o dispositivo físico

### Pasos de Prueba:

1. Abre la app
2. En la pantalla de login, click en "Sign in with Google"
3. Debería aparecer el selector de cuentas de Google
4. Selecciona tu cuenta
5. Acepta los permisos
6. La app debería iniciar sesión correctamente

---

## ❌ Solución de Problemas

### Error: "No credentials available"

**Causas comunes:**
- ✅ Ya agregamos el metadata al AndroidManifest.xml
- ⚠️ Falta crear las credenciales en Google Cloud Console (pasos 4A y 4B)
- ⚠️ El Web Client ID en GoogleAuthManager.kt no es correcto
- ⚠️ El SHA-1 registrado no coincide con el de tu keystore

**Solución:**
1. Verifica que completaste los pasos 4A y 4B
2. Actualiza el Web Client ID (paso 6)
3. Recompila la app

### Error: "API not enabled"

**Solución:**
- Completa el paso 5 (habilitar APIs)

### Error: "Invalid client"

**Solución:**
- Verifica que el Web Client ID en GoogleAuthManager.kt sea correcto
- Asegúrate de haber creado tanto la credencial Android como la Web

### El diálogo de Google no aparece

**Solución:**
1. Verifica que estés usando un dispositivo/emulador con Google Play Services
2. Verifica que el usuario de prueba esté agregado (paso 3, punto 7)
3. Borra datos de la app y vuelve a intentar

---

## 📱 Dispositivos de Prueba

### Emulador
- Usa imágenes del sistema con **Google APIs** (no "Google APIs missing")
- Versión recomendada: Android 13 (API 33) o superior

### Dispositivo Físico
- Debe tener Google Play Services instalado
- Debe estar conectado a internet

---

## 🔐 Para Producción (Release)

Cuando vayas a publicar la app:

1. Genera un keystore de release
2. Obtén el SHA-1 del keystore de release:
   ```bash
   keytool -list -v -keystore path/to/release.keystore -alias your-alias
   ```
3. Agrega el SHA-1 de release a las credenciales Android en Google Cloud Console
4. Actualiza el Web Client ID si es diferente

---

## 📞 Ayuda Adicional

- Documentación oficial: https://developers.google.com/identity/sign-in/android/start
- Credential Manager: https://developer.android.com/training/sign-in/credential-manager
- Google Cloud Console: https://console.cloud.google.com/

---

## ✅ Checklist Final

- [ ] Proyecto creado/seleccionado en Google Cloud Console
- [ ] Pantalla de consentimiento OAuth configurada
- [ ] Usuario de prueba agregado
- [ ] Credencial Android creada con SHA-1 correcto
- [ ] Credencial Web creada
- [ ] Web Client ID copiado y actualizado en GoogleAuthManager.kt
- [ ] APIs habilitadas (Google Sign-In e Identity Toolkit)
- [ ] App recompilada e instalada
- [ ] Probado en dispositivo/emulador con Google Play Services

---

**Última actualización:** 2025-11-27
