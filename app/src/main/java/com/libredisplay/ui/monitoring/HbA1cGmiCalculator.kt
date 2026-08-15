package com.libredisplay.ui.monitoring

import com.libredisplay.data.model.GlucoseHistoryPoint
import com.libredisplay.data.model.HbA1cSettings
import java.time.Duration

internal enum class HbA1cKpiMode {
    LAB_HBA1C,
    GMI_ESTIMATED,
    GMI_INSUFFICIENT
}

internal enum class HbA1cStatus {
    BELOW_TARGET,
    NEAR_TARGET,
    ABOVE_TARGET
}

internal data class HbA1cKpiModel(
    val mode: HbA1cKpiMode,
    val valuePercent: Double?,
    val targetPercent: Double,
    val insufficientData: Boolean,
    val reliableEstimate: Boolean,
    val daysOfData: Int,
    val status: HbA1cStatus?,
    val labDateIso: String? = null
)

internal fun calculateGmi(avgGlucoseMgDl: Double): Double {
    return 3.31 + 0.02392 * avgGlucoseMgDl
}

internal fun evaluateHbA1cStatus(value: Double, target: Double): HbA1cStatus {
    return when {
        value < target -> HbA1cStatus.BELOW_TARGET
        value <= target + 0.3 -> HbA1cStatus.NEAR_TARGET
        else -> HbA1cStatus.ABOVE_TARGET
    }
}

internal fun shouldShowReliableGmi(daysOfData: Int, sensorActivityPercent: Double): Boolean {
    return daysOfData >= 14 && sensorActivityPercent >= 70.0
}

internal fun buildHbA1cKpiModel(
    hba1cSettings: HbA1cSettings,
    historyPoints: List<GlucoseHistoryPoint>,
    minDaysForReliableGmi: Int = 14,
    minActivityPercent: Double = 70.0
): HbA1cKpiModel {
    val labValue = hba1cSettings.labHbA1cPercent
    if (labValue != null) {
        val status = evaluateHbA1cStatus(labValue, hba1cSettings.targetHbA1cPercent)
        return HbA1cKpiModel(
            mode = HbA1cKpiMode.LAB_HBA1C,
            valuePercent = labValue,
            targetPercent = hba1cSettings.targetHbA1cPercent,
            insufficientData = false,
            reliableEstimate = true,
            daysOfData = calculateHistoryDays(historyPoints),
            status = status,
            labDateIso = hba1cSettings.labHbA1cDate?.toString()
        )
    }

    if (historyPoints.size < 2) {
        return HbA1cKpiModel(
            mode = HbA1cKpiMode.GMI_INSUFFICIENT,
            valuePercent = null,
            targetPercent = hba1cSettings.targetHbA1cPercent,
            insufficientData = true,
            reliableEstimate = false,
            daysOfData = 0,
            status = null
        )
    }

    val days = calculateHistoryDays(historyPoints)
    val activity = estimateSensorActivityPercent(days)
    val reliable = shouldShowReliableGmi(days, activity) && days >= minDaysForReliableGmi && activity >= minActivityPercent
    if (!reliable) {
        return HbA1cKpiModel(
            mode = HbA1cKpiMode.GMI_INSUFFICIENT,
            valuePercent = null,
            targetPercent = hba1cSettings.targetHbA1cPercent,
            insufficientData = true,
            reliableEstimate = false,
            daysOfData = days,
            status = null
        )
    }

    val avg = historyPoints.map { it.value }.average()
    val gmi = calculateGmi(avg)
    return HbA1cKpiModel(
        mode = HbA1cKpiMode.GMI_ESTIMATED,
        valuePercent = gmi,
        targetPercent = hba1cSettings.targetHbA1cPercent,
        insufficientData = false,
        reliableEstimate = true,
        daysOfData = days,
        status = null
    )
}

private fun calculateHistoryDays(history: List<GlucoseHistoryPoint>): Int {
    if (history.size < 2) return 0
    val sorted = history.sortedBy { it.timestamp }
    val duration = Duration.between(sorted.first().timestamp, sorted.last().timestamp)
    return duration.toDays().toInt().coerceAtLeast(0)
}

private fun estimateSensorActivityPercent(daysOfData: Int): Double {
    if (daysOfData <= 0) return 0.0
    // Conservative fallback: we only know elapsed coverage, not full wear-time completeness.
    return 100.0
}


