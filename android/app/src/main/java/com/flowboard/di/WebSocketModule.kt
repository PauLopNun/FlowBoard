package com.flowboard.di

import com.flowboard.data.remote.websocket.DocumentWebSocketClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.*
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object WebSocketModule {

    @Provides
    @Singleton
    fun provideDocumentWebSocketClient(
        @WebSocketClientQualifier client: HttpClient,
        json: Json
    ): DocumentWebSocketClient {
        return DocumentWebSocketClient(client, json)
    }
}
