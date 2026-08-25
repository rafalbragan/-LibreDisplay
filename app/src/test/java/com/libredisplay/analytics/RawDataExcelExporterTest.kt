package com.libredisplay.analytics

import com.libredisplay.data.model.GlucoseHistoryPoint
import com.libredisplay.data.model.GlucoseTrend
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.Instant
import java.util.zip.ZipFile

class RawDataExcelExporterTest {

    @Test
    fun writeRawDataWorkbook_createsXlsxWithRawDataAndSummarySheets() {
        val file = File.createTempFile("librecare-raw-", ".xlsx")
        file.deleteOnExit()

        RawDataExcelExporter.writeRawDataWorkbook(
            destination = file,
            personDisplayName = "Jan Kowalski",
            patientId = "patient-1",
            readings = listOf(
                reading("2026-08-24T10:00:00Z", 110),
                reading("2026-08-24T10:05:00Z", 120)
            )
        )

        assertTrue(file.exists())
        assertTrue(file.length() > 0)

        ZipFile(file).use { zip ->
            assertTrue(zip.getEntry("xl/worksheets/sheet1.xml") != null)
            assertTrue(zip.getEntry("xl/worksheets/sheet2.xml") != null)
            val workbook = zip.getInputStream(zip.getEntry("xl/workbook.xml"))
                .bufferedReader(Charsets.UTF_8)
                .readText()
            assertTrue(workbook.contains("Dane surowe"))
            val sheet1 = zip.getInputStream(zip.getEntry("xl/worksheets/sheet1.xml"))
                .bufferedReader(Charsets.UTF_8)
                .readText()
            assertTrue(sheet1.contains("timestamp_utc"))
            assertTrue(sheet1.contains("2026-08-24T10:00:00Z"))
            assertTrue(sheet1.contains("glukoza_mg_dl"))
        }
    }

    private fun reading(timestamp: String, value: Int): GlucoseHistoryPoint {
        return GlucoseHistoryPoint(
            value = value,
            timestamp = Instant.parse(timestamp),
            trend = GlucoseTrend.FLAT
        )
    }
}


