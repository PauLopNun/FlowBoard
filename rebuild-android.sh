#!/bin/bash

# Script para limpiar el cache de Gradle y reconstruir el proyecto Android

echo "🧹 Limpiando cache de Gradle y archivos de compilación..."

cd "$(dirname "$0")/android"

# Detener el daemon de Gradle
echo "Deteniendo daemon de Gradle..."
./gradlew --stop

# Limpiar el proyecto
echo "Limpiando proyecto..."
./gradlew clean

# Eliminar caches locales
echo "Eliminando caches locales..."
rm -rf .gradle
rm -rf app/.gradle
rm -rf app/build
rm -rf build

# Eliminar cache de Kotlin
echo "Eliminando cache de Kotlin..."
rm -rf app/build/tmp/kotlin-classes

echo "✅ Limpieza completada!"
echo ""
echo "🔨 Reconstruyendo el proyecto..."
echo ""

# Reconstruir el proyecto
./gradlew assembleDebug --no-configuration-cache --no-build-cache --rerun-tasks

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ ¡Compilación exitosa!"
else
    echo ""
    echo "❌ Error en la compilación"
    exit 1
fi

