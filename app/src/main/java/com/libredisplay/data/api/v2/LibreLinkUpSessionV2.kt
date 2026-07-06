package com.libredisplay.data.api.v2

/**
 * Session state for the strict V2 auth flow.
 */
data class LibreLinkUpSessionV2(
    val region: String,
    val baseUrl: String,
    val token: String,
    val userId: String,
    val accountIdHash: String,
    val patientId: String
) {
    val accountId: String get() = accountIdHash
}
