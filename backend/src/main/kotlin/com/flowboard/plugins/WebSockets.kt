package com.flowboard.plugins

import io.ktor.server.application.*
import io.ktor.server.websocket.*
import java.time.Duration

// [PSP - RA4.a] configureWebSockets instala el plugin WebSocket (RFC 6455) en el servidor Ktor.
// WebSocket es un protocolo de comunicación FULL-DUPLEX sobre TCP:
//   - HTTP: el cliente hace una petición → el servidor responde → conexión cerrada
//   - WebSocket: cliente y servidor mantienen la conexión abierta y AMBOS pueden enviar datos
// La comunicación en tiempo real requiere que el servidor notifique a los clientes al instante.
// Con HTTP necesitaríamos polling cada segundo; WebSocket mantiene la conexión abierta y el
// servidor puede enviar datos cuando quiera, sin que el cliente lo solicite.
// Equivalente concepto del temario: socket TCP persistente de PU3 (socket.SOCK_STREAM sin close).
fun Application.configureWebSockets() {
    install(WebSockets) {

        // [PSP - RA4.g] Disponibilidad del servicio — mecanismo de keep-alive.
        // El servidor envía un frame PING al cliente cada 30 segundos.
        // Si el cliente responde con PONG → la conexión sigue viva.
        // Si no responde en 15 segundos → el servidor cierra la conexión (evita conexiones zombie).
        // El cliente Android también envía PINGs (TaskWebSocketClient.kt, startPingLoop()).
        pingPeriod = Duration.ofSeconds(30)   // ping cada 30 segundos

        // [PSP - RA4.g] Si el cliente no responde al PING en 15 segundos, la conexión se cierra
        timeout = Duration.ofSeconds(15)

        // Tamaño máximo permitido de un frame WebSocket (Long.MAX_VALUE = sin límite práctico)
        // Un frame es la unidad mínima de datos en el protocolo WebSocket
        maxFrameSize = Long.MAX_VALUE

        // masking=false: en producción los frames del servidor no necesitan enmascaramiento
        // (el enmascaramiento es obligatorio solo para frames del cliente → cliente lo gestiona)
        masking = false

        // [PSP - RA4.c] Se pueden añadir extensiones del protocolo WebSocket aquí,
        // como deflate para comprimir los mensajes y reducir el ancho de banda.
        extensions {
            // extensiones futuras (compresión, etc.)
        }
    }
}
