# Guía Completa de Publicación en Google Play Store - FlowBoard

## 📋 Índice
1. [Preparación Previa](#preparación-previa)
2. [Configurar Keystore de Firma](#configurar-keystore-de-firma)
3. [Configurar Build para Release](#configurar-build-para-release)
4. [Generar APK/AAB de Release](#generar-apkaab-de-release)
5. [Crear Cuenta de Desarrollador](#crear-cuenta-de-desarrollador)
6. [Configurar la Aplicación en Play Console](#configurar-la-aplicación-en-play-console)
7. [Preparar Assets (Capturas, Íconos, etc.)](#preparar-assets)
8. [Subir APK/AAB](#subir-apkaab)
9. [Configurar Privacidad y Clasificación](#configurar-privacidad-y-clasificación)
10. [Enviar para Revisión](#enviar-para-revisión)
11. [Post-Publicación](#post-publicación)

---

## 📝 Preparación Previa

### Checklist Antes de Publicar

- [ ] La app funciona correctamente en varios dispositivos
- [ ] No hay crashes ni bugs críticos
- [ ] Todas las funciones principales están implementadas
- [ ] El backend está desplegado y funcionando (Render)
- [ ] Las URLs de producción están configuradas en la app
- [ ] Has probado la app con datos reales
- [ ] Tienes preparados todos los assets (íconos, capturas, etc.)
- [ ] Tienes preparada la descripción y textos de marketing

---

## 🔐 Configurar Keystore de Firma

### ¿Qué es un Keystore?

Un keystore es un archivo que contiene las claves criptográficas para firmar tu aplicación. **¡MUY IMPORTANTE:**
- **NUNCA** compartas tu keystore
- **NUNCA** lo subas a GitHub
- **NUNCA** pierdas este archivo (sin él no podrás actualizar tu app)
- **GUÁRDALO** en un lugar seguro (2-3 copias de respaldo)

### Paso 1: Generar Keystore

Abre terminal en el directorio del proyecto:

```bash
cd C:\Users\paulo\Desktop\FlowBoard\android\app

# Crear directorio para keystores (ignorado por git)
mkdir keystore

# Generar keystore
keytool -genkey -v -keystore keystore/flowboard-release.jks \
  -alias flowboard \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000

# Si keytool no se encuentra, busca tu JDK:
# Windows: C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe
```

**Te pedirá:**
1. **Contraseña del keystore:** Elige una contraseña segura (mínimo 6 caracteres)
2. **Nombre y apellido:** Tu nombre o nombre de la compañía
3. **Unidad organizacional:** Ej: "Development"
4. **Organización:** Ej: "FlowBoard"
5. **Ciudad:** Tu ciudad
6. **Estado/Provincia:** Tu estado
7. **Código de país:** Ej: "ES" para España
8. **Contraseña de la clave:** Puedes usar la misma que el keystore (presiona Enter)

**Ejemplo de salida:**
```
Generating 2,048 bit RSA key pair and self-signed certificate (SHA256withRSA)
with a validity of 10,000 days for: CN=Paulo López, OU=Development, O=FlowBoard, L=Madrid, ST=Madrid, C=ES
[Storing keystore/flowboard-release.jks]
```

### Paso 2: Guardar Credenciales de Forma Segura

Crea un archivo `keystore/keystore.properties` (NO lo subas a Git):

```properties
storeFile=./keystore/flowboard-release.jks
storePassword=TU_CONTRASEÑA_KEYSTORE
keyAlias=flowboard
keyPassword=TU_CONTRASEÑA_KEY
```

### Paso 3: Actualizar .gitignore

Asegúrate de que `android/.gitignore` incluya:

```
# Keystores
*.jks
*.keystore
keystore/
keystore.properties
```

---

## ⚙️ Configurar Build para Release

### Paso 1: Actualizar app/build.gradle

```kotlin
android {
    namespace = "com.flowboard"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.flowboard"
        minSdk = 24
        targetSdk = 34
        versionCode = 1      // Incrementa con cada release
        versionName = "1.0.0" // Versión visible para usuarios

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Cargar keystore properties
    val keystorePropertiesFile = rootProject.file("keystore/keystore.properties")
    val keystoreProperties = java.util.Properties()

    if (keystorePropertiesFile.exists()) {
        keystoreProperties.load(java.io.FileInputStream(keystorePropertiesFile))
    }

    signingConfigs {
        create("release") {
            if (keystorePropertiesFile.exists()) {
                storeFile = file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true  // Activar ProGuard/R8
            isShrinkResources = true // Eliminar recursos no usados
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-DEBUG"
        }
    }

    // Configuración para Android App Bundle
    bundle {
        language {
            enableSplit = true
        }
        density {
            enableSplit = true
        }
        abi {
            enableSplit = true
        }
    }
}
```

### Paso 2: Configurar ProGuard Rules

En `android/app/proguard-rules.pro`:

```pro
# FlowBoard ProGuard Rules

# Keep Retrofit
-keepattributes Signature
-keepattributes Exceptions
-keep class retrofit2.** { *; }

# Keep OkHttp
-dontwarn okhttp3.**
-keep class okhttp3.** { *; }

# Keep Ktor
-keep class io.ktor.** { *; }
-keepclassmembers class io.ktor.** { *; }

# Keep kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Keep models
-keep class com.flowboard.domain.model.** { *; }
-keep class com.flowboard.data.local.entities.** { *; }
-keep class com.flowboard.data.remote.dto.** { *; }

# Keep Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# Keep Compose
-keep class androidx.compose.** { *; }
```

### Paso 3: Actualizar AndroidManifest.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <!-- Permisos -->
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

    <application
        android:name=".FlowBoardApplication"
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.FlowBoard"
        android:usesCleartextTraffic="false">  <!-- ← IMPORTANTE: false en producción -->

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:theme="@style/Theme.FlowBoard"
            android:screenOrientation="portrait">  <!-- ← Opcional: forzar portrait -->
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

---

## 📦 Generar APK/AAB de Release

### Opción 1: Android App Bundle (AAB) - Recomendado

**Google Play requiere AAB para nuevas apps desde agosto 2021.**

```bash
cd C:\Users\paulo\Desktop\FlowBoard\android

# Generar AAB firmado
gradlew.bat bundleRelease

# El archivo estará en:
# app/build/outputs/bundle/release/app-release.aab
```

**Tamaño aproximado:** 8-15 MB (sin comprimir)

### Opción 2: APK (Para testing o distribución directa)

```bash
cd C:\Users\paulo\Desktop\FlowBoard\android

# Generar APK firmado
gradlew.bat assembleRelease

# El archivo estará en:
# app/build/outputs/apk/release/app-release.apk
```

**Tamaño aproximado:** 20-30 MB

### Verificar la Firma

```bash
# Para AAB
jarsigner -verify -verbose -certs app/build/outputs/bundle/release/app-release.aab

# Para APK
jarsigner -verify -verbose -certs app/build/outputs/apk/release/app-release.apk

# Debe mostrar "jar verified" sin warnings
```

---

## 💳 Crear Cuenta de Desarrollador

### Paso 1: Registrarse en Google Play Console

1. Ve a [https://play.google.com/console](https://play.google.com/console)
2. Haz clic en **"Sign up"**
3. Inicia sesión con tu cuenta de Google

### Paso 2: Pagar la Cuota de Registro

- **Costo:** $25 USD (pago único de por vida)
- **Métodos de pago:** Tarjeta de crédito/débito
- Este pago es **no reembolsable**

### Paso 3: Completar tu Perfil

**Información requerida:**
- Nombre del desarrollador (público)
- Email de contacto
- Sitio web (opcional pero recomendado)
- Dirección física
- Número de teléfono

**Tipo de cuenta:**
- **Individual:** Para desarrolladores independientes
- **Organización:** Requiere documentación legal de la empresa

### Paso 4: Aceptar Términos

Lee y acepta:
- Acuerdo de Distribución del Desarrollador de Google Play
- Políticas del Programa para Desarrolladores
- Exportación de EE.UU. y leyes de sanciones

---

## 🎮 Configurar la Aplicación en Play Console

### Paso 1: Crear Nueva Aplicación

1. En Play Console, haz clic en **"Create app"**
2. Selecciona:
   - **App name:** FlowBoard
   - **Default language:** Spanish (o tu idioma)
   - **App or game:** App
   - **Free or paid:** Free
3. Marca las casillas de declaración
4. Haz clic en **"Create app"**

### Paso 2: Configurar Detalles de la Aplicación

#### 2.1 App Details

**Ubicación:** Dashboard → App details

- **App name:** FlowBoard
- **Short description:** (máx. 80 caracteres)
  ```
  Gestión de tareas colaborativa en tiempo real con tableros Kanban
  ```
- **Full description:** (máx. 4000 caracteres)
  ```
  FlowBoard es la aplicación definitiva para gestión de tareas y colaboración en equipo.

  CARACTERÍSTICAS PRINCIPALES:
  ✓ Tableros Kanban colaborativos
  ✓ Sincronización en tiempo real
  ✓ Gestión de proyectos
  ✓ Tareas con prioridades y fechas límite
  ✓ Calendario de eventos
  ✓ Etiquetas y filtros personalizables
  ✓ Modo offline-first
  ✓ Colaboración multi-usuario
  ✓ Notificaciones de cambios en tiempo real

  IDEAL PARA:
  • Equipos remotos
  • Gestión de proyectos
  • Organización personal
  • Estudiantes y educadores
  • Startups y pequeñas empresas

  PRIVACIDAD Y SEGURIDAD:
  • Autenticación segura
  • Datos encriptados
  • Sin publicidad
  • Respetamos tu privacidad

  SOPORTE:
  ¿Necesitas ayuda? Contáctanos en support@flowboard.com
  ```

- **Category:** Productivity
- **Tags:** project management, tasks, productivity, collaboration
- **Contact details:**
  - **Website:** https://flowboard.com (o tu sitio)
  - **Email:** support@flowboard.com
  - **Phone:** +34 XXX XXX XXX (opcional)

#### 2.2 Store Listing

Aquí necesitas subir assets visuales (ver sección siguiente).

---

## 🎨 Preparar Assets

### 1. App Icon

**Requisitos:**
- **Tamaño:** 512 x 512 px
- **Formato:** PNG (32-bit)
- **Sin transparencias**
- **Debe verse bien en diferentes fondos**

**Ubicación en proyecto:**
```
android/app/src/main/res/
├── mipmap-hdpi/ic_launcher.png (72x72)
├── mipmap-mdpi/ic_launcher.png (48x48)
├── mipmap-xhdpi/ic_launcher.png (96x96)
├── mipmap-xxhdpi/ic_launcher.png (144x144)
└── mipmap-xxxhdpi/ic_launcher.png (192x192)
```

**Herramientas para crear íconos:**
- [Android Asset Studio](https://romannurik.github.io/AndroidAssetStudio/icons-launcher.html)
- [Figma](https://www.figma.com/)
- [Canva](https://www.canva.com/)

### 2. Feature Graphic

**Requisitos:**
- **Tamaño:** 1024 x 500 px
- **Formato:** PNG o JPEG
- **Peso máximo:** 1 MB
- **Contenido:** Banner promocional de la app

**Ejemplo de diseño:**
```
┌────────────────────────────────────────┐
│  [Logo]      FlowBoard                 │
│  Gestión de tareas colaborativa        │
│  [Mockup de la app]                    │
└────────────────────────────────────────┘
```

### 3. Screenshots (Capturas de Pantalla)

**Requisitos:**
- **Mínimo:** 2 capturas (recomendado: 4-8)
- **Tamaño:** Entre 320px y 3840px
- **Formato:** PNG o JPEG
- **Orientación:** Portrait (para apps móviles)

**Capturas recomendadas:**
1. Pantalla de login/bienvenida
2. Lista de tareas (vista principal)
3. Detalle de tarea
4. Tablero Kanban
5. Calendario de eventos
6. Colaboración en tiempo real
7. Filtros y búsqueda
8. Configuración

**Tips:**
- Usa dispositivos con pantallas grandes (ej: Pixel 6, Galaxy S21)
- Muestra datos reales (no lorem ipsum)
- Destaca features principales
- Usa texto descriptivo en las capturas (opcional)

### 4. Promotional Video (Opcional)

**Requisitos:**
- **Duración:** 30 segundos - 2 minutos
- **Formato:** YouTube URL
- **Contenido:** Demo de la app, features principales

### Generar Screenshots con Android Studio

1. Ejecuta la app en emulador
2. Usa diferentes tamaños de pantalla:
   - Pixel 6 (1080 x 2400)
   - Pixel 6 Pro (1440 x 3120)
3. Navega a cada pantalla importante
4. Presiona el botón de cámara en el emulador
5. Las capturas se guardan en: `C:\Users\usuario\.android\avd\...`

---

## ⬆️ Subir APK/AAB

### Paso 1: Crear Release en Play Console

1. Ve a **"Release" → "Production"**
2. Haz clic en **"Create new release"**
3. Si es tu primera vez, acepta usar Google Play App Signing

### Paso 2: Subir AAB

1. Haz clic en **"Upload"**
2. Selecciona tu archivo `app-release.aab`
3. Espera a que se procese (1-5 minutos)

Play Console mostrará:
- Versión (versionCode y versionName)
- Tamaño de descarga aproximado
- API levels soportados
- Arquitecturas (ARM, x86, etc.)

### Paso 3: Release Notes

Escribe lo nuevo en esta versión:

```
Version 1.0.0 (Primera versión)

• Gestión de tareas con prioridades
• Tableros colaborativos en tiempo real
• Sincronización automática
• Calendario de eventos
• Modo offline
• Interfaz moderna con Material Design 3
```

### Paso 4: Revisar y Guardar

1. Haz clic en **"Save"**
2. Revisa que no haya errores o advertencias
3. **NO hagas clic en "Review release" aún** (falta configurar más cosas)

---

## 🔒 Configurar Privacidad y Clasificación

### Paso 1: Privacy Policy (Política de Privacidad)

**Requisitos de Google:**
- URL pública con tu política de privacidad
- Debe explicar qué datos recoges y cómo los usas

**Generador gratuito:**
[https://www.freeprivacypolicy.com/](https://www.freeprivacypolicy.com/)

**Puntos clave a incluir:**
- Datos que recoges (email, nombre, tareas)
- Cómo usas los datos
- Con quién compartes datos (ninguno)
- Derechos del usuario (acceso, eliminación)
- Cookies y tracking (si aplica)

**Subir a GitHub Pages (gratis):**
1. Crea `privacy-policy.md` en tu repo
2. GitHub Pages → Habilitar
3. URL: `https://usuario.github.io/FlowBoard/privacy-policy.html`

**Configurar en Play Console:**
```
Dashboard → App content → Privacy policy → Add
```

### Paso 2: Data Safety

**Ubicación:** App content → Data safety

Responde las preguntas sobre qué datos recoges:

**¿Recopilas o compartes datos de usuario?**
- ✅ Sí

**Tipos de datos:**
- Información personal (nombre, email)
- Archivos y documentos (tareas, proyectos)

**¿Todos los datos están encriptados en tránsito?**
- ✅ Sí (HTTPS/WSS)

**¿Ofreces una manera de solicitar eliminación de datos?**
- ✅ Sí (email: support@flowboard.com)

**¿Los datos se usan solo para funcionalidad de la app?**
- ✅ Sí
- ❌ No para publicidad
- ❌ No para analytics de terceros

### Paso 3: App Content

**Target audience:**
- Ages 13+ (Teen and up)

**Content rating:**
- Complete cuestionario IARC
- Responde honestamente sobre contenido de la app
- FlowBoard probablemente será "Everyone" o "Teen"

**News app:**
- ❌ No

**COVID-19 contact tracing:**
- ❌ No

**Ads:**
- ❌ No contiene anuncios

**In-app purchases:**
- ❌ No (de momento)

---

## 🚀 Enviar para Revisión

### Paso 1: Checklist Final

- [ ] AAB subido y procesado
- [ ] Screenshots y assets subidos
- [ ] Descripción completa
- [ ] Política de privacidad configurada
- [ ] Data safety completado
- [ ] Content rating completado
- [ ] Todas las secciones en Play Console tienen ✅

### Paso 2: Revisar Release

1. Ve a **"Release" → "Production"**
2. Haz clic en tu draft release
3. Haz clic en **"Review release"**

### Paso 3: Enviar

1. Revisa todos los detalles
2. Haz clic en **"Start rollout to Production"**
3. Confirma

**Tiempo de revisión:** 1-7 días (usualmente 1-2 días)

### Durante la Revisión

Google revisará:
- Cumplimiento de políticas
- Funcionalidad de la app
- Contenido inapropiado
- Permisos excesivos
- Malware

**Recibirás un email cuando:**
- La app sea aprobada
- Haya problemas que corregir
- La app sea rechazada

---

## 📱 Post-Publicación

### Cuando tu App sea Aprobada

**Recibirás:**
- Email de confirmación
- La app estará visible en Play Store en 1-2 horas
- URL: `https://play.google.com/store/apps/details?id=com.flowboard`

### Monitoreo

**Play Console Dashboard muestra:**
- Instalaciones
- Calificaciones y reseñas
- Crashes y ANRs
- Estadísticas de rendimiento

### Responder Reseñas

- Responde a reseñas (especialmente negativas)
- Agradece feedback positivo
- Soluciona problemas reportados

### Actualizaciones

Para actualizar la app:

1. Incrementa `versionCode` y `versionName` en `build.gradle`
```kotlin
versionCode = 2
versionName = "1.0.1"
```

2. Genera nuevo AAB
```bash
gradlew.bat bundleRelease
```

3. En Play Console:
   - Production → Create new release
   - Upload nuevo AAB
   - Write release notes
   - Review and rollout

### Marketing

**Promociona tu app:**
- Comparte el link en redes sociales
- Crea landing page
- Escribe blog post
- Pide a amigos que la prueben y califiquen
- Considera Google Ads (opcional)

**Link directo:**
```
https://play.google.com/store/apps/details?id=com.flowboard
```

**Badge de Play Store:**
```html
<a href='https://play.google.com/store/apps/details?id=com.flowboard'>
  <img alt='Get it on Google Play'
       src='https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png'/>
</a>
```

---

## ⚠️ Problemas Comunes

### App Rechazada

**Razones comunes:**
1. **Política de privacidad ausente o insuficiente**
   - Solución: Actualiza tu política con más detalles

2. **Permisos no justificados**
   - Solución: Explica por qué necesitas cada permiso

3. **Contenido inapropiado**
   - Solución: Revisa contenido y cumple políticas

4. **App crashea durante revisión**
   - Solución: Prueba exhaustivamente antes de subir

5. **Metadata engañosa**
   - Solución: Sé honesto en descripción y screenshots

### Crashes Después de Publicar

**Play Console → Quality → Crashes**
- Ve stack traces
- Reproduce el crash
- Corrige y sube nueva versión

### Bajas Calificaciones

- Lee reseñas cuidadosamente
- Identifica problemas comunes
- Actualiza la app con fixes
- Responde a usuarios afectados

---

## 📊 Métricas de Éxito

### Primeros 30 Días

**Objetivos realistas:**
- Instalaciones: 50-100
- Calificación promedio: > 4.0 estrellas
- Tasa de retención (día 1): > 40%

### Crecimiento Orgánico

**Factores clave:**
- Boca a boca
- Calificaciones positivas
- Updates regulares
- Respuesta a feedback

---

## 🎯 Checklist Completo

### Pre-Publicación
- [ ] App funcional sin bugs críticos
- [ ] Backend en producción (Render)
- [ ] URLs de producción configuradas
- [ ] Keystore generado y respaldado
- [ ] Build firmado (AAB)
- [ ] Cuenta de desarrollador creada ($25)

### Assets
- [ ] Ícono de la app (512x512)
- [ ] Feature graphic (1024x500)
- [ ] Mínimo 2 screenshots
- [ ] Descripciones escritas
- [ ] Política de privacidad publicada

### Play Console
- [ ] App creada
- [ ] AAB subido
- [ ] Data safety completado
- [ ] Content rating completado
- [ ] Release notes escritos

### Post-Publicación
- [ ] App publicada y visible
- [ ] Link compartido
- [ ] Monitoreo de crashes
- [ ] Respuesta a reseñas

---

## 🎉 ¡Felicidades!

Si completaste todos los pasos, **¡tu app está en Google Play Store!** 🚀

**Próximos pasos:**
1. Monitorea métricas en Play Console
2. Responde a usuarios
3. Planifica próximas features
4. Prepara actualizaciones regulares

**Recursos adicionales:**
- [Play Console Help](https://support.google.com/googleplay/android-developer/)
- [Android Developers Guides](https://developer.android.com/distribute)
- [Material Design Guidelines](https://m3.material.io/)

---

**Versión:** 1.0.0
**Última actualización:** 2025-11-25
**Autor:** FlowBoard Team
**Contacto:** support@flowboard.com
