#!/bin/bash

# Script para hacer commit y push de las correcciones de base de datos

echo "🔧 Aplicando correcciones de conexión a PostgreSQL..."
echo ""

git add backend/src/main/kotlin/com/flowboard/data/database/DatabaseFactory.kt
git add SOLUCION_ERROR_DATABASE.md

git commit -m "Fix: Resolver error de conexión a PostgreSQL en Render

- Convertir hostname interno a externo automáticamente
- Agregar manejo de errores para permitir inicio sin DB
- Mejorar logs de debugging
- Agregar validación de formato de DATABASE_URL
- Documentar solución completa en SOLUCION_ERROR_DATABASE.md

Fixes: UnknownHostException dpg-d4isl1muk2gs739l3lh0-a"

echo ""
echo "✅ Commit realizado"
echo ""
echo "📋 ACCIÓN REQUERIDA EN RENDER:"
echo ""
echo "1. Ve a: https://dashboard.render.com"
echo "2. Abre tu PostgreSQL database"
echo "3. Copia la 'External Database URL'"
echo "4. En tu Web Service → Environment"
echo "5. Actualiza DATABASE_URL con la External URL"
echo "6. Debe incluir: .oregon-postgres.render.com"
echo ""
echo "Luego ejecuta: git push origin main"
echo ""

