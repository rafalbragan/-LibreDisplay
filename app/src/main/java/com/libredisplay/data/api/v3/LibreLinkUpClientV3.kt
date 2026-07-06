package com.libredisplay.data.api.v3

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.libredisplay.data.api.LibreLinkUpClient
import com.libredisplay.data.api.LibreLinkUpException
import com.libredisplay.data.model.GlucoseReading
import com.libredisplay.data.model.GlucoseTrend
import com.libredisplay.diagnostics.DiagnosticLogger
import java.time.Instant

private const val TAG = "LibreLinkUpClientV3"

class LibreLinkUpClientV3(
    private val initialRegion: String,
    private val authV3: LibreLinkUpAuthV3Contract = LibreLinkUpAuthV3()
) : LibreLinkUpClient {

    private var session: LibreLinkUpSessionV3? = null

    override suspend fun login(email: String, password: String) {
        session = authV3.authorize(initialRegion = initialRegion, email = email, password = password)
        DiagnosticLogger.logInfo(
            TAG,
            "V3 login complete region=${session?.region} patientId=${session?.patientId} tokenPrefix=${session?.token?.take(10)}..."
        )
    }

    override suspend fun getConnections(): List<String> {
        val current = session ?: throw LibreLinkUpException("V3 session missing. Call login() first.")
        return listOf(current.patientId)
    }

    override suspend fun getLatestReading(): GlucoseReading? {
        val current = session ?: throw LibreLinkUpException("V3 session missing. Call login() first.")
        val graph = authV3.fetchGraph(current)
        return parseGraph(graph)
    }

    private fun parseGraph(graph: LibreLinkUpGraphV3): GlucoseReading? {
        val payload = graph.payload
        val graphDataCount = readArraySize(payload, "data", "graphData") ?: readArraySize(payload, "graphData") ?: 0

        val measurement = readNodeAtPath(payload, "data.connection.glucoseMeasurement")
            ?.takeIf { it.isJsonObject }
            ?.asJsonObject
            ?: return null

        val value = readInt(measurement, "Value") ?: readInt(measurement, "value") ?: return null
        val trendCode = readInt(measurement, "TrendArrow") ?: readInt(measurement, "trendArrow") ?: 0
        val tsRaw = readString(measurement, "FactoryTimestamp") ?: readString(measurement, "Timestamp")

        DiagnosticLogger.logInfo(
            TAG,
            "V3 graph patientId=${graph.patientId} graphDataCount=$graphDataCount currentGlucoseFound=true"
        )

        return GlucoseReading.of(
            value = value,
            timestamp = parseTimestamp(tsRaw),
            trend = GlucoseTrend.fromApiCode(trendCode)
        )
    }

    private fun readArraySize(root: JsonObject, vararg path: String): Int? {
        var current: JsonElement = root
        for (segment in path) {
            if (!current.isJsonObject) return null
            current = current.asJsonObject.get(segment) ?: return null
        }
        if (!current.isJsonArray) return null
        return current.asJsonArray.size()
    }

    private fun readNodeAtPath(root: JsonObject, path: String): JsonElement? {
        var current: JsonElement = root
        path.split('.').forEach { key ->
            if (!current.isJsonObject) return null
            current = current.asJsonObject.get(key) ?: return null
        }
        return current
    }

    private fun readInt(obj: JsonObject, key: String): Int? {
        val value = obj.get(key) ?: return null
        if (!value.isJsonPrimitive) return null
        val p = value.asJsonPrimitive
        return when {
            p.isNumber -> p.asInt
            p.isString -> p.asString.toIntOrNull()
            else -> null
        }
    }

    private fun readString(obj: JsonObject, key: String): String? {
        val value = obj.get(key) ?: return null
        if (!value.isJsonPrimitive || !value.asJsonPrimitive.isString) return null
        return value.asString
    }

    private fun parseTimestamp(raw: String?): Instant {
        if (raw.isNullOrBlank()) return Instant.now()
        return runCatching { Instant.parse(raw) }.getOrElse { Instant.now() }
    }
}

