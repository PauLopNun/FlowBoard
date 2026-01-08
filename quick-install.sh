#!/bin/bash

# Script para compilar e instalar la app Android rápidamente

echo "📱 FlowBoard - Compilación e Instalación Rápida"
echo "=============================================="
echo ""

# Verificar que existe el SDK de Android
if [ ! -f "android/local.properties" ]; then
    echo "❌ Error: No se encuentra android/local.properties"
    echo "   Ejecuta primero: ./setup-android-sdk.sh"
    exit 1
fi

# Ir al directorio android
cd android

echo "🧹 Limpiando build anterior..."
./gradlew clean > /dev/null 2>&1

echo "🔨 Compilando app (esto puede tardar 1-2 minutos)..."
./gradlew assembleDebug

if [ $? -eq 0 ]; then
    echo "✅ Compilación exitosa"
    echo ""

    # Verificar si hay dispositivo conectado
    DEVICES=$(adb devices | grep -v "List" | grep "device" | wc -l)

    if [ $DEVICES -eq 0 ]; then
        echo "⚠️  No hay dispositivos conectados"
        echo ""
        echo "Para instalar manualmente:"
        echo "  adb install -r app/build/outputs/apk/debug/app-debug.apk"
        echo ""
        echo "APK generado en:"
        echo "  android/app/build/outputs/apk/debug/app-debug.apk"
    else
        echo "📲 Instalando en dispositivo..."
        adb install -r app/build/outputs/apk/debug/app-debug.apk

        if [ $? -eq 0 ]; then
            echo "✅ App instalada correctamente"
            echo ""
            echo "🚀 Abriendo app..."
            adb shell am start -n com.flowboard/.MainActivity
            echo ""
            echo "📊 Para ver logs en tiempo real:"
            echo "  adb logcat | grep -E 'LoginViewModel|AuthApiService'"
        else
            echo "❌ Error al instalar la app"
        fi
    fi
else
    echo "❌ Error en la compilación"
    echo ""
    echo "Intenta compilar manualmente:"
    echo "  cd android"
    echo "  ./gradlew assembleDebug --stacktrace"
    exit 1
fi

cd ..
echo ""
echo "=============================================="
echo "✅ Proceso completado"
echo "=============================================="

