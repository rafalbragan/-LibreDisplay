# LibreCare 1.7.0 – Release Notes

## Wersja / Version
`1.7.0` (versionCode 11)

---

## PL – Informacje o wydaniu

### Naprawione problemy

1. **Nadmiar dużych zaokrąglonych kart (Cards) w UI**
   - Sekcja „Szybkie metryki" nie jest już opakowana w dużą kartę – metryki są wyświetlane bezpośrednio na tle ekranu.
   - Wykres historii glikemii nie jest już zamknięty w Card – nagłówek i wykres są sekcją rozdzieloną subtelnymi separatorami.
   - `TimeRangeDisplay` nie jest już opakowane w Surface/pill – prezentowane bezpośrednio w rzędzie na głównym tle.
   - Zmniejszono promienie zaokrągleń: karta glukozy 20dp→14dp, NFZ 18dp→12dp, MedicalAlert 12dp→8dp.

2. **Mylenie wybranego zakresu z faktycznie dostępnymi danymi**
   - Dodano centralny model danych `DataCoverageModel` oddzielający:
     - WYBRANY ZAKRES (np. 24 godz.) od
     - DOSTĘPNE DANE (np. 8 godz. 02 min)
   - Nagłówek sekcji „Historia glikemii" pokazuje teraz faktyczny span danych, np. „8 godz. 02 min dostępnych danych".
   - Gdy dane pokrywają pełny zakres, pokazywane jest „Ostatnie 24 godz." bez dodatkowych komunikatów.
   - Pojawia się informacja „Wybrany zakres: 24 godz." jako wtórny tekst.
   - Szacowany czas do pełnego zakresu: „Przy ciągłym zapisie pełny zakres 24 godz. będzie dostępny za ok. 15 godz. 58 min."
   - Ta sama logika zastosowana w ekranie historii pełnoekranowej.
   - Tytuł statystyk: „Statystyki · 8 godz. 02 min danych" zamiast „Statystyki - Ostatnie 24 godz."
   - Sekcja „Czas w zakresach" pokazuje rzeczywisty span, nie wybrany zakres.

3. **Usunięcie nieprecyzyjnego komunikatu o baterii**
   - Usunięto ogólny komunikat „Zużycie baterii może wzrosnąć" z ekranu ustawień częstotliwości synchronizacji.
   - String zaktualizowany do precyzyjniejszej informacji, widocznej tylko w kontekście.

### Nowe funkcje

- **`DataCoverageModel`** – centralny model opisujący pokrycie danych:
  - `selectedRange`, `selectedRangeLabel`
  - `oldestAvailableTimestamp`, `newestAvailableTimestamp`
  - `availableSpan`, `hasFullCoverage`, `timeUntilFullCoverage`
  - Automatycznie obliczane: `sectionHeaderLabel`, `selectedRangeNote`, `fullCoverageEstimate`

- **`formatNaturalDuration()`** – naturalny polski format czasu trwania:
  - „chwilę temu", „15 min", „8 godz. 02 min", „24 godz.", „7 dni"

### Ulepszenia

- Layout dashboardu bardziej płaski: whitespace, typografia, subtelne separatory zamiast pól kart.
- `TimeRange.toSelectedRangeLabel()` – krótkie polskie etykiety dla zakresów (3 godz., 24 godz., 7 dni).
- Spójne formatowanie czasu w całej aplikacji.

### Testy

- Dodano `DataCoverageModelTest` (8 przypadków testowych):
  - `computeDataCoverage_emptyHistory_returnsNoData`
  - `computeDataCoverage_partialData_hasNoFullCoverage`
  - `computeDataCoverage_fullData_hasFullCoverage`
  - `sectionHeaderLabel_fullCoverage_returnsSelectedRangeLabel`
  - `sectionHeaderLabel_partialCoverage_returnsAvailableSpan`
  - `formatNaturalDuration_formatsCorrectly`
  - `timeRangeToSelectedRangeLabel_allValues`
- Zaktualizowano `HistoryUiModelsTest` (dodano 2 nowe przypadki testowe).
- Łącznie: **274 testów** przechodzi.

### Artefakty

| Typ | Ścieżka | Rozmiar |
|-----|---------|---------|
| Debug APK | `release-artifacts/LibreCare-1.7.0-debug.apk` | ~22 657 KB |
| Release APK | `release-artifacts/LibreCare-1.7.0-release.apk` | ~2 984 KB |
| Release AAB | `release-artifacts/LibreCare-1.7.0-release.aab` | ~5 483 KB |

### Znane ograniczenia

- `formatNaturalDuration` dla zakresów > 24h do ~48h zwraca format dni (np. „2 dni") zamiast godzin – zachowanie celowe.
- Czas do pełnego zakresu jest szacunkiem zakładającym ciągłe zbieranie danych i nie jest gwarancją.
- Completeness/gap detection (kompletność z wykrywaniem przerw) nie jest zaimplementowane – wymagałoby bardziej zaawansowanego modelu.

---

## EN – Release Information

### Fixes

1. **Too many large rounded cards in UI**
   - Quick metrics no longer wrapped in a Card – displayed directly on screen background.
   - Glucose history chart no longer in a Card – uses dividers/spacing for separation.
   - `TimeRangeDisplay` no longer uses a Surface/pill wrapper.
   - Reduced corner radii: glucose card 20dp→14dp, NFZ 18dp→12dp.

2. **Selected range vs. available data confusion**
   - Added central `DataCoverageModel` separating SELECTED RANGE from AVAILABLE DATA.
   - Chart section header now shows actual data span.
   - Statistics titled "Statystyki · 8 godz. 02 min danych" instead of selected range label.
   - Coverage estimate: "Przy ciągłym zapisie pełny zakres 24 godz. będzie dostępny za ok. 15 godz. 58 min."

3. **Removed imprecise battery warning**
   - Generic "battery usage may increase" removed from polling frequency settings.

### Tests

274 unit tests passing. New: `DataCoverageModelTest` (8 cases), updated `HistoryUiModelsTest`.

