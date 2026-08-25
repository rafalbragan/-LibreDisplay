# LibreCare 2.10.0 - Release Notes

Data wydania / Release date: 2026-08-25

## PL

### Wersja
- `versionName`: `2.10.0`
- `versionCode`: `33`

### Nowe funkcje
- Ekran `Analiza danych` z tabelą metryk dla okresów `1g / 3g / 6g / 24g / 7d / 30d / Własny`.
- Tygodniowy wykres stacked (poniżej / w zakresie / powyżej) z filtrem `Tylko nocne`.
- Nakładka 14 dni (cienkie linie dzienne + gruba linia średniej minutowej).
- Eksport surowych danych do Excela (`.xlsx`) i udostępnianie pliku.

### Poprawki
- Naprawiono zachowanie selektora zakresu `1g / 3g / ...` na ekranie głównym: przewijanie jest manualne i nie przełącza zakresu automatycznie.
- Poprawiono dostępność większych zakresów w orientacji poziomej i wąskich szerokościach.

### Ulepszenia
- Ujednolicone modele metryk okresowych (TIR, średnia, CV, GMI, min/max, epizody, aktywność sensora).
- Dodane testy jednostkowe dla logiki analizy, wykresów i eksportu.

### Testy
- `AnalysisMetricsFactoryTest`
- `AnalysisChartFactoryTest`
- `RawDataExcelExporterTest`
- `HomeChartRangeSelectorBehaviorTest`
- `./gradlew :app:compileDebugKotlin`
- `./gradlew :app:testDebugUnitTest --tests ...`

### Artefakty
- DEBUG APK: `release-artifacts/LibreCare-2.10.0-debug.apk`
- RELEASE APK: `release-artifacts/LibreCare-2.10.0-release.apk`
- RELEASE AAB: `release-artifacts/LibreCare-2.10.0-release.aab`
- Google Play upload file: `release-artifacts/LibreCare-2.10.0-release.aab`

### Znane ograniczenia
- W tej wersji selektor okresu `Własny` na ekranie Analizy korzysta z pełnego lokalnego zakresu danych; dedykowany picker dat będzie rozszerzany w kolejnych iteracjach.

## EN

### Version
- `versionName`: `2.10.0`
- `versionCode`: `33`

### New features
- `Data analysis` screen with metrics table for `1h / 3h / 6h / 24h / 7d / 30d / Custom`.
- Weekly stacked range chart (below / in range / above) with `Night only` filter.
- 14-day overlay (thin daily lines + thick minute-average line).
- Raw data export to Excel (`.xlsx`) with file sharing.

### Fixes
- Fixed Home range selector (`1h / 3h / ...`) behavior: scrolling is manual and does not auto-switch the selected range.
- Improved access to larger ranges on landscape and narrow widths.

### Improvements
- Unified period metrics model (TIR, average, CV, GMI, min/max, episodes, sensor activity).
- Added unit tests for analysis, chart, and export logic.

### Tests
- `AnalysisMetricsFactoryTest`
- `AnalysisChartFactoryTest`
- `RawDataExcelExporterTest`
- `HomeChartRangeSelectorBehaviorTest`
- `./gradlew :app:compileDebugKotlin`
- `./gradlew :app:testDebugUnitTest --tests ...`

### Artifacts
- DEBUG APK: `release-artifacts/LibreCare-2.10.0-debug.apk`
- RELEASE APK: `release-artifacts/LibreCare-2.10.0-release.apk`
- RELEASE AAB: `release-artifacts/LibreCare-2.10.0-release.aab`
- Google Play upload file: `release-artifacts/LibreCare-2.10.0-release.aab`

### Known limitations
- In this version, the `Custom` period on the Analysis screen uses the full local data range; dedicated date pickers will be extended in follow-up iterations.

