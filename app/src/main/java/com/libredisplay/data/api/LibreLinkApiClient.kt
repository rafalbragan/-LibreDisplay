package com.libredisplay.data.api

import com.libredisplay.data.model.GlucoseReading

open class LibreLinkApiClient(
    private val productHeader: String = "llu.android",
    private val versionHeader: String = "4.17.0"
) {
    private val delegate = RetrofitLibreLinkUpClient(initialRegion = "EU")

    open suspend fun login(region: String, email: String, password: String): LoginResult {
        delegate.login(email, password, region)
        return LoginResult(token = "", region = region.uppercase(), accountIdHash = "")
    }

    open suspend fun getConnections(
        region: String,
        token: String,
        accountIdHash: String? = null
    ): Nothing = throw UnsupportedOperationException("Legacy LibreLinkApiClient is no longer used by the clean app")

    open suspend fun getLatestGraph(
        region: String,
        token: String,
        patientId: String,
        accountIdHash: String? = null
    ): Nothing = throw UnsupportedOperationException("Legacy LibreLinkApiClient is no longer used by the clean app")
}

data class LoginResult(
    val token: String,
    val region: String,
    val accountIdHash: String
)
