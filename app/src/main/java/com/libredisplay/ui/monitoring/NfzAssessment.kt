package com.libredisplay.ui.monitoring

import com.libredisplay.analytics.GlucoseMetricsCalculator
import com.libredisplay.data.model.GlucoseHistoryPoint
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import kotlin.math.roundToInt

internal data class NfzRefundCriteriaConfig(
    val minimumSensorActivityPercent: Int = 75,
    val targetRangeLowMgDl: Int = 70,
    val targetRangeHighMgDl: Int = 180,
    val minimumTirPercent: Int = 70,
    val maxHbA1cPercent: Double = 7.5,
    val childMinimumMeasurementsPerDay: Int = 8,
    val continuationMinMonths: Int = 4,
    val continuationMaxMonths: Int = 6,
    val fallbackActivityWindowMonths: Int = 3,
    val recommendedMaxSensorGapHours: Int = 24,
    val rulesVersion: String = "NFZ-continuation-2026-08",
    val sourceNote: String = "Ocena orientacyjna na podstawie ogólnodostępnych warunków i lokalnie zapisanych danych. Wymaga potwierdzenia przez lekarza lub NFZ."
)

internal enum class NfzPatientGroup {
    UNKNOWN,
    ADULT,
    CHILD
}

internal data class NfzPatientProfile(
    val patientGroup: NfzPatientGroup = NfzPatientGroup.UNKNOWN,
    val therapeuticGoalReached: Boolean = false,
    val hbA1cPercent: Double? = null,
    val firstOrderRealizationAt: Instant? = null,
    val profileCompleted: Boolean = false
)

internal enum class NfzCriterionStatus {
    MET,
    NOT_MET,
    UNKNOWN,
    NOT_APPLICABLE
}

internal data class NfzCriterionEvaluation(
    val condition: String,
    val currentValue: String,
    val requiredValue: String,
    val status: NfzCriterionStatus,
    val reason: String,
    val recommendation: String?
)

internal data class NfzRecommendation(
    val priority: Int,
    val text: String
)

internal data class NfzAssessmentMetrics(
    val evaluationStart: Instant,
    val evaluationEnd: Instant,
    val usedFallbackWindow: Boolean,
    val sensorActivityPercent: Double?,
    val dataCoveragePercent: Double?,
    val longestGapWithoutSensorData: Duration?,
    val averageGapBetweenReadings: Duration?,
    val readingsPerDay: Double?,
    val daysWithData: Int,
    val daysWithoutData: Int,
    val tirPercent: Int?,
    val timeBelowRange: Duration?,
    val timeAboveRange: Duration?,
    val gmiPercent: Double?,
    val hbA1cPercent: Double?,
    val totalReadings: Int
)

internal data class NfzAssessment(
    val config: NfzRefundCriteriaConfig,
    val metrics: NfzAssessmentMetrics,
    val criteria: List<NfzCriterionEvaluation>,
    val recommendations: List<NfzRecommendation>,
    val headline: String,
    val details: String
)

internal data class NfzStatusSummaryUi(
    val status: NfzStatus,
    val headline: String,
    val details: String,
    val activityLabel: String,
    val tirLabel: String,
    val hba1cOrGmiLabel: String,
    val longestGapLabel: String?,
    val keyReasons: List<String>,
    val keyRecommendations: List<String>
)

internal fun defaultNfzProfile(): NfzPatientProfile = NfzPatientProfile()

internal fun buildNfzInfoDialogText(config: NfzRefundCriteriaConfig): String =
    "LibreCare może pomóc ocenić wybrane warunki kontynuacji refundacji na podstawie lokalnie zapisanych danych. " +
        "Dla części grup kryteria mogą obejmować aktywność czujnika przez co najmniej ${config.minimumSensorActivityPercent}% czasu oraz kontrolę glikemii, np. TIR ${config.targetRangeLowMgDl}-${config.targetRangeHighMgDl} mg/dL powyżej ${config.minimumTirPercent}%, HbA1c poniżej ${config.maxHbA1cPercent.toString().replace('.', ',')}% albo indywidualne cele terapeutyczne ustalone z lekarzem. " +
        "U dzieci może mieć znaczenie bardzo regularne monitorowanie glikemii, np. co najmniej ${config.childMinimumMeasurementsPerDay} odczytów na dobę. " +
        "Ocena kontynuacji zwykle dotyczy okresu od ${config.continuationMinMonths} do ${config.continuationMaxMonths} miesięcy od realizacji pierwszego zlecenia, a przy brakach technicznych można pomocniczo spojrzeć na ostatnie ${config.fallbackActivityWindowMonths} miesiące danych. " +
        "Zasady mogą się zmieniać. Aplikacja nie jest oficjalnym silnikiem decyzyjnym NFZ i nie zastępuje lekarza."

