# 🛠️ Guía de Desarrollo - FlowBoard

## 📂 Estructura del Proyecto (Monorepo)

Este proyecto utiliza una arquitectura **monorepo** que separa el frontend Android y el backend Ktor:

```
FlowBoard/
├── android/          # Aplicación Android (Kotlin + Jetpack Compose)
├── backend/          # API Backend (Ktor + PostgreSQL)
├── docs/             # Documentación del proyecto
└── README.md         # Documentación principal
```

## 🎯 Cómo Abrir el Proyecto

### Opción 1: Abrir Subproyectos Independientemente (✅ Recomendado)

Esta es la forma **más profesional y estándar** para trabajar con monorepos:

#### Para el Frontend Android:
```
Android Studio → File → Open → .../FlowBoard/android
```

#### Para el Backend:
```
IntelliJ IDEA → File → Open → .../FlowBoard/backend
```

**Ventajas:**
- ✅ Mejor rendimiento (solo carga el proyecto necesario)
- ✅ Configuraciones independientes
- ✅ No hay conflictos entre módulos
- ✅ Es el estándar de la industria (React Native, Flutter, etc.)

### Opción 2: Abrir desde la Raíz (Composite Build)

También puedes abrir la carpeta raíz `FlowBoard` en Android Studio, que incluirá el módulo Android mediante **Gradle Composite Build**.

```
Android Studio → File → Open → .../FlowBoard
```

**Nota:** El backend NO se incluirá automáticamente (debe abrirse por separado en IntelliJ).

## 🏗️ Compilar y Ejecutar

### Desde la Raíz (si abriste FlowBoard/)

```bash
# Windows
gradlew.bat -p android assembleDebug

# Linux/Mac
./gradlew -p android assembleDebug
```

### Desde el Módulo Android (si abriste android/)

```bash
# Windows
gradlew.bat assembleDebug

# Linux/Mac
./gradlew assembleDebug
```

### Ejecutar en Emulador/Dispositivo

```bash
# Desde raíz
gradlew.bat -p android installDebug

# Desde android/
gradlew.bat installDebug
```

O simplemente usa el botón **Run ▶️** en Android Studio.

## 🧪 Testing

### Tests de Android
```bash
# Desde raíz
gradlew.bat -p android test
gradlew.bat -p android connectedAndroidTest

# Desde android/
gradlew.bat test
gradlew.bat connectedAndroidTest
```

### Tests de Backend
```bash
# Desde raíz
gradlew.bat -p backend test

# Desde backend/
gradlew.bat test
```

## 📦 Builds de Producción

### APK de Release (Android)
```bash
gradlew.bat -p android assembleRelease
```

El APK se generará en:
```
android/app/build/outputs/apk/release/app-release.apk
```

### Bundle de Android (AAB)
```bash
gradlew.bat -p android bundleRelease
```

## 🔧 Configuración del Entorno

### Android
- **JDK**: 17 o superior
- **Android Studio**: Hedgehog (2023.1.1) o superior
- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 34 (Android 14)
- **Gradle**: 8.2+
- **Kotlin**: 1.9.22

### Backend
- **JDK**: 17 o superior
- **PostgreSQL**: 13 o superior
- **Ktor**: 2.3.7

## 🚀 Flujo de Trabajo Recomendado

1. **Configurar Backend** (una vez):
   ```bash
   cd backend
   # Configurar PostgreSQL y variables de entorno
   ./gradlew run
   ```

2. **Desarrollar Android**:
   - Abrir `android/` en Android Studio
   - Ejecutar con Run ▶️
   - La app funciona offline por defecto (Room)

3. **Sincronización**:
   - Backend corriendo → la app sincroniza automáticamente
   - Backend apagado → la app usa solo datos locales

## 📱 Configuración de API

La URL del backend se configura en:
```
android/app/src/main/java/com/flowboard/data/remote/api/NetworkConfig.kt
```

**Desarrollo local:**
```kotlin
const val BASE_URL = "http://10.0.2.2:8080"  // Emulador
const val BASE_URL = "http://localhost:8080" // Dispositivo físico
```

**Producción:**
```kotlin
const val BASE_URL = "https://api.flowboard.com"
```

## 🐛 Troubleshooting

### "Cannot find settings.gradle" al abrir FlowBoard/
✅ **Solución**: Abre directamente `FlowBoard/android/` en Android Studio.

### "Gradle sync failed"
```bash
# Limpiar caché de Gradle
gradlew.bat clean
gradlew.bat --stop

# Reiniciar Android Studio
```

### "Backend connection failed"
- Verifica que el backend esté corriendo en `http://localhost:8080`
- Revisa la URL en `NetworkConfig.kt`
- La app funciona offline, no es crítico

## 📚 Recursos Adicionales

- [README.md](README.md) - Documentación principal del proyecto
- [docs/](docs/) - Documentación técnica detallada
- [CONTRIBUTING.md](CONTRIBUTING.md) - Guía de contribución

## 🤝 Convenciones de Desarrollo

- **Branches**: `feature/nombre`, `bugfix/nombre`, `release/version`
- **Commits**: Conventional Commits (`feat:`, `fix:`, `docs:`, etc.)
- **Code Style**: Kotlin Coding Conventions + ktlint
- **Architecture**: Clean Architecture + MVVM

---

**¿Preguntas?** Consulta el [README.md](README.md) principal o abre un issue.
