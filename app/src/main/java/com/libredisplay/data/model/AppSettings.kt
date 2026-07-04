package com.libredisplay.data.model

/**
 * User-defined application settings.
 *
 * @param email           LibreLinkUp account email.
 * @param password        LibreLinkUp account password (stored encrypted).
 * @param region          Server region (EU, US, DE, FR).
 * @param refreshInterval Polling interval in minutes.
 * @param kioskMode       When true the app hides the navigation bar and prevents leaving.
 * @param useMock         When true the app uses MockLibreLinkUpClient (for testing).
 */
data class AppSettings(
    val email: String = "",
    val password: String = "",
    val region: String = "EU",
    val refreshInterval: Int = 5,
    val kioskMode: Boolean = false,
    val useMock: Boolean = true
)