internal fun assessNfzRefundContinuation(
    history: List<GlucoseHistoryPoint>,
    targetLow: Int,
    targetHigh: Int,
    now: Instant = Instant.now(),
    zoneId: ZoneId = DateTimeFormatterProvider.deviceZoneId(),
    profile: NfzPatientProfile = defaultNfzProfile(),
    config: NfzRefundCriteriaConfig = NfzRefundCriteriaConfig()
): NfzAssessment {
    val metrics = computeNfzAssessmentMetrics(
        history = history,
        targetLow = targetLow,
        targetHigh = targetHigh,
        now = now,
        zoneId = zoneId,
        profile = profile,
        config = config
    )

    val criteria = listOf(
        evaluateSensorActivityCriterion(metrics, config),
        evaluateTirCriterion(metrics, config),
        evaluateHbA1cCriterion(metrics, profile, config),
        evaluateMonitoringFrequencyCriterion(metrics, profile, config),
        evaluateEvaluationPeriodCriterion(metrics, profile, config)
    )

    val recommendations = buildNfzRecommendations(metrics, criteria, profile, config)
    val hasCriticalFailure = criteria.any {
        it.status == NfzCriterionStatus.NOT_MET && it.condition in setOf("Aktywność czujnika", "TIR 70-180 mg/dL", "HbA1c / GMI")
    }
    val hasPositiveSignals = criteria.any { it.status == NfzCriterionStatus.MET }

    val headline = when {
        hasCriticalFailure -> "Na podstawie dostępnych danych wygląda na to, że część warunków kontynuacji refundacji nie jest obecnie spełniona."
        hasPositiveSignals && criteria.none { it.status == NfzCriterionStatus.NOT_MET } -> "Na podstawie dostępnych danych część warunków wygląda na spełnioną, ale wymaga potwierdzenia przez lekarza lub NFZ."
        else -> "Aplikacja nie ma wystarczających danych, aby w pełni ocenić warunki kontynuacji refundacji."
    }

    val details = buildString {
        append("Okres oceny: ")
        append(PolishDateTimeFormatter.formatAbsolute(metrics.evaluationStart, zoneId))
        append(" – ")
        append(PolishDateTimeFormatter.formatAbsolute(metrics.evaluationEnd, zoneId))
        if (metrics.usedFallbackWindow) {
            append(". Zastosowano techniczne okno pomocnicze z ostatnich ")
            append(config.fallbackActivityWindowMonths)
            append(" miesięcy danych.")
        }
        append(" ")
        append(config.sourceNote)
    }

    return NfzAssessment(
        config = config,
        metrics = metrics,
        criteria = criteria,
        recommendations = recommendations,
        headline = headline,
        details = details
    )
}

