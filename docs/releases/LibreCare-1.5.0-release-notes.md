# LibreCare Release Notes v1.5.0

**Version:** 1.5.0 (versionCode 9)  
**Release Date:** 2026-08-18  
**Branch:** master  
**Git Commit:** 76e529c

---

## PL

### Najwazniejsze zmiany
- Dashboard LibreCare zostal przeprojektowany zgodnie z kierunkiem makiety: ciemny medyczny layout, wyrazniejsza hierarchia kart i bardziej nowoczesny shell.
- Dodano kompaktowy selector osob, kompaktowy wiersz zakresu danych, mocniejsza karte aktualnej glikemii i czytelniejsza karte ostrzezen.
- Przebudowano preview wykresu oraz pelnoekranowy ekran historii glikemii z chipsami zakresu, legenda, statystykami i placeholderem notatek.
- Dodano dolna nawigacje oraz uporzadkowano palete kolorow i strefe czasu dla glownych timestampow.

### Nowe funkcje
- **Top bar zgodny z mockupem**:
  - LibreCare po lewej stronie
  - ikony: Odswiez, Historia glikemii, Ustawienia
  - bez dropdownu osoby w top barze
- **Selector osob**:
  - kompaktowe chipy z podswietleniem wybranej osoby
  - przewijanie poziome przy wiekszej liczbie osob
- **Karta aktualnej glikemii**:
  - dominujacy wynik mg/dL
  - wyrazny trend i status
  - osobna karta warningow pod glikemia
  - badge swiezosci danych
- **KPI cards**:
  - kompaktowy poziomy uklad
  - brak agresywnego obcinania kluczowych wartosci
  - zachowane rozroznienie `0m` vs `brak danych`
- **Historia glikemii**:
  - chipsy zakresu: 3h, 6h, 12h, 24h, 7 dni, 30 dni, 90 dni, 365 dni
  - czytelniejszy wykres z pasmami ryzyka i tooltipem nad punktem
  - legenda zakresow z czasem i procentem
  - sekcja `Statystyki`
  - placeholder `Notatki i zdarzenia`
- **NFZ**:
  - karta dashboardowa jest bardziej kompaktowa
  - szczegoly warunkow przeniesiono do dialogu `Warunki refundacji NFZ`
- **Bottom navigation**:
  - Glowna
  - Historia
  - Dodaj
  - Alarmy
  - Wiecej

### Poprawki i zmiany techniczne
- Dodano `LibreCareColors` jako centralne tokeny kolorow.
- Dodano `HistoryUiModels.kt` dla legendy zakresow, statystyk historii i prezentacji trendu.
- Rozszerzono `HistoryAggregation.kt` o krotkie zakresy 3h i 6h.
- Ujednolicono formatowanie czasu przez `PolishDateTimeFormatter` i `DateTimeFormatterProvider`.
- Dashboard oraz historia korzystaja z jednej osi czasu przez `readingTimeline(...)`.
- Warning UI nie ukrywa juz krytycznego stanu glikemii, gdy dane sa nieaktualne.

### Testy
- `./gradlew clean` - PASS
- `./gradlew testDebugUnitTest` - PASS
- `./gradlew lint` - PASS
- `./gradlew connectedDebugAndroidTest` - nie wykonano, brak podlaczonego urzadzenia/emulatora
- `./gradlew assembleDebug` - PASS
- `./gradlew assembleRelease` - PASS
- `./gradlew bundleRelease` - PASS

### Artefakty
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
- Release APK: `app/build/outputs/apk/release/app-release.apk`
- Release AAB: `app/build/outputs/bundle/release/app-release.aab`
- Kopie LibreCare:
  - `release-artifacts/LibreCare-1.5.0-debug.apk`
  - `release-artifacts/LibreCare-1.5.0-release.apk`
  - `release-artifacts/LibreCare-1.5.0-release.aab`

