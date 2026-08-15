package com.libredisplay.ui.analytics

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.libredisplay.LibreDisplayApp
import com.libredisplay.analytics.DailyMetric
import com.libredisplay.analytics.GlucoseMetricsCalculator
import com.libredisplay.analytics.RangeDistribution
import com.libredisplay.analytics.SensorActivity
import com.libredisplay.data.model.GlucoseHistoryPoint
import com.libredisplay.data.model.LibreConnectionPerson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.temporal.ChronoUnit

enum class AnalyticsRange(val days: Long) {
    DAYS_7(7),
    DAYS_14(14),
    DAYS_30(30),
    DAYS_90(90),
    DAYS_180(180),
    DAYS_365(365)
}

data class AnalyticsUiState(
    val persons: List<LibreConnectionPerson> = emptyList(),
    val selectedPatientId: String? = null,
    val selectedPersonName: String? = null,
    val selectedRange: AnalyticsRange = AnalyticsRange.DAYS_14,
    val readings: List<GlucoseHistoryPoint> = emptyList(),
    val rangeDistribution: RangeDistribution? = null,
    val sensorActivity: SensorActivity? = null,
    val averageGlucose: Double? = null,
    val gmi: Double? = null,
    val dailyMetrics: List<DailyMetric> = emptyList(),
    val isLoading: Boolean = false,
    val infoMessage: String? = null
)

class AnalyticsViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as LibreDisplayApp
    private val localRepository = app.localGlucoseHistoryRepository
    private val settingsRepository = app.settingsRepository

    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    init {
        reload()
    }

    fun reload() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val settings = settingsRepository.loadSettings()
            val snapshot = app.glucoseSyncRepository.loadLatestLocalSnapshot(settings.selectedPatientId)
            val persons = snapshot?.persons.orEmpty()
            val selectedPerson = snapshot?.selectedPerson ?: persons.firstOrNull()
            val selectedPatientId = selectedPerson?.patientId
            val selectedName = selectedPerson?.displayName
            _uiState.update {
                it.copy(
                    persons = persons,
                    selectedPatientId = selectedPatientId,
                    selectedPersonName = selectedName,
                    isLoading = false,
                    infoMessage = if (persons.isEmpty()) "Brak lokalnych danych. Poczekaj na synchronizację." else null
                )
            }
            recalculate()
        }
    }

    fun onRangeSelected(range: AnalyticsRange) {
        _uiState.update { it.copy(selectedRange = range) }
        recalculate()
    }

    fun onPersonSelected(patientId: String) {
        val selected = _uiState.value.persons.firstOrNull { it.patientId == patientId }
        _uiState.update {
            it.copy(
                selectedPatientId = patientId,
                selectedPersonName = selected?.displayName ?: it.selectedPersonName
            )
        }
        recalculate()
    }

    private fun recalculate() {
        viewModelScope.launch {
            val state = _uiState.value
            val patientId = state.selectedPatientId ?: return@launch
            val end = Instant.now()
            val start = end.minus(state.selectedRange.days, ChronoUnit.DAYS)
            val readings = localRepository.loadHistory(patientId, start, end)
            val settings = settingsRepository.loadSettings()
            val rangeDistribution = GlucoseMetricsCalculator.calculateRangeDistribution(
                readings = readings,
                targetLow = settings.targetLow,
                targetHigh = settings.targetHigh,
                lowCritical = 54,
                highCritical = 250
            )
            val activity = GlucoseMetricsCalculator.calculateSensorActivity(
                readings = readings,
                periodStart = start,
                periodEnd = end,
                expectedIntervalMinutes = 15
            )
            val avg = GlucoseMetricsCalculator.calculateAverageGlucose(readings).takeIf { it.isFinite() }
            val gmi = avg?.let { GlucoseMetricsCalculator.calculateGmi(it) }
            val daily = GlucoseMetricsCalculator.calculateDailyMetrics(
                readings = readings,
                targetLow = settings.targetLow,
                targetHigh = settings.targetHigh,
                lowCritical = 54,
                highCritical = 250
            )
            _uiState.update {
                it.copy(
                    readings = readings,
                    rangeDistribution = rangeDistribution,
                    sensorActivity = activity,
                    averageGlucose = avg,
                    gmi = gmi,
                    dailyMetrics = daily,
                    infoMessage = if (readings.isEmpty()) "Brak danych dla wybranego okresu." else null
                )
            }
        }
    }
}

