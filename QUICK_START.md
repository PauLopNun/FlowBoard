# ⚡ FlowBoard - Quick Start Guide

Guía rápida para empezar a desarrollar en FlowBoard en menos de 5 minutos.

## 🎯 Lo Más Importante

**FlowBoard es un proyecto monorepo** con dos partes principales:
- **`android/`** - App móvil (Kotlin + Jetpack Compose)
- **`backend/`** - API REST (Ktor + PostgreSQL)

**La app funciona completamente offline** - no necesitas el backend para desarrollar el frontend.

## 🚀 Inicio Rápido (Solo Android)

### 1️⃣ Abrir el Proyecto

**Opción Recomendada:**
```
Android Studio → File → Open → FlowBoard/android/
```

**Alternativa (desde raíz):**
```
Android Studio → File → Open → FlowBoard/
```

### 2️⃣ Compilar y Ejecutar

**Desde Android Studio:**
1. Espera a que Gradle sincronice
2. Click en Run ▶️ (o presiona Shift+F10)
3. ¡Listo!

**Desde terminal:**
```bash
# Windows
flow.bat build
flow.bat run

# Linux/Mac
./flow.sh build
./flow.sh run
```

## 📱 Usar la App

La app incluye datos de demo locales:

- **Email:** demo@flowboard.com
- **Password:** demo123

Puedes crear tareas, eventos y proyectos sin necesidad de backend.

## 🖥️ Inicio Rápido (Con Backend)

### Prerrequisitos
- PostgreSQL instalado y corriendo
- Puerto 8080 disponible

### Pasos

**1. Crear base de datos:**
```bash
createdb flowboard
```

**2. Configurar variables de entorno:**
```bash
# Linux/Mac
export DATABASE_URL="jdbc:postgresql://localhost:5432/flowboard"
export DATABASE_USER="tu_usuario"
export DATABASE_PASSWORD="tu_contraseña"
export JWT_SECRET="tu_secreto_super_seguro"

# Windows (PowerShell)
$env:DATABASE_URL="jdbc:postgresql://localhost:5432/flowboard"
$env:DATABASE_USER="tu_usuario"
$env:DATABASE_PASSWORD="tu_contraseña"
$env:JWT_SECRET="tu_secreto_super_seguro"
```

**3. Iniciar backend:**
```bash
# Con script de utilidades
./flow.sh backend    # Linux/Mac
flow.bat backend     # Windows

# O manualmente
cd backend
./gradlew run
```

**4. Verificar que funciona:**
```bash
curl http://localhost:8080/health
# Debería responder: {"status":"ok"}
```

## 🛠️ Comandos Útiles

### Script de Utilidades

```bash
# Ver ayuda completa
./flow.sh help       # Linux/Mac
flow.bat help        # Windows

# Compilar app Android
./flow.sh build

# Ejecutar tests
./flow.sh test

# Limpiar builds
./flow.sh clean

# Instalar en dispositivo
./flow.sh run

# Iniciar backend
./flow.sh backend
```

### Comandos Gradle Directos

```bash
# Android (desde raíz)
./gradlew -p android assembleDebug
./gradlew -p android test
./gradlew -p android installDebug

# Android (desde android/)
cd android
./gradlew assembleDebug
./gradlew test
./gradlew installDebug

# Backend (desde raíz)
./gradlew -p backend build
./gradlew -p backend test
./gradlew -p backend run

# Backend (desde backend/)
cd backend
./gradlew build
./gradlew test
./gradlew run
```

## 📂 Archivos Importantes

- **`android/app/build.gradle`** - Dependencias y configuración Android
- **`android/app/src/main/java/com/flowboard/`** - Código fuente Android
- **`backend/src/main/kotlin/com/flowboard/`** - Código fuente backend
- **`backend/src/main/resources/application.conf`** - Configuración Ktor

## 🔧 Configuración de API

Para que Android se conecte al backend, verifica la URL en:

**`android/app/src/main/java/com/flowboard/data/remote/api/NetworkConfig.kt`**

```kotlin
// Para emulador Android
const val BASE_URL = "http://10.0.2.2:8080"

// Para dispositivo físico en la misma red
const val BASE_URL = "http://tu.ip.local:8080"

// Para producción
const val BASE_URL = "https://api.flowboard.com"
```

## 🐛 Troubleshooting

### "Gradle sync failed"
```bash
./gradlew clean
./gradlew --stop
# Luego: File → Invalidate Caches → Invalidate and Restart
```

### "Cannot connect to backend"
- ✅ La app funciona offline, no es crítico
- Verifica que el backend esté corriendo: `curl http://localhost:8080/health`
- Revisa la URL en `NetworkConfig.kt`

### "No connected devices"
```bash
# Ver dispositivos conectados
adb devices

# Si no aparecen, reconectar USB o reiniciar ADB
adb kill-server
adb start-server
```

### "Build takes too long"
- Asegúrate de tener al menos 8GB RAM libres
- Activa Gradle daemon: ya está configurado en `gradle.properties`
- Considera aumentar heap: `-Xmx4096m` en `gradle.properties`

## 📚 Siguiente Paso

Una vez que la app funcione, lee la documentación completa:

- **[README.md](README.md)** - Visión general del proyecto
- **[DEVELOPMENT.md](DEVELOPMENT.md)** - Guía de desarrollo detallada
- **[PROJECT_STRUCTURE.md](PROJECT_STRUCTURE.md)** - Estructura del proyecto
- **[CONTRIBUTING.md](CONTRIBUTING.md)** - Cómo contribuir

## 💬 Necesitas Ayuda?

- 📖 Revisa la documentación en `docs/`
- 🐛 Reporta bugs en GitHub Issues
- 💡 Propón features en GitHub Discussions

---

**¡Bienvenido a FlowBoard!** 🚀✨

Organizando el futuro, una tarea a la vez.
