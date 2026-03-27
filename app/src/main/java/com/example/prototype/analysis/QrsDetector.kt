package com.example.prototype.analysis

import android.util.Log
import com.example.prototype.database.EcgPoint

class QrsDetector {

    fun detectQrs(
        ecgPoints: List<EcgPoint>,
        peak: RPeak,
        baselineWindow: Int = 30,   // увеличено
        searchWindow: Int = 80       // увеличено
    ): QrsResult? {
        val idx = peak.refinedIndex
        if (idx < 0 || idx >= ecgPoints.size) {
            Log.d("QrsDetector", "Invalid idx: $idx, size=${ecgPoints.size}")
            return null
        }

        // 1. Оцениваем изолинию
        val baselineStart = maxOf(0, idx - baselineWindow)
        val baselineEnd = maxOf(0, idx - 5) // заканчиваем за 5 точек до R
        if (baselineEnd <= baselineStart) {
            Log.d("QrsDetector", "Baseline window empty: start=$baselineStart, end=$baselineEnd")
            return null
        }
        val baselineValues = ecgPoints.subList(baselineStart, baselineEnd).map { it.filteredValue }
        val baseline = baselineValues.average().toInt()
        Log.d("QrsDetector", "Baseline at idx=$idx: $baseline")

        // 2. Ищем начало QRS
        var startIdx = idx
        for (i in idx downTo maxOf(0, idx - searchWindow)) {
            if (ecgPoints[i].filteredValue <= baseline) {
                startIdx = i
                break
            }
        }

        // 3. Ищем конец QRS
        var endIdx = idx
        for (i in idx .. minOf(ecgPoints.lastIndex, idx + searchWindow)) {
            if (ecgPoints[i].filteredValue <= baseline) {
                endIdx = i
                break
            }
        }

        val widthPoints = endIdx - startIdx
        val widthMs = widthPoints * 20

        Log.d("QrsDetector", "Peak idx=$idx: start=$startIdx, end=$endIdx, widthPoints=$widthPoints, widthMs=$widthMs")

        if (widthPoints < 3 || widthPoints > 30) { // верхний порог увеличен
            Log.d("QrsDetector", "Width out of range, rejecting")
            return null
        }

        return QrsResult(startIdx, endIdx, widthMs)
    }

    data class QrsResult(
        val startIndex: Int,
        val endIndex: Int,
        val widthMs: Int
    )
}