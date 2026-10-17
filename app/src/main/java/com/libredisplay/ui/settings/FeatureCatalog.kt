package com.libredisplay.ui.settings

import com.libredisplay.BuildConfig

enum class FeatureStatus(val labelPl: String) {
    PLANNED("PLANOWANE"),
    IN_DEVELOPMENT("W ROZWOJU"),
    FOR_TESTING("DO TESTOW"),
    DEPLOYED("WDROZONE"),
    ON_HOLD("WSTRZYMANE")
}

data class FeatureRecord(
    val name: String,
    val description: String,
    val status: FeatureStatus,
    val targetVersion: String,
    val references: List<String>,
    val updatedAt: String
)

data class VersionHistoryRecord(
    val versionName: String,
    val versionCode: Int,
    val releaseDate: String,
    val releaseType: String,
    val newFeatures: List<String>,
    val fixedIssues: List<String>,
    val improvements: List<String>,
    val knownLimitations: List<String>
)

object FeatureCatalog {

    fun featureRecords(): List<FeatureRecord> = listOf(
        FeatureRecord(
            name = "Chart 2.0: gesty i nawigacja",
            description = "Pan, pinch, double-tap, mini-nawigator i przycisk pelnego ekranu na Home oraz duzym wykresie.",
            status = FeatureStatus.FOR_TESTING,
            targetVersion = "2.16.3",
            references = listOf("REQ-0004", "PR #38"),
            updatedAt = "2026-10-17"
        ),
        FeatureRecord(
            name = "Stabilizacja nieaktywnej osoby (#29)",
            description = "Osłona przed prezentowaniem nieaktualnego pomiaru jako biezacego + dalsza diagnostyka crasha.",
            status = FeatureStatus.FOR_TESTING,
            targetVersion = "2.16.3",
            references = listOf("BUG #29", "PR #35"),
            updatedAt = "2026-10-17"
        ),
        FeatureRecord(
            name = "Stabilizacja synchronizacji CONNECTIONS (#30)",
            description = "Cache TTL, ochrona sesji i redukcja nadmiarowych zapytan przy HTTP 429.",
            status = FeatureStatus.FOR_TESTING,
            targetVersion = "2.16.3",
            references = listOf("BUG #30", "PR #36"),
            updatedAt = "2026-10-17"
        ),
        FeatureRecord(
            name = "Eksport diagnostyki do pliku",
            description = "Akcja 'Zapisz log do pliku' przez SAF z redakcja danych wrazliwych.",
            status = FeatureStatus.FOR_TESTING,
            targetVersion = "2.16.3",
            references = listOf("REQ-DIAG-EXPORT"),
            updatedAt = "2026-10-17"
        ),
        FeatureRecord(
            name = "Alarmy #28",
            description = "Zmiany zachowania alarmow pozostaja poza obecnym zakresem.",
            status = FeatureStatus.ON_HOLD,
            targetVersion = "n/d",
            references = listOf("BUG #28", "PO HOLD"),
            updatedAt = "2026-10-17"
        )
    )

    fun versionHistory(): List<VersionHistoryRecord> = listOf(
        VersionHistoryRecord(
            versionName = BuildConfig.VERSION_NAME,
            versionCode = BuildConfig.VERSION_CODE,
            releaseDate = "2026-10-17",
            releaseType = BuildConfig.BUILD_CHANNEL,
            newFeatures = listOf(
                "Chart 2.0: pan/pinch/double-tap + nawigacja przyciskami",
                "Dedykowany ekran funkcji i historii wersji",
                "Zapis logu diagnostycznego do pliku (SAF)"
            ),
            fixedIssues = listOf(
                "Lepsza ochrona stale/no-data dla wybranej osoby",
                "Redakcja danych wrazliwych w eksporcie diagnostyki"
            ),
            improvements = listOf(
                "Raport ostatniego crasha dostepny po ponownym uruchomieniu",
                "Metadane builda: commit SHA i kanal"
            ),
            knownLimitations = listOf(
                "Wymagana reczna walidacja gestow na urzadzeniu",
                "Issue #29 wymaga dalszej reprodukcji logcat dla finalnego potwierdzenia"
            )
        ),
        VersionHistoryRecord(
            versionName = "2.16.2",
            versionCode = 46,
            releaseDate = "2026-10-16",
            releaseType = "TEST",
            newFeatures = listOf("Rozszerzone zakresy czasu i kontekst osi czasu"),
            fixedIssues = listOf("Podstawowe poprawki regresji #26/#27"),
            improvements = listOf("Dodatkowe testy stabilnosci"),
            knownLimitations = listOf("Niespojna obsluga gestow Home wymagajaca dopracowania")
        )
    )
}

