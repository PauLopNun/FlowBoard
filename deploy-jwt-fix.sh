#!/bin/bash
set -e

cd /home/paulopnun/AndroidStudioProjects/FlowBoard || exit 1

echo "🧹 Limpiando archivos compilados antiguos..."
rm -rf backend/bin/
rm -rf backend/build/

echo "📦 Agregando cambios..."
git add backend/src/main/kotlin/com/flowboard/plugins/Security.kt
git add backend/src/main/kotlin/com/flowboard/plugins/Database.kt
git add backend/src/main/kotlin/com/flowboard/data/database/DatabaseFactory.kt
git add backend/src/main/kotlin/com/flowboard/routes/WebSocketRoutes.kt
git add backend/src/main/kotlin/com/flowboard/data/models/DocumentWebSocketMessage.kt
git add backend/src/main/kotlin/com/flowboard/data/models/WebSocketMessage.kt
git add backend/src/main/kotlin/com/flowboard/domain/DocumentService.kt
git add backend/src/main/kotlin/com/flowboard/domain/NotificationService.kt

echo "📝 Commit..."
git commit -m "Fix CRÍTICO: JWT config name + lazy DB init

✅ ERRORES RESUELTOS:
1. JWT Authentication: 'auth-jwt' → 'jwt' (coincide con rutas)
2. Lazy DB init: ApplicationStarted event (no durante startup)
3. DB errors no bloquean inicio de app
4. Hostname conversion automática

✅ ERRORES ANTERIORES:
- WebSocketRoutes: username/color corregidos
- DocumentWebSocketMessage: type fields
- Import eq agregado
- synkLastModified eliminado

BUILD SUCCESSFUL - ahora app iniciará correctamente"

echo "📤 Push..."
git push -u origin master

echo ""
echo "✅ DEPLOYMENT INICIADO"
echo "Tiempo estimado: 3 minutos"
echo "Monitorea: https://dashboard.render.com/web/srv-d4isldeuk2gs739l3rk0"

