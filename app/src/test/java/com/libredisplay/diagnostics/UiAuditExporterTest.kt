package com.libredisplay.diagnostics

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import com.libredisplay.AppScreen

class UiAuditExporterTest {

    @Test
    fun isAllowedEmail_acceptsConfiguredAddress_caseInsensitiveAndTrimmed() {
        assertTrue(UiAuditExporter.isAllowedEmail("rafal.b.ragan@gmail.com"))
        assertTrue(UiAuditExporter.isAllowedEmail(" RAFAL.B.RAGAN@GMAIL.COM "))
    }

    @Test
    fun isAllowedEmail_rejectsOtherAddresses() {
        assertFalse(UiAuditExporter.isAllowedEmail("other@example.com"))
        assertFalse(UiAuditExporter.isAllowedEmail(""))
    }

    @Test
    fun buildReportContent_listsMetadataFiles() {
        val report = UiAuditExporter.buildReportContent(
            appVersion = "2.0.1",
            generatedAt = LocalDateTime.of(2026, 8, 20, 12, 0),
            results = listOf(
                UiAuditCaptureResult(
                    step = UiAuditStep(
                        screen = AppScreen.Monitoring,
                        label = "Główny",
                        routePath = "Główny",
                        expectedBehaviors = listOf("wykres")
                    ),
                    screenshotFileName = "01_glowny.png",
                    captureSuccess = true,
                    metadataFileName = "01_glowny.json"
                )
            )
        )

        assertTrue(report.contains("01_glowny.json"))
        assertTrue(report.contains("captureMode=viewport_png"))
        assertTrue(report.contains("Tekstowy score UX"))
    }
}

