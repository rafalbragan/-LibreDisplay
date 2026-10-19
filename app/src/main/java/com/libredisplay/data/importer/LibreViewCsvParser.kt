package com.libredisplay.data.importer

import com.univocity.parsers.csv.CsvParser
import com.univocity.parsers.csv.CsvParserSettings
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.io.InputStream
import java.io.InputStreamReader
import java.io.PushbackReader
import java.nio.charset.CodingErrorAction
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle
import java.util.Locale

enum class AmbiguousTimePolicy { SKIP, EARLIER_OFFSET, LATER_OFFSET }
data class CsvTimeOptions(val zone: ZoneId, val ambiguous: AmbiguousTimePolicy = AmbiguousTimePolicy.SKIP)
data class CsvMeasurement(val record: Long, val timestamp: Instant, val value: Int, val type: Int,
    val localTimestamp: String, val offset: String)
data class CsvSummary(val supported: Int, val skipped: Int, val invalid: Int, val ambiguous: Int,
    val nonexistent: Int, val earliest: Instant?, val latest: Instant?, val metadata: String)
class CsvFormatException(message: String) : IllegalArgumentException(message)

/** Parses only the explicitly supported LibreView schema; never uses a patient name as an ID. */
class LibreViewCsvParser {
    suspend fun parse(input: InputStream, options: CsvTimeOptions, onMeasurement: suspend (CsvMeasurement) -> Unit): CsvSummary {
        val settings = CsvParserSettings().apply {
            isLineSeparatorDetectionEnabled = true
            setReadInputOnSeparateThread(false)
            maxCharsPerColumn = 8192
            maxColumns = 128
            setSkipEmptyLines(true)
        }
        val parser = CsvParser(settings)
        val decoder = Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
        val reader = PushbackReader(InputStreamReader(input, decoder), 1)
        val first = reader.read()
        if (first != -1 && first != 0xFEFF) reader.unread(first)
        var supported = 0; var skipped = 0; var invalid = 0; var ambiguous = 0; var nonexistent = 0
        var earliest: Instant? = null; var latest: Instant? = null
        try {
            parser.beginParsing(reader)
            val metadata = parser.parseNext() ?: throw CsvFormatException("Pusty plik CSV.")
            val header = parser.parseNext() ?: throw CsvFormatException("Brak nagłówka LibreView.")
            val columns = header.map { normalize(it.orEmpty()) }
            fun column(vararg names: String): Int {
                val matches = columns.indices.filter { columns[it] in names.map(::normalize) }
                if (matches.size != 1) throw CsvFormatException("Nieobsługiwany nagłówek LibreView: ${names.first()}.")
                return matches.single()
            }
            val time = column("Znacznik czasu urządzenia", "Device Timestamp")
            val type = column("Typ zapisu", "Typ rekordu", "Typ rejestru", "Record Type")
            val historical = column("Historyczne stężenie glukozy mg/dL", "Historia glukozy mg/dL", "Historic Glucose mg/dL")
            val scan = column("Zeskanowane stężenie glukozy mg/dL", "Skanowanie glukozy mg/dL", "Scan Glucose mg/dL")
            column("Urządzenie", "Device")
            column("Numer seryjny", "Serial Number")
            if (columns.any { "mmol" in it }) throw CsvFormatException("Obsługiwane są wyłącznie jednostki mg/dL.")
            var record = 2L
            while (true) {
                currentCoroutineContext().ensureActive()
                val row = parser.parseNext() ?: break
                record++
                val rowType = row.getOrNull(type)?.trim()?.toIntOrNull()
                if (rowType != 0 && rowType != 1) { skipped++; continue }
                val rawTime = row.getOrNull(time)?.trim().orEmpty()
                val value = row.getOrNull(if (rowType == 0) historical else scan)?.trim()?.toIntOrNull()
                val local = runCatching { LocalDateTime.parse(rawTime, TIME_FORMAT) }.getOrNull()
                if (row.size != header.size || value == null || value !in 20..600 || local == null) {
                    skipped++; invalid++; continue
                }
                val offsets = options.zone.rules.getValidOffsets(local)
                if (offsets.isEmpty()) { skipped++; nonexistent++; continue }
                if (offsets.size > 1) {
                    ambiguous++
                    if (options.ambiguous == AmbiguousTimePolicy.SKIP) { skipped++; continue }
                }
                val offset = if (options.ambiguous == AmbiguousTimePolicy.LATER_OFFSET) offsets.last() else offsets.first()
                val timestamp = local.toInstant(offset)
                onMeasurement(CsvMeasurement(record, timestamp, value, rowType, rawTime, offset.id))
                supported++
                if (earliest == null || timestamp < earliest) earliest = timestamp
                if (latest == null || timestamp > latest) latest = timestamp
            }
            return CsvSummary(supported, skipped, invalid, ambiguous, nonexistent, earliest, latest,
                metadata.filterNotNull().joinToString(" · ").take(512))
        } catch (error: com.univocity.parsers.common.TextParsingException) {
            // Parser exception text can contain private row contents: never surface or log it.
            throw CsvFormatException("Nieprawidłowa struktura CSV lub przekroczony limit długości pola.")
        } finally {
            parser.stopParsing()
            reader.close()
        }
    }

    private fun normalize(text: String) = text.trim().replace('\u00a0', ' ').lowercase(Locale.ROOT)
    companion object {
        private val TIME_FORMAT = DateTimeFormatter.ofPattern("dd-MM-uuuu HH:mm", Locale.ROOT)
            .withResolverStyle(ResolverStyle.STRICT)
    }
}


