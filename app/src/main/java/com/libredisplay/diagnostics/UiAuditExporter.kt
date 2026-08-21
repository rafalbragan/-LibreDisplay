package com.libredisplay.diagnostics

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import com.google.gson.GsonBuilder
import com.libredisplay.AppScreen
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

data class UiAuditStep(
    val screen: AppScreen,
    val label: String,
    val routePath: String,
    val expectedBehaviors: List<String>
)

data class UiAuditCaptureContext(
    val appVersion: String,
    val appMode: String,
    val selectedPatientId: String?,
    val refreshIntervalSeconds: Int,
    val backgroundPollingMinutes: Int,
    val retentionHours: Int,
    val targetLowMgDl: Int,
    val targetHighMgDl: Int
)

data class UiAuditImageMetadata(
    val fileName: String,
    val absolutePath: String,
    val widthPx: Int,
    val heightPx: Int,
    val fileSizeBytes: Long,
    val density: Float,
    val densityDpi: Int,
    val screenWidthDp: Int,
    val screenHeightDp: Int,
    val fontScale: Float,
    val orientation: String,
    val darkTheme: Boolean,
    val captureMode: String
)

data class UiAuditDeviceMetadata(
    val manufacturer: String,
    val brand: String,
    val model: String,
    val device: String,
    val product: String,
    val hardware: String,
    val sdkInt: Int,
    val release: String,
    val locale: String
)

data class UiAuditCaptureMetadata(
    val generatedAt: String,
    val stepLabel: String,
    val routePath: String,
    val screen: String,
    val expectedBehaviors: List<String>,
    val image: UiAuditImageMetadata,
    val device: UiAuditDeviceMetadata,
    val app: UiAuditCaptureContext
)

data class UiAuditCaptureResult(
    val step: UiAuditStep,
    val screenshotFileName: String,
    val captureSuccess: Boolean,
    val metadataFileName: String? = null,
    val screenWidthDp: Int? = null,
    val fontScale: Float? = null,
    val orientation: String? = null,
    val darkTheme: Boolean? = null
)

object UiAuditExporter {
    private const val ALLOWED_EMAIL = "rafal.b.ragan@gmail.com"
    private val stampFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
    private val gson = GsonBuilder().setPrettyPrinting().create()

    fun isAllowedEmail(email: String): Boolean {
        return email.trim().lowercase() == ALLOWED_EMAIL
    }

    fun createSessionDirectory(activity: Activity): File {
        val stamp = LocalDateTime.now().format(stampFormatter)
        val baseDir = activity.getExternalFilesDir(null)?.resolve("ui-audit")
            ?: File(activity.filesDir, "ui-audit")
        val sessionDir = File(baseDir, "session-$stamp")
        sessionDir.mkdirs()
        return sessionDir
    }

    fun captureCurrentScreen(activity: Activity, destinationFile: File): Boolean {
        val root = activity.window?.decorView?.rootView ?: return false
        if (root.width <= 0 || root.height <= 0) return false

        val bitmap = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        root.draw(canvas)

        return runCatching {
            FileOutputStream(destinationFile).use { output ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
            }
            true
        }.getOrElse { false }
    }

