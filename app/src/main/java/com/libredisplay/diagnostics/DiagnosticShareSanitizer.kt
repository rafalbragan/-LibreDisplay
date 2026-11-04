package com.libredisplay.diagnostics

/**
 * Export only bounded diagnostic metadata. Free text (including bodies, headers, URLs,
 * tags and exception messages) is not a trustworthy source of anonymous information.
 * A denylist of secret names cannot safely cover encoded credentials or unknown fields.
 * The local log is unchanged; this policy applies at the user-initiated export boundary.
 */
internal object DiagnosticShareSanitizer {
    private const val OMITTED = "[pominięto dane swobodne]"
    private val envelope = Regex("""^(\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2} \[(?:DEBUG|INFO|WARN|ERROR)\]) [^\s]+ (.*)$""")
    private val number = Regex(
        """(?:httpCode|HTTP|apiStatus|Retry-After|retryAfterSeconds|parsedRetryAfterSeconds|responseByteCount|responseContentLength)=\s*(?:\d{1,9}|n/a)""",
        RegexOption.IGNORE_CASE
    )
    private val operation = Regex("""STEP=(?:LOGIN|CONNECTIONS|GRAPH)""", RegexOption.IGNORE_CASE)
    private val method = Regex("""METHOD=(?:GET|POST|PUT|DELETE|PATCH|HEAD|OPTIONS)""", RegexOption.IGNORE_CASE)
    private val boolean = Regex(
        """(?:responseDecoded|responseWasGzipEncoded|responseBodyLooksLikeGzip|responseBodyLooksLikeJson|REQUEST_EMAIL_IS_NORMALIZED)=(?:true|false)""",
        RegexOption.IGNORE_CASE
    )
    private val exception = Regex(
        """^(?:Caused by: )?((?:java\.io\.IOException|java\.net\.(?:SocketTimeoutException|UnknownHostException|ConnectException)|java\.lang\.(?:IllegalStateException|IllegalArgumentException|SecurityException)))(?::.*)?$"""
    )
    private val events = setOf("FINAL REQUEST", "RESPONSE", "LibreCare started", "=====================================")

    fun sanitize(raw: String): String = raw.lineSequence().joinToString("\n") { line ->
        val match = envelope.matchEntire(line)
        val prefix = match?.groupValues?.get(1)?.plus(" ").orEmpty()
        val payload = (match?.groupValues?.get(2) ?: line).trim()
        val safe = when {
            payload.isEmpty() -> ""
            payload in events -> payload
            number.matches(payload) || operation.matches(payload) || method.matches(payload) || boolean.matches(payload) -> payload
            else -> exception.matchEntire(payload)?.groupValues?.get(1)?.plus(" $OMITTED") ?: OMITTED
        }
        prefix + safe
    }
}

