package com.libredisplay.data.repository

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
        settingsRepository.setDemoModeEnabled(false)
        DiagnosticLogger.logInfo("PrivacyRepository", "disconnectAccount completed")
    }

    fun clearSessionData() {
        authRepository.clearSession()
        settingsRepository.clearSessionData()
        DiagnosticLogger.logInfo("PrivacyRepository", "clearSessionData completed")
    }

    suspend fun resetAppData() {
        deleteMyStoredData()
        settingsRepository.clearAll()
        authRepository.clearSession()
        DiagnosticLogger.logInfo("PrivacyRepository", "resetAppData completed")
    }

    suspend fun deleteDemoData() {
        localHistoryRepository.deleteDemoData()
        patientSettingsDao.deleteDemoSettings()
        settingsRepository.clearSelectedPatientId()
        settingsRepository.setDemoModeEnabled(false)
        authRepository.clearSession()
        settingsRepository.clearSessionData()
        DiagnosticLogger.logInfo("PrivacyRepository", "deleteDemoData completed")
    }
}

