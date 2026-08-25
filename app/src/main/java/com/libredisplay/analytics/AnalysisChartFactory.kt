package com.libredisplay.analytics

import com.libredisplay.data.model.GlucoseHistoryPoint
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit

data class OverlayDayLine(
    val date: LocalDate,
    val points: List<MinutePoint>
)

data class MinutePoint(
    val minuteOfDay: Int,
    val valueMgDl: Int
)

data class OverlayMinuteAverage(
    val minuteOfDay: Int,
    val averageMgDl: Double,
    val sampleCount: Int
)

data class FourteenDayOverlay(
    val dayLines: List<OverlayDayLine>,
    val averageLine: List<OverlayMinuteAverage>
)

enum class BarChartMode { DAILY, MONTHLY }

/** One stacked distribution bar (a day in DAILY mode, a month in MONTHLY mode). */
data class RangeBar(
    val label: String,
    val fullLabel: String,
    val startInclusive: Instant,
    val endExclusive: Instant,
    val belowPercent: Int,
    val inRangePercent: Int,
    val abovePercent: Int,
    val averageGlucose: Double?,
    val minGlucose: Int?,
    val maxGlucose: Int?,
    val veryLowEpisodes: Int,
    val veryHighEpisodes: Int,
    val readingsCount: Int
) {
    val hasData: Boolean get() = readingsCount > 0
}

/** A scrollable window of bars plus the date range it covers and scroll affordances. */
data class BarChartWindow(
    val mode: BarChartMode,
    val bars: List<RangeBar>,
    val rangeLabel: String,
    val windowStart: Instant,
    val windowEnd: Instant,
    val canScrollOlder: Boolean,
    val canScrollNewer: Boolean
)

object AnalysisChartFactory {

    private val PL_MONTHS = arrayOf(
        "sty", "lut", "mar", "kwi", "maj", "cze", "lip", "sie", "wrz", "paź", "lis", "gru"
    )

    /** True when the collected span is long enough for a meaningful 12-month view. */
    fun hasMonthlyData(
        readings: List<GlucoseHistoryPoint>,
        now: Instant,
        zoneId: ZoneId = ZoneId.systemDefault(),
        minDays: Long = 60
    ): Boolean {
        val oldest = readings.minByOrNull { it.timestamp }?.timestamp ?: return false
        return ChronoUnit.DAYS.between(
            oldest.atZone(zoneId).toLocalDate(),
            now.atZone(zoneId).toLocalDate()
        ) >= minDays
    }

