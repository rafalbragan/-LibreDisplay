package com.libredisplay.data.repository

import android.content.Context
import com.libredisplay.data.api.PersistedLibreLinkUpSession
import com.libredisplay.data.model.AppMode
import com.libredisplay.data.model.AppSettings
import com.libredisplay.data.model.HbA1cSettings
import com.libredisplay.data.model.QuickMetricId
import com.libredisplay.data.storage.SecureStorage
import com.libredisplay.diagnostics.DiagnosticLogger
import java.time.LocalDate

class SettingsRepository(context: Context) {

    private val storage = SecureStorage(context)

    fun saveSettings(settings: AppSettings) {
        val normalizedEmail = settings.email.trim().lowercase()
        val normalizedPassword = settings.password.trim()
        val originalPassword = settings.password
        val storedSelectedPatientId = storage.getString(SecureStorage.KEY_SELECTED_PATIENT_ID).trim().takeIf { it.isNotBlank() }
        val requestedSelectedPatientId = settings.selectedPatientId?.trim().takeIf { !it.isNullOrBlank() }
        val selectedPatientIdToStore = when (settings.appMode) {
            AppMode.DEMO -> {
                requestedSelectedPatientId?.takeIf(::isDemoPatientId)
                    ?: storedSelectedPatientId?.takeIf(::isDemoPatientId)
                    ?: DEFAULT_DEMO_PATIENT_ID
            }
            AppMode.LIVE -> {
                requestedSelectedPatientId?.takeIf { !isDemoPatientId(it) }
                    ?: storedSelectedPatientId?.takeIf { !isDemoPatientId(it) }
            }
            AppMode.NONE -> null
        }

        val passwordHadLeadingWhitespace = originalPassword.isNotEmpty() && originalPassword.first().isWhitespace()
        val passwordHadTrailingWhitespace = originalPassword.isNotEmpty() && originalPassword.last().isWhitespace()
        val passwordHadNewLine = originalPassword.contains('\n') || originalPassword.contains('\r')

        DiagnosticLogger.logInfo(
            "SettingsRepository",
            "Settings saved: emailLengthOriginal=${settings.email.length} emailLengthNormalized=${normalizedEmail.length} passwordLengthOriginal=${originalPassword.length} passwordLengthNormalized=${normalizedPassword.length} passwordHadLeadingWhitespace=$passwordHadLeadingWhitespace passwordHadTrailingWhitespace=$passwordHadTrailingWhitespace passwordHadNewLine=$passwordHadNewLine"
        )

        storage.putString(SecureStorage.KEY_EMAIL, normalizedEmail)
        storage.putString(SecureStorage.KEY_PASSWORD, normalizedPassword)
        storage.putString(SecureStorage.KEY_REGION, settings.region.uppercase())
        storage.putString(SecureStorage.KEY_REGION_MODE, settings.regionMode.uppercase())
        storage.putString(SecureStorage.KEY_CUSTOM_BASE_URL, settings.customBaseUrl.trim())
        storage.putInt(SecureStorage.KEY_REFRESH_INTERVAL, settings.refreshInterval.coerceIn(30, 300))
        storage.putInt(SecureStorage.KEY_TARGET_LOW, settings.targetLow.coerceIn(40, 300))
        storage.putInt(SecureStorage.KEY_TARGET_HIGH, settings.targetHigh.coerceIn(60, 400))
        storage.putInt(SecureStorage.KEY_TREND_WINDOW_MINUTES, settings.trendWindowMinutes)
        storage.putBoolean(SecureStorage.KEY_SHOW_STATISTICS, settings.showStatistics)
        storage.putBoolean(SecureStorage.KEY_KIOSK_MODE, settings.kioskMode)
        storage.putString(SecureStorage.KEY_APP_MODE, settings.appMode.name)
        storage.putBoolean(SecureStorage.KEY_USE_MOCK, settings.useMock)
        storage.putBoolean(SecureStorage.KEY_USE_AUTH_V3, true)
        storage.putString(SecureStorage.KEY_SELECTED_PATIENT_ID, selectedPatientIdToStore.orEmpty())
        storage.putInt(
            SecureStorage.KEY_RETENTION_HOURS,
            settings.retentionHours.coerceIn(AppSettings.MIN_RETENTION_HOURS, AppSettings.MAX_RETENTION_HOURS)
        )
        storage.putInt(SecureStorage.KEY_BACKGROUND_POLLING_MINUTES, settings.backgroundPollingMinutes.coerceIn(5, 60))
        storage.putBoolean(SecureStorage.KEY_BACKGROUND_SERVICE_ENABLED, settings.backgroundServiceEnabled)
    }

