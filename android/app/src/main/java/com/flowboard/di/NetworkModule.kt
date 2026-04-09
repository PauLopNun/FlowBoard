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

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class HttpClientQualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class WebSocketClientQualifier

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        prettyPrint = true
        isLenient = true
        ignoreUnknownKeys = true
    }

    @Provides
    @Singleton
    fun provideHttpClient(): HttpClient = HttpClient(Android) {
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
        install(WebSockets) {
            pingInterval = 30_000
            maxFrameSize = Long.MAX_VALUE
        }
    }

    @Provides
    @Singleton
    fun provideAuthApiService(@HttpClientQualifier httpClient: HttpClient): AuthApiService =
        AuthApiService(httpClient)

    @Provides
    @Singleton
    fun provideTaskApiService(
        @HttpClientQualifier httpClient: HttpClient,
        authRepository: AuthRepository
    ): TaskApiService = TaskApiService(httpClient, authRepository)

    @Provides
    @Singleton
    fun providePermissionApiService(
        @HttpClientQualifier httpClient: HttpClient,
        authRepository: AuthRepository
    ): PermissionApiService = PermissionApiServiceImpl(httpClient, authRepository)

    @Provides
    @Singleton
    fun provideTaskWebSocketClient(@WebSocketClientQualifier httpClient: HttpClient): TaskWebSocketClient =
        TaskWebSocketClient(httpClient)
}
