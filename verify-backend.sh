#!/bin/bash

# Script para verificar el estado del backend desplegado en Render

echo "🔍 Verificando el backend de FlowBoard en Render..."
echo ""

BASE_URL="https://flowboard-api-phrk.onrender.com"

# Test 1: Verificar que el servidor responde
echo "📡 Test 1: Verificando que el servidor responde..."
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/api/v1/auth/login" -X POST -H "Content-Type: application/json" -d '{"email":"test","password":"test"}')

if [ "$HTTP_CODE" -eq 400 ] || [ "$HTTP_CODE" -eq 401 ] || [ "$HTTP_CODE" -eq 200 ]; then
    echo "✅ Servidor responde correctamente (HTTP $HTTP_CODE)"
else
    echo "❌ Servidor no responde correctamente (HTTP $HTTP_CODE)"
fi

echo ""

# Test 2: Intentar registrar un usuario de prueba
echo "📝 Test 2: Intentando registrar usuario de prueba..."
REGISTER_RESPONSE=$(curl -s -X POST "$BASE_URL/api/v1/auth/register" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test-'$(date +%s)'@flowboard.com",
    "password": "password123",
    "username": "testuser'$(date +%s)'",
    "fullName": "Test User"
  }')

if echo "$REGISTER_RESPONSE" | grep -q "token"; then
    echo "✅ Registro exitoso"
    echo "📄 Respuesta: $REGISTER_RESPONSE"
else
    echo "⚠️  Registro falló (puede ser que el usuario ya exista)"
    echo "📄 Respuesta: $REGISTER_RESPONSE"
fi

echo ""

# Test 3: Intentar hacer login con credenciales de prueba
echo "🔐 Test 3: Intentando login con credenciales de prueba..."
LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@flowboard.com",
    "password": "password123"
  }')

if echo "$LOGIN_RESPONSE" | grep -q "token"; then
    echo "✅ Login exitoso con test@flowboard.com"
    echo "📄 Token recibido"
else
    echo "⚠️  Login falló - puede que el usuario no exista aún"
    echo "📄 Respuesta: $LOGIN_RESPONSE"
fi

echo ""
echo "📊 Resumen:"
echo "   • URL base: $BASE_URL"
echo "   • Endpoint de login: $BASE_URL/api/v1/auth/login"
echo "   • Endpoint de registro: $BASE_URL/api/v1/auth/register"
echo ""
echo "💡 Tip: Si ves errores 404, el servidor puede estar 'durmiendo'."
echo "   Espera 30-60 segundos y vuelve a intentar."

