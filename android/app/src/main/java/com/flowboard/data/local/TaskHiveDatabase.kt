package com.flowboard.data.local

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.flowboard.data.local.dao.ProjectDao
import com.flowboard.data.local.dao.TaskDao
import com.flowboard.data.local.dao.UserDao
import com.flowboard.data.local.entities.ProjectEntity
import com.flowboard.data.local.entities.TaskEntity
import com.flowboard.data.local.entities.UserEntity
import com.flowboard.data.local.entities.NotificationEntity
import com.flowboard.data.local.entities.ChatRoomEntity
import com.flowboard.data.local.entities.MessageEntity
import com.flowboard.data.local.entities.ChatParticipantEntity
import com.flowboard.data.local.entities.TypingIndicatorEntity

@Database(
    entities = [
        TaskEntity::class,
        UserEntity::class,
        ProjectEntity::class,
        NotificationEntity::class,
        ChatRoomEntity::class,
        MessageEntity::class,
        ChatParticipantEntity::class,
        TypingIndicatorEntity::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class FlowBoardDatabase : RoomDatabase() {

    abstract fun taskDao(): TaskDao
    abstract fun userDao(): UserDao
    abstract fun projectDao(): ProjectDao
    abstract fun notificationDao(): com.flowboard.data.local.dao.NotificationDao
    abstract fun chatDao(): com.flowboard.data.local.dao.ChatDao
    
    companion object {
        const val DATABASE_NAME = "flowboard_database"
        
        @Volatile
        private var INSTANCE: FlowBoardDatabase? = null
        
        fun getDatabase(context: Context): FlowBoardDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FlowBoardDatabase::class.java,
                    DATABASE_NAME
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}