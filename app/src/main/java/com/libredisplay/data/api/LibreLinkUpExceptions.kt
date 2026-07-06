package com.libredisplay.data.api

open class LibreLinkUpException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

class NonRetryableLibreLinkUpException(
    message: String,
    cause: Throwable? = null
) : LibreLinkUpException(message, cause)

class LibreResponseDecodingException(
    message: String,
    val encoding: String? = null,
    val contentType: String? = null,
    cause: Throwable? = null
) : LibreLinkUpException(message, cause)

open class LibreLinkUpHttpException(
    val statusCode: Int,
    val responseBody: String = "",
    val retryAfterSeconds: Int? = null,
    val lockoutInfo: LibreLockoutInfo? = null,
    message: String = "HTTP $statusCode"
) : LibreLinkUpException(message)

{
    constructor(
        statusCode: Int,
        responseBody: String,
        retryAfterSeconds: Int?,
        message: String
    ) : this(statusCode, responseBody, retryAfterSeconds, null, message)
}

class CriticalLibreLinkUpException(
    statusCode: Int,
    responseBody: String,
    message: String = "HTTP $statusCode"
) : LibreLinkUpHttpException(statusCode, responseBody, null, null, message)

fun Throwable.stopsAutomaticPolling(): Boolean = when (this) {
    is NonRetryableLibreLinkUpException -> true
    is LibreLinkUpHttpException -> statusCode in setOf(400, 401, 403, 429, 430, 500)
    is CriticalLibreLinkUpException -> statusCode in setOf(400, 401, 403, 429, 430, 500)
    else -> false
}

