package com.libredisplay.ui.monitoring

import android.graphics.Bitmap
import android.os.Build
import android.os.Process
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.libredisplay.LibreDisplayApp
import com.libredisplay.MainActivity
import kotlinx.coroutines.CompletableDeferred
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class P0PersonRecoveryInstrumentedTest {
    @get:Rule val compose = createEmptyComposeRule()
    private lateinit var app: LibreDisplayApp
    private lateinit var fixture: P0RecoveryFixture
    private var scenario: ActivityScenario<MainActivity>? = null
    private lateinit var vm: MonitoringViewModel

    @Before fun setUp() {
        check(Build.MODEL.startsWith("sdk_gphone") && Build.HARDWARE == "ranchu") {
            "This destructive-process test is emulator-only; physical devices are forbidden."
        }
        app = ApplicationProvider.getApplicationContext()
        fixture = P0RecoveryFixture(app)
    }

    @After fun closeActivity() { scenario?.close() }

    @Test fun inactiveSelection_canReturnWithoutMisattribution() {
        fixture.seedActiveSession()
        launch()
        awaitActive()
        selectInactive()
        awaitNoData()
        assertNotEquals(P0RecoveryFixture.INACTIVE, app.settingsRepository.loadSettings().selectedPatientId)
        capture("inactive-no-data")
        compose.onNodeWithText("Aktywna Testowa").performClick()
        awaitActive()
        capture("active-restored")
        selectInactive()
        awaitNoData()
        // Leave the actual failed choice in the UI for the external process termination phase.
        marker("normal-restart")
    }

    @Test fun afterProcessRestart_lastGoodSelectionStarts() {
        verifyNewProcess("normal-restart")
        assertNotEquals(P0RecoveryFixture.INACTIVE, app.settingsRepository.loadSettings().selectedPatientId)
        launch()
        awaitActive()
        capture("process-restarted")
    }

    @Test fun prepareLegacyInactiveSelectionForProcessRestart() {
        fixture.seedActiveSession()
        launch()
        awaitActive()
        selectInactive()
        compose.waitUntil(15_000) { !vm.uiState.value.isLoading }
        // Simulate an already-installed old version that persisted the failed selection.
        app.settingsRepository.saveSelectedPatientId(P0RecoveryFixture.INACTIVE)
        marker("legacy-restart")
    }

    @Test fun afterProcessRestart_legacyInactiveSelectionIsRecoverable() {
        verifyNewProcess("legacy-restart")
        assertEquals(P0RecoveryFixture.INACTIVE, app.settingsRepository.loadSettings().selectedPatientId)
        launch()
        awaitNoData()
        assertNotEquals(P0RecoveryFixture.INACTIVE, app.settingsRepository.loadSettings().selectedPatientId)
        assertTrue(app.settingsRepository.hasPersistedSessionData())
        capture("legacy-startup-no-data")
        compose.onNodeWithText("Aktywna Testowa").performClick()
        awaitActive()
    }

    @Test fun stalePersonNeverDisplaysActivePersonsReading() {
        fixture.seedActiveSession()
        fixture.http.inactiveHasStaleReading = true
        launch()
        awaitActive()
        marker("stale-restart")
        selectInactive()
        compose.waitUntil(15_000) { vm.uiState.value.reading?.value == 231 && !vm.uiState.value.isLoading }
        assertEquals(P0RecoveryFixture.INACTIVE, vm.uiState.value.selectedPatientId)
        assertTrue(vm.uiState.value.reading!!.history.all { it.value == 231 })
        compose.onAllNodesWithText("231", useUnmergedTree = true).onFirst().assertIsDisplayed()
        val warning = buildGlucoseStatusPresentation(vm.uiState.value.reading).freshnessWarning!!
        compose.onAllNodesWithText(warning.title, useUnmergedTree = true).onFirst().assertIsDisplayed()
        capture("inactive-stale")
        compose.onNodeWithText("Aktywna Testowa").performClick()
        awaitActive()
        selectInactive()
        compose.waitUntil(15_000) { vm.uiState.value.reading?.value == 231 && !vm.uiState.value.isLoading }
    }

    @Test fun afterProcessRestart_staleSelectionDoesNotCrash() {
        verifyNewProcess("stale-restart")
        assertEquals(P0RecoveryFixture.INACTIVE, app.settingsRepository.loadSettings().selectedPatientId)
        fixture.http.inactiveHasStaleReading = true
        launch()
        compose.waitUntil(15_000) {
            fixture.http.graphRequests.contains(P0RecoveryFixture.INACTIVE) && vm.uiState.value.reading?.value == 231 && !vm.uiState.value.isLoading
        }
        compose.onAllNodesWithText("231", useUnmergedTree = true).onFirst().assertIsDisplayed()
        capture("stale-process-restarted")
        compose.onNodeWithText("Aktywna Testowa").performClick()
        awaitActive()
    }

    @Test fun pendingPolling_cannotHideSwitcherOrOverwriteLatestChoice() {
        fixture.seedActiveSession()
        launch()
        awaitActive()
        val gate = CompletableDeferred<Unit>()
        fixture.http.gatedPatient = P0RecoveryFixture.ACTIVE
        fixture.http.graphGate = gate
        val requestsBefore = fixture.http.graphRequests.size
        scenario!!.onActivity { vm.refreshNow() }
        compose.waitUntil(15_000) { fixture.http.graphRequests.size > requestsBefore }
        selectInactive()
        compose.onNodeWithText("Aktywna Testowa").assertIsDisplayed().performClick()
        selectInactive()
        gate.complete(Unit)
        awaitNoData()
        capture("rapid-switch-no-misattribution")
    }

    private fun launch() {
        scenario = ActivityScenario.launch(MainActivity::class.java).also {
            it.onActivity { activity -> vm = ViewModelProvider(activity)[MonitoringViewModel::class.java] }
        }
    }

    private fun selectInactive() {
        compose.onNodeWithText("Nieaktywna Testowa").assertIsDisplayed().performClick()
        compose.waitUntil(15_000) { vm.uiState.value.selectedPatientId == P0RecoveryFixture.INACTIVE }
    }

    private fun awaitActive() {
        compose.waitUntil(15_000) {
            vm.uiState.value.let { it.selectedPatientId == P0RecoveryFixture.ACTIVE && it.reading?.value == 147 && !it.isLoading }
        }
        assertTrue(vm.uiState.value.reading!!.history.size >= 49)
        assertTrue(vm.uiState.value.reading!!.history.all { it.value == 147 })
        compose.onNodeWithText("Aktywna Testowa").assertIsDisplayed()
        compose.onAllNodesWithText("147", useUnmergedTree = true).onFirst().assertIsDisplayed()
    }

    private fun awaitNoData() {
        try {
            compose.waitUntil(15_000) {
                fixture.http.graphRequests.contains(P0RecoveryFixture.INACTIVE) && !vm.uiState.value.isLoading
            }
        } catch (timeout: androidx.compose.ui.test.ComposeTimeoutException) {
            throw AssertionError(
                "No-data transition: selected=${vm.uiState.value.selectedPatientId}, " +
                    "connection=${vm.uiState.value.connectionState}, loading=${vm.uiState.value.isLoading}, " +
                    "sessionPresent=${app.settingsRepository.hasPersistedSessionData()}, " +
                    "requests=${fixture.http.graphRequests}", timeout
            )
        }
        assertEquals(P0RecoveryFixture.INACTIVE, vm.uiState.value.selectedPatientId)
        assertNull("Another person's reading must never remain under the inactive identity", vm.uiState.value.reading)
        assertTrue(vm.homeHistory.value.isEmpty())
        assertTrue(vm.detailedHistory.value.isEmpty())
        compose.onNodeWithText("Aktywna Testowa").assertIsDisplayed()
        compose.onNodeWithText("Brak aktualnych danych dla wybranej osoby.").assertIsDisplayed()
        compose.onAllNodesWithText("147", useUnmergedTree = true).assertCountEquals(0)
    }

    private fun marker(name: String) {
        assertTrue("The session must exist before process termination", app.settingsRepository.hasPersistedSessionData())
        app.getSharedPreferences("p0-process-proof", 0).edit()
            .putInt(name, Process.myPid()).commit().also { assertTrue(it) }
    }

    private fun verifyNewProcess(name: String) {
        val previousPid = app.getSharedPreferences("p0-process-proof", 0).getInt(name, -1)
        assertTrue("Run the preparation phase first", previousPid > 0)
        assertNotEquals("Activity recreation is NOT process-death coverage", previousPid, Process.myPid())
        assertTrue("The synthetic session must survive process termination", app.settingsRepository.hasPersistedSessionData())
    }

    private fun capture(name: String) {
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        val dir = File(app.getExternalFilesDir(null), "p0-evidence").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
}







