package com.libredisplay.ui.monitoring

import androidx.compose.ui.geometry.Rect
import com.libredisplay.analytics.RangeDistribution
import com.libredisplay.analytics.SensorActivity
import com.libredisplay.data.model.GlucoseReading
import com.libredisplay.data.model.GlucoseHistoryPoint
import com.libredisplay.data.model.GlucoseTrend
import com.libredisplay.data.model.HbA1cSettings
import com.libredisplay.data.model.LibreConnectionPerson
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

internal typealias GlucosePoint = GlucoseHistoryPoint

internal enum class NfzStatus {
    GREEN,
    YELLOW,
    RED,
    GRAY
}

internal data class DashboardTypography(
    val glucoseValueSp: Int,
    val glucoseUnitSp: Int,
    val trendArrowSp: Int,
    val trendDescriptionSp: Int
)

internal data class ChartPointLabel(
    val dateTime: String,
    val valueText: String,
    val valueLabel: String,
    val timeLabel: String,
    val statusText: String,
    val trendText: String? = null
)

internal data class RangeTileUi(
    val percentLabel: String,
    val durationLabel: String,
    val hasData: Boolean
)

internal data class NfzStatusUi(
    val status: NfzStatus,
    val headline: String,
    val details: String
)

internal data class DashboardMetricTile(
    val label: String,
    val value: String,
    val supportingText: String,
    val hasData: Boolean
)

internal data class TrendWindowSnapshot(
    val points: List<GlucoseHistoryPoint>,
    val windowMinutes: Int,
    val spanMinutes: Double
)

internal data class TrendRateEstimate(
    val mgDlPerMinute: Double,
    val mgDlPer15Minutes: Double,
    val windowMinutes: Int,
    val sampleCount: Int = 0,
    val spanMinutes: Double = 0.0
)

internal data class TrendProjectionThresholds(
    val lowThresholdMgDl: Int,
    val highThresholdMgDl: Int
)

internal data class TrendProjection(
    val rateMgDlPerMinute: Double,
    val thresholdMgDl: Int,
    val minutesToThreshold: Int
)

internal val TrendRateEstimate.derivedTrend: GlucoseTrend
    get() = GlucoseTrend.fromSlope(mgDlPerMinute)

internal sealed interface MonitoringAction {
    data class OpenHistory(val context: HistoryOpenContext) : MonitoringAction
}

internal data class DashboardUiState(
    val selectedPersonHbA1cSettings: HbA1cSettings,
    val hba1cKpi: HbA1cKpiModel,
    val historyPoints: List<GlucoseHistoryPoint>,
    val readingAgeMinutes: Long?,
    val selectedPersonName: String?,
    val selectedPatientId: String?
)

internal enum class PersonSwitcherMode {
    INLINE_CHIPS,
    SCROLLABLE_CHIPS
}

internal data class HistoryOpenContext(
    val patientId: String?,
    val timeRange: TimeRangeState
)

internal fun MonitoringUiState.shouldShowPersonSwitcher(): Boolean = mapSwitcherPersons(availablePersons).size > 1

internal fun shouldShowRetryAction(connectionState: ConnectionState): Boolean = when (connectionState) {
    ConnectionState.Connected,
    ConnectionState.Connecting -> false
    else -> true
}

internal fun mapSwitcherPersons(persons: List<LibreConnectionPerson>): List<LibreConnectionPerson> = persons.take(3)

internal fun personSwitcherModeForCount(personCount: Int): PersonSwitcherMode =
    if (personCount <= 3) PersonSwitcherMode.INLINE_CHIPS else PersonSwitcherMode.SCROLLABLE_CHIPS

internal fun dashboardIdentityOccurrences(showTopBarIdentity: Boolean, showHeaderIdentity: Boolean, showSwitcherIdentity: Boolean): Int {
    var count = 0
    if (showTopBarIdentity) count++
    if (showHeaderIdentity) count++
    if (showSwitcherIdentity) count++
    return count
}

internal fun buildHistoryOpenContext(state: MonitoringUiState): HistoryOpenContext =
    HistoryOpenContext(
        patientId = state.selectedPatientId,
        timeRange = state.timeRange
    )

