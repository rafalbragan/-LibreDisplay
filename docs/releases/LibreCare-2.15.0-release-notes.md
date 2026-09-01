# LibreCare 2.15.0 - Release Notes (Updated)

**Data wydania / Release date**: 2026-09-01  
**versionCode**: 41  
**versionName**: 2.15.0  
**Poprzednia wersja / Previous version**: 2.14.0 (versionCode 40)

---

## Polski

### Nowe funkcje

- **Wiek danych przeniesiony na górę** - Informacja o tym jak stare są dane przeniesiona z karty glukozy do górnego paska aplikacji
- **Uwspólniony format czasu** - Całkowicie spójne formatowanie czasu między "Ostatnią aktualizacją" i "Wiekiem danych"

### Ulepszenia interfejsu

- Bardziej zwarta karta główna glukozy poprzez usunięcie zduplikowanej informacji o wieku danych
- Lepsze formatowanie względnego czasu w polu "Wiek danych":
  - `przed chwilą` - dla danych świeższych niż 1 minuta
  - `X min temu` - dla danych w minutach (np. "5 min temu")
  - `X godz. Y min temu` - dla danych do 24 godzin (np. "2 godz. 30 min temu")
  - `X dni Y godz. temu` - dla starszych danych (np. "2 dni 3 godz. temu")

### Zawarte poprzednie zmiany (z tego wydania)

- Dla szybkich trendów (`RISING_FAST`, `FALLING_FAST`) ekran główny pokazuje tempo zmian glikemii w formacie `mg/dL/min`.
- Dodano krótkoterminową projekcję `W tym tempie...` do następnego istotnego progu glikemii zamiast stałej projekcji tylko do `54 mg/dL`.

### Poprawki

- Ukryto rate/ETA dla trendów zwykłych (`RISING`, `FALLING`, `FLAT`) oraz dla danych nieświeżych, zbyt małej liczby próbek, niestabilnego slope i projekcji dłuższych niż `90 min`.
- Ujednolicono granicę `bardzo wysoka glikemia` do `>250 mg/dL`, zgodnie z istniejącą domenową definicją używaną już w historii i statystykach.

### Testy

- `./gradlew clean` - ✅ PASS
- `./gradlew testDebugUnitTest` - ✅ PASS (509 testów)
- `./gradlew lint` - ✅ PASS
- `./gradlew assembleDebug` - ✅ PASS
- `./gradlew assembleRelease` - ✅ PASS
- `./gradlew bundleRelease` - ✅ PASS
- Test regresyjny formatowania wieku danych - ✅ PASS
- Test regresyjny: przełączenie na osobę bez danych wykresu kończy się kontrolowanym błędem domenowym - ✅ PASS
- Brak uruchomionych testów connected: No connected device/emulator available.

### Artefakty

- `app/build/outputs/apk/debug/app-debug.apk` (23908326 B / 23.9 MB)
- `app/build/outputs/apk/release/app-release.apk` (3695753 B / 3.7 MB)
- `app/build/outputs/bundle/release/app-release.aab` (6713405 B / 6.7 MB)

### Znane ograniczenia

- Brak

---

## English

### New features

- **Data age moved to top bar** - Data age information moved from glucose card to the top bar of the application
- **Unified time format** - Completely consistent time formatting between "Last Update" and "Data Age"

### UI Improvements

- More compact main glucose card by removing duplicated data age information
- Better relative time formatting in the "Data Age" field:
  - `before a moment` - for data fresher than 1 minute
  - `X min ago` - for data in minutes (e.g., "5 min ago")
  - `X hrs Y min ago` - for data up to 24 hours (e.g., "2 hrs 30 min ago")
  - `X days Y hrs ago` - for older data (e.g., "2 days 3 hrs ago")

### Included previous changes (from this release)

- The Home screen now shows glucose change speed in `mg/dL/min` for fast trends (`RISING_FAST`, `FALLING_FAST`).
- Added short-term `At this pace...` projection to the next meaningful glucose threshold instead of a fixed `54 mg/dL` projection only.

### Fixes

- Hidden rate/ETA for ordinary trends (`RISING`, `FALLING`, `FLAT`) and for stale data, too few samples, unstable slopes, and projections longer than `90 min`.
- Aligned the `very high glucose` boundary to `>250 mg/dL`, matching the existing domain definition already used by history and statistics.

### Tests

- `./gradlew clean` - ✅ PASS
- `./gradlew testDebugUnitTest` - ✅ PASS (509 tests)
- `./gradlew lint` - ✅ PASS
- `./gradlew assembleDebug` - ✅ PASS
- `./gradlew assembleRelease` - ✅ PASS
- `./gradlew bundleRelease` - ✅ PASS
- Data age formatting regression test - ✅ PASS
- Regression test: switching to a person without graph data now follows a controlled domain-error path - ✅ PASS
- Connected tests not run: No connected device/emulator available.

### Artifacts

- `app/build/outputs/apk/debug/app-debug.apk` (23908326 B / 23.9 MB)
- `app/build/outputs/apk/release/app-release.apk` (3695753 B / 3.7 MB)
- `app/build/outputs/bundle/release/app-release.aab` (6713405 B / 6.7 MB)

### Known limitations

- None
