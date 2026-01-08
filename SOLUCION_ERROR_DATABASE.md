# 🔧 SOLUCIÓN: Error de Conexión PostgreSQL en Render

## 🚨 Error Actual

```
UnknownHostException: dpg-d4isl1muk2gs739l3lh0-a
Failed to initialize pool: The connection attempt failed
```

**Causa:** Render está usando un hostname **interno** que no se puede resolver.

---

## ✅ SOLUCIÓN RÁPIDA (3 Pasos)

### Paso 1: Verificar la Base de Datos en Render

1. Ve a **Render Dashboard**: https://dashboard.render.com
2. Busca tu **PostgreSQL database** (nombre probablemente: `flowboard-db` o similar)
3. **Verifica que esté "Available"** (no "Suspended" o "Creating")

### Paso 2: Obtener la URL Externa Correcta

En el dashboard de tu PostgreSQL database:

1. Ve a la pestaña **"Info"** o **"Connect"**
2. Busca **"External Database URL"** (NO "Internal Database URL")
3. Debería verse así:
   ```
   postgresql://user:pass@dpg-xxxxx-a.oregon-postgres.render.com/dbname
   ```
   **NOTA:** Debe tener `.oregon-postgres.render.com` al final

### Paso 3: Actualizar Variable de Entorno

En tu **Web Service** (backend):

1. Ve a **Environment** tab
2. Busca la variable `DATABASE_URL`
3. **Si existe y tiene hostname interno** (sin `.render.com`):
   - Click en **Edit**
   - Reemplázala con la **External Database URL** del Paso 2
   - Click en **Save Changes**

4. **Si NO existe `DATABASE_URL`:**
   - Click en **"Add Environment Variable"**
   - Key: `DATABASE_URL`
   - Value: La External Database URL del Paso 2
   - Click en **Save**

5. **Redeploy manual:**
   - Ve a la pestaña **"Manual Deploy"**
   - Click en **"Deploy latest commit"**

---

## 🔄 ALTERNATIVA: Vincular Automáticamente

Si no quieres configurar manualmente:

1. En tu **Web Service** → Environment tab
2. Busca la sección **"Add Database"**
3. Click en **"+ Add Database"**
4. Selecciona tu PostgreSQL database
5. Render configurará `DATABASE_URL` automáticamente con la URL correcta

---

## 🛠️ Cambios en el Código (Ya Aplicados)

He modificado `DatabaseFactory.kt` para:

1. ✅ **Convertir hostname interno a externo automáticamente**
   - `dpg-xxxxx-a` → `dpg-xxxxx-a.oregon-postgres.render.com`

2. ✅ **Permitir que la app inicie sin DB** (para debugging)
   - No fallará completamente si no puede conectarse
   - Imprimirá error pero continuará

3. ✅ **Logs mejorados**
   - Muestra exactamente qué hostname está usando
   - Facilita el debugging

---

## 📋 Verificación Paso a Paso

### 1. Verificar que el PostgreSQL esté activo

```bash
# Desde tu terminal local, verifica la conexión
curl https://flowboard-api-phrk.onrender.com
```

Si ves "Service Unavailable" o "Instance failed", el problema es la DB.

### 2. Revisar los logs del deployment

En Render Dashboard → Tu Web Service → Logs

Busca:
```
✅ Database connection configured for Render
📍 Host: dpg-xxxxx-a.oregon-postgres.render.com
🗄️  Database: flowboard
```

Si ves:
```
❌ Database initialization failed
⚠️  Application will start WITHOUT database functionality
```

Entonces la conexión falló, pero la app seguirá iniciando.

### 3. Verificar las variables de entorno

En Render Dashboard → Web Service → Environment

Debe existir:
```
DATABASE_URL = postgresql://user:pass@dpg-xxxxx-a.oregon-postgres.render.com/dbname
```

**IMPORTANTE:** El hostname debe tener `.oregon-postgres.render.com` (o `.render.com`)

---

## 🎯 Formatos de DATABASE_URL

### ❌ INCORRECTO (Hostname Interno)
```
postgresql://user:pass@dpg-d4isl1muk2gs739l3lh0-a/flowboard
```

### ✅ CORRECTO (Hostname Externo)
```
postgresql://user:pass@dpg-d4isl1muk2gs739l3lh0-a.oregon-postgres.render.com/flowboard
```

O también puede ser:
```
postgresql://user:pass@dpg-xxxxx.oregon-postgres.render.com:5432/flowboard
```

---

## 🔍 Debugging

### Opción 1: Ver qué URL está usando la app

En los logs de Render, busca:
```
🔍 Configuring database connection...
DATABASE_URL present: true
📍 Host: [aquí verás el host que está usando]
```

### Opción 2: Probar conexión manual

Desde tu máquina local (requiere instalar `psql`):
```bash
# Reemplaza con tus valores reales
psql "postgresql://user:pass@dpg-xxxxx-a.oregon-postgres.render.com/flowboard?sslmode=require"
```

Si se conecta, el problema es la configuración en Render.

---

## 📝 Checklist de Solución

- [ ] PostgreSQL database está "Available" en Render
- [ ] Obtuve la "External Database URL"
- [ ] La External URL tiene `.oregon-postgres.render.com`
- [ ] Configuré `DATABASE_URL` en Environment con la External URL
- [ ] Guardé los cambios
- [ ] Hice redeploy manual
- [ ] Los logs muestran "✅ Database connection configured"
- [ ] La app inició correctamente

---

## 🚀 Después de la Solución

Una vez que la app inicie correctamente:

1. **Verificar que funciona:**
   ```bash
   curl https://flowboard-api-phrk.onrender.com/api/v1/auth/login \
     -X POST \
     -H "Content-Type: application/json" \
     -d '{"email":"test@flowboard.com","password":"password123"}'
   ```

2. **Hacer commit de los cambios del código:**
   ```bash
   git add backend/src/main/kotlin/com/flowboard/data/database/DatabaseFactory.kt
   git commit -m "Fix: Mejorar conexión a PostgreSQL en Render con hostname externo"
   git push origin main
   ```

3. **Probar la app Android:**
   ```bash
   ./quick-install.sh
   ```

---

## 🆘 Si Nada Funciona

### Plan B: Crear Nueva Base de Datos

1. En Render Dashboard → New → PostgreSQL
2. Nombre: `flowboard-db-v2`
3. Plan: Free
4. Region: Oregon (mismo que el Web Service)
5. Click en "Create Database"
6. Espera a que esté "Available"
7. Copia la "External Database URL"
8. En tu Web Service → Environment → Actualiza `DATABASE_URL`
9. Redeploy

### Plan C: Usar Base de Datos Externa

Considera usar:
- **Supabase** (PostgreSQL gratis): https://supabase.com
- **Neon** (PostgreSQL gratis): https://neon.tech
- **ElephantSQL** (PostgreSQL gratis): https://www.elephantsql.com

Luego configura `DATABASE_URL` con la URL que te den.

---

## 📞 Información Importante

**Hostname actual que está fallando:**
```
dpg-d4isl1muk2gs739l3lh0-a
```

**Debería ser:**
```
dpg-d4isl1muk2gs739l3lh0-a.oregon-postgres.render.com
```

El código ahora convierte esto automáticamente, pero **debes asegurarte de que Render use la URL correcta**.

---

**Última actualización:** 2026-01-08 19:25  
**Estado:** Código actualizado, esperando configuración de Render

