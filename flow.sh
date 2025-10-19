#!/bin/bash
# FlowBoard - Script de utilidades para desarrollo
# Uso: ./flow.sh [comando]

# Colores para output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m' # Sin color

show_help() {
    echo ""
    echo "================================"
    echo "   FlowBoard - Comandos Rápidos"
    echo "================================"
    echo ""
    echo "Uso: ./flow.sh [comando]"
    echo ""
    echo "Comandos disponibles:"
    echo ""
    echo "  build      - Compilar app Android (debug)"
    echo "  clean      - Limpiar builds"
    echo "  test       - Ejecutar tests de Android"
    echo "  run        - Instalar y ejecutar en dispositivo"
    echo "  backend    - Iniciar backend Ktor"
    echo "  install    - Instalar APK en dispositivo"
    echo "  help       - Mostrar esta ayuda"
    echo ""
    echo "Ejemplos:"
    echo "  ./flow.sh build"
    echo "  ./flow.sh run"
    echo "  ./flow.sh backend"
    echo ""
}

build_android() {
    echo -e "${BLUE}[FlowBoard]${NC} Compilando Android app (Debug)..."
    ./gradlew -p android assembleDebug
}

clean_all() {
    echo -e "${BLUE}[FlowBoard]${NC} Limpiando builds..."
    ./gradlew clean
    ./gradlew -p android clean
    if [ -d "backend" ]; then
        ./gradlew -p backend clean
    fi
    echo -e "${GREEN}[FlowBoard]${NC} Limpieza completada."
}

test_android() {
    echo -e "${BLUE}[FlowBoard]${NC} Ejecutando tests de Android..."
    ./gradlew -p android test
}

run_app() {
    echo -e "${BLUE}[FlowBoard]${NC} Instalando y ejecutando app..."
    ./gradlew -p android installDebug
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}[FlowBoard]${NC} App instalada correctamente."
        echo -e "${BLUE}[FlowBoard]${NC} Inicia la app manualmente en tu dispositivo."
    else
        echo -e "${RED}[ERROR]${NC} No se pudo instalar la app. Verifica que haya un dispositivo conectado."
    fi
}

run_backend() {
    echo -e "${BLUE}[FlowBoard]${NC} Iniciando backend Ktor..."
    echo -e "${BLUE}[INFO]${NC} Asegúrate de tener PostgreSQL corriendo."
    cd backend
    ./gradlew run
    cd ..
}

install_apk() {
    echo -e "${BLUE}[FlowBoard]${NC} Instalando APK en dispositivo..."
    ./gradlew -p android installDebug
}

# Procesamiento de comandos
case "$1" in
    "build")
        build_android
        ;;
    "clean")
        clean_all
        ;;
    "test")
        test_android
        ;;
    "run")
        run_app
        ;;
    "backend")
        run_backend
        ;;
    "install")
        install_apk
        ;;
    "help"|"")
        show_help
        ;;
    *)
        echo -e "${RED}[ERROR]${NC} Comando desconocido: $1"
        echo "Usa './flow.sh help' para ver los comandos disponibles."
        exit 1
        ;;
esac
