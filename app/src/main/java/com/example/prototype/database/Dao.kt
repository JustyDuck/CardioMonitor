package com.example.prototype.database
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Insert
    suspend fun insertSession(session: Session): Long

    @Update
    suspend fun updateSession(session: Session)

    @Query("SELECT * FROM sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<Session>>

    @Query("UPDATE sessions SET endTime = :endTime WHERE id = :sessionId")
    suspend fun updateSessionEndTime(sessionId: Long, endTime: Long)
}


@Dao
interface EcgPointDao {
    @Insert
    suspend fun insertEcgPoint(point: EcgPoint)

    @Insert
    suspend fun insertAllEcgPoints(points: List<EcgPoint>)

    @Query("SELECT * FROM ecg_points WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getEcgPointsForSession(sessionId: Long): Flow<List<EcgPoint>>
}


@Dao
interface HeartRateDao {
    @Insert
    suspend fun insertHeartRate(hr: HeartRate)

    @Query("SELECT * FROM heart_rates WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getHeartRatesForSession(sessionId: Long): Flow<List<HeartRate>>
}

@Dao
interface WeatherDataDao {
    @Insert
    suspend fun insertWeatherData(weather: WeatherData)

    @Query("SELECT * FROM weather_data WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getWeatherForSession(sessionId: Long): Flow<List<WeatherData>>
}

