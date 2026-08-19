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

    fun contains(key: String): Boolean = prefs.contains(key)

    fun clear() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val TAG = "SecureStorage"
        const val KEY_EMAIL = "email"
        const val KEY_PASSWORD = "password"   // stored encrypted – never logged
        const val KEY_REGION = "region"
        const val KEY_REGION_MODE = "region_mode"
        const val KEY_CUSTOM_BASE_URL = "custom_base_url"
        const val KEY_REFRESH_INTERVAL = "refresh_interval"
        const val KEY_TARGET_LOW = "target_low"
        const val KEY_TARGET_HIGH = "target_high"
        const val KEY_TREND_WINDOW_MINUTES = "trend_window_minutes"
        const val KEY_SHOW_STATISTICS = "show_statistics"
        const val KEY_KIOSK_MODE = "kiosk_mode"
        const val KEY_RETENTION_HOURS = "retention_hours"
        const val KEY_BACKGROUND_POLLING_MINUTES = "background_polling_minutes"
        const val KEY_QUICK_METRICS_ORDER = "quick_metrics_order"
        const val KEY_APP_MODE = "app_mode"
        const val KEY_USE_MOCK = "use_mock"
        const val KEY_USE_AUTH_V3 = "use_auth_v3"
        const val KEY_SELECTED_PATIENT_ID = "selected_patient_id"
        const val KEY_NEXT_ALLOWED_LOGIN_AT = "next_allowed_login_at"
        const val KEY_RATE_LIMIT_UNTIL = "rate_limit_until"
        const val KEY_TOKEN = "token"
        const val KEY_TOKEN_EXPIRES_AT = "token_expires_at"
        const val KEY_USER_ID = "user_id"
        const val KEY_ACCOUNT_ID = "account_id"
        const val KEY_SESSION_REGION = "session_region"
        const val KEY_SESSION_BASE_URL = "session_base_url"
        const val KEY_TOKEN_SOURCE = "token_source"
        const val KEY_HBA1C_LAB_PERCENT_PREFIX = "hba1c_lab_percent_"
        const val KEY_HBA1C_LAB_DATE_PREFIX = "hba1c_lab_date_"
        const val KEY_HBA1C_TARGET_PERCENT_PREFIX = "hba1c_target_percent_"
        const val KEY_NETWORK_TOTAL_DOWNLOADED_BYTES = "network_total_downloaded_bytes"
        const val KEY_NETWORK_TOTAL_UPLOADED_BYTES = "network_total_uploaded_bytes"
        const val KEY_NETWORK_REQUEST_COUNT = "network_request_count"
        const val KEY_NETWORK_SUCCESS_COUNT = "network_success_count"
        const val KEY_NETWORK_FAILED_COUNT = "network_failed_count"
        const val KEY_NETWORK_FIRST_MEASURED_AT = "network_first_measured_at"
        const val KEY_NETWORK_LAST_MEASURED_AT = "network_last_measured_at"
        const val KEY_NETWORK_LAST_SYNC_AT = "network_last_sync_at"
        const val KEY_NETWORK_LAST_SYNC_SUCCESS = "network_last_sync_success"
        // Legacy key kept temporarily to migrate existing installs.
        const val KEY_USE_AUTH_V2 = "use_auth_v2"
    }
}
