package com.flowboard.di

import com.flowboard.data.remote.api.AuthApiService
import com.flowboard.data.remote.api.PermissionApiService
import com.flowboard.data.remote.api.PermissionApiServiceImpl
import com.flowboard.data.remote.api.TaskApiService
import com.flowboard.data.remote.websocket.TaskWebSocketClient
import com.flowboard.data.repository.AuthRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.plugins.websocket.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import javax.inject.Qualifier
import javax.inject.Singleton

// [PSP - RA4.c] Qualifiers de Hilt: permiten tener dos instancias de HttpClient en el mismo módulo.
// @HttpClientQualifier      → cliente REST (motor Android, nativo de Ktor)
// @WebSocketClientQualifier → cliente WebSocket (motor OkHttp, mejor soporte WS en Android)
// Hilt usa estos qualifiers para saber qué instancia inyectar en cada punto.
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class HttpClientQualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class WebSocketClientQualifier

// [PSP - RA4.c] NetworkModule — módulo de Inyección de Dependencias (DI) con Hilt.
// @Module: declara cómo construir cada objeto de red.
// @InstallIn(SingletonComponent::class): los objetos viven durante todo el ciclo de vida de la app.
// "object NetworkModule" → Kotlin singleton: una sola instancia de la clase.
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    // [PSP - RA4.b] provideJson — configuración del serializador/deserializador JSON (kotlinx.serialization).
    // ContentNegotiation (más abajo) usa esta instancia para convertir:
    //   JSON recibido  →  objeto Kotlin  (al recibir una respuesta del servidor)
    //   objeto Kotlin  →  JSON enviado   (al enviar el cuerpo de una petición)
    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        prettyPrint = false
        isLenient = true
        ignoreUnknownKeys = true  // si el servidor añade campos nuevos, el cliente no falla
        encodeDefaults = true
        coerceInputValues = true
    }

    // [PSP - RA4.c] provideHttpClient — cliente HTTP para peticiones REST.
    // @Singleton: Hilt crea UNA SOLA instancia y la reutiliza en toda la app.
    // Motor Android: usa HttpURLConnection nativo, optimizado para Android.
    @Provides
    @Singleton
    fun provideHttpClient(): HttpClient = HttpClient(Android) {
        engine {
            connectTimeout = 30_000  // 30 segundos para establecer conexión TCP
            socketTimeout = 30_000   // 30 segundos de inactividad antes de cerrar
        }
        // [PSP - RA4.b] ContentNegotiation — negocia automáticamente el formato de datos con el servidor.
        // Convierte la respuesta JSON en objetos Kotlin y viceversa (serialización/deserialización).
        install(ContentNegotiation) {
            json(Json { isLenient = true; ignoreUnknownKeys = true })
        }
        // [PSP - RA4.h] Logging — registra cabeceras HTTP en Logcat para depuración.
        // LogLevel.HEADERS muestra las cabeceras de cada petición/respuesta sin exponer el cuerpo.
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.HEADERS
        }
        expectSuccess = false  // no lanza excepción en respuestas 4xx/5xx; el código las gestiona
    }

    // [PSP - RA4.c] @HttpClientQualifier — cliente HTTP cualificado, usado por los ApiService.
    // El qualifier permite que Hilt lo distinga del cliente WebSocket al inyectar dependencias.
    @Provides
    @Singleton
    @HttpClientQualifier
    fun provideHttpClientQualified(): HttpClient = HttpClient(Android) {
        engine {
            connectTimeout = 30_000
            socketTimeout = 30_000
        }
        install(ContentNegotiation) {
            json(Json { isLenient = true; ignoreUnknownKeys = true })
        }
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.HEADERS
        }
        expectSuccess = false
    }

    // [PSP - RA4.c] @WebSocketClientQualifier — cliente WebSocket con motor OkHttp.
    // Se usa un motor separado porque OkHttp tiene mejor soporte para WebSocket en Android
    // que el motor Android nativo (gestiona mejor los frames, pings y la reconexión).
    // [PSP - RA4.g] install(WebSockets) { pingInterval = 30_000 } — mecanismo de keep-alive.
    // El cliente envía un frame PING al servidor cada 30 segundos para verificar disponibilidad.
    // Si el servidor no responde, TaskWebSocketClient detecta la desconexión e inicia la reconexión.
    @Provides
    @Singleton
    @WebSocketClientQualifier
    fun provideWebSocketClient(): HttpClient = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json { isLenient = true; ignoreUnknownKeys = true })
        }
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.INFO
        }
        // [PSP - RA4.g] Ping WebSocket desde el cliente cada 30 segundos.
        // Complementa el pingPeriod=30s del servidor (WebSockets.kt) para disponibilidad bidireccional.
        install(WebSockets) {
            pingInterval = 30_000        // milisegundos entre pings
            maxFrameSize = Long.MAX_VALUE // tamaño máximo de un frame WebSocket
        }
    }

    // [PSP - RA5.c] provideAuthApiService — cliente del API de autenticación (login, registro, reset).
    // @Singleton: una sola instancia en toda la app.
    @Provides
    @Singleton
    fun provideAuthApiService(@HttpClientQualifier httpClient: HttpClient): AuthApiService =
        AuthApiService(httpClient)

    // [PSP - RA4.d] provideTaskApiService — cliente del API REST de tareas.
    // authRepository inyecta el JWT en cada cabecera Authorization: Bearer <token>.
    @Provides
    @Singleton
    fun provideTaskApiService(
        @HttpClientQualifier httpClient: HttpClient,
        authRepository: AuthRepository
    ): TaskApiService = TaskApiService(httpClient, authRepository)

    // [PSP - RA5.d] providePermissionApiService — cliente del API de permisos y roles.
    @Provides
    @Singleton
    fun providePermissionApiService(
        @HttpClientQualifier httpClient: HttpClient,
        authRepository: AuthRepository
    ): PermissionApiService = PermissionApiServiceImpl(httpClient, authRepository)

    // [PSP - RA4.f] provideTaskWebSocketClient — singleton del cliente WebSocket de tareas.
    // Usa @WebSocketClientQualifier (OkHttp) porque mantiene la conexión abierta permanentemente.
    // Al ser @Singleton, todos los ViewModels comparten la misma sesión WebSocket activa.
    @Provides
    @Singleton
    fun provideTaskWebSocketClient(@WebSocketClientQualifier httpClient: HttpClient): TaskWebSocketClient =
        TaskWebSocketClient(httpClient)

    @Provides
    @Singleton
    fun provideAiApiService(@HttpClientQualifier httpClient: HttpClient): com.flowboard.data.remote.api.AiApiService =
        com.flowboard.data.remote.api.AiApiService(httpClient)

    @Provides
    @Singleton
    fun provideNotificationApiService(
        @HttpClientQualifier httpClient: HttpClient,
        authRepository: AuthRepository
    ): com.flowboard.data.remote.api.NotificationApiService =
        com.flowboard.data.remote.api.NotificationApiService(httpClient, authRepository)
}
