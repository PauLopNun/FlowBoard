#!/bin/bash

# Script rápido para verificar que el proyecto compila correctamente

echo "🔍 Verificando compilación del proyecto Android..."
echo ""

cd "$(dirname "$0")/android"

# Verificar que el SDK existe
if [ ! -d "/home/paulopnun/Android/Sdk" ]; then
    echo "❌ Error: Android SDK no encontrado en /home/paulopnun/Android/Sdk"
    echo "   Por favor, instala el Android SDK o actualiza la ruta en android/local.properties"
    exit 1
fi

echo "✅ Android SDK encontrado"
echo ""

# Verificar archivo local.properties
if [ ! -f "local.properties" ]; then
    echo "❌ Error: android/local.properties no encontrado"
    exit 1
fi

echo "✅ local.properties existe"
echo ""

# Intentar compilar solo las clases (más rápido que assembleDebug)
echo "🔨 Compilando clases de Kotlin..."
echo ""

./gradlew compileDebugKotlin --no-configuration-cache 2>&1 | tee compile.log

if [ ${PIPESTATUS[0]} -eq 0 ]; then
    echo ""
    echo "✅ ¡Compilación de Kotlin exitosa!"
    echo "   No hay errores de símbolos no vinculados (Unbound symbols)"
    echo ""
    echo "📝 Para compilar el APK completo, ejecuta:"
    echo "   ./gradlew assembleDebug"
    exit 0
else
    echo ""
    echo "❌ Error en la compilación"
    echo "   Revisa el archivo compile.log para más detalles"

    # Buscar errores específicos de IR
    if grep -q "Unbound.*symbol" compile.log; then
        echo ""
        echo "⚠️  Se detectaron errores de símbolos no vinculados"
        echo "   Verifica que no haya definiciones duplicadas de clases/enums"
    fi

    exit 1
fi

