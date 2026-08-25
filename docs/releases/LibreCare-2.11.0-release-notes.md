# LibreCare 2.11.0 - Release Notes

Data wydania / Release date: 2026-08-25

## PL

### Wersja
- `versionName`: `2.11.0`
- `versionCode`: `35`

### Nowe funkcje
- Menu `Historia` → `Analiza` z nową ikoną (wykres słupkowy).
- Wykres słupkowy rozkładu czasu w zakresie: 14 słupków dziennych, podpisane osie i legenda, dotknięcie słupka pokazuje szczegóły, przewijanie w czasie oraz etykieta zakresu dat.
- Widok miesięczny (12 słupków) po zebraniu wystarczających danych.
- Nakładka profilu dobowego: 14 cienkich linii dziennych + gruba linia średniej, oś godzin i pasmo docelowe, aktualizacja przy przewijaniu.
- Automatyczne obserwacje trendów dobowych.

### Ulepszenia
- Eksport `.xlsx` respektuje wybrany zakres własny.

### Testy
- `AnalysisChartFactoryTest`
- `AnalysisTrendInterpreterTest`
- `./gradlew testDebugUnitTest` — PASS

### Artefakty
- DEBUG APK: `release-artifacts/LibreCare-2.11.0-debug.apk`
- RELEASE APK: `release-artifacts/LibreCare-2.11.0-release.apk`
- RELEASE AAB: `release-artifacts/LibreCare-2.11.0-release.aab`
- Google Play upload file: `release-artifacts/LibreCare-2.11.0-release.aab`

### Znane ograniczenia
- Obserwacje trendów opierają się na danych z aktualnie widocznego okna (do 14 dni). W widoku miesięcznym nakładka pokazuje ostatnie 14 dni widocznego zakresu.

## EN

### Version
- `versionName`: `2.11.0`
- `versionCode`: `35`

### New features
- Bottom nav `Historia` → `Analiza` with a new bar-chart icon.
- Time-in-range bar chart: 14 daily bars, labeled axes + legend, tap-to-inspect, time scrolling, and a live date-range label.
- Monthly view (12 bars) once enough data is collected.
- Day-profile overlay: 14 thin daily lines + a thick average line, hour axis + target band, updates while scrolling.
- Automatic day-profile trend observations.

### Improvements
- `.xlsx` export honours the selected custom range.

### Tests
- `AnalysisChartFactoryTest`
- `AnalysisTrendInterpreterTest`
- `./gradlew testDebugUnitTest` — PASS

### Artifacts
- DEBUG APK: `release-artifacts/LibreCare-2.11.0-debug.apk`
- RELEASE APK: `release-artifacts/LibreCare-2.11.0-release.apk`
- RELEASE AAB: `release-artifacts/LibreCare-2.11.0-release.aab`
- Google Play upload file: `release-artifacts/LibreCare-2.11.0-release.aab`

### Known limitations
- Trend observations are based on the currently visible window (up to 14 days). In monthly view the overlay shows the most recent 14 days of the visible range.