internal fun computeNfzAssessmentMetrics(
    history: List<GlucoseHistoryPoint>,
    targetLow: Int,
    targetHigh: Int,
    now: Instant,
    zoneId: ZoneId,
    profile: NfzPatientProfile,
    config: NfzRefundCriteriaConfig
): NfzAssessmentMetrics {
    val sorted = history.distinctBy { it.timestamp to it.value }.sortedBy { it.timestamp }
    val evaluationEnd = sorted.lastOrNull()?.timestamp ?: now
    val maxWindow = Duration.ofDays((config.fallbackActivityWindowMonths * 30).toLong())
    val evaluationStart = maxOf(sorted.firstOrNull()?.timestamp ?: evaluationEnd.minus(maxWindow), evaluationEnd.minus(maxWindow))
    val visible = sorted.filter { !it.timestamp.isBefore(evaluationStart) && !it.timestamp.isAfter(evaluationEnd) }
    val usedFallbackWindow = profile.firstOrderRealizationAt == null

    if (visible.size < 2) {
        return NfzAssessmentMetrics(
            evaluationStart = evaluationStart,
            evaluationEnd = evaluationEnd,
            usedFallbackWindow = usedFallbackWindow,
            sensorActivityPercent = null,
            dataCoveragePercent = null,
            longestGapWithoutSensorData = null,
            averageGapBetweenReadings = null,
            readingsPerDay = null,
            daysWithData = visible.map { PolishDateTimeFormatter.dateOf(it.timestamp, zoneId) }.distinct().size,
            daysWithoutData = 0,
            tirPercent = null,
            timeBelowRange = null,
            timeAboveRange = null,
            gmiPercent = null,
            hbA1cPercent = profile.hbA1cPercent,
            totalReadings = visible.size
        )
    }

    val rangeDistribution = GlucoseMetricsCalculator.calculateRangeDistribution(
        readings = visible,
        targetLow = targetLow,
        targetHigh = targetHigh,
        lowCritical = 54,
        highCritical = 250
    )
    val activity = GlucoseMetricsCalculator.calculateSensorActivity(
        readings = visible,
        periodStart = evaluationStart,
        periodEnd = evaluationEnd
    )
    val average = visible.map { it.value }.average()
    val rawGaps = visible.zipWithNext().map { Duration.between(it.first.timestamp, it.second.timestamp) }
    val longestGap = rawGaps.maxOrNull()
    val averageGap = rawGaps
        .takeIf { it.isNotEmpty() }
        ?.map { it.toMinutes() }
        ?.average()
        ?.let { Duration.ofMinutes(it.roundToInt().toLong()) }
    val daysWithData = visible.map { PolishDateTimeFormatter.dateOf(it.timestamp, zoneId) }.distinct().size
    val totalDays = PolishDateTimeFormatter.calendarDaysBetween(evaluationStart, evaluationEnd, zoneId).coerceAtLeast(1)
    val daysWithoutData = (totalDays - daysWithData).coerceAtLeast(0)
    val readingsPerDay = visible.size.toDouble() / totalDays.toDouble()

    return NfzAssessmentMetrics(
        evaluationStart = evaluationStart,
        evaluationEnd = evaluationEnd,
        usedFallbackWindow = usedFallbackWindow,
        sensorActivityPercent = activity.activityPercent,
        dataCoveragePercent = activity.activityPercent,
        longestGapWithoutSensorData = longestGap,
        averageGapBetweenReadings = averageGap,
        readingsPerDay = readingsPerDay,
        daysWithData = daysWithData,
        daysWithoutData = daysWithoutData,
        tirPercent = rangeDistribution.inRangePercent,
        timeBelowRange = rangeDistribution.belowRangeDuration,
        timeAboveRange = rangeDistribution.aboveRangeDuration,
        gmiPercent = average.takeIf { it.isFinite() }?.let(GlucoseMetricsCalculator::calculateGmi),
        hbA1cPercent = profile.hbA1cPercent,
        totalReadings = visible.size
    )
}

private fun evaluateSensorActivityCriterion(
    metrics: NfzAssessmentMetrics,
    config: NfzRefundCriteriaConfig
): NfzCriterionEvaluation {
    val current = metrics.sensorActivityPercent
    return when {
        current == null -> NfzCriterionEvaluation(
            condition = "Aktywność czujnika",
            currentValue = "brak danych",
            requiredValue = "co najmniej ${config.minimumSensorActivityPercent}%",
            status = NfzCriterionStatus.UNKNOWN,
            reason = "Brak wystarczających danych, aby oszacować aktywność czujnika w okresie oceny.",
            recommendation = "Zbieraj dane przez dłuższy okres i sprawdzaj, czy aplikacja regularnie pobiera odczyty."
        )
        current >= config.minimumSensorActivityPercent -> NfzCriterionEvaluation(
            condition = "Aktywność czujnika",
            currentValue = "${current.roundToInt()}%",
            requiredValue = "co najmniej ${config.minimumSensorActivityPercent}%",
            status = NfzCriterionStatus.MET,
            reason = "Aktywność czujnika w danych mieści się powyżej progu pomocniczego.",
            recommendation = null
        )
        else -> NfzCriterionEvaluation(
            condition = "Aktywność czujnika",
            currentValue = "${current.roundToInt()}%",
            requiredValue = "co najmniej ${config.minimumSensorActivityPercent}%",
            status = NfzCriterionStatus.NOT_MET,
            reason = "Aktywność czujnika jest niższa niż wymagany próg. W danych występują długie przerwy bez odczytów.",
            recommendation = "Staraj się utrzymywać ciągłość używania sensorów. Po wymianie sensora aktywuj nowy możliwie szybko. Sprawdź też, czy aplikacja regularnie pobiera dane."
        )
    }
}

