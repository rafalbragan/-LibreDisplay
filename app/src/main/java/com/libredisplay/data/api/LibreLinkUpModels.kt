package com.libredisplay.data.api

import com.google.gson.annotations.SerializedName
import com.google.gson.JsonObject
import java.time.Instant

data class LoginRequest(
    val email: String,
    val password: String
)

data class LibreLoginError(
    val message: String? = null,
    val lockout: Int? = null
)

data class LibreAuthTicket(
    val token: String? = null,
    val expires: Long? = null
)

data class LibreLoginData(
    val redirect: Boolean? = null,
    val region: String? = null,
    val authTicket: LibreAuthTicket? = null,
    val user: JsonObject? = null
)

data class LibreLoginResponse(
    val status: Int? = null,
    val data: LibreLoginData? = null,
    val error: LibreLoginError? = null
)

data class LibreGraphResponse(
    val status: Int? = null,
    val data: LibreGraphData? = null
)

data class LibreGraphData(
    val connection: LibreGraphConnection? = null,
    @SerializedName("graphData")
    val graphData: List<LibreGraphMeasurement>? = null
)

data class LibreGraphConnection(
    @SerializedName("glucoseMeasurement")
    val glucoseMeasurement: LibreGraphMeasurement? = null
)

data class LibreGraphMeasurement(
    @SerializedName("FactoryTimestamp")
    val factoryTimestamp: String? = null,
    @SerializedName("Timestamp")
    val timestamp: String? = null,
    @SerializedName("ValueInMgPerDl")
    val valueInMgPerDl: Double? = null,
    @SerializedName("Value")
    val value: Double? = null,
    @SerializedName("GlucoseUnits")
    val glucoseUnits: Int? = null,
    @SerializedName("TrendArrow")
    val trendArrow: Int? = null,
    val isHigh: Boolean? = null,
    val isLow: Boolean? = null
)

data class LibreLinkUpSession(
    val token: String,
    val userId: String,
    val accountIdHash: String,
    val region: String,
    val baseUrl: String,
    val tokenExpiresAtEpochSeconds: Long? = null
)

data class PersistedLibreLinkUpSession(
    val token: String,
    val userId: String,
    val accountIdHash: String,
    val region: String,
    val baseUrl: String,
    val tokenExpiresAtEpochSeconds: Long? = null
)

data class LibreLockoutInfo(
    val apiStatus: Int? = null,
    val apiCode: Int? = null,
    val message: String? = null,
    val failures: Int? = null,
    val intervalSeconds: Int? = null,
    val lockoutSeconds: Int? = null,
    val retryAfterSeconds: Int? = null
)

data class LibreLinkUpConnection(
    val patientId: String,
    val raw: JsonObject
)

data class NormalizedGlucosePoint(
    val value: Int,
    val timestamp: Instant,
    val trendArrow: Int? = null
)

data class NormalizedGlucosePayload(
    val value: Int,
    val unit: String,
    val timestamp: Instant,
    val trendArrow: Int,
    val isHigh: Boolean,
    val isLow: Boolean,
    val raw: JsonObject,
    val history: List<NormalizedGlucosePoint>
)
