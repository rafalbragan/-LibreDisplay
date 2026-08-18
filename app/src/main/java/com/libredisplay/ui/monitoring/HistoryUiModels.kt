package com.libredisplay.ui.monitoring

import androidx.compose.ui.graphics.Color
import com.libredisplay.analytics.GlucoseMetricsCalculator
import com.libredisplay.analytics.RangeDistribution
import com.libredisplay.data.model.GlucoseHistoryPoint
import com.libredisplay.data.model.GlucoseTrend
import com.libredisplay.ui.theme.LibreCareColors
import java.time.Duration

internal data class TrendPresentation(
    val label: String,
    val arrow: String,
    val color: Color
)

internal data class HistoryLegendRowUi(
    val label: String,
    val threshold: String,
    val durationLabel: String,
    val percentLabel: String,
    val color: Color,
    val duration: Duration,
    val percent: Int,
    val hasData: Boolean
)

internal data class HistoryStatCardUi(
    val label: String,
    val value: String,
    val supportingText: String,
    val accent: Color,
    val hasData: Boolean
)

internal data class HistoryStatsSectionUi(
    val title: String,
    val cards: List<HistoryStatCardUi>
)

internal data class HistoryEventUi(
    val category: String,
    val title: String,
    val timeLabel: String
)

internal fun trendPresentation(trend: GlucoseTrend): TrendPresentation = when (trend) {
    GlucoseTrend.RISING_FAST -> TrendPresentation("Szybko rośnie", trend.arrow, LibreCareColors.AccentRed)
    GlucoseTrend.RISING -> TrendPresentation("Rośnie", trend.arrow, LibreCareColors.AccentAmber)
    GlucoseTrend.FLAT -> TrendPresentation("Stabilnie", trend.arrow, LibreCareColors.AccentTeal)
    GlucoseTrend.FALLING -> TrendPresentation("Spada", trend.arrow, LibreCareColors.AccentBlue)
    GlucoseTrend.FALLING_FAST -> TrendPresentation("Szybko spada", trend.arrow, LibreCareColors.AccentPurple)
    GlucoseTrend.UNKNOWN -> TrendPresentation("Nieznany", trend.arrow, LibreCareColors.TextSecondary)
}

internal fun historyLegendRows(
    history: List<GlucoseHistoryPoint>,
    targetLow: Int,
    targetHigh: Int
): List<HistoryLegendRowUi> {
    if (history.size < 2) {
        return listOf(
            HistoryLegendRowUi("Bardzo wysoka", ">250 mg/dL", "brak danych", "—", LibreCareColors.AccentPurple, Duration.ZERO, 0, false),
            HistoryLegendRowUi("Wysoka", "181-250 mg/dL", "brak danych", "—", LibreCareColors.AccentAmber, Duration.ZERO, 0, false),
            HistoryLegendRowUi("W zakresie", "$targetLow-$targetHigh mg/dL", "brak danych", "—", LibreCareColors.AccentGreen, Duration.ZERO, 0, false),
            HistoryLegendRowUi("Niska", "54-69 mg/dL", "brak danych", "—", LibreCareColors.AccentRed, Duration.ZERO, 0, false),
            HistoryLegendRowUi("Bardzo niska", "<54 mg/dL", "brak danych", "—", LibreCareColors.AccentRed, Duration.ZERO, 0, false)
        )
    }

    val distribution = GlucoseMetricsCalculator.calculateRangeDistribution(
        readings = history,
        targetLow = targetLow,
        targetHigh = targetHigh,
        lowCritical = 54,
        highCritical = 250
    )
    val totalCovered = distribution.belowRangeDuration + distribution.inRangeDuration + distribution.aboveRangeDuration
    fun durationFromPercent(percent: Int): Duration {
        if (percent <= 0 || totalCovered.isZero) return Duration.ZERO
        return Duration.ofMillis((totalCovered.toMillis() * (percent.toDouble() / 100.0)).toLong())
    }

    val veryLowDuration = durationFromPercent(distribution.belowCriticalPercent)
    val lowDuration = (distribution.belowRangeDuration - veryLowDuration).coerceAtLeast(Duration.ZERO)
    val veryHighDuration = durationFromPercent(distribution.aboveVeryHighPercent)
    val highDuration = (distribution.aboveRangeDuration - veryHighDuration).coerceAtLeast(Duration.ZERO)
    val lowPercent = (distribution.belowRangePercent - distribution.belowCriticalPercent).coerceAtLeast(0)
    val highPercent = (distribution.aboveRangePercent - distribution.aboveVeryHighPercent).coerceAtLeast(0)

    return listOf(
        HistoryLegendRowUi("Bardzo wysoka", ">250 mg/dL", PolishDateTimeFormatter.formatRangeTileDuration(veryHighDuration, true), "${distribution.aboveVeryHighPercent}%", LibreCareColors.AccentPurple, veryHighDuration, distribution.aboveVeryHighPercent, true),
        HistoryLegendRowUi("Wysoka", "181-250 mg/dL", PolishDateTimeFormatter.formatRangeTileDuration(highDuration, true), "${highPercent}%", LibreCareColors.AccentAmber, highDuration, highPercent, true),
        HistoryLegendRowUi("W zakresie", "$targetLow-$targetHigh mg/dL", PolishDateTimeFormatter.formatRangeTileDuration(distribution.inRangeDuration, true), "${distribution.inRangePercent}%", LibreCareColors.AccentGreen, distribution.inRangeDuration, distribution.inRangePercent, true),
        HistoryLegendRowUi("Niska", "54-69 mg/dL", PolishDateTimeFormatter.formatRangeTileDuration(lowDuration, true), "${lowPercent}%", LibreCareColors.AccentRed, lowDuration, lowPercent, true),
        HistoryLegendRowUi("Bardzo niska", "<54 mg/dL", PolishDateTimeFormatter.formatRangeTileDuration(veryLowDuration, true), "${distribution.belowCriticalPercent}%", LibreCareColors.AccentRed, veryLowDuration, distribution.belowCriticalPercent, true)
    )
}