internal fun compactDashboardRangeLabel(
    timeRange: TimeRangeState,
    latestReadingAt: Instant?,
    zoneId: ZoneId = DateTimeFormatterProvider.deviceZoneId()
): String {
    val rangeLabel = when (timeRange.presetRange) {
        PresetTimeRange.LAST_12_HOURS -> "12h"
        PresetTimeRange.LAST_24_HOURS -> "24h"
        PresetTimeRange.LAST_7_DAYS -> "7 dni"
        PresetTimeRange.LAST_14_DAYS -> "14 dni"
        PresetTimeRange.LAST_30_DAYS -> "30 dni"
        PresetTimeRange.LAST_90_DAYS -> "90 dni"
        PresetTimeRange.LAST_12_MONTHS -> "12 mies."
    }
    val readingLabel = latestReadingAt?.let {
        val today = Instant.now().atZone(zoneId).toLocalDate()
        val readingDate = it.atZone(zoneId).toLocalDate()
        val prefix = when (readingDate) {
            today -> "dziś"
            today.minusDays(1) -> "wczoraj"
            else -> DateTimeFormatterProvider.compactDateFormatter().withZone(zoneId).format(it)
        }
        "Dane: $prefix ${PolishDateTimeFormatter.formatTime(it, zoneId)}"
    } ?: "Dane: brak"
    return "$readingLabel • Zakres: $rangeLabel"
}

internal fun chartClickAction(state: MonitoringUiState): MonitoringAction =
    MonitoringAction.OpenHistory(buildHistoryOpenContext(state))

internal fun topBarPersonSubtitle(personName: String?): String {
    val safeName = personName?.trim().takeIf { !it.isNullOrBlank() } ?: "-"
    return "Osoba: $safeName"
}

internal fun chartTitleForPerson(personName: String?): String {
    val safeName = personName?.trim().takeIf { !it.isNullOrBlank() } ?: "-"
    return "Historia glikemii - $safeName"
}

internal fun dispatchPersonSelection(patientId: String, onSelected: (String) -> Unit) {
    val normalized = patientId.trim()
    if (normalized.isNotBlank()) onSelected(normalized)
}

internal fun isLandscapeDashboard(orientation: Int): Boolean =
    orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

internal fun glucoseCardTypographyForWidth(screenWidthDp: Int): DashboardTypography {
    val glucoseValue = when {
        screenWidthDp >= 700 -> 82
        screenWidthDp >= 480 -> 74
        screenWidthDp >= 380 -> 72
        else -> 64
    }
    return DashboardTypography(
        glucoseValueSp = glucoseValue,
        glucoseUnitSp = (glucoseValue * 0.44f).toInt().coerceIn(26, 36),
        trendArrowSp = glucoseValue,
        trendDescriptionSp = (glucoseValue * 0.30f).toInt().coerceIn(18, 24)
    )
}

internal fun glucoseValueAndUnitText(value: Int, unit: String = "mg/dL"): String = "$value $unit"

internal fun trendContentDescription(trend: GlucoseTrend): String = when (trend) {
    GlucoseTrend.RISING_FAST -> "Glikemia szybko rośnie"
    GlucoseTrend.RISING -> "Glikemia rośnie"
    GlucoseTrend.FLAT -> "Glikemia stabilna"
    GlucoseTrend.FALLING -> "Glikemia spada"
    GlucoseTrend.FALLING_FAST -> "Glikemia szybko spada"
    GlucoseTrend.UNKNOWN -> "Trend nieznany"
}

internal fun glucoseRangeStatus(value: Int, targetLow: Int, targetHigh: Int): String {
    return when {
        value < 54 -> "Krytycznie nisko"
        value < targetLow -> "Poniżej zakresu"
        value > 250 -> "Bardzo wysoko"
        value > targetHigh -> "Powyżej zakresu"
        else -> "W zakresie"
    }
}

internal fun formatChartPointLabel(
    point: GlucoseHistoryPoint,
    targetLow: Int,
    targetHigh: Int,
    zoneId: ZoneId
): ChartPointLabel {
    val absoluteDateTime = PolishDateTimeFormatter.formatAbsoluteWithSeconds(point.timestamp, zoneId)
    return ChartPointLabel(
        dateTime = absoluteDateTime,
        valueText = glucoseValueAndUnitText(point.value),
        valueLabel = "Wartość: ${glucoseValueAndUnitText(point.value)}",
        timeLabel = "Czas: $absoluteDateTime",
        statusText = glucoseRangeStatus(point.value, targetLow, targetHigh),
        trendText = trendContentDescription(point.trend)
    )
}

