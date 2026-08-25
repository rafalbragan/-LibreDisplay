# LibreCare 2.10.1 - Release Notes

Data wydania / Release date: 2026-08-25

## PL

### Wersja
- `versionName`: `2.10.1`
- `versionCode`: `34`

### Nowe funkcje
- Ekran `Analiza danych`: picker zakresu własnego `od-do`.
- Ekran `Analiza danych`: dotykowe wskazanie (tooltip tekstowy) na wykresie tygodniowym i nakładce 14 dni.

### Poprawki
- Selektor zakresu na Home nie przełącza się sam podczas scrollowania; większe zakresy są osiągalne ręcznie.

### Ulepszenia
- Eksport do Excela (`.xlsx`) respektuje wybrany zakres własny.

### Testy
- `DataAnalysisCustomRangeTest`
- `HomeChartRangeSelectorBehaviorTest`
- `./gradlew :app:compileDebugKotlin`

### Artefakty
- DEBUG APK: `release-artifacts/LibreCare-2.10.1-debug.apk`
- RELEASE APK: `release-artifacts/LibreCare-2.10.1-release.apk`
- RELEASE AAB: `release-artifacts/LibreCare-2.10.1-release.aab`
- Google Play upload file: `release-artifacts/LibreCare-2.10.1-release.aab`

### Znane ograniczenia
- Tooltipy analityczne są obecnie tekstowe; pełne dymki z pozycjonowaniem pod palcem będą dalej rozwijane.

## EN

### Version
- `versionName`: `2.10.1`
- `versionCode`: `34`

### New features
- `Data analysis` screen: custom `from-to` date range picker.
- `Data analysis` screen: tap-based textual tooltip feedback for weekly and 14-day charts.

### Fixes
- Home range selector no longer changes range implicitly while scrolling; larger ranges are reachable via manual scrolling.

### Improvements
- Excel (`.xlsx`) export now respects selected custom range.

### Tests
- `DataAnalysisCustomRangeTest`
- `HomeChartRangeSelectorBehaviorTest`
- `./gradlew :app:compileDebugKotlin`

### Artifacts
- DEBUG APK: `release-artifacts/LibreCare-2.10.1-debug.apk`
- RELEASE APK: `release-artifacts/LibreCare-2.10.1-release.apk`
- RELEASE AAB: `release-artifacts/LibreCare-2.10.1-release.aab`
- Google Play upload file: `release-artifacts/LibreCare-2.10.1-release.aab`

### Known limitations
- Analytical tooltips are currently textual; full bubble positioning under finger will be expanded in follow-up iterations.

