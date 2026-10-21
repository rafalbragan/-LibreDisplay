package com.libredisplay.diagnostics

import android.app.Application
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.test.core.app.ApplicationProvider
import com.libredisplay.ui.settings.shareLog
import org.junit.After
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowContentResolver
import org.robolectric.shadows.ShadowToast
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35])
class DiagnosticLoggerExportTest {
    private lateinit var context: Context
    private lateinit var source: File
    private lateinit var shared: File

    @Before fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        DiagnosticLogger.init(context)
        source = File(DiagnosticLogger.logFilePath())
        val directory = requireNotNull(source.parentFile)
        directory.deleteRecursively()
        directory.mkdirs()
        source.writeText("")
        shared = directory.resolve("shared")
        val authority = "${context.packageName}.fileprovider"
        val info = requireNotNull(context.packageManager.resolveContentProvider(authority, PackageManager.GET_META_DATA))
        val provider = FileProvider().apply { attachInfo(this@DiagnosticLoggerExportTest.context, info) }
        ShadowContentResolver.registerProviderInternal(authority, provider)
        if (File.separatorChar == '/') assertNotNull(FileProvider.getUriForFile(context, authority, source))
        ShadowToast.reset()
    }

    @After fun tearDown() {
        source.parentFile?.deleteRecursively()
    }

    @Test fun actualSharedFilePreservesSafeResponseDiagnostics() {
        DiagnosticLogger.logResponse("GRAPH", 503, """{"status":503,"error":{"message":"synthetic-private-detail"}}""",
            headers = mapOf("Retry-After" to "30"), responseByteCount = 42, responseDecoded = true)
        val intent = sharedIntent()
        val text = readShared(intent)
        assertTrue(text.contains("RESPONSE"))
        assertTrue(text.contains("step=GRAPH"))
        assertTrue(text.contains("httpCode=503"))
        assertTrue(text.contains("apiStatus=503"))
        assertTrue(text.contains("Retry-After=30"))
        assertTrue(text.contains("responseByteCount=42"))
        assertFalse(text.contains("synthetic-private-detail"))
        assertEquals(Intent.ACTION_SEND, intent.action)
        assertEquals("text/plain", intent.type)
        assertEquals(Intent.FLAG_GRANT_READ_URI_PERMISSION, intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION)
        assertEquals(uri(intent), intent.clipData!!.getItemAt(0).uri)
        assertNotEquals(Uri.fromFile(source), uri(intent))
        assertTrue(uri(intent).path!!.contains("/shared/"))
    }

    @Test fun actualSharedBytesOmitCredentialsHeadersUrlsAndPersonalDetails() {
        val secrets = listOf("secret-password", "secret-token", "secret-cookie", "secret-basic", "secret-bearer",
            "secret-account", "secret-patient", "patient@example.invalid", "Jan Kowalski", "secret-query", "secret-fragment",
            "encoded%2Fsecret", "555123456", "192.0.2.42", "secret-unknown", "secret-tag")
        val raw = """
            2026-10-21 10:00:00 [ERROR] secret-tag HTTP=503
            password=secret-password token=secret-token
            Authorization: Basic secret-basic
            authorization=Bearer secret-bearer
            Cookie: session=secret-cookie
            Set-Cookie: session=secret-cookie; HttpOnly
            {"accountId":"secret-account","patientId":"secret-patient","email":"patient@example.invalid","name":"Jan Kowalski"}
            https://patient@example.invalid:secret-password@fixture.invalid/patient/secret-patient?opaque=secret-query#secret-fragment
            Location=https://fixture.invalid/encoded%2Fsecret
            java.io.IOException: secret-unknown
                at unknown.path.secret-unknown(Source.kt:12)
            phone=555123456 ip=192.0.2.42
            x-new-sensitive-field=secret-unknown
            METHOD=GET
            STEP=CONNECTIONS
        """.trimIndent()
        source.writeText(raw)
        val exported = readShared(sharedIntent())
        secrets.forEach { assertFalse("Leaked synthetic marker: $it", exported.contains(it)) }
        assertTrue(exported.contains("2026-10-21 10:00:00 [ERROR]"))
        assertTrue(exported.contains("HTTP=503"))
        assertTrue(exported.contains("java.io.IOException"))
        assertTrue(exported.contains("METHOD=GET"))
        assertTrue(exported.contains("STEP=CONNECTIONS"))
        assertEquals("Local log must not be replaced by export", raw, source.readText())
        assertEquals(1, shared.listFiles()!!.size)
        shared.listFiles()!!.forEach { file ->
            secrets.forEach { assertFalse(file.readText().contains(it)) }
        }
    }

    @Test fun encodedAndMixedCaseUnknownFieldsCannotBypassSanitization() {
        val raw = """
            PaSsWoRd: "private marker with spaces"
            {"access_token":"private marker with spaces","nested":{"refreshToken":"another marker"}}
            HEADER={x-session=another marker}
            https://fixture.invalid/?opaque=%70%72%69%76%61%74%65
            httpCode=503 secret-suffix
            STEP=GRAPH secret-suffix
            apiStatus=12345678901234567890
            Caused by: java.lang.IllegalStateException: another marker
        """.trimIndent()
        val clean = DiagnosticShareSanitizer.sanitize(raw)
        listOf("private", "another", "%70", "secret-suffix", "12345678901234567890").forEach { assertFalse(clean.contains(it)) }
        assertTrue(clean.contains("java.lang.IllegalStateException"))
    }

    @Test fun emptyLogHasExplicitSafeContent() {
        assertEquals("Brak wpisów diagnostycznych", readShared(sharedIntent()))
    }

    @Test fun snapshotsAreIndependentAndNeverExposeLaterRawWrites() {
        source.writeText("httpCode=503")
        val first = sharedIntent()
        source.writeText("password=later-secret\nhttpCode=401")
        val second = sharedIntent()
        assertNotEquals(uri(first), uri(second))
        assertEquals("httpCode=503", readShared(first))
        assertTrue(readShared(second).contains("httpCode=401"))
        assertFalse(readShared(second).contains("later-secret"))
    }

    @Test fun unreadableSourceDoesNotBecomeSuccessfulEmptyExport() {
        source.delete()
        source.mkdir() // Deterministic IOException on read, including on Windows/admin accounts.
        assertNull(DiagnosticLogger.createShareIntent(context))
        assertFalse(shared.exists())
    }

    @Test fun blockedDestinationDoesNotFallBackToRawOrPreviousFile() {
        source.writeText("token=private-token")
        val previous = sharedIntent()
        shared.deleteRecursively()
        shared.writeText("blocked")
        assertNull(DiagnosticLogger.createShareIntent(context))
        assertFalse(uri(previous).path!!.endsWith("librecare.log"))
        assertEquals("token=private-token", source.readText())
    }

    @Test fun fileProviderFailureDeletesPreparedSnapshotAndReturnsNoIntent() {
        source.writeText("token=private-token")
        val wrongAuthority = object : ContextWrapper(context) {
            override fun getPackageName() = "invalid.synthetic.authority"
        }
        assertNull(DiagnosticLogger.createShareIntent(wrongAuthority))
        assertTrue(shared.listFiles().orEmpty().isEmpty())
    }

    @Test fun clearingLogAlsoRemovesShareSnapshots() {
        sharedIntent()
        DiagnosticLogger.clear()
        assertTrue(shared.listFiles().orEmpty().isEmpty())
        assertEquals("", source.readText())
    }

    @Test fun sharePreparationFailureIsReportedWithoutLaunchingAnything() {
        shared.writeText("blocked")
        var launched = false
        val caller = object : ContextWrapper(context) {
            override fun startActivity(intent: Intent) { launched = true }
        }
        shareLog(caller)
        assertFalse(launched)
        assertEquals("Nie można udostępnić logu diagnostycznego. Spróbuj ponownie.", ShadowToast.getTextOfLatestToast())
    }

    @Test fun chooserLaunchFailureIsReportedWithoutRawFallback() {
        requireUnixFileProvider()
        var attempts = 0
        val caller = object : ContextWrapper(context) {
            override fun startActivity(intent: Intent) {
                attempts++
                assertEquals(Intent.ACTION_CHOOSER, intent.action)
                throw ActivityNotFoundException("synthetic")
            }
        }
        shareLog(caller)
        assertEquals(1, attempts)
        assertEquals("Nie można udostępnić logu diagnostycznego. Spróbuj ponownie.", ShadowToast.getTextOfLatestToast())
    }

    private fun requireUnixFileProvider() {
        // AndroidX uses '/' when comparing canonical roots; Windows Robolectric uses '\\'.
        // Keep the actual URI assertions on Linux CI; API 36 instrumentation also reads the bytes.
        assumeTrue("AndroidX FileProvider canonical roots require Unix in Robolectric", File.separatorChar == '/')
    }

    private fun sharedIntent(): Intent {
        requireUnixFileProvider()
        return requireNotNull(DiagnosticLogger.createShareIntent(context))
    }

    @Suppress("DEPRECATION")
    private fun uri(intent: Intent): Uri = requireNotNull(intent.getParcelableExtra(Intent.EXTRA_STREAM))
    private fun readShared(intent: Intent): String = requireNotNull(context.contentResolver.openInputStream(uri(intent)))
        .bufferedReader(Charsets.UTF_8).use { it.readText() }
}

