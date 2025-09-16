package com.flowboard.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.requestvalidation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*

fun Application.configureValidation() {
    install(RequestValidation)
    
    install(StatusPages) {
        exception<RequestValidationException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, cause.reasons.joinToString())
        }
        
        exception<Throwable> { call, cause ->
            call.respondText(
                text = "500: $cause", 
                status = HttpStatusCode.InternalServerError
            )
        }
    }
}