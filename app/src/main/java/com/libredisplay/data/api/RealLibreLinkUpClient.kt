package com.libredisplay.data.api

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.libredisplay.data.model.GlucoseReading
import com.libredisplay.data.model.GlucoseTrend
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.Instant
import java.time.format.DateTimeParseException

/**
 * @param region The server region selected by the user (EU, US, DE, FR).
 */
class RealLibreLinkUpClient(private val region: String) : LibreLinkUpClient {

    private var authToken: String? = null
    private var patientId: String? = null

    private val api: LibreLinkUpApiService by lazy {
        val logging = HttpLoggingInterceptor().apply {
            // Keep logs minimal; never log credentials or tokens.
            level = HttpLoggingInterceptor.Level.BASIC
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        Retrofit.Builder()
            .baseUrl(resolveBaseUrl(region))
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(LibreLinkUpApiService::class.java)
    }

    override suspend fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            throw LibreLinkUpException("Podaj e-mail i haslo LibreLinkUp")
        }

        val response = api.login(LoginRequest(email = email, password = password))
        if (!response.isSuccessful) {
            throw LibreLinkUpException("Logowanie do LibreLinkUp nie powiodlo sie (HTTP ${response.code()})")
        }

        val body = response.body()
            ?: throw LibreLinkUpException("Brak odpowiedzi z serwera LibreLinkUp")

        val token = findFirstString(body, listOf("token", "authTicket", "jwtToken", "accessToken"))
            ?: throw LibreLinkUpException("Nie znaleziono tokenu autoryzacji w odpowiedzi LibreLinkUp")

        authToken = token
        patientId = null
    }

    override suspend fun getConnections(): List<String> {
        val token = authToken ?: throw LibreLinkUpException("Brak sesji. Zaloguj sie ponownie")
        val response = api.getConnections("Bearer $token")

        if (!response.isSuccessful) {
            throw LibreLinkUpException("Nie udalo sie pobrac listy polaczen (HTTP ${response.code()})")
        }

        val body = response.body() ?: return emptyList()
        val ids = mutableListOf<String>()
        collectConnectionIds(body, ids)

        val uniqueIds = ids.distinct()
        patientId = uniqueIds.firstOrNull()
        return uniqueIds
    }

    override suspend fun getLatestReading(): GlucoseReading? {
        val token = authToken ?: throw LibreLinkUpException("Brak sesji. Zaloguj sie ponownie")
        val connectionId = patientId ?: getConnections().firstOrNull()
            ?: throw LibreLinkUpException("Brak aktywnych polaczen LibreLinkUp")

        val response = api.getLatestGraph("Bearer $token", connectionId)
        if (!response.isSuccessful) {
            throw LibreLinkUpException("Nie udalo sie pobrac aktualnego pomiaru (HTTP ${response.code()})")
        }

        val body = response.body() ?: return null
        val glucoseValue = findFirstInt(body, listOf("Value", "value", "glucose", "Glucose", "FactoryValue"))
            ?: return null
        val trendCode = findFirstInt(body, listOf("TrendArrow", "trendArrow", "trend", "Trend")) ?: 0
        val timestamp = parseTimestamp(
            findFirstString(body, listOf("Timestamp", "FactoryTimestamp", "timestamp", "Date"))
        )

        return GlucoseReading.of(
            value = glucoseValue,
            timestamp = timestamp,
            trend = GlucoseTrend.fromApiCode(trendCode)
        )
    }

    private fun resolveBaseUrl(regionCode: String): String {
        return when (regionCode.uppercase()) {
            "US" -> "https://api-us.libreview.io/"
            "DE" -> "https://api-de.libreview.io/"
            "FR" -> "https://api-fr.libreview.io/"
            else -> "https://api-eu.libreview.io/"
        }
    }

    private fun collectConnectionIds(node: JsonElement, output: MutableList<String>) {
        if (node.isJsonObject) {
            val obj = node.asJsonObject
            listOf("patientId", "connectionId", "id").forEach { key ->
                val value = obj.get(key)
                if (value != null && value.isJsonPrimitive && value.asJsonPrimitive.isString) {
                    output += value.asString
                }
            }
            obj.entrySet().forEach { (_, value) -> collectConnectionIds(value, output) }
        } else if (node.isJsonArray) {
            node.asJsonArray.forEach { collectConnectionIds(it, output) }
        }
    }

    private fun findFirstString(root: JsonElement, candidateKeys: List<String>): String? {
        if (root.isJsonObject) {
            val obj = root.asJsonObject
            candidateKeys.forEach { key ->
                val v = obj.get(key)
                if (v != null && v.isJsonPrimitive && v.asJsonPrimitive.isString) {
                    return v.asString
                }
                if (v != null && v.isJsonObject && v.asJsonObject.get("token") != null) {
                    val token = v.asJsonObject.get("token")
                    if (token.isJsonPrimitive && token.asJsonPrimitive.isString) return token.asString
                }
            }
            obj.entrySet().forEach { (_, value) ->
                findFirstString(value, candidateKeys)?.let { return it }
            }
        } else if (root.isJsonArray) {
            root.asJsonArray.forEach {
                findFirstString(it, candidateKeys)?.let { value -> return value }
            }
        }
        return null
    }

    private fun findFirstInt(root: JsonElement, candidateKeys: List<String>): Int? {
        if (root.isJsonObject) {
            val obj = root.asJsonObject
            candidateKeys.forEach { key ->
                val v = obj.get(key)
                if (v != null && v.isJsonPrimitive) {
                    val primitive = v.asJsonPrimitive
                    if (primitive.isNumber) return primitive.asInt
                    if (primitive.isString) primitive.asString.toIntOrNull()?.let { return it }
                }
            }
            obj.entrySet().forEach { (_, value) ->
                findFirstInt(value, candidateKeys)?.let { return it }
            }
        } else if (root.isJsonArray) {
            root.asJsonArray.forEach {
                findFirstInt(it, candidateKeys)?.let { value -> return value }
            }
        }
        return null
    }

    private fun parseTimestamp(raw: String?): Instant {
        if (raw.isNullOrBlank()) return Instant.now()
        return try {
            Instant.parse(raw)
        } catch (_: DateTimeParseException) {
            Instant.now()
        }
    }
}

