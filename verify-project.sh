#!/bin/bash

# ═══════════════════════════════════════════════════════════════════════════
#  FlowBoard - Script de Verificación Pre-Presentación
#  Ejecuta este script antes de tu presentación para verificar que todo esté listo
# ═══════════════════════════════════════════════════════════════════════════

echo "╔════════════════════════════════════════════════════════════════════╗"
echo "║                                                                    ║"
echo "║         FlowBoard - Verificación Pre-Presentación                  ║"
echo "║                                                                    ║"
echo "╚════════════════════════════════════════════════════════════════════╝"
echo ""

# Colores para output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Contador de verificaciones
PASSED=0
FAILED=0
WARNINGS=0

# Función para imprimir resultados
print_result() {
    if [ $1 -eq 0 ]; then
        echo -e "${GREEN}✓ PASS${NC} - $2"
        ((PASSED++))
    elif [ $1 -eq 1 ]; then
        echo -e "${RED}✗ FAIL${NC} - $2"
        ((FAILED++))
    else
        echo -e "${YELLOW}⚠ WARN${NC} - $2"
        ((WARNINGS++))
    fi
}

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  1. VERIFICANDO ESTRUCTURA DEL PROYECTO"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# Verificar directorios principales
if [ -d "android" ]; then
    print_result 0 "Directorio android/ existe"
else
    print_result 1 "Directorio android/ NO encontrado"
fi

if [ -d "backend" ]; then
    print_result 0 "Directorio backend/ existe"
else
    print_result 2 "Directorio backend/ NO encontrado (opcional)"
fi

if [ -d "docs" ]; then
    print_result 0 "Directorio docs/ existe"
else
    print_result 2 "Directorio docs/ NO encontrado"
fi

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  2. VERIFICANDO DOCUMENTACIÓN"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# Verificar archivos de documentación
docs=(
    "README.md"
    "PROYECTO_FINALIZADO.md"
    "QUICK_GUIDE.md"
    "CHECKLIST_PRESENTACION.md"
    "COLLABORATIVE_EDITOR_IMPLEMENTATION.md"
    "INDICE_DOCUMENTACION.md"
    "SETUP_ANDROID_SDK.md"
)

for doc in "${docs[@]}"; do
    if [ -f "$doc" ]; then
        print_result 0 "Documentación: $doc"
    else
        print_result 1 "FALTA: $doc"
    fi
done

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  3. VERIFICANDO ARCHIVOS CLAVE DEL CÓDIGO"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# Verificar archivos clave
key_files=(
    "android/app/src/main/java/com/flowboard/FlowBoardApp.kt"
    "android/app/src/main/java/com/flowboard/MainActivity.kt"
    "android/app/src/main/java/com/flowboard/presentation/ui/screens/auth/LoginScreen.kt"
    "android/app/src/main/java/com/flowboard/presentation/ui/screens/auth/RegisterScreen.kt"
    "android/app/src/main/java/com/flowboard/presentation/ui/screens/tasks/CreateTaskScreen.kt"
    "android/app/src/main/java/com/flowboard/presentation/ui/screens/tasks/TaskDetailScreen.kt"
    "android/app/src/main/java/com/flowboard/presentation/ui/screens/documents/CollaborativeDocumentScreen.kt"
    "android/app/src/main/java/com/flowboard/presentation/ui/components/CollaborativeRichTextEditor.kt"
    "android/app/src/main/java/com/flowboard/presentation/viewmodel/DocumentViewModel.kt"
)

for file in "${key_files[@]}"; do
    if [ -f "$file" ]; then
        print_result 0 "$(basename "$file")"
    else
        print_result 1 "FALTA: $(basename "$file")"
    fi
done

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  4. VERIFICANDO CONFIGURACIÓN DE GRADLE"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

if [ -f "android/build.gradle" ]; then
    print_result 0 "build.gradle encontrado"
else
    print_result 1 "build.gradle NO encontrado"
fi

if [ -f "android/app/build.gradle" ]; then
    print_result 0 "app/build.gradle encontrado"
