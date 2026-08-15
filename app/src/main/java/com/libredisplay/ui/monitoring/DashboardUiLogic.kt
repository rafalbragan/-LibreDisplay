package com.libredisplay.ui.monitoring

import androidx.compose.ui.geometry.Rect
import com.libredisplay.analytics.RangeDistribution
import com.libredisplay.analytics.SensorActivity
import com.libredisplay.data.model.GlucoseHistoryPoint
import com.libredisplay.data.model.GlucoseTrend
import com.libredisplay.data.model.HbA1cSettings
import com.libredisplay.data.model.LibreConnectionPerson
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.max

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

internal data class DashboardUiState(
    val selectedPersonHbA1cSettings: HbA1cSettings,
    val hba1cKpi: HbA1cKpiModel,
    val historyPoints: List<GlucoseHistoryPoint>,
    val readingAgeMinutes: Long?,
    val selectedPersonName: String?,
    val selectedPatientId: String?
)

internal fun MonitoringUiState.shouldShowPersonSwitcher(): Boolean = mapSwitcherPersons(availablePersons).size > 1

internal fun shouldShowRetryAction(connectionState: ConnectionState): Boolean = when (connectionState) {
    ConnectionState.Connected,
    ConnectionState.Connecting -> false
    else -> true
}

internal fun mapSwitcherPersons(persons: List<LibreConnectionPerson>): List<LibreConnectionPerson> = persons.take(3)

internal fun topBarPersonSubtitle(personName: String?): String {
    val safeName = personName?.trim().takeIf { !it.isNullOrBlank() } ?: "-"
    return "Osoba: $safeName"
}

internal fun chartTitleForPerson(personName: String?): String {
    val safeName = personName?.trim().takeIf { !it.isNullOrBlank() } ?: "-"
    return "Historia glukozy - $safeName"
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
    val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(zoneId)
    return ChartPointLabel(
        dateTime = formatter.format(point.timestamp),
        valueText = glucoseValueAndUnitText(point.value),
        statusText = glucoseRangeStatus(point.value, targetLow, targetHigh),
        trendText = trendContentDescription(point.trend)
    )
}

internal fun formatReadingAge(duration: Duration?): String {
    val safeDuration = duration ?: return "Brak czasu pomiaru"
    if (safeDuration.isNegative || safeDuration.isZero) return "przed chwilą"

    val totalMinutes = safeDuration.toMinutes()
    if (totalMinutes < 1) return "przed chwilą"
    if (totalMinutes < 60) return "Dane sprzed ${totalMinutes} min"

    val totalHours = totalMinutes / 60
    val minutes = totalMinutes % 60
    if (totalHours < 24) {
        return if (totalHours == 1L) {
            "Dane sprzed 1 godz. ${minutes} min"
        } else {
            "Dane sprzed ${totalHours} godz. ${minutes} min"
        }
    }

    val days = totalHours / 24
    val hours = totalHours % 24
    val dayLabel = if (days == 1L) "1 dzień" else "$days dni"
    return "Dane sprzed $dayLabel ${hours} godz. ${minutes} min"
}

internal fun formatDurationLabel(duration: Duration?): String {
    if (duration == null || duration.isZero || duration.isNegative) return "brak danych"
    val totalMinutes = duration.toMinutes().coerceAtLeast(0)
    val days = totalMinutes / (24 * 60)
    val hours = (totalMinutes % (24 * 60)) / 60
    val minutes = totalMinutes % 60
    return when {
        days > 0 -> "$days d ${hours} h ${minutes} min"
        hours > 0 -> "$hours godz. ${minutes} min"
        else -> "$minutes min"
    }
}

internal fun rangeTileUi(percent: Int?, duration: Duration?): RangeTileUi {
    if (percent == null || duration == null || duration.isNegative || duration.isZero) {
        return RangeTileUi(percentLabel = "—", durationLabel = "brak danych", hasData = false)
    }
    return RangeTileUi(
        percentLabel = "$percent%",
        durationLabel = formatDurationLabel(duration),
        hasData = true
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
