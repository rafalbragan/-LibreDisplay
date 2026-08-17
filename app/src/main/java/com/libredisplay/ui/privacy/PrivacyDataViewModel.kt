package com.libredisplay.ui.privacy

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.libredisplay.LibreDisplayApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PrivacyActionEvent(
    val message: String,
    val navigateToStart: Boolean = false
)

class PrivacyDataViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as LibreDisplayApp
    private val privacyRepository = app.privacyRepository

    private val _event = MutableStateFlow<PrivacyActionEvent?>(null)
    val event: StateFlow<PrivacyActionEvent?> = _event.asStateFlow()

    val isDemoMode: Boolean
        get() = app.settingsRepository.loadSettings().useMock

    fun deleteMyStoredData() {
        viewModelScope.launch {
            privacyRepository.deleteMyStoredData()
            _event.value = PrivacyActionEvent("Stored data deleted from this device.", navigateToStart = true)
        }
    }

    fun deleteLocalGlucoseHistory() {
        viewModelScope.launch {
            privacyRepository.deleteLocalGlucoseHistory()
            _event.value = PrivacyActionEvent("Local glucose history deleted.")
        }
    }

    fun deleteMonitoredPeople() {
        viewModelScope.launch {
            privacyRepository.deleteObservedPeople()
            _event.value = PrivacyActionEvent("Monitored people deleted. Selected person cleared.", navigateToStart = true)
        }
    }

    fun disconnectLibreLinkUpAccount() {
        privacyRepository.disconnectAccount()
        _event.value = PrivacyActionEvent("LibreLinkUp account disconnected. Session cleared.", navigateToStart = true)
    }

    fun clearSessionData() {
        privacyRepository.clearSessionData()
        _event.value = PrivacyActionEvent("Session data cleared.")
    }

    fun resetAppData() {
        viewModelScope.launch {
            privacyRepository.resetAppData()
            _event.value = PrivacyActionEvent("App data reset completed.", navigateToStart = true)
        }
    }

    fun deleteDemoData() {
        viewModelScope.launch {
            privacyRepository.deleteDemoData()
            _event.value = PrivacyActionEvent("Demo data deleted.", navigateToStart = true)
        }
    }

    fun consumeEvent() {
        _event.value = null
    }
}

