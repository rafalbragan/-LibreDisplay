package com.libredisplay.ui.monitoring

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.libredisplay.data.model.GlucoseReading
import java.time.Duration
import java.time.Instant

internal enum class WarningTone {
    SAFE,
    CAUTION,
    WARNING,
    URGENT,
    CRITICAL,
    MUTED
}

internal enum class GlucoseWarningLevel(val priority: Int) {
    VERY_LOW(1),
    STALE_DATA(2),
    EXTREME_HIGH(3),
    VERY_HIGH(4),
    LOW(5),
    HIGH(6),
    SENSOR_GAP(7),
    IN_RANGE(8),
    NO_DATA(9)
}

internal data class GlucoseWarningConfig(
    val veryLowMgDl: Int = 54,
    val lowMgDl: Int = 70,
    val targetLowMgDl: Int = 70,
    val targetHighMgDl: Int = 180,
    val highWarningMgDl: Int = 200,
    val veryHighMgDl: Int = 300,
    val extremeHighMgDl: Int = 400,
    val staleAfter: Duration = Duration.ofMinutes(15)  // Ujednolcony próg - 15 minut
)

internal data class GlucoseWarning(
    val level: GlucoseWarningLevel,
    val title: String,
    val message: String,
    val tone: WarningTone,
    val iconName: String
)

internal data class GlucoseStatusPresentation(
    val primary: GlucoseWarning,
    val secondary: List<GlucoseWarning>,
    val medicalWarning: GlucoseWarning? = null,
    val freshnessWarning: GlucoseWarning? = null,
    val isStale: Boolean = false
)

internal fun buildGlucoseWarnings(
    reading: GlucoseReading?,
    now: Instant = Instant.now(),
    config: GlucoseWarningConfig = GlucoseWarningConfig()
): List<GlucoseWarning> {
    if (reading == null) {
        return listOf(
            GlucoseWarning(
                level = GlucoseWarningLevel.NO_DATA,
                title = "Brak danych",
                message = "Aplikacja nie ma aktualnego odczytu glikemii.",
                tone = WarningTone.MUTED,
                iconName = "access_time"
            )
        )
    }

    val age = Duration.between(reading.timestamp, now)
    val underlying = when {
        reading.value < config.veryLowMgDl -> GlucoseWarning(
            level = GlucoseWarningLevel.VERY_LOW,
            title = "Bardzo niska glikemia",
            message = "Bardzo niska glikemia. To może wymagać pilnej pomocy. Jeśli osoba nie może bezpiecznie przyjąć węglowodanów lub traci przytomność, wezwij pomoc.",
            tone = WarningTone.CRITICAL,
            iconName = "error_outline"
        )
        reading.value < config.lowMgDl -> GlucoseWarning(
            level = GlucoseWarningLevel.LOW,
            title = "Niska glikemia",
            message = "Niska glikemia. Podejmij działania zgodne z planem leczenia hipoglikemii.",
            tone = WarningTone.URGENT,
            iconName = "warning_amber"
        )
        reading.value > config.extremeHighMgDl -> GlucoseWarning(
            level = GlucoseWarningLevel.EXTREME_HIGH,
            title = "Skrajnie wysoka glikemia",
            message = "Skrajnie wysoka glikemia. Jeśli występują objawy, ketony lub złe samopoczucie, pilnie skontaktuj się z pomocą medyczną.",
            tone = WarningTone.CRITICAL,
            iconName = "error_outline"
        )
        reading.value > config.veryHighMgDl -> GlucoseWarning(
            level = GlucoseWarningLevel.VERY_HIGH,
            title = "Bardzo wysoka glikemia",
            message = "Bardzo wysoka glikemia. Jeśli utrzymuje się lub występują objawy, skontaktuj się z lekarzem.",
            tone = WarningTone.URGENT,
            iconName = "error_outline"
        )
        reading.value > config.highWarningMgDl -> GlucoseWarning(
            level = GlucoseWarningLevel.HIGH,
            title = "Wysoka glikemia",
            message = "Wysoka glikemia. Sprawdź trend i rozważ działania zgodne z planem leczenia.",
            tone = WarningTone.WARNING,
            iconName = "warning_amber"
        )
        reading.value > config.targetHighMgDl -> GlucoseWarning(
            level = GlucoseWarningLevel.HIGH,
            title = "Glikemia powyżej zakresu",
            message = "Glikemia powyżej zakresu. Obserwuj trend i postępuj zgodnie z zaleceniami lekarza.",
            tone = WarningTone.CAUTION,
            iconName = "warning_amber"
        )
        else -> GlucoseWarning(
            level = GlucoseWarningLevel.IN_RANGE,
            title = "Glikemia w zakresie.",
            message = "Glikemia w zakresie.",
            tone = WarningTone.SAFE,
            iconName = "verified"
        )
    }

    val warnings = mutableListOf(underlying)
    if (age > config.staleAfter) {
        warnings += GlucoseWarning(
            level = GlucoseWarningLevel.STALE_DATA,
            title = "Dane nieaktualne",
            message = "Ostatni odczyt jest starszy niż ${config.staleAfter.toMinutes()} min. Aktualna glikemia może być inna niż ostatnio zapisana.",
            tone = WarningTone.MUTED,
            iconName = "access_time"
        )
    }

    return warnings.sortedBy { it.level.priority }
}

internal fun buildGlucoseStatusPresentation(
    reading: GlucoseReading?,
    now: Instant = Instant.now(),
    config: GlucoseWarningConfig = GlucoseWarningConfig()
): GlucoseStatusPresentation {
    val warnings = buildGlucoseWarnings(reading = reading, now = now, config = config)
    val freshnessWarning = warnings.firstOrNull { it.level == GlucoseWarningLevel.STALE_DATA }
    val medicalWarning = warnings.firstOrNull {
        it.level != GlucoseWarningLevel.STALE_DATA && it.level != GlucoseWarningLevel.NO_DATA
    }
    if (medicalWarning == null) {
        return GlucoseStatusPresentation(
            primary = warnings.first(),
            secondary = warnings.drop(1),
            medicalWarning = null,
            freshnessWarning = freshnessWarning,
            isStale = freshnessWarning != null
        )
    }

    val shouldLeadWithFreshness = freshnessWarning != null && medicalWarning.tone !in setOf(WarningTone.URGENT, WarningTone.CRITICAL)
    val primary = if (shouldLeadWithFreshness) freshnessWarning else medicalWarning
    val secondary = buildList {
        warnings.forEach { warning ->
            if (warning != primary) add(warning)
        }
    }
    return GlucoseStatusPresentation(
        primary = primary,
        secondary = secondary,
        medicalWarning = medicalWarning,
        freshnessWarning = freshnessWarning,
        isStale = freshnessWarning != null
    )
}

internal fun warningToneColor(tone: WarningTone): Color = when (tone) {
    WarningTone.SAFE -> Color(0xFF43C59E)
    WarningTone.CAUTION -> Color(0xFFF2B84B)
    WarningTone.WARNING -> Color(0xFFE05A6A)
    WarningTone.URGENT -> Color(0xFFD6455D)
    WarningTone.CRITICAL -> Color(0xFFB8324A)
    WarningTone.MUTED -> Color(0xFF94A3B8)
}

internal fun warningIcon(iconName: String): ImageVector = when (iconName) {
    "verified" -> Icons.Filled.Verified
    "access_time" -> Icons.Filled.AccessTime
    "error_outline" -> Icons.Filled.ErrorOutline
    else -> Icons.Filled.WarningAmber
}

