# LibreCare - Release Report 2.10.0

Data: 2026-08-25

## Summary
- Wdrożono ekran `Analiza danych` z metrykami wielookresowymi oraz wykresami analitycznymi.
- Dodano eksport surowych danych glikemii do pliku `.xlsx` i udostępnianie przez systemowy share sheet.
- Naprawiono zachowanie selektora zakresu Home (manualne przewijanie, brak automatycznej zmiany zakresu podczas scrolla).

## Architecture review
- **ViewModels**: `DataAnalysisViewModel` rozszerzony o model wykresów (`weeklyBars`, `overlay14Days`) i zdarzenie eksportu.
- **Repository layer**: wykorzystano istniejące API `LocalGlucoseHistoryRepository` (`loadHistory`, `loadStoredRange`) bez zmian kontraktów.
- **Room/migrations**: brak zmian schematu, migracje nie były wymagane.
- **Navigation**: ekran `AppScreen.Analytics` przepięty na `DataAnalysisScreen`.
- **Privacy & Data**: eksport realizowany lokalnie do `files/exports/` i udostępniany przez `FileProvider`.

## UI changes
- `DataAnalysisScreen`:
  - tabela metryk okresowych,
  - sekcja wykresu tygodniowego stacked,
  - sekcja nakładki 14 dni,
  - filtr `Cały dzień / Tylko nocne`,
  - akcja `Eksportuj surowe dane do Excela`.
- `HomeChartRangeSelector`:
  - usunięto automatyczne przewijanie do zaznaczonego chipa,
  - przewijanie jest manualne i nie przełącza zakresu samoczynnie.

## Database changes
- Brak zmian w modelu danych Room.
- Brak migracji.

## Tests
- `AnalysisMetricsFactoryTest` - PASS
- `AnalysisChartFactoryTest` - PASS
- `RawDataExcelExporterTest` - PASS
- `HomeChartRangeSelectorBehaviorTest` - PASS
- `:app:compileDebugKotlin` - PASS

## Artifacts
- DEBUG APK: `release-artifacts/LibreCare-2.10.0-debug.apk`
- RELEASE APK: `release-artifacts/LibreCare-2.10.0-release.apk`
- RELEASE AAB: `release-artifacts/LibreCare-2.10.0-release.aab`
- Google Play upload file: `release-artifacts/LibreCare-2.10.0-release.aab`

## Remaining risks
- Wykresy analityczne w tej iteracji nie mają jeszcze pełnego tooltipu i nawigacji dotykowej do pełnej historii.
- Własny zakres w analizie jest obecnie oparty o pełny lokalny zakres, bez osobnego pickera dat `od-do`.

