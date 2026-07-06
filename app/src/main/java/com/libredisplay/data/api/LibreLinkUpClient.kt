package com.libredisplay.data.api

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.libredisplay.data.model.GlucoseHistoryPoint
import com.libredisplay.data.model.GlucoseHistoryStats
import com.libredisplay.data.model.GlucoseReading
import com.libredisplay.data.model.calculateLast12HoursStats
import com.libredisplay.data.model.GlucoseTrend
import com.libredisplay.diagnostics.DiagnosticLogger
import com.libredisplay.diagnostics.DiagnosticStatus
import com.google.gson.JsonParser
import java.security.MessageDigest
import java.time.Instant
import java.util.Base64
import java.util.concurrent.atomic.AtomicLong

interface LibreLinkUpClient {
    suspend fun login(email: String, password: String)
    suspend fun getConnections(): List<String>
    suspend fun getLatestReading(): GlucoseReading?
}

interface AuthCapableLibreLinkUpClient {
    suspend fun login(email: String, password: String, region: String)
    fun hasActiveSession(): Boolean
    fun clearSession()
    fun exportSession(): PersistedLibreLinkUpSession?
    fun importSession(session: PersistedLibreLinkUpSession)
}

class RetrofitLibreLinkUpClient(
    private val initialRegion: String = "EU",
    private val http: LibreLinkUpHttp = OkHttpLibreLinkUpHttp(),
    private val apiBaseUrlOverride: String? = LibreLinkUpConfig.baseUrlOverrideOrNull(),
    private val preferredPatientId: String? = LibreLinkUpConfig.preferredPatientId()
) : LibreLinkUpClient, AuthCapableLibreLinkUpClient {

    private var session: LibreLinkUpSession? = null
    private var patientId: String? = null
    private var requestedRegion: String = initialRegion.uppercase()
    private var lastCredentials: LoginRequest? = null
    private val loginAttemptCounter = AtomicLong(0)

    override suspend fun login(email: String, password: String) {
        login(email, password, requestedRegion)
    }

    override suspend fun login(email: String, password: String, region: String) {
        requestedRegion = region.uppercase().ifBlank { "AUTO" }
        lastCredentials = LoginRequest(email = email, password = password)
        session = performLogin(
            email = email,
            password = password,
            region = requestedRegion,
            redirectCount = 0
        )
        patientId = null
    }

    override suspend fun getConnections(): List<String> {
        val activeSession = session ?: throw LibreLinkUpException("Brak aktywnej sesji LibreLinkUp")
        val connectionsBody = executeJsonStep(step = "CONNECTIONS") {
            http.getConnections(activeSession.baseUrl, activeSession.token, activeSession.accountIdHash)
        }

        val patientIds = extractPatientIds(connectionsBody)
        if (patientIds.isEmpty()) {
            throw NonRetryableLibreLinkUpException("Brak patientId w odpowiedzi connections")
        }

        val configuredPatient = preferredPatientId?.takeIf { it.isNotBlank() }
        val selectedPatient = when {
            configuredPatient == null -> patientIds.first()
            patientIds.contains(configuredPatient) -> configuredPatient
            else -> throw NonRetryableLibreLinkUpException("Skonfigurowany LIBRE_PATIENT_ID nie istnieje na liscie connections")
        }

        patientId = selectedPatient
        DiagnosticLogger.logInfo(
            "LibreLinkUpClient",
            "Connections count=${patientIds.size} selectedPatientId=$selectedPatient preferredConfigured=${configuredPatient != null}"
        )
        DiagnosticStatus.setGetConnections("OK")
        return patientIds
    }

    override suspend fun getLatestReading(): GlucoseReading {
        val activeSession = session ?: throw LibreLinkUpException("Brak aktywnej sesji LibreLinkUp")
        val activePatientId = patientId ?: getConnections().firstOrNull()
            ?: throw NonRetryableLibreLinkUpException("Brak patientId do pobrania wykresu")

        DiagnosticLogger.logInfo(
            "LibreLinkUpClient",
            "GRAPH REQUEST START patientIdPresent=${activePatientId.isNotBlank()}"
        )

        val graphResponse = http.getGraph(
            baseUrl = activeSession.baseUrl,
            token = activeSession.token,
            accountIdHash = activeSession.accountIdHash,
            patientId = activePatientId
        )
        val graphBody = requireJsonSuccess(step = "GRAPH", response = graphResponse)

        val measurement = findCurrentMeasurement(graphBody)
            ?: throw NonRetryableLibreLinkUpException("Brak aktualnego pomiaru glukozy w odpowiedzi graph")

        val currentValue = measurement.intAt("ValueInMgPerDl")
            ?: measurement.intAt("Value")
            ?: measurement.intAt("value")
            ?: throw NonRetryableLibreLinkUpException("Brak aktualnej wartosci glukozy w graph")
        val currentTimestamp = parseTimestamp(
            measurement.stringAt("Timestamp")
                ?: measurement.stringAt("FactoryTimestamp")
                ?: measurement.stringAt("timestamp")
        ) ?: Instant.now()
        val trendCode = measurement.intAt("TrendArrow") ?: measurement.intAt("trendArrow") ?: 0
        val trend = GlucoseTrend.fromApiCode(trendCode)
        val currentPoint = GlucoseHistoryPoint(currentValue, currentTimestamp, trend)

        val graphDataRaw = findGraphData(graphBody)
        val mappingResult = mapHistoryWithDiagnostics(graphDataRaw)
        val history = mappingResult.mapped
            .distinctBy { it.timestamp }
            .sortedBy { it.timestamp }
        val rangeStats = calculateLast12HoursStats(
            history = history,
            current = currentPoint,
            now = Instant.now()
        )
        val stats = if (rangeStats.usedPoints.isNotEmpty()) {
            GlucoseHistoryStats.from(rangeStats.usedPoints, 80, 180)
        } else {
            GlucoseHistoryStats()
        }
        val historyHours = historyHoursAvailable(rangeStats.usedPoints)
        val warning = detectTrendMismatch(trend, history)
        val normalized = normalizePayload(
            measurement = measurement,
            graphBody = graphBody,
            currentValue = currentValue,
            currentTimestamp = currentTimestamp,
            trendCode = trendCode,
            history = history
        )

        if (warning != null) {
            DiagnosticLogger.logWarning("LibreLinkUpClient", warning)
            DiagnosticStatus.setWarning(warning)
        }

        val distinctValues = history.map { it.value }.distinct().size
        DiagnosticLogger.logInfo(
            "LibreLinkUpClient",
            buildString {
                appendLine("GRAPH RESPONSE")
                appendLine("httpCode=${graphResponse.code()}")
                appendLine("apiStatus=${graphBody.intAt("status") ?: "n/a"}")
                appendLine("hasCurrentMeasurement=${measurement.entrySet().isNotEmpty()}")
                appendLine("graphDataPresent=${graphDataRaw.isNotEmpty()}")
                appendLine("graphDataRawCount=${mappingResult.rawCount}")
                appendLine("graphDataMappedCount=${mappingResult.mappedCount}")
                appendLine("graphDataValidValueCount=${mappingResult.validValueCount}")
                appendLine("graphDataDistinctValueCount=$distinctValues")
                appendLine("oldestHistoryTimestamp=${rangeStats.oldestTimestamp ?: "n/a"}")
                appendLine("newestHistoryTimestamp=${rangeStats.newestTimestamp ?: "n/a"}")
                appendLine("currentTimestamp=$currentTimestamp")
                appendLine("calculatedMin12h=${rangeStats.minimum ?: "n/a"}")
                appendLine("calculatedMax12h=${rangeStats.maximum ?: "n/a"}")
                appendLine("historyFallbackToCurrent=false")
                if (mappingResult.rawCount > 0 && mappingResult.mappedCount == 0) {
                    appendLine("invalidTimestampCount=${mappingResult.invalidTimestampCount}")
                    appendLine("missingValueCount=${mappingResult.missingValueCount}")
                    appendLine("invalidValueCount=${mappingResult.invalidValueCount}")
                    appendLine("duplicateCount=${mappingResult.duplicateCount}")
                    appendLine("outsideWindowCount=${rangeStats.outsideWindowCount}")
                }
            }
        )

        val historyRange = if (history.isNotEmpty()) {
            "oldest=${history.first().timestamp} newest=${history.last().timestamp}"
        } else {
            "oldest=n/a newest=n/a"
        }
        DiagnosticLogger.logInfo(
            "LibreLinkUpClient",
            "Graph patientId=$activePatientId historyCount=${history.size} $historyRange unit=${normalized.unit}"
        )
        DiagnosticStatus.setGetLatestGraph("OK")

        return GlucoseReading.of(
            value = normalized.value,
            timestamp = normalized.timestamp,
            trend = trend,
            history = history,
            stats = stats,
            trendApiCode = normalized.trendArrow,
            trendValidationWarning = warning,
            historyHoursAvailable = historyHours
        )
    }

    override fun hasActiveSession(): Boolean = session != null

    override fun clearSession() {
        session = null
        patientId = null
    }

    override fun exportSession(): PersistedLibreLinkUpSession? {
        val active = session ?: return null
        return PersistedLibreLinkUpSession(
            token = active.token,
            userId = active.userId,
            accountIdHash = active.accountIdHash,
            region = active.region,
            baseUrl = active.baseUrl,
            tokenExpiresAtEpochSeconds = active.tokenExpiresAtEpochSeconds
        )
    }

    override fun importSession(session: PersistedLibreLinkUpSession) {
        this.requestedRegion = session.region.uppercase().ifBlank { "EU" }
        this.session = LibreLinkUpSession(
            token = session.token,
            userId = session.userId,
            accountIdHash = session.accountIdHash,
            region = session.region,
            baseUrl = session.baseUrl,
            tokenExpiresAtEpochSeconds = session.tokenExpiresAtEpochSeconds
        )
    }

    private suspend fun performLogin(
        email: String,
        password: String,
        region: String,
        redirectCount: Int
    ): LibreLinkUpSession {
        val baseUrl = baseUrlForRegion(region)
        val attemptId = loginAttemptCounter.incrementAndGet()
        var loginNetworkRequestCount = 0
        DiagnosticLogger.logInfo("LibreLinkUpClient", "LOGIN ATTEMPT START attemptId=$attemptId")
        DiagnosticLogger.logInfo(
            "LibreLinkUpClient",
            "LOGIN attemptId=$attemptId regionSelection=$region resolvedBaseUrl=$baseUrl redirectCount=$redirectCount"
        )

        try {
            loginNetworkRequestCount += 1
            val request = LoginRequest(email = email, password = password)
            if (request.email != email) {
                throw NonRetryableLibreLinkUpException("Wykryto niespojnosc danych logowania. Zadanie nie zostalo wyslane.")
            }
            val response = http.login(baseUrl, request)
            val body = requireJsonSuccess(step = "LOGIN", response = response)

            val status = body.intAt("status") ?: 0
            if (status == 2) {
                val detail = "LibreLinkUp odrzucil logowanie. Nie oznacza to jednoznacznie, ze wpisane haslo jest bledne. Kolejna proba nie zostanie wykonana automatycznie."
                DiagnosticStatus.setLogin(ok = false, detail = detail)
                DiagnosticLogger.logInfo("LibreLinkUpClient", "LOGIN ATTEMPT END attemptId=$attemptId result=failure")
                throw NonRetryableLibreLinkUpException(detail)
            }

            if (status == 4) {
                val detail = "Logowanie wymaga dodatkowej akcji konta (np. akceptacji Terms/Privacy lub weryfikacji)."
                DiagnosticStatus.setLogin(ok = false, detail = detail)
                DiagnosticLogger.logInfo("LibreLinkUpClient", "LOGIN ATTEMPT END attemptId=$attemptId result=failure")
                throw NonRetryableLibreLinkUpException(detail)
            }

            val redirect = body.objectAt("data")?.boolAt("redirect") == true
            if (redirect) {
                val redirectRegion = body.objectAt("data")?.stringAt("region").orEmpty()
                val detail = "Serwer zasugerowal region: ${redirectRegion.ifBlank { "unknown" }}. Nie wykonano kolejnej proby logowania. Zmien region recznie i sprobuj ponownie."
                DiagnosticStatus.setLogin(ok = false, detail = detail)
                DiagnosticLogger.logInfo("LibreLinkUpClient", "LOGIN ATTEMPT END attemptId=$attemptId result=failure")
                throw NonRetryableLibreLinkUpException(detail)
            }

            DiagnosticLogger.logInfo("LibreLinkUpClient", "LOGIN attemptId=$attemptId tokenPath=data.authTicket.token")
            val token = body.stringAt("data", "authTicket", "token")
                ?: run {
                    DiagnosticLogger.logJsonSchema("LOGIN TOKEN MISSING", body)
                    DiagnosticLogger.logInfo("LibreLinkUpClient", "LOGIN ATTEMPT END attemptId=$attemptId result=failure")
                    throw NonRetryableLibreLinkUpException("Brak tokenu w sciezce data.authTicket.token")
                }
            val userId = body.stringAt("data", "user", "id")
                ?: run {
                    DiagnosticLogger.logJsonSchema("LOGIN USER ID MISSING", body)
                    DiagnosticLogger.logInfo("LibreLinkUpClient", "LOGIN ATTEMPT END attemptId=$attemptId result=failure")
                    throw NonRetryableLibreLinkUpException("Brak user.id w sciezce data.user.id")
                }

            val accountIdHash = sha256(userId)
            val tokenExpiry = extractJwtExpiryEpochSeconds(token)
            DiagnosticStatus.setLogin(ok = true)
            DiagnosticStatus.setToken(true)
            DiagnosticLogger.logInfo("LibreLinkUpClient", "LOGIN ATTEMPT END attemptId=$attemptId result=success")
            return LibreLinkUpSession(
                token = token,
                userId = userId,
                accountIdHash = accountIdHash,
                region = region,
                baseUrl = baseUrl,
                tokenExpiresAtEpochSeconds = tokenExpiry
            )
        } catch (throwable: Throwable) {
            if (throwable !is NonRetryableLibreLinkUpException) {
                DiagnosticLogger.logInfo("LibreLinkUpClient", "LOGIN ATTEMPT END attemptId=$attemptId result=failure")
            }
            throw throwable
        } finally {
            DiagnosticLogger.logInfo(
                "LibreLinkUpClient",
                "LOGIN NETWORK REQUEST COUNT attemptId=$attemptId count=$loginNetworkRequestCount"
            )
        }
    }

    private suspend fun executeJsonStep(
        step: String,
        request: suspend () -> retrofit2.Response<JsonObject>
    ): JsonObject {
        val response = request()
        return requireJsonSuccess(step = step, response = response)
    }

    private suspend fun <T> executeWithNetworkRetry(step: String, request: suspend () -> T): T {
        // Auto retry is disabled; perform exactly one request attempt.
        return request()
    }

    private suspend fun tryReloginOnce(): Boolean {
        // Auto relogin disabled in single-attempt mode.
        return false
    }

    private fun requireJsonSuccess(
        step: String,
        response: retrofit2.Response<JsonObject>
    ): JsonObject {
        if (!response.isSuccessful) {
            val rawBody = runCatching { response.errorBody()?.string().orEmpty() }.getOrDefault("")
            val headerRetry = parseRetryAfterHeader(response.headers()["Retry-After"])
            DiagnosticStatus.setLastError("$step HTTP ${response.code()}")
            throw LibreLinkUpHttpException(
                statusCode = response.code(),
                responseBody = rawBody,
                retryAfterSeconds = headerRetry ?: extractRetryAfterSeconds(rawBody),
                lockoutInfo = parseLockoutInfo(rawBody, response.code(), headerRetry ?: extractRetryAfterSeconds(rawBody)),
                message = "HTTP ${response.code()}"
            )
        }

        val body = response.body() ?: throw LibreResponseDecodingException(
            message = "Pusta odpowiedz JSON dla kroku $step",
            encoding = response.headers()["Content-Encoding"],
            contentType = response.headers()["Content-Type"]
        )
        val logicalStatus = body.intAt("status")
        if (logicalStatus in setOf(400, 401, 403, 429, 430, 500)) {
            val rawBody = body.toString()
            throw LibreLinkUpHttpException(
                statusCode = logicalStatus ?: response.code(),
                responseBody = rawBody,
                retryAfterSeconds = extractRetryAfterSeconds(rawBody),
                lockoutInfo = parseLockoutInfo(rawBody, logicalStatus, extractRetryAfterSeconds(rawBody)),
                message = "HTTP ${logicalStatus ?: response.code()}"
            )
        }
        val apiErrorMessage = runCatching { body.objectAt("error")?.stringAt("message") }.getOrNull()
        val hasAuthTicket = runCatching { body.stringAt("data", "authTicket", "token") != null }.getOrDefault(false)
        val hasRedirect = runCatching { body.objectAt("data")?.boolAt("redirect") == true }.getOrDefault(false)
        DiagnosticLogger.logInfo(
            "LibreLinkUpClient",
            "step=$step httpCode=${response.code()} responseDecoded=true responseBodyLooksLikeJson=true apiStatus=${logicalStatus ?: "n/a"} apiErrorMessage=${apiErrorMessage ?: "n/a"} hasAuthTicket=$hasAuthTicket hasRedirect=$hasRedirect"
        )
        return body
    }

    private fun parseRetryAfterHeader(raw: String?): Int? {
        if (raw.isNullOrBlank()) return null
        return raw.trim().toIntOrNull()
    }

    private fun extractPatientIds(body: JsonObject): List<String> {
        return body.arrayAt("data")
            ?.mapNotNull {
                it.takeIf(JsonElement::isJsonObject)
                    ?.asJsonObject
                    ?.stringAt("patientId")
                    ?.takeIf(String::isNotBlank)
            }
            .orEmpty()
            .distinct()
    }

    private fun baseUrlForRegion(region: String): String {
        val normalized = region.trim()
        if (normalized.startsWith("http://", ignoreCase = true) || normalized.startsWith("https://", ignoreCase = true)) {
            return LibreLinkUpConfig.normalizeBaseUrl(normalized)
        }

        apiBaseUrlOverride?.let { return LibreLinkUpConfig.normalizeBaseUrl(it) }

        return when (normalized.uppercase()) {
            "AUTO", "EU" -> "https://api-eu.libreview.io/"
            "GLOBAL" -> "https://api.libreview.io/"
            "EU2" -> "https://api-eu2.libreview.io/"
            "US" -> "https://api-us.libreview.io/"
            "DE" -> "https://api-de.libreview.io/"
            "FR" -> "https://api-fr.libreview.io/"
            "JP" -> "https://api-jp.libreview.io/"
            "AP" -> "https://api-ap.libreview.io/"
            else -> LibreLinkUpConfig.defaultBaseUrl()
        }
    }

    private fun normalizePayload(
        measurement: JsonObject,
        graphBody: JsonObject,
        currentValue: Int,
        currentTimestamp: Instant,
        trendCode: Int,
        history: List<GlucoseHistoryPoint>
    ): NormalizedGlucosePayload {
        val unit = measurement.stringAt("MeasurementUnit")
            ?: graphBody.objectAt("data")?.stringAt("uom")
            ?: "mg/dL"
        val points = history.map { point ->
            NormalizedGlucosePoint(
                value = point.value,
                timestamp = point.timestamp,
                trendArrow = apiCodeFromTrend(point.trend)
            )
        }
        return NormalizedGlucosePayload(
            value = currentValue,
            unit = unit,
            timestamp = currentTimestamp,
            trendArrow = trendCode,
            isHigh = currentValue > GlucoseReading.HIGH_THRESHOLD,
            isLow = currentValue < GlucoseReading.LOW_THRESHOLD,
            raw = graphBody,
            history = points
        )
    }

    private fun apiCodeFromTrend(trend: GlucoseTrend): Int = when (trend) {
        GlucoseTrend.FALLING_FAST -> 1
        GlucoseTrend.FALLING -> 2
        GlucoseTrend.FLAT -> 3
        GlucoseTrend.RISING -> 4
        GlucoseTrend.RISING_FAST -> 5
        GlucoseTrend.UNKNOWN -> 0
    }

    private fun findCurrentMeasurement(body: JsonObject): JsonObject? {
        return body.objectAt("data", "connection", "glucoseMeasurement")
            ?: body.objectAt("data", "connection", "glucoseItem")
            ?: body.objectAt("data", "glucoseMeasurement")
            ?: body.objectAt("data", "glucoseItem")
            ?: body.objectAt("connection", "glucoseMeasurement")
            ?: body.objectAt("connection", "glucoseItem")
    }

    private fun findGraphData(body: JsonObject): List<JsonElement> {
        return body.arrayAt("data", "graphData")
            ?.toList()
            ?: body.arrayAt("graphData")?.toList()
            ?: body.arrayAt("data", "connection", "graphData")?.toList()
            ?: emptyList()
    }

    private fun sha256(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun parseTimestamp(raw: String?): Instant? {
        return LibreTimestampParser.parse(raw)
    }

    private fun historyHoursAvailable(points: List<GlucoseHistoryPoint>): Double {
        if (points.size < 2) return 0.0
        val seconds = points.last().timestamp.epochSecond - points.first().timestamp.epochSecond
        return (seconds.coerceAtLeast(0) / 3600.0)
    }

    private fun detectTrendMismatch(
        apiTrend: GlucoseTrend,
        history: List<GlucoseHistoryPoint>
    ): String? {
        if (history.size < 3) return null
        val recent = history.takeLast(3)
        val first = recent.first()
        val last = recent.last()
        val minutes = ((last.timestamp.epochSecond - first.timestamp.epochSecond).toDouble() / 60.0)
            .takeIf { it > 0.0 } ?: return null
        val slope = (last.value - first.value).toDouble() / minutes
        val historyTrend = GlucoseTrend.fromSlope(slope)
        return if (historyTrend != GlucoseTrend.UNKNOWN && historyTrend != apiTrend) {
            "Trend mismatch detected"
        } else {
            null
        }
    }

    private fun containsMalformedJwt(rawBody: String): Boolean {
        return rawBody.contains("missing or malformed jwt", ignoreCase = true)
    }

    private fun parseLockoutInfo(rawBody: String, fallbackStatus: Int?, retryAfterSeconds: Int?): LibreLockoutInfo? {
        if (rawBody.isBlank()) return null
        return runCatching {
            val root = JsonParser.parseString(rawBody).asJsonObject
            val status = root.intAt("status") ?: fallbackStatus
            val data = root.objectAt("data")
            val nestedData = data?.objectAt("data")
            val apiCode = data?.intAt("code")
            val message = data?.stringAt("message") ?: root.objectAt("error")?.stringAt("message")
            val failures = nestedData?.intAt("failures")
            val interval = nestedData?.intAt("interval")
            val lockout = nestedData?.intAt("lockout") ?: data?.intAt("lockout")
            LibreLockoutInfo(
                apiStatus = status,
                apiCode = apiCode,
                message = message,
                failures = failures,
                intervalSeconds = interval,
                lockoutSeconds = lockout,
                retryAfterSeconds = retryAfterSeconds
            )
        }.getOrNull()
    }

    private fun extractJwtExpiryEpochSeconds(token: String): Long? {
        return runCatching {
            val parts = token.split('.')
            if (parts.size < 2) return null
            val payload = String(Base64.getUrlDecoder().decode(parts[1]))
            JsonParser.parseString(payload).asJsonObject.get("exp")?.takeIf { !it.isJsonNull }?.asLong
        }.getOrNull()
    }
}

private data class HistoryMappingResult(
    val rawCount: Int,
    val mappedCount: Int,
    val validValueCount: Int,
    val invalidTimestampCount: Int,
    val missingValueCount: Int,
    val invalidValueCount: Int,
    val duplicateCount: Int,
    val mapped: List<GlucoseHistoryPoint>
)

private fun mapHistoryWithDiagnostics(elements: List<JsonElement>): HistoryMappingResult {
    var invalidTimestampCount = 0
    var missingValueCount = 0
    var invalidValueCount = 0
    val parsed = mutableListOf<GlucoseHistoryPoint>()

    elements.forEach { element ->
        val point = element.asHistoryPointOrNull(
            onMissingValue = { missingValueCount += 1 },
            onInvalidValue = { invalidValueCount += 1 },
            onInvalidTimestamp = { invalidTimestampCount += 1 }
        )
        if (point != null) parsed += point
    }

    val deduplicated = parsed.distinctBy { it.timestamp to it.value }
    return HistoryMappingResult(
        rawCount = elements.size,
        mappedCount = deduplicated.size,
        validValueCount = parsed.count { it.value > 0 },
        invalidTimestampCount = invalidTimestampCount,
        missingValueCount = missingValueCount,
        invalidValueCount = invalidValueCount,
        duplicateCount = (parsed.size - deduplicated.size).coerceAtLeast(0),
        mapped = deduplicated
    )
}

private fun JsonObject.stringAt(vararg path: String): String? {
    var current: JsonElement = this
    for (segment in path) {
        current = current.asJsonObject.get(segment) ?: return null
    }
    return current.takeIf { !it.isJsonNull }?.asString
}

private fun JsonObject.intAt(vararg path: String): Int? {
    var current: JsonElement = this
    for (segment in path) {
        current = current.asJsonObject.get(segment) ?: return null
    }
    return current.takeIf { !it.isJsonNull }?.asInt
}

private fun JsonObject.boolAt(vararg path: String): Boolean? {
    var current: JsonElement = this
    for (segment in path) {
        current = current.asJsonObject.get(segment) ?: return null
    }
    return current.takeIf { !it.isJsonNull }?.asBoolean
}

private fun JsonObject.objectAt(vararg path: String): JsonObject? {
    var current: JsonElement = this
    for (segment in path) {
        current = current.asJsonObject.get(segment) ?: return null
    }
    return current.takeIf { it.isJsonObject }?.asJsonObject
}

private fun JsonObject.arrayAt(vararg path: String): JsonArray? {
    var current: JsonElement = this
    for (segment in path) {
        current = current.asJsonObject.get(segment) ?: return null
    }
    return current.takeIf { it.isJsonArray }?.asJsonArray
}

private fun JsonElement.asHistoryPointOrNull(
    onMissingValue: (() -> Unit)? = null,
    onInvalidValue: (() -> Unit)? = null,
    onInvalidTimestamp: (() -> Unit)? = null
): GlucoseHistoryPoint? {
    if (!isJsonObject) return null
    val obj = asJsonObject
    val value = obj.get("ValueInMgPerDl")?.takeIf { !it.isJsonNull }?.asInt
        ?: obj.get("Value")?.takeIf { !it.isJsonNull }?.asInt
        ?: obj.get("value")?.takeIf { !it.isJsonNull }?.asInt
        ?: run {
            onMissingValue?.invoke()
            return null
        }
    if (value <= 0) {
        onInvalidValue?.invoke()
        return null
    }
    val timestampRaw = obj.get("Timestamp")?.takeIf { !it.isJsonNull }?.asString
        ?: obj.get("FactoryTimestamp")?.takeIf { !it.isJsonNull }?.asString
        ?: obj.get("timestamp")?.takeIf { !it.isJsonNull }?.asString
        ?: run {
            onInvalidTimestamp?.invoke()
            return null
        }
    val timestamp = LibreTimestampParser.parse(timestampRaw) ?: run {
        onInvalidTimestamp?.invoke()
        return null
    }
    val trendCode = obj.get("TrendArrow")?.takeIf { !it.isJsonNull }?.asInt
        ?: obj.get("trendArrow")?.takeIf { !it.isJsonNull }?.asInt
        ?: 0
    return GlucoseHistoryPoint(value = value, timestamp = timestamp, trend = GlucoseTrend.fromApiCode(trendCode))
}
