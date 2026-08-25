# LibreCare - Release Report 2.11.0

Data: 2026-08-25

## Summary
- Przebudowano ekran Analizy: nowy wykres słupkowy (14 dni / 12 miesięcy) z osiami, legendą, dotykowym podglądem i przewijaniem w czasie.
- Przebudowano nakładkę dobową (14 cienkich linii + gruba średnia) z osiami i pasmem docelowym, aktualizowaną przy przewijaniu słupków.
- Dodano automatyczne obserwacje trendów dobowych.
- Zmieniono etykietę i ikonę menu `Historia` → `Analiza`.

## Architecture review
- **ViewModels**: `DataAnalysisViewModel` — bufor w pamięci (≤400 dni), tryb słupków (DAILY/MONTHLY), offsety przewijania, `barWindow`, `overlay`, `trendObservations`, `monthlyAvailable`, `selectedBarIndex`.
- **Domain**: `AnalysisChartFactory` — `dailyWindow`, `monthlyWindow`, `overlayForWindow`, `hasMonthlyData`, `maxDailyOffset`, `maxMonthlyOffset`. Nowy `AnalysisTrendInterpreter`.
- **Repository**: bez zmian kontraktów (`loadHistory`, `loadStoredRange`).
- **Room/migrations**: brak zmian, migracje niepotrzebne.
- **Navigation**: `TopLevelNavigationBar` — label i ikona.

## UI changes
- `RangeBarChart`: osie 0–100%, legenda, tap-select + drag-scroll, przyciski `‹ Starsze` / `Nowsze ›`, przełącznik Dzień/Miesiąc, etykieta zakresu dat, panel szczegółów słupka.
- `OverlayLineChart`: oś godzin 00–24, oś mg/dL, pasmo docelowe, 14 cienkich linii + gruba średnia.
- Sekcja `Obserwacje` z tekstowymi trendami.

## Database changes
- Brak zmian schematu Room, brak migracji.

## Tests
- `AnalysisChartFactoryTest` — PASS
- `AnalysisTrendInterpreterTest` — PASS
- `:app:testDebugUnitTest` — PASS
- `:app:compileDebugKotlin` — PASS

## Artifacts
- DEBUG APK: `release-artifacts/LibreCare-2.11.0-debug.apk`
- RELEASE APK: `release-artifacts/LibreCare-2.11.0-release.apk`
- RELEASE AAB: `release-artifacts/LibreCare-2.11.0-release.aab`
- Google Play upload file: `release-artifacts/LibreCare-2.11.0-release.aab`

## Remaining risks
- Bufor analizy ograniczony do ~400 dni; przewijanie miesięczne poza ten zakres jest wyłączone (canScrollOlder=false).
- Nakładka w widoku miesięcznym pokazuje ostatnie 14 dni widocznego zakresu (świadome uproszczenie dla czytelności).

