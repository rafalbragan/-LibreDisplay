# Quality Review Report — LibreCare 2.13.0

**Data**: 2026-08-27  
**Wersja / Version**: 2.13.0 (versionCode 39)  
**Poprzednia wersja / Previous**: 2.12.0 (versionCode 38)  
**Zakres / Scope**: DEC-0007 Phase 1 — Analysis Screen Enhancements

---

## Podsumowanie / Summary

Pierwszy etap (Phase 1) implementacji DEC-0007 i planu z `DESIGN_SUMMARY.md`. Zmieniono ekran Analiza (zmienna długość okresu nakładki, nawigacja okna, ulepszone obserwacje, sticky tabela metryk). Zakładka Futures była już gotowa w 2.12.0.

---

## Przegląd architektury / Architecture Review

| Komponent | Status |
|-----------|--------|
| `AnalyticsViewModel.kt` | ✅ Zmieniony — nowe pola stanu + 4 nowe funkcje |
| `DataAnalysisUiState` | ✅ Rozszerzony o `analysisPeriodDays`, `canNavigateBackward`, `canNavigateForward`, `metricsScrollOffset`, `overlayRangeLabel` |
| `AnalysisChartFactory.overlayForWindow` | ✅ Bez zmian — parametr `maxDays` już istniał |
| `AnalyticsScreen.kt` | ✅ Zmieniony — kontrolka okresu, dynamiczny tytuł, sticky tabela, ulepszone obserwacje |
| `FuturesScreen.kt` | ✅ EXISTS — bez zmian |
| `AppNavigationState.kt` | ✅ EXISTS — bez zmian |
| `TopLevelNavigationBar.kt` | ✅ EXISTS — bez zmian |
| `MainActivity.kt` | ✅ EXISTS — bez zmian |
| Baza danych / Room | NOT APPLICABLE — brak zmian schematu |
| Migracje / Migrations | NOT APPLICABLE |

---

## Zmiany UI / UI Changes

### AnalyticsScreen

1. **Sekcja kontrolki okresu** (nowa):
   - Nagłówek "📊 WIZUALIZACJA ZAKRESU"
   - Pole tekstowe `OutlinedTextField` dla liczby dni (1–90)
   - 5 przycisków nawigacji: ◀◀ Miesiąc, ◀ Tydzień, Dzisiaj, Tydzień ▶, Miesiąc ▶▶
   - Etykieta zakresu dat: "Zakres: DD.MM — DD.MM (N dni)"

2. **Tytuł wykresu dobowego**:
   - PRZED: "Profil dobowy (nakładka 14 dni)" (hardcoded)
   - PO: "Profil dobowy (nakładka ${state.analysisPeriodDays} dni)" (dynamiczny)

3. **Sekcja obserwacji**:
   - PRZED: Nagłówek "Obserwacje", 13sp bold, treść 12sp
   - PO: Nagłówek "⭐ WNIOSKI & OBSERWACJE", 14sp bold, treść 14sp / lineHeight 20sp
   - Padding zwiększony z 10dp do 12dp

4. **Tabela metryk**:
   - PRZED: Cały `Row` owijał poziome przewijanie (lewa kolumna też się przewijała)
   - PO: Lewa kolumna (112dp) jest stała, prawa część przewija się poziomo
   - Pozycja przewijania zapisywana w stanie ViewModel i przywracana przy powrocie na ekran

---

## Testy / Tests

| Test | Status |
|------|--------|
| `./gradlew clean` | PASS |
| `./gradlew testDebugUnitTest` (491 testów) | PASS (0 błędów) |
| `./gradlew lint` | PASS |
| `./gradlew assembleDebug` | PASS |
| `./gradlew assembleRelease` | PASS |
| `./gradlew bundleRelease` | PASS |
| Testy connected | No connected device/emulator available |

### Nowe testy jednostkowe

- `AnalysisChartFactoryTest.overlayForWindow_7days_limitsToSevenDayLines` ✅
- `AnalysisChartFactoryTest.overlayForWindow_30days_acceptsFullRange` ✅
- `AnalysisChartFactoryTest.overlayForWindow_emptyReadings_returnsEmpty` ✅
- `AnalysisChartFactoryTest.maxDailyOffset_withShortPeriod_returnsLargerMax` ✅

---

## Artefakty / Artifacts

| Plik | Rozmiar |
|------|---------|
| `release-artifacts/LibreCare-2.13.0-debug.apk` | 22,77 MB |
| `release-artifacts/LibreCare-2.13.0-release.apk` | 3,52 MB |
| `release-artifacts/LibreCare-2.13.0-release.aab` | 6,39 MB |

**Google Play Upload File**: `release-artifacts/LibreCare-2.13.0-release.aab` (6,39 MB)

---

## Ryzyka / Remaining Risks

1. **Brak testów ViewModel** — `DataAnalysisViewModel` nie ma dedykowanych testów jednostkowych (wymaga `AndroidViewModel` / Robolectric). Logika clamping i nawigacji jest pośrednio pokryta przez testy `AnalysisChartFactory`.
2. **Brak testów connected** — Brak urządzenia/emulatora do weryfikacji UI na żywo.
3. **OutlinedTextField height** — Na niektórych motywach `OutlinedTextField` może mieć niestandardowe padding; nie zweryfikowano na wszystkich konfiguracjach.
4. **Wydajność nakładki 90 dni** — Nakładka dla 90 dni może być wolna na urządzeniach low-end (obliczenia w `Dispatchers.Default`, ale nie testowano z pełnym buforem 90-dniowym na fizycznym urządzeniu).

---

## Branding Check

- Brak nowych referencji "LibreDisplay" w zmodyfikowanych plikach.
- Zakres zmian: ViewModel + Screen w pakiecie `com.libredisplay` (package name — legacyowe, nie user-facing).

