# Configuración del Android SDK

Esta guía te ayudará a resolver el error: `SDK location not found. Define a valid SDK location with an ANDROID_HOME environment variable...`

## 🚀 Solución Rápida (Automática)

Si ya tienes Android Studio instalado, ejecuta el script de configuración automática:

```bash
chmod +x setup-android-sdk.sh
./setup-android-sdk.sh
```

Este script:
- ✅ Busca automáticamente el SDK en ubicaciones comunes
- ✅ Crea/actualiza el archivo `android/local.properties`
- ✅ Te muestra las variables de entorno a configurar

## 📋 Solución Manual

### Paso 1: Instalar Android Studio (si no lo tienes)

#### Linux (Ubuntu/Debian):
```bash
# Opción 1: Snap (recomendado)
sudo snap install android-studio --classic

# Opción 2: Descargar desde el sitio oficial
wget https://redirector.gvt1.com/edgedl/android/studio/ide-zips/2023.1.1.26/android-studio-2023.1.1.26-linux.tar.gz
tar -xzf android-studio-*.tar.gz
sudo mv android-studio /opt/
/opt/android-studio/bin/studio.sh
```

#### Mac:
```bash
# Con Homebrew
brew install --cask android-studio

# O descarga el .dmg desde:
# https://developer.android.com/studio
```

#### Windows:
Descarga el instalador desde: https://developer.android.com/studio

### Paso 2: Localizar el SDK

El SDK se instala automáticamente con Android Studio en:

- **Linux/Mac**: `~/Android/Sdk` o `/Users/<usuario>/Android/Sdk`
- **Windows**: `C:\Users\<usuario>\AppData\Local\Android\Sdk`

Para verificar la ubicación en Android Studio:
1. Abre Android Studio
2. Ve a `Settings/Preferences → Appearance & Behavior → System Settings → Android SDK`
3. Copia la ruta que aparece en "Android SDK Location"

### Paso 3: Configurar `local.properties`

#### Opción A: Creación Automática
Android Studio crea este archivo automáticamente cuando abres el proyecto por primera vez.

#### Opción B: Creación Manual
Crea el archivo `android/local.properties` con el siguiente contenido:

**Linux/Mac:**
```properties
sdk.dir=/home/tu-usuario/Android/Sdk
```

**Windows:**
```properties
sdk.dir=C\:\\Users\\tu-usuario\\AppData\\Local\\Android\\Sdk
```

⚠️ **Importante:** 
- En Windows, usa doble barra invertida (`\\`)
- Reemplaza `tu-usuario` con tu nombre de usuario real
- NO agregues este archivo a Git (ya está en `.gitignore`)

### Paso 4: Configurar Variables de Entorno (Recomendado)

#### Linux/Mac

Edita `~/.bashrc` (o `~/.zshrc` si usas Zsh):

```bash
# Android SDK
export ANDROID_HOME=$HOME/Android/Sdk
export ANDROID_SDK_ROOT=$ANDROID_HOME
export PATH=$PATH:$ANDROID_HOME/emulator
export PATH=$PATH:$ANDROID_HOME/platform-tools
export PATH=$PATH:$ANDROID_HOME/tools
export PATH=$PATH:$ANDROID_HOME/tools/bin
```

Luego recarga el archivo:
```bash
source ~/.bashrc  # o ~/.zshrc
```

#### Windows

1. Abre "Variables de entorno del sistema"
   - Click derecho en "Este equipo" → Propiedades
   - Configuración avanzada del sistema
   - Variables de entorno

2. Agrega las siguientes variables de usuario:
   - `ANDROID_HOME`: `C:\Users\<tu-usuario>\AppData\Local\Android\Sdk`
   - `ANDROID_SDK_ROOT`: `C:\Users\<tu-usuario>\AppData\Local\Android\Sdk`

3. Edita la variable `Path` y agrega:
   - `%ANDROID_HOME%\platform-tools`
   - `%ANDROID_HOME%\tools`
   - `%ANDROID_HOME%\emulator`

### Paso 5: Verificar la Configuración

Abre una nueva terminal y ejecuta:

```bash
# Verificar variables de entorno
echo $ANDROID_HOME  # Linux/Mac
echo %ANDROID_HOME%  # Windows

# Verificar que adb funciona
adb --version

# Debe mostrar algo como:
# Android Debug Bridge version 1.0.41
```

## 🔍 Solución de Problemas

### El SDK no se encuentra en la ubicación esperada

**Buscar manualmente:**

Linux/Mac:
```bash
find ~ -name "platform-tools" -type d 2>/dev/null
```

Windows (PowerShell):
```powershell
Get-ChildItem -Path C:\Users -Filter "platform-tools" -Recurse -ErrorAction SilentlyContinue
```

### El archivo local.properties existe pero sigue fallando

1. **Verifica la ruta:**
   ```bash
   cat android/local.properties
   ```

2. **Asegúrate que la ruta existe:**
   ```bash
   ls -la /ruta/mostrada/en/local.properties
   ```

3. **Verifica permisos:**
   ```bash
   ls -la android/local.properties
   # Debe ser legible
   ```

4. **Reconstruye el proyecto:**
   ```bash
   cd android
   ./gradlew clean
   ./gradlew build
   ```

### Android Studio no instala el SDK automáticamente

1. Abre Android Studio
2. Ve a `Tools → SDK Manager`
3. En la pestaña "SDK Platforms", instala al menos una versión de Android (ej: Android 14)
4. En "SDK Tools", asegúrate de tener instalado:
   - Android SDK Build-Tools
   - Android SDK Platform-Tools
   - Android Emulator (opcional)
   - Android SDK Command-line Tools

### Error: "JAVA_HOME is not set"

El proyecto requiere JDK 17+. Configura JAVA_HOME:

Linux/Mac:
```bash
# Encuentra Java
/usr/libexec/java_home -V  # Mac
whereis java               # Linux

# Agrega a ~/.bashrc o ~/.zshrc
export JAVA_HOME=/ruta/a/jdk-17
export PATH=$PATH:$JAVA_HOME/bin
```

Windows:
1. Instala JDK 17+ desde https://adoptium.net/
2. Configura variable de entorno:
   - `JAVA_HOME`: `C:\Program Files\Java\jdk-17`

## 📝 Verificación Final

Después de configurar todo, ejecuta:

```bash
# Desde la raíz del proyecto
./flow.sh build

# O manualmente
cd android
./gradlew assembleDebug
```

Si todo está correcto, deberías ver:
```
BUILD SUCCESSFUL in Xs
```

## 🆘 Ayuda Adicional

Si sigues teniendo problemas:

1. **Revisa los logs detallados:**
   ```bash
   cd android
   ./gradlew assembleDebug --stacktrace --info
   ```

2. **Limpia completamente:**
   ```bash
   cd android
   ./gradlew clean
   rm -rf .gradle build
   rm -rf app/.gradle app/build
   ```

3. **Abre un issue en GitHub** con:
   - Sistema operativo y versión
   - Versión de Android Studio
   - Contenido de `local.properties`
   - Logs completos del error

## 📚 Referencias

- [Android Studio Download](https://developer.android.com/studio)
- [Android SDK Setup](https://developer.android.com/studio/intro/update#sdk-manager)
- [Environment Variables](https://developer.android.com/studio/command-line/variables)
- [Gradle Build Troubleshooting](https://developer.android.com/studio/build/troubleshoot)

---

**¿Todo funcionando?** ¡Genial! Continúa con la [Guía de Inicio Rápido](README.md#-inicio-rápido) 🚀

