#!/bin/bash

echo "╔════════════════════════════════════════════════════════════════╗"
echo "║  🚀 Limpieza y Compilación - FlowBoard (Kotlin 2.0 + KSP)     ║"
echo "╚════════════════════════════════════════════════════════════════╝"
echo ""

cd "$(dirname "$0")/android"

# 1. Detener daemon
echo "⏹️  Paso 1/6: Deteniendo Gradle daemon..."
./gradlew --stop 2>/dev/null

# 2. Limpiar proyecto
echo "🧼 Paso 2/6: Limpiando proyecto..."
./gradlew clean --quiet 2>/dev/null || true

# 3. Eliminar caches locales
echo "🗑️  Paso 3/6: Eliminando caches locales..."
rm -rf .gradle
rm -rf app/.gradle
rm -rf app/build
rm -rf build
rm -rf */build

# 4. Eliminar caches de Kotlin
echo "🗑️  Paso 4/6: Eliminando caches de Kotlin..."
rm -rf ~/.gradle/caches/modules-2/files-2.1/org.jetbrains.kotlin
rm -rf ~/.gradle/caches/transforms-*
rm -rf ~/.gradle/caches/build-cache-*
rm -rf ~/.gradle/caches/8.13/kotlin-dsl
rm -rf ~/.gradle/caches/8.13/scripts

# 5. Eliminar archivos de bloqueo
echo "🔓 Paso 5/6: Eliminando archivos de bloqueo..."
find . -name "*.lock" -delete 2>/dev/null
rm -f gradle.lockfile 2>/dev/null

echo ""
echo "✅ Limpieza completada!"
echo ""
echo "═══════════════════════════════════════════════════════════════"
echo "🔨 Paso 6/6: Compilando con Kotlin 2.0.0 + Compose Plugin..."
echo "═══════════════════════════════════════════════════════════════"
echo ""

# 6. Compilar
./gradlew assembleDebug --refresh-dependencies --no-configuration-cache

BUILD_STATUS=$?

echo ""
echo "═══════════════════════════════════════════════════════════════"

if [ $BUILD_STATUS -eq 0 ]; then
    echo "✅ ¡COMPILACIÓN EXITOSA!"
    echo ""
    echo "📦 APK generado en:"
    echo "   $(pwd)/app/build/outputs/apk/debug/app-debug.apk"
    echo ""
    echo "📝 Puedes instalarlo con:"
    echo "   adb install app/build/outputs/apk/debug/app-debug.apk"
    echo ""
    exit 0
else
    echo "❌ Error en la compilación"
    echo ""
    echo "💡 Revisa los mensajes de error arriba."
    echo "   Si el problema persiste:"
    echo "   1. File > Invalidate Caches / Restart (en el IDE)"
    echo "   2. rm -rf ~/.gradle/caches/"
    echo ""
    exit 1
fi

