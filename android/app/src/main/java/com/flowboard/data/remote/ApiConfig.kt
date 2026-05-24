package com.flowboard.data.remote

// [PSP - RA4.c] ApiConfig — Singleton (Kotlin object) con todas las URLs de la API.
// Un "object" en Kotlin es un singleton por diseño del lenguaje:
//   - Se crea UNA SOLA VEZ cuando se carga la clase por primera vez.
//   - Cualquier parte de la app que use ApiConfig.BASE_URL accede a la MISMA instancia.
// Al centralizar las URLs aquí, si el servidor cambia solo hay que modificar esta clase.
object ApiConfig {

    // [PSP - RA5.f] URL de producción — SIEMPRE usa HTTPS (HTTP + TLS).
    // TLS (Transport Layer Security) cifra todo el tráfico entre la app Android y el servidor:
    //   - Confidencialidad: nadie puede leer los datos en tránsito (incluido el JWT)
    //   - Integridad: nadie puede modificar los datos en tránsito sin que se detecte
    //   - Autenticación: el certificado SSL de Render.com (Let's Encrypt) verifica el servidor
    // Equivalente PU5: el túnel RSA+Fernet del temario — aquí TLS hace lo mismo automáticamente.
    private const val PRODUCTION_BASE_URL  = "https://flowboard-api-phrk.onrender.com"

    // URL para desarrollo local (el emulador Android accede al PC por 10.0.2.2)
    private const val DEVELOPMENT_BASE_URL = "http://10.0.2.2:8080"

    // [PSP] En producción siempre usamos HTTPS. En desarrollo local HTTP (no hay certificado).
    private const val USE_PRODUCTION = true

    // [PSP - RA5.f] BASE_URL — punto de acceso único al servidor.
    // Toda la app usa esta constante → si el servidor cambia, solo hay que cambiarla aquí.
    val BASE_URL: String = if (USE_PRODUCTION) PRODUCTION_BASE_URL else DEVELOPMENT_BASE_URL

    // [PSP - RA4.a] API_BASE_URL — ruta base de la REST API (protocolo HTTP/REST)
    // Todos los endpoints REST están bajo /api/v1/ (versionado de API)
    val API_BASE_URL = "$BASE_URL/api/v1"

    // [PSP - RA5.g] WS_BASE_URL — URL para conexiones WebSocket.
    // En producción: wss:// = WebSocket Secure = WebSocket sobre TLS
    //   igual que HTTPS pero para el protocolo WebSocket (RFC 6455)
    // En desarrollo: ws:// = WebSocket sin cifrado (solo en local, no hay datos sensibles en tránsito)
    val WS_BASE_URL: String = if (USE_PRODUCTION)
        "wss://flowboard-api-phrk.onrender.com"   // [PSP] WSS = WebSocket + TLS cifrado
    else
        "ws://10.0.2.2:8080"

    // [PSP - RA4.a] Endpoints principales del API REST
    // Cada endpoint corresponde a un recurso REST con sus operaciones CRUD
    object Endpoints {
        const val AUTH     = "/auth"       // POST /auth/login, POST /auth/register
        const val TASKS    = "/tasks"      // GET, POST, PUT, DELETE /tasks
        const val PROJECTS = "/projects"   // GET, POST, PUT, DELETE /projects
        const val USERS    = "/users"      // GET, PUT /users
        const val WS_BOARDS = "/ws/boards" // WebSocket de tareas en tiempo real
    }

    fun getCustomBaseUrl(customUrl: String): String {
        return if (customUrl.isNotEmpty()) customUrl else BASE_URL
    }
}