else
    print_result 1 "app/build.gradle NO encontrado"
fi

if [ -f "android/local.properties" ]; then
    print_result 0 "local.properties existe"
else
    print_result 2 "local.properties NO existe (ejecutar setup-android-sdk.sh)"
fi

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  5. VERIFICANDO HERRAMIENTAS NECESARIAS"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# Verificar Java
if command -v java &> /dev/null; then
    JAVA_VERSION=$(java -version 2>&1 | head -n 1)
    print_result 0 "Java instalado: $JAVA_VERSION"
else
    print_result 1 "Java NO instalado"
fi

# Verificar Android SDK
if [ ! -z "$ANDROID_HOME" ]; then
    print_result 0 "ANDROID_HOME configurado: $ANDROID_HOME"
else
    print_result 2 "ANDROID_HOME NO configurado"
fi

# Verificar adb
if command -v adb &> /dev/null; then
    ADB_VERSION=$(adb version | head -n 1)
    print_result 0 "ADB disponible: $ADB_VERSION"
else
    print_result 2 "ADB NO disponible"
fi

# Verificar git
if command -v git &> /dev/null; then
    GIT_VERSION=$(git --version)
    print_result 0 "Git instalado: $GIT_VERSION"
else
    print_result 2 "Git NO instalado"
fi

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  6. VERIFICANDO COMPILACIÓN"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

echo "Intentando compilar el proyecto..."
echo "(Esto puede tardar un momento...)"
echo ""

cd android

if [ -f "gradlew" ]; then
    chmod +x gradlew

    # Intenta compilar (solo mostrar si hay error)
    if ./gradlew assembleDebug --quiet > /tmp/gradle-output.log 2>&1; then
        print_result 0 "Compilación exitosa"
    else
        print_result 1 "Error en compilación (ver /tmp/gradle-output.log)"
        echo ""
        echo "Últimas líneas del error:"
        tail -n 20 /tmp/gradle-output.log
    fi
else
    print_result 1 "gradlew NO encontrado"
fi

cd ..

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  7. RESUMEN DE VERIFICACIÓN"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

TOTAL=$((PASSED + FAILED + WARNINGS))

echo -e "${GREEN}✓ Verificaciones pasadas:${NC} $PASSED/$TOTAL"
echo -e "${RED}✗ Verificaciones fallidas:${NC} $FAILED/$TOTAL"
echo -e "${YELLOW}⚠ Advertencias:${NC} $WARNINGS/$TOTAL"
echo ""

if [ $FAILED -eq 0 ]; then
    echo "╔════════════════════════════════════════════════════════════════════╗"
    echo "║                                                                    ║"
    echo -e "║  ${GREEN}✓ ¡PROYECTO LISTO PARA PRESENTACIÓN!${NC}                           ║"
    echo "║                                                                    ║"
    echo "╚════════════════════════════════════════════════════════════════════╝"
    echo ""
    echo "Siguiente paso:"
    echo "  1. Lee CHECKLIST_PRESENTACION.md"
    echo "  2. Practica el flujo de demo"
    echo "  3. Prepara respuestas a preguntas"
    echo ""
    echo "¡Mucha suerte! 🚀"
else
    echo "╔════════════════════════════════════════════════════════════════════╗"
    echo "║                                                                    ║"
    echo -e "║  ${YELLOW}⚠ HAY PROBLEMAS QUE RESOLVER${NC}                                   ║"
    echo "║                                                                    ║"
    echo "╚════════════════════════════════════════════════════════════════════╝"
    echo ""
    echo "Acciones recomendadas:"
    echo "  1. Revisar los errores mostrados arriba"
    echo "  2. Ejecutar ./setup-android-sdk.sh si hay problemas con el SDK"
    echo "  3. Verificar que todos los archivos estén presentes"
    echo "  4. Ejecutar ./flow.sh clean && ./flow.sh build"
    echo ""
    echo "Consulta SETUP_ANDROID_SDK.md para ayuda."
fi

echo ""
echo "═══════════════════════════════════════════════════════════════════════════"
echo ""

