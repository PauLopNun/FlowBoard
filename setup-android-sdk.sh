#!/bin/bash
# Script para configurar la ubicación del Android SDK

echo "🔍 Buscando Android SDK..."

# Ubicaciones comunes del SDK en Linux
POSSIBLE_LOCATIONS=(
    "$HOME/Android/Sdk"
    "$HOME/android-sdk"
    "$HOME/.android/sdk"
    "/usr/lib/android-sdk"
    "/opt/android-sdk"
    "$ANDROID_HOME"
    "$ANDROID_SDK_ROOT"
)

SDK_FOUND=""

# Buscar SDK en las ubicaciones comunes
for location in "${POSSIBLE_LOCATIONS[@]}"; do
    if [ -d "$location" ] && [ -d "$location/platform-tools" ]; then
        echo "✅ SDK encontrado en: $location"
        SDK_FOUND="$location"
        break
    fi
done

if [ -z "$SDK_FOUND" ]; then
    echo "❌ No se encontró el Android SDK en las ubicaciones comunes."
    echo ""
    echo "Por favor, instala Android Studio que incluye el SDK, o descarga el SDK manualmente."
    echo ""
    echo "Ubicaciones verificadas:"
    for location in "${POSSIBLE_LOCATIONS[@]}"; do
        echo "  - $location"
    done
    echo ""
    echo "Después de instalar, ejecuta este script nuevamente."
    exit 1
fi

# Crear local.properties en el directorio android
LOCAL_PROPS_FILE="android/local.properties"

echo ""
echo "📝 Configurando $LOCAL_PROPS_FILE..."

cat > "$LOCAL_PROPS_FILE" << EOF
## This file must *NOT* be checked into Version Control Systems,
# as it contains information specific to your local configuration.
#
# Location of the SDK. This is only used by Gradle.
# For customization when using a Version Control System, please read the
# header note.
sdk.dir=$SDK_FOUND
EOF

echo "✅ Archivo $LOCAL_PROPS_FILE configurado correctamente"
echo ""
echo "Contenido del archivo:"
cat "$LOCAL_PROPS_FILE"
echo ""

# Exportar variables de entorno (para la sesión actual)
echo "📋 Variables de entorno:"
echo "export ANDROID_HOME=\"$SDK_FOUND\""
echo "export ANDROID_SDK_ROOT=\"$SDK_FOUND\""
echo "export PATH=\"\$PATH:\$ANDROID_HOME/platform-tools:\$ANDROID_HOME/tools\""
echo ""
echo "💡 Para hacer permanentes estos cambios, añade las líneas anteriores a tu ~/.bashrc o ~/.zshrc"
echo ""
echo "✅ Configuración completada!"
echo ""
echo "Ahora puedes ejecutar:"
echo "  cd android && ./gradlew build"
echo "  o usar Android Studio para abrir el proyecto"

