package com.libredisplay.data.importer

import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import com.libredisplay.LibreDisplayApp
import com.libredisplay.data.local.GlucoseReadingEntity
import com.libredisplay.data.local.LibreDisplayDatabase
import com.libredisplay.data.local.ObservedPersonEntity
import com.libredisplay.data.model.AppMode
import com.libredisplay.data.model.AppSettings
import com.libredisplay.data.repository.LocalGlucoseHistoryRepository
import com.libredisplay.data.repository.SettingsRepository
import com.libredisplay.ui.analytics.AnalysisPeriod
import com.libredisplay.ui.analytics.DataAnalysisViewModel
import com.libredisplay.ui.privacy.CsvImportViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File
import java.time.Instant

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class ImportedHistoryViewModelTest {
    private lateinit var app: LibreDisplayApp
    private lateinit var db: LibreDisplayDatabase
    private val store = ViewModelStore()
    @Before fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val context = RuntimeEnvironment.getApplication()
        app = LibreDisplayApp()
        ContextWrapper::class.java.getDeclaredMethod("attachBaseContext", Context::class.java).apply { isAccessible = true }.invoke(app, context)
        db = Room.inMemoryDatabaseBuilder(context, LibreDisplayDatabase::class.java).allowMainThreadQueries().build()
        val settings = SettingsRepository(context).apply { clearAll(); saveSettings(AppSettings(appMode = AppMode.LIVE)) }
        inject("database", db); inject("settingsRepository", settings)
        inject("localGlucoseHistoryRepository", LocalGlucoseHistoryRepository(db.observedPersonDao(), db.glucoseReadingDao(), db.syncRunDao()))
    }
    @After fun close() { store.clear(); db.close(); Dispatchers.resetMain() }
    private fun inject(name: String, value: Any) { LibreDisplayApp::class.java.getDeclaredField(name).apply { isAccessible = true }.set(app, value) }
    private suspend fun await(predicate: () -> Boolean) { withTimeout(10_000) { while (!predicate()) delay(10) } }
    @Test fun inactiveImportedProfileAndSixYearCustomRangeAreAvailableInAnalysis() = runBlocking {
        val first = Instant.parse("2020-05-01T00:00:00Z"); val last = Instant.parse("2026-10-01T00:00:00Z")
        db.observedPersonDao().upsertAll(listOf(ObservedPersonEntity("local-csv-demo", null, null, "Profil testowy", isActive = false,
            lastSeenAt = last, createdAt = first, updatedAt = last)))
        db.glucoseReadingDao().insertIgnore(listOf(first, last).map { GlucoseReadingEntity("local:$it", "local-csv-demo", it, 123, null, null,
            source = "LibreViewCSV", receivedAt = last, createdAt = last) })
        val model = DataAnalysisViewModel(app).also { store.put("analysis", it) }
        await { !model.uiState.value.isLoading && model.uiState.value.metricsByPeriod.isNotEmpty() }
        assertEquals("local-csv-demo", model.uiState.value.selectedPatientId)
        assertEquals(2, model.uiState.value.metricsByPeriod[AnalysisPeriod.CUSTOM]!!.readingsCount)
        assertEquals(first, model.uiState.value.customStart)
    }
    @Test fun importViewModelRequiresPreviewAndExplicitNameBeforeWriting() = runBlocking {
        val file = File(app.cacheDir, "synthetic-import-vm.csv")
        file.writeText("Dane glukozy,DEMO\r\nUrządzenie,Numer seryjny,Znacznik czasu urządzenia,Typ zapisu,Historyczne stężenie glukozy mg/dL,Zeskanowane stężenie glukozy mg/dL\r\nLibre,synthetic,01-05-2020 10:00,0,110,")
        try {
            val model = CsvImportViewModel(app).also { store.put("import", it) }
            model.newProfile("DEMO")
            model.preview(Uri.fromFile(file))
            await { !model.state.value.busy }
            assertNotNull(model.state.value.preview)
            assertEquals(0L, db.glucoseReadingDao().countAllReadings())
            model.confirm("wrong name")
            await { !model.state.value.busy }
            assertEquals(0L, db.glucoseReadingDao().countAllReadings())
            model.confirm("DEMO")
            await { !model.state.value.busy }
            assertEquals(1, model.state.value.result!!.inserted)
        } finally { file.delete() }
    }
}

