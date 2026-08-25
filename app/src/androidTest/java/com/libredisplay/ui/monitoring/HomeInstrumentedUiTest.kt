package com.libredisplay.ui.monitoring

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.libredisplay.data.model.GlucoseReading
import com.libredisplay.data.model.GlucoseTrend
import com.libredisplay.testing.scenario.GlucoseScenario
import com.libredisplay.testing.scenario.GlucoseScenarioEngine
import com.libredisplay.ui.theme.LibreDisplayTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.Instant

/**
 * On-device Compose UI test for the dashboard current-glucose card.
 *
 * This exercises the real Compose UI testing pipeline (testTag + semantics + assertions) on an
 * instrumented device/emulator. It is compiled into the debug androidTest APK. When no device or
 * emulator is available it is reported as SKIPPED_NEEDS_DEVICE - it is never silently passed.
 */
@RunWith(AndroidJUnit4::class)
class HomeInstrumentedUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun reading(value: Int, trend: GlucoseTrend): GlucoseReading {
        val dataset = GlucoseScenarioEngine.dataset(GlucoseScenario.FULL_24H_DATA)
        return GlucoseReading.of(
            value = value,
            timestamp = dataset.newestTimestamp ?: Instant.parse("2026-08-24T12:00:00Z"),
            trend = trend,
            history = dataset.points.dropLast(1)
        )
    }

    @Test
    fun currentGlucoseCard_rendersValueUnitTrendAndSeverity() {
        composeRule.setContent {
            LibreDisplayTheme {
                CurrentGlucoseHeroCard(reading = reading(148, GlucoseTrend.RISING), targetLow = 80, targetHigh = 180)
            }
        }

        composeRule.onNodeWithTag(LibreCareTestTags.HOME_CURRENT_GLUCOSE_VALUE).assertTextEquals("148")
        composeRule.onNodeWithTag(LibreCareTestTags.HOME_CURRENT_GLUCOSE_UNIT).assertTextEquals("mg/dL")
        composeRule.onNodeWithTag(LibreCareTestTags.HOME_CURRENT_GLUCOSE_TREND).assertIsDisplayed()
        composeRule.onNodeWithTag(LibreCareTestTags.HOME_CURRENT_GLUCOSE_SEVERITY).assertIsDisplayed()
    }
}

