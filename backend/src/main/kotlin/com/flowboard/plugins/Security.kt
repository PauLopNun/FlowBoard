package com.flowboard.plugins

// [PSP - RA5.b] Librería auth0/java-jwt — implementa el estándar JWT (RFC 7519)
// Equivalente a: import jwt  en Python (PyJWT)
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*

// [PSP - RA5.c] configureSecurity() instala el plugin de autenticación JWT en Ktor.
// A partir de aquí, cualquier ruta envuelta en authenticate("auth-jwt") { }
// exige un token válido — si no hay token o es inválido, Ktor responde 401 automáticamente.
fun Application.configureSecurity() {
    install(Authentication) {
        jwt("auth-jwt") {                           // nombre del esquema de autenticación
            realm = JwtConfig.realm
            verifier(JwtConfig.verifier)            // [PSP] usa el verificador HMAC-SHA256 de JwtConfig
            validate { credential ->
                // [PSP - RA5.c] Política de acceso: el token debe contener un userId no vacío.
                // Si no lo tiene → validate devuelve null → Ktor responde 401 Unauthorized.
                val userId = credential.payload.getClaim("userId")?.asString()
                if (!userId.isNullOrEmpty()) JWTPrincipal(credential.payload) else null
            }
        }
    }
}

// [PSP - RA5.b] JwtConfig — Singleton (object en Kotlin) que centraliza toda la lógica JWT.
// Un "object" en Kotlin es un singleton por diseño: se crea una sola instancia al cargar la clase.
// Pregunta posible: "¿dónde creas el token?" → aquí, en makeToken()
// Pregunta posible: "¿dónde está HMAC-SHA256?" → aquí, en la línea Algorithm.HMAC256(secret)
object JwtConfig {

    // [PSP - RA5.a] Principio de programación segura: el secreto NO está hardcodeado.
    // Se lee de la variable de entorno JWT_SECRET (configurada en Render.com).
    // Si no existe (desarrollo local), usa "dev-secret-key" como fallback.
    private val secret   = System.getenv("JWT_SECRET")   ?: "dev-secret-key"
    private val issuer   = System.getenv("JWT_ISSUER")   ?: "flowboard-api"
    private val audience = System.getenv("JWT_AUDIENCE") ?: "flowboard-app"
    val realm = "FlowBoard Access"

    // [PSP - RA5.b] AQUÍ SE DECLARA EL ALGORITMO CRIPTOGRÁFICO: HMAC-SHA256.
    // HMAC = Hash-based Message Authentication Code.
    // SHA-256 = función de hash de 256 bits.
    // El 'secret' es la clave simétrica compartida — solo el servidor la conoce.
    // Equivalente Python: Algorithm.HMAC256(secret) ≈ hmac.new(secret.encode(), msg, hashlib.sha256)
    private val algorithm = Algorithm.HMAC256(secret)

    // [PSP - RA5.c] El verifier comprueba en CADA PETICIÓN que la firma del token es válida.
    // Si alguien modifica el payload del token, la firma no cuadra → verifier lo rechaza.
    val verifier = JWT
        .require(algorithm)         // exige que el token esté firmado con nuestro HMAC-SHA256
        .withIssuer(issuer)         // exige que el campo "iss" sea "flowboard-api"
        .withAudience(audience)     // exige que el campo "aud" sea "flowboard-app"
        .build()

    // [PSP - RA5.b] AQUÍ SE CREA (FIRMA) EL TOKEN JWT.
    // Estructura del JWT resultante: HEADER.PAYLOAD.SIGNATURE
    //   HEADER:    {"alg":"HS256","typ":"JWT"}          (codificado en Base64Url)
    //   PAYLOAD:   {"sub":"Authentication","iss":"flowboard-api","email":"...","userId":"..."}
    //   SIGNATURE: HMAC-SHA256(base64(header) + "." + base64(payload), secret)
    // La firma HMAC-SHA256 se genera en .sign(algorithm) — última línea del bloque.
    fun makeToken(email: String, userId: String, username: String = ""): String =
        JWT.create()
            .withSubject("Authentication")  // propósito del token
            .withIssuer(issuer)             // quién lo emite
            .withAudience(audience)         // para quién es
            .withClaim("email", email)      // datos del usuario en el PAYLOAD
            .withClaim("userId", userId)    // el servidor lo lee en cada petición para saber quién eres
            .withClaim("username", username)
            .sign(algorithm)               // ← AQUÍ SE APLICA HMAC-SHA256 y se genera la SIGNATURE
}