    fun loadSettings(): AppSettings {
        val email = storage.getString(SecureStorage.KEY_EMAIL)
        val password = storage.getString(SecureStorage.KEY_PASSWORD)
        val appMode = resolveAppMode(
            storedMode = storage.getString(SecureStorage.KEY_APP_MODE),
            legacyUseMock = storage.getBoolean(SecureStorage.KEY_USE_MOCK, false),
            hasCredentials = email.isNotBlank() && password.isNotBlank(),
            hasPersistedSession = loadPersistedSession() != null
        )
        return AppSettings(
            email = email,
            password = password,
            region = storage.getString(SecureStorage.KEY_REGION, "EU").ifBlank { "EU" },
            regionMode = storage.getString(SecureStorage.KEY_REGION_MODE, "EU").ifBlank { "EU" },
            customBaseUrl = storage.getString(SecureStorage.KEY_CUSTOM_BASE_URL),
            refreshInterval = storage.getInt(SecureStorage.KEY_REFRESH_INTERVAL, 60).coerceIn(30, 300),
            targetLow = storage.getInt(SecureStorage.KEY_TARGET_LOW, 80).coerceIn(40, 300),
            targetHigh = storage.getInt(SecureStorage.KEY_TARGET_HIGH, 180).coerceIn(60, 400),
            trendWindowMinutes = storage.getInt(SecureStorage.KEY_TREND_WINDOW_MINUTES, 3),
            showStatistics = storage.getBoolean(SecureStorage.KEY_SHOW_STATISTICS, true),
            kioskMode = storage.getBoolean(SecureStorage.KEY_KIOSK_MODE, false),
            appMode = appMode,
            selectedPatientId = storage.getString(SecureStorage.KEY_SELECTED_PATIENT_ID).trim().takeIf { it.isNotBlank() },
            useAuthV3 = true,
            retentionHours = storage.getInt(
                SecureStorage.KEY_RETENTION_HOURS,
                AppSettings.DEFAULT_RETENTION_HOURS
            ).coerceIn(AppSettings.MIN_RETENTION_HOURS, AppSettings.MAX_RETENTION_HOURS),
            backgroundPollingMinutes = storage.getInt(SecureStorage.KEY_BACKGROUND_POLLING_MINUTES, 60).coerceIn(5, 60),
            backgroundServiceEnabled = storage.getBoolean(SecureStorage.KEY_BACKGROUND_SERVICE_ENABLED, true)
        ).normalized()
    }

    fun isConfigured(): Boolean = loadSettings().isConfigured()

    fun clearAll() {
        storage.clear()
    }

    fun clearSelectedPatientId() {
        storage.putString(SecureStorage.KEY_SELECTED_PATIENT_ID, "")
    }

    fun clearRateLimitState() {
        clearRateLimitUntilEpochMillis()
        clearNextAllowedLoginAt()
    }

    fun clearSessionData() {
        clearPersistedSession()
        clearRateLimitState()
    }

    fun isRestorePromptAcknowledged(): Boolean {
        return storage.getBoolean(SecureStorage.KEY_RESTORE_PROMPT_ACK, false)
    }

    fun setRestorePromptAcknowledged(value: Boolean) {
        storage.putBoolean(SecureStorage.KEY_RESTORE_PROMPT_ACK, value)
    }

    fun clearAccountCredentials() {
        storage.putString(SecureStorage.KEY_EMAIL, "")
        storage.putString(SecureStorage.KEY_PASSWORD, "")
    }

