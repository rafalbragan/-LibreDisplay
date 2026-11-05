package com.libredisplay.data.demo

import android.app.Application
import com.libredisplay.data.model.AppMode
import com.libredisplay.data.model.AppSettings
import com.libredisplay.data.repository.SettingsRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class DemoModeIsolationTest {

    @After
    fun resetScenario() {
        DemoScenarioController.reset()
    }

    @Test
    fun mikaScenarioRejectsLiveAndOtherDemoPatients() = runTest {
        DemoScenarioController.selectScenario(DemoScenario.MIKA_DEMO)
        val client = ScenarioAwareMockLibreLinkUpClient()
        assertNotNull(client.getLatestReading("demo-person-mika"))
        for (patientId in listOf("live-person", "demo-person-anna")) {
            assertThrows(IllegalArgumentException::class.java) {
                kotlinx.coroutines.runBlocking { client.getLatestReading(patientId) }
            }
        }
    }

    @Test
    fun demoToLiveToDemo_keepsDemoSelectionIsolatedFromLivePatientState() {
        val context = RuntimeEnvironment.getApplication()
        val repo = SettingsRepository(context)
        repo.clearAll()

        repo.saveSettings(
            AppSettings(
                appMode = AppMode.DEMO,
                selectedPatientId = "demo-person-mika"
            )
        )
        assertEquals("demo-person-mika", repo.loadSettings().selectedPatientId)

        repo.switchToLiveMode()
        val live = repo.loadSettings()
        assertEquals(AppMode.LIVE, live.appMode)
        assertEquals(null, live.selectedPatientId)

        repo.switchToDemoMode()
        val demoAgain = repo.loadSettings()
        assertEquals(AppMode.DEMO, demoAgain.appMode)
        assertEquals("demo-person-anna", demoAgain.selectedPatientId)
    }
}
