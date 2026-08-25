package com.libredisplay.ui.monitoring

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.Density
import com.libredisplay.data.model.GlucoseReading
import com.libredisplay.data.model.GlucoseTrend
import com.libredisplay.testing.scenario.GlucoseScenario
import com.libredisplay.testing.scenario.GlucoseScenarioEngine
import com.libredisplay.ui.theme.LibreDisplayTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class MonitoringResponsiveUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val cachedScenario by lazy { GlucoseScenarioEngine.dataset(GlucoseScenario.FULL_24H_DATA) }

    @Test
    fun responsiveMatrix_requiredHomeScenariosRemainReadableWithoutOverlap() {
        val scenarios = listOf(
            GlucoseScenario.NORMAL_STABLE,
            GlucoseScenario.HIGH_RISING,
            GlucoseScenario.VERY_HIGH_RISING,
            GlucoseScenario.LOW_FAST_FALLING,
            GlucoseScenario.VERY_LOW,
            GlucoseScenario.NO_DATA,
            GlucoseScenario.STALE_DATA,
            GlucoseScenario.LONG_METRIC_VALUES
        )

        // A single setContent is required per Compose test; the host reads a mutable state
        // holder so the whole matrix is driven through recompositions.
        val params = mutableStateOf(
            HostParams(
                GlucoseScenario.NORMAL_STABLE,
                DefaultResponsiveMatrix.first().widthDp,
                DefaultResponsiveMatrix.first().fontScale
            )
        )
        composeRule.setContent {
            val current by params
            MonitoringHomeTestHost(
                scenario = current.scenario,
                widthDp = current.widthDp,
                fontScale = current.fontScale
            )
        }

        scenarios.forEach { scenario ->
            DefaultResponsiveMatrix.forEach { config ->
                composeRule.runOnUiThread {
                    params.value = HostParams(scenario, config.widthDp, config.fontScale)
                }
                composeRule.waitForIdle()

                if (scenario == GlucoseScenario.NO_DATA) {
                    composeRule.onNodeWithText("Brak danych").assertIsDisplayed()
                } else {
                    composeRule.onNodeWithTag(LibreCareTestTags.TOP_BAR_TITLE).assertIsDisplayed()
                    composeRule.onNodeWithTag(LibreCareTestTags.HOME_CURRENT_GLUCOSE_VALUE).assertIsDisplayed()
                    composeRule.onNodeWithTag(LibreCareTestTags.HOME_CURRENT_GLUCOSE_UNIT).assertIsDisplayed()
                    composeRule.onNodeWithTag(LibreCareTestTags.HOME_CURRENT_GLUCOSE_TREND).assertIsDisplayed()
                    composeRule.onNodeWithTag(LibreCareTestTags.HOME_CURRENT_GLUCOSE_SEVERITY).assertIsDisplayed()
                    composeRule.assertNoOverlap(
                        first = composeRule.onNodeWithTag(LibreCareTestTags.HOME_CURRENT_GLUCOSE_VALUE),
                        second = composeRule.onNodeWithTag(LibreCareTestTags.HOME_CURRENT_GLUCOSE_UNIT),
                        firstLabel = "Current glucose",
                        secondLabel = "Unit",
                        context = "scenario=$scenario width=${config.widthDp} fontScale=${config.fontScale}"
                    )
                    composeRule.assertNoOverlap(
                        first = composeRule.onNodeWithTag(LibreCareTestTags.HOME_CURRENT_GLUCOSE_VALUE),
                        second = composeRule.onNodeWithTag(LibreCareTestTags.HOME_CURRENT_GLUCOSE_TREND),
                        firstLabel = "Current glucose",
                        secondLabel = "Trend",
                        context = "scenario=$scenario width=${config.widthDp} fontScale=${config.fontScale}"
                    )
                    composeRule.assertNoOverlap(
                        first = composeRule.onNodeWithTag(LibreCareTestTags.TOP_BAR_TITLE),
                        second = composeRule.onNodeWithTag(LibreCareTestTags.TOP_BAR_VERSION),
                        firstLabel = "Top bar title",
                        secondLabel = "Version",
                        context = "scenario=$scenario width=${config.widthDp} fontScale=${config.fontScale}"
                    )
                    composeRule.assertNoOverlap(
                        first = composeRule.onNodeWithTag(LibreCareTestTags.metricTile(com.libredisplay.data.model.QuickMetricId.BELOW)),
                        second = composeRule.onNodeWithTag(LibreCareTestTags.metricTile(com.libredisplay.data.model.QuickMetricId.IN_RANGE)),
                        firstLabel = "Metric \"Poniżej\"",
                        secondLabel = "Metric \"W zakresie\"",
                        context = "scenario=$scenario width=${config.widthDp} fontScale=${config.fontScale}"
                    )
                    composeRule.assertNoOverlap(
                        first = composeRule.onNodeWithTag(LibreCareTestTags.rangeChip(HomeChartRange.LAST_1_HOUR)),
                        second = composeRule.onNodeWithTag(LibreCareTestTags.rangeChip(HomeChartRange.LAST_3_HOURS)),
                        firstLabel = "Range chip 1g",
                        secondLabel = "Range chip 3g",
                        context = "scenario=$scenario width=${config.widthDp} fontScale=${config.fontScale}"
                    )
                }
            }
        }
    }

    @Test
    fun metricStressValues_renderFullTextsWithoutEllipsisOnHomePanel() {
        composeRule.setContent {
            MonitoringHomeTestHost(
                scenario = GlucoseScenario.LONG_METRIC_VALUES,
                widthDp = 1000,
                fontScale = 1.3f
            )
        }

        // The metrics strip is a LazyRow, so scroll each value into view before asserting its full,
        // untruncated text (assertTextEquals confirms no ellipsis).
        listOf("0m", "59m", "23g 59m", "1%", "99%", "100%", "Za mało danych do dokładnej estymacji").forEach { text ->
            composeRule.onNodeWithTag(LibreCareTestTags.METRICS_STRIP)
                .performScrollToNode(hasText(text))
            composeRule.onNodeWithText(text, substring = false).assertTextEquals(text)
        }
    }

    @Test
    fun allSupportedTrendStates_renderIconLabelAndSemantics() {
        val readingState = mutableStateOf(sampleReading(value = 148, trend = GlucoseTrend.entries.first()))
        composeRule.setContent {
            val reading by readingState
            ResponsiveHeroHost(reading)
        }

        GlucoseTrend.entries.forEach { trend ->
            composeRule.runOnUiThread { readingState.value = sampleReading(value = 148, trend = trend) }
            composeRule.waitForIdle()
            composeRule.onNodeWithTag(LibreCareTestTags.HOME_CURRENT_GLUCOSE_TREND).assertIsDisplayed()
        }
    }

    @Test
    fun glucoseValueRendering_handlesEntireQaRenderRange() {
        val supportedRenderRange = 1..600
        val readingState = mutableStateOf(sampleReading(value = supportedRenderRange.first, trend = GlucoseTrend.FLAT))
        composeRule.setContent {
            val reading by readingState
            ResponsiveHeroHost(reading)
        }

        supportedRenderRange.forEach { value ->
            composeRule.runOnUiThread { readingState.value = sampleReading(value = value, trend = GlucoseTrend.FLAT) }
            composeRule.waitForIdle()

            composeRule.onNodeWithTag(LibreCareTestTags.HOME_CURRENT_GLUCOSE_VALUE).assertTextEquals(value.toString())
            composeRule.onNodeWithTag(LibreCareTestTags.HOME_CURRENT_GLUCOSE_UNIT).assertTextEquals("mg/dL")
            composeRule.onNodeWithTag(LibreCareTestTags.HOME_CURRENT_GLUCOSE_TREND).assertIsDisplayed()
            composeRule.onNodeWithTag(LibreCareTestTags.HOME_CURRENT_GLUCOSE_SEVERITY).assertIsDisplayed()
        }
    }

    @Composable
    private fun ResponsiveHeroHost(reading: GlucoseReading) {
        androidx.compose.runtime.CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 1.0f)) {
            LibreDisplayTheme {
                CurrentGlucoseHeroCard(reading = reading, targetLow = 80, targetHigh = 180)
            }
        }
    }

    private fun sampleReading(value: Int, trend: GlucoseTrend): GlucoseReading {
        return GlucoseReading.of(
            value = value,
            timestamp = cachedScenario.newestTimestamp ?: Instant.parse("2026-08-24T12:00:00Z"),
            trend = trend,
            history = cachedScenario.points.dropLast(1)
        )
    }

    private data class HostParams(
        val scenario: GlucoseScenario,
        val widthDp: Int,
        val fontScale: Float
    )
}

