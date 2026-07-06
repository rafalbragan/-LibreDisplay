package com.libredisplay.data.model

import com.libredisplay.ui.monitoring.DisplaySettings

data class AppSettings(
    val email: String = "",
    val password: String = "",
    val region: String = "EU",
    val regionMode: String = "EU",
    val customBaseUrl: String = "",
    val refreshInterval: Int = 15,
    val targetLow: Int = 80,
    val targetHigh: Int = 180,
    val trendWindowMinutes: Int = DisplaySettings.DEFAULT_TREND_WINDOW_MINUTES,
    val showStatistics: Boolean = true,
    val kioskMode: Boolean = false,
    val useMock: Boolean = false,
    val useAuthV3: Boolean = true
) {
    fun isConfigured(): Boolean = useMock || (email.isNotBlank() && password.isNotBlank())

    fun loginRegionSelection(): String {
        return when (regionMode.uppercase()) {
            "AUTO" -> "EU"
            "GLOBAL" -> "GLOBAL"
            "CUSTOM" -> customBaseUrl
            else -> regionMode.uppercase()
        }
    }
}
