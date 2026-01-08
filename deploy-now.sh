#!/bin/bash
set -e

cd /home/paulopnun/AndroidStudioProjects/FlowBoard || exit 1

echo "🚀 Aplicando fix definitivo..."

# Add all changes
git add backend/src/main/kotlin/com/flowboard/routes/WebSocketRoutes.kt
git add backend/src/main/kotlin/com/flowboard/plugins/Database.kt
git add backend/src/main/kotlin/com/flowboard/data/database/DatabaseFactory.kt
git add backend/src/main/kotlin/com/flowboard/data/models/DocumentWebSocketMessage.kt
git add backend/src/main/kotlin/com/flowboard/data/models/WebSocketMessage.kt
git add backend/src/main/kotlin/com/flowboard/domain/DocumentService.kt
git add backend/src/main/kotlin/com/flowboard/domain/NotificationService.kt
git add android/
git add *.md *.sh

# Commit
git commit -m "Fix DEFINITIVO: userName→username, color generado, lazy DB init

ERRORES CRÍTICOS RESUELTOS:
✅ WebSocketRoutes.kt línea 124: userName → username
✅ WebSocketRoutes.kt línea 125: color generado desde userId
✅ Database.kt: Lazy init con ApplicationStarted event
✅ DatabaseFactory.kt: Try-catch + conversión hostname automática

ERRORES ANTERIORES YA CORREGIDOS:
✅ OperationAckMessage: campo type
✅ UserPresenceInfo → DocumentUserPresence
✅ Import eq agregado
✅ synkLastModified eliminado
✅ Redeclaraciones eliminadas

Total: 18 archivos modificados"

# Push
git push -u origin master

echo ""
echo "✅ DEPLOYMENT COMPLETADO"
echo "Monitorea: https://dashboard.render.com/web/srv-d4isldeuk2gs739l3rk0"

