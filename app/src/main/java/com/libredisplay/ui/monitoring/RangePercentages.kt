package com.libredisplay.ui.monitoring

import com.libredisplay.data.model.GlucoseHistoryPoint

data class RangePercentages(
    val below: Int?,
    val inRange: Int?,
    val above: Int?
)

fun calculateRangePercentages(
    history: List<GlucoseHistoryPoint>,
    targetLow: Int,
    targetHigh: Int
): RangePercentages {
    if (history.isEmpty()) return RangePercentages(null, null, null)

    val total = history.size.toDouble()
    fun pct(count: Int): Int = ((count / total) * 100.0).toInt().coerceIn(0, 100)

    val belowCount = history.count { it.value < targetLow }
    val aboveCount = history.count { it.value > targetHigh }
    val inRangeCount = (history.size - belowCount - aboveCount).coerceAtLeast(0)

    return RangePercentages(
        below = pct(belowCount),
        inRange = pct(inRangeCount),
        above = pct(aboveCount)
    )
}

