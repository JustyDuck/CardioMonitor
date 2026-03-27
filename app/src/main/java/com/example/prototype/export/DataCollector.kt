package com.example.prototype.export

import com.example.prototype.database.EcgPoint
import com.example.prototype.database.HeartRate
import com.example.prototype.database.WeatherData
import com.example.prototype.database.Repository
import kotlinx.coroutines.flow.first


class DataCollector(private val repository: Repository) {


    suspend fun collect(sessionId: Long): List<RowData> {
        // Получаем все данные для сеанса
        val ecgPoints = repository.getEcgPointsForSession(sessionId).first()
        val heartRates = repository.getHeartRatesForSession(sessionId).first()
        val weatherList = repository.getWeatherForSession(sessionId).first()

        if (ecgPoints.isEmpty()) return emptyList()

        // Сортируем по времени (по возрастанию)
        val sortedEcg = ecgPoints.sortedBy { it.timestamp }
        val sortedHr = heartRates.sortedBy { it.timestamp }
        val sortedWeather = weatherList.sortedBy { it.timestamp }

        val startTime = sortedEcg.first().timestamp // время начала сеанса (абсолютное)

        var hrIndex = 0
        var weatherIndex = 0
        val result = mutableListOf<RowData>()

        for ((index, ecg) in sortedEcg.withIndex()) {

            // Ищем последний пульс, не превышающий время ecg
            while (hrIndex < sortedHr.size - 1 && sortedHr[hrIndex + 1].timestamp <= ecg.timestamp) {
                hrIndex++
            }
            val currentHr = if (sortedHr.isNotEmpty() && sortedHr[hrIndex].timestamp <= ecg.timestamp)
                sortedHr[hrIndex].value else null

            // Ищем последние метеоданные
            while (weatherIndex < sortedWeather.size - 1 && sortedWeather[weatherIndex + 1].timestamp <= ecg.timestamp) {
                weatherIndex++
            }
            val currentWeather = if (sortedWeather.isNotEmpty() && sortedWeather[weatherIndex].timestamp <= ecg.timestamp)
                sortedWeather[weatherIndex] else null

            val timeSec = index * 0.02 // шаг 20 мс

            result.add(
                RowData(
                    timeSec = timeSec,
                    raw = ecg.rawValue,
                    filtered = ecg.filteredValue,
                    qrs = ecg.qrsPoint,
                    hr = currentHr,
                    temp = currentWeather?.temperature,
                    hum = currentWeather?.humidity,
                    pres = currentWeather?.pressure
                )
            )
        }

        return result
    }
}