package com.example.prototype.database

import android.util.Log
import kotlinx.coroutines.flow.Flow

class Repository(private val db: AppDatabase) {

    // Сеансы
    suspend fun startSession(
        startTime: Long,
        note: String? = null,
        sessionType: String? = null,
        plannedDurationSeconds: Int? = null
        ): Long {
        val session = Session(
            startTime = startTime,
            note = note,
            sessionType = sessionType,
            plannedDurationSeconds = plannedDurationSeconds
            )
        return db.sessionDao().insertSession(session)
    }

    suspend fun finishSession(sessionId: Long, endTime: Long) {
        db.sessionDao().updateSessionEndTime(sessionId, endTime)
    }

    fun getAllSessions(): Flow<List<Session>> = db.sessionDao().getAllSessions()

    // Точки ЭКГ
    suspend fun saveEcgPoint(sessionId: Long, timestamp: Long, rawValue: Int, filteredValue: Int, qrsPoint: Int) {
        try {
            db.ecgPointDao().insertEcgPoint(
                EcgPoint(
                    sessionId = sessionId,
                    timestamp = timestamp,
                    rawValue = rawValue,
                    filteredValue = filteredValue,
                    qrsPoint = qrsPoint
                )
            )
            Log.d("Repository", "Saved ECG point: session=$sessionId, raw=$rawValue")
        } catch (e: Exception) {
            Log.e("Repository", "Error saving ECG point", e)
        }
    }

    // Пульс
    suspend fun saveHeartRate(sessionId: Long, timestamp: Long, value: Int) {
        db.heartRateDao().insertHeartRate(HeartRate(sessionId = sessionId, timestamp = timestamp, value = value))
    }

    // Метеоданные
    suspend fun saveWeatherData(sessionId: Long, timestamp: Long, temp: Float, hum: Float, pres: Float) {
        db.weatherDataDao().insertWeatherData(
            WeatherData(
                sessionId = sessionId,
                timestamp = timestamp,
                temperature = temp,
                humidity = hum,
                pressure = pres
            )
        )
    }

    // Получение данных для сеанса
    fun getEcgPointsForSession(sessionId: Long): Flow<List<EcgPoint>> = db.ecgPointDao().getEcgPointsForSession(sessionId)
    fun getHeartRatesForSession(sessionId: Long): Flow<List<HeartRate>> = db.heartRateDao().getHeartRatesForSession(sessionId)
    fun getWeatherForSession(sessionId: Long): Flow<List<WeatherData>> = db.weatherDataDao().getWeatherForSession(sessionId)

    fun getSessionById(id: Long): Flow<Session> = db.sessionDao().getSessionById(id)

    // RR-интервалы
    suspend fun saveRRIntervals(intervals: List<RRInterval>) {
        db.rrIntervalDao().insertAll(intervals)
    }
    suspend fun getRRIntervals(sessionId: Long): Flow<List<RRInterval>> = db.rrIntervalDao().getForSession(sessionId)

    // QRS-комплексы
    suspend fun saveQrsComplexes(complexes: List<QrsComplex>) {
        db.qrsComplexDao().insertAll(complexes)
    }
    suspend fun getQrsComplexes(sessionId: Long): Flow<List<QrsComplex>> = db.qrsComplexDao().getForSession(sessionId)

    // Волновые аннотации
    suspend fun saveWaveAnnotations(annotations: List<WaveAnnotation>) {
        db.waveAnnotationDao().insertAll(annotations)
    }
    suspend fun getWaveAnnotations(sessionId: Long): Flow<List<WaveAnnotation>> = db.waveAnnotationDao().getForSession(sessionId)

    // Анализ сеанса
    suspend fun saveSessionAnalysis(analysis: SessionAnalysis) {
        db.sessionAnalysisDao().insertOrReplace(analysis)
    }
    suspend fun updateSessionAnalysis(analysis: SessionAnalysis) {
        db.sessionAnalysisDao().update(analysis)
    }
     fun getSessionAnalysis(sessionId: Long): Flow<SessionAnalysis?> = db.sessionAnalysisDao().getForSession(sessionId)

    // Аннотации событий
    suspend fun saveAnnotations(annotations: List<Annotation>) {
        db.annotationDao().insertAll(annotations)
    }
    suspend fun getAnnotations(sessionId: Long): Flow<List<Annotation>> = db.annotationDao().getForSession(sessionId)


}