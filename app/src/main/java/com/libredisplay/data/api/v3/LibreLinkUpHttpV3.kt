package com.libredisplay.data.api.v3

import android.util.Log
import com.google.gson.JsonObject
import com.libredisplay.diagnostics.DiagnosticLogger
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okio.buffer
import okio.sink
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import java.io.ByteArrayOutputStream

private const val TAG = "LibreLinkUpHttpV3"

interface LibreLinkUpTransportV3 {
    suspend fun login(baseUrl: String, request: LoginRequestV3): Response<JsonObject>
    suspend fun getConnections(baseUrl: String, token: String, accountIdHash: String): Response<JsonObject>
    suspend fun getGraph(baseUrl: String, token: String, accountIdHash: String, patientId: String): Response<JsonObject>
}

class LibreLinkUpHttpV3(
    private val productHeader: String = "llu.android",
    private val versionHeader: String = "4.17.0",
    private val userAgent: String = "LibreDisplay/1.0 (Android)"
) : LibreLinkUpTransportV3 {

    private val services = mutableMapOf<String, LibreLinkUpApiServiceV3>()

    override suspend fun login(baseUrl: String, request: LoginRequestV3): Response<JsonObject> {
        return service(baseUrl).login(request)
    }

    override suspend fun getConnections(baseUrl: String, token: String, accountIdHash: String): Response<JsonObject> {
        return service(baseUrl).getConnections("Bearer $token", accountIdHash)
    }

    override suspend fun getGraph(baseUrl: String, token: String, accountIdHash: String, patientId: String): Response<JsonObject> {
        return service(baseUrl).getGraph("Bearer $token", accountIdHash, patientId)
    }

    internal fun buildHeadersForTest(method: String): Map<String, String> {
        val headers = linkedMapOf(
            "Accept" to "application/json",
            "product" to productHeader,
            "version" to versionHeader,
            "cache-control" to "no-cache",
            "connection" to "Keep-Alive",
            "User-Agent" to userAgent
        )
        if (method.uppercase() in setOf("POST", "PUT", "PATCH")) {
            headers["Content-Type"] = "application/json"
        }
        return headers
    }

    private fun service(baseUrl: String): LibreLinkUpApiServiceV3 {
        return services.getOrPut(baseUrl) {
            val headerInterceptor = Interceptor { chain ->
                val original = chain.request()
                val builder = original.newBuilder()
                buildHeadersForTest(original.method).forEach { (name, value) ->
                    builder.header(name, value)
                }
                return@Interceptor chain.proceed(builder.build())
            }

            val requestResponseTraceInterceptor = Interceptor { chain ->
                val request = chain.request()
                val serializedBody = bodyAsUtf8(request)

                DiagnosticLogger.logInfo(TAG, "V3 REQUEST")
                DiagnosticLogger.logInfo(TAG, "URL=${request.url}")
                DiagnosticLogger.logInfo(TAG, "METHOD=${request.method}")
                DiagnosticLogger.logInfo(TAG, "HEADERS=${formatHeadersForLog(request.headers.names().sorted().associateWith { request.header(it).orEmpty() })}")
                DiagnosticLogger.logInfo(TAG, "BODY AFTER SERIALIZATION=${maskBody(serializedBody)}")

                val response = chain.proceed(request)
                val bodyPreview = runCatching { response.peekBody(1024 * 1024).string() }.getOrDefault("<UNAVAILABLE>")

                DiagnosticLogger.logInfo(TAG, "V3 RESPONSE")
                DiagnosticLogger.logInfo(TAG, "HTTP=${response.code}")
                DiagnosticLogger.logInfo(TAG, "HEADERS=${formatHeadersForLog(response.headers.names().sorted().associateWith { response.header(it).orEmpty() })}")
                DiagnosticLogger.logInfo(TAG, "BODY=${maskBody(bodyPreview)}")

                response
            }

            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }

            val client = OkHttpClient.Builder()
                .addInterceptor(headerInterceptor)
                .addInterceptor(requestResponseTraceInterceptor)
                .addInterceptor(logging)
                .build()

            Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(LibreLinkUpApiServiceV3::class.java)
        }
    }

    private fun bodyAsUtf8(request: okhttp3.Request): String {
        val body = request.body ?: return ""
        return runCatching {
            val output = ByteArrayOutputStream()
            val sink = output.sink().buffer()
            body.writeTo(sink)
            sink.flush()
            output.toByteArray().toString(Charsets.UTF_8)
        }.getOrDefault("<UNAVAILABLE>")
    }

    private fun formatHeadersForLog(headers: Map<String, String>): String {
        return headers.entries.joinToString(separator = ", ") { (name, value) ->
            if (name.equals("Authorization", ignoreCase = true)) {
                "$name=${maskAuthorization(value)}"
            } else {
                "$name=$value"
            }
        }
    }

    private fun maskAuthorization(value: String): String {
        if (value.isBlank()) return "<MISSING>"
        if (value.startsWith("Bearer ")) {
            return "Bearer ${value.removePrefix("Bearer ").take(10)}..."
        }
        return "${value.take(10)}..."
    }

    private fun maskBody(raw: String): String {
        if (raw.isBlank()) return raw
        var masked = raw
        masked = masked.replace(
            Regex("\"password\"\\s*:\\s*\"[^\"]*\"", RegexOption.IGNORE_CASE),
            "\"password\":\"***MASKED***\""
        )
        masked = masked.replace(
            Regex("(?i)Bearer\\s+[A-Za-z0-9._-]+"),
            "Bearer ***"
        )
        return masked
    }

    private interface LibreLinkUpApiServiceV3 {
        @POST("/llu/auth/login")
        suspend fun login(@Body request: LoginRequestV3): Response<JsonObject>

        @GET("/llu/connections")
        suspend fun getConnections(
            @Header("Authorization") authorization: String,
            @Header("Account-Id") accountId: String
        ): Response<JsonObject>

        @GET("/llu/connections/{patientId}/graph")
        suspend fun getGraph(
            @Header("Authorization") authorization: String,
            @Header("Account-Id") accountId: String,
            @Path("patientId") patientId: String
        ): Response<JsonObject>
    }
}

