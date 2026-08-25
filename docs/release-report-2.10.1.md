# LibreCare - Release Report 2.10.1

Data: 2026-08-25

## Summary
- Dokończono zakres Analizy o wybór własnego przedziału dat i dotykowe wskazanie danych na wykresach.
- Domknięto UX problem selektora zakresu Home (manual scroll bez automatycznej zmiany zakresu).

## Architecture review
- `DataAnalysisViewModel`: dodano `onCustomRangeSelected` + konwersję zakresu pickera na granice dobowe.
- `DataAnalysisScreen`: dodano `DateRangePickerDialog` i obsługę wskazywania punktów/słupków przez tap.
- `MonitoringScreen`: selektor zakresu Home działa wyłącznie manualnie.

## UI changes
- Nowy przycisk: `Ustaw zakres własny (od-do)` na ekranie Analizy.
- Tooltipy tekstowe pod wykresami po tapnięciu.

## Database changes
- Brak zmian schematu Room.

## Tests
- `DataAnalysisCustomRangeTest` - PASS
- `HomeChartRangeSelectorBehaviorTest` - PASS
- `:app:compileDebugKotlin` - PASS

## Artifacts
- DEBUG APK: `release-artifacts/LibreCare-2.10.1-debug.apk`
- RELEASE APK: `release-artifacts/LibreCare-2.10.1-release.apk`
- RELEASE AAB: `release-artifacts/LibreCare-2.10.1-release.aab`

## Remaining risks
- Tooltipy w Analizie są realizowane jako opis tekstowy pod wykresem; pełne dymki „pod palcem” do dalszego dopracowania.