    /** Largest daily scroll offset (in days) that keeps the window start within collected data. */
    fun maxDailyOffset(
        readings: List<GlucoseHistoryPoint>,
        now: Instant,
        count: Int = 14,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Int {
        val oldest = readings.minByOrNull { it.timestamp }?.timestamp ?: return 0
        val span = ChronoUnit.DAYS.between(
            oldest.atZone(zoneId).toLocalDate(),
            now.atZone(zoneId).toLocalDate()
        ).toInt()
        return (span - (count - 1)).coerceAtLeast(0)
    }

    /** Largest monthly scroll offset (in months) that keeps the window within collected data. */
    fun maxMonthlyOffset(
        readings: List<GlucoseHistoryPoint>,
        now: Instant,
        count: Int = 12,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Int {
        val oldest = readings.minByOrNull { it.timestamp }?.timestamp ?: return 0
        val span = ChronoUnit.MONTHS.between(
            YearMonth.from(oldest.atZone(zoneId)),
            YearMonth.from(now.atZone(zoneId))
        ).toInt()
        return (span - (count - 1)).coerceAtLeast(0)
    }

    fun dailyWindow(
        readings: List<GlucoseHistoryPoint>,
        now: Instant,
        offsetDays: Int,
        count: Int = 14,
        targetLow: Int,
        targetHigh: Int,
        lowCritical: Int,
        highCritical: Int,
        nightOnly: Boolean = false,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): BarChartWindow {
        val safeOffset = offsetDays.coerceAtLeast(0)
        val endDate = now.atZone(zoneId).toLocalDate().minusDays(safeOffset.toLong())
        val startDate = endDate.minusDays((count - 1).toLong())
        val windowStart = startDate.atStartOfDay(zoneId).toInstant()
        val windowEnd = endDate.plusDays(1).atStartOfDay(zoneId).toInstant()

        val bars = (0 until count).map { i ->
            val date = startDate.plusDays(i.toLong())
            val dayStart = date.atStartOfDay(zoneId).toInstant()
            val dayEnd = date.plusDays(1).atStartOfDay(zoneId).toInstant()
            val points = readings
                .filter { !it.timestamp.isBefore(dayStart) && it.timestamp.isBefore(dayEnd) }
                .filter { !nightOnly || isNight(it.timestamp, zoneId) }
            aggregate(
                label = date.dayOfMonth.toString(),
                fullLabel = "%02d.%02d".format(date.dayOfMonth, date.monthValue),
                startInclusive = dayStart,
                endExclusive = dayEnd,
                points = points,
                targetLow = targetLow,
                targetHigh = targetHigh,
                lowCritical = lowCritical,
                highCritical = highCritical
            )
        }

        val oldest = readings.minByOrNull { it.timestamp }?.timestamp
        return BarChartWindow(
            mode = BarChartMode.DAILY,
            bars = bars,
            rangeLabel = rangeLabel(startDate, endDate),
            windowStart = windowStart,
            windowEnd = windowEnd,
            canScrollOlder = oldest != null && oldest.isBefore(windowStart),
            canScrollNewer = safeOffset > 0
        )
    }

    fun monthlyWindow(
        readings: List<GlucoseHistoryPoint>,
        now: Instant,
        offsetMonths: Int,
        count: Int = 12,
        targetLow: Int,
        targetHigh: Int,
        lowCritical: Int,
        highCritical: Int,
        nightOnly: Boolean = false,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): BarChartWindow {
        val safeOffset = offsetMonths.coerceAtLeast(0)
        val endMonth = YearMonth.from(now.atZone(zoneId)).minusMonths(safeOffset.toLong())
        val startMonth = endMonth.minusMonths((count - 1).toLong())
        val windowStart = startMonth.atDay(1).atStartOfDay(zoneId).toInstant()
        val windowEnd = endMonth.plusMonths(1).atDay(1).atStartOfDay(zoneId).toInstant()

        val bars = (0 until count).map { i ->
            val month = startMonth.plusMonths(i.toLong())
            val mStart = month.atDay(1).atStartOfDay(zoneId).toInstant()
            val mEnd = month.plusMonths(1).atDay(1).atStartOfDay(zoneId).toInstant()
            val points = readings
                .filter { !it.timestamp.isBefore(mStart) && it.timestamp.isBefore(mEnd) }
                .filter { !nightOnly || isNight(it.timestamp, zoneId) }
            aggregate(
                label = PL_MONTHS[month.monthValue - 1],
                fullLabel = "${PL_MONTHS[month.monthValue - 1]} ${month.year}",
                startInclusive = mStart,
                endExclusive = mEnd,
                points = points,
                targetLow = targetLow,
                targetHigh = targetHigh,
                lowCritical = lowCritical,
                highCritical = highCritical
            )
        }

        val oldest = readings.minByOrNull { it.timestamp }?.timestamp
        return BarChartWindow(
            mode = BarChartMode.MONTHLY,
            bars = bars,
            rangeLabel = "${PL_MONTHS[startMonth.monthValue - 1]} ${startMonth.year} – " +
                "${PL_MONTHS[endMonth.monthValue - 1]} ${endMonth.year}",
            windowStart = windowStart,
            windowEnd = windowEnd,
            canScrollOlder = oldest != null && oldest.isBefore(windowStart),
            canScrollNewer = safeOffset > 0
        )
    }

    /**
     * Builds the daily overlay for the days contained in [windowStart, windowEnd). If the window
     * spans more than [maxDays] days (e.g. the monthly view), only the most recent [maxDays] are
     * used so the chart stays readable.
     */
    fun overlayForWindow(
        readings: List<GlucoseHistoryPoint>,
        windowStart: Instant,
        windowEnd: Instant,
        zoneId: ZoneId = ZoneId.systemDefault(),
        maxDays: Int = 14
    ): FourteenDayOverlay {
        val inWindow = readings.filter {
            !it.timestamp.isBefore(windowStart) && it.timestamp.isBefore(windowEnd)
        }
        if (inWindow.isEmpty()) return FourteenDayOverlay(emptyList(), emptyList())

        val allDates = inWindow.map { it.timestamp.atZone(zoneId).toLocalDate() }.distinct().sorted()
        val selectedDates = if (allDates.size > maxDays) allDates.takeLast(maxDays).toSet() else allDates.toSet()

        val dayLines = inWindow
            .filter { it.timestamp.atZone(zoneId).toLocalDate() in selectedDates }
            .groupBy { it.timestamp.atZone(zoneId).toLocalDate() }
            .entries.sortedBy { it.key }
            .map { (date, dayPoints) ->
                OverlayDayLine(
                    date = date,
                    points = dayPoints.sortedBy { it.timestamp }.map { point ->
                        val lt = point.timestamp.atZone(zoneId).toLocalTime()
                        MinutePoint(lt.hour * 60 + lt.minute, point.value)
                    }
                )
            }

        val perMinute = mutableMapOf<Int, MutableList<Int>>()
        dayLines.forEach { line ->
            line.points.forEach { p -> perMinute.getOrPut(p.minuteOfDay) { mutableListOf() }.add(p.valueMgDl) }
        }
        val averageLine = perMinute.entries.sortedBy { it.key }
            .map { (minute, values) -> OverlayMinuteAverage(minute, values.average(), values.size) }
        return FourteenDayOverlay(dayLines, averageLine)
    }

    /** Backwards-compatible helper: the last 14 days' overlay ending at [now]. */
    fun fourteenDayOverlay(
        readings: List<GlucoseHistoryPoint>,
        now: Instant,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): FourteenDayOverlay {
        val endDate = now.atZone(zoneId).toLocalDate()
        val startDate = endDate.minusDays(13)
        return overlayForWindow(
            readings,
            startDate.atStartOfDay(zoneId).toInstant(),
            endDate.plusDays(1).atStartOfDay(zoneId).toInstant(),
            zoneId,
            14
        )
    }

    private fun aggregate(
        label: String,
        fullLabel: String,
        startInclusive: Instant,
        endExclusive: Instant,
        points: List<GlucoseHistoryPoint>,
        targetLow: Int,
        targetHigh: Int,
        lowCritical: Int,
        highCritical: Int
    ): RangeBar {
        if (points.isEmpty()) {
            return RangeBar(label, fullLabel, startInclusive, endExclusive, 0, 0, 0, null, null, null, 0, 0, 0)
        }
        val total = points.size.toDouble()
        val below = points.count { it.value < targetLow }
        val above = points.count { it.value > targetHigh }
        val inRange = points.size - below - above
        return RangeBar(
            label = label,
            fullLabel = fullLabel,
            startInclusive = startInclusive,
            endExclusive = endExclusive,
            belowPercent = ((below / total) * 100.0).toInt().coerceIn(0, 100),
            inRangePercent = ((inRange / total) * 100.0).toInt().coerceIn(0, 100),
            abovePercent = ((above / total) * 100.0).toInt().coerceIn(0, 100),
            averageGlucose = points.map { it.value }.average(),
            minGlucose = points.minOf { it.value },
            maxGlucose = points.maxOf { it.value },
            veryLowEpisodes = countEpisodes(points) { it < lowCritical },
            veryHighEpisodes = countEpisodes(points) { it > highCritical },
            readingsCount = points.size
        )
    }

    private fun rangeLabel(start: LocalDate, end: LocalDate): String {
        val s = "%02d.%02d".format(start.dayOfMonth, start.monthValue)
        val e = "%02d.%02d".format(end.dayOfMonth, end.monthValue)
        return if (s == e) s else "$s – $e"
    }

    private fun isNight(instant: Instant, zoneId: ZoneId): Boolean {
        val time = instant.atZone(zoneId).toLocalTime()
        return time >= LocalTime.of(22, 0) || time < LocalTime.of(6, 0)
    }

    private fun countEpisodes(points: List<GlucoseHistoryPoint>, predicate: (Int) -> Boolean): Int {
        if (points.isEmpty()) return 0
        val sorted = points.sortedBy { it.timestamp }
        var episodes = 0
        var inEpisode = false
        sorted.forEach { point ->
            val matches = predicate(point.value)
            if (matches && !inEpisode) {
                episodes += 1
                inEpisode = true
            } else if (!matches) {
                inEpisode = false
            }
        }
        return episodes
    }
}

