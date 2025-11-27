#!/bin/bash

# Script para obtener SHA-1 y SHA-256 fingerprints para Google Sign-In
# Estos fingerprints son necesarios para configurar Google Cloud Console

echo "=========================================="
echo "   Obteniendo SHA-1 y SHA-256 Fingerprints"
echo "=========================================="
echo ""

# Verificar si keytool está disponible
if ! command -v keytool &> /dev/null; then
    echo "❌ ERROR: keytool no está instalado o no está en el PATH"
    echo "keytool viene con el JDK. Asegúrate de tener el JDK instalado."
    exit 1
fi

echo "🔍 Buscando keystore de debug..."
echo ""

# Ruta del keystore de debug
KEYSTORE_PATH="$HOME/.android/debug.keystore"

# Verificar si existe el keystore
if [ ! -f "$KEYSTORE_PATH" ]; then
    echo "❌ ERROR: No se encontró el keystore de debug en:"
    echo "   $KEYSTORE_PATH"
    echo ""
    echo "💡 El keystore se crea automáticamente al compilar la app."
    echo "   Intenta compilar la app primero: ./gradlew assembleDebug"
    exit 1
fi

echo "✅ Keystore encontrado en:"
echo "   $KEYSTORE_PATH"
echo ""
echo "=========================================="
echo "   DEBUG KEYSTORE (Para desarrollo)"
echo "=========================================="
echo ""

# Obtener fingerprints del keystore de debug
# Contraseña por defecto: android
keytool -list -v -keystore "$KEYSTORE_PATH" -alias androiddebugkey -storepass android -keypass android | grep -E "SHA1:|SHA256:"

echo ""
echo "=========================================="
echo "   INSTRUCCIONES"
echo "=========================================="
echo ""
echo "1. Copia los valores SHA1 y SHA256 de arriba"
echo "2. Ve a Google Cloud Console:"
echo "   https://console.cloud.google.com/apis/credentials"
echo ""
echo "3. Selecciona tu proyecto o crea uno nuevo"
echo ""
echo "4. Crea credenciales OAuth 2.0:"
echo "   - Tipo: Android"
echo "   - Nombre del paquete: com.flowboard"
echo "   - SHA-1: [pega el valor SHA1 de arriba]"
echo ""
echo "5. IMPORTANTE: También crea credenciales Web:"
echo "   - Tipo: ID de cliente web"
echo "   - Nombre: FlowBoard Web Client"
echo "   - Copia el Client ID y actualiza GoogleAuthManager.kt"
echo ""
echo "6. Habilita la API de Google Sign-In:"
echo "   https://console.cloud.google.com/apis/library/identitytoolkit.googleapis.com"
echo ""
echo "=========================================="
echo ""
echo "📝 NOTA: Para RELEASE (producción), necesitarás repetir esto"
echo "         con el keystore de release cuando lo tengas."
echo ""