### Znane ograniczenia
- `Notatki i zdarzenia` to obecnie placeholder UI bez persystencji.
- Dashboardowy selector nie eksponuje opcji `Wszyscy`, bo obecna architektura snapshotu jest per wybrana osoba.
- Bottom navigation dla `Dodaj` i `Alarmy` jest wizualnie zaimplementowana, ale bez pelnego workflow domenowego.
- Connected tests nie zostaly uruchomione z powodu braku podlaczonego urzadzenia/emulatora.

### Dalsze kroki manualne
1. Otworz dashboard i porownaj hierarchie z mockupem.
2. Zmien osobe i potwierdz natychmiastowe przeladowanie danych bez ponownego logowania.
3. Dotknij wykres na dashboardzie i sprawdz pelny ekran historii.
4. Przeciagnij po wykresie historii i potwierdz czytelny tooltip nad punktem.
5. Otworz dialog NFZ i zweryfikuj sekcje: Status, Okres oceny, Kryteria, Dlaczego niespelnione, Zalecenia.
6. Zweryfikuj dolna nawigacje oraz zachowanie przyciskow Historia i Wiecej.

---

## EN

### Highlights
- LibreCare received a mockup-based dashboard redesign with a darker medical UI, stronger card hierarchy, and a more modern shell.
- Added a compact person selector, compact time-range row, a stronger glucose hero card, and a clearer warning card.
- Reworked the chart preview and full-screen glucose history with range chips, legend, statistics, and a notes placeholder.
- Added bottom navigation and cleaned up color tokens and local-time handling for main timestamps.

### New features
- **Top bar aligned with the mockup**:
  - LibreCare title on the left
  - Refresh, history, and settings actions on the right
  - no monitored-person dropdown in the top bar
- **Compact person selector**:
  - highlighted selected person chip
  - horizontal scrolling when needed
- **Current glucose hero**:
  - dominant mg/dL value
  - visible trend state
  - separate warning card below the hero
  - stale-data badge when needed
- **KPI cards**:
  - compact horizontal layout
  - reduced clipping of key values
  - preserved `0m` vs `no data`
- **Glucose history**:
  - quick ranges: 3h, 6h, 12h, 24h, 7d, 30d, 90d, 365d
  - improved chart readability with risk bands and an above-point tooltip
  - range legend with duration and percentage
  - statistics section
  - notes/events placeholder
- **NFZ**:
  - compact dashboard card
  - detailed conditions moved to the `NFZ refund conditions` dialog
- **Bottom navigation**:
  - Home
  - History
  - Add
  - Alarms
  - More

### Technical changes
- Added `LibreCareColors` as centralized color tokens.
- Added `HistoryUiModels.kt` for history legend/stat UI models and trend presentation.
- Extended `HistoryAggregation.kt` with 3h and 6h quick ranges.
- Unified time formatting through shared formatter utilities.
- Dashboard and history now share one timeline via `readingTimeline(...)`.
- Warning presentation no longer hides critical glucose severity behind stale-data status.

### Validation
- `./gradlew clean` - PASS
- `./gradlew testDebugUnitTest` - PASS
- `./gradlew lint` - PASS
- `./gradlew connectedDebugAndroidTest` - not executed, no connected device/emulator
- `./gradlew assembleDebug` - PASS
- `./gradlew assembleRelease` - PASS
- `./gradlew bundleRelease` - PASS

### Artifacts
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
- Release APK: `app/build/outputs/apk/release/app-release.apk`
- Release AAB: `app/build/outputs/bundle/release/app-release.aab`
- LibreCare copies:
  - `release-artifacts/LibreCare-1.5.0-debug.apk`
  - `release-artifacts/LibreCare-1.5.0-release.apk`
  - `release-artifacts/LibreCare-1.5.0-release.aab`

### Known limitations
- `Notes and events` is currently a UI placeholder without persistence.
- The dashboard does not expose an `All` person option because the current snapshot architecture is person-specific.
- `Add` and `Alarms` in bottom navigation are visually present but not backed by a full domain workflow yet.
- Connected UI tests were not executed because no device or emulator was attached.

