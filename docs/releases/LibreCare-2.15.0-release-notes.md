# LibreCare 2.15.0 - Release Notes

**Data wydania / Release date**: 2026-08-31  
**versionCode**: 41  
**versionName**: 2.15.0  
**Poprzednia wersja / Previous version**: 2.14.0 (versionCode 40)

---

## Polski

### Nowe funkcje

- Dla szybkich trendów (`RISING_FAST`, `FALLING_FAST`) ekran główny pokazuje tempo zmian glikemii w formacie `mg/dL/min`.
- Dodano krótkoterminową projekcję `W tym tempie...` do następnego istotnego progu glikemii zamiast stałej projekcji tylko do `54 mg/dL`.
- Wspólny model projekcji używa rzeczywistych, ostatnich próbek CGM oraz tego samego okna trendu co obliczanie slope.

### Ulepszenia

- Projekcja wykorzystuje dynamiczne progi: dolny próg użytkownika, `54 mg/dL`, górny próg użytkownika oraz `250 mg/dL` jako próg very-high.
- Dla glikemii już bardzo wysokiej i nadal szybko rosnącej aplikacja komunikuje stan bez arbitralnej ekstrapolacji do `350/390/400 mg/dL`.
- Tekst projekcji jest wyświetlany osobno od alertu medycznego, dzięki czemu obecny wynik CGM pozostaje wizualnie najważniejszy.

### Poprawki

- Ukryto rate/ETA dla trendów zwykłych (`RISING`, `FALLING`, `FLAT`) oraz dla danych nieświeżych, zbyt małej liczby próbek, niestabilnego slope i projekcji dłuższych niż `90 min`.
- Ujednolicono granicę `bardzo wysoka glikemia` do `>250 mg/dL`, zgodnie z istniejącą domenową definicją używaną już w historii i statystykach.

### Testy

- `./gradlew clean` - PASS
- `./gradlew testDebugUnitTest` - PASS
- `./gradlew lint` - PASS
- `./gradlew assembleDebug` - PASS
- `./gradlew assembleRelease` - PASS
- `./gradlew bundleRelease` - PASS
- Test regresyjny: przełączenie na osobę bez danych wykresu kończy się kontrolowanym błędem domenowym (`SelectedPersonGraphException`) bez awarii.
- Brak uruchomionych testów connected: No connected device/emulator available.

### Artefakty

- `C:\Users\SG0216827\IdeaProjects\LibreDisplay\release-artifacts\LibreCare-2.15.0-debug.apk` (23891925 B)
- `C:\Users\SG0216827\IdeaProjects\LibreDisplay\release-artifacts\LibreCare-2.15.0-release.apk` (3695748 B)
- `C:\Users\SG0216827\IdeaProjects\LibreDisplay\release-artifacts\LibreCare-2.15.0-release.aab` (6713459 B)

### Znane ograniczenia

- Projekcja ma charakter krótkoterminowy i liniowy; nie jest to model predykcyjny glikemii.
- Brak walidacji connected tests w tym środowisku (`adb` niedostępne).

---

## English

### New features

- The Home screen now shows glucose change speed in `mg/dL/min` for fast trends (`RISING_FAST`, `FALLING_FAST`).
- Added short-term `At this pace...` projection to the next meaningful glucose threshold instead of a fixed `54 mg/dL` projection only.
- The shared projection model uses real recent CGM samples and the same trend window as the slope calculation.

### Improvements

- Projection now uses dynamic thresholds: the configured low threshold, `54 mg/dL`, the configured high threshold, and `250 mg/dL` as the very-high threshold.
- When glucose is already very high and still rising fast, the app reports that state without arbitrary extrapolation to `350/390/400 mg/dL`.
- Projection text is rendered separately from the medical warning so the current CGM reading remains visually dominant.

### Fixes

- Hidden rate/ETA for ordinary trends (`RISING`, `FALLING`, `FLAT`) and for stale data, too few samples, unstable slopes, and projections longer than `90 min`.
- Aligned the `very high glucose` boundary to `>250 mg/dL`, matching the existing domain definition already used by history and statistics.

### Tests

- `./gradlew clean` - PASS
- `./gradlew testDebugUnitTest` - PASS
- `./gradlew lint` - PASS
- `./gradlew assembleDebug` - PASS
- `./gradlew assembleRelease` - PASS
- `./gradlew bundleRelease` - PASS
- Regression test: switching to a person without graph data now follows a controlled domain-error path (`SelectedPersonGraphException`) instead of a crash.
- Connected tests not run: No connected device/emulator available.

### Artifacts

- `C:\Users\SG0216827\IdeaProjects\LibreDisplay\release-artifacts\LibreCare-2.15.0-debug.apk` (23891925 B)
- `C:\Users\SG0216827\IdeaProjects\LibreDisplay\release-artifacts\LibreCare-2.15.0-release.apk` (3695748 B)
- `C:\Users\SG0216827\IdeaProjects\LibreDisplay\release-artifacts\LibreCare-2.15.0-release.aab` (6713459 B)

### Known limitations

- The projection is short-horizon and linear; it is not a glucose prediction model.
- Connected-device validation is not available in this environment (`adb` unavailable).

