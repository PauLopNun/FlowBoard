#!/bin/bash

# Script para limpiar y compilar el proyecto Android
# Resuelve problemas de cache y versiones conflictivas

echo "🧹 Limpiando proyecto Android..."

# Detener daemon de Gradle
./gradlew --stop

# Limpiar cache y builds
./gradlew clean --no-daemon

# Limpiar cache de Gradle (opcional, descomentar si es necesario)
# rm -rf ~/.gradle/caches/
# rm -rf ~/.gradle/daemon/

# Limpiar directorios de build
echo "🗑️  Eliminando directorios de compilación..."
rm -rf app/build
rm -rf build
rm -rf .gradle

# Limpiar cache de KSP
echo "🗑️  Limpiando cache de KSP..."
rm -rf app/build/generated/ksp

echo "✅ Limpieza completada"
echo ""
echo "🔨 Compilando proyecto..."

# Compilar proyecto
./gradlew assembleDebug --no-daemon --warning-mode all

echo ""
echo "✅ Compilación completada"

