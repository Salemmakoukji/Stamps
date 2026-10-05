package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [DailyStamp::class], version = 2, exportSchema = false)
abstract class StampDatabase : RoomDatabase() {
    abstract fun stampDao(): StampDao

    companion object {
        @Volatile
        private var INSTANCE: StampDatabase? = null

        fun getDatabase(context: Context): StampDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    StampDatabase::class.java,
                    "daily_stamps.db"
                ).fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
