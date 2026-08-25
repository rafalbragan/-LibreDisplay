package com.libredisplay.analytics

import com.libredisplay.data.model.GlucoseHistoryPoint
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

data class WeeklyRangeBar(
    val date: LocalDate,
    val belowPercent: Int,
    val inRangePercent: Int,
    val abovePercent: Int,
    val veryLowEpisodes: Int,
    val veryHighEpisodes: Int,
    val readingsCount: Int
)

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

object AnalysisChartFactory {

    fun weeklyStackedBars(
        readings: List<GlucoseHistoryPoint>,
        now: Instant,
        targetLow: Int,
        targetHigh: Int,
        lowCritical: Int,
        highCritical: Int,
        zoneId: ZoneId = ZoneId.systemDefault(),
        nightOnly: Boolean = false
    ): List<WeeklyRangeBar> {
        val endDate = now.atZone(zoneId).toLocalDate()
        val dates = (6 downTo 0).map { endDate.minusDays(it.toLong()) }
        return dates.map { date ->
            val points = readings
                .asSequence()
                .filter { it.timestamp.atZone(zoneId).toLocalDate() == date }
                .filter { !nightOnly || isNight(it.timestamp, zoneId) }
                .toList()

            if (points.isEmpty()) {
                return@map WeeklyRangeBar(
                    date = date,
                    belowPercent = 0,
                    inRangePercent = 0,
                    abovePercent = 0,
                    veryLowEpisodes = 0,
                    veryHighEpisodes = 0,
                    readingsCount = 0
                )
            }

            val below = points.count { it.value < targetLow }
            val inRange = points.count { it.value in targetLow..targetHigh }
            val above = points.count { it.value > targetHigh }
            val total = points.size.toDouble()

            WeeklyRangeBar(
                date = date,
                belowPercent = ((below / total) * 100.0).toInt().coerceIn(0, 100),
                inRangePercent = ((inRange / total) * 100.0).toInt().coerceIn(0, 100),
                abovePercent = ((above / total) * 100.0).toInt().coerceIn(0, 100),
                veryLowEpisodes = countEpisodes(points) { it < lowCritical },
                veryHighEpisodes = countEpisodes(points) { it > highCritical },
                readingsCount = points.size
            )
        }
    }

    fun fourteenDayOverlay(
        readings: List<GlucoseHistoryPoint>,
        now: Instant,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): FourteenDayOverlay {
        val endDate = now.atZone(zoneId).toLocalDate()
        val startDate = endDate.minusDays(13)
        val allowedDates = generateSequence(startDate) { previous ->
            if (previous == endDate) null else previous.plusDays(1)
        }.toSet()

        val pointsByDate = readings
            .asSequence()
            .filter { it.timestamp.atZone(zoneId).toLocalDate() in allowedDates }
            .groupBy { it.timestamp.atZone(zoneId).toLocalDate() }

        val dayLines = pointsByDate
            .entries
            .sortedBy { it.key }
            .map { (date, dayPoints) ->
                OverlayDayLine(
                    date = date,
                    points = dayPoints
                        .sortedBy { it.timestamp }
                        .map { point ->
                            val localTime = point.timestamp.atZone(zoneId).toLocalTime()
                            MinutePoint(localTime.hour * 60 + localTime.minute, point.value)
                        }
                )
            }

        val perMinuteValues = mutableMapOf<Int, MutableList<Int>>()
        dayLines.forEach { line ->
            line.points.forEach { point ->
                perMinuteValues.getOrPut(point.minuteOfDay) { mutableListOf() }.add(point.valueMgDl)
            }
        }

        val averageLine = perMinuteValues
            .entries
            .sortedBy { it.key }
            .map { (minute, values) ->
                OverlayMinuteAverage(
                    minuteOfDay = minute,
                    averageMgDl = values.average(),
                    sampleCount = values.size
                )
            }

        return FourteenDayOverlay(dayLines = dayLines, averageLine = averageLine)
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