    fun setAppMode(appMode: AppMode) {
        storage.putString(SecureStorage.KEY_APP_MODE, appMode.name)
        storage.putBoolean(SecureStorage.KEY_USE_MOCK, appMode == AppMode.DEMO)
        if (appMode == AppMode.NONE) {
            clearSelectedPatientId()
        } else if (appMode == AppMode.LIVE && isDemoPatientId(storage.getString(SecureStorage.KEY_SELECTED_PATIENT_ID))) {
            clearSelectedPatientId()
        }
    }

    fun switchToDemoMode() {
        val current = loadSettings()
        saveSettings(
            current.copy(
                appMode = AppMode.DEMO,
                selectedPatientId = DEFAULT_DEMO_PATIENT_ID
            )
        )
    }

    fun switchToLiveMode() {
        val current = loadSettings()
        saveSettings(
            current.copy(
                appMode = AppMode.LIVE,
                selectedPatientId = current.selectedPatientId?.takeIf { !isDemoPatientId(it) }
            )
        )
    }

    fun resetModeSelection() {
        val current = loadSettings()
        saveSettings(current.copy(appMode = AppMode.NONE, selectedPatientId = null))
    }

    fun hasStoredCredentials(): Boolean {
        val settings = loadSettings()
        return settings.hasCredentials()
    }

    fun hasPersistedSessionData(): Boolean = loadPersistedSession() != null

    fun shouldShowLoginForm(): Boolean {
        val settings = loadSettings()
        return settings.appMode == AppMode.LIVE && !hasPersistedSessionData() && !settings.hasCredentials()
    }

    fun saveNextAllowedLoginAt(epochMillis: Long) {
        storage.putString(SecureStorage.KEY_NEXT_ALLOWED_LOGIN_AT, epochMillis.toString())
    }

    fun loadNextAllowedLoginAt(): Long {
        return storage.getString(SecureStorage.KEY_NEXT_ALLOWED_LOGIN_AT).toLongOrNull() ?: 0L
    }

    fun clearNextAllowedLoginAt() {
        storage.putString(SecureStorage.KEY_NEXT_ALLOWED_LOGIN_AT, "0")
    }

    fun saveRateLimitUntilEpochMillis(epochMillis: Long) {
        storage.putString(SecureStorage.KEY_RATE_LIMIT_UNTIL, epochMillis.toString())
    }

    fun loadRateLimitUntilEpochMillis(): Long {
        return storage.getString(SecureStorage.KEY_RATE_LIMIT_UNTIL).toLongOrNull() ?: 0L
    }

    fun clearRateLimitUntilEpochMillis() {
        storage.putString(SecureStorage.KEY_RATE_LIMIT_UNTIL, "0")
    }

    fun savePersistedSession(session: PersistedLibreLinkUpSession) {
        storage.putString(SecureStorage.KEY_TOKEN, session.token)
        storage.putString(SecureStorage.KEY_USER_ID, session.userId)
        storage.putString(SecureStorage.KEY_ACCOUNT_ID, session.accountIdHash)
        storage.putString(SecureStorage.KEY_SESSION_REGION, session.region)
        storage.putString(SecureStorage.KEY_SESSION_BASE_URL, session.baseUrl)
        storage.putString(SecureStorage.KEY_TOKEN_EXPIRES_AT, session.tokenExpiresAtEpochSeconds?.toString().orEmpty())
        storage.putString(SecureStorage.KEY_TOKEN_SOURCE, "current")
    }

    fun saveNormalizedEmail(email: String) {
        storage.putString(SecureStorage.KEY_EMAIL, email)
    }

    fun saveSelectedPatientId(patientId: String?) {
        val normalizedPatientId = patientId?.trim().takeIf { !it.isNullOrBlank() }.orEmpty()
        storage.putString(SecureStorage.KEY_SELECTED_PATIENT_ID, normalizedPatientId)
        DiagnosticLogger.logInfo(
            "SettingsRepository",
            "selectedPatientId saved patientIdPrefix=${normalizedPatientId.take(6)}"
        )
    }

