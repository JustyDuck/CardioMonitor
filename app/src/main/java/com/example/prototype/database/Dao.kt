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

    @Query("SELECT * FROM sessions WHERE id = :id")
    fun getSessionById(id: Long): Flow<Session>
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

@Dao
interface RRIntervalDao {
    @Insert
    suspend fun insert(interval: RRInterval)

    @Insert
    suspend fun insertAll(intervals: List<RRInterval>)

    @Query("SELECT * FROM rr_intervals WHERE sessionId = :sessionId ORDER BY `index` ASC")
    fun getForSession(sessionId: Long): Flow<List<RRInterval>>
}

@Dao
interface QrsComplexDao {
    @Insert
    suspend fun insert(complex: QrsComplex)

    @Insert
    suspend fun insertAll(complexes: List<QrsComplex>)

    @Query("SELECT * FROM qrs_complexes WHERE sessionId = :sessionId ORDER BY `index` ASC")
    fun getForSession(sessionId: Long): Flow<List<QrsComplex>>
}

@Dao
interface WaveAnnotationDao {
    @Insert
    suspend fun insert(annotation: WaveAnnotation)

    @Insert
    suspend fun insertAll(annotations: List<WaveAnnotation>)

    @Query("SELECT * FROM wave_annotations WHERE sessionId = :sessionId ORDER BY complexIndex ASC, type ASC")
    fun getForSession(sessionId: Long): Flow<List<WaveAnnotation>>
}

@Dao
interface SessionAnalysisDao {
    @Insert
    suspend fun insert(analysis: SessionAnalysis)

    @Update
    suspend fun update(analysis: SessionAnalysis)

    @Query("SELECT * FROM session_analysis WHERE sessionId = :sessionId")
    fun getForSession(sessionId: Long): Flow<SessionAnalysis?>

    @Query("SELECT * FROM session_analysis WHERE sessionId IN (:sessionIds)")
    fun getForSessions(sessionIds: List<Long>): Flow<List<SessionAnalysis>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(analysis: SessionAnalysis)
}

@Dao
interface AnnotationDao {
    @Insert
    suspend fun insert(annotation: Annotation)

    @Insert
    suspend fun insertAll(annotations: List<Annotation>)

    @Query("SELECT * FROM annotations WHERE sessionId = :sessionId ORDER BY startIndex ASC")
    fun getForSession(sessionId: Long): Flow<List<Annotation>>
}




