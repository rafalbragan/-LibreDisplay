package com.libredisplay.data.api

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.libredisplay.diagnostics.DiagnosticLogger
import com.libredisplay.diagnostics.DiagnosticStatus
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import okio.GzipSource
import okio.buffer
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.ConcurrentHashMap

interface LibreLinkUpHttp {
    suspend fun login(baseUrl: String, request: LoginRequest): Response<JsonObject>
    suspend fun getConnections(baseUrl: String, token: String, accountIdHash: String): Response<JsonObject>
    suspend fun getGraph(baseUrl: String, token: String, accountIdHash: String, patientId: String): Response<JsonObject>
    fun buildHeadersForTest(method: String, token: String? = null, accountIdHash: String? = null): Map<String, String>
}

class OkHttpLibreLinkUpHttp(
    private val gson: Gson = GsonBuilder().disableHtmlEscaping().create()
) : LibreLinkUpHttp {

    private val linkUpVersion: String = LibreLinkUpConfig.linkUpVersion()

    private val client: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor())
        .build()

    private val services = ConcurrentHashMap<String, LibreLinkUpApiService>()

    override suspend fun login(baseUrl: String, request: LoginRequest): Response<JsonObject> {
        return service(baseUrl).login(
            headers = buildHeaders(step = "LOGIN", method = "POST"),
            request = request
        )
    }

    override suspend fun getConnections(baseUrl: String, token: String, accountIdHash: String): Response<JsonObject> {
        return service(baseUrl).getConnections(
            headers = buildHeaders(
                step = "CONNECTIONS",
                method = "GET",
                token = token,
                accountIdHash = accountIdHash
            )
        )
    }

    override suspend fun getGraph(
        baseUrl: String,
        token: String,
        accountIdHash: String,
        patientId: String
    ): Response<JsonObject> {
        return service(baseUrl).getGraph(
            headers = buildHeaders(
                step = "GRAPH",
                method = "GET",
                token = token,
                accountIdHash = accountIdHash
            ),
            patientId = patientId
        )
    }

    override fun buildHeadersForTest(
        method: String,
        token: String?,
        accountIdHash: String?
    ): Map<String, String> = buildHeaders(
        step = "TEST",
        method = method,
        token = token,
        accountIdHash = accountIdHash
    ).filterKeys { it != INTERNAL_STEP_HEADER }

    private fun buildHeaders(
        step: String,
        method: String,
        token: String? = null,
        accountIdHash: String? = null
    ): Map<String, String> {
        val headers = linkedMapOf(
            INTERNAL_STEP_HEADER to step,
            "Accept" to "application/json",
            "Cache-Control" to "no-cache",
            "Pragma" to "no-cache",
            "Expires" to "0",
            "Connection" to "Keep-Alive",
            "Product" to "llu.android",
            "Version" to linkUpVersion,
            "User-Agent" to "LibreLinkUp/$linkUpVersion"
        )

        if (method.uppercase() in setOf("POST", "PUT", "PATCH")) {
            // Keep explicit content type for final request visibility and API compatibility.
            headers["Content-Type"] = "application/json"
        }
        if (!token.isNullOrBlank()) {
            headers["Authorization"] = "Bearer $token"
        }
        if (!accountIdHash.isNullOrBlank()) {
            headers["Account-Id"] = accountIdHash
        }
        return headers
    }

    private fun service(baseUrl: String): LibreLinkUpApiService {
        return services.getOrPut(baseUrl) {
            Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
                .create(LibreLinkUpApiService::class.java)
        }
    }

    private fun loggingInterceptor(): Interceptor = Interceptor { chain ->
        val original = chain.request()
        val step = original.header(INTERNAL_STEP_HEADER) ?: "UNKNOWN"

        val requestBuilder = original.newBuilder().removeHeader(INTERNAL_STEP_HEADER)
        if (original.method.equals("POST", ignoreCase = true)
            || original.method.equals("PUT", ignoreCase = true)
            || original.method.equals("PATCH", ignoreCase = true)
        ) {
            // Force final HTTP request header for JSON body writes.
            requestBuilder.header("Content-Type", "application/json")
        }
        val request = requestBuilder.build()

        val requestBody = bodyToString(request.body)
        val bodyContentType = request.body?.contentType()?.toString() ?: ""
        DiagnosticLogger.logRequest(
            step = step,
            url = request.url.toString(),
            method = request.method,
            headers = request.headers.toMultimap().mapValues { (_, values) -> values.joinToString(",") },
            bodyAfterSerialization = requestBody,
            bodyContentType = bodyContentType
        )
        DiagnosticStatus.setRequest(step = step, url = request.url.toString(), method = request.method)

        val response = chain.proceed(request)
        val initialPreviewBytes = runCatching { response.peekBody(MAX_LOG_BYTES.toLong()).bytes() }.getOrDefault(ByteArray(0))
        val initialBodyLooksLikeGzip = initialPreviewBytes.hasGzipMagic()
        val responseContentEncoding = response.header("Content-Encoding")
            ?: response.networkResponse?.header("Content-Encoding")
        val responseWasGzipEncoded = responseContentEncoding?.contains("gzip", ignoreCase = true) == true

        val safeResponse = if (initialBodyLooksLikeGzip && response.body != null) {
            val originalBody = response.body ?: throw LibreResponseDecodingException(
                message = "Odpowiedz LibreLinkUp jest skompresowana GZIP, ale nie zostala rozpakowana przez klienta HTTP.",
                encoding = responseContentEncoding,
                contentType = null
            )
            val decompressedBytes = runCatching {
                val compressedBytes = originalBody.bytes()
                val source = GzipSource(Buffer().write(compressedBytes))
                source.buffer().use { it.readByteArray() }
            }.getOrElse { cause ->
                throw LibreResponseDecodingException(
                    message = "Odpowiedz LibreLinkUp jest skompresowana GZIP, ale nie zostala rozpakowana przez klienta HTTP.",
                    encoding = responseContentEncoding,
                    contentType = originalBody.contentType()?.toString(),
                    cause = cause
                )
            }
            response.newBuilder()
                .removeHeader("Content-Encoding")
                .removeHeader("Content-Length")
                .body(decompressedBytes.toResponseBody(originalBody.contentType()))
                .build()
        } else {
            response
        }

        val previewBytes = runCatching { safeResponse.peekBody(MAX_LOG_BYTES.toLong()).bytes() }.getOrDefault(ByteArray(0))
        val responseBodyLooksLikeGzip = previewBytes.hasGzipMagic()
        val responseBodyLooksLikeJson = previewBytes.looksLikeJson()
        val responseContentType = safeResponse.body?.contentType()?.toString().orEmpty()
        val responseContentLength = safeResponse.body?.contentLength()?.takeIf { it >= 0L }
            ?: safeResponse.header("Content-Length")?.toLongOrNull()
        val responseBody = if (responseBodyLooksLikeGzip) {
            "<binary:gzip>"
        } else {
            runCatching { String(previewBytes, Charsets.UTF_8) }.getOrDefault("")
        }
        val sanitizedBody = sanitizeResponseBodyForLogs(step = step, body = responseBody)
        val responseHeaders = mapOf(
            "Retry-After" to (safeResponse.header("Retry-After") ?: ""),
            "Date" to (safeResponse.header("Date") ?: ""),
            "Server" to (safeResponse.header("Server") ?: ""),
            "CF-Ray" to (safeResponse.header("CF-Ray") ?: ""),
            "X-Request-Id" to (safeResponse.header("X-Request-Id") ?: "")
        ).filterValues { it.isNotBlank() }
        DiagnosticLogger.logResponse(
            step = step,
            httpCode = safeResponse.code,
            responseBody = sanitizedBody,
            headers = responseHeaders,
            responseContentType = responseContentType,
            responseByteCount = previewBytes.size,
            responseContentEncoding = responseContentEncoding,
            responseContentLength = responseContentLength,
            responseWasGzipEncoded = responseWasGzipEncoded,
            responseBodyLooksLikeGzip = responseBodyLooksLikeGzip,
            responseBodyLooksLikeJson = responseBodyLooksLikeJson,
            responseDecoded = !responseBodyLooksLikeGzip && (responseWasGzipEncoded || initialBodyLooksLikeGzip)
        )
        DiagnosticStatus.setResponse(step = step, httpCode = safeResponse.code, responseBody = sanitizedBody)

        safeResponse
    }

    private fun bodyToString(body: RequestBody?): String {
        if (body == null) return ""
        return runCatching {
            val buffer = Buffer()
            body.writeTo(buffer)
            buffer.readUtf8()
        }.getOrDefault("")
    }

    companion object {
        private const val INTERNAL_STEP_HEADER = "X-LibreDisplay-Step"
        private const val MAX_LOG_BYTES = 64 * 1024
    }
}

