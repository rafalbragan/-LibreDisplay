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
    val navigateToStart: Boolean = false,
    val navigateToLogin: Boolean = false
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
            _event.value = PrivacyActionEvent("Dane zostały usunięte z tego urządzenia.", navigateToStart = true)
        }
    }

    fun deleteLocalGlucoseHistory() {
        viewModelScope.launch {
            privacyRepository.deleteLocalGlucoseHistory()
            _event.value = PrivacyActionEvent("Lokalna historia glikemii została usunięta.")
        }
    }

    fun deleteMonitoredPeople() {
        viewModelScope.launch {
            privacyRepository.deleteObservedPeople()
            _event.value = PrivacyActionEvent("Monitorowane osoby i wybór osoby zostały usunięte.", navigateToStart = true)
        }
    }

    fun disconnectLibreLinkUpAccount() {
        privacyRepository.disconnectAccount()
        _event.value = PrivacyActionEvent("Konto LibreLinkUp zostało odłączone.", navigateToStart = true)
    }

    fun clearSessionData() {
        privacyRepository.clearSessionData()
        _event.value = PrivacyActionEvent("Dane sesji zostały wyczyszczone.", navigateToLogin = true)
    }

    fun clearSavedTokenAndLoginAgain() {
        privacyRepository.clearSavedTokenAndPrepareLiveLogin()
        _event.value = PrivacyActionEvent(
            "Zapisany token został usunięty. Zaloguj się ponownie.",
            navigateToLogin = true
        )
    }

    fun resetAppData() {
        viewModelScope.launch {
            privacyRepository.resetAppData()
            _event.value = PrivacyActionEvent("Aplikacja została zresetowana.", navigateToStart = true)
        }
    }

    fun deleteDemoData() {
        viewModelScope.launch {
            privacyRepository.deleteDemoData()
            _event.value = PrivacyActionEvent("Dane trybu demo zostały usunięte.", navigateToStart = true)
        }
    }

    fun consumeEvent() {
        _event.value = null
    }
}

