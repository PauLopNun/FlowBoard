package com.flowboard.di

import com.flowboard.data.remote.api.AuthApiService
import com.flowboard.data.remote.api.TaskApiService
import com.flowboard.data.remote.websocket.TaskWebSocketClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.websocket.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideHttpClient(): HttpClient {
        return HttpClient(Android) {
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    isLenient = true
                    ignoreUnknownKeys = true
                })
            }

            install(Logging) {
                logger = Logger.DEFAULT
                level = LogLevel.INFO
            }

            install(Auth) {
                bearer {
                    loadTokens {
                        // Load stored tokens (implement token storage)
                        null
                    }
                    refreshTokens {
                        // Refresh tokens logic
                        null
                    }
                }
            }

            // WebSocket support
            install(WebSockets) {
                pingInterval = 30_000 // 30 segundos
                maxFrameSize = Long.MAX_VALUE
            }
        }
    }

    @Provides
    @Singleton
    fun provideTaskApiService(httpClient: HttpClient): TaskApiService {
        return TaskApiService(httpClient)
    }

    @Provides
    @Singleton
    fun provideTaskWebSocketClient(httpClient: HttpClient): TaskWebSocketClient {
        return TaskWebSocketClient(httpClient)
    }

    @Provides
    @Singleton
    fun provideAuthApiService(httpClient: HttpClient): AuthApiService {
        return AuthApiService(httpClient)
    }
}