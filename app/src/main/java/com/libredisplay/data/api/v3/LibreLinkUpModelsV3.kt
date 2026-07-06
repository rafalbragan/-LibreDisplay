package com.libredisplay.data.api.v3

import com.google.gson.JsonObject

data class LoginRequestV3(
    val email: String,
    val password: String
)

data class LoginResponseV3(
    val status: Int?,
    val redirect: Boolean,
    val redirectRegion: String?,
    val token: String?,
    val userId: String?,
    val errorMessage: String?,
    val rawBody: JsonObject
)

