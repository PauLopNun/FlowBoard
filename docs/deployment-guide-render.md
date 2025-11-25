# Guía de Deployment - FlowBoard Backend en Render

## 📋 Índice
1. [Preparación del Proyecto](#preparación-del-proyecto)
2. [Configuración de Render](#configuración-de-render)
3. [Variables de Entorno](#variables-de-entorno)
4. [Configuración de Base de Datos](#configuración-de-base-de-datos)
5. [Deployment](#deployment)
6. [Verificación](#verificación)
7. [Troubleshooting](#troubleshooting)

---

## 🚀 Preparación del Proyecto

### Paso 1: Crear Procfile para Render

Crea un archivo `Procfile` en la raíz del directorio `backend/`:

```bash
# backend/Procfile
web: java -jar build/libs/backend-all.jar
```

### Paso 2: Crear render.yaml

Crea `render.yaml` en la raíz del directorio `backend/`:

```yaml
services:
  - type: web
    name: flowboard-backend
    env: docker
    region: oregon
    plan: free
    buildCommand: ./gradlew clean build
    startCommand: java -Xmx512m -jar build/libs/backend-all.jar
    envVars:
      - key: PORT
        value: 8080
      - key: JWT_SECRET
        generateValue: true
      - key: JWT_ISSUER
        value: flowboard.com
      - key: JWT_AUDIENCE
        value: flowboard-audience
      - key: DATABASE_URL
        fromDatabase:
          name: flowboard-db
          property: connectionString
    healthCheckPath: /

databases:
  - name: flowboard-db
    databaseName: flowboard
    user: flowboard_user
    plan: free
```

### Paso 3: Actualizar build.gradle.kts para Fat JAR

Asegúrate de que `backend/build.gradle.kts` tenga la configuración para crear un Fat JAR:

```kotlin
plugins {
    kotlin("jvm") version "1.9.22"
    id("io.ktor.plugin") version "2.3.7"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.22"
}

application {
    mainClass.set("com.flowboard.ApplicationKt")
}

ktor {
    fatJar {
        archiveFileName.set("backend-all.jar")
    }
}
```

### Paso 4: Configurar puerto dinámico

Actualiza `Application.kt` para leer el puerto desde variable de entorno:

```kotlin
fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080

    embeddedServer(
        Netty,
        port = port,
        host = "0.0.0.0",
        module = Application::module
    ).start(wait = true)
}
```

### Paso 5: Actualizar DatabaseFactory para usar DATABASE_URL

```kotlin
object DatabaseFactory {
    fun init() {
        val databaseUrl = System.getenv("DATABASE_URL")
            ?: "jdbc:postgresql://localhost:5432/flowboard?user=postgres&password=postgres"

        val config = HikariConfig().apply {
            jdbcUrl = databaseUrl
            driverClassName = "org.postgresql.Driver"
            maximumPoolSize = 10
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            validate()
        }

        val dataSource = HikariDataSource(config)

        Database.connect(dataSource)

        transaction {
            SchemaUtils.create(Tasks, Users, Projects)
        }
    }
}
```

### Paso 6: Actualizar CORS para Render

En `plugins/HTTP.kt`:

```kotlin
install(CORS) {
    allowHost("flowboard-backend.onrender.com", schemes = listOf("https"))
    allowHost("flowboard-backend.onrender.com", schemes = listOf("wss"))
    allowHost("localhost:3000") // Para desarrollo local
    allowMethod(HttpMethod.Options)
    allowMethod(HttpMethod.Put)
    allowMethod(HttpMethod.Delete)
    allowMethod(HttpMethod.Patch)
    allowHeader(HttpHeaders.Authorization)
    allowHeader(HttpHeaders.ContentType)
    allowCredentials = true
}
```

### Paso 7: Actualizar gitignore

Asegúrate de que `backend/.gitignore` incluya:

```
.gradle
build/
!gradle/wrapper/gradle-wrapper.jar
*.jar
!gradle-wrapper.jar
```

---

## ⚙️ Configuración de Render

### Paso 1: Crear Cuenta en Render

1. Ve a [https://render.com](https://render.com)
2. Crea una cuenta (puedes usar GitHub)
3. Verifica tu email

### Paso 2: Conectar Repositorio GitHub

1. En Render Dashboard, haz clic en **"New +"**
2. Selecciona **"Web Service"**
3. Conecta tu cuenta de GitHub
4. Autoriza a Render para acceder a tu repositorio FlowBoard

### Paso 3: Configurar Web Service

**Configuración básica:**
- **Name:** `flowboard-backend`
- **Region:** `Oregon (US West)`
- **Branch:** `master`
- **Root Directory:** `backend`
- **Runtime:** `Gradle`
- **Build Command:** `./gradlew clean build`
- **Start Command:** `java -Xmx512m -jar build/libs/backend-all.jar`
- **Plan:** `Free` (para empezar)

**Avanzado:**
- **Auto-Deploy:** `Yes` (para deployment automático en push)
- **Health Check Path:** `/`

### Paso 4: Configurar Base de Datos PostgreSQL

1. En Render Dashboard, haz clic en **"New +"**
2. Selecciona **"PostgreSQL"**
3. Configuración:
   - **Name:** `flowboard-db`
   - **Database:** `flowboard`
   - **User:** `flowboard_user`
   - **Region:** `Oregon` (misma que web service)
   - **Plan:** `Free`
4. Haz clic en **"Create Database"**
5. Espera a que se cree (1-2 minutos)

---

## 🔐 Variables de Entorno

### Configurar en Render Dashboard

En tu Web Service, ve a **"Environment"** y agrega:

| Variable | Valor | Descripción |
|----------|-------|-------------|
| `PORT` | `8080` | Puerto del servidor (Render lo sobrescribirá) |
| `JWT_SECRET` | `[GENERAR SECRETO SEGURO]` | Secreto para firmar JWT tokens |
| `JWT_ISSUER` | `flowboard.com` | Emisor de tokens JWT |
| `JWT_AUDIENCE` | `flowboard-audience` | Audiencia de tokens JWT |
| `DATABASE_URL` | `[AUTO]` | URL de conexión PostgreSQL (generada por Render) |

**Generar JWT_SECRET seguro:**
```bash
# En tu terminal local
openssl rand -hex 64

# O en Node.js
node -e "console.log(require('crypto').randomBytes(64).toString('hex'))"
```

**DATABASE_URL se configura automáticamente:**
1. Ve a tu PostgreSQL Database en Render
2. Copia el **"Internal Database URL"**
3. En tu Web Service → Environment → Add Environment Variable
4. Key: `DATABASE_URL`
5. Value: Pega la URL copiada

---

## 🚀 Deployment

### Método 1: Deployment Automático (Recomendado)

1. Haz commit de todos los cambios:
```bash
cd C:\Users\paulo\Desktop\FlowBoard
git add .
git commit -m "Configure backend for Render deployment"
git push origin master
```

2. Render detectará el push y comenzará el deployment automáticamente

3. Monitorea el progreso en **Render Dashboard → Logs**

### Método 2: Deployment Manual

1. En Render Dashboard → Tu Web Service
2. Haz clic en **"Manual Deploy"** → **"Deploy latest commit"**
3. Espera a que termine (5-10 minutos la primera vez)

### Logs del Deployment

Render mostrará logs en tiempo real:

```
==> Building...
./gradlew clean build
> Task :compileKotlin
> Task :compileJava
> Task :processResources
> Task :classes
> Task :jar
> Task :buildFatJar
BUILD SUCCESSFUL in 2m 15s

==> Deploying...
Starting service...
Server started at http://0.0.0.0:8080
```

---

## ✅ Verificación

### Paso 1: Verificar Deployment Exitoso

Una vez desplegado, Render te dará una URL:
```
https://flowboard-backend.onrender.com
```

### Paso 2: Probar Endpoint Principal

```bash
curl https://flowboard-backend.onrender.com

# Respuesta esperada:
"FlowBoard API is running!"
```

### Paso 3: Probar Endpoint de Stats

```bash
curl https://flowboard-backend.onrender.com/ws/stats

# Respuesta esperada:
{"activeSessions":0,"activeRooms":0,"timestamp":"2025-11-25T..."}
```

### Paso 4: Probar WebSocket

Usa un cliente WebSocket (ej: websocat, wscat):

```bash
# Instalar wscat
npm install -g wscat

# Conectar (necesitas un token JWT válido)
wscat -c wss://flowboard-backend.onrender.com/ws/boards \
  -H "Authorization: Bearer YOUR_JWT_TOKEN_HERE"
```

### Paso 5: Actualizar Android App

En `TaskWebSocketClient.kt`, actualiza la URL:

```kotlin
companion object {
    private const val TAG = "TaskWebSocketClient"

    // ❌ Desarrollo local
    // private const val WS_URL = "ws://10.0.2.2:8080/ws/boards"

    // ✅ Producción en Render
    private const val WS_URL = "wss://flowboard-backend.onrender.com/ws/boards"

    // Resto del código...
}
```

Y en `TaskApiService.kt`:

```kotlin
companion object {
    // ✅ Producción
    private const val BASE_URL = "https://flowboard-backend.onrender.com/api/v1"

    // ❌ Desarrollo
    // private const val BASE_URL = "http://10.0.2.2:8080/api/v1"
}
```

---

## 🔧 Troubleshooting

### Problema 1: Build Failed

**Síntoma:** El build falla con errores de Gradle

**Solución:**
1. Verifica que `gradlew` tenga permisos de ejecución:
```bash
chmod +x backend/gradlew
git add backend/gradlew
git commit -m "Make gradlew executable"
git push
```

2. Verifica `gradle/wrapper/gradle-wrapper.properties`:
```properties
distributionUrl=https\://services.gradle.org/distributions/gradle-8.5-bin.zip
```

### Problema 2: "Cannot connect to database"

**Síntoma:** Logs muestran `Connection refused` o `Unknown host`

**Solución:**
1. Verifica que `DATABASE_URL` esté configurada correctamente
2. Usa **Internal Database URL**, no External:
   - ✅ `postgresql://flowboard_user:...@dpg-XXX-a/flowboard` (Internal)
   - ❌ `postgresql://flowboard_user:...@oregon-postgres.render.com/flowboard` (External)

### Problema 3: WebSocket no conecta

**Síntoma:** Android app no puede conectar via WebSocket

**Solución:**
1. Verifica que uses **WSS** (no WS) en producción:
   ```kotlin
   private const val WS_URL = "wss://flowboard-backend.onrender.com/ws/boards"
   ```

2. Verifica que CORS permita tu dominio

3. Verifica que el JWT token sea válido

### Problema 4: "Service crashed"

**Síntoma:** El servicio se inicia pero se cae inmediatamente

**Solución:**
1. Revisa los logs en Render Dashboard
2. Verifica que el puerto esté configurado correctamente:
```kotlin
val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
```

3. Aumenta la memoria disponible:
```yaml
startCommand: java -Xmx512m -jar build/libs/backend-all.jar
```

### Problema 5: Free Tier Limitations

**Limitaciones del plan Free:**
- El servicio se duerme después de 15 minutos de inactividad
- Tarda ~30 segundos en despertar en la primera request
- 750 horas/mes de uptime (suficiente para 24/7)

**Solución para "cold starts":**
- Upgrade a plan Starter ($7/mes) para servicio 24/7
- O implementa un ping automático cada 10 minutos

---

## 📊 Monitoring

### Logs en Tiempo Real

```
Render Dashboard → Tu Service → Logs
```

### Métricas

Render Free plan incluye:
- Request count
- Response time
- CPU usage
- Memory usage

### Alertas

Configura alertas en:
```
Render Dashboard → Tu Service → Settings → Notifications
```

---

## 🔄 Actualizaciones

### Deployment Automático

Cada vez que hagas `git push origin master`, Render:
1. Detecta el cambio
2. Ejecuta `./gradlew clean build`
3. Reinicia el servicio con el nuevo JAR

### Rollback

Si algo sale mal:
1. Ve a **Render Dashboard → Tu Service → Events**
2. Haz clic en **"Rollback to"** en un deployment previo exitoso

---

## 🎯 Mejores Prácticas

### 1. Usar Branches para Staging

Crea un servicio separado para staging:
- **Production:** `master` branch → `flowboard-backend`
- **Staging:** `develop` branch → `flowboard-backend-staging`

### 2. Secrets Management

**NO hagas commit de:**
- JWT secrets
- Database passwords
- API keys

Usa siempre variables de entorno en Render.

### 3. Health Checks

Implementa un endpoint dedicado:

```kotlin
get("/health") {
    val dbHealthy = try {
        transaction { true }
    } catch (e: Exception) {
        false
    }

    if (dbHealthy) {
        call.respond(HttpStatusCode.OK, mapOf("status" to "healthy"))
    } else {
        call.respond(HttpStatusCode.ServiceUnavailable, mapOf("status" to "unhealthy"))
    }
}
```

### 4. Logs Estructurados

Usa logging con niveles:
```kotlin
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("Application")

logger.info("Server started successfully")
logger.error("Database connection failed", exception)
```

---

## 💰 Costos

### Plan Free (Actual)
- **Costo:** $0/mes
- **RAM:** 512 MB
- **CPU:** Compartida
- **Uptime:** 750 horas/mes
- **Durmiente:** Sí (después de 15 min inactividad)

### Plan Starter (Recomendado para Producción)
- **Costo:** $7/mes
- **RAM:** 512 MB
- **CPU:** Compartida
- **Uptime:** Ilimitado (24/7)
- **Durmiente:** No

### Plan Standard (Para Apps con Tráfico)
- **Costo:** $25/mes
- **RAM:** 2 GB
- **CPU:** Dedicada
- **Uptime:** Ilimitado
- **Durmiente:** No

---

## 🚀 Siguiente: Publicar Android App

Ahora que tu backend está en producción en Render, puedes:
1. Actualizar las URLs en la app Android
2. Probar la app contra el backend en producción
3. Seguir la guía de publicación en Play Store

Ver: `docs/play-store-publishing-guide.md`

---

**Versión:** 1.0.0
**Última actualización:** 2025-11-25
**Autor:** FlowBoard Team
