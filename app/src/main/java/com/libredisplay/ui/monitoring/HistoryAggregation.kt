package com.libredisplay.ui.monitoring

import com.libredisplay.data.model.GlucoseHistoryPoint
import com.libredisplay.data.model.GlucoseTrend
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

internal enum class TimeRange(val duration: Duration, val label: String, val shortLabel: String) {
    LAST_3_HOURS(Duration.ofHours(3), "Ostatnie 3 godz.", "3h"),
    LAST_6_HOURS(Duration.ofHours(6), "Ostatnie 6 godz.", "6h"),
    LAST_12_HOURS(Duration.ofHours(12), "Ostatnie 12 godz.", "12h"),
    LAST_24_HOURS(Duration.ofHours(24), "Ostatnie 24 godz.", "24h"),
    LAST_3_DAYS(Duration.ofDays(3), "Ostatnie 3 dni", "3 dni"),
    LAST_7_DAYS(Duration.ofDays(7), "Ostatnie 7 dni", "7 dni"),
    LAST_30_DAYS(Duration.ofDays(30), "Ostatnie 30 dni", "30 dni"),
    LAST_90_DAYS(Duration.ofDays(90), "Ostatnie 90 dni", "90 dni"),
    LAST_365_DAYS(Duration.ofDays(365), "Ostatni rok", "365 dni")
}

internal enum class DataQualityStatus {
    NO_DATA,
    LOW,
    MEDIUM,
    HIGH
}

internal data class MetricBucket(
    val start: Instant,
    val end: Instant,
    val averageGlucoseMgDl: Double?,
    val minMgDl: Int?,
    val maxMgDl: Int?,
    val belowRangePercent: Double?,
    val inRangePercent: Double?,
    val aboveRangePercent: Double?,
    val sensorActivityPercent: Double?,
    val gmiPercent: Double?,
    val readingsCount: Int,
    val dataQuality: DataQualityStatus
)

internal data class AggregatedGlucoseSeries(
    val buckets: List<MetricBucket>,
    val totalReadings: Int,
    val start: Instant,
    val end: Instant,
    val bucketSize: Duration,
    val range: TimeRange
)

internal fun aggregateReadingsForRange(
    readings: List<GlucoseHistoryPoint>,
    timeRange: TimeRange,
    bucketSize: Duration,
    targetLow: Int = 70,
    targetHigh: Int = 180,
    lowCritical: Int = 54,
    highCritical: Int = 250,
    maxGap: Duration = Duration.ofMinutes(20),
    zoneId: ZoneId = ZoneId.systemDefault()
): AggregatedGlucoseSeries {
    val sorted = readings
        .asSequence()
        .filter { it.value > 0 }
        .distinctBy { it.timestamp to it.value }
        .sortedBy { it.timestamp }
        .toList()

    val end = sorted.lastOrNull()?.timestamp ?: Instant.now()
    val start = end.minus(timeRange.duration)
    val bucketSeconds = bucketSize.seconds.coerceAtLeast(60)
    val alignedStart = Instant.ofEpochSecond((start.epochSecond / bucketSeconds) * bucketSeconds)

    val buckets = mutableListOf<MetricBucket>()
    var cursor = alignedStart
    while (cursor.isBefore(end) || cursor == end) {
        val bucketEnd = cursor.plus(bucketSize).let { if (it.isAfter(end)) end else it }
        buckets += calculateBucket(
            readings = sorted,
            bucketStart = cursor,
            bucketEnd = bucketEnd,
            targetLow = targetLow,
            targetHigh = targetHigh,
            lowCritical = lowCritical,
            highCritical = highCritical,
            maxGap = maxGap,
            zoneId = zoneId
        )
        if (!cursor.isBefore(end)) break
        cursor = cursor.plus(bucketSize)
        if (!cursor.isBefore(bucketEnd)) {
            // keep progressing even when bucketEnd is trimmed to the final reading timestamp
            continue
        }
    }

    return AggregatedGlucoseSeries(
        buckets = buckets,
        totalReadings = sorted.size,
        start = start,
        end = end,
        bucketSize = bucketSize,
        range = timeRange
    )
}

