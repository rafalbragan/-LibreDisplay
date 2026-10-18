package com.libredisplay.alert

import androidx.test.core.app.ApplicationProvider
import com.libredisplay.data.model.AppMode
import com.libredisplay.data.model.AppSettings
import com.libredisplay.data.model.GlucoseReading
import com.libredisplay.data.model.GlucoseTrend
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class)
class GlucoseAlertCoordinatorTest {

    @Test
    fun onReading_returnsNullForDemoModeData() {
        val coordinator = createCoordinator()
        val settings = AppSettings(appMode = AppMode.DEMO, selectedPatientId = "demo-person-mika")
        val reading = GlucoseReading.of(
            value = 52,
            timestamp = Instant.now(),
            trend = GlucoseTrend.FALLING_FAST
        )

        val alert = coordinator.onReading(settings, reading)

        assertNull(alert)
    }

    private fun createCoordinator(): GlucoseAlertCoordinator {
        return GlucoseAlertCoordinator(
            alarmPlayer = NoOpAlarmPlayer(),
            tts = NoOpTextToSpeechEngine(),
            vibration = NoOpVibrationController(),
            notifications = AndroidNotificationDispatcher(ApplicationProvider.getApplicationContext())
        )
    }
}

