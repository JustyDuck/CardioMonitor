package com.example.prototype.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        Session::class, EcgPoint::class, HeartRate::class, WeatherData::class,
        RRInterval::class, QrsComplex::class, WaveAnnotation::class,
        SessionAnalysis::class, Annotation::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
    abstract fun ecgPointDao(): EcgPointDao
    abstract fun heartRateDao(): HeartRateDao
    abstract fun weatherDataDao(): WeatherDataDao

    abstract fun rrIntervalDao(): RRIntervalDao
    abstract fun qrsComplexDao(): QrsComplexDao
    abstract fun waveAnnotationDao(): WaveAnnotationDao
    abstract fun sessionAnalysisDao(): SessionAnalysisDao
    abstract fun annotationDao(): AnnotationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ekg_monitor.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}