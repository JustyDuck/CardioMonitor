package com.example.prototype.analysis

import com.example.prototype.database.RRInterval

class RRIntervalCalculator {

    fun calculateIntervals(peaks: List<RPeak>, sessionId: Long): List<RRInterval> {
        if (peaks.size < 2) return emptyList()

        val intervals = mutableListOf<RRInterval>()
        for (i in 1 until peaks.size) {
            val prev = peaks[i-1]
            val curr = peaks[i]
            val rrMs = (curr.time - prev.time).toInt()
            val quality = if (rrMs in 300..2000) 1 else 0

            intervals.add(
                RRInterval(
                    sessionId = sessionId,
                    index = i - 1,
                    rrMs = rrMs,
                    quality = quality,
                    rPeakIndex = curr.refinedIndex.toLong()
                )
            )
        }
        return intervals
    }
}
