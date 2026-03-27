package com.example.prototype.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.prototype.components.ZoomableEcgGraph
import com.example.prototype.database.HeartRate
import com.example.prototype.database.SessionAnalysis
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
    val analysis by viewModel.getSessionAnalysis(sessionId).collectAsState(initial = null)

    if (ecgPoints.isEmpty()) {
        Text("Нет данных для этого сеанса", modifier = Modifier.padding(16.dp))
        return
    }

    val ecgValues = ecgPoints.map { it.filteredValue }
    val timestamps = ecgPoints.indices.map { it.toLong() * 20 }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        ZoomableEcgGraph(
            ecgValues = ecgValues,
            timestamps = timestamps,
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
        )
        WeatherDetailPanel(weatherList, modifier = Modifier.fillMaxWidth())
        AnalysisPanel(
            heartRates = heartRates,
            analysis = analysis,
            onReanalyze = { viewModel.reanalyzeSession(sessionId) }
        )
    }
}

@Composable
fun WeatherDetailPanel(weatherList: List<WeatherData>, modifier: Modifier = Modifier) {
    if (weatherList.isEmpty()) {
        Text("Нет метеоданных", modifier = modifier.padding(8.dp))
        return
    }

    val avgTemp = weatherList.map { it.temperature }.average()
    val avgHum = weatherList.map { it.humidity }.average()
    val avgPres = weatherList.map { it.pressure }.average()
    val lastWeather = weatherList.lastOrNull()

    Column(modifier = modifier.padding(8.dp)) {
        Text("Средние метеоданные:", fontWeight = FontWeight.Bold)
        Text("Температура: ${"%.1f".format(avgTemp)}°C")
        Text("Влажность: ${"%.1f".format(avgHum)}%")
        Text("Давление: ${"%.1f".format(avgPres)} hPa")
        Spacer(modifier = Modifier.height(4.dp))
//        if (lastWeather != null) {
//            Text("Последние:", fontWeight = FontWeight.Bold)
//            Text("Температура: ${lastWeather.temperature}°C")
//            Text("Влажность: ${lastWeather.humidity}%")
//            Text("Давление: ${lastWeather.pressure} hPa")
//        }
    }
}

@Composable
fun AnalysisPanel(heartRates: List<HeartRate>, analysis: SessionAnalysis?) {
    if (analysis == null) {
        Text("Анализ выполняется...", modifier = Modifier.padding(8.dp))
        return
    }
    Column(modifier = Modifier.padding(8.dp)) {
        Text("Средний пульс: ${analysis.avgHr} уд/мин")
        Text("SDNN: ${analysis.sdnn} мс")
        Text("RMSSD: ${analysis.rmssd} мс")
        Text("pNN50: ${analysis.pnn50}%")
        Text("Мин. пульс: ${analysis.minHr} / Макс. пульс: ${analysis.maxHr}")
        Text("Качество сигнала: ${analysis.overallQuality}%")
        analysis.avgQrsWidth?.let { Text("Средняя ширина QRS: $it мс") }
    }
}



@Composable
fun AnalysisPanel(
    heartRates: List<HeartRate>,
    analysis: SessionAnalysis?,
    onReanalyze: () -> Unit
) {
    if (analysis == null) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text("Анализ выполняется...")
            Button(onClick = onReanalyze) {
                Text("Пересчитать")
            }
        }
        return
    }
    Column(modifier = Modifier.padding(8.dp)) {
        Text("Средний пульс: ${analysis.avgHr} уд/мин")
        Text("SDNN: ${analysis.sdnn} мс")
        Text("RMSSD: ${analysis.rmssd} мс")
        Text("pNN50: ${analysis.pnn50}%")
        Text("Мин. пульс: ${analysis.minHr} / Макс. пульс: ${analysis.maxHr}")
        Text("Качество сигнала: ${analysis.overallQuality}%")
        analysis.avgQrsWidth?.let { Text("Средняя ширина QRS: $it мс") }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onReanalyze) {
            Text("Пересчитать анализ")
        }
    }
}