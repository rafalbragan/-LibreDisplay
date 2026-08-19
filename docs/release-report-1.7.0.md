# Release Report – LibreCare 1.7.0

**Data:** 2026-08-19  
**Wersja:** 1.7.0 (versionCode 11)  
**Poprzednia wersja:** 1.6.0 (versionCode 10)

---

## Streszczenie

Iteracja UI skupiona na trzech konkretnych problemach zgłoszonych przez użytkownika:
1. Nadmiar dużych zaokrąglonych kart i kontenerów.
2. Mylenie wybranego zakresu czasu z faktycznie dostępnymi danymi.
3. Niejasny komunikat o zużyciu baterii.

Nie zmieniano logiki pobierania danych ani ViewModeli.

---

## Przegląd architektury

### Zmienione pliki

| Plik | Opis zmiany |
|------|-------------|
| `DataCoverageModel.kt` | NOWY – centralny model coverage |
| `PolishFormatters.kt` | Dodano `formatNaturalDuration()` |
| `HistoryUiModels.kt` | `historyStatsSection()` – coverage-aware tytuł |
| `MonitoringScreen.kt` | Usunięto Card z metrics i chart, dodano coverage info |
| `RedesignedMetrics.kt` | Usunięto Card z `ImprovedQuickMetricsPanel` |
| `RedesignedGlucoseCard.kt` | Zmniejszono corner radius 20→14dp |
| `DashboardComponents.kt` | Uproszczono `TimeRangeDisplay` |
| `FullScreenGlucoseChartScreen.kt` | Coverage-aware labels i stats title |
| `PollingFrequencyScreen.kt` | Usunięto battery warning card |
| `strings.xml` | Zaktualizowano `polling_battery_warning` |
| `build.gradle.kts` | versionCode 10→11, versionName 1.6.0→1.7.0 |

### Nie zmieniane (zachowano działanie)

- `MonitoringViewModel.kt` – brak zmian
- `GlucoseChart.kt` – brak zmian
- `NfzAssessment.kt` – brak zmian
- Baza danych / Room – brak zmian (migracja nie wymagana)
- Logika pollingu – brak zmian

---

## Zmiany UI

### Usunięte Card/Surface wrappers

1. `ImprovedQuickMetricsPanel` – usunięto `Card(RoundedCornerShape(16dp))` wrapper
2. `GlucoseChartCard` – usunięto `Card(RoundedCornerShape(18dp))` wrapper
3. `TimeRangeDisplay` – usunięto `Surface(RoundedCornerShape(16dp))` wrapper
4. Battery warning card w `PollingFrequencyScreen` – usunięto

Łącznie: **4 usunięte duże Card/Surface containers**.

### Zmniejszone corner radii

- `RedesignedCurrentGlucoseCard`: 20dp → 14dp
- `NfzStatusCompactCard`: 18dp → 12dp
- `MedicalAlertInline`: 12dp → 8dp

### Nowa logika coverage

```
DataCoverageModel:
  selectedRange: Duration (np. PT24H)
  selectedRangeLabel: "24 godz."
  availableSpan: Duration (np. PT8H2M)
  hasFullCoverage: Boolean
  timeUntilFullCoverage: Duration? (np. PT15H58M)

sectionHeaderLabel:
  hasFullCoverage=true  → "24 godz."
  hasFullCoverage=false → "8 godz. 02 min dostępnych danych"

selectedRangeNote:
  hasFullCoverage=true  → null
  hasFullCoverage=false → "Wybrany zakres: 24 godz."

fullCoverageEstimate:
  hasFullCoverage=true  → null
  hasFullCoverage=false → "Przy ciągłym zapisie pełny zakres 24 godz. będzie dostępny za ok. 15 godz. 58 min."
```

---

## Zmiany bazy danych

Brak. Migracja nie jest wymagana.

---

## Testy

| Test | Wynik |
|------|-------|
| `./gradlew testDebugUnitTest` | **PASS** (274 testów) |
| `./gradlew lint` | **PASS** |
| `./gradlew assembleDebug` | **PASS** |
| `./gradlew assembleRelease` | **PASS** |
| `./gradlew bundleRelease` | **PASS** |

### Nowe testy

- `DataCoverageModelTest` (8 przypadków):
  - Empty history coverage
  - Partial data → no full coverage
  - Full data → has full coverage
  - sectionHeaderLabel full/partial coverage
  - formatNaturalDuration correctness
  - TimeRange.toSelectedRangeLabel all values

- `HistoryUiModelsTest` (2 nowe przypadki):
  - Partial coverage → uses available span in title
  - Full coverage → uses selected range label

---

## Artefakty

| Typ | Ścieżka | Rozmiar |
|-----|---------|---------|
| **Debug APK** | `release-artifacts/LibreCare-1.7.0-debug.apk` | 22 657 KB |
| **Release APK** | `release-artifacts/LibreCare-1.7.0-release.apk` | 2 984 KB |
| **Release AAB** | `release-artifacts/LibreCare-1.7.0-release.aab` | 5 483 KB |

**Google Play Upload File:** `release-artifacts/LibreCare-1.7.0-release.aab`

---

## Weryfikacja brandingu

Przeszukano: `LibreDisplay`, `LIBREDISPLAY`, `libredisplay` w plikach user-facing.
- Aplikacja ID: `com.libredisplay` – legacy, nie zmieniano (applicationId)
- Nazwa aplikacji w UI: `LibreCare` (strings.xml) – poprawne
- Nowe pliki używają wyłącznie nazwy `LibreCare` w komentarzach i dokumentacji

---

## Pozostałe ryzyka

1. **Gap detection** – aplikacja nie wykrywa przerw w danych; `availableSpan` to prosty span od oldest do newest, może zawierać luki. Jeżeli dane mają luki, coverage może być przeszacowany.
2. **timeUntilFullCoverage** – szacunek zakładający ciągłe zbieranie. Jeśli synchronizacja jest przerywana, czas będzie niedokładny. Komunikat to wyraźnie zaznacza: „Przy ciągłym zapisie…"
3. **Reorder mode bez przycisku** – w `ImprovedQuickMetricsPanel` usunięto przycisk „Zmień kolejność" (był w nagłówku usuniętej Card). Tryb reorder nadal działa przez long press.
4. **Connected tests** – nie uruchomiono (brak podłączonego urządzenia/emulatora).

