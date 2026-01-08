#!/bin/bash

echo "🚀 Deployment Rápido - FlowBoard Backend Fix"
echo "=========================================="
echo ""

# Agregar cambios
git add backend/src/main/kotlin/com/flowboard/data/database/DatabaseFactory.kt
git add backend/src/main/kotlin/com/flowboard/data/models/DocumentWebSocketMessage.kt
git add backend/src/main/kotlin/com/flowboard/routes/WebSocketRoutes.kt
git add backend/src/main/kotlin/com/flowboard/data/models/WebSocketMessage.kt
git add backend/src/main/kotlin/com/flowboard/domain/DocumentService.kt
git add backend/src/main/kotlin/com/flowboard/domain/NotificationService.kt
git add *.md
git add *.sh

echo "📝 Haciendo commit..."
git commit -m "Fix: Resolver todos los errores del backend para Render

✅ Errores de compilación corregidos:
- OperationAckMessage: campo type agregado
- UserPresenceInfo → DocumentUserPresence: conversión implementada
- Redeclaraciones eliminadas
- Imports faltantes agregados

✅ Conexión a PostgreSQL corregida:
- Conversión automática de hostname interno a externo
- dpg-xxxxx-a → dpg-xxxxx-a.oregon-postgres.render.com
- Manejo de errores mejorado (permite inicio sin DB)
- SSL configurado (sslmode=require)
- Timeouts optimizados (30s)

✅ Logs de debugging mejorados
✅ Documentación completa agregada"

echo ""
echo "✅ Commit completado"
echo ""
echo "📤 Haciendo push a GitHub..."
git push origin main

echo ""
echo "=========================================="
echo "✅ Push completado"
echo ""
echo "📊 Render detectará los cambios automáticamente"
echo "⏱️  El deployment tomará 2-3 minutos"
echo ""
echo "🔍 Monitorea el deployment en:"
echo "   https://dashboard.render.com/web/srv-d4isldeuk2gs739l3rk0"
echo ""
echo "📋 En los logs, busca:"
echo "   ✅ Database connection configured for Render"
echo "   📍 Host: dpg-xxxxx.oregon-postgres.render.com"
echo "   ✅ Database initialized successfully"
echo ""
echo "🧪 Después del deployment, verifica:"
echo "   ./verify-backend.sh"
echo ""

