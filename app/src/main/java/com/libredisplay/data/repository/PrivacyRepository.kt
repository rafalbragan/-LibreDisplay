package com.libredisplay.data.repository

import com.libredisplay.data.model.AppMode
import com.libredisplay.data.local.PatientSettingsDao
import com.libredisplay.diagnostics.DiagnosticLogger

class PrivacyRepository(
    private val settingsRepository: SettingsRepository,
    private val authRepository: AuthRepository,
    private val localHistoryRepository: LocalGlucoseHistoryRepository,
    private val patientSettingsDao: PatientSettingsDao
) {

    suspend fun deleteMyStoredData() {
        localHistoryRepository.deleteLocalGlucoseHistory()
        localHistoryRepository.deleteObservedPeople()
        localHistoryRepository.deleteAllSyncRuns()
        patientSettingsDao.deleteAll()
        settingsRepository.clearAll()
        settingsRepository.setAppMode(AppMode.NONE)
        authRepository.clearSession()
        DiagnosticLogger.logInfo("PrivacyRepository", "deleteMyStoredData completed")
    }

    suspend fun deleteLocalGlucoseHistory() {
        localHistoryRepository.deleteLocalGlucoseHistory()
        DiagnosticLogger.logInfo("PrivacyRepository", "deleteLocalGlucoseHistory completed")
    }

    suspend fun deleteObservedPeople() {
        localHistoryRepository.deleteObservedPeople()
        patientSettingsDao.deleteAll()
        settingsRepository.clearSelectedPatientId()
        DiagnosticLogger.logInfo("PrivacyRepository", "deleteObservedPeople completed")
    }

    fun disconnectAccount() {
        authRepository.clearSession()
        settingsRepository.clearSessionData()
        settingsRepository.clearAccountCredentials()
        settingsRepository.clearSelectedPatientId()
        settingsRepository.setAppMode(AppMode.NONE)
        DiagnosticLogger.logInfo("PrivacyRepository", "disconnectAccount completed")
    }

    fun clearSessionData() {
        authRepository.clearSession()
        settingsRepository.clearSessionData()
        settingsRepository.setAppMode(AppMode.LIVE)
        val current = settingsRepository.loadSettings()
        if (current.isDemoPatientSelected()) {
            settingsRepository.clearSelectedPatientId()
        }
        DiagnosticLogger.logInfo("PrivacyRepository", "clearSessionData completed")
    }

    fun clearSavedTokenAndPrepareLiveLogin() {
        authRepository.clearSession()
        settingsRepository.clearPersistedSession()
        settingsRepository.setAppMode(AppMode.LIVE)
        val current = settingsRepository.loadSettings()
        if (current.isDemoPatientSelected()) {
            settingsRepository.clearSelectedPatientId()
        }
        DiagnosticLogger.logInfo("PrivacyRepository", "clearSavedTokenAndPrepareLiveLogin completed")
    }

    suspend fun resetAppData() {
        deleteMyStoredData()
        settingsRepository.clearAll()
        settingsRepository.setAppMode(AppMode.NONE)
        authRepository.clearSession()
        DiagnosticLogger.logInfo("PrivacyRepository", "resetAppData completed")
    }

    suspend fun deleteDemoData() {
        localHistoryRepository.deleteDemoData()
        patientSettingsDao.deleteDemoSettings()
        settingsRepository.clearSelectedPatientId()
        settingsRepository.setAppMode(AppMode.NONE)
        authRepository.clearSession()
        settingsRepository.clearSessionData()
        DiagnosticLogger.logInfo("PrivacyRepository", "deleteDemoData completed")
    }
}

