package com.flowboard.di

import android.content.Context
import androidx.room.Room
import com.flowboard.data.local.FlowBoardDatabase
import com.flowboard.data.local.dao.ProjectDao
import com.flowboard.data.local.dao.TaskDao
import com.flowboard.data.local.dao.UserDao
import com.flowboard.data.local.dao.ChatDao
import com.flowboard.data.local.dao.DocumentDao
import com.flowboard.data.local.dao.PendingOperationDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideFlowBoardDatabase(
        @ApplicationContext context: Context
    ): FlowBoardDatabase {
        return Room.databaseBuilder(
            context,
            FlowBoardDatabase::class.java,
            FlowBoardDatabase.DATABASE_NAME
        )
        .fallbackToDestructiveMigration()
        .build()
    }

    @Provides
    fun provideTaskDao(database: FlowBoardDatabase): TaskDao {
        return database.taskDao()
    }

    @Provides
    fun provideUserDao(database: FlowBoardDatabase): UserDao {
        return database.userDao()
    }

    @Provides
    fun provideProjectDao(database: FlowBoardDatabase): ProjectDao {
        return database.projectDao()
    }

    @Provides
    fun provideChatDao(database: FlowBoardDatabase): ChatDao {
        return database.chatDao()
    }

    @Provides
    fun provideDocumentDao(database: FlowBoardDatabase) = database.documentDao()

    @Provides
    fun providePendingOperationDao(database: FlowBoardDatabase) = database.pendingOperationDao()
}