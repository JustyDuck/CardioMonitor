package com.example.prototype.analysis

import com.example.prototype.database.RRInterval
import kotlin.math.pow
import kotlin.math.sqrt

class HrvCalculator {

    data class HrvResult(
        val avgHr: Int,
        val sdnn: Int,
        val rmssd: Int,
        val pnn50: Int
    )

    fun calculateHrv(rrIntervals: List<RRInterval>): HrvResult {
        if (rrIntervals.isEmpty()) return HrvResult(0, 0, 0, 0)

        val values = rrIntervals.map { it.rrMs.toDouble() }
        val avgRR = values.average()
        val avgHr = (60000 / avgRR).toInt()

        val sdnn = sqrt(values.map { (it - avgRR).pow(2) }.average()).toInt()

        val diffs = values.zipWithNext { a, b -> kotlin.math.abs(a - b) }
        val rmssd = sqrt(diffs.map { it * it }.average()).toInt()
        val pnn50 = (diffs.count { it > 50 } * 100 / diffs.size).toInt()

        return HrvResult(avgHr, sdnn, rmssd, pnn50)
    }
}