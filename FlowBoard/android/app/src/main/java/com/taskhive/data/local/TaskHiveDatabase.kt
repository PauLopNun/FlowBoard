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

@Database(
    entities = [
        TaskEntity::class,
        UserEntity::class,
        ProjectEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class FlowBoardDatabase : RoomDatabase() {
    
    abstract fun taskDao(): TaskDao
    abstract fun userDao(): UserDao
    abstract fun projectDao(): ProjectDao
    
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
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}