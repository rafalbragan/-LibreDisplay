package com.libredisplay.analytics

import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class AnalysisTrendInterpreterTest {

    @Test
    fun interpret_returnsMessageWhenTooLittleData() {
        val overlay = FourteenDayOverlay(
            dayLines = listOf(OverlayDayLine(date = LocalDate.of(2026, 8, 24), points = listOf(MinutePoint(600, 120)))),
            averageLine = emptyList()
        )
        val result = AnalysisTrendInterpreter.interpret(overlay, 80, 180)
        assertTrue(result.size == 1 && result.first().contains("Za mało"))
    }

    @Test
    fun interpret_detectsNoonPeak() {
        val hours = listOf(0, 3, 6, 9, 12, 15, 18, 21)
        val dayLines = (0 until 3).map { day ->
            OverlayDayLine(
                date = LocalDate.of(2026, 8, 20 + day),
                points = hours.map { h -> MinutePoint(h * 60, if (h == 12) 240 else 110) }
            )
        }
        val result = AnalysisTrendInterpreter.interpret(FourteenDayOverlay(dayLines, emptyList()), 80, 180)
        assertTrue(result.isNotEmpty())
        assertTrue(result.any { it.contains("12:00") })
        assertTrue(result.any { it.contains("południa") })
    }
}

