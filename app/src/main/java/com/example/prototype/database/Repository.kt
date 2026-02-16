package com.example.prototype.database

import kotlinx.coroutines.flow.Flow

class Repository(private val db: AppDatabase) {

    // Сеансы
    suspend fun startSession(startTime: Long, note: String? = null): Long {
        val session = Session(startTime = startTime, note = note)
        return db.sessionDao().insertSession(session)
    }

    suspend fun finishSession(sessionId: Long, endTime: Long) {
        db.sessionDao().updateSessionEndTime(sessionId, endTime)
    }

    fun getAllSessions(): Flow<List<Session>> = db.sessionDao().getAllSessions()

    // Точки ЭКГ
    suspend fun saveEcgPoint(sessionId: Long, timestamp: Long, rawValue: Int, filteredValue: Int) {
        db.ecgPointDao().insertEcgPoint(
            EcgPoint(
                sessionId = sessionId,
                timestamp = timestamp,
                rawValue = rawValue,
                filteredValue = filteredValue
            )
        )
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
}