internal fun historyStatsSection(
    history: List<GlucoseHistoryPoint>,
    rangeLabel: String,
    targetLow: Int,
    targetHigh: Int
): HistoryStatsSectionUi {
    val title = "Statystyki - $rangeLabel"
    if (history.size < 2) {
        return HistoryStatsSectionUi(
            title = title,
            cards = listOf(
                HistoryStatCardUi("Średnia", "mało danych", "mg/dL", LibreCareColors.TextSecondary, false),
                HistoryStatCardUi("GMI", "mało danych", "szacunek", LibreCareColors.TextSecondary, false),
                HistoryStatCardUi("CV", "mało danych", "zmienność", LibreCareColors.TextSecondary, false),
                HistoryStatCardUi("Czas w zakresie", "brak danych", "TIR", LibreCareColors.TextSecondary, false)
            )
        )
    }

    val average = history.map { it.value }.average()
    val gmi = average.takeIf { it.isFinite() }?.let(GlucoseMetricsCalculator::calculateGmi)
    val tir = GlucoseMetricsCalculator.calculateRangeDistribution(
        readings = history,
        targetLow = targetLow,
        targetHigh = targetHigh,
        lowCritical = 54,
        highCritical = 250
    )
    val mean = average
    val variance = history.map { (it.value - mean) * (it.value - mean) }.average()
    val sd = kotlin.math.sqrt(variance)
    val cv = if (mean > 0.0 && mean.isFinite()) (sd / mean) * 100.0 else Double.NaN

    return HistoryStatsSectionUi(
        title = title,
        cards = listOf(
            HistoryStatCardUi("Średnia", if (average.isFinite()) "${average.toInt()} mg/dL" else "mało danych", "glukoza", LibreCareColors.AccentBlue, average.isFinite()),
            HistoryStatCardUi("GMI", gmi?.let { "${"%.1f".format(it).replace('.', ',')}%" } ?: "mało danych", "szacunek", LibreCareColors.AccentTeal, gmi != null),
            HistoryStatCardUi("CV", if (cv.isFinite()) "${"%.0f".format(cv)}%" else "mało danych", "zmienność", LibreCareColors.AccentPurple, cv.isFinite()),
            HistoryStatCardUi("Czas w zakresie", PolishDateTimeFormatter.formatRangeTileDuration(tir.inRangeDuration, true), "${tir.inRangePercent}%", LibreCareColors.AccentGreen, true)
        )
    )
}

internal fun placeholderHistoryEvents(): List<HistoryEventUi> = listOf(
    HistoryEventUi(category = "Wysoka glikemia", title = "Dodaj notatkę o wysokiej glikemii", timeLabel = "Dzisiaj 18:36"),
    HistoryEventUi(category = "Lekarz", title = "Miejsce na wpis po konsultacji", timeLabel = "Wczoraj 14:10"),
    HistoryEventUi(category = "Posiłek", title = "Miejsce na notatkę o posiłku", timeLabel = "Wczoraj 12:30")
)

