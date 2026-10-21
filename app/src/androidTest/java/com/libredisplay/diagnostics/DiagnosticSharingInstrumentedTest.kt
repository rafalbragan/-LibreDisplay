package com.libredisplay.diagnostics

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File

/** Real FileProvider/content-resolver verification, synthetic data on an emulator only. */
class DiagnosticSharingInstrumentedTest {
    private lateinit var context: Context
    private lateinit var source: File

    @Before fun prepareSyntheticLog() {
        check(Build.HARDWARE in listOf("ranchu", "goldfish") && !Build.MODEL.startsWith("SM-"))
        context = ApplicationProvider.getApplicationContext()
        DiagnosticLogger.init(context)
        DiagnosticLogger.clear()
        source = File(DiagnosticLogger.logFilePath())
    }

    @After fun clearSyntheticLog() { DiagnosticLogger.clear() }

    @Suppress("DEPRECATION")
    @Test fun actualFileProviderBytesAreSanitizedAndReadable() {
        source.writeText("httpCode=503\nRetry-After=30\nAuthorization: Basic synthetic-private-auth\n" +
            "Cookie: synthetic-private-cookie\npatientId=synthetic-private-person\n" +
            "https://fixture.invalid/?opaque=synthetic-private-url\njava.io.IOException: synthetic-private-error")
        val intent = requireNotNull(DiagnosticLogger.createShareIntent(context))
        val uri = requireNotNull(intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM))
        val bytes = requireNotNull(context.contentResolver.openInputStream(uri)).use { it.readBytes() }
        val exported = bytes.toString(Charsets.UTF_8)
        assertFalse(exported.contains("synthetic-private"))
        assertTrue(exported.contains("httpCode=503"))
        assertTrue(exported.contains("Retry-After=30"))
        assertTrue(exported.contains("java.io.IOException"))
        assertEquals(Intent.FLAG_GRANT_READ_URI_PERMISSION, intent.flags)
        assertEquals(uri, intent.clipData!!.getItemAt(0).uri)
        assertTrue(uri.path!!.contains("/shared/"))
        assertTrue(source.readText().contains("synthetic-private-auth"))
    }

    @Test fun blockedOutputNeverReturnsRawLogIntent() {
        source.writeText("password=synthetic-private-password")
        val shared = requireNotNull(source.parentFile).resolve("shared")
        shared.deleteRecursively()
        shared.writeText("blocked")
        assertNull(DiagnosticLogger.createShareIntent(context))
        assertEquals("password=synthetic-private-password", source.readText())
    }
}

