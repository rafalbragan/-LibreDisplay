package com.libredisplay.data.api.v3

data class LibreLinkUpSessionV3(
    val region: String,
    val baseUrl: String,
    val token: String,
    val userId: String,
    val accountIdHash: String,
    val patientId: String
)

