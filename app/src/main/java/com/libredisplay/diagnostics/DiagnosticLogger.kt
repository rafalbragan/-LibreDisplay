package com.libredisplay.diagnostics

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object DiagnosticLogger {
    private const val DEFAULT_TAG = "LibreDisplay"
    private const val LOG_FILE_NAME = "libredisplay.log"
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    @Volatile
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun startNewSession(context: Context) {
        appContext = context.applicationContext
        clear()
        DiagnosticStatus.reset()
        logInfo(DEFAULT_TAG, "=====================================")
        logInfo(DEFAULT_TAG, "LibreDisplay started")
        logInfo(DEFAULT_TAG, "=====================================")
    }

    fun logRequest(
        step: String,
        url: String,
        method: String,
        headers: Map<String, String>,
        bodyAfterSerialization: String,
        bodyContentType: String = ""
    ) {
        val loginMeta = if (step.equals("LOGIN", ignoreCase = true)) loginPayloadMeta(bodyAfterSerialization) else null
        append(
            level = "INFO",
            tag = DEFAULT_TAG,
            message = buildString {
                appendLine("FINAL REQUEST")
                appendLine("STEP=$step")
                appendLine("URL=$url")
                appendLine("METHOD=$method")
                appendLine("HEADERS=${maskHeaders(headers)}")
                appendLine("BODY_CONTENT_TYPE=${bodyContentType.ifBlank { "none" }}")
                if (step.equals("LOGIN", ignoreCase = true)) {
                    appendLine("REQUEST_EMAIL_IS_NORMALIZED=${loginMeta?.emailIsNormalized ?: false}")
                    appendLine("REQUEST_BODY_UTF8_BYTE_COUNT=${loginMeta?.bodyUtf8ByteCount ?: bodyAfterSerialization.toByteArray(Charsets.UTF_8).size}")
                    appendLine("REQUEST_BODY_FIELD_NAMES=${loginMeta?.fieldNames ?: emptyList<String>()}")
                    append("BODY_AFTER_SERIALIZATION=${maskedLoginPayload(bodyAfterSerialization)}")
                } else {
                    append("BODY_AFTER_SERIALIZATION=${maskText(bodyAfterSerialization)}")
                }
            }
        )
    }

    fun logResponse(
        step: String,
        httpCode: Int,
        responseBody: String,
        headers: Map<String, String> = emptyMap(),
        responseContentType: String = "",
        responseByteCount: Int = 0,
        responseContentEncoding: String? = null,
        responseContentLength: Long? = null,
        responseWasGzipEncoded: Boolean? = null,
        responseBodyLooksLikeGzip: Boolean? = null,
        responseBodyLooksLikeJson: Boolean? = null,
        responseDecoded: Boolean? = null
    ) {
        val parsed = parseResponseMeta(responseBody)
        append(
            level = "INFO",
            tag = DEFAULT_TAG,
            message = buildString {
                appendLine("RESPONSE")
                appendLine("step=$step")
                appendLine("httpCode=$httpCode")
                appendLine("apiStatus=${parsed.apiStatus ?: "n/a"}")
                appendLine("apiErrorMessage=${parsed.apiErrorMessage ?: "n/a"}")
                headers["Retry-After"]?.let { appendLine("Retry-After=$it") }
                headers["Date"]?.let { appendLine("Date=$it") }
                headers["Server"]?.let { appendLine("Server=$it") }
                headers["CF-Ray"]?.let { appendLine("CF-Ray=$it") }
                headers["X-Request-Id"]?.let { appendLine("X-Request-Id=$it") }
                responseContentEncoding?.let { appendLine("responseContentEncoding=$it") }
                appendLine("responseContentType=${responseContentType.ifBlank { "n/a" }}")
                responseContentLength?.let { appendLine("responseContentLength=$it") }
                responseWasGzipEncoded?.let { appendLine("responseWasGzipEncoded=$it") }
                responseBodyLooksLikeGzip?.let { appendLine("responseBodyLooksLikeGzip=$it") }
                responseBodyLooksLikeJson?.let { appendLine("responseBodyLooksLikeJson=$it") }
                responseDecoded?.let { appendLine("responseDecoded=$it") }
                appendLine("responseByteCount=$responseByteCount")
                append("responseBody=${maskText(responseBody)}")
            }
        )
    }

    fun logJsonSchema(label: String, jsonElement: JsonElement) {
        append("WARN", DEFAULT_TAG, "$label schema=${maskText(jsonElement.toString())}")
    }

    fun logDebug(tag: String, message: String) = append("DEBUG", tag, maskText(message))
    fun logInfo(tag: String, message: String) = append("INFO", tag, maskText(message))
    fun logWarning(tag: String, message: String) = append("WARN", tag, maskText(message))
    fun logError(tag: String, message: String) = append("ERROR", tag, maskText(message))
    fun logDebug(message: String) = logDebug(DEFAULT_TAG, message)
    fun logInfo(message: String) = logInfo(DEFAULT_TAG, message)
    fun logError(message: String) = logError(DEFAULT_TAG, message)

    fun logException(tag: String, throwable: Throwable, message: String = "") {
        val stack = StringWriter().also { writer ->
            PrintWriter(writer).use { throwable.printStackTrace(it) }
        }.toString()
        val prefix = if (message.isBlank()) "" else "$message\n"
        append("ERROR", tag, maskText(prefix + stack))
    }

    fun readAll(): String {
        val file = getOrCreateLogFile() ?: return ""
        return runCatching { file.readText(Charsets.UTF_8) }.getOrDefault("")
    }

    fun clear() {
        val file = getOrCreateLogFile() ?: return
        runCatching { file.writeText("", Charsets.UTF_8) }
    }

    fun logFilePath(): String = getOrCreateLogFile()?.absolutePath.orEmpty()

    fun createShareIntent(context: Context): Intent? {
        val file = getOrCreateLogFile() ?: return null
        if (!file.exists()) return null
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        return Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "LibreDisplay - log diagnostyczny")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun append(level: String, tag: String, message: String) {
        val file = getOrCreateLogFile() ?: return
        val line = "${LocalDateTime.now().format(formatter)} [$level] $tag $message\n"
        synchronized(this) {
            runCatching { file.appendText(line, Charsets.UTF_8) }
        }
    }

    private fun getOrCreateLogFile(): File? {
        val context = appContext ?: return null
        val logsDir = context.getExternalFilesDir(null)?.resolve("logs") ?: return null
        return logsDir.resolve(LOG_FILE_NAME).also {
            runCatching {
                it.parentFile?.mkdirs()
                if (!it.exists()) it.createNewFile()
            }
        }
    }

    private fun maskHeaders(headers: Map<String, String>): Map<String, String> {
        return headers.mapValues { (key, value) ->
            when {
                key.equals("Authorization", ignoreCase = true) -> "Bearer ***"
                key.equals("Account-Id", ignoreCase = true) -> value.take(8) + "…"
                else -> maskText(value)
            }
        }
    }

    private fun maskText(input: String): String {
        return input
            .replace(Regex("(?i)(\"password\"\\s*:\\s*\")[^\"]*(\")"), "$1***$2")
            .replace(Regex("(?i)(\"token\"\\s*:\\s*\")[^\"]*(\")"), "$1***$2")
            .replace(Regex("(?i)Bearer\\s+[A-Za-z0-9._-]+"), "Bearer ***")
            .replace(Regex("(?i)(\"email\"\\s*:\\s*\")([^\"]*)(\")")) { match ->
                val original = match.groupValues[2]
                "${match.groupValues[1]}${maskEmail(original)}${match.groupValues[3]}"
            }
    }

    private fun loginPayloadMeta(rawBody: String): LoginPayloadMeta {
        if (rawBody.isBlank()) return LoginPayloadMeta()
        return runCatching {
            val obj = JsonParser.parseString(rawBody).asJsonObject
            val email = obj.get("email")?.takeIf { !it.isJsonNull }?.asString.orEmpty()
            val password = obj.get("password")?.takeIf { !it.isJsonNull }?.asString.orEmpty()
            val normalizedEmail = email.trim().lowercase()
            LoginPayloadMeta(
                emailMasked = maskEmail(email),
                emailLength = email.length,
                passwordLength = password.length,
                passwordUtf8Bytes = password.toByteArray(Charsets.UTF_8).size,
                hasLeadingWhitespace = password.isNotEmpty() && password.first().isWhitespace(),
                hasTrailingWhitespace = password.isNotEmpty() && password.last().isWhitespace(),
                emailIsNormalized = email == normalizedEmail,
                bodyUtf8ByteCount = rawBody.toByteArray(Charsets.UTF_8).size,
                fieldNames = obj.keySet().toList().sorted()
            )
        }.getOrDefault(LoginPayloadMeta())
    }

    private fun parseResponseMeta(rawBody: String): ResponseMeta {
        if (rawBody.isBlank()) return ResponseMeta()
        return runCatching {
            val root = JsonParser.parseString(rawBody).asJsonObject
            ResponseMeta(
                apiStatus = root.get("status")?.takeIf { !it.isJsonNull }?.asInt,
                apiErrorMessage = root.getAsJsonObject("error")?.get("message")?.takeIf { !it.isJsonNull }?.asString
            )
        }.getOrDefault(ResponseMeta())
    }

    private fun maskedLoginPayload(rawBody: String): String {
        if (rawBody.isBlank()) return "{}"
        return runCatching {
            val obj = JsonParser.parseString(rawBody).asJsonObject
            val email = obj.get("email")?.takeIf { !it.isJsonNull }?.asString.orEmpty()
            "{\"email\":\"${maskEmail(email)}\",\"password\":\"***\"}"
        }.getOrDefault("{\"email\":\"***\",\"password\":\"***\"}")
    }

    private fun maskEmail(email: String): String {
        if (email.isBlank() || !email.contains("@")) return "***"
        val parts = email.split("@", limit = 2)
        val local = parts[0]
        val domain = parts[1]
        val localMasked = if (local.length <= 1) "*" else "${local.first()}***"
        return "$localMasked@$domain"
    }

    private data class LoginPayloadMeta(
        val emailMasked: String = "***",
        val emailLength: Int = 0,
        val passwordLength: Int = 0,
        val passwordUtf8Bytes: Int = 0,
        val hasLeadingWhitespace: Boolean = false,
        val hasTrailingWhitespace: Boolean = false,
        val emailIsNormalized: Boolean = false,
        val bodyUtf8ByteCount: Int = 0,
        val fieldNames: List<String> = emptyList()
    )

    private data class ResponseMeta(
        val apiStatus: Int? = null,
        val apiErrorMessage: String? = null
    )
}
