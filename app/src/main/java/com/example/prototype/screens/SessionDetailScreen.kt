package com.example.prototype.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.prototype.components.ZoomableEcgGraph
import com.example.prototype.database.HeartRate
import com.example.prototype.database.WeatherData
import com.example.prototype.viewmodels.EcgViewModel

@Composable
fun SessionDetailScreen(
    sessionId: Long,
    viewModel: EcgViewModel = viewModel()
) {
    val ecgPoints by viewModel.getEcgPointsForSession(sessionId).collectAsState(initial = emptyList())
    val heartRates by viewModel.getHeartRatesForSession(sessionId).collectAsState(initial = emptyList())
    val weatherList by viewModel.getWeatherForSession(sessionId).collectAsState(initial = emptyList())

    // Если данных нет, показываем заглушку
    if (ecgPoints.isEmpty()) {
        Text("Нет данных для этого сеанса", modifier = Modifier.padding(16.dp))
        return
    }

    // Преобразуем EcgPoint в список значений для графика (используем filteredValue)
    val ecgValues = ecgPoints.map { it.filteredValue }
    val timestamps = ecgPoints.map { it.timestamp }

    Column(modifier = Modifier.fillMaxSize()) {
        // Зона графика (занимает 60% высоты)
        Box(modifier = Modifier.weight(3f)) {
            ZoomableEcgGraph(
                ecgValues = ecgValues,
                timestamps = timestamps,
                modifier = Modifier.fillMaxSize()
            )
        }
        // Зона метеоданных (20% высоты)
        Box(modifier = Modifier.weight(1f)) {
            WeatherDetailPanel(weatherList)
        }
        // Зона анализа (20% высоты)
        Box(modifier = Modifier.weight(1f)) {
            AnalysisPanel(heartRates)
        }
    }
}

@Composable
fun WeatherDetailPanel(weatherList: List<WeatherData>) {
    // Пока просто отображаем последние метеоданные
    val lastWeather = weatherList.lastOrNull()
    Text(
        text = if (lastWeather != null) {
            "Температура: ${lastWeather.temperature}°C\n" +
                    "Влажность: ${lastWeather.humidity}%\n" +
                    "Давление: ${lastWeather.pressure} hPa"
        } else {
            "Нет метеоданных"
        },
        modifier = Modifier.padding(8.dp)
    )
}

@Composable
fun AnalysisPanel(heartRates: List<HeartRate>) {
    // Заглушка для будущего анализа
    val avgHr = if (heartRates.isNotEmpty()) {
        heartRates.map { it.value }.average().toInt()
    } else 0
    Text(
        text = "Средний пульс: $avgHr уд/мин\n" +
                "Всего точек пульса: ${heartRates.size}",
        modifier = Modifier.padding(8.dp)
    )
}