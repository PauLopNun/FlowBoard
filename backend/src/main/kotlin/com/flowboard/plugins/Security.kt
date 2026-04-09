package com.flowboard.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*

fun Application.configureSecurity() {
    install(Authentication) {
        jwt("auth-jwt") {
            realm = JwtConfig.realm
            verifier(JwtConfig.verifier)
            validate { credential ->
                val userId = credential.payload.getClaim("userId")?.asString()
                if (!userId.isNullOrEmpty()) JWTPrincipal(credential.payload) else null
            }
        }
    }
}

object JwtConfig {
    private val secret = System.getenv("JWT_SECRET") ?: "dev-secret-key"
    private val issuer = System.getenv("JWT_ISSUER") ?: "flowboard-api"
    private val audience = System.getenv("JWT_AUDIENCE") ?: "flowboard-app"
    val realm = "FlowBoard Access"

    private val algorithm = Algorithm.HMAC256(secret)

    val verifier = JWT
        .require(algorithm)
        .withIssuer(issuer)
        .withAudience(audience)
        .build()

    fun makeToken(email: String, userId: String, username: String = ""): String =
        JWT.create()
            .withSubject("Authentication")
            .withIssuer(issuer)
            .withAudience(audience)
            .withClaim("email", email)
            .withClaim("userId", userId)
            .withClaim("username", username)
            .sign(algorithm)
}
