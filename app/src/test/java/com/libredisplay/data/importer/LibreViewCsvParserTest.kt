package com.libredisplay.data.importer

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class LibreViewCsvParserTest {
    private val warsaw = CsvTimeOptions(ZoneId.of("Europe/Warsaw"))
    private val header = "Urządzenie,Numer seryjny,Znacznik czasu urządzenia,Typ zapisu,Historyczne stężenie glukozy mg/dL,Zeskanowane stężenie glukozy mg/dL,Notatki"
    private val abbottHeader = "Urządzenie,Numer seryjny,Znacznik czasu w urządzeniu,Rodzaj zapisu,Historyczne stężenia glukozy mg/dL,Stężenie glukozy na podstawie skanowania mg/dL,Ciała ketonowe mmol/L,Notatki"
    private fun abbottCsv(rows: String) = "Dane glukozy,Profil syntetyczny DEMO\r\n$abbottHeader\r\n$rows"
    private fun csv(rows: String, metadata: String = "Dane glukozy,Profil demonstracyjny") = "$metadata\r\n$header\r\n$rows"
    private suspend fun parse(text: String, options: CsvTimeOptions = warsaw): Pair<CsvSummary, List<CsvMeasurement>> {
        val values = mutableListOf<CsvMeasurement>()
        val summary = LibreViewCsvParser().parse(text.byteInputStream(), options) { values += it }
        return summary to values
    }
    @Test fun historicalScanAndEventAreDistinct() = runTest {
        val (summary, rows) = parse(csv("Libre,synthetic,01-05-2020 10:00,0,110,,\r\nLibre,synthetic,01-05-2020 10:01,1,,120,\r\nLibre,synthetic,01-05-2020 10:02,6,999,999,event"))
        assertEquals(2, summary.supported); assertEquals(1, summary.skipped)
        assertEquals(listOf(0, 1), rows.map { it.type })
        assertEquals(Instant.parse("2020-05-01T08:00:00Z"), rows.first().timestamp)
    }
    @Test fun bomCrLfQuotedCommaEscapedQuotesAndMultiline() = runTest {
        val (summary, rows) = parse("\uFEFF" + csv("Libre,synthetic,01-05-2020 10:00,0,110,,\"Notatka, z \"\"cytatem\"\"\r\ni nową linią\""))
        assertEquals(1, summary.supported); assertEquals(3L, rows.single().record)
    }
    @Test fun ambiguousTimesAreSkippedByDefault() = runTest {
        val (summary, rows) = parse(csv("Libre,synthetic,25-10-2020 02:30,0,110,,"))
        assertTrue(rows.isEmpty()); assertEquals(1, summary.ambiguous); assertEquals(1, summary.skipped)
    }
    @Test fun explicitDstOffsetsDifferByOneHour() = runTest {
        val text = csv("Libre,synthetic,25-10-2020 02:30,0,110,,")
        val earlier = parse(text, warsaw.copy(ambiguous = AmbiguousTimePolicy.EARLIER_OFFSET)).second.single()
        val later = parse(text, warsaw.copy(ambiguous = AmbiguousTimePolicy.LATER_OFFSET)).second.single()
        assertEquals(3600L, later.timestamp.epochSecond - earlier.timestamp.epochSecond)
        assertEquals("+02:00", earlier.offset); assertEquals("+01:00", later.offset)
    }
    @Test fun nonexistentDstTimeNeverShiftsSilently() = runTest {
        val (summary, rows) = parse(csv("Libre,synthetic,29-03-2020 02:30,0,110,,"))
        assertTrue(rows.isEmpty()); assertEquals(1, summary.nonexistent)
    }
    @Test fun invalidDatesValuesAndTruncatedRowsAreSkipped() = runTest {
        val (summary, _) = parse(csv("Libre,synthetic,31-02-2020 10:00,0,110,,\nLibre,synthetic,01-05-2020 10:00,0,NaN,,\nLibre,synthetic,01-05-2020 10:00,0,110"))
        assertEquals(3, summary.invalid); assertEquals(0, summary.supported)
    }
    @Test fun wrongUnitsAreRejected() = runTest {
        try { parse(csv("").replace("mg/dL", "mmol/L")); fail() } catch (_: CsvFormatException) { }
    }
    @Test fun unrelatedCsvAndDuplicateRequiredHeadersAreRejected() = runTest {
        for (text in listOf("first,last\na,b", csv("").replace("Notatki", "Typ zapisu"))) {
            try { parse(text); fail() } catch (_: CsvFormatException) { }
        }
    }
    @Test fun cancellationIsNotConvertedIntoSkippedData() = runTest {
        try {
            LibreViewCsvParser().parse(csv("Libre,synthetic,01-05-2020 10:00,0,110,,").byteInputStream(), warsaw) {
                throw CancellationException("test cancellation")
            }
            fail()
        } catch (_: CancellationException) { }
    }
    @Test fun malformedUtf8FailsInsteadOfReplacingNames() = runTest {
        try { LibreViewCsvParser().parse(byteArrayOf(0xC3.toByte(), 0x28).inputStream(), warsaw) { }; fail() }
        catch (_: java.io.IOException) { }
    }
    @Test fun metadataNeverBecomesPatientIdentity() = runTest {
        val (summary, _) = parse(csv("Libre,synthetic,01-05-2020 10:00,0,110,,", "Dane glukozy,Osoba DEMO"))
        assertTrue(summary.metadata.contains("Osoba DEMO"))
    }
    @Test fun exactAbbottHeadersAllowKetonesButOnlyTypesZeroAndOneAreGlucose() = runTest {
        val (summary, rows) = parse("\uFEFF" + abbottCsv(
            "Libre,synthetic,01-05-2020 10:00,0,110,,0.2,\"DEMO, \"\"cytat\"\"\r\nnotatka\"\r\n" +
                "Libre,synthetic,01-05-2020 10:01,1,,120,0.3,\r\n" +
                "Libre,synthetic,01-05-2020 10:02,6,130,140,0.4,"))
        assertEquals(2, summary.supported); assertEquals(1, summary.skipped)
        assertEquals(listOf(0, 1), rows.map { it.type })
        assertEquals(listOf(110, 120), rows.map { it.value })
        assertEquals(listOf(3L, 4L), rows.map { it.record })
        assertEquals(Instant.parse("2020-05-01T08:00:00Z"), rows.first().timestamp)
    }
    @Test fun legacyHeadersAlsoAllowUnrelatedMmolKetoneColumn() = runTest {
        val (summary, rows) = parse(csv("Libre,synthetic,01-05-2020 10:00,0,110,,0.5")
            .replace("Notatki", "Ciała ketonowe mmol/L"))
        assertEquals(1, summary.supported); assertEquals(110, rows.single().value)
    }
    @Test fun eachGlucoseColumnRequiresMgDlBeforeAnyMeasurementIsEmitted() = runTest {
        val original = abbottCsv("Libre,synthetic,01-05-2020 10:00,0,110,,0.5,")
        for (column in listOf("Historyczne stężenia glukozy mg/dL", "Stężenie glukozy na podstawie skanowania mg/dL")) {
            for (unit in listOf("mmol/L", "mg/L", "")) {
                var emitted = 0
                try {
                    LibreViewCsvParser().parse(original.replace(column, column.replace("mg/dL", unit)).byteInputStream(), warsaw) { emitted++ }
                    fail("Unsupported glucose units must fail closed")
                } catch (_: CsvFormatException) { }
                assertEquals(0, emitted)
            }
        }
    }
    @Test fun exactHeadersCountInvalidGlucoseWithoutFallingBackToKetonesOrScan() = runTest {
        val rows = listOf("", "LO", "HI", "NaN", "110.5", "19", "601").flatMap { value ->
            listOf("Libre,synthetic,01-05-2020 10:00,0,$value,120,0.5,",
                "Libre,synthetic,01-05-2020 10:01,1,120,$value,0.5,")
        } + listOf("Libre,synthetic,01-05-2020 10:02,0,20,,0.5,", "Libre,synthetic,01-05-2020 10:03,1,,600,0.5,")
        val (summary, accepted) = parse(abbottCsv(rows.joinToString("\r\n")))
        assertEquals(14, summary.invalid); assertEquals(14, summary.skipped)
        assertEquals(listOf(20, 600), accepted.map { it.value })
    }
    @Test fun exactHeadersPreserveDstReportingAndCancellation() = runTest {
        val text = abbottCsv("Libre,synthetic,25-10-2020 02:30,0,110,,0.5,\r\nLibre,synthetic,29-03-2020 02:30,1,,120,0.5,")
        val (summary, rows) = parse(text)
        assertTrue(rows.isEmpty()); assertEquals(1, summary.ambiguous); assertEquals(1, summary.nonexistent)
        val earlier = parse(text, warsaw.copy(ambiguous = AmbiguousTimePolicy.EARLIER_OFFSET)).second.single()
        val later = parse(text, warsaw.copy(ambiguous = AmbiguousTimePolicy.LATER_OFFSET)).second.single()
        assertEquals(3600L, later.timestamp.epochSecond - earlier.timestamp.epochSecond)
        try {
            LibreViewCsvParser().parse(text.byteInputStream(), warsaw.copy(ambiguous = AmbiguousTimePolicy.EARLIER_OFFSET)) {
                throw CancellationException("synthetic cancellation")
            }
            fail("Cancellation must propagate")
        } catch (_: CancellationException) { }
    }
    @Test fun metadataRemainsInPreviewButNotSummaryDiagnosticsOrParserErrors() = runTest {
        val marker = "SYNTHETIC_PRIVATE_METADATA"
        val (summary, _) = parse(csv("", marker))
        assertTrue(summary.metadata.contains(marker))
        assertFalse("Summary diagnostics must redact metadata", summary.toString().contains(marker))
        try {
            parse(csv("", marker.repeat(1000)))
            fail("Oversized field must fail")
        } catch (error: CsvFormatException) {
            assertFalse("Exception must redact metadata", error.stackTraceToString().contains(marker))
            assertNull(error.cause)
        }
    }
    @Test fun englishGlucoseHeadersRemainSupportedWithKetones() = runTest {
        val text = "Glucose Data,Synthetic DEMO\r\nDevice,Serial Number,Device Timestamp,Record Type,Historic Glucose mg/dL,Scan Glucose mg/dL,Ketone mmol/L\r\nLibre,synthetic,01-05-2020 10:00,1,,120,0.5"
        assertEquals(120, parse(text).second.single().value)
    }
    @Test fun observedExportScaleIsStreamedWithoutRetainingMeasurements() = runTest {
        val text = buildString {
            append(csv(""))
            repeat(24_815) { append("Libre,synthetic,01-05-2020 10:00,0,110,,\r\n") }
            repeat(3_493) { append("Libre,synthetic,19-10-2026 10:00,1,,120,\r\n") }
            repeat(21_459) { append("Libre,synthetic,01-05-2020 10:00,6,,,event\r\n") }
        }
        var calls = 0
        val summary = LibreViewCsvParser().parse(text.byteInputStream(), warsaw) { calls++ }
        assertEquals(28_308, calls); assertEquals(21_459, summary.skipped)
        assertEquals(Instant.parse("2020-05-01T08:00:00Z"), summary.earliest)
        assertEquals(Instant.parse("2026-10-19T08:00:00Z"), summary.latest)
    }
}


