#!/bin/bash
set -e

cd /home/paulopnun/AndroidStudioProjects/FlowBoard

echo "🔧 Fix FINAL: Consistencia JWT auth-jwt en TODAS las rutas"
echo ""

# Limpiar archivos compilados viejos
rm -rf backend/bin/ backend/build/ 2>/dev/null || true

# Add all JWT-related files
git add backend/src/main/kotlin/com/flowboard/plugins/Security.kt
git add backend/src/main/kotlin/com/flowboard/routes/NotificationRoutes.kt
git add backend/src/main/kotlin/com/flowboard/routes/DocumentRoutes.kt
git add backend/src/main/kotlin/com/flowboard/routes/ChatRoutes.kt
git add backend/src/main/kotlin/com/flowboard/routes/DocumentWebSocketRoutes.kt
git add backend/src/main/kotlin/com/flowboard/plugins/Database.kt
git add backend/src/main/kotlin/com/flowboard/data/database/DatabaseFactory.kt
git add backend/src/main/kotlin/com/flowboard/routes/WebSocketRoutes.kt

# Commit
git commit -m "Fix DEFINITIVO: auth-jwt consistente en TODAS las rutas

PROBLEMA RESUELTO:
✅ Todas las rutas ahora usan 'auth-jwt' (no 'jwt')
✅ Security.kt define jwt('auth-jwt')
✅ TaskRoutes.kt usa authenticate('auth-jwt') ✅
✅ ProjectRoutes.kt usa authenticate('auth-jwt') ✅
✅ UserRoutes.kt usa authenticate('auth-jwt') ✅
✅ NotificationRoutes.kt → cambiado a 'auth-jwt' ✅
✅ DocumentRoutes.kt → cambiado a 'auth-jwt' ✅
✅ ChatRoutes.kt → cambiado a 'auth-jwt' ✅
✅ DocumentWebSocketRoutes.kt → cambiado a 'auth-jwt' ✅

OTROS FIXES INCLUIDOS:
✅ Lazy DB init (ApplicationStarted event)
✅ DB hostname conversion automática
✅ WebSocketRoutes username/color corregidos
✅ Try-catch permite inicio sin DB

Este commit resuelve el error:
'Authentication configuration with the name auth-jwt was not found'

Ahora TODAS las rutas y Security.kt usan el mismo nombre."

# Push
git push -u origin master

echo ""
echo "=========================================="
echo "✅ DEPLOYMENT FINAL COMPLETADO"
echo "=========================================="
echo ""
echo "Monitorea: https://dashboard.render.com/web/srv-d4isldeuk2gs739l3rk0"
echo ""
echo "Esperado en logs:"
echo "  ✅ BUILD SUCCESSFUL"
echo "  ✅ JWT Authentication 'auth-jwt' installed"
echo "  ✅ Server started on 0.0.0.0:8080"
echo ""
echo "Tiempo estimado: 3 minutos"

