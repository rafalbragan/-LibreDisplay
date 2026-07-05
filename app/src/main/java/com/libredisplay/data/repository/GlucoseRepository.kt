package com.libredisplay.data.repository

import android.util.Log
import com.google.gson.JsonElement
import com.libredisplay.data.api.LibreLinkUpClient
import com.libredisplay.data.api.LibreLinkApiClient
import com.libredisplay.data.api.LibreLinkUpException
import com.libredisplay.data.api.LibreLinkUpHttpException
import com.libredisplay.data.api.MockLibreLinkUpClient
import com.libredisplay.data.model.GlucoseReading
import com.libredisplay.data.model.GlucoseTrend
import java.time.Instant
import java.time.format.DateTimeParseException

private const val TAG = "GlucoseRepository"

/**
 * Orchestrates authentication and data fetching from LibreLinkUp.
 *
 * Selects between [MockLibreLinkUpClient] and [RealLibreLinkUpClient] based on
 * the current [AppSettings.useMock] flag.
 *
 * Never logs passwords or auth tokens.
 */
class GlucoseRepository(private val settingsRepository: SettingsRepository) {

    private var client: LibreLinkUpClient? = null
    private val apiClient = LibreLinkApiClient()
    private val authRepository = AuthRepository(settingsRepository, apiClient)
    private var loggedIn = false
    private var activeConnectionId: String? = null

    /**
     * Ensures the client is authenticated.  Called automatically before
     * [fetchLatestReading] if not already authenticated.
     */
    suspend fun ensureAuthenticated() {
        val settings = settingsRepository.loadSettings()
        if (settings.useMock) {
            val mockClient: LibreLinkUpClient = MockLibreLinkUpClient()
            client = mockClient
            mockClient.login(settings.email, settings.password)
            loggedIn = true
            return
        }

        // Real mode: keep one authenticated session and reuse token.
        authRepository.ensureLoggedIn(force = false)
        loggedIn = true
        Log.d(TAG, "Authentication successful (useMock=false)")
    }

    /**
     * Fetches the latest glucose reading.
     *
     * Re-authenticates automatically if not logged in.
     *
     * @return [GlucoseReading] or null if no data is available.
     * @throws Exception propagated from the underlying client.
     */
    suspend fun fetchLatestReading(): GlucoseReading? {
        val settings = settingsRepository.loadSettings()
        if (!loggedIn || (settings.useMock && client == null)) {
            ensureAuthenticated()
        }

        if (settings.useMock) {
            return client!!.getLatestReading()
        }

        return fetchRealReadingWithRetry()
    }

    private suspend fun fetchRealReadingWithRetry(): GlucoseReading? {
        return try {
            fetchRealReadingOnce()
        } catch (e: LibreLinkUpHttpException) {
            when (e.statusCode) {
                401 -> {
                    // One controlled relogin attempt.
                    authRepository.clearSession()
                    authRepository.ensureLoggedIn(force = true)
                    fetchRealReadingOnce()
                }
                429, 430 -> {
                    authRepository.activateCooldown()
                    throw LibreLinkUpException("Zbyt wiele prob polaczenia. Sprobuj pozniej lub sprawdz dane logowania.")
                }
                else -> throw LibreLinkUpException("Blad pobierania danych glukozy (HTTP ${e.statusCode})")
            }
        }
    }

    private suspend fun fetchRealReadingOnce(): GlucoseReading? {
        authRepository.ensureLoggedIn(force = false)
        val token = authRepository.currentToken()
            ?: throw LibreLinkUpException("Brak aktywnej sesji LibreLinkUp")
        val region = authRepository.currentRegion()

        if (activeConnectionId == null) {
            val connectionsResponse = apiClient.getConnections(region, token)
            if (!connectionsResponse.isSuccessful) {
                val err = runCatching { connectionsResponse.errorBody()?.string().orEmpty() }.getOrDefault("")
                throw LibreLinkUpHttpException(connectionsResponse.code(), err)
            }
            val body = connectionsResponse.body()
            activeConnectionId = body?.let { collectConnectionIds(it).firstOrNull() }
        }

        val connectionId = activeConnectionId
            ?: throw LibreLinkUpException("Brak aktywnego polaczenia LibreLinkUp")

        val graphResponse = apiClient.getLatestGraph(region, token, connectionId)
        if (!graphResponse.isSuccessful) {
            val err = runCatching { graphResponse.errorBody()?.string().orEmpty() }.getOrDefault("")
            throw LibreLinkUpHttpException(graphResponse.code(), err)
        }

        val body = graphResponse.body() ?: return null
        val value = firstInt(body, listOf("Value", "value", "glucose", "Glucose", "FactoryValue")) ?: return null
        val trendCode = firstInt(body, listOf("TrendArrow", "trendArrow", "trend", "Trend")) ?: 0
        val timestampRaw = firstString(body, listOf("Timestamp", "FactoryTimestamp", "timestamp", "Date"))
        val timestamp = parseTimestamp(timestampRaw)

        return GlucoseReading.of(
            value = value,
            timestamp = timestamp,
            trend = GlucoseTrend.fromApiCode(trendCode)
        )
    }

    private fun collectConnectionIds(node: JsonElement): List<String> {
        val out = mutableListOf<String>()

        fun walk(element: JsonElement) {
            if (element.isJsonObject) {
                val obj = element.asJsonObject
                listOf("patientId", "connectionId", "id").forEach { key ->
                    val value = obj.get(key)
                    if (value != null && value.isJsonPrimitive && value.asJsonPrimitive.isString) {
                        out += value.asString
                    }
                }
                obj.entrySet().forEach { (_, value) -> walk(value) }
            } else if (element.isJsonArray) {
                element.asJsonArray.forEach { walk(it) }
            }
        }

        walk(node)
        return out.distinct()
    }

    private fun firstString(node: JsonElement, keys: List<String>): String? {
        if (node.isJsonObject) {
            val obj = node.asJsonObject
            keys.forEach { key ->
                val value = obj.get(key)
                if (value != null && value.isJsonPrimitive && value.asJsonPrimitive.isString) {
                    return value.asString
                }
            }
            obj.entrySet().forEach { (_, value) ->
                firstString(value, keys)?.let { return it }
            }
        } else if (node.isJsonArray) {
            node.asJsonArray.forEach {
                firstString(it, keys)?.let { value -> return value }
            }
        }
        return null
    }

    private fun firstInt(node: JsonElement, keys: List<String>): Int? {
        if (node.isJsonObject) {
            val obj = node.asJsonObject
            keys.forEach { key ->
                val value = obj.get(key)
                if (value != null && value.isJsonPrimitive) {
                    val p = value.asJsonPrimitive
                    if (p.isNumber) return p.asInt
                    if (p.isString) p.asString.toIntOrNull()?.let { return it }
                }
            }
            obj.entrySet().forEach { (_, value) ->
                firstInt(value, keys)?.let { return it }
            }
        } else if (node.isJsonArray) {
            node.asJsonArray.forEach {
                firstInt(it, keys)?.let { value -> return value }
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

    /** Force re-authentication on the next call (e.g. after settings change). */
    fun invalidate() {
        client = null
        loggedIn = false
        activeConnectionId = null
        authRepository.clearSession()
    }
}

