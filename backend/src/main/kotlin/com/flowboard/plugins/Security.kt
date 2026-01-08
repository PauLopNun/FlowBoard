package com.flowboard.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*

fun Application.configureSecurity() {
    val jwtAudience = environment.config.propertyOrNull("jwt.audience")?.getString() ?: "flowboard-audience"
    val jwtDomain = environment.config.propertyOrNull("jwt.domain")?.getString() ?: "flowboard.com"
    val jwtRealm = environment.config.propertyOrNull("jwt.realm")?.getString() ?: "FlowBoard API"
    val jwtSecret = environment.config.propertyOrNull("jwt.secret")?.getString() ?: "your-secret-key"
    
    install(Authentication) {
        jwt("jwt") {  // Cambiar de "auth-jwt" a "jwt" para coincidir con las rutas
            realm = jwtRealm
            verifier(
                JWT
                    .require(Algorithm.HMAC256(jwtSecret))
                    .withAudience(jwtAudience)
                    .withIssuer(jwtDomain)
                    .build()
            )
            validate { credential ->
                if (credential.payload.getClaim("email").asString() != "") {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
        }
    }
}

object JwtConfig {
    private val secret = System.getenv("JWT_SECRET") ?: "your-secret-key"
    private val issuer = System.getenv("JWT_ISSUER") ?: "flowboard.com"
    private val audience = System.getenv("JWT_AUDIENCE") ?: "flowboard-audience"
    val realm = "FlowBoard API"
    
    private val algorithm = Algorithm.HMAC256(secret)
    
    val verifier = JWT
        .require(algorithm)
        .withIssuer(issuer)
        .withAudience(audience)
        .build()
    
    fun makeToken(email: String, userId: String): String = JWT.create()
        .withSubject("Authentication")
        .withIssuer(issuer)
        .withAudience(audience)
        .withClaim("email", email)
        .withClaim("userId", userId)
        .sign(algorithm)
}