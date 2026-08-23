package com.libredisplay.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.libredisplay.LibreDisplayApp
import com.libredisplay.data.model.AppMode
import com.libredisplay.data.model.AppSettings
import com.libredisplay.data.model.HbA1cSettings
import com.libredisplay.data.model.QuickMetricId
import com.libredisplay.data.repository.SyncReason
import com.libredisplay.diagnostics.DiagnosticLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as LibreDisplayApp
    private val settingsRepository = app.settingsRepository
    private val authRepository = app.authRepository

    private val _settings = MutableStateFlow(settingsRepository.loadSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private val _hba1cSettings = MutableStateFlow(
        settingsRepository.loadHbA1cSettings(_settings.value.selectedPatientId)
    )
    val hba1cSettings: StateFlow<HbA1cSettings> = _hba1cSettings.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _quickMetricsOrder = MutableStateFlow(settingsRepository.loadQuickMetricsOrder())
    val quickMetricsOrder: StateFlow<List<QuickMetricId>> = _quickMetricsOrder.asStateFlow()

    private val _quickMetricsVisibility = MutableStateFlow(settingsRepository.loadQuickMetricsVisibility())
    val quickMetricsVisibility: StateFlow<Map<QuickMetricId, Boolean>> = _quickMetricsVisibility.asStateFlow()

    fun reloadFromRepository() {
        _settings.value = settingsRepository.loadSettings()
        _hba1cSettings.value = settingsRepository.loadHbA1cSettings(_settings.value.selectedPatientId)
        _quickMetricsOrder.value = settingsRepository.loadQuickMetricsOrder()
        _quickMetricsVisibility.value = settingsRepository.loadQuickMetricsVisibility()
    }

    fun moveQuickMetricUp(metricId: QuickMetricId) {
        val current = _quickMetricsOrder.value.toMutableList()
        val index = current.indexOf(metricId)
        if (index <= 0) return
        current.removeAt(index)
        current.add(index - 1, metricId)
        persistQuickMetricOrder(current)
    }

    fun moveQuickMetricDown(metricId: QuickMetricId) {
        val current = _quickMetricsOrder.value.toMutableList()
        val index = current.indexOf(metricId)
        if (index == -1 || index >= current.lastIndex) return
        current.removeAt(index)
        current.add(index + 1, metricId)
        persistQuickMetricOrder(current)
    }

    fun setQuickMetricsOrder(order: List<QuickMetricId>) {
        persistQuickMetricOrder(order)
    }

    private fun persistQuickMetricOrder(order: List<QuickMetricId>) {
        _quickMetricsOrder.value = QuickMetricId.normalizeOrder(order)
    }

    fun setQuickMetricVisible(metricId: QuickMetricId, visible: Boolean) {
        val updated = _quickMetricsVisibility.value.toMutableMap()
        updated[metricId] = visible
        _quickMetricsVisibility.value = updated
    }

    fun onEmailChange(value: String) {
        _settings.value = _settings.value.copy(email = value)
    }

    fun onPasswordChange(value: String) {
        _settings.value = _settings.value.copy(password = value)
    }

    fun onUseMockChange(value: Boolean) {
        _settings.value = _settings.value.copy(appMode = if (value) AppMode.DEMO else AppMode.LIVE)
    }

    fun onLabHbA1cPercentChange(value: String) {
        val normalized = value.replace(',', '.').toDoubleOrNull()
        _hba1cSettings.value = _hba1cSettings.value.copy(
            labHbA1cPercent = normalized?.coerceIn(3.5, 20.0)
        )
    }

    fun onLabHbA1cDateChange(value: String) {
        val parsed = runCatching { LocalDate.parse(value.trim()) }.getOrNull()
        _hba1cSettings.value = _hba1cSettings.value.copy(labHbA1cDate = parsed)
    }

    fun onTargetHbA1cPercentChange(value: String) {
        val parsed = value.replace(',', '.').toDoubleOrNull()?.coerceIn(5.0, 12.0) ?: 7.5
        _hba1cSettings.value = _hba1cSettings.value.copy(targetHbA1cPercent = parsed)
    }

    fun onRegionModeChange(value: String) {
        _settings.value = _settings.value.copy(regionMode = value)
    }

    fun onCustomBaseUrlChange(value: String) {
        _settings.value = _settings.value.copy(customBaseUrl = value)
    }

    fun onTargetLowChange(value: String) {
        value.toIntOrNull()?.let { parsed ->
            val low = parsed.coerceIn(40, 300)
            val high = _settings.value.targetHigh.coerceAtLeast(low + 1)
            _settings.value = _settings.value.copy(targetLow = low, targetHigh = high)
        }
    }

    fun onTargetHighChange(value: String) {
        value.toIntOrNull()?.let { parsed ->
            val high = parsed.coerceIn(60, 400)
            val low = _settings.value.targetLow.coerceAtMost(high - 1)
            _settings.value = _settings.value.copy(targetLow = low, targetHigh = high)
        }
    }

    fun saveSettings() {
        if (_isSaving.value) return
        val settingsToSave = _settings.value.let { current ->
            if (current.appMode == AppMode.NONE && (current.hasCredentials() || current.useMock)) {
                current.copy(appMode = if (current.useMock) AppMode.DEMO else AppMode.LIVE)
            } else {
                current
            }
        }
        viewModelScope.launch {
            _isSaving.value = true
            try {
                settingsRepository.saveSettings(settingsToSave)
                settingsRepository.saveHbA1cSettings(
                    _hba1cSettings.value.copy(patientId = settingsToSave.selectedPatientId)
                )
                settingsRepository.saveQuickMetricsOrder(_quickMetricsOrder.value)
                settingsRepository.saveQuickMetricsVisibility(_quickMetricsVisibility.value)
                authRepository.clearSession()
                reloadFromRepository()
                DiagnosticLogger.logInfo("SettingsViewModel", "Settings saved appMode=${settingsToSave.appMode}")
                _message.value = "Ustawienia zapisane"
            } finally {
                _isSaving.value = false
            }
        }
    }

    /**
     * Login is a single, non-interactive flow:
     *
     *  1. the password is verified against LibreLinkUp,
     *  2. only then the stored data file is merged into the live database,
     *  3. the last 12 hours are downloaded immediately,
     *  4. the refreshed data is written back into the automatic data file,
     *  5. the Home screen opens - the user is never asked whether to "connect".
     */
    fun saveAndLogin() {
        if (_isSaving.value) return
        val draft = _settings.value.copy(appMode = AppMode.LIVE)
        viewModelScope.launch {
            _isSaving.value = true
            runCatching {
                settingsRepository.saveSettings(draft)
                settingsRepository.saveHbA1cSettings(
                    _hba1cSettings.value.copy(patientId = draft.selectedPatientId)
                )
                settingsRepository.saveQuickMetricsOrder(_quickMetricsOrder.value)
                settingsRepository.saveQuickMetricsVisibility(_quickMetricsVisibility.value)
                // Step 1 - verify the credentials before anything is restored.
                authRepository.ensureAuthenticated(force = true)
            }.onSuccess {
                prepareDataAfterVerifiedLogin()
                reloadFromRepository()
                _message.value = "Ustawienia zapisane"
            }.onFailure {
                val cooldownSeconds = authRepository.cooldownRemainingSeconds()
                _message.value = if (cooldownSeconds > 0) {
                    "Zbyt wiele prób logowania. Spróbuj ponownie za $cooldownSeconds sekund."
                } else {
                    "Nie udało się zalogować. Sprawdź email i hasło albo spróbuj ponownie później."
                }
            }.also {
                _isSaving.value = false
            }
        }
    }

    private suspend fun prepareDataAfterVerifiedLogin() {
        // Step 2 - merge the archive with whatever is already on the device.
        runCatching { app.appDataBackupRepository.mergeAutomaticBackupAfterLogin() }
            .onSuccess { result ->
                result?.let {
                    DiagnosticLogger.logInfo(
                        "SettingsViewModel",
                        "LOGIN MERGE persons=${it.summary.livePersons} readings=${it.summary.liveReadings}"
                    )
                }
            }
            .onFailure {
                DiagnosticLogger.logWarning("SettingsViewModel", "LOGIN MERGE failed reason=${it.message}")
            }

        // Step 3 - immediately download the window LibreLinkUp reliably exposes.
        runCatching { app.glucoseSyncRepository.syncAllPersons(SyncReason.LOGIN) }
            .onFailure {
                DiagnosticLogger.logWarning("SettingsViewModel", "LOGIN SYNC failed reason=${it.message}")
            }

        // Step 4 - the fresh readings belong in the automatic data file right away.
        app.appDataBackupRepository.refreshAutomaticBackupQuietly()

        // The user just logged in explicitly, so the startup data question is redundant.
        settingsRepository.setRestorePromptAcknowledged(true)
    }

    fun resetSession() {
        val current = settingsRepository.loadSettings()
        authRepository.clearSession()
        if (current.isDemoPatientSelected()) {
            settingsRepository.clearSelectedPatientId()
        }
        settingsRepository.switchToLiveMode()
        reloadFromRepository()
        _message.value = "Zapisany token został usunięty. Zaloguj się ponownie."
    }

    fun clearMessage() {
        _message.value = null
    }
}
