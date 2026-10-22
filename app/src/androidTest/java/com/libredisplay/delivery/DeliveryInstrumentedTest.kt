package com.libredisplay.delivery

import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.os.Build
import com.libredisplay.DeliveryTestActivity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.libredisplay.LibreDisplayApp
import com.libredisplay.data.importer.LibreViewImportRepository
import com.libredisplay.data.local.GlucoseReadingEntity
import com.libredisplay.data.local.ObservedPersonEntity
import com.libredisplay.data.model.AppMode
import com.libredisplay.data.model.AppSettings
import com.libredisplay.ui.monitoring.*
import com.libredisplay.ui.privacy.CsvImportScreen
import com.libredisplay.ui.privacy.CsvImportViewModel
import com.libredisplay.ui.privacy.PrivacyDataScreen
import com.libredisplay.ui.theme.LibreDisplayTheme
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class DeliveryInstrumentedTest {
    @get:Rule val rule = createAndroidComposeRule<DeliveryTestActivity>()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val device get() = UiDevice.getInstance(instrumentation)
    private val app get() = instrumentation.targetContext.applicationContext as LibreDisplayApp
    @Before fun seedSyntheticDataOnly(): Unit = runBlocking {
        check(Build.HARDWARE in listOf("ranchu", "goldfish") && !Build.MODEL.startsWith("SM-"))
        if (device.hasObject(By.text("com.libredisplay.test keeps stopping"))) {
            device.findObject(By.res("android", "aerr_close"))?.click()
        }
        app.settingsRepository.saveSettings(AppSettings(appMode = AppMode.DEMO, backgroundServiceEnabled = false))
        app.database.glucoseReadingDao().deleteAllReadings()
        app.database.observedPersonDao().deleteAllPeople()
        val now = Instant.parse("2026-10-19T10:00:00Z")
        app.database.observedPersonDao().upsertAll(listOf(A, B).map { id ->
            ObservedPersonEntity(id, null, null, if (id == A) "Osoba DEMO A" else "Osoba DEMO B",
                isActive = true, lastSeenAt = now, createdAt = now, updatedAt = now)
        })
        val times = (0..2350).map { now.minusSeconds(it * 86_400L) } + (1..287).map { now.minusSeconds(it * 900L) }
        times.distinct().chunked(400).forEach { chunk ->
            app.database.glucoseReadingDao().insertIgnore(chunk.mapIndexed { i, time ->
                GlucoseReadingEntity("$A:${time.toEpochMilli()}", A, time, 95 + ((time.epochSecond / 900) % 100).toInt(),
                    null, null, source = if (i % 5 == 0) "LibreViewScan" else "DemoMode", receivedAt = now, createdAt = now)
            })
        }
        app.database.glucoseReadingDao().insertIgnore(listOf(GlucoseReadingEntity("$B:one", B, now, 301, null, null,
            source = "DemoMode", receivedAt = now, createdAt = now)))
    }
    @After fun resetOrientation() { device.setOrientationNatural(); device.unfreezeRotation() }

    @Test fun privacyEntryOpensRealImporter() {
        rule.setContent { LibreDisplayTheme { PrivacyDataScreen({}, {}, {}, {}) } }
        rule.onNodeWithText("Import LibreView CSV").assertIsDisplayed().performClick()
        rule.onNodeWithTag("csv-import-screen").assertIsDisplayed()
        screenshot("01-import-profile")
    }

    @Test fun actualDocumentPickerPreviewConfirmationAndRepeatImport() {
        val model = ViewModelProvider(rule.activity)[CsvImportViewModel::class.java]
        rule.setContent { LibreDisplayTheme { CsvImportScreen({}, {}, model) } }
        rule.waitUntil(10_000) { model.state.value.targets.isNotEmpty() }
        rule.runOnIdle { model.newProfile("Profil DEMO import") }
        csvNode("csv-select-file").performClick()
        chooseSyntheticRoot()
        check(device.wait(Until.hasObject(By.text("LibreView-DEMO.csv")), 10_000))
        device.findObject(By.text("LibreView-DEMO.csv")).click()
        rule.waitUntil(15_000) { model.state.value.preview != null || model.state.value.message != null }
        val preview = requireNotNull(model.state.value.preview) { model.state.value.message.orEmpty() }
        assertEquals(5, preview.summary.supported); assertEquals(1, preview.summary.skipped)
        assertEquals(3, preview.counts.newRecords); assertEquals(1, preview.counts.duplicates); assertEquals(1, preview.counts.conflicts)
        assertEquals(0L, runBlocking { app.database.glucoseReadingDao().countLiveReadings() })
        csvNode("csv-confirm-name").performTextInput("Profil DEMO import")
        device.pressBack()
        csvNode("csv-confirm")
        screenshot("02-import-preview")
        rule.onNodeWithTag("csv-confirm").performClick()
        rule.waitUntil(15_000) { model.state.value.result != null }
        assertEquals(3, model.state.value.result!!.inserted)
        screenshot("03-import-complete")
        csvNode("csv-select-file").performClick()
        check(device.wait(Until.hasObject(By.text("LibreView-DEMO.csv")), 10_000))
        device.findObject(By.text("LibreView-DEMO.csv")).click()
        rule.waitUntil(15_000) { model.state.value.preview != null }
        assertEquals(0, model.state.value.preview!!.counts.newRecords)
        assertEquals(4, model.state.value.preview!!.counts.duplicates)
        csvNode("csv-confirm-name").performTextReplacement("Profil DEMO import")
        device.pressBack()
        csvNode("csv-confirm").performClick()
        rule.waitUntil(15_000) { model.state.value.result != null }
        assertEquals(0, model.state.value.result!!.inserted)
        val id = requireNotNull(model.state.value.selected).id
        val before = runBlocking { app.database.glucoseReadingDao().getRangeByPatient(id, Instant.EPOCH, Instant.parse("2027-01-01T00:00:00Z")) }
        runBlocking { app.localGlucoseHistoryRepository.deleteReadingsOlderThanHours(24, Instant.parse("2027-01-01T00:00:00Z")) }
        val after = runBlocking { app.database.glucoseReadingDao().getRangeByPatient(id, Instant.EPOCH, Instant.parse("2027-01-01T00:00:00Z")) }
        assertEquals(3, after.size); assertEquals(before, after)
        assertTrue(after.all { it.importProvenance != null })
    }

    @Test fun actualFolderPickerListsOnlyHeaderVerifiedFiles() {
        val model = ViewModelProvider(rule.activity)[CsvImportViewModel::class.java]
        rule.setContent { LibreDisplayTheme { CsvImportScreen({}, {}, model) } }
        rule.waitUntil(10_000) { model.state.value.targets.isNotEmpty() }
        rule.runOnIdle { model.select(model.state.value.targets.first()) }
        csvNode("csv-select-folder").performClick()
        chooseSyntheticRoot()
        val use = device.wait(Until.findObject(By.text(java.util.regex.Pattern.compile("(?i)use this folder|użyj tego folderu"))), 10_000)
        checkNotNull(use).click()
        device.wait(Until.findObject(By.text(java.util.regex.Pattern.compile("(?i)allow|zezwól"))), 5_000)?.click()
        rule.waitUntil(20_000) { model.state.value.files.isNotEmpty() || (!model.state.value.busy && model.state.value.message != null) }
        assertEquals(listOf("LibreView-DEMO.csv"), model.state.value.files.map { it.name })
        assertEquals(5, model.state.value.files.single().summary.supported)
        rule.onNodeWithText("Niepoprawny-LibreView.csv").assertDoesNotExist()
    }

    @Test fun pinchPanExactPointAndMultiYearRenderingWork() {
        rule.setContent { LibreDisplayTheme { HistoryExplorerScreen({}, A) } }
        waitForChart()
        screenshot("04-chart-day")
        rule.onNodeWithTag("history-range-Całość").performClick()
        waitForChart()
        val full = duration()
        assertTrue(full > 5L * 365 * 86_400_000)
        rule.onNodeWithTag("history-chart").performTouchInput {
            pinch(Offset(width * 0.4f, height * 0.5f), Offset(width * 0.6f, height * 0.5f),
                Offset(width * 0.2f, height * 0.5f), Offset(width * 0.8f, height * 0.5f), 700)
        }
        rule.waitUntil(10_000) { duration() < full }
        waitForChart()
        val oldStart = start()
        rule.onNodeWithTag("history-chart").performTouchInput {
            swipe(Offset(width * 0.4f, height * 0.5f), Offset(width * 0.7f, height * 0.5f), 500)
        }
        rule.waitUntil(10_000) { start() != oldStart }
        waitForChart()
        assertTrue(rule.onNodeWithTag("history-chart").fetchSemanticsNode().config[ExplorerBucketsKey] <= 801)
        screenshot("05-chart-multiyear")
        val from = start(); val until = from + duration()
        val density = rule.activity.resources.displayMetrics.density
        rule.onNodeWithTag("history-chart").performTouchInput { click(Offset((width + 30 * density) / 2, height * 0.5f)) }
        val expected = runBlocking { app.database.glucoseReadingDao().nearestInViewport(A, from, until, from + (until - from) / 2) }!!
        val timestamp = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(ZoneId.systemDefault()).format(expected.timestamp)
        rule.waitUntil(10_000) { rule.onNodeWithTag("history-tooltip").fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.Text].any { it.text.contains(timestamp) } }
        rule.onNodeWithTag("history-tooltip").assertTextContains("${expected.valueMgDl} mg/dL", substring = true)
    }

    @Test fun landscapeKeepsChartVisibleAndPersonSwitchIsIsolated() {
        rule.setContent { LibreDisplayTheme { HistoryExplorerScreen({}, A) } }
        waitForChart()
        rule.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        rule.waitUntil(10_000) { rule.activity.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE }
        rule.onNodeWithTag("history-range-Rok").performClick()
        waitForChart()
        val chart = rule.onNodeWithTag("history-chart").fetchSemanticsNode().boundsInRoot
        assertTrue(chart.height > 80)
        screenshot("06-chart-landscape")
        rule.onNodeWithText("Osoba").performClick()
        rule.onNodeWithText("Osoba DEMO B").performClick()
        waitForChart()
        rule.onNodeWithTag("history-chart").performTouchInput { click(center) }
        rule.waitUntil(10_000) { rule.onNodeWithTag("history-tooltip").fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.Text].any { it.text.contains("301 mg/dL") } }
        assertEquals(1, rule.onNodeWithTag("history-chart").fetchSemanticsNode().config[ExplorerBucketsKey])
    }

    @Test fun initialDashboardRangeIsPreserved() {
        rule.setContent { LibreDisplayTheme { HistoryExplorerScreen({}, A, 7 * 86_400_000L) } }
        waitForChart()
        rule.onNodeWithTag("history-range-Tydzień").assertIsSelected()
        assertEquals(7 * 86_400_000L, duration())
    }

    @Test fun systemBackGestureReturnsFromImporterToPrivacy() {
        rule.setContent { LibreDisplayTheme { PrivacyDataScreen({}, {}, {}, {}) } }
        rule.onNodeWithText("Import LibreView CSV").performClick()
        rule.onNodeWithTag("csv-import-screen").assertIsDisplayed()
        device.swipe(1, device.displayHeight / 2, device.displayWidth / 3, device.displayHeight / 2, 30)
        rule.waitUntil(10_000) { rule.onAllNodesWithTag("csv-import-screen").fetchSemanticsNodes().isEmpty() }
        rule.onNodeWithText("Import LibreView CSV").assertIsDisplayed()
    }

    @Test fun historyBackHandlerRespondsToDeterministicSystemBack() {
        var backCount = 0
        rule.setContent { LibreDisplayTheme { HistoryExplorerScreen({ backCount++ }, A) } }
        waitForChart()

        // Control probe: use the platform back event, not an edge swipe whose
        // recognition depends on the emulator navigation mode and System UI.
        device.pressBack()
        rule.waitUntil(10_000) { backCount == 1 }
        rule.runOnIdle {
            assertEquals("History BackHandler must dispatch exactly once", 1, backCount)
            assertFalse("Activity must stay alive after the callback", rule.activity.isFinishing)
        }
    }

    @Test fun historyBackGestureAndSystemInsetsRemainCorrect() {
        var backCount = 0
        rule.setContent { LibreDisplayTheme { HistoryExplorerScreen({ backCount++ }, A) } }
        waitForChart()
        val decor = rule.activity.window.decorView
        val insets = requireNotNull(androidx.core.view.ViewCompat.getRootWindowInsets(decor))
            .getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars() or
                androidx.core.view.WindowInsetsCompat.Type.displayCutout())
        listOf(rule.onNodeWithText("Wstecz"), rule.onNodeWithTag("history-chart"),
            rule.onNodeWithTag("history-latest")).forEach { node ->
            val bounds = node.fetchSemanticsNode().boundsInWindow
            assertTrue("Control overlaps status bar/cutout", bounds.top >= insets.top - 1)
            assertTrue("Control overlaps navigation bar", bounds.bottom <= decor.height - insets.bottom + 1)
        }
        device.swipe(1, device.displayHeight / 2, device.displayWidth / 3, device.displayHeight / 2, 30)
        rule.waitUntil(10_000) { backCount == 1 }
        rule.runOnIdle { assertFalse(rule.activity.isFinishing) }
    }

    @Test fun darkThemeAndLargeFontKeepHistoryReadable() {
        rule.setContent {
            val config = android.content.res.Configuration(androidx.compose.ui.platform.LocalConfiguration.current).apply {
                uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES
                fontScale = 1.4f
            }
            val density = androidx.compose.ui.platform.LocalDensity.current.density
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.ui.platform.LocalConfiguration provides config,
                androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(density, 1.4f)
            ) { LibreDisplayTheme { HistoryExplorerScreen({}, A) } }
        }
        waitForChart()
        rule.onNodeWithTag("history-chart").assertIsDisplayed()
        rule.onNodeWithTag("history-latest").assertIsDisplayed()
        rule.onNodeWithText("romby: przedziały ze skanami", substring = true).assertIsDisplayed()
        screenshot("07-chart-dark-large-font")
    }

    private fun chooseSyntheticRoot() {
        device.waitForIdle()
        if (device.wait(Until.hasObject(By.text("LibreView-DEMO.csv")), 1_000)) return
        val evidence = File(app.getExternalFilesDir(null), "delivery-screenshots").apply { mkdirs() }
        device.dumpWindowHierarchy(File(evidence, "picker-hierarchy.xml"))
        if (!device.hasObject(By.text("LibreCare DEMO CSV"))) {
            val roots = device.wait(Until.findObject(By.desc(java.util.regex.Pattern.compile("(?i).*roots.*|.*katalog.*|.*pokaż.*|.*show.*"))), 5_000)
                ?: device.findObject(By.clazz("android.widget.ImageButton"))
            checkNotNull(roots) { "System document picker root drawer not found: ${device.currentPackageName}" }.click()
        }
        repeat(3) {
            device.waitForIdle()
            try {
                checkNotNull(device.wait(Until.findObject(By.text("LibreCare DEMO CSV")), 10_000)).click()
                check(device.wait(Until.hasObject(By.text("LibreView-DEMO.csv")), 10_000))
                return
            } catch (_: androidx.test.uiautomator.StaleObjectException) { /* Drawer animation replaced the node. */ }
        }
        error("System picker did not stabilize")
    }
    private fun csvNode(tag: String): SemanticsNodeInteraction {
        rule.onNodeWithTag("csv-import-screen").performScrollToNode(hasTestTag(tag))
        return rule.onNodeWithTag(tag).performScrollTo()
    }
    private fun duration() = rule.onNodeWithTag("history-chart").fetchSemanticsNode().config[ExplorerDurationKey]
    private fun start() = rule.onNodeWithTag("history-chart").fetchSemanticsNode().config[ExplorerStartKey]
    private fun waitForChart() {
        rule.waitUntil(15_000) { rule.onNodeWithTag("history-chart").fetchSemanticsNode().config[ExplorerBucketsKey] > 0 }
    }
    private fun screenshot(name: String) {
        rule.waitForIdle()
        val directory = File(app.getExternalFilesDir(null), "delivery-screenshots").apply { mkdirs() }
        val bitmap = requireNotNull(instrumentation.uiAutomation.takeScreenshot())
        File(directory, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
    companion object { private const val A = "demo-person-delivery-a"; private const val B = "demo-person-delivery-b" }
}








