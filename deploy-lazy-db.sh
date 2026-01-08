#!/bin/bash

echo "🚀 Deployment Final - Fix de Base de Datos Lazy"
echo "=============================================="
echo ""

# Add all changes
git add backend/src/main/kotlin/com/flowboard/data/database/DatabaseFactory.kt
git add backend/src/main/kotlin/com/flowboard/plugins/Database.kt
git add backend/src/main/kotlin/com/flowboard/data/models/DocumentWebSocketMessage.kt
git add backend/src/main/kotlin/com/flowboard/routes/WebSocketRoutes.kt
git add backend/src/main/kotlin/com/flowboard/data/models/WebSocketMessage.kt
git add backend/src/main/kotlin/com/flowboard/domain/DocumentService.kt
git add backend/src/main/kotlin/com/flowboard/domain/NotificationService.kt
git add *.md *.sh

echo "📝 Haciendo commit..."
git commit -m "Fix: Inicialización lazy de DB para evitar fallos en Docker build

✅ Cambios principales:
- DatabaseFactory.init() ahora es lazy (no falla durante build)
- Se inicializa en ApplicationStarted event (después del build)
- Conversión automática hostname interno → externo (dpg-xxx → dpg-xxx.oregon-postgres.render.com)
- Manejo de errores mejorado (permite inicio sin DB)
- Logs de debugging mejorados

✅ Errores de compilación corregidos:
- OperationAckMessage: campo type agregado
- UserPresenceInfo → DocumentUserPresence: conversión implementada
- Redeclaraciones eliminadas
- Imports faltantes agregados

Esto permite que el build de Docker se complete incluso si DATABASE_URL
no es accesible durante la compilación. La conexión a DB se intentará
cuando la aplicación se inicie, no durante el build."

echo ""
echo "✅ Commit completado"
echo ""
echo "📤 Haciendo push..."
git push origin main

echo ""
echo "=============================================="
echo "✅ Deployment iniciado"
echo ""
echo "📊 Monitorea en:"
echo "   https://dashboard.render.com/web/srv-d4isldeuk2gs739l3rk0"
echo ""
echo "🔍 Busca en los logs:"
echo "   ✅ 'BUILD SUCCESSFUL'"
echo "   ✅ 'Database configuration registered (lazy initialization)'"
echo "   ✅ 'Application started - initializing database...'"
echo ""
echo "⏱️  Tiempo estimado: 3-5 minutos"
echo ""

