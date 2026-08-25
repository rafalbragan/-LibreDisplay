package com.libredisplay.analytics

import com.libredisplay.data.model.GlucoseHistoryPoint
import java.io.File
import java.io.FileOutputStream
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object RawDataExcelExporter {

    private val localDateTimeFormat = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss", Locale("pl", "PL"))

    fun writeRawDataWorkbook(
        destination: File,
        personDisplayName: String,
        patientId: String,
        readings: List<GlucoseHistoryPoint>,
        zoneId: ZoneId = ZoneId.systemDefault()
    ) {
        destination.parentFile?.mkdirs()
        val sorted = readings.sortedBy { it.timestamp }
        FileOutputStream(destination).use { fos ->
            ZipOutputStream(fos).use { zip ->
                put(zip, "[Content_Types].xml", contentTypes())
                put(zip, "_rels/.rels", rootRels())
                put(zip, "xl/workbook.xml", workbookXml())
                put(zip, "xl/_rels/workbook.xml.rels", workbookRels())
                put(zip, "xl/styles.xml", stylesXml())
                put(zip, "xl/worksheets/sheet1.xml", rawDataSheetXml(personDisplayName, patientId, sorted, zoneId))
                put(zip, "xl/worksheets/sheet2.xml", summarySheetXml(personDisplayName, patientId, sorted))
            }
        }
    }

    private fun put(zip: ZipOutputStream, path: String, content: String) {
        zip.putNextEntry(ZipEntry(path))
        zip.write(content.toByteArray(Charsets.UTF_8))
        zip.closeEntry()
    }

    private fun contentTypes(): String =
        """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
          <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
          <Default Extension="xml" ContentType="application/xml"/>
          <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
          <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
          <Override PartName="/xl/worksheets/sheet2.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
          <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
        </Types>
        """.trimIndent()

    private fun rootRels(): String =
        """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
          <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
        </Relationships>
        """.trimIndent()

    private fun workbookXml(): String =
        """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"
          xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
          <sheets>
            <sheet name="Dane surowe" sheetId="1" r:id="rId1"/>
            <sheet name="Podsumowanie" sheetId="2" r:id="rId2"/>
          </sheets>
        </workbook>
        """.trimIndent()

    private fun workbookRels(): String =
        """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
          <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
          <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet2.xml"/>
          <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
        </Relationships>
        """.trimIndent()

    private fun stylesXml(): String =
        """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
          <fonts count="1"><font><sz val="11"/><name val="Calibri"/></font></fonts>
          <fills count="1"><fill><patternFill patternType="none"/></fill></fills>
          <borders count="1"><border/></borders>
          <cellStyleXfs count="1"><xf/></cellStyleXfs>
          <cellXfs count="1"><xf xfId="0"/></cellXfs>
          <cellStyles count="1"><cellStyle name="Normal" xfId="0" builtinId="0"/></cellStyles>
        </styleSheet>
        """.trimIndent()

    private fun rawDataSheetXml(
        personDisplayName: String,
        patientId: String,
        readings: List<GlucoseHistoryPoint>,
        zoneId: ZoneId
    ): String {
        val rows = StringBuilder()
        var rowIndex = 1
        fun textCell(ref: String, value: String): String {
            return "<c r=\"$ref\" t=\"inlineStr\"><is><t>${escape(value)}</t></is></c>"
        }
        fun numCell(ref: String, value: Int): String {
            return "<c r=\"$ref\"><v>$value</v></c>"
        }

        rows.append("<row r=\"${rowIndex++}\">")
        rows.append(textCell("A1", "Osoba"))
        rows.append(textCell("B1", personDisplayName))
        rows.append(textCell("C1", "ID pacjenta"))
        rows.append(textCell("D1", patientId))
        rows.append("</row>")

        rows.append("<row r=\"${rowIndex++}\">")
        rows.append(textCell("A2", "timestamp_utc"))
        rows.append(textCell("B2", "timestamp_lokalny"))
        rows.append(textCell("C2", "glukoza_mg_dl"))
        rows.append(textCell("D2", "trend_strzalka"))
        rows.append(textCell("E2", "trend_opis"))
        rows.append("</row>")

        readings.forEach { point ->
            val rowNumber = rowIndex++
            val localText = localDateTimeFormat.format(point.timestamp.atZone(zoneId))
            rows.append("<row r=\"$rowNumber\">")
            rows.append(textCell("A$rowNumber", point.timestamp.toString()))
            rows.append(textCell("B$rowNumber", localText))
            rows.append(numCell("C$rowNumber", point.value))
            rows.append(textCell("D$rowNumber", point.trend.arrow))
            rows.append(textCell("E$rowNumber", point.trend.description))
            rows.append("</row>")
        }

        return """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
              <sheetData>
                $rows
              </sheetData>
            </worksheet>
        """.trimIndent()
    }

    private fun summarySheetXml(personDisplayName: String, patientId: String, readings: List<GlucoseHistoryPoint>): String {
        val min = readings.minOfOrNull { it.value }
        val max = readings.maxOfOrNull { it.value }
        val avg = readings.map { it.value }.average().takeIf { it.isFinite() }
        val first = readings.firstOrNull()?.timestamp?.toString().orEmpty()
        val last = readings.lastOrNull()?.timestamp?.toString().orEmpty()

        val rows = buildString {
            append("<row r=\"1\">")
            append("<c r=\"A1\" t=\"inlineStr\"><is><t>Podsumowanie eksportu</t></is></c>")
            append("</row>")

            append(labelValueRow(2, "Osoba", personDisplayName))
            append(labelValueRow(3, "ID pacjenta", patientId))
            append(labelValueRow(4, "Liczba odczytów", readings.size.toString()))
            append(labelValueRow(5, "Pierwszy odczyt UTC", first))
            append(labelValueRow(6, "Ostatni odczyt UTC", last))
            append(labelValueRow(7, "Minimum", min?.toString().orEmpty()))
            append(labelValueRow(8, "Maksimum", max?.toString().orEmpty()))
            append(labelValueRow(9, "Średnia", avg?.let { String.format(Locale.US, "%.1f", it) }.orEmpty()))
        }

        return """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
              <sheetData>
                $rows
              </sheetData>
            </worksheet>
        """.trimIndent()
    }

    private fun labelValueRow(index: Int, label: String, value: String): String {
        val a = "<c r=\"A$index\" t=\"inlineStr\"><is><t>${escape(label)}</t></is></c>"
        val b = "<c r=\"B$index\" t=\"inlineStr\"><is><t>${escape(value)}</t></is></c>"
        return "<row r=\"$index\">$a$b</row>"
    }

    private fun escape(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}

