package com.libredisplay.data.api.v2

import com.libredisplay.data.api.RetrofitLibreLinkUpClient

class LibreLinkUpAuthV2(
    private val productHeader: String = "llu.android",
    private val versionHeader: String = "4.17.0"
) {
    private val delegate = RetrofitLibreLinkUpClient(initialRegion = "EU")

    suspend fun authorize(initialRegion: String, email: String, password: String): LibreLinkUpSessionV2 {
        delegate.login(email, password, initialRegion)
        return LibreLinkUpSessionV2(
            token = "",
            userId = "",
            accountIdHash = "",
            region = initialRegion.uppercase(),
            baseUrl = when (initialRegion.uppercase()) {
                "US" -> "https://api-us.libreview.io/"
                "DE" -> "https://api-de.libreview.io/"
                "FR" -> "https://api-fr.libreview.io/"
                else -> "https://api-eu.libreview.io/"
            },
            patientId = ""
        )
    }
}
