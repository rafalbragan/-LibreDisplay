package com.libredisplay.data.api.v3

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.libredisplay.data.api.LibreLinkUpException
import com.libredisplay.data.api.LibreLinkUpHttpException
import com.libredisplay.data.api.NonRetryableLibreLinkUpException
import com.libredisplay.diagnostics.DiagnosticLogger
import java.security.MessageDigest

private const val TAG = "LibreLinkUpAuthV3"

interface LibreLinkUpAuthV3Contract {
    suspend fun authorize(initialRegion: String, email: String, password: String): LibreLinkUpSessionV3
    suspend fun fetchGraph(session: LibreLinkUpSessionV3): LibreLinkUpGraphV3
}

class LibreLinkUpAuthV3(
    private val http: LibreLinkUpTransportV3 = LibreLinkUpHttpV3(),
    private val versionHeader: String = "4.17.0"
) : LibreLinkUpAuthV3Contract {

    override suspend fun authorize(initialRegion: String, email: String, password: String): LibreLinkUpSessionV3 {
        val normalizedRegion = normalizeRegion(initialRegion)
        val initialBaseUrl = baseUrlForRegion(normalizedRegion)

        logLoginContext(step = "STEP 1 LOGIN", region = normalizedRegion, baseUrl = initialBaseUrl, email = email, password = password)
        val firstLogin = executeLogin(baseUrl = initialBaseUrl, email = email, password = password)

        val finalLogin = if (firstLogin.redirect) {
            val redirectedRegion = firstLogin.redirectRegion?.trim().orEmpty().uppercase()
            if (redirectedRegion.isBlank()) {
                throw NonRetryableLibreLinkUpException("V3: redirect=true, ale brak data.region w odpowiedzi login.")
            }
            val redirectedBaseUrl = baseUrlForRegion(redirectedRegion)
            DiagnosticLogger.logInfo(TAG, "STEP 2 REDIRECT region=$normalizedRegion -> $redirectedRegion")
            logLoginContext(step = "STEP 2 LOGIN REDIRECT", region = redirectedRegion, baseUrl = redirectedBaseUrl, email = email, password = password)
            executeLogin(baseUrl = redirectedBaseUrl, email = email, password = password)
        } else {
            firstLogin
        }

        val token = finalLogin.token
            ?: run {
                logJsonSchema("LOGIN TOKEN MISSING", finalLogin.rawBody)
                throw NonRetryableLibreLinkUpException("V3: brak tokenu w sciezce data.authTicket.token")
            }

        val userId = finalLogin.userId
            ?: run {
                logJsonSchema("LOGIN USER ID MISSING", finalLogin.rawBody)
                throw NonRetryableLibreLinkUpException("V3: brak user.id w sciezce data.user.id")
            }

        val resolvedRegion = if (firstLogin.redirect) {
            firstLogin.redirectRegion?.trim().orEmpty().uppercase().ifBlank { normalizedRegion }
        } else {
            normalizedRegion
        }
        val resolvedBaseUrl = baseUrlForRegion(resolvedRegion)
        val accountIdHash = sha256Hex(userId)

        DiagnosticLogger.logInfo(TAG, "STEP 3 TOKEN tokenPresent=${token.isNotBlank()} tokenPrefix=${token.take(10)}...")
        DiagnosticLogger.logInfo(TAG, "STEP 4 USER userIdPresent=${userId.isNotBlank()} accountIdPrefix=${accountIdHash.take(10)}...")

        val connections = executeConnections(
            baseUrl = resolvedBaseUrl,
            token = token,
            accountIdHash = accountIdHash
        )
        val patientId = connections.firstOrNull()?.patientId
            ?: throw NonRetryableLibreLinkUpException("V3: brak data[0].patientId w odpowiedzi /llu/connections")

        DiagnosticLogger.logInfo(TAG, "STEP 7 PATIENT patientId=$patientId")

        return LibreLinkUpSessionV3(
            region = resolvedRegion,
            baseUrl = resolvedBaseUrl,
            token = token,
            userId = userId,
            accountIdHash = accountIdHash,
            patientId = patientId
        )
    }

    override suspend fun fetchGraph(session: LibreLinkUpSessionV3): LibreLinkUpGraphV3 {
        DiagnosticLogger.logInfo(TAG, "STEP 8 GRAPH region=${session.region} patientId=${session.patientId}")
        val response = http.getGraph(
            baseUrl = session.baseUrl,
            token = session.token,
            accountIdHash = session.accountIdHash,
            patientId = session.patientId
        )
        val bodyText = response.errorBodyTextOrEmpty().ifBlank { response.body()?.toString().orEmpty() }
        if (!response.isSuccessful) {
            throwForHttpCode("graph", response.code(), bodyText)
        }
        val body = response.body() ?: throw LibreLinkUpException("V3: puste body dla /llu/connections/{patientId}/graph")
        return LibreLinkUpGraphV3(patientId = session.patientId, payload = body)
    }

    private suspend fun executeLogin(baseUrl: String, email: String, password: String): LoginResponseV3 {
        val response = http.login(baseUrl, LoginRequestV3(email = email, password = password))
        val bodyText = response.errorBodyTextOrEmpty().ifBlank { response.body()?.toString().orEmpty() }

        if (!response.isSuccessful) {
            throwForHttpCode("login", response.code(), bodyText)
        }

        val body = response.body() ?: throw LibreLinkUpException("V3: puste body login")
        val status = body.readInt("status")
        val errorMessage = body.readString("error", "message")

        if (status == 2) {
            throw NonRetryableLibreLinkUpException(
                "LibreLinkUp odrzucil login status=2 (${errorMessage.orEmpty()}). URL=$baseUrl" +
                    ", version=$versionHeader"
            )
        }

        if (status != null && status != 0) {
            throw LibreLinkUpException("V3: nieobslugiwany status login=$status body=$bodyText")
        }

        val redirect = body.readBoolean("data", "redirect") ?: false
        val redirectRegion = body.readString("data", "region")

        return LoginResponseV3(
            status = status,
            redirect = redirect,
            redirectRegion = redirectRegion,
            token = body.readString("data", "authTicket", "token"),
            userId = body.readString("data", "user", "id"),
            errorMessage = errorMessage,
            rawBody = body
        )
    }

    private suspend fun executeConnections(baseUrl: String, token: String, accountIdHash: String): List<LibreLinkUpConnectionV3> {
        val response = http.getConnections(
            baseUrl = baseUrl,
            token = token,
            accountIdHash = accountIdHash
        )
        val bodyText = response.errorBodyTextOrEmpty().ifBlank { response.body()?.toString().orEmpty() }

        if (!response.isSuccessful) {
            throwForHttpCode("connections", response.code(), bodyText)
        }

        val body = response.body() ?: throw LibreLinkUpException("V3: puste body dla /llu/connections")
        val data = body.get("data")
        if (data == null || !data.isJsonArray) {
            logJsonSchema("CONNECTIONS INVALID SCHEMA", body)
            throw NonRetryableLibreLinkUpException("V3: oczekiwano tablicy data[] w /llu/connections")
        }

        val connections = data.asJsonArray.mapNotNull { node ->
            if (!node.isJsonObject) return@mapNotNull null
            val patientIdNode = node.asJsonObject.get("patientId")
            if (patientIdNode == null || !patientIdNode.isJsonPrimitive || !patientIdNode.asJsonPrimitive.isString) {
                return@mapNotNull null
            }
            LibreLinkUpConnectionV3(patientId = patientIdNode.asString)
        }

        DiagnosticLogger.logInfo(
            TAG,
            "STEP 6 CONNECTIONS tokenPresent=${token.isNotBlank()} accountIdPresent=${accountIdHash.isNotBlank()} count=${connections.size} patientIdPresent=${connections.firstOrNull()?.patientId?.isNotBlank() == true}"
        )

        return connections
    }

    private fun throwForHttpCode(step: String, httpCode: Int, bodyText: String): Nothing {
        val retryAfter = parseRetryAfter(bodyText)
        if (httpCode in setOf(400, 401, 403, 429, 430)) {
            val retryAfterText = if (httpCode == 429 && retryAfter != null) " retry_after=$retryAfter" else ""
            throw NonRetryableLibreLinkUpException(
                "V3 $step HTTP $httpCode.$retryAfterText body=${bodyText.ifBlank { "<empty>" }}"
            )
        }
        throw LibreLinkUpHttpException(httpCode, bodyText, null, "V3 $step failed")
    }

    private fun parseRetryAfter(bodyText: String): String? {
        val regex = Regex("\"retry_after\"\\s*:\\s*(\"[^\"]+\"|[0-9]+)", RegexOption.IGNORE_CASE)
        val match = regex.find(bodyText) ?: return null
        return match.groupValues.getOrNull(1)?.trim('"')
    }

    private fun baseUrlForRegion(region: String): String {
        return when (region.uppercase()) {
            "EU" -> "https://api-eu.libreview.io/"
            "US" -> "https://api-us.libreview.io/"
            "DE" -> "https://api-de.libreview.io/"
            "FR" -> "https://api-fr.libreview.io/"
            else -> throw NonRetryableLibreLinkUpException("V3: nieobslugiwany region '$region'")
        }
    }

    private fun normalizeRegion(region: String): String {
        return region.trim().uppercase().ifBlank { "EU" }
    }

    private fun JsonObject.readString(vararg path: String): String? {
        var current: JsonElement = this
        for (segment in path) {
            if (!current.isJsonObject) return null
            current = current.asJsonObject.get(segment) ?: return null
        }
        return if (current.isJsonPrimitive && current.asJsonPrimitive.isString) current.asString else null
    }

    private fun JsonObject.readBoolean(vararg path: String): Boolean? {
        var current: JsonElement = this
        for (segment in path) {
            if (!current.isJsonObject) return null
            current = current.asJsonObject.get(segment) ?: return null
        }
        if (!current.isJsonPrimitive) return null
        val primitive = current.asJsonPrimitive
        return when {
            primitive.isBoolean -> primitive.asBoolean
            primitive.isString -> primitive.asString.toBooleanStrictOrNull()
            else -> null
        }
    }

    private fun JsonObject.readInt(vararg path: String): Int? {
        var current: JsonElement = this
        for (segment in path) {
            if (!current.isJsonObject) return null
            current = current.asJsonObject.get(segment) ?: return null
        }
        if (!current.isJsonPrimitive) return null
        val primitive = current.asJsonPrimitive
        return when {
            primitive.isNumber -> primitive.asInt
            primitive.isString -> primitive.asString.toIntOrNull()
            else -> null
        }
    }

    private fun logLoginContext(step: String, region: String, baseUrl: String, email: String, password: String) {
        DiagnosticLogger.logInfo(TAG, step)
        DiagnosticLogger.logInfo(TAG, "region=$region")
        DiagnosticLogger.logInfo(TAG, "baseUrl=$baseUrl")
        DiagnosticLogger.logInfo(TAG, "endpoint=/llu/auth/login")
        DiagnosticLogger.logInfo(TAG, "product=llu.android")
        DiagnosticLogger.logInfo(TAG, "version=$versionHeader")
        DiagnosticLogger.logInfo(TAG, "email masked=${maskEmail(email)}")
        DiagnosticLogger.logInfo(TAG, "emailLength=${email.length}")
        DiagnosticLogger.logInfo(TAG, "passwordLength=${password.length}")
    }

    private fun logJsonSchema(prefix: String, json: JsonObject) {
        DiagnosticLogger.logError(TAG, "$prefix rootKeys=${json.keySet().sorted()}")
        json.entrySet().sortedBy { it.key }.forEach { (key, value) ->
            DiagnosticLogger.logError(TAG, "$prefix path=root.$key type=${describeType(value)}")
        }
    }

    private fun describeType(element: JsonElement): String {
        if (element.isJsonObject) return "object"
        if (element.isJsonArray) return "array"
        if (!element.isJsonPrimitive) return "unknown"
        val p = element.asJsonPrimitive
        return when {
            p.isBoolean -> "boolean"
            p.isNumber -> "number"
            p.isString -> "string"
            else -> "primitive"
        }
    }

    private fun maskEmail(email: String): String {
        val trimmed = email.trim()
        if (trimmed.isBlank() || !trimmed.contains("@")) return "<blank>"
        val parts = trimmed.split("@", limit = 2)
        val local = parts[0]
        val prefix = local.take(2)
        return "$prefix***@${parts[1]}"
    }

    private fun sha256Hex(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
        return digest.joinToString(separator = "") { b -> "%02x".format(b) }
    }

    private fun retrofit2.Response<JsonObject>.errorBodyTextOrEmpty(): String {
        return runCatching { errorBody()?.string().orEmpty() }.getOrDefault("")
    }
}

