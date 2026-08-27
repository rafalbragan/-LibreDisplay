package com.libredisplay.ui.analytics

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.libredisplay.LibreDisplayApp
import com.libredisplay.analytics.AnalysisChartFactory
import com.libredisplay.analytics.AnalysisMetricsFactory
import com.libredisplay.analytics.AnalysisTrendInterpreter
import com.libredisplay.analytics.BarChartMode
import com.libredisplay.analytics.BarChartWindow
import com.libredisplay.analytics.FourteenDayOverlay
import com.libredisplay.analytics.PeriodMetrics
import com.libredisplay.analytics.RawDataExcelExporter
import com.libredisplay.data.model.GlucoseHistoryPoint
import com.libredisplay.data.model.LibreConnectionPerson
import com.libredisplay.diagnostics.DiagnosticLogger
import com.libredisplay.ui.monitoring.PolishDateTimeFormatter
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

data class DataAnalysisExportEvent(
    val filePath: String,
    val message: String
)

data class PickerUtcDateRange(
    val startUtcMillis: Long,
    val endUtcMillis: Long
)

internal fun pickerRangeToInstants(range: PickerUtcDateRange, zoneId: ZoneId): Pair<Instant, Instant> {
    val startDate = Instant.ofEpochMilli(range.startUtcMillis).atZone(ZoneOffset.UTC).toLocalDate()
    val endDate = Instant.ofEpochMilli(range.endUtcMillis).atZone(ZoneOffset.UTC).toLocalDate()
    val start = startDate.atStartOfDay(zoneId).toInstant()
    val end = endDate.plusDays(1).atStartOfDay(zoneId).toInstant().minusSeconds(1)
    return start to end
}

enum class AnalysisPeriod(val label: String, val duration: Duration?) {
    H1("1g", Duration.ofHours(1)),
    H3("3g", Duration.ofHours(3)),
    H6("6g", Duration.ofHours(6)),
    H24("24g", Duration.ofHours(24)),
    D7("7d", Duration.ofDays(7)),
    D30("30d", Duration.ofDays(30)),
    CUSTOM("Własny", null)
}

data class DataAnalysisUiState(
    val persons: List<LibreConnectionPerson> = emptyList(),
    val selectedPatientId: String? = null,
    val selectedPersonName: String? = null,
    val customStart: Instant? = null,
    val customEnd: Instant? = null,
    val customRangeLabel: String = "Zakres własny: —",
    val metricsByPeriod: Map<AnalysisPeriod, PeriodMetrics> = emptyMap(),
    val barMode: BarChartMode = BarChartMode.DAILY,
    val monthlyAvailable: Boolean = false,
    val barWindow: BarChartWindow? = null,
    val selectedBarIndex: Int = -1,
    val overlay: FourteenDayOverlay = FourteenDayOverlay(emptyList(), emptyList()),
    val trendObservations: List<String> = emptyList(),
    val nightOnlyEnabled: Boolean = false,
    val isLoading: Boolean = false,
    val infoMessage: String? = null,
    /** Number of days used for the overlay chart (1–90). Default is 14. */
    val analysisPeriodDays: Int = 14,
    /** True when the overlay window can be scrolled further into the past. */
    val canNavigateBackward: Boolean = false,
    /** True when the overlay window has been shifted and can move forward to today. */
    val canNavigateForward: Boolean = false,
    /** Saved horizontal scroll offset of the metrics table (px). */
    val metricsScrollOffset: Int = 0,
    /** Human-readable label for the current overlay date range, e.g. "24.08 — 06.09 (14 dni)". */
    val overlayRangeLabel: String = ""
)

class DataAnalysisViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as LibreDisplayApp
    private val localRepository = app.localGlucoseHistoryRepository
    private val settingsRepository = app.settingsRepository

    private val _uiState = MutableStateFlow(DataAnalysisUiState())
    val uiState: StateFlow<DataAnalysisUiState> = _uiState.asStateFlow()

    private val _exportEvent = MutableStateFlow<DataAnalysisExportEvent?>(null)
    val exportEvent: StateFlow<DataAnalysisExportEvent?> = _exportEvent.asStateFlow()

    /**
     * Prevents a failure while switching person / recomputing charts from reaching the process-wide
     * crash handler (which would close the app). Instead it is logged and surfaced as an info message.
     */
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        DiagnosticLogger.logException("DataAnalysisViewModel", throwable, "Analiza – nieobsłużony błąd")
        _uiState.update {
            it.copy(
                isLoading = false,
                infoMessage = "Nie udało się wczytać danych analizy. Spróbuj ponownie."
            )
        }
    }

    /** In-memory buffer (last ~400 days) so scrolling/zooming the bars never re-hits the DB. */
    private var analysisBuffer: List<GlucoseHistoryPoint> = emptyList()
    private var barOffsetDays: Int = 0
    private var barOffsetMonths: Int = 0
    /** How many days the overlay window is shifted into the past relative to today (0 = today). */
    private var overlayOffsetDays: Int = 0

    private data class ChartsResult(
        val mode: BarChartMode,
        val monthlyAvailable: Boolean,
        val window: BarChartWindow,
        val overlay: FourteenDayOverlay,
        val trends: List<String>,
        val canNavigateBackward: Boolean,
        val canNavigateForward: Boolean,
        val overlayRangeLabel: String
    )

    init {
        reload()
    }

    fun reload() {
        viewModelScope.launch(exceptionHandler) {
            _uiState.update { it.copy(isLoading = true, infoMessage = null) }
            val settings = settingsRepository.loadSettings()
            val snapshot = app.glucoseSyncRepository.loadLatestLocalSnapshot(settings.selectedPatientId)
            val persons = snapshot?.persons.orEmpty()
            val selectedPerson = snapshot?.selectedPerson ?: persons.firstOrNull()
            val selectedPatientId = selectedPerson?.patientId
            val selectedName = selectedPerson?.displayName
            val storedRange = selectedPatientId?.let { localRepository.loadStoredRange(it) }
            val customStart = storedRange?.oldest
            val customEnd = storedRange?.newest
            _uiState.update {
                it.copy(
                    persons = persons,
                    selectedPatientId = selectedPatientId,
                    selectedPersonName = selectedName,
                    customStart = customStart,
                    customEnd = customEnd,
                    customRangeLabel = customLabel(customStart, customEnd),
                    infoMessage = if (persons.isEmpty()) "Brak lokalnych danych. Poczekaj na synchronizację." else null
                )
            }
            loadBuffer(selectedPatientId)
            recalculate()
            recomputeCharts()
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun onPersonSelected(patientId: String) {
        viewModelScope.launch(exceptionHandler) {
            _uiState.update { it.copy(isLoading = true) }
            val selected = _uiState.value.persons.firstOrNull { it.patientId == patientId }
            val storedRange = localRepository.loadStoredRange(patientId)
            barOffsetDays = 0
            barOffsetMonths = 0
            overlayOffsetDays = 0
            _uiState.update {
                it.copy(
                    selectedPatientId = patientId,
                    selectedPersonName = selected?.displayName ?: it.selectedPersonName,
                    customStart = storedRange?.oldest,
                    customEnd = storedRange?.newest,
                    customRangeLabel = customLabel(storedRange?.oldest, storedRange?.newest),
                    selectedBarIndex = -1
                )
            }
            loadBuffer(patientId)
            recalculate()
            recomputeCharts()
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun onNightOnlyChanged(enabled: Boolean) {
        _uiState.update { it.copy(nightOnlyEnabled = enabled, selectedBarIndex = -1) }
        viewModelScope.launch(exceptionHandler) { recomputeCharts() }
    }

    fun onBarModeChanged(mode: BarChartMode) {
        barOffsetDays = 0
        barOffsetMonths = 0
        _uiState.update { it.copy(barMode = mode, selectedBarIndex = -1) }
        viewModelScope.launch(exceptionHandler) { recomputeCharts() }
    }

    /** Positive = older, negative = newer. Unit is days (DAILY) or months (MONTHLY). */
    fun onBarScroll(deltaUnits: Int) {
        val now = Instant.now()
        if (_uiState.value.barMode == BarChartMode.DAILY) {
            val max = AnalysisChartFactory.maxDailyOffset(analysisBuffer, now)
            barOffsetDays = (barOffsetDays + deltaUnits).coerceIn(0, max)
        } else {
            val max = AnalysisChartFactory.maxMonthlyOffset(analysisBuffer, now)
            barOffsetMonths = (barOffsetMonths + deltaUnits).coerceIn(0, max)
        }
        _uiState.update { it.copy(selectedBarIndex = -1) }
        viewModelScope.launch(exceptionHandler) { recomputeCharts() }
    }

    fun onBarSelected(index: Int) {
        _uiState.update { it.copy(selectedBarIndex = if (it.selectedBarIndex == index) -1 else index) }
    }

    fun onCustomRangeSelected(range: PickerUtcDateRange) {
        val zone = ZoneId.systemDefault()
        val (start, end) = pickerRangeToInstants(range, zone)
        if (end.isBefore(start)) {
            _uiState.update { it.copy(infoMessage = "Nieprawidłowy zakres dat.") }
            return
        }
        _uiState.update {
            it.copy(
                customStart = start,
                customEnd = end,
                customRangeLabel = customLabel(start, end),
                infoMessage = null
            )
        }
        viewModelScope.launch(exceptionHandler) { recalculate() }
    }

    /**
     * Updates the number of days shown in the overlay chart. Clamped to [1, 90].
     * Triggers a chart recomputation with the new period.
     */
    fun onAnalysisPeriodChanged(days: Int) {
        val clamped = days.coerceIn(1, 90)
        if (clamped == _uiState.value.analysisPeriodDays) return
        _uiState.update { it.copy(analysisPeriodDays = clamped) }
        viewModelScope.launch(exceptionHandler) { recomputeCharts() }
    }

    /**
     * Shifts the overlay window by [deltaDays] days (positive = older, negative = newer).
     * Clamps the offset so the window stays within available data and never goes into the future.
     */
    fun onNavigateAnalysisPeriod(deltaDays: Int) {
        val now = Instant.now()
        val maxOffset = AnalysisChartFactory.maxDailyOffset(
            analysisBuffer, now, _uiState.value.analysisPeriodDays
        )
        overlayOffsetDays = (overlayOffsetDays + deltaDays).coerceIn(0, maxOffset)
        viewModelScope.launch(exceptionHandler) { recomputeCharts() }
    }

    /** Resets the overlay window to end at today. */
    fun onResetOverlayToToday() {
        overlayOffsetDays = 0
        viewModelScope.launch(exceptionHandler) { recomputeCharts() }
    }

    /** Saves the horizontal scroll offset of the metrics table so it can be restored on re-entry. */
    fun onMetricsScrollChanged(offset: Int) {
        _uiState.update { it.copy(metricsScrollOffset = offset) }
    }

    fun exportRawDataToExcel() {
        viewModelScope.launch(exceptionHandler) {
            val state = _uiState.value
            val patientId = state.selectedPatientId
            if (patientId.isNullOrBlank()) {
                _uiState.update { it.copy(infoMessage = "Wybierz osobę przed eksportem danych.") }
                return@launch
            }

            _uiState.update { it.copy(isLoading = true) }
            runCatching {
                val range = localRepository.loadStoredRange(patientId)
                    ?: error("Brak danych do eksportu.")
                val customStart = state.customStart
                val customEnd = state.customEnd
                val from = customStart ?: range.oldest
                val to = customEnd ?: range.newest
                val readings = localRepository.loadHistory(patientId, from, to)
                if (readings.isEmpty()) error("Brak danych do eksportu.")

                val personName = state.selectedPersonName ?: patientId
                val exportDir = java.io.File(getApplication<Application>().filesDir, "exports").apply { mkdirs() }
                val stamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss", Locale.US)
                    .withZone(ZoneId.systemDefault())
                    .format(Instant.now())
                val safeName = personName
                    .replace("[^a-zA-Z0-9_-]".toRegex(), "-")
                    .trim('-')
                    .ifBlank { "osoba" }
                val file = java.io.File(exportDir, "LibreCare-analiza-${safeName}-${stamp}.xlsx")
                RawDataExcelExporter.writeRawDataWorkbook(
                    destination = file,
                    personDisplayName = personName,
                    patientId = patientId,
                    readings = readings
                )
                file
            }.onSuccess { file ->
                _exportEvent.value = DataAnalysisExportEvent(
                    filePath = file.absolutePath,
                    message = "Eksport zapisany: ${file.name}"
                )
                _uiState.update { it.copy(isLoading = false, infoMessage = "Plik gotowy do udostępnienia.") }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        infoMessage = "Nie udało się wyeksportować danych: ${error.message.orEmpty()}"
                    )
                }
            }
        }
    }

    fun consumeExportEvent() {
        _exportEvent.value = null
    }

    private suspend fun loadBuffer(patientId: String?) {
        analysisBuffer = if (patientId == null) {
            emptyList()
        } else {
            runCatching {
                val now = Instant.now()
                localRepository.loadHistory(patientId, now.minus(400, ChronoUnit.DAYS), now)
            }.getOrElse { throwable ->
                DiagnosticLogger.logException(
                    "DataAnalysisViewModel",
                    throwable,
                    "loadBuffer failed for ${patientId.take(6)}"
                )
                _uiState.update { it.copy(infoMessage = "Nie udało się wczytać pełnej historii do analizy.") }
                emptyList()
            }
        }
    }

    private suspend fun recomputeCharts() {
        val state = _uiState.value
        val patientId = state.selectedPatientId
        if (patientId == null) {
            _uiState.update {
                it.copy(
                    barWindow = null,
                    overlay = FourteenDayOverlay(emptyList(), emptyList()),
                    trendObservations = emptyList(),
                    monthlyAvailable = false,
                    canNavigateBackward = false,
                    canNavigateForward = false,
                    overlayRangeLabel = ""
                )
            }
            return
        }
        val settings = settingsRepository.loadSettings()
        val buffer = analysisBuffer
        val nightOnly = state.nightOnlyEnabled
        val requestedMode = state.barMode
        val offsetDays = barOffsetDays
        val offsetMonths = barOffsetMonths
        val periodDays = state.analysisPeriodDays
        val overlayOffset = overlayOffsetDays
        val result = withContext(Dispatchers.Default) {
            val now = Instant.now()
            val zone = ZoneId.systemDefault()
            val monthlyAvailable = AnalysisChartFactory.hasMonthlyData(buffer, now)
            val effectiveMode = if (requestedMode == BarChartMode.MONTHLY && !monthlyAvailable) BarChartMode.DAILY else requestedMode
            val window = if (effectiveMode == BarChartMode.MONTHLY) {
                AnalysisChartFactory.monthlyWindow(
                    buffer, now, offsetMonths, 12,
                    settings.targetLow, settings.targetHigh, 54, 250, nightOnly
                )
            } else {
                AnalysisChartFactory.dailyWindow(
                    buffer, now, offsetDays, 14,
                    settings.targetLow, settings.targetHigh, 54, 250, nightOnly
                )
            }

            // Overlay window — independent of the bar chart window; respects analysisPeriodDays
            val overlayEndDate = now.atZone(zone).toLocalDate().minusDays(overlayOffset.toLong())
            val overlayStartDate = overlayEndDate.minusDays((periodDays - 1).toLong())
            val overlayWindowStart = overlayStartDate.atStartOfDay(zone).toInstant()
            val overlayWindowEnd = overlayEndDate.plusDays(1).atStartOfDay(zone).toInstant()

            val overlay = AnalysisChartFactory.overlayForWindow(buffer, overlayWindowStart, overlayWindowEnd, zone, periodDays)
            val trends = AnalysisTrendInterpreter.interpret(overlay, settings.targetLow, settings.targetHigh)

            val oldest = buffer.minByOrNull { it.timestamp }?.timestamp
            val canBack = oldest != null && oldest.isBefore(overlayWindowStart)
            val canForward = overlayOffset > 0
            val startLbl = "%02d.%02d".format(overlayStartDate.dayOfMonth, overlayStartDate.monthValue)
            val endLbl = "%02d.%02d".format(overlayEndDate.dayOfMonth, overlayEndDate.monthValue)
            val rangeLabel = "$startLbl — $endLbl ($periodDays dni)"

            ChartsResult(effectiveMode, monthlyAvailable, window, overlay, trends, canBack, canForward, rangeLabel)
        }
        _uiState.update {
            it.copy(
                barMode = result.mode,
                monthlyAvailable = result.monthlyAvailable,
                barWindow = result.window,
                overlay = result.overlay,
                trendObservations = result.trends,
                canNavigateBackward = result.canNavigateBackward,
                canNavigateForward = result.canNavigateForward,
                overlayRangeLabel = result.overlayRangeLabel
            )
        }
    }

    private suspend fun recalculate() {
        val state = _uiState.value
        val patientId = state.selectedPatientId ?: run {
            _uiState.update {
                it.copy(
                    metricsByPeriod = AnalysisPeriod.entries.associateWith { PeriodMetrics.empty },
                    infoMessage = "Brak wybranej osoby do analizy."
                )
            }
            return
        }
        val settings = settingsRepository.loadSettings()
        val buffer = analysisBuffer
        val customStart = state.customStart
        val customEnd = state.customEnd
        val matrix = withContext(Dispatchers.Default) {
            val now = Instant.now()
            AnalysisPeriod.entries.associateWith { period ->
                val (periodStart, periodEnd) =
                    if (period == AnalysisPeriod.CUSTOM && customStart != null && customEnd != null) {
                        customStart to customEnd
                    } else {
                        now.minus(period.duration ?: Duration.ofDays(30)) to now
                    }
                val scoped = buffer.filter { !it.timestamp.isBefore(periodStart) && !it.timestamp.isAfter(periodEnd) }
                AnalysisMetricsFactory.calculate(scoped, periodStart, periodEnd, settings.targetLow, settings.targetHigh, 54, 250)
            }
        }
        _uiState.update {
            it.copy(
                metricsByPeriod = matrix,
                infoMessage = if (matrix.values.all { m -> m.readingsCount == 0 }) "Brak danych dla wybranych okresów." else null
            )
        }
    }

    private fun customLabel(start: Instant?, end: Instant?): String =
        if (start != null && end != null) {
            "Zakres własny: ${PolishDateTimeFormatter.formatRangeLabel(start, end).removePrefix("Zakres: ")}"
        } else {
            "Zakres własny: —"
        }
}

