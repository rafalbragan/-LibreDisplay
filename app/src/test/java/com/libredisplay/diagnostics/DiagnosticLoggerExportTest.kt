package com.libredisplay.diagnostics

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class)
class DiagnosticLoggerExportTest {

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        DiagnosticLogger.init(context)
        DiagnosticLogger.clear()
    }

    @Test
    fun exportRedactedLog_masksSensitiveFields() {
        DiagnosticLogger.logInfo(
            "Diagnostics",
            "token=super-secret patientId=PATIENT123 selectedPersonName=Jan Kowalski Bearer abc.def"
        )

        val exported = DiagnosticLogger.exportRedactedLog()

        assertFalse(exported.contains("super-secret"))
        assertFalse(exported.contains("PATIENT123"))
        assertFalse(exported.contains("Jan Kowalski"))
        assertFalse(exported.contains("abc.def"))
    }

    @Test
    fun recordUnhandledException_persistsCrashReportWithContext() {
        val throwable = IllegalStateException("patientId=PATIENT321 token=abc")

        DiagnosticLogger.recordUnhandledException(
            throwable = throwable,
            threadName = "main",
            lastScreen = "Monitoring",
            lastOperation = "navigateTo:Diagnostics"
        )

        val crashReport = DiagnosticLogger.readLastCrashReport()

        assertTrue(crashReport.contains("versionName="))
        assertTrue(crashReport.contains("versionCode="))
        assertTrue(crashReport.contains("commitSha="))
        assertTrue(crashReport.contains("lastScreen=Monitoring"))
        assertTrue(crashReport.contains("lastOperation=navigateTo:Diagnostics"))
        assertFalse(crashReport.contains("PATIENT321"))
    }
}

