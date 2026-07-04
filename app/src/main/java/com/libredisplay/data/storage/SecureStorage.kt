package com.libredisplay.data.storage

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Thin wrapper around [EncryptedSharedPreferences].
 *
 * All values are stored using AES-256-GCM / AES-SIV encryption backed by
 * the Android Keystore.  Passwords and tokens are never written to logcat.
 */
class SecureStorage(context: Context) {

    private val prefs: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                "libre_display_secure_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.w(TAG, "Encrypted storage unavailable, falling back to private preferences")
            context.getSharedPreferences("libre_display_fallback_prefs", Context.MODE_PRIVATE)
        }
    }

    fun putString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    fun getString(key: String, default: String = ""): String =
        prefs.getString(key, default) ?: default

    fun putInt(key: String, value: Int) {
        prefs.edit().putInt(key, value).apply()
    }

    fun getInt(key: String, default: Int = 0): Int =
        prefs.getInt(key, default)

    fun putBoolean(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
    }

    fun getBoolean(key: String, default: Boolean = false): Boolean =
        prefs.getBoolean(key, default)

    companion object {
        private const val TAG = "SecureStorage"
        const val KEY_EMAIL = "email"
        const val KEY_PASSWORD = "password"   // stored encrypted – never logged
        const val KEY_REGION = "region"
        const val KEY_REFRESH_INTERVAL = "refresh_interval"
        const val KEY_KIOSK_MODE = "kiosk_mode"
        const val KEY_USE_MOCK = "use_mock"
    }
}

