package com.libredisplay.data.repository

import android.content.Context
import com.libredisplay.data.api.PersistedLibreLinkUpSession
import com.libredisplay.data.model.AppSettings
import com.libredisplay.data.storage.SecureStorage

class SettingsRepository(context: Context) {

    private val storage = SecureStorage(context)

    fun saveSettings(settings: AppSettings) {
        storage.putString(SecureStorage.KEY_EMAIL, settings.email.trim())
        storage.putString(SecureStorage.KEY_PASSWORD, settings.password)
        storage.putString(SecureStorage.KEY_REGION, settings.region.uppercase())
        storage.putString(SecureStorage.KEY_REGION_MODE, settings.regionMode.uppercase())
        storage.putString(SecureStorage.KEY_CUSTOM_BASE_URL, settings.customBaseUrl.trim())
        storage.putInt(SecureStorage.KEY_REFRESH_INTERVAL, settings.refreshInterval.coerceIn(30, 300))
        storage.putInt(SecureStorage.KEY_TARGET_LOW, settings.targetLow.coerceIn(40, 300))
        storage.putInt(SecureStorage.KEY_TARGET_HIGH, settings.targetHigh.coerceIn(60, 400))
        storage.putInt(SecureStorage.KEY_TREND_WINDOW_MINUTES, settings.trendWindowMinutes)
        storage.putBoolean(SecureStorage.KEY_SHOW_STATISTICS, settings.showStatistics)
        storage.putBoolean(SecureStorage.KEY_KIOSK_MODE, settings.kioskMode)
        storage.putBoolean(SecureStorage.KEY_USE_MOCK, settings.useMock)
        storage.putBoolean(SecureStorage.KEY_USE_AUTH_V3, true)
    }

    fun loadSettings(): AppSettings {
        return AppSettings(
            email = storage.getString(SecureStorage.KEY_EMAIL),
            password = storage.getString(SecureStorage.KEY_PASSWORD),
            region = storage.getString(SecureStorage.KEY_REGION, "EU").ifBlank { "EU" },
            regionMode = storage.getString(SecureStorage.KEY_REGION_MODE, "EU").ifBlank { "EU" },
            customBaseUrl = storage.getString(SecureStorage.KEY_CUSTOM_BASE_URL),
            refreshInterval = storage.getInt(SecureStorage.KEY_REFRESH_INTERVAL, 60).coerceIn(30, 300),
            targetLow = storage.getInt(SecureStorage.KEY_TARGET_LOW, 80).coerceIn(40, 300),
            targetHigh = storage.getInt(SecureStorage.KEY_TARGET_HIGH, 180).coerceIn(60, 400),
            trendWindowMinutes = storage.getInt(SecureStorage.KEY_TREND_WINDOW_MINUTES, 3),
            showStatistics = storage.getBoolean(SecureStorage.KEY_SHOW_STATISTICS, true),
            kioskMode = storage.getBoolean(SecureStorage.KEY_KIOSK_MODE, false),
            useMock = storage.getBoolean(SecureStorage.KEY_USE_MOCK, false),
            useAuthV3 = true
        ).normalized()
    }

    fun isConfigured(): Boolean = loadSettings().isConfigured()

    fun clearAll() {
        storage.clear()
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
        return copy(
            targetLow = low,
            targetHigh = high,
            refreshInterval = refreshInterval.coerceIn(30, 300),
            regionMode = regionMode.ifBlank { "EU" }.uppercase().let { if (it == "AUTO") "EU" else it },
            region = region.ifBlank { "EU" }.uppercase(),
            customBaseUrl = customBaseUrl.trim()
        )
    }
}

data class TokenDiagnostics(
    val tokenPresent: Boolean,
    val source: String
)

