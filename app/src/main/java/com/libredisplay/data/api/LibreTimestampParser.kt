package com.libredisplay.data.api

import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoField
import java.util.Locale

internal object LibreTimestampParser {

    private val localFormatters: List<DateTimeFormatter> = listOf(
        DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendPattern("M/d/yyyy h:mm:ss a")
            .toFormatter(Locale.US),
        DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendPattern("M/d/yyyy h:mm a")
            .toFormatter(Locale.US),
        DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendPattern("M/d/yyyy H:mm:ss")
            .toFormatter(Locale.US),
        DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendPattern("M/d/yyyy H:mm")
            .toFormatter(Locale.US),
        DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendPattern("yyyy-MM-dd HH:mm:ss")
            .toFormatter(Locale.US),
        DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendPattern("yyyy-MM-dd HH:mm")
            .toFormatter(Locale.US),
        DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendPattern("yyyy-MM-dd'T'HH:mm:ss")
            .optionalStart()
            .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true)
            .optionalEnd()
            .toFormatter(Locale.US)
    )

    fun parse(raw: String?): Instant? {
        if (raw.isNullOrBlank()) return null
        val normalized = raw.trim()

        normalized.toLongOrNull()?.let { epoch ->
            return if (normalized.length <= 10) Instant.ofEpochSecond(epoch) else Instant.ofEpochMilli(epoch)
        }

        runCatching { Instant.parse(normalized) }.getOrNull()?.let { return it }
        runCatching { OffsetDateTime.parse(normalized).toInstant() }.getOrNull()?.let { return it }

        for (formatter in localFormatters) {
            try {
                val local = LocalDateTime.parse(normalized, formatter)
                return local.toInstant(ZoneOffset.UTC)
            } catch (_: DateTimeParseException) {
                // Try next formatter.
            }
        }

        return null
    }
}

