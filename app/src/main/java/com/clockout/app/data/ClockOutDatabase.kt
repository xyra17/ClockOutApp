package com.clockout.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [WorkDayEntity::class], version = 1, exportSchema = false)
abstract class ClockOutDatabase : RoomDatabase() {
    abstract fun workDayDao(): WorkDayDao

    companion object {
        @Volatile private var instance: ClockOutDatabase? = null
        fun get(context: Context): ClockOutDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                ClockOutDatabase::class.java,
                "clockout.db",
            ).build().also { instance = it }
        }
    }
}
