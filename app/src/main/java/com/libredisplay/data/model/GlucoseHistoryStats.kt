package com.libredisplay.data.model

data class GlucoseHistoryStats(
    val min: Int? = null,
    val max: Int? = null,
    val timeInRangePercent: Int = 0,
    val lowPercent: Int = 0,
    val inRangePercent: Int = 0,
    val highPercent: Int = 0
) {
    companion object {
        fun from(
            points: List<GlucoseHistoryPoint>,
            targetLow: Int,
            targetHigh: Int
        ): GlucoseHistoryStats {
            if (points.isEmpty()) return GlucoseHistoryStats()
            val low = points.count { it.value < targetLow }
            val high = points.count { it.value > targetHigh }
            val inRange = points.size - low - high
            fun percent(count: Int): Int = ((count.toDouble() / points.size.toDouble()) * 100.0).toInt()
            return GlucoseHistoryStats(
                min = points.minOf { it.value },
                max = points.maxOf { it.value },
                timeInRangePercent = percent(inRange),
                lowPercent = percent(low),
                inRangePercent = percent(inRange),
                highPercent = percent(high)
            )
        }
    }
}

