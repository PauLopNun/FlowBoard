#!/bin/bash

# Script de deployment automático para FlowBoard
# Hace commit, push y verifica el deployment

set -e  # Salir si hay algún error

echo "🚀 FlowBoard - Script de Deployment Automático"
echo "=============================================="
echo ""

# Colores para output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Paso 1: Verificar que estamos en la rama correcta
echo "📋 Paso 1: Verificando rama git..."
CURRENT_BRANCH=$(git branch --show-current)
if [ "$CURRENT_BRANCH" != "main" ]; then
    echo -e "${YELLOW}⚠️  Estás en la rama '$CURRENT_BRANCH', no 'main'${NC}"
    read -p "¿Continuar de todas formas? (y/n) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
else
    echo -e "${GREEN}✅ En rama 'main'${NC}"
fi

# Paso 2: Verificar cambios pendientes
echo ""
echo "📋 Paso 2: Verificando cambios..."
if git diff-index --quiet HEAD --; then
    echo -e "${YELLOW}⚠️  No hay cambios para hacer commit${NC}"
    read -p "¿Quieres hacer push de todas formas? (y/n) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 0
    fi
else
    # Mostrar archivos modificados
    echo -e "${GREEN}Archivos modificados:${NC}"
    git status --short
    echo ""

    # Paso 3: Hacer commit
    echo "📋 Paso 3: Haciendo commit..."
    git add .

    # Mensaje de commit por defecto o personalizado
    DEFAULT_MESSAGE="Fix: Resolver errores de compilación del backend y mejorar login en Android"
    read -p "Mensaje de commit (Enter para usar mensaje por defecto): " COMMIT_MESSAGE
    COMMIT_MESSAGE=${COMMIT_MESSAGE:-$DEFAULT_MESSAGE}

    git commit -m "$COMMIT_MESSAGE"
    echo -e "${GREEN}✅ Commit realizado${NC}"
fi

# Paso 4: Push a origin
echo ""
echo "📋 Paso 4: Haciendo push a origin..."
git push -u origin "$CURRENT_BRANCH"
echo -e "${GREEN}✅ Push completado${NC}"

# Paso 5: Esperar un poco para que Render detecte los cambios
echo ""
echo "⏳ Esperando 5 segundos para que Render detecte los cambios..."
sleep 5

# Paso 6: Verificar el backend
echo ""
echo "📋 Paso 5: Verificando el backend..."
echo ""

BASE_URL="https://flowboard-api-phrk.onrender.com"

echo "🔍 Intentando conectar al servidor..."
echo "   (Esto puede tardar 30-60 segundos si el servidor estaba durmiendo)"
echo ""

# Dar tiempo al servidor para despertar
MAX_RETRIES=6
RETRY_COUNT=0
SERVER_READY=false

while [ $RETRY_COUNT -lt $MAX_RETRIES ]; do
    HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/api/v1/auth/login" \
        -X POST \
        -H "Content-Type: application/json" \
        -d '{"email":"test","password":"test"}' \
        --max-time 10)

    if [ "$HTTP_CODE" -eq 400 ] || [ "$HTTP_CODE" -eq 401 ] || [ "$HTTP_CODE" -eq 200 ]; then
        SERVER_READY=true
        break
    fi

    RETRY_COUNT=$((RETRY_COUNT + 1))
    if [ $RETRY_COUNT -lt $MAX_RETRIES ]; then
        echo "   Intento $RETRY_COUNT/$MAX_RETRIES - Esperando 10 segundos..."
        sleep 10
    fi
done

if [ "$SERVER_READY" = true ]; then
    echo -e "${GREEN}✅ Servidor está funcionando (HTTP $HTTP_CODE)${NC}"
else
    echo -e "${RED}❌ No se pudo conectar al servidor después de $MAX_RETRIES intentos${NC}"
    echo "   El deployment puede estar en progreso. Verifica en:"
    echo "   https://dashboard.render.com"
fi

# Paso 7: Resumen
echo ""
echo "=============================================="
echo "📊 RESUMEN DEL DEPLOYMENT"
echo "=============================================="
echo ""
echo "Git:"
echo "  ✅ Commit realizado"
echo "  ✅ Push a $CURRENT_BRANCH completado"
echo ""
echo "Backend:"
echo "  • URL: $BASE_URL"
echo "  • Estado: $([ "$SERVER_READY" = true ] && echo "✅ Online" || echo "⏳ Despertando...")"
echo ""
echo "Próximos pasos:"
echo "  1. Monitorea el deployment en: https://dashboard.render.com"
echo "  2. Cuando esté 'live', ejecuta: ./verify-backend.sh"
echo "  3. Recompila la app Android: ./compile-android.sh"
echo "  4. Prueba el login en la app"
echo ""
echo "📚 Documentación:"
echo "  • GUIA_COMPLETA_SOLUCION.md - Guía paso a paso"
echo "  • SOLUCION_LOGIN.md - Troubleshooting de login"
echo "  • BACKEND_ERRORS_FIXED.md - Errores corregidos"
echo ""
echo -e "${GREEN}🎉 ¡Deployment iniciado con éxito!${NC}"

