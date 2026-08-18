package com.libredisplay.data.model

import com.libredisplay.ui.monitoring.DisplaySettings

data class AppSettings(
    val email: String = "",
    val password: String = "",
    val selectedPatientId: String? = null,
    val region: String = "EU",
    val regionMode: String = "EU",
    val customBaseUrl: String = "",
    val refreshInterval: Int = 60,
    val targetLow: Int = 80,
    val targetHigh: Int = 180,
    val trendWindowMinutes: Int = DisplaySettings.DEFAULT_TREND_WINDOW_MINUTES,
    val showStatistics: Boolean = true,
    val kioskMode: Boolean = false,
    val appMode: AppMode = AppMode.NONE,
    val useAuthV3: Boolean = true
) {
    val useMock: Boolean
        get() = appMode == AppMode.DEMO

    fun hasCredentials(): Boolean = email.isNotBlank() && password.isNotBlank()

    fun isConfigured(): Boolean = appMode == AppMode.DEMO || (appMode == AppMode.LIVE && hasCredentials())

    fun isDemoPatientSelected(): Boolean = selectedPatientId?.startsWith("demo-person-") == true

    fun loginRegionSelection(): String {
        return when (regionMode.uppercase()) {
            "AUTO" -> "EU"
            "GLOBAL" -> "GLOBAL"
            "CUSTOM" -> customBaseUrl
            else -> regionMode.uppercase()
        }
    }
}