internal fun formatReadingAge(duration: Duration?): String {
     val safeDuration = duration ?: return "brak czasu pomiaru"
     if (safeDuration.isNegative || safeDuration.isZero) return "przed chwilą"

     val totalMinutes = safeDuration.toMinutes()
     if (totalMinutes < 1) return "przed chwilą"
     if (totalMinutes < 60) return "${totalMinutes} min temu"

     val totalHours = totalMinutes / 60
     val minutes = totalMinutes % 60
     if (totalHours < 24) {
         return if (minutes == 0L) {
             if (totalHours == 1L) "1 godz. temu" else "${totalHours} godz. temu"
         } else {
             val minSuffix = if (minutes == 1L) "min" else "min"
             if (totalHours == 1L) "1 godz. ${minutes} $minSuffix temu" else "${totalHours} godz. ${minutes} $minSuffix temu"
         }
     }

     val days = totalHours / 24
     val hours = totalHours % 24
     val dayLabel = if (days == 1L) "1 dzień" else "$days dni"
     val hourLabel = if (hours == 1L) "1 godz." else "$hours godz."
     return "$dayLabel $hourLabel temu"
 }

internal fun formatDurationLabel(duration: Duration?): String {
    return PolishDateTimeFormatter.formatCompactDuration(duration)
}

internal fun estimateTrendRate(
    reading: GlucoseReading,
    requestedWindowMinutes: Int
): TrendRateEstimate? {
    val snapshot = trendWindowSnapshot(reading = reading, requestedWindowMinutes = requestedWindowMinutes) ?: return null
    val first = snapshot.points.first()
    val last = snapshot.points.last()
    val mgDlPerMinute = (last.value - first.value).toDouble() / snapshot.spanMinutes
    return TrendRateEstimate(
        mgDlPerMinute = mgDlPerMinute,
        mgDlPer15Minutes = mgDlPerMinute * 15.0,
        windowMinutes = snapshot.windowMinutes,
        sampleCount = snapshot.points.size,
        spanMinutes = snapshot.spanMinutes
    )
}

internal fun formatTrendRatePerMinute(rateMgDlPerMinute: Double): String {
    val sign = if (rateMgDlPerMinute >= 0.0) "+" else "−"
    return "$sign${"%.1f".format(java.util.Locale.US, kotlin.math.abs(rateMgDlPerMinute))} mg/dL/min"
}

internal fun buildTrendProjection(
    reading: GlucoseReading,
    trendWindowMinutes: Int,
    thresholds: TrendProjectionThresholds,
    now: Instant
): TrendProjection? {
    if (reading.trend != GlucoseTrend.RISING_FAST && reading.trend != GlucoseTrend.FALLING_FAST) return null
    val elapsedDuration = Duration.between(reading.timestamp, now)
    if (elapsedDuration.toMinutes() > 15 || elapsedDuration.isNegative) return null

    val estimate = estimateTrendRate(reading, trendWindowMinutes) ?: return null
    if (estimate.derivedTrend != reading.trend) return null

    val target = when (reading.trend) {
        GlucoseTrend.RISING_FAST -> listOf(thresholds.highThresholdMgDl, 250)
            .filter { it > reading.value }
            .minOrNull()
        GlucoseTrend.FALLING_FAST -> listOf(thresholds.lowThresholdMgDl, 54)
            .filter { it < reading.value }
            .maxOrNull()
        else -> null
    } ?: return null

    val totalMinutesToTarget = ((target - reading.value).toDouble() / estimate.mgDlPerMinute)
    if (!totalMinutesToTarget.isFinite() || totalMinutesToTarget <= 0.0) return null

    val elapsedMinutes = elapsedDuration.toMillis() / 60_000.0
    val remainingMinutes = totalMinutesToTarget - elapsedMinutes
    if (remainingMinutes <= 0.0 || remainingMinutes > 90.0) return null

    return TrendProjection(
        rateMgDlPerMinute = estimate.mgDlPerMinute,
        thresholdMgDl = target,
        minutesToThreshold = remainingMinutes.roundToInt().coerceAtLeast(1)
    )
}

internal fun formatTrendProjectionMessage(projection: TrendProjection): String =
    "W tym tempie: około ${projection.thresholdMgDl} mg/dL za ${projection.minutesToThreshold} min."

internal fun trendWindowSnapshot(
    reading: GlucoseReading,
    requestedWindowMinutes: Int
): TrendWindowSnapshot? {
    val windowMinutes = requestedWindowMinutes.coerceIn(3, 20)
    val timeline = readingTimeline(reading)
    if (timeline.size < 2) return null

    val end = timeline.last().timestamp
    val windowStart = end.minus(Duration.ofMinutes(windowMinutes.toLong()))
    val inWindow = timeline.filter { !it.timestamp.isBefore(windowStart) && !it.timestamp.isAfter(end) }
    if (inWindow.size < 2) return null

    val first = inWindow.first()
    val last = inWindow.last()
    val spanMinutes = Duration.between(first.timestamp, last.timestamp).seconds.toDouble() / 60.0
    if (!spanMinutes.isFinite() || spanMinutes <= 0.0) return null

    return TrendWindowSnapshot(
        points = inWindow,
        windowMinutes = windowMinutes,
        spanMinutes = spanMinutes
    )
}

