package com.libredisplay.data.api

import com.libredisplay.BuildConfig

object LibreLinkUpConfig {
    private const val DEFAULT_BASE_URL = "https://api-eu.libreview.io"
    private const val DEFAULT_VERSION = "4.17.0"

    fun baseUrlOverrideOrNull(): String? {
        val raw = BuildConfig.LIBRE_API_BASE_URL.trim()
        return raw.ifBlank { "" }
            .let { normalizeBaseUrl(it) }
            .takeIf { it != normalizeBaseUrl(DEFAULT_BASE_URL) }
    }

    fun defaultBaseUrl(): String = normalizeBaseUrl(
        BuildConfig.LIBRE_API_BASE_URL.trim().ifBlank { DEFAULT_BASE_URL }
    )

    fun linkUpVersion(): String = BuildConfig.LIBRE_LINKUP_VERSION.trim().ifBlank { DEFAULT_VERSION }

    fun preferredPatientId(): String? = BuildConfig.LIBRE_PATIENT_ID.trim().ifBlank { null }

    fun normalizeBaseUrl(raw: String): String = raw.trim().removeSuffix("/") + "/"
}
