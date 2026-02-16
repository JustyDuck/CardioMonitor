package com.example.prototype.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Session::class, EcgPoint::class, HeartRate::class, WeatherData::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
    abstract fun ecgPointDao(): EcgPointDao
    abstract fun heartRateDao(): HeartRateDao
    abstract fun weatherDataDao(): WeatherDataDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ekg_monitor.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}