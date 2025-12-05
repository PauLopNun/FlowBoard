#!/bin/bash

# Script rápido de compilación para FlowBoard Android
# Ejecutar desde el directorio raíz del proyecto

echo "🚀 FlowBoard - Compilación Android"
echo "=================================="
echo ""

# Verificar que estamos en el directorio correcto
if [ ! -d "android" ]; then
    echo "❌ Error: No se encuentra el directorio 'android'"
    echo "   Ejecuta este script desde el directorio raíz de FlowBoard"
    exit 1
fi

cd android

echo "🛑 Deteniendo Gradle daemon..."
./gradlew --stop 2>/dev/null

echo ""
echo "🧹 Limpiando proyecto..."
./gradlew clean --no-daemon

echo ""
echo "🔨 Compilando APK debug..."
./gradlew assembleDebug --no-daemon --warning-mode all

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ ¡Compilación exitosa!"
    echo ""
    echo "📱 APK generado en:"
    echo "   android/app/build/outputs/apk/debug/app-debug.apk"
    echo ""
else
    echo ""
    echo "❌ Error en la compilación"
    echo ""
    echo "💡 Sugerencias:"
    echo "   1. Verifica que android/local.properties esté configurado"
    echo "   2. Ejecuta: cd android && ./gradlew assembleDebug --stacktrace"
    echo "   3. Revisa SOLUCION_KOTLIN_VERSION.md para más información"
    echo ""
    exit 1
fi

