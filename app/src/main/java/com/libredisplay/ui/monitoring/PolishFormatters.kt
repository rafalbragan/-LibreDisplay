package com.libredisplay.ui.monitoring

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

internal object DateTimeFormatterProvider {
    private val polishLocale = Locale("pl", "PL")

    fun deviceZoneId(): ZoneId = ZoneId.systemDefault()

    fun timeFormatter(): DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", polishLocale)

    fun compactDateFormatter(): DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM", polishLocale)

    fun compactDateTimeFormatter(): DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM HH:mm", polishLocale)

    fun absoluteDateTimeFormatter(): DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy, HH:mm", polishLocale)

    fun chartDateTimeFormatter(): DateTimeFormatter = absoluteDateTimeFormatter()
}

internal object PolishDateTimeFormatter {
    fun formatUserFacing(
        instant: Instant,
        now: Instant = Instant.now(),
        zoneId: ZoneId = DateTimeFormatterProvider.deviceZoneId()
    ): String {
        val localDate = instant.atZone(zoneId).toLocalDate()
        val nowDate = now.atZone(zoneId).toLocalDate()
        val timeText = DateTimeFormatterProvider.timeFormatter().withZone(zoneId).format(instant)
        return when (localDate) {
            nowDate -> "Dzisiaj $timeText"
            nowDate.minusDays(1) -> "Wczoraj $timeText"
            else -> formatAbsolute(instant, zoneId)
        }
    }

    fun formatAbsolute(
        instant: Instant,
        zoneId: ZoneId = DateTimeFormatterProvider.deviceZoneId()
    ): String = DateTimeFormatterProvider.absoluteDateTimeFormatter().withZone(zoneId).format(instant)

    fun formatTime(
        instant: Instant,
        zoneId: ZoneId = DateTimeFormatterProvider.deviceZoneId()
    ): String = DateTimeFormatterProvider.timeFormatter().withZone(zoneId).format(instant)

    fun formatChartAxisLabel(
        instant: Instant,
        visibleDuration: Duration,
        zoneId: ZoneId = DateTimeFormatterProvider.deviceZoneId()
    ): String {
        val safeDuration = if (visibleDuration.isNegative || visibleDuration.isZero) Duration.ofHours(24) else visibleDuration
        val formatter = when {
            safeDuration <= Duration.ofHours(36) -> DateTimeFormatterProvider.timeFormatter()
            safeDuration <= Duration.ofDays(7) -> DateTimeFormatterProvider.compactDateTimeFormatter()
            else -> DateTimeFormatterProvider.compactDateFormatter()
        }
        return formatter.withZone(zoneId).format(instant)
    }

    fun formatCompactDuration(duration: Duration?): String {
        if (duration == null) return "brak danych"
        val totalMinutes = duration.toMinutes().coerceAtLeast(0)
        if (totalMinutes == 0L) return "0m"

        val days = totalMinutes / (24 * 60)
        val hours = (totalMinutes % (24 * 60)) / 60
        val minutes = totalMinutes % 60
        val parts = mutableListOf<String>()

        if (days > 0) {
            parts += "${days}d"
        }
        if (hours > 0) {
            parts += "${hours}g"
        }
        if (minutes > 0 && days == 0L) {
            parts += "${minutes}m"
        } else if (minutes > 0 && parts.isEmpty()) {
            parts += "${minutes}m"
        }

        return parts.joinToString(" ").ifBlank { "0m" }
    }

    fun hasAnyReadings(readings: List<*>): Boolean = readings.isNotEmpty()

    fun formatRangeTileDuration(duration: Duration?, hasReadings: Boolean): String {
        if (!hasReadings || duration == null || duration.isNegative) return "brak danych"
        return formatCompactDuration(duration)
    }

    fun formatRangeLabel(
        start: Instant,
        end: Instant,
        zoneId: ZoneId = DateTimeFormatterProvider.deviceZoneId()
    ): String {
        val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale("pl", "PL")).withZone(zoneId)
        val startText = formatter.format(start)
        val endText = formatter.format(end)
        return if (startText == endText) {
            "Zakres: $startText"
        } else {
            "Zakres: $startText - $endText"
        }
    }

    fun calendarDaysBetween(
        start: Instant,
        end: Instant,
        zoneId: ZoneId = DateTimeFormatterProvider.deviceZoneId()
    ): Int {
        val startDate = start.atZone(zoneId).toLocalDate()
        val endDate = end.atZone(zoneId).toLocalDate()
        return java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate).toInt() + 1
    }

    fun dateOf(
        instant: Instant,
        zoneId: ZoneId = DateTimeFormatterProvider.deviceZoneId()
    ): LocalDate = instant.atZone(zoneId).toLocalDate()
}

