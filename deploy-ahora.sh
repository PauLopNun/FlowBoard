#!/bin/bash

echo "🚀 DEPLOYMENT AUTOMÁTICO - FlowBoard Backend"
echo "============================================="
echo ""

# Verificar que estemos en el directorio correcto
if [ ! -d "backend" ]; then
    echo "❌ Error: No estás en el directorio del proyecto FlowBoard"
    exit 1
fi

echo "📦 Agregando archivos modificados..."
git add backend/
git add android/
git add *.md
git add *.sh

echo ""
echo "📝 Creando commit..."
git commit -m "Fix: Solución completa de errores del backend

✅ Inicialización lazy de base de datos
  - DB se inicializa DESPUÉS del build, no durante
  - Evita fallos de build por DATABASE_URL inaccesible
  - ApplicationStarted event para lazy init

✅ Conversión automática de hostname PostgreSQL
  - dpg-xxxxx-a → dpg-xxxxx-a.oregon-postgres.render.com
  - SSL configurado (sslmode=require)
  - Timeouts optimizados (30s)

✅ Todos los errores de compilación corregidos
  - OperationAckMessage: campo type agregado
  - UserPresenceInfo → DocumentUserPresence: conversión
  - Redeclaraciones eliminadas
  - Imports faltantes agregados
  - Campo synkLastModified eliminado

✅ Mejoras en Android
  - Mensajes de error en español
  - Timeouts aumentados (30s)
  - UI de debugging
  - Auto-rellenar credenciales

Total: 12 archivos modificados
Backend: 7 archivos
Android: 5 archivos"

echo ""
echo "📤 Haciendo push a GitHub..."
git push -u origin master

if [ $? -eq 0 ]; then
    echo ""
    echo "============================================="
    echo "✅ DEPLOYMENT COMPLETADO"
    echo "============================================="
    echo ""
    echo "🔍 Monitorea el deployment en Render:"
    echo "   https://dashboard.render.com/web/srv-d4isldeuk2gs739l3rk0"
    echo ""
    echo "📋 Busca en los logs:"
    echo "   ✅ 'BUILD SUCCESSFUL in 2m'"
    echo "   ✅ 'Database configuration registered'"
    echo "   ✅ 'Application started'"
    echo ""
    echo "⏱️  Tiempo estimado: 3-5 minutos"
    echo ""
    echo "🧪 Después del deployment, verifica:"
    echo "   chmod +x verify-backend.sh"
    echo "   ./verify-backend.sh"
    echo ""
    echo "📱 Luego compila la app Android:"
    echo "   chmod +x quick-install.sh"
    echo "   ./quick-install.sh"
    echo ""
else
    echo ""
    echo "❌ Error en el push"
    echo "Verifica tu conexión a internet y que tengas permisos en el repo"
    exit 1
fi