    fun loadQuickMetricsOrder(): List<QuickMetricId> {
        val raw = storage.getString(SecureStorage.KEY_QUICK_METRICS_ORDER)
        if (raw.isBlank()) return QuickMetricId.DEFAULT_ORDER
        val parsed = raw.split(',')
            .mapNotNull { QuickMetricId.fromStorageId(it.trim()) }
        return QuickMetricId.normalizeOrder(parsed)
    }

    fun saveQuickMetricsOrder(order: List<QuickMetricId>) {
        val normalized = QuickMetricId.normalizeOrder(order)
        val serialized = normalized.joinToString(",") { it.storageId }
        storage.putString(SecureStorage.KEY_QUICK_METRICS_ORDER, serialized)
    }

    fun loadQuickMetricsVisibility(): Map<QuickMetricId, Boolean> {
        val defaults = QuickMetricId.entries.associateWith { it in QuickMetricId.DEFAULT_VISIBLE }.toMutableMap()
        val raw = storage.getString(SecureStorage.KEY_QUICK_METRICS_VISIBILITY)
        if (raw.isBlank()) return defaults

        raw.split(',')
            .map { it.trim() }
            .filter { it.contains(':') }
            .forEach { token ->
                val parts = token.split(':', limit = 2)
                val metricId = QuickMetricId.fromStorageId(parts[0].trim()) ?: return@forEach
                defaults[metricId] = parts[1].trim() == "1"
            }
        return defaults
    }

    fun saveQuickMetricsVisibility(visibility: Map<QuickMetricId, Boolean>) {
        val serialized = QuickMetricId.entries.joinToString(",") { metricId ->
            val visible = visibility[metricId] ?: (metricId in QuickMetricId.DEFAULT_VISIBLE)
            "${metricId.storageId}:${if (visible) "1" else "0"}"
        }
        storage.putString(SecureStorage.KEY_QUICK_METRICS_VISIBILITY, serialized)
    }

    fun loadHbA1cSettings(patientId: String?): HbA1cSettings {
        val suffix = patientStorageSuffix(patientId)
        val labValue = storage.getString(SecureStorage.KEY_HBA1C_LAB_PERCENT_PREFIX + suffix)
            .replace(',', '.')
            .toDoubleOrNull()
        val labDate = storage.getString(SecureStorage.KEY_HBA1C_LAB_DATE_PREFIX + suffix)
            .takeIf { it.isNotBlank() }
            ?.let { raw -> runCatching { LocalDate.parse(raw) }.getOrNull() }
        val target = storage.getString(SecureStorage.KEY_HBA1C_TARGET_PERCENT_PREFIX + suffix)
            .replace(',', '.')
            .toDoubleOrNull()
            ?.coerceIn(5.0, 12.0)
            ?: 7.5
        return HbA1cSettings(
            patientId = patientId,
            labHbA1cPercent = labValue,
            labHbA1cDate = labDate,
            targetHbA1cPercent = target
        )
    }

    fun saveHbA1cSettings(settings: HbA1cSettings) {
        val suffix = patientStorageSuffix(settings.patientId)
        val normalizedLab = settings.labHbA1cPercent
            ?.takeIf { it.isFinite() }
            ?.coerceIn(3.5, 20.0)
        val normalizedTarget = settings.targetHbA1cPercent
            .takeIf { it.isFinite() }
            ?.coerceIn(5.0, 12.0)
            ?: 7.5
        storage.putString(
            SecureStorage.KEY_HBA1C_LAB_PERCENT_PREFIX + suffix,
            normalizedLab?.toString().orEmpty()
        )
        storage.putString(
            SecureStorage.KEY_HBA1C_LAB_DATE_PREFIX + suffix,
            settings.labHbA1cDate?.toString().orEmpty()
        )
        storage.putString(
            SecureStorage.KEY_HBA1C_TARGET_PERCENT_PREFIX + suffix,
            normalizedTarget.toString()
        )
    }

