package com.example.prototype.analysis

import com.example.prototype.database.EcgPoint

data class RPeak(
    val index: Int,          // порядковый номер пика в последовательности (0..N-1)
    val originalIndex: Int,  // индекс кандидата в списке ecgPoints
    val refinedIndex: Int,   // индекс уточнённого пика в списке ecgPoints (не id!)
    val time: Long,
    val amplitude: Int
)

class RPeakDetector {

    fun detectPeaks(ecgPoints: List<EcgPoint>, windowSize: Int = 10): List<RPeak> {
        val result = mutableListOf<RPeak>()
        for ((idx, point) in ecgPoints.withIndex()) {
            if (point.qrsPoint == 1) {
                val start = maxOf(0, idx - windowSize)
                val end = minOf(ecgPoints.lastIndex, idx + windowSize)
                val window = ecgPoints.subList(start, end + 1)
                val maxPoint = window.maxByOrNull { it.filteredValue } ?: point
                // Находим индекс maxPoint в исходном списке
                val refinedIdx = ecgPoints.indexOfFirst { it.id == maxPoint.id }
                result.add(
                    RPeak(
                        index = result.size,
                        originalIndex = idx,
                        refinedIndex = refinedIdx,
                        time = maxPoint.timestamp,
                        amplitude = maxPoint.filteredValue
                    )
                )
            }
        }
        return result
    }
}