private fun evaluateTirCriterion(
    metrics: NfzAssessmentMetrics,
    config: NfzRefundCriteriaConfig
): NfzCriterionEvaluation {
    val tir = metrics.tirPercent
    return when {
        tir == null -> NfzCriterionEvaluation(
            condition = "TIR 70-180 mg/dL",
            currentValue = "brak danych",
            requiredValue = "powyżej ${config.minimumTirPercent}%",
            status = NfzCriterionStatus.UNKNOWN,
            reason = "Brak wystarczających danych do oceny czasu w zakresie ${config.targetRangeLowMgDl}-${config.targetRangeHighMgDl} mg/dL.",
            recommendation = "Zbieraj dane przez dłuższy okres, aby aplikacja mogła policzyć TIR."
        )
        tir >= config.minimumTirPercent -> NfzCriterionEvaluation(
            condition = "TIR 70-180 mg/dL",
            currentValue = "$tir%",
            requiredValue = "powyżej ${config.minimumTirPercent}%",
            status = NfzCriterionStatus.MET,
            reason = "Czas w zakresie ${config.targetRangeLowMgDl}-${config.targetRangeHighMgDl} mg/dL jest na poziomie pomocniczego progu lub wyżej.",
            recommendation = null
        )
        else -> NfzCriterionEvaluation(
            condition = "TIR 70-180 mg/dL",
            currentValue = "$tir%",
            requiredValue = "powyżej ${config.minimumTirPercent}%",
            status = NfzCriterionStatus.NOT_MET,
            reason = "Czas w zakresie ${config.targetRangeLowMgDl}-${config.targetRangeHighMgDl} mg/dL jest poniżej progu.",
            recommendation = "Omów wyniki z lekarzem. Sprawdź okresy podwyższonej lub obniżonej glikemii i możliwe przyczyny."
        )
    }
}

private fun evaluateHbA1cCriterion(
    metrics: NfzAssessmentMetrics,
    profile: NfzPatientProfile,
    config: NfzRefundCriteriaConfig
): NfzCriterionEvaluation {
    val hbA1c = metrics.hbA1cPercent ?: profile.hbA1cPercent
    val gmi = metrics.gmiPercent
    return when {
        profile.therapeuticGoalReached -> NfzCriterionEvaluation(
            condition = "HbA1c / GMI",
            currentValue = "cel indywidualny",
            requiredValue = "HbA1c < ${config.maxHbA1cPercent.toString().replace('.', ',')}% lub cel indywidualny",
            status = NfzCriterionStatus.MET,
            reason = "Profil wskazuje osiągnięcie indywidualnego celu terapeutycznego ustalonego z lekarzem.",
            recommendation = null
        )
        hbA1c != null && hbA1c < config.maxHbA1cPercent -> NfzCriterionEvaluation(
            condition = "HbA1c / GMI",
            currentValue = "HbA1c ${hbA1c.toPolishPercent()}",
            requiredValue = "HbA1c < ${config.maxHbA1cPercent.toString().replace('.', ',')}% lub TIR > ${config.minimumTirPercent}%",
            status = NfzCriterionStatus.MET,
            reason = "Wprowadzony wynik HbA1c mieści się poniżej progu pomocniczego.",
            recommendation = null
        )
        hbA1c != null -> NfzCriterionEvaluation(
            condition = "HbA1c / GMI",
            currentValue = "HbA1c ${hbA1c.toPolishPercent()}",
            requiredValue = "HbA1c < ${config.maxHbA1cPercent.toString().replace('.', ',')}% lub TIR > ${config.minimumTirPercent}%",
            status = NfzCriterionStatus.NOT_MET,
            reason = "Wprowadzony wynik HbA1c jest powyżej progu pomocniczego.",
            recommendation = "Skonsultuj wyniki z lekarzem i oceń, czy potrzebne są zmiany w planie leczenia."
        )
        gmi != null -> NfzCriterionEvaluation(
            condition = "HbA1c / GMI",
            currentValue = "GMI ${gmi.toPolishPercent()}",
            requiredValue = "HbA1c < ${config.maxHbA1cPercent.toString().replace('.', ',')}% lub TIR > ${config.minimumTirPercent}%",
            status = if (gmi < config.maxHbA1cPercent) NfzCriterionStatus.MET else NfzCriterionStatus.NOT_MET,
            reason = if (gmi < config.maxHbA1cPercent) {
                "Szacowany GMI wygląda korzystnie, ale nie zastępuje laboratoryjnego HbA1c."
            } else {
                "Szacowany GMI jest podwyższony. To wskaźnik pomocniczy i nie zastępuje laboratoryjnego HbA1c."
            },
            recommendation = if (gmi < config.maxHbA1cPercent) null else "Jeśli to możliwe, uzupełnij aktualny wynik HbA1c i omów wyniki z lekarzem."
        )
        else -> NfzCriterionEvaluation(
            condition = "HbA1c / GMI",
            currentValue = "brak danych",
            requiredValue = "HbA1c < ${config.maxHbA1cPercent.toString().replace('.', ',')}% lub TIR > ${config.minimumTirPercent}%",
            status = NfzCriterionStatus.UNKNOWN,
            reason = "Brak wystarczających danych do obliczenia GMI albo brak informacji o HbA1c.",
            recommendation = "Zbieraj dane przez dłuższy okres albo uzupełnij wynik HbA1c, jeśli aplikacja obsługuje takie pole."
        )
    }
}

