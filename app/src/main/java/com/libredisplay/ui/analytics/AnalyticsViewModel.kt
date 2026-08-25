package com.libredisplay.ui.analytics

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.libredisplay.LibreDisplayApp
import com.libredisplay.analytics.AnalysisChartFactory
import com.libredisplay.analytics.AnalysisMetricsFactory
import com.libredisplay.analytics.FourteenDayOverlay
import com.libredisplay.analytics.PeriodMetrics
import com.libredisplay.analytics.RawDataExcelExporter
import com.libredisplay.analytics.WeeklyRangeBar
import com.libredisplay.data.model.LibreConnectionPerson
import com.libredisplay.ui.monitoring.PolishDateTimeFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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
    val weeklyBars: List<WeeklyRangeBar> = emptyList(),
    val overlay14Days: FourteenDayOverlay = FourteenDayOverlay(emptyList(), emptyList()),
    val nightOnlyEnabled: Boolean = false,
    val isLoading: Boolean = false,
    val infoMessage: String? = null
)

class DataAnalysisViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as LibreDisplayApp
    private val localRepository = app.localGlucoseHistoryRepository
    private val settingsRepository = app.settingsRepository

    private val _uiState = MutableStateFlow(DataAnalysisUiState())
    val uiState: StateFlow<DataAnalysisUiState> = _uiState.asStateFlow()

    private val _exportEvent = MutableStateFlow<DataAnalysisExportEvent?>(null)
    val exportEvent: StateFlow<DataAnalysisExportEvent?> = _exportEvent.asStateFlow()

    init {
        reload()
    }

    fun reload() {
        viewModelScope.launch {
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
                    customRangeLabel = if (customStart != null && customEnd != null) {
                        "Zakres własny: ${PolishDateTimeFormatter.formatRangeLabel(customStart, customEnd).removePrefix("Zakres: ")}"
                    } else {
                        "Zakres własny: —"
                    },
                    isLoading = false,
                    infoMessage = if (persons.isEmpty()) "Brak lokalnych danych. Poczekaj na synchronizację." else null
                )
            }
            recalculate()
        }
    }

    fun onPersonSelected(patientId: String) {
        viewModelScope.launch {
            val selected = _uiState.value.persons.firstOrNull { it.patientId == patientId }
            val storedRange = localRepository.loadStoredRange(patientId)
            _uiState.update {
                it.copy(
                    selectedPatientId = patientId,
                    selectedPersonName = selected?.displayName ?: it.selectedPersonName,
                    customStart = storedRange?.oldest,
                    customEnd = storedRange?.newest,
                    customRangeLabel = if (storedRange != null) {
                        "Zakres własny: ${PolishDateTimeFormatter.formatRangeLabel(storedRange.oldest, storedRange.newest).removePrefix("Zakres: ")}"
                    } else {
                        "Zakres własny: —"
                    }
                )
            }
            recalculate()
        }
    }

    fun onNightOnlyChanged(enabled: Boolean) {
        _uiState.update { it.copy(nightOnlyEnabled = enabled) }
        recalculate()
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
                customRangeLabel = "Zakres własny: ${PolishDateTimeFormatter.formatRangeLabel(start, end).removePrefix("Zakres: ")}",
                infoMessage = null
            )
        }
        recalculate()
    }

    fun exportRawDataToExcel() {
        viewModelScope.launch {
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

    private fun recalculate() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val state = _uiState.value
            val patientId = state.selectedPatientId
            if (patientId == null) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        metricsByPeriod = AnalysisPeriod.entries.associateWith { PeriodMetrics.empty },
                        infoMessage = "Brak wybranej osoby do analizy."
                    )
                }
                return@launch
            }
            val now = Instant.now()
            val fixedWindowStart = now.minus(30, ChronoUnit.DAYS)
            val fixedWindowReadings = localRepository.loadHistory(patientId, fixedWindowStart, now)
            val customStart = state.customStart
            val customEnd = state.customEnd
            val customReadings = if (customStart != null && customEnd != null) {
                if (customStart >= fixedWindowStart && customEnd <= now) {
                    fixedWindowReadings.filter { point ->
                        !point.timestamp.isBefore(customStart) && !point.timestamp.isAfter(customEnd)
                    }
                } else {
                    localRepository.loadHistory(patientId, customStart, customEnd)
                }
            } else {
                emptyList()
            }
            val settings = settingsRepository.loadSettings()
            val weeklyBars = AnalysisChartFactory.weeklyStackedBars(
                readings = fixedWindowReadings,
                now = now,
                targetLow = settings.targetLow,
                targetHigh = settings.targetHigh,
                lowCritical = 54,
                highCritical = 250,
                nightOnly = state.nightOnlyEnabled
            )
            val overlay14 = AnalysisChartFactory.fourteenDayOverlay(
                readings = fixedWindowReadings,
                now = now
            )

            val matrix = AnalysisPeriod.entries.associateWith { period ->
                val (periodStart, periodEnd, source) = if (period == AnalysisPeriod.CUSTOM && customStart != null && customEnd != null) {
                    Triple(customStart, customEnd, customReadings)
                } else {
                    val start = now.minus(period.duration ?: Duration.ofDays(30))
                    Triple(start, now, fixedWindowReadings)
                }
                val scoped = source.filter { point ->
                    !point.timestamp.isBefore(periodStart) && !point.timestamp.isAfter(periodEnd)
                }
                AnalysisMetricsFactory.calculate(
                    readings = scoped,
                    periodStart = periodStart,
                    periodEnd = periodEnd,
                    targetLow = settings.targetLow,
                    targetHigh = settings.targetHigh,
                    lowCritical = 54,
                    highCritical = 250
                )
            }

            _uiState.update {
                it.copy(
                    metricsByPeriod = matrix,
                    weeklyBars = weeklyBars,
                    overlay14Days = overlay14,
                    isLoading = false,
                    infoMessage = if (matrix.values.all { metrics -> metrics.readingsCount == 0 }) {
                        "Brak danych dla wybranych okresów."
                    } else {
                        null
                    }
                )
            }
        }
    }
}

