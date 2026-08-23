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

private enum class GlucoseContext {
    LOW,
    IN_RANGE,
    HIGH
}

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
    val hasData: Boolean,
    val tooltipTitle: String? = null,
    val tooltipExplanation: String? = null,
    val tooltipFormula: String? = null
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
    GlucoseTrend.FLAT -> TrendPresentation("Bez zmian", trend.arrow, LibreCareColors.AccentTeal)
    GlucoseTrend.FALLING -> TrendPresentation("Spada", trend.arrow, LibreCareColors.AccentBlue)
    GlucoseTrend.FALLING_FAST -> TrendPresentation("Szybko spada", trend.arrow, LibreCareColors.AccentPurple)
    GlucoseTrend.UNKNOWN -> TrendPresentation("Nieznany", trend.arrow, LibreCareColors.TextSecondary)
}

internal fun trendPresentation(
    trend: GlucoseTrend,
    glucoseValue: Int,
    targetLow: Int,
    targetHigh: Int
): TrendPresentation {
    val base = trendPresentation(trend)
    val boundaryMargin = 10
    val nearLowBoundary = glucoseValue in targetLow..(targetLow + boundaryMargin)
    val nearHighBoundary = glucoseValue in (targetHigh - boundaryMargin)..targetHigh
    val context = when {
        glucoseValue < targetLow -> GlucoseContext.LOW
        glucoseValue > targetHigh -> GlucoseContext.HIGH
        else -> GlucoseContext.IN_RANGE
    }

    val contextualColor = when (context) {
        GlucoseContext.HIGH -> when (trend) {
            GlucoseTrend.FALLING, GlucoseTrend.FALLING_FAST -> LibreCareColors.AccentGreen
            GlucoseTrend.RISING -> warningToneColor(WarningTone.WARNING)
            GlucoseTrend.RISING_FAST -> warningToneColor(WarningTone.CRITICAL)
            GlucoseTrend.FLAT -> LibreCareColors.AccentAmber
            GlucoseTrend.UNKNOWN -> LibreCareColors.TextSecondary
        }
        GlucoseContext.LOW -> when (trend) {
            GlucoseTrend.RISING, GlucoseTrend.RISING_FAST -> LibreCareColors.AccentAmber
            GlucoseTrend.FALLING -> warningToneColor(WarningTone.WARNING)
            GlucoseTrend.FALLING_FAST -> warningToneColor(WarningTone.CRITICAL)
            GlucoseTrend.FLAT -> LibreCareColors.AccentAmber
            GlucoseTrend.UNKNOWN -> LibreCareColors.TextSecondary
        }
        GlucoseContext.IN_RANGE -> when {
            nearLowBoundary && trend in setOf(GlucoseTrend.FALLING, GlucoseTrend.FALLING_FAST) -> warningToneColor(WarningTone.CAUTION)
            nearHighBoundary && trend in setOf(GlucoseTrend.RISING, GlucoseTrend.RISING_FAST) -> warningToneColor(WarningTone.CAUTION)
            else -> base.color
        }
    }

    return base.copy(color = contextualColor)
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
    targetHigh: Int,
    coverage: DataCoverageModel? = null
): HistoryStatsSectionUi {
    // Use actual available span in title, not selected range
    val title = if (coverage != null && !coverage.hasFullCoverage && !coverage.availableSpan.isZero) {
        "Statystyki · ${coverage.availableSpanLabel} danych"
    } else {
        "Statystyki · $rangeLabel"
    }
     if (history.size < 2) {
         return HistoryStatsSectionUi(
             title = title,
             cards = listOf(
                 HistoryStatCardUi(
                     "GMI", "Brak", "pomiary", LibreCareColors.TextSecondary, false,
                     "Glucose Management Indicator",
                     "Szacunkowe HbA1c na podstawie średniej glukozy z sensora. Wymaga co najmniej 14 dni danych (ok. 96 pomiarów).",
                     "GMI = 3,31 + 0,02392 × średnia glukoza"
                 ),
                 HistoryStatCardUi(
                     "CV", "Brak", "pomiary", LibreCareColors.TextSecondary, false,
                     "Współczynnik zmienności",
                     "Mierzy stabilność poziomu cukru. Niższy CV oznacza bardziej stabilne wartości. Wymaga co najmniej 10 pomiarów.",
                     "CV = (odchylenie std. / średnia) × 100%"
                 ),
                 HistoryStatCardUi(
                     "Czas w zakresie", "Brak", "pomiary", LibreCareColors.TextSecondary, false,
                     "Time In Range",
                     "Procent czasu, gdy poziom glukozy był w docelowym zakresie.",
                     null
                 )
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
             HistoryStatCardUi(
                 "GMI",
                 gmi?.let { "${"%.1f".format(it).replace('.', ',')}%" } ?: "Brak",
                 if (gmi != null) "szacunek" else "wymagane 14 dni",
                 LibreCareColors.AccentTeal,
                 gmi != null,
                 "Glucose Management Indicator",
                 "Szacunkowe HbA1c na podstawie średniej glukozy z sensora. Nie zastępuje badania laboratoryjnego.",
                 "GMI = 3,31 + 0,02392 × średnia glukoza"
             ),
             HistoryStatCardUi(
                 "CV",
                 if (cv.isFinite()) "${"%.0f".format(cv)}%" else "Brak",
                 if (cv.isFinite()) "zmienność" else "wymagane 10+ pomiarów",
                 LibreCareColors.AccentPurple,
                 cv.isFinite(),
                 "Współczynnik zmienności",
                 "Mierzy stabilność poziomu cukru. Niższy CV oznacza bardziej stabilne wartości.",
                 "CV = (odchylenie std. / średnia) × 100%"
             ),
             HistoryStatCardUi(
                 "Czas w zakresie",
                 PolishDateTimeFormatter.formatRangeTileDuration(tir.inRangeDuration, true),
                 "${tir.inRangePercent}%",
                 LibreCareColors.AccentGreen,
                 true,
                 "Time In Range",
                 "Procent czasu, gdy poziom glukozy był w docelowym zakresie (${targetLow}-${targetHigh} mg/dL).",
                 null
             )
         )
     )
}

internal fun placeholderHistoryEvents(): List<HistoryEventUi> = listOf(
    HistoryEventUi(category = "Wysoka glikemia", title = "Dodaj notatkę o wysokiej glikemii", timeLabel = "Dzisiaj 18:36"),
    HistoryEventUi(category = "Lekarz", title = "Miejsce na wpis po konsultacji", timeLabel = "Wczoraj 14:10"),
    HistoryEventUi(category = "Posiłek", title = "Miejsce na notatkę o posiłku", timeLabel = "Wczoraj 12:30")
)

