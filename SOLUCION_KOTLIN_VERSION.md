./gradlew assembleDebug --no-daemon --stacktrace
```

O con más información:

```bash
./gradlew assembleDebug --no-daemon --info --stacktrace
```

## Problemas Comunes y Soluciones

### Error: "SDK location not found"
Asegúrate de que existe `/android/local.properties` con:
```properties
sdk.dir=/ruta/a/android/sdk
```

### Error: Daemon de Gradle no responde
```bash
./gradlew --stop
pkill -f gradle
```

### Cache corrupta
```bash
rm -rf ~/.gradle/caches/
rm -rf ~/.gradle/daemon/
./gradlew clean --no-daemon
```

### Problemas con KSP
```bash
rm -rf app/build/generated/ksp
./gradlew clean kspDebugKotlin --no-daemon
```

## Alternativas si el Problema Persiste

### Opción A: Actualizar a Kotlin 2.1.0

Si las librerías principales requieren Kotlin 2.1, considera actualizar todo el proyecto:

En `/android/build.gradle`:
```groovy
ext {
    kotlin_version = '2.1.0'
    ksp_version = '2.1.0-1.0.29'
    // ...
}

plugins {
    id 'org.jetbrains.kotlin.android' version '2.1.0' apply false
    id 'org.jetbrains.kotlin.plugin.compose' version '2.1.0' apply false
    id 'com.google.devtools.ksp' version '2.1.0-1.0.29' apply false
    // ...
}
```

### Opción B: Reemplazar richeditor-compose

Si `richeditor-compose` sigue causando problemas, considera alternativas:

1. **Usar versión anterior**:
   ```groovy
   implementation 'com.mohamedrejeb.richeditor:richeditor-compose:1.0.0-rc06'
   ```

2. **Implementación propia**: Crear un editor de texto simple con Compose básico

3. **Otras librerías**: Buscar alternativas en Maven Central

## Resumen de Archivos Modificados

- ✅ `/android/build.gradle` - Versiones actualizadas de Hilt y KSP
- ✅ `/android/app/build.gradle` - Configuración de resolución y exclusiones
- ✅ `/android/clean-build.sh` - Script de ayuda para limpieza y compilación

## Próximos Pasos

1. Ejecutar el script de limpieza: `./clean-build.sh`
2. Si falla, revisar los logs completos con `--stacktrace`
3. Verificar que `local.properties` esté configurado correctamente
4. Si es necesario, considerar la actualización a Kotlin 2.1.0

## Notas Importantes

- **No mezclar versiones**: Asegúrate de que todas las dependencias de Kotlin usen la misma versión
- **Cache de Gradle**: A veces es necesario limpiar `~/.gradle/caches/`
- **Android Studio**: Si usas Android Studio, usa "File > Invalidate Caches / Restart"
- **Daemon de Gradle**: Reinicia el daemon después de cambios en versiones: `./gradlew --stop`
# Solución a Problemas de Compatibilidad Kotlin 2.0 / 2.1

## Problema
El error indica un conflicto entre versiones de metadatos de Kotlin:
```
Provided Metadata instance has version 2.1.0, while maximum supported version is 2.0.0
```

Esto ocurre cuando algunas librerías están compiladas con Kotlin 2.1.x pero el proyecto usa Kotlin 2.0.0.

## Cambios Realizados

### 1. Actualización de Versiones en `/android/build.gradle`

Se actualizaron las siguientes versiones:
- **Hilt**: `2.51` → `2.52` (mejor soporte para Kotlin 2.0)
- **KSP**: `2.0.0-1.0.21` → `2.0.0-1.0.24` (versión más reciente compatible)

### 2. Configuración de Resolución de Dependencias en `/android/app/build.gradle`

Se agregó una configuración para forzar el uso de Kotlin 2.0.0:

```groovy
configurations.all {
    resolutionStrategy {
        force 'org.jetbrains.kotlin:kotlin-stdlib:2.0.0'
        force 'org.jetbrains.kotlin:kotlin-stdlib-jdk8:2.0.0'
        force 'org.jetbrains.kotlin:kotlin-stdlib-jdk7:2.0.0'
        force 'org.jetbrains.kotlin:kotlin-reflect:2.0.0'
    }
}
```

### 3. Dependencias Explícitas de Kotlin

Se agregaron dependencias explícitas al inicio del bloque `dependencies`:

```groovy
// Kotlin Standard Library (forzar versión 2.0.0)
implementation 'org.jetbrains.kotlin:kotlin-stdlib:2.0.0'
implementation 'org.jetbrains.kotlin:kotlin-stdlib-jdk8:2.0.0'
```

### 4. Exclusiones en richeditor-compose

La librería `richeditor-compose` estaba compilada con Kotlin 2.1.x, causando conflictos:

```groovy
implementation('com.mohamedrejeb.richeditor:richeditor-compose:1.0.0-rc13') {
    exclude group: 'org.jetbrains.kotlin', module: 'kotlin-stdlib'
    exclude group: 'org.jetbrains.kotlin', module: 'kotlin-stdlib-jdk8'
    exclude group: 'org.jetbrains.kotlin', module: 'kotlin-stdlib-jdk7'
}
```

## Pasos para Compilar

### Opción 1: Usando el Script de Limpieza (Recomendado)

```bash
cd android
chmod +x clean-build.sh
./clean-build.sh
```

### Opción 2: Manual

```bash
cd android

# Detener daemon de Gradle
./gradlew --stop

# Limpiar proyecto
./gradlew clean --no-daemon

# Limpiar directorios de build manualmente
rm -rf app/build
rm -rf build
rm -rf .gradle

# Compilar
./gradlew assembleDebug --no-daemon
```

### Opción 3: Desde la Raíz del Proyecto

```bash
# Desde /home/paulopnun/Escritorio/FlowBoard
cd android && ./gradlew clean assembleDebug --no-daemon
```

## Verificación de Errores

Para ver errores detallados durante la compilación:

```bash

