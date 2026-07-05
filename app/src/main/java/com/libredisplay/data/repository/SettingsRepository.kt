package com.libredisplay.data.repository

import android.content.Context
import com.libredisplay.data.model.AppSettings
import com.libredisplay.data.storage.SecureStorage

/**
 * Repository responsible for persisting and reading [AppSettings].
 *
 * All sensitive fields (email, password) are stored via [SecureStorage]
 * which uses EncryptedSharedPreferences backed by the Android Keystore.
 *
 * Passwords are NEVER emitted to logcat or crash-reporting services.
 */
class SettingsRepository(context: Context) {

    private val storage = SecureStorage(context)

    fun saveSettings(settings: AppSettings) {
        storage.putString(SecureStorage.KEY_EMAIL, settings.email)
        // Password stored encrypted – intentionally not logged
        storage.putString(SecureStorage.KEY_PASSWORD, settings.password)
        storage.putString(SecureStorage.KEY_REGION, settings.region)
        storage.putInt(SecureStorage.KEY_REFRESH_INTERVAL, settings.refreshInterval)
        storage.putBoolean(SecureStorage.KEY_KIOSK_MODE, settings.kioskMode)
        storage.putBoolean(SecureStorage.KEY_USE_MOCK, settings.useMock)
    }

    fun loadSettings(): AppSettings = AppSettings(
        email = storage.getString(SecureStorage.KEY_EMAIL),
        password = storage.getString(SecureStorage.KEY_PASSWORD),
        region = storage.getString(SecureStorage.KEY_REGION, "EU"),
        refreshInterval = storage.getInt(SecureStorage.KEY_REFRESH_INTERVAL, 5),
        kioskMode = storage.getBoolean(SecureStorage.KEY_KIOSK_MODE, false),
        useMock = storage.getBoolean(SecureStorage.KEY_USE_MOCK, false)
    )

    fun isConfigured(): Boolean {
        val s = loadSettings()
        return s.email.isNotBlank() && s.password.isNotBlank()
    }
}

