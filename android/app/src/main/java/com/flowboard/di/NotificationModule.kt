package com.flowboard.di

import com.flowboard.data.local.dao.NotificationDao
import com.flowboard.data.local.FlowBoardDatabase
import com.flowboard.data.repository.NotificationRepositoryImpl
import com.flowboard.domain.repository.NotificationRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class NotificationModule {

    @Binds
    @Singleton
    abstract fun bindNotificationRepository(
        notificationRepositoryImpl: NotificationRepositoryImpl
    ): NotificationRepository

    companion object {
        @Provides
        @Singleton
        fun provideNotificationDao(database: FlowBoardDatabase): NotificationDao {
            return database.notificationDao()
        }
    }
}
