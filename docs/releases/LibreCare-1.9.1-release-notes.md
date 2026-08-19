# LibreCare 1.9.1 – Release Notes

## PL

### Wersja
- versionName: 1.9.1
- versionCode: 15

### Najważniejsza zmiana
To jest kolejna iteracja spłaszczania UI LibreCare: mniej przycisków/pills, jeszcze bardziej kompaktowa góra Home i lżejszy selektor zakresu czasu na ekranie historii.

### Zmiany
- Top bar używa teraz wspólnego tła ekranu zamiast wizualnie podniesionej powierzchni.
- Pasek świeżości danych i statusu sensora jest bardziej tekstowy, z mniejszą ilością blokowego tła.
- `TimeRangeDisplay` na Home jest bardziej kompaktowy i mniej „kontrolkowy”.
- Selektor zakresu czasu na ekranie historii został przebudowany z przycisków `TextButton` / `OutlinedButton` do płaskiego selektora tekstowego z delikatnym underline dla aktywnej opcji.
- Zachowano istniejącą logikę danych, nawigacji i wykresów.

### Usprawnienia
- Jeszcze więcej pionowego miejsca w górnej części ekranu.
- Mniej elementów przypominających chipy lub pills.
- Większa spójność z kierunkiem „shared background + whitespace + typography + divider”.

### Testy
- Dodano `HistorySelectorModelTest`.
- Uruchomiono `clean`, `testDebugUnitTest`, `lint`, `assembleDebug`, `assembleRelease`, `bundleRelease`.

### Artefakty
- Debug APK: `release-artifacts/LibreCare-1.9.1-debug.apk`
- Release APK: `release-artifacts/LibreCare-1.9.1-release.apk`
- Release AAB: `release-artifacts/LibreCare-1.9.1-release.aab`

### Znane ograniczenia
- Nie uruchomiono testów na urządzeniu/emulatorze — brak podłączonego urządzenia.

---

## EN

### Version
- versionName: 1.9.1
- versionCode: 15

### Main change
This is a follow-up flat UI iteration for LibreCare: fewer button/pill-like controls, a more compact Home top area, and a lighter history range selector.

### Changes
- The top bar now uses the shared screen background instead of a visually elevated surface.
- The data freshness and sensor status area is more text-first with less block-like background treatment.
- `TimeRangeDisplay` on Home is more compact and less control-heavy.
- The history time-range selector was rebuilt from `TextButton` / `OutlinedButton` controls into a flat text selector with a subtle underline for the active option.
- Existing data, navigation, and chart logic were preserved.

### Improvements
- More vertical space recovered in the upper part of the screen.
- Fewer elements that visually resemble chips or pills.
- Better consistency with the “shared background + whitespace + typography + divider” direction.

### Tests
- Added `HistorySelectorModelTest`.
- Ran `clean`, `testDebugUnitTest`, `lint`, `assembleDebug`, `assembleRelease`, `bundleRelease`.

### Artifacts
- Debug APK: `release-artifacts/LibreCare-1.9.1-debug.apk`
- Release APK: `release-artifacts/LibreCare-1.9.1-release.apk`
- Release AAB: `release-artifacts/LibreCare-1.9.1-release.aab`

### Known limitations
- No device/emulator tests were run because no connected device was available.

