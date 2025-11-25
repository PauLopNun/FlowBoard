# Guía de Despliegue de FlowBoard Backend en Render

Esta guía te ayudará a desplegar tu backend de FlowBoard en Render para que esté disponible 24/7.

## Requisitos Previos

1. Cuenta en GitHub (para conectar tu repositorio)
2. Cuenta en Render (gratis): https://render.com

## Paso 1: Preparar el Repositorio

Asegúrate de que todos los cambios estén en tu repositorio de GitHub:

```bash
git add .
git commit -m "Add Render deployment configuration"
git push origin master
```

## Paso 2: Crear el Servicio en Render

### Opción A: Despliegue Automático con render.yaml (Recomendado)

1. Ve a https://render.com y crea una cuenta o inicia sesión
2. En el dashboard, haz clic en **"New +"** y selecciona **"Blueprint"**
3. Conecta tu repositorio de GitHub
4. Render detectará automáticamente el archivo `render.yaml` y creará:
   - Un servicio web para el backend
   - Una base de datos PostgreSQL gratuita
5. Haz clic en **"Apply"** para crear los servicios

### Opción B: Despliegue Manual

Si prefieres hacerlo manualmente:

1. **Crear la Base de Datos:**
   - Haz clic en **"New +"** → **"PostgreSQL"**
   - Nombre: `flowboard-db`
   - Plan: **Free**
   - Región: Elige la más cercana (ej: Oregon)
   - Haz clic en **"Create Database"**

2. **Crear el Servicio Web:**
   - Haz clic en **"New +"** → **"Web Service"**
   - Conecta tu repositorio de GitHub
   - Nombre: `flowboard-api`
   - Environment: **Docker**
   - Dockerfile Path: `./backend/Dockerfile`
   - Docker Context: `./backend`
   - Plan: **Free**
   - Región: Elige la misma que la base de datos

3. **Configurar Variables de Entorno:**
   En la sección "Environment", agrega las siguientes variables:
   ```
   DATABASE_URL = [Copia desde tu base de datos PostgreSQL]
   PORT = 8080
   KTOR_ENV = production
   JWT_SECRET = [Genera un secreto aleatorio seguro]
   JWT_ISSUER = flowboard-api
   JWT_AUDIENCE = flowboard-app
   JWT_REALM = FlowBoard Access
   ```

4. Haz clic en **"Create Web Service"**

## Paso 3: Verificar el Despliegue

1. Render comenzará a construir tu aplicación (esto puede tomar 5-10 minutos la primera vez)
2. Una vez completado, verás un mensaje **"Live"** en verde
3. Tu backend estará disponible en: `https://flowboard-api.onrender.com`

### Probar el Backend

Prueba que el backend esté funcionando:

```bash
curl https://flowboard-api.onrender.com/api/v1/auth/login
```

## Paso 4: Actualizar la URL en el Android App

La URL ya está configurada en `ApiConfig.kt`, pero necesitas actualizarla con tu URL real de Render:

1. Abre: `android/app/src/main/java/com/flowboard/data/remote/ApiConfig.kt`
2. Actualiza la línea 12 con tu URL de Render:
   ```kotlin
   private const val PRODUCTION_BASE_URL = "https://flowboard-api.onrender.com"
   ```
3. La app ahora usará:
   - En **modo DEBUG**: `http://10.0.2.2:8080` (localhost)
   - En **modo RELEASE**: `https://flowboard-api.onrender.com` (producción)

## Paso 5: Compilar la App para Producción

Para probar con el servidor de producción:

```bash
# Compilar en modo release
cd android
./gradlew assembleRelease

# O cambiar temporalmente BuildConfig.DEBUG a false para pruebas
```

## Notas Importantes sobre el Plan Gratuito de Render

⚠️ **El plan gratuito de Render tiene limitaciones:**

- **Hibernación**: El servicio se "duerme" después de 15 minutos de inactividad
- **Primer arranque lento**: Cuando la app hace la primera petición después de hibernar, puede tardar 30-60 segundos en responder
- **750 horas/mes**: Suficiente si solo tienes un servicio

### Soluciones para la Hibernación:

1. **Upgrade a plan pago** ($7/mes): Sin hibernación, mejor rendimiento
2. **UptimeRobot** (gratis): Servicio que hace ping a tu API cada 5 minutos para mantenerla activa
3. **Cron Job propio**: Configura un job que llame a tu API periódicamente

## Monitoreo y Logs

1. En el dashboard de Render, haz clic en tu servicio `flowboard-api`
2. Ve a la pestaña **"Logs"** para ver los logs en tiempo real
3. Ve a **"Metrics"** para ver uso de CPU, memoria, etc.

## Actualizar el Backend

Cada vez que hagas cambios y los subas a GitHub:

```bash
git add .
git commit -m "Update backend"
git push origin master
```

Render detectará los cambios automáticamente y desplegará la nueva versión.

## Alternativas a Render

Si prefieres otras opciones gratuitas:

- **Railway.app**: Similar a Render, $5 de crédito gratis/mes
- **Fly.io**: Plan gratuito con mejores especificaciones
- **Koyeb**: Plan gratuito sin hibernación (limitado)

## Solución de Problemas

### El build falla
- Revisa los logs en Render
- Verifica que el Dockerfile esté correcto
- Asegúrate de que las dependencias en `build.gradle.kts` sean compatibles

### Error de conexión a la base de datos
- Verifica que `DATABASE_URL` esté correctamente configurada
- Revisa que la base de datos esté en la misma región que el servicio web

### La app Android no se conecta
- Verifica que la URL en `ApiConfig.kt` sea correcta
- Asegúrate de compilar en modo release o cambiar a producción manualmente
- Revisa los logs de la app para ver errores de red

## ¿Necesitas Ayuda?

Si tienes problemas con el despliegue, revisa:
- Documentación de Render: https://render.com/docs
- Logs en el dashboard de Render
- Estado del servicio: https://status.render.com