private fun sanitizeResponseBodyForLogs(step: String, body: String): String {
    return when (step.uppercase()) {
        "LOGIN", "CONNECTIONS", "GRAPH" -> "<omitted:sensitive ${step.uppercase()} body>"
        else -> body
    }
}

private fun ByteArray.hasGzipMagic(): Boolean =
    size >= 2 &&
        (this[0].toInt() and 0xff) == 0x1f &&
        (this[1].toInt() and 0xff) == 0x8b

private fun ByteArray.looksLikeJson(): Boolean {
    val first = firstNonWhitespaceByte() ?: return false
    return first == '{'.code.toByte() || first == '['.code.toByte()
}

private fun ByteArray.firstNonWhitespaceByte(): Byte? {
    for (byte in this) {
        val c = byte.toInt().toChar()
        if (!c.isWhitespace()) return byte
    }
    return null
}

internal fun extractRetryAfterSeconds(rawBody: String): Int? {
    if (rawBody.isBlank()) return null
    return runCatching {
        val root = JsonParser.parseString(rawBody).asJsonObject
        root.get("retry_after")?.takeIf { !it.isJsonNull }?.asInt
            ?: root.getAsJsonObject("error")?.get("retry_after")?.takeIf { !it.isJsonNull }?.asInt
            ?: root.getAsJsonObject("data")?.get("retry_after")?.takeIf { !it.isJsonNull }?.asInt
            ?: root.getAsJsonObject("data")?.getAsJsonObject("data")?.get("lockout")?.takeIf { !it.isJsonNull }?.asInt
    }.getOrNull()
}