    fun tokenDiagnostics(): TokenDiagnostics {
        val tokenPresent = storage.getString(SecureStorage.KEY_TOKEN).isNotBlank()
        val source = storage.getString(SecureStorage.KEY_TOKEN_SOURCE).ifBlank { if (tokenPresent) "current" else "none" }
        return TokenDiagnostics(tokenPresent = tokenPresent, source = source)
    }

    fun loadPersistedSession(): PersistedLibreLinkUpSession? {
        val token = storage.getString(SecureStorage.KEY_TOKEN)
        val userId = storage.getString(SecureStorage.KEY_USER_ID)
        val accountId = storage.getString(SecureStorage.KEY_ACCOUNT_ID)
        val region = storage.getString(SecureStorage.KEY_SESSION_REGION)
        val baseUrl = storage.getString(SecureStorage.KEY_SESSION_BASE_URL)
        if (token.isBlank() || userId.isBlank() || accountId.isBlank() || region.isBlank() || baseUrl.isBlank()) {
            return null
        }
        return PersistedLibreLinkUpSession(
            token = token,
            userId = userId,
            accountIdHash = accountId,
            region = region,
            baseUrl = baseUrl,
            tokenExpiresAtEpochSeconds = storage.getString(SecureStorage.KEY_TOKEN_EXPIRES_AT).toLongOrNull()
        )
    }

    fun clearPersistedSession() {
        storage.putString(SecureStorage.KEY_TOKEN, "")
        storage.putString(SecureStorage.KEY_USER_ID, "")
        storage.putString(SecureStorage.KEY_ACCOUNT_ID, "")
        storage.putString(SecureStorage.KEY_SESSION_REGION, "")
        storage.putString(SecureStorage.KEY_SESSION_BASE_URL, "")
        storage.putString(SecureStorage.KEY_TOKEN_EXPIRES_AT, "")
        storage.putString(SecureStorage.KEY_TOKEN_SOURCE, "")
    }

    private fun AppSettings.normalized(): AppSettings {
        val low = targetLow.coerceIn(40, 300)
        val high = targetHigh.coerceAtLeast(low + 1).coerceAtMost(400)
        val normalizedSelectedPatientId = when (appMode) {
            AppMode.DEMO -> selectedPatientId?.takeIf(::isDemoPatientId) ?: DEFAULT_DEMO_PATIENT_ID
            AppMode.LIVE -> selectedPatientId?.takeIf { !isDemoPatientId(it) }
            AppMode.NONE -> null
        }
        return copy(
            targetLow = low,
            targetHigh = high,
            refreshInterval = refreshInterval.coerceIn(30, 300),
            regionMode = regionMode.ifBlank { "EU" }.uppercase().let { if (it == "AUTO") "EU" else it },
            region = region.ifBlank { "EU" }.uppercase(),
            customBaseUrl = customBaseUrl.trim(),
            selectedPatientId = normalizedSelectedPatientId?.trim().takeIf { !it.isNullOrBlank() },
            retentionHours = retentionHours.coerceIn(AppSettings.MIN_RETENTION_HOURS, AppSettings.MAX_RETENTION_HOURS),
            backgroundPollingMinutes = backgroundPollingMinutes.coerceIn(5, 60)
        )
    }

    private fun resolveAppMode(
        storedMode: String,
        legacyUseMock: Boolean,
        hasCredentials: Boolean,
        hasPersistedSession: Boolean
    ): AppMode {
        return runCatching { AppMode.valueOf(storedMode) }.getOrNull()
            ?: when {
                legacyUseMock -> AppMode.DEMO
                hasCredentials || hasPersistedSession -> AppMode.LIVE
                else -> AppMode.NONE
            }
    }

    private fun isDemoPatientId(patientId: String?): Boolean {
        return patientId?.startsWith("demo-person-") == true
    }

    private fun patientStorageSuffix(patientId: String?): String {
        return patientId?.trim().takeIf { !it.isNullOrBlank() } ?: "global"
    }

    private companion object {
        const val DEFAULT_DEMO_PATIENT_ID = "demo-person-anna"
    }
}

data class TokenDiagnostics(
    val tokenPresent: Boolean,
    val source: String
)