private fun calculateBucket(
    readings: List<GlucoseHistoryPoint>,
    bucketStart: Instant,
    bucketEnd: Instant,
    targetLow: Int,
    targetHigh: Int,
    lowCritical: Int,
    highCritical: Int,
    maxGap: Duration,
    zoneId: ZoneId
): MetricBucket {
    val bucketDurationMillis = Duration.between(bucketStart, bucketEnd).toMillis().coerceAtLeast(1)
    val relevant = readings.filter { !it.timestamp.isBefore(bucketStart) && !it.timestamp.isAfter(bucketEnd) }
    val segments = readings.zipWithNext().mapNotNull { (current, next) ->
        val delta = Duration.between(current.timestamp, next.timestamp)
        if (delta.isNegative || delta > maxGap) null else current to next
    }.filter { (current, next) -> next.timestamp.isAfter(bucketStart) && current.timestamp.isBefore(bucketEnd) }

    var coveredMillis = 0L
    var belowMillis = 0L
    var inRangeMillis = 0L
    var aboveMillis = 0L
    var sum = 0.0
    val values = mutableListOf<Int>()

    segments.forEach { (current, next) ->
        val segmentStart = if (current.timestamp.isBefore(bucketStart)) bucketStart else current.timestamp
        val segmentEnd = if (next.timestamp.isAfter(bucketEnd)) bucketEnd else next.timestamp
        val overlap = Duration.between(segmentStart, segmentEnd).toMillis()
        if (overlap <= 0) return@forEach
        coveredMillis += overlap
        sum += current.value * overlap
        values += current.value
        values += next.value
        when {
            current.value < lowCritical -> belowMillis += overlap
            current.value < targetLow -> belowMillis += overlap
            current.value > highCritical -> aboveMillis += overlap
            current.value > targetHigh -> aboveMillis += overlap
            else -> inRangeMillis += overlap
        }
    }

    if (values.isEmpty()) {
        values.addAll(relevant.map { it.value })
    }

    val readingsCount = relevant.size
    val sensorActivityPercent = if (coveredMillis <= 0L) null else (coveredMillis.toDouble() / bucketDurationMillis.toDouble() * 100.0).coerceIn(0.0, 100.0)
    val activity = sensorActivityPercent ?: 0.0
    val dataQuality = when {
        coveredMillis <= 0L -> DataQualityStatus.NO_DATA
        activity >= 75.0 -> DataQualityStatus.HIGH
        activity >= 50.0 -> DataQualityStatus.MEDIUM
        else -> DataQualityStatus.LOW
    }
    val average = if (coveredMillis > 0L) sum / coveredMillis.toDouble() else null
    val gmi = average?.takeIf { it.isFinite() }?.let { 3.31 + 0.02392 * it }

    fun pct(value: Long): Double? = if (coveredMillis <= 0L) null else (value.toDouble() / coveredMillis.toDouble() * 100.0).coerceIn(0.0, 100.0)

    return MetricBucket(
        start = bucketStart,
        end = bucketEnd,
        averageGlucoseMgDl = average,
        minMgDl = values.minOrNull(),
        maxMgDl = values.maxOrNull(),
        belowRangePercent = pct(belowMillis),
        inRangePercent = pct(inRangeMillis),
        aboveRangePercent = pct(aboveMillis),
        sensorActivityPercent = sensorActivityPercent,
        gmiPercent = gmi,
        readingsCount = readingsCount,
        dataQuality = dataQuality
    )
}

internal fun chartModeForRange(range: TimeRange): String = when (range) {
    TimeRange.LAST_3_HOURS,
    TimeRange.LAST_6_HOURS,
    TimeRange.LAST_12_HOURS,
    TimeRange.LAST_24_HOURS,
    TimeRange.LAST_7_DAYS -> "line"
    TimeRange.LAST_3_DAYS -> "aggregated"
    TimeRange.LAST_30_DAYS,
    TimeRange.LAST_90_DAYS,
    TimeRange.LAST_365_DAYS -> "bar"
}

internal fun TimeRangeState.toHistoryTimeRange(): TimeRange = when (presetRange) {
    PresetTimeRange.LAST_12_HOURS -> TimeRange.LAST_12_HOURS
    PresetTimeRange.LAST_24_HOURS -> TimeRange.LAST_24_HOURS
    PresetTimeRange.LAST_7_DAYS -> TimeRange.LAST_7_DAYS
    PresetTimeRange.LAST_14_DAYS -> TimeRange.LAST_30_DAYS
    PresetTimeRange.LAST_30_DAYS -> TimeRange.LAST_30_DAYS
    PresetTimeRange.LAST_90_DAYS -> TimeRange.LAST_90_DAYS
    PresetTimeRange.LAST_12_MONTHS -> TimeRange.LAST_365_DAYS
}

internal fun rangeStartInstant(range: TimeRange, end: Instant = Instant.now()): Instant = end.minus(range.duration)

internal fun bucketSizeForRange(range: TimeRange): Duration = when (range) {
    TimeRange.LAST_3_HOURS -> Duration.ofMinutes(5)
    TimeRange.LAST_6_HOURS -> Duration.ofMinutes(5)
    TimeRange.LAST_12_HOURS -> Duration.ofMinutes(5)
    TimeRange.LAST_24_HOURS -> Duration.ofMinutes(10)
    TimeRange.LAST_3_DAYS -> Duration.ofHours(1)
    TimeRange.LAST_7_DAYS -> Duration.ofHours(3)
    TimeRange.LAST_30_DAYS -> Duration.ofDays(1)
    TimeRange.LAST_90_DAYS -> Duration.ofDays(1)
    TimeRange.LAST_365_DAYS -> Duration.ofDays(7)
}