private fun evaluateMonitoringFrequencyCriterion(
    metrics: NfzAssessmentMetrics,
    profile: NfzPatientProfile,
    config: NfzRefundCriteriaConfig
): NfzCriterionEvaluation {
    val readingsPerDay = metrics.readingsPerDay
    return when (profile.patientGroup) {
        NfzPatientGroup.ADULT -> NfzCriterionEvaluation(
            condition = "Regularność monitorowania",
            currentValue = readingsPerDay?.let { "${it.roundToInt()} odczytów/dzień" } ?: "brak danych",
            requiredValue = "dotyczy wybranych grup, np. dzieci: co najmniej ${config.childMinimumMeasurementsPerDay} odczytów/dzień",
            status = NfzCriterionStatus.NOT_APPLICABLE,
            reason = "To kryterium częściej dotyczy wybranych grup pediatrycznych i wymaga potwierdzenia z lekarzem.",
            recommendation = null
        )
        NfzPatientGroup.CHILD -> when {
            readingsPerDay == null -> NfzCriterionEvaluation(
                condition = "Regularność monitorowania",
                currentValue = "brak danych",
                requiredValue = "co najmniej ${config.childMinimumMeasurementsPerDay} odczytów/dzień",
                status = NfzCriterionStatus.UNKNOWN,
                reason = "Brak wystarczających danych do oceny regularności monitorowania.",
                recommendation = "Zwiększ regularność monitorowania i zapisuj odczyty codziennie."
            )
            readingsPerDay >= config.childMinimumMeasurementsPerDay -> NfzCriterionEvaluation(
                condition = "Regularność monitorowania",
                currentValue = "${readingsPerDay.roundToInt()} odczytów/dzień",
                requiredValue = "co najmniej ${config.childMinimumMeasurementsPerDay} odczytów/dzień",
                status = NfzCriterionStatus.MET,
                reason = "Średnia liczba odczytów na dobę wygląda na wystarczającą dla tego kryterium pomocniczego.",
                recommendation = null
            )
            else -> NfzCriterionEvaluation(
                condition = "Regularność monitorowania",
                currentValue = "${readingsPerDay.roundToInt()} odczytów/dzień",
                requiredValue = "co najmniej ${config.childMinimumMeasurementsPerDay} odczytów/dzień",
                status = NfzCriterionStatus.NOT_MET,
                reason = "Średnia liczba odczytów na dobę jest zbyt niska.",
                recommendation = "Zwiększ regularność monitorowania. Upewnij się, że odczyty są zapisywane codziennie."
            )
        }
        NfzPatientGroup.UNKNOWN -> NfzCriterionEvaluation(
            condition = "Regularność monitorowania",
            currentValue = readingsPerDay?.let { "${it.roundToInt()} odczytów/dzień" } ?: "brak danych",
            requiredValue = "dla wybranych grup: co najmniej ${config.childMinimumMeasurementsPerDay} odczytów/dzień",
            status = NfzCriterionStatus.UNKNOWN,
            reason = "Brak profilu pacjenta, więc aplikacja nie wie, czy to kryterium dotyczy tej osoby.",
            recommendation = "Uzupełnij dane pacjenta potrzebne do oceny refundacji."
        )
    }
}