internal fun rangeTileUi(percent: Int?, duration: Duration?, hasReadings: Boolean = duration != null || percent != null): RangeTileUi {
    if (!hasReadings || percent == null || duration == null || duration.isNegative) {
        return RangeTileUi(percentLabel = "—", durationLabel = "brak danych", hasData = false)
    }
    return RangeTileUi(
        percentLabel = "$percent%",
        durationLabel = PolishDateTimeFormatter.formatRangeTileDuration(duration, hasReadings = true),
        hasData = true
    )
}

internal fun readingTimeline(reading: GlucoseReading): List<GlucoseHistoryPoint> {
    return (reading.history + GlucoseHistoryPoint(value = reading.value, timestamp = reading.timestamp, trend = reading.trend))
        .distinctBy { it.timestamp to it.value }
        .sortedBy { it.timestamp }
}

internal fun buildDashboardMetricTiles(
    reading: GlucoseReading,
    targetLow: Int,
    targetHigh: Int,
    sensorActivityPeriodEnd: Instant = Instant.now(),
    sensorWindow: Duration = Duration.ofDays(14)
): List<DashboardMetricTile> {
    val history = readingTimeline(reading)
    val distribution = rangeDistributionFromHistory(history, targetLow, targetHigh)
    val below = rangeTileUi(distribution?.belowRangePercent, distribution?.belowRangeDuration, hasReadings = history.isNotEmpty())
    val inRange = rangeTileUi(distribution?.inRangePercent, distribution?.inRangeDuration, hasReadings = history.isNotEmpty())
    val above = rangeTileUi(distribution?.aboveRangePercent, distribution?.aboveRangeDuration, hasReadings = history.isNotEmpty())
    val sensorActivity = sensorActivityFromHistory(history, sensorActivityPeriodEnd.minus(sensorWindow), sensorActivityPeriodEnd)
    val gmi = buildHbA1cKpiModel(HbA1cSettings(history.firstOrNull()?.timestamp?.toString(), null, null, 7.5), reading.history)

    return listOf(
        DashboardMetricTile("Poniżej", below.durationLabel, below.percentLabel, below.hasData),
        DashboardMetricTile("Zakres", inRange.durationLabel, inRange.percentLabel, inRange.hasData),
        DashboardMetricTile("Powyżej", above.durationLabel, above.percentLabel, above.hasData),
        DashboardMetricTile(
            label = "GMI",
            value = gmi.valuePercent?.let { "${"%.1f".format(it).replace('.', ',')}%" } ?: "mało danych",
            supportingText = if (gmi.valuePercent == null) "szacunek" else "szacowane",
            hasData = gmi.valuePercent != null
        ),
        DashboardMetricTile(
            label = "Czujnik",
            value = sensorActivity?.activityPercent?.let { "${it.toInt()}%" } ?: "—",
            supportingText = if (sensorActivity == null) "brak danych" else "14 dni",
            hasData = sensorActivity != null
        )
    )
}

internal fun evaluateNfzStatus(
    sensorActivityPercent: Double?,
    tirPercent: Int?,
    labHbA1cPercent: Double?,
    therapeuticGoalReached: Boolean = false
): NfzStatusUi {
    if (sensorActivityPercent == null || tirPercent == null) {
        return NfzStatusUi(
            status = NfzStatus.GRAY,
            headline = "Nie można ocenić",
            details = "Status ma charakter informacyjny. Ostatecznej oceny dokonuje lekarz zgodnie z aktualnymi zasadami refundacji."
        )
    }

    val clinicalCondition = (tirPercent > 70) || (labHbA1cPercent?.let { it < 7.5 } == true) || therapeuticGoalReached
    return when {
        sensorActivityPercent >= 75.0 && clinicalCondition -> NfzStatusUi(
            status = NfzStatus.GREEN,
            headline = "Warunki widoczne w aplikacji wyglądają na spełnione",
            details = "Status ma charakter informacyjny. Ostatecznej oceny dokonuje lekarz zgodnie z aktualnymi zasadami refundacji."
        )
        sensorActivityPercent >= 65.0 -> NfzStatusUi(
            status = NfzStatus.YELLOW,
            headline = "Blisko spełnienia albo za mało danych",
            details = "Status ma charakter informacyjny. Ostatecznej oceny dokonuje lekarz zgodnie z aktualnymi zasadami refundacji."
        )
        else -> NfzStatusUi(
            status = NfzStatus.RED,
            headline = "Warunki widoczne w aplikacji nie są spełnione",
            details = "Status ma charakter informacyjny. Ostatecznej oceny dokonuje lekarz zgodnie z aktualnymi zasadami refundacji."
        )
    }
}

