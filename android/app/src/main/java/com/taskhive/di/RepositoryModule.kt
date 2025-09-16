package com.flowboard.di

import com.flowboard.data.local.dao.TaskDao
import com.flowboard.data.remote.api.TaskApiService
import com.flowboard.data.repository.TaskRepositoryImpl
import com.flowboard.domain.repository.TaskRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideTaskRepository(
        taskDao: TaskDao,
        taskApiService: TaskApiService
    ): TaskRepository {
        return TaskRepositoryImpl(taskDao, taskApiService)
    }
}