private fun evaluateEvaluationPeriodCriterion(
    metrics: NfzAssessmentMetrics,
    profile: NfzPatientProfile,
    config: NfzRefundCriteriaConfig
): NfzCriterionEvaluation {
    val start = profile.firstOrderRealizationAt
    return if (start == null) {
        NfzCriterionEvaluation(
            condition = "Okres oceny kontynuacji",
            currentValue = "okno techniczne ${config.fallbackActivityWindowMonths} mies.",
            requiredValue = "zwykle ${config.continuationMinMonths}-${config.continuationMaxMonths} mies. od pierwszej realizacji",
            status = NfzCriterionStatus.UNKNOWN,
            reason = "Brak daty pierwszej realizacji zlecenia. Aplikacja pokazuje metryki z technicznego okna danych, ale nie może potwierdzić właściwego okresu kontynuacji.",
            recommendation = "Przed złożeniem zlecenia zweryfikuj aktualne zasady z lekarzem, NFZ lub punktem realizacji zleceń."
        )
    } else {
        val months = Duration.between(start, metrics.evaluationEnd).toDays() / 30.0
        val met = months >= config.continuationMinMonths && months <= config.continuationMaxMonths
        NfzCriterionEvaluation(
            condition = "Okres oceny kontynuacji",
            currentValue = "${months.roundToInt()} mies.",
            requiredValue = "${config.continuationMinMonths}-${config.continuationMaxMonths} mies. od pierwszej realizacji",
            status = if (met) NfzCriterionStatus.MET else NfzCriterionStatus.NOT_MET,
            reason = if (met) "Okres między pierwszą realizacją a końcem ocenianych danych mieści się w pomocniczym przedziale." else "Okres ocenianych danych nie mieści się w pomocniczym przedziale dla kontynuacji.",
            recommendation = if (met) null else "Zweryfikuj datę pierwszej realizacji z lekarzem lub punktem realizacji zlecenia."
        )
    }
}

private fun buildNfzRecommendations(
    metrics: NfzAssessmentMetrics,
    criteria: List<NfzCriterionEvaluation>,
    profile: NfzPatientProfile,
    config: NfzRefundCriteriaConfig
): List<NfzRecommendation> {
    val items = mutableListOf<NfzRecommendation>()
    val tir = metrics.tirPercent
    val activity = metrics.sensorActivityPercent
    val longestGapHours = metrics.longestGapWithoutSensorData?.toHours()

    if ((metrics.timeBelowRange ?: Duration.ZERO) > Duration.ofHours(1)) {
        items += NfzRecommendation(1, "Omów wyniki z lekarzem, szczególnie jeśli często występują wartości poniżej zakresu lub znacznie powyżej zakresu.")
    }
    if (activity == null || activity < config.minimumSensorActivityPercent || (longestGapHours ?: 0L) >= config.recommendedMaxSensorGapHours) {
        items += NfzRecommendation(2, "Utrzymuj wysoką aktywność czujnika. Cel aplikacji: co najmniej ${config.minimumSensorActivityPercent}% pokrycia danych.")
        items += NfzRecommendation(2, "Unikaj długich przerw między sensorami. Po zakończeniu jednego sensora aktywuj kolejny możliwie szybko.")
        items += NfzRecommendation(2, "Sprawdź, czy telefon regularnie synchronizuje dane z LibreLinkUp.")
    }
    if (tir != null && tir < config.minimumTirPercent) {
        items += NfzRecommendation(3, "Przeanalizuj okresy poza zakresem i omów wyniki z lekarzem. Niski TIR może utrudniać ocenę kontynuacji.")
    }
    if ((metrics.timeBelowRange ?: Duration.ZERO) > Duration.ZERO) {
        items += NfzRecommendation(4, "Zwróć uwagę na epizody niskiej glikemii i stosuj plan leczenia hipoglikemii ustalony z lekarzem.")
    }
    if (profile.patientGroup == NfzPatientGroup.CHILD && (metrics.readingsPerDay ?: 0.0) < config.childMinimumMeasurementsPerDay) {
        items += NfzRecommendation(5, "Zwiększ regularność monitorowania. Upewnij się, że odczyty są zapisywane codziennie.")
    }
    if (!profile.profileCompleted || profile.patientGroup == NfzPatientGroup.UNKNOWN) {
        items += NfzRecommendation(6, "Uzupełnij brakujące informacje o pacjencie potrzebne do oceny refundacji.")
    }
    if (profile.firstOrderRealizationAt == null) {
        items += NfzRecommendation(7, "Dodaj datę pierwszej realizacji zlecenia, jeśli ją znasz, aby lepiej ocenić okres kontynuacji.")
    }
    items += NfzRecommendation(8, "Przed złożeniem zlecenia zweryfikuj aktualne zasady z lekarzem, NFZ lub punktem realizacji zleceń.")

    criteria.filter { it.status == NfzCriterionStatus.NOT_MET || it.status == NfzCriterionStatus.UNKNOWN }
        .mapNotNull { it.recommendation }
        .forEach { recommendationText ->
            val priority = items.firstOrNull { it.text == recommendationText }?.priority ?: 6
            items += NfzRecommendation(priority, recommendationText)
        }

    return items.distinctBy { it.text }.sortedWith(compareBy<NfzRecommendation> { it.priority }.thenBy { it.text })
}

