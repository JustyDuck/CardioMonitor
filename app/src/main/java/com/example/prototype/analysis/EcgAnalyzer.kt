package com.example.prototype.analysis

import android.util.Log
import com.example.prototype.database.*
import kotlinx.coroutines.flow.first

class EcgAnalyzer(private val repository: Repository) {

    suspend fun analyze(sessionId: Long) {
        val ecgPoints = repository.getEcgPointsForSession(sessionId).first()
        if (ecgPoints.isEmpty()) {
            Log.d("EcgAnalyzer", "No ECG points for session $sessionId")
            // Сохраняем пустой анализ
            val emptyAnalysis = SessionAnalysis(
                sessionId = sessionId,
                avgHr = 0,
                sdnn = 0,
                rmssd = 0,
                pnn50 = 0,
                minHr = 0,
                maxHr = 0,
                bradycardiaEpisodes = 0,
                tachycardiaEpisodes = 0,
                prematureCount = 0,
                atrialFibrillation = false,
                avgQrsWidth = null,
                overallQuality = 0,
                analysisDate = System.currentTimeMillis()
            )
            repository.saveSessionAnalysis(emptyAnalysis)
            return
        }

        // Найти уточнённые R-пики
        val peakDetector = RPeakDetector()
        val peaks = peakDetector.detectPeaks(ecgPoints)
        if (peaks.size < 2) {
            Log.d("EcgAnalyzer", "Not enough peaks to compute RR intervals")
            // Сохраняем пустой анализ
            val emptyAnalysis = SessionAnalysis(
                sessionId = sessionId,
                avgHr = 0,
                sdnn = 0,
                rmssd = 0,
                pnn50 = 0,
                minHr = 0,
                maxHr = 0,
                bradycardiaEpisodes = 0,
                tachycardiaEpisodes = 0,
                prematureCount = 0,
                atrialFibrillation = false,
                avgQrsWidth = null,
                overallQuality = 0,
                analysisDate = System.currentTimeMillis()
            )
            repository.saveSessionAnalysis(emptyAnalysis)
            return
        }

        // Вычисляем RR-интервалы
        val rrCalculator = RRIntervalCalculator()
        val rrIntervals = rrCalculator.calculateIntervals(peaks, sessionId)
        repository.saveRRIntervals(rrIntervals)

        // Расчёт ВСР на качественных интервалах
        val goodIntervals = rrIntervals.filter { it.quality == 1 }
        val hrv = HrvCalculator().calculateHrv(goodIntervals)
        val minHr = goodIntervals.map { 60000 / it.rrMs }.minOrNull() ?: 0
        val maxHr = goodIntervals.map { 60000 / it.rrMs }.maxOrNull() ?: 0

        // Расчёт ширины QRS
        val qrsDetector = QrsDetector()
        val qrsWidths = mutableListOf<Int>()
        for (peak in peaks) {
            val qrsResult = qrsDetector.detectQrs(ecgPoints, peak)
            if (qrsResult != null) {
                qrsWidths.add(qrsResult.widthMs)
            }
        }
        val avgQrsWidth = if (qrsWidths.isNotEmpty()) qrsWidths.average().toInt() else null

        // Заглушки для аритмий
        val bradyEpisodes = 0
        val tachyEpisodes = 0
        val prematureCount = 0
        val atrialFibrillation = false

        // Сохраняем анализ
        val analysis = SessionAnalysis(
            sessionId = sessionId,
            avgHr = hrv.avgHr,
            sdnn = hrv.sdnn,
            rmssd = hrv.rmssd,
            pnn50 = hrv.pnn50,
            minHr = minHr,
            maxHr = maxHr,
            bradycardiaEpisodes = bradyEpisodes,
            tachycardiaEpisodes = tachyEpisodes,
            prematureCount = prematureCount,
            atrialFibrillation = atrialFibrillation,
            avgQrsWidth = avgQrsWidth,
            overallQuality = if (rrIntervals.isNotEmpty()) (goodIntervals.size * 100 / rrIntervals.size) else 0,
            analysisDate = System.currentTimeMillis()
        )
        repository.saveSessionAnalysis(analysis)
        Log.d("EcgAnalyzer", "Analysis saved for session $sessionId")
    }
}