internal fun findNearestPoint(
    points: List<GlucosePoint>,
    touchX: Float,
    chartBounds: Rect,
    timeRangeStart: Instant,
    timeRangeEnd: Instant
): GlucosePoint? {
    if (points.isEmpty()) return null
    if (!chartBounds.width.isFinite() || !chartBounds.height.isFinite() || chartBounds.width <= 0f) {
        return points.firstOrNull()
    }
    val durationMillis = max(1L, Duration.between(timeRangeStart, timeRangeEnd).toMillis())
    return points.sortedBy { it.timestamp }.minByOrNull { point ->
        val millisFromStart = Duration.between(timeRangeStart, point.timestamp).toMillis().coerceIn(0L, durationMillis)
        val x = chartBounds.left + (millisFromStart.toFloat() / durationMillis.toFloat()) * chartBounds.width
        abs(x - touchX)
    }
}

internal fun MonitoringUiState.toDashboardUiState(now: Instant = Instant.now()): DashboardUiState {
    val reading = reading
    val selectedHistory = reading?.history.orEmpty()
    val hba1cSettings = HbA1cSettings(
        patientId = selectedPatientId,
        labHbA1cPercent = labHbA1cPercent,
        labHbA1cDate = labHbA1cDate,
        targetHbA1cPercent = targetHbA1cPercent
    )
    val kpi = buildHbA1cKpiModel(
        hba1cSettings = hba1cSettings,
        historyPoints = selectedHistory
    )
    val ageMinutes = reading?.timestamp?.let { Duration.between(it, now).toMinutes().coerceAtLeast(0) }
    return DashboardUiState(
        selectedPersonHbA1cSettings = hba1cSettings,
        hba1cKpi = kpi,
        historyPoints = selectedHistory,
        readingAgeMinutes = ageMinutes,
        selectedPersonName = selectedPersonName,
        selectedPatientId = selectedPatientId
    )
}

internal fun rangeDistributionFromHistory(
    history: List<GlucoseHistoryPoint>,
    targetLow: Int,
    targetHigh: Int,
    maxGap: Duration = Duration.ofMinutes(20)
): RangeDistribution? {
    if (history.size < 2) return null
    return com.libredisplay.analytics.GlucoseMetricsCalculator.calculateRangeDistribution(
        readings = history,
        targetLow = targetLow,
        targetHigh = targetHigh,
        lowCritical = 54,
        highCritical = 250,
        maxGap = maxGap
    )
}

internal fun sensorActivityFromHistory(
    history: List<GlucoseHistoryPoint>,
    periodStart: Instant,
    periodEnd: Instant
): SensorActivity? {
    if (history.size < 2) return null
    return com.libredisplay.analytics.GlucoseMetricsCalculator.calculateSensorActivity(
        readings = history,
        periodStart = periodStart,
        periodEnd = periodEnd,
        expectedIntervalMinutes = 15,
        maxGap = Duration.ofMinutes(20)
    )
}

internal data class DashboardMetrics(
    val belowDuration: Duration?,
    val belowPercent: Int?,
    val inRangeDuration: Duration?,
    val inRangePercent: Int?,
    val aboveDuration: Duration?,
    val abovePercent: Int?,
    val gmiValue: Double?,
    val hba1cValue: Double?
)

internal fun buildDashboardMetrics(
    reading: GlucoseReading,
    targetLow: Int,
    targetHigh: Int
): DashboardMetrics {
    val history = readingTimeline(reading)
    val distribution = rangeDistributionFromHistory(history, targetLow, targetHigh)
    val gmi = buildHbA1cKpiModel(
        HbA1cSettings(reading.timestamp.toString(), null, null, 7.5),
        reading.history
    )

    return DashboardMetrics(
        belowDuration = distribution?.belowRangeDuration,
        belowPercent = distribution?.belowRangePercent,
        inRangeDuration = distribution?.inRangeDuration,
        inRangePercent = distribution?.inRangePercent,
        aboveDuration = distribution?.aboveRangeDuration,
        abovePercent = distribution?.aboveRangePercent,
        gmiValue = gmi.valuePercent,
        hba1cValue = null  // TODO: Dodaj HbA1c jeśli dostępne z lab results
    )
}