internal fun nfzStatusLabel(status: NfzCriterionStatus): String = when (status) {
    NfzCriterionStatus.MET -> "Kryterium spełnione"
    NfzCriterionStatus.NOT_MET -> "Kryterium niespełnione"
    NfzCriterionStatus.UNKNOWN -> "Nie można ocenić"
    NfzCriterionStatus.NOT_APPLICABLE -> "Dotyczy wybranych grup"
}

internal fun NfzAssessment.toStatusSummaryUi(): NfzStatusSummaryUi {
    val criticalConditions = setOf("Aktywność czujnika", "TIR 70-180 mg/dL", "HbA1c / GMI")
    val criticalFailure = criteria.any { it.status == NfzCriterionStatus.NOT_MET && it.condition in criticalConditions }
    val hasUnknowns = criteria.any { it.status == NfzCriterionStatus.UNKNOWN }
    val hasNonCriticalFailures = criteria.any { it.status == NfzCriterionStatus.NOT_MET && it.condition !in criticalConditions }
    val hasMet = criteria.any { it.status == NfzCriterionStatus.MET }
    val overallStatus = when {
        criteria.all { it.status == NfzCriterionStatus.UNKNOWN || it.status == NfzCriterionStatus.NOT_APPLICABLE } -> NfzStatus.GRAY
        criticalFailure -> NfzStatus.RED
        hasUnknowns || hasNonCriticalFailures -> NfzStatus.YELLOW
        hasMet -> NfzStatus.GREEN
        else -> NfzStatus.GRAY
    }

    val prioritizedReasons = criteria
        .sortedBy {
            when (it.status) {
                NfzCriterionStatus.NOT_MET -> 0
                NfzCriterionStatus.UNKNOWN -> 1
                NfzCriterionStatus.MET -> 2
                NfzCriterionStatus.NOT_APPLICABLE -> 3
            }
        }
        .map { "${it.condition}: ${it.reason}" }
        .distinct()
        .take(3)

    val hba1cOrGmiLabel = when {
        metrics.hbA1cPercent != null -> "HbA1c ${metrics.hbA1cPercent.toPolishPercent()}"
        metrics.gmiPercent != null -> "GMI ${metrics.gmiPercent.toPolishPercent()}"
        else -> "brak danych"
    }

    return NfzStatusSummaryUi(
        status = overallStatus,
        headline = headline,
        details = details,
        activityLabel = metrics.sensorActivityPercent?.roundToInt()?.let { "$it%" } ?: "brak danych",
        tirLabel = metrics.tirPercent?.let { "$it%" } ?: "brak danych",
        hba1cOrGmiLabel = hba1cOrGmiLabel,
        longestGapLabel = metrics.longestGapWithoutSensorData?.let(PolishDateTimeFormatter::formatCompactDuration),
        keyReasons = prioritizedReasons,
        keyRecommendations = recommendations.map { it.text }.distinct().take(3)
    )
}

private fun Double.toPolishPercent(): String = "${"%.1f".format(this).replace('.', ',')}%"

