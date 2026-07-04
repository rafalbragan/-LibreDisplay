package com.libredisplay.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.libredisplay.data.model.AppSettings
import com.libredisplay.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel for the Settings screen.
 *
 * Exposes the current [AppSettings] as a [StateFlow] and persists changes
 * via [SettingsRepository].
 *
 * Passwords are never logged anywhere in this class.
 */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository = SettingsRepository(application)

    private val _settings = MutableStateFlow(settingsRepository.loadSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private val _saveSuccess = MutableStateFlow(false)
    val saveSuccess: StateFlow<Boolean> = _saveSuccess.asStateFlow()

    fun onEmailChange(value: String) {
        _settings.value = _settings.value.copy(email = value)
    }

    fun onPasswordChange(value: String) {
        // Password is updated in-memory only – never logged
        _settings.value = _settings.value.copy(password = value)
    }

    fun onRegionChange(value: String) {
        _settings.value = _settings.value.copy(region = value)
    }

    fun onRefreshIntervalChange(value: Int) {
        _settings.value = _settings.value.copy(refreshInterval = value.coerceIn(1, 60))
    }

    fun onKioskModeChange(value: Boolean) {
        _settings.value = _settings.value.copy(kioskMode = value)
    }

    fun onUseMockChange(value: Boolean) {
        _settings.value = _settings.value.copy(useMock = value)
    }

    fun saveSettings() {
        settingsRepository.saveSettings(_settings.value)
        _saveSuccess.value = true
    }

    fun clearSaveSuccess() {
        _saveSuccess.value = false
    }
}