    fun writeCaptureMetadata(
        activity: Activity,
        destinationFile: File,
        screenshotFile: File,
        step: UiAuditStep,
        context: UiAuditCaptureContext
    ): Boolean {
        val root = activity.window?.decorView?.rootView ?: return false
        val resources = activity.resources
        val configuration = resources.configuration
        val displayMetrics = resources.displayMetrics
        val imageMetadata = UiAuditImageMetadata(
            fileName = screenshotFile.name,
            absolutePath = screenshotFile.absolutePath,
            widthPx = root.width,
            heightPx = root.height,
            fileSizeBytes = screenshotFile.length().coerceAtLeast(0L),
            density = displayMetrics.density,
            densityDpi = displayMetrics.densityDpi,
            screenWidthDp = configuration.screenWidthDp,
            screenHeightDp = configuration.screenHeightDp,
            fontScale = configuration.fontScale,
            orientation = when (configuration.orientation) {
                android.content.res.Configuration.ORIENTATION_LANDSCAPE -> "landscape"
                android.content.res.Configuration.ORIENTATION_PORTRAIT -> "portrait"
                else -> "undefined"
            },
            darkTheme = (configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES,
            captureMode = "viewport_png"
        )
        val deviceMetadata = UiAuditDeviceMetadata(
            manufacturer = Build.MANUFACTURER.orEmpty(),
            brand = Build.BRAND.orEmpty(),
            model = Build.MODEL.orEmpty(),
            device = Build.DEVICE.orEmpty(),
            product = Build.PRODUCT.orEmpty(),
            hardware = Build.HARDWARE.orEmpty(),
            sdkInt = Build.VERSION.SDK_INT,
            release = Build.VERSION.RELEASE.orEmpty(),
            locale = Locale.getDefault().toLanguageTag()
        )
        val metadata = UiAuditCaptureMetadata(
            generatedAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            stepLabel = step.label,
            routePath = step.routePath,
            screen = step.screen.name,
            expectedBehaviors = step.expectedBehaviors,
            image = imageMetadata,
            device = deviceMetadata,
            app = context
        )
        return runCatching {
            destinationFile.writeText(gson.toJson(metadata), Charsets.UTF_8)
            true
        }.getOrElse { false }
    }

    fun buildReportContent(
        appVersion: String,
        generatedAt: LocalDateTime,
        results: List<UiAuditCaptureResult>
    ): String {
        val sb = StringBuilder()
        sb.appendLine("# LibreCare UI Audit Report")
        sb.appendLine()
        sb.appendLine("- Wersja aplikacji: $appVersion")
        sb.appendLine("- Data wygenerowania: ${generatedAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))}")
        sb.appendLine("- Liczba krokow: ${results.size}")
        sb.appendLine()
        sb.appendLine("## Zrzuty ekranow")
        sb.appendLine()

        results.forEachIndexed { index, result ->
            val status = if (result.captureSuccess) "OK" else "BLAD"
            val score = scoreCapture(result)
            val scoreLabel = scoreLabel(score)
            sb.appendLine("${index + 1}. [$status] ${result.step.label}")
            sb.appendLine("   - Sciezka ekranu: ${result.step.routePath}")
            sb.appendLine("   - Plik: ${result.screenshotFileName}")
            result.metadataFileName?.let { sb.appendLine("   - Metadane: $it") }
            sb.appendLine("   - Tekstowy score UX: $score/100 ($scoreLabel)")
            val flags = captureFlags(result)
            if (flags.isNotEmpty()) {
                sb.appendLine("   - Flagi ryzyka: ${flags.joinToString(", ")}")
            }
            sb.appendLine("   - Zachowania do weryfikacji:")
            result.step.expectedBehaviors.forEach { behavior ->
                sb.appendLine("     - $behavior")
            }
            sb.appendLine()
        }

        val globalScore = if (results.isEmpty()) 0 else results.map { scoreCapture(it) }.average().toInt()
        sb.appendLine("## Tekstowy score UX")
        sb.appendLine("- Sredni score: $globalScore/100 (${scoreLabel(globalScore)})")
        sb.appendLine("- Interpretacja: 90-100 bardzo dobry, 75-89 dobry, 60-74 umiarkowany, <60 wysoki priorytet poprawy.")
        sb.appendLine("- Uwaga: score jest heurystyczny i nie zastępuje recznej oceny medycznej czy testow na urzadzeniu.")
        sb.appendLine()

        sb.appendLine("## Globalna checklista UX (optymalizacje)")
        sb.appendLine("- Gestosc informacji: czy sekcje nie marnuja pionowej przestrzeni?")
        sb.appendLine("- Hierarchia: czy najwazniejsze dane sa najwyzej i bez duplikacji?")
        sb.appendLine("- Spacing: czy nie ma nadmiarowych Spacer/padding 24dp+?")
        sb.appendLine("- Nawigacja: czy kazdy przycisk prowadzi do oczekiwanego celu?")
        sb.appendLine("- Komunikaty pustego stanu: czy sa precyzyjne i pomocne?")
        sb.appendLine("- Accessibility: kontrast, dotyk 48dp+, czytelna typografia.")
        sb.appendLine()
        sb.appendLine("## Uwagi")
        sb.appendLine("- Raport jest generowany automatycznie z aktywnej sesji aplikacji.")
        sb.appendLine("- Do kazdego zrzutu dopisywany jest plik JSON z metadanymi urzadzenia, rozdzielczoscia i ustawieniami aplikacji.")
        sb.appendLine("- captureMode=viewport_png oznacza aktualnie zrzut widocznego viewportu podczas audytu.")

        return sb.toString()
    }

    private fun scoreCapture(result: UiAuditCaptureResult): Int {
        var score = 100
        if (!result.captureSuccess) score -= 70

        val width = result.screenWidthDp
        if (width != null) {
            when {
                width < 360 -> score -= 14
                width < 400 -> score -= 8
            }
        }

        val fontScale = result.fontScale
        if (fontScale != null) {
            when {
                fontScale >= 1.5f -> score -= 16
                fontScale >= 1.3f -> score -= 10
            }
        }

        return score.coerceIn(0, 100)
    }

    private fun scoreLabel(score: Int): String = when {
        score >= 90 -> "bardzo dobry"
        score >= 75 -> "dobry"
        score >= 60 -> "umiarkowany"
        else -> "wymaga poprawy"
    }

    private fun captureFlags(result: UiAuditCaptureResult): List<String> {
        val flags = mutableListOf<String>()
        if (!result.captureSuccess) flags += "brak zrzutu"
        result.screenWidthDp?.let {
            if (it < 360) flags += "bardzo waski viewport"
            else if (it < 400) flags += "waski viewport"
        }
        result.fontScale?.let {
            if (it >= 1.5f) flags += "duzy fontScale (>=1.5)"
            else if (it >= 1.3f) flags += "podniesiony fontScale (>=1.3)"
        }
        return flags
    }
}
