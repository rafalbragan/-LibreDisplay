package com.libredisplay.ui.monitoring

import android.app.Application
import com.github.takahirom.roborazzi.captureRoboImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.libredisplay.testing.scenario.GlucoseScenario
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Golden / screenshot tests for the LibreCare dashboard.
 *
 * These run entirely on the JVM through Robolectric NATIVE graphics + Roborazzi, so they need no
 * device or emulator. Baselines live under `app/src/test/screenshots/`.
 *
 * Workflow:
 * - Record baselines:  `./gradlew :app:recordRoborazziDebug`
 * - Verify (fails + writes expected/actual/diff on mismatch): `./gradlew :app:verifyRoborazziDebug`
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, sdk = [34])
class HomeScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun capture(name: String, scenario: GlucoseScenario, widthDp: Int, fontScale: Float) {
        composeRule.setContent {
            MonitoringHomeTestHost(scenario = scenario, widthDp = widthDp, fontScale = fontScale)
        }
        composeRule.onRoot().captureRoboImage("src/test/screenshots/$name.png")
    }

    @Test
    fun home_normal_360() = capture("Home_Normal_360", GlucoseScenario.NORMAL_STABLE, widthDp = 360, fontScale = 1.0f)

    @Test
    fun home_normal_384() = capture("Home_Normal_384", GlucoseScenario.NORMAL_STABLE, widthDp = 384, fontScale = 1.0f)

    @Test
    fun home_font130() = capture("Home_Font130", GlucoseScenario.NORMAL_STABLE, widthDp = 384, fontScale = 1.3f)

    @Test
    fun home_font150() = capture("Home_Font150", GlucoseScenario.NORMAL_STABLE, widthDp = 411, fontScale = 1.5f)

    @Test
    fun home_veryHigh() = capture("Home_VeryHigh", GlucoseScenario.VERY_HIGH_RISING, widthDp = 384, fontScale = 1.0f)

    @Test
    fun home_low() = capture("Home_Low", GlucoseScenario.LOW_FALLING, widthDp = 384, fontScale = 1.0f)

    @Test
    fun home_stale() = capture("Home_Stale", GlucoseScenario.STALE_DATA, widthDp = 384, fontScale = 1.0f)

    @Test
    fun home_noData() = capture("Home_NoData", GlucoseScenario.NO_DATA, widthDp = 384, fontScale = 1.0f)
}

