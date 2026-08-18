# LibreCare Release Notes v1.4.1

**Version:** 1.4.1 (versionCode 7)  
**Release Date:** 2026-08-18  
**Branch:** master  
**Git Commit:** 76e529c

---

## PL

### Najwazniejsze zmiany
- Ujednolicono formatowanie czasu i duration w dashboardzie, wykresie i historii pelnoekranowej.
- Dodano centralny model podsumowania NFZ z powodami, metrykami i najwazniejszymi zaleceniami.
- Dodano podglad punktu wykresu podczas tap/drag bez utraty szybkiego przejscia do historii.
- Uporzadkowano warning UI tak, aby przeterminowane dane nie ukrywaly pilnych stanow klinicznych.

### Funkcje i UX
- **Lokalny czas urzadzenia:**
  - Dashboard, tooltipy wykresu i historia pelnoekranowa korzystaja z jednego formattera i jednej strefy czasu.
  - Kompaktowe etykiety osi X dostosowuja sie do zakresu (`HH:mm`, `dd.MM HH:mm`, `dd.MM`).
- **Wykres glikemii:**
  - Drag po wykresie zaznacza najblizszy punkt i pokazuje tooltip na samym wykresie.
  - Tap blisko punktu wybiera pomiar.
  - Tap poza punktami nadal otwiera pelny ekran historii.
- **Pelny ekran historii:**
  - Startuje z najnowszym punktem jako domyslnie zaznaczonym.
  - Pokazuje spójny lokalny czas dla osi, tooltipow oraz kart Min/Max.
- **NFZ:**
  - Karta refundacji korzysta z centralnego podsumowania statusu (`GREEN/YELLOW/RED/GRAY`).
  - Pokazuje aktywnosc czujnika, TIR, HbA1c/GMI, najdluzsza przerwe, kluczowe powody i top zalecenia.
- **Warning UI:**
  - Swiezosc danych jest widoczna, ale nie maskuje stanów `URGENT`/`CRITICAL`.
  - Zachowane zostaje rozroznienie miedzy `0m` a `brak danych`.

### Zmiany techniczne
- Dodano/rozszerzono:
  - `PolishDateTimeFormatter` i `DateTimeFormatterProvider`
  - `readingTimeline(...)` do wspolnej osi czasu dla metryk, NFZ i wykresu
  - `NfzStatusSummaryUi` oraz mapowanie `NfzAssessment.toStatusSummaryUi()`
  - `NearestHistoryPointMatch` dla lepszego rozpoznawania tap/drag na wykresie
- Zaktualizowano:
  - `MonitoringScreen.kt`
  - `GlucoseChart.kt`
  - `FullScreenGlucoseChartScreen.kt`
  - `DashboardUiLogic.kt`
  - `TimeRangeState.kt`
  - testy formatterow, warningow, dashboardu i layoutu wykresu

### Testy i walidacja
- `./gradlew testDebugUnitTest` - PASS
- `./gradlew lint` - PASS
- `./gradlew assembleDebug` - PASS
- `./gradlew assembleRelease` - PASS
- `./gradlew bundleRelease` - PASS

### Artefakty
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk` (23,5 MiB)
- Release APK: `app/build/outputs/apk/release/app-release.apk` (2,9 MiB)
- Release AAB: `app/build/outputs/bundle/release/app-release.aab` (5,2 MiB)

### Ograniczenia / uwagi
- Karta NFZ ma charakter informacyjny i nadal nie zastępuje decyzji lekarza, NFZ ani punktu realizacji zlecen.
- Connected Android tests nie byly wymagane do tego zakresu i nie byly uruchamiane w tej walidacji.

---

## EN

### Highlights
- Unified time and duration formatting across the dashboard, chart, and full-screen history.
- Added a central NFZ status summary model with reasons, metrics, and top recommendations.
- Added point-preview interactions on chart tap/drag while preserving quick navigation to history.
- Refined warning UI so stale data does not hide urgent clinical states.

### UX and behavior
- **Local device time:** dashboard labels, chart tooltips, and full-screen history now use one formatter and one device zone.
- **Chart interactions:** dragging selects the nearest point and shows an in-chart tooltip; tapping near a point selects it; tapping the background still opens full-screen history.
- **Full-screen history:** now defaults to the latest point and keeps timestamps consistent across axis labels, tooltips, and Min/Max cards.
- **NFZ card:** now uses a centralized summary state and surfaces activity, TIR, HbA1c/GMI, longest gap, key reasons, and top recommendations.
- **Warning UI:** stale-data messaging remains visible without suppressing urgent/critical glucose warnings.

### Validation
- `./gradlew testDebugUnitTest` - PASS
- `./gradlew lint` - PASS
- `./gradlew assembleDebug` - PASS
- `./gradlew assembleRelease` - PASS
- `./gradlew bundleRelease` - PASS

### Artifacts
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk` (23.5 MiB)
- Release APK: `app/build/outputs/apk/release/app-release.apk` (2.9 MiB)
- Release AAB: `app/build/outputs/bundle/release/app-release.aab` (5.2 MiB)

### Notes
- The NFZ section remains informational and does not replace medical, payer, or dispensing-point decisions.
- Connected Android tests were not part of this validation scope.

