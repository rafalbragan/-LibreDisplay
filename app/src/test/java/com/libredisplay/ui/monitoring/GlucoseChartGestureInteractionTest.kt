package com.libredisplay.ui.monitoring

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import com.libredisplay.data.model.GlucoseHistoryPoint
import com.libredisplay.data.model.GlucoseTrend
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class)
class GlucoseChartGestureInteractionTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun dragWithoutLongPress_doesNotSelectHistoryPoint() {
        var selectedCount = 0
        composeRule.setContent {
            GlucoseChart(
                points = samplePoints(),
                targetLow = 70,
                targetHigh = 180,
                onPointSelected = { selectedCount++ },
                chartHeight = 220.dp
            )
        }

        composeRule.onRoot().performTouchInput {
            down(center)
            moveBy(Offset(180f, 0f))
            up()
        }

        assertEquals(0, selectedCount)
    }

    @Test
    fun longPressDrag_selectsHistoryPoint() {
        var selectedCount = 0
        composeRule.setContent {
            GlucoseChart(
                points = samplePoints(),
                targetLow = 70,
                targetHigh = 180,
                onPointSelected = { selectedCount++ },
                chartHeight = 220.dp
            )
        }

        composeRule.onRoot().performTouchInput {
            down(center)
            advanceEventTime(700)
            moveBy(Offset(60f, 0f))
            up()
        }

        assertTrue(selectedCount >= 1)
    }

    private fun samplePoints(): List<GlucoseHistoryPoint> {
        val base = Instant.parse("2026-10-17T10:00:00Z")
        return listOf(
            GlucoseHistoryPoint(110, base.minusSeconds(600), GlucoseTrend.FLAT),
            GlucoseHistoryPoint(118, base.minusSeconds(300), GlucoseTrend.FLAT),
            GlucoseHistoryPoint(124, base, GlucoseTrend.RISING)
        )
    }
}

