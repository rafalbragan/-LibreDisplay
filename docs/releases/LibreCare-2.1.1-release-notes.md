# LibreCare 2.1.1 - Release Notes

## PL

### Wersja
- previous: `2.1.0 (18)`
- current: `2.1.1 (19)`

### Nowe funkcje
- Brak nowych funkcji. To iteracja layoutowa Home.

### Poprawki i usprawnienia
- Przebudowano WYŁĄCZNIE wygląd i layout ekranu głównego `Home` bez zmian API, ViewModel, repozytoriów i logiki danych.
- Zwężono top area: bardziej kompaktowy `LibreTopBar` oraz pasek świeżości/sensora w jednej zwartej linii.
- Dodano kompaktowy wiersz zakresu: `Zakres · dane do HH:mm` oraz akcję `Historia >` w tym samym rzędzie.
- Przebudowano blok aktualnej glikemii: duża wartość nadal dominuje, strzałka trendu jest bliżej odczytu, a duplikacja znaczenia status/trend została usunięta.
- Przebudowano metryki do płaskiego, gęstszego układu 4-kolumnowego (fallback 2x2 na węższych szerokościach), z akcją `Edytuj >` inline.
- Podniesiono sekcję `Historia glikemii` i utrzymano wykres Home jako preview (~220dp), aby znacząca część wykresu była widoczna na pierwszym ekranie.
- Ograniczono liczbę etykiet osi oraz padding osi na Home (bez zmiany danych/progów), żeby zwiększyć obszar samego wykresu.

### Testy
- Wykonano: `./gradlew clean`
- Wykonano: `./gradlew testDebugUnitTest`
- Wykonano: `./gradlew lint`
- Wykonano: `./gradlew assembleDebug`
- Wykonano: `./gradlew assembleRelease`
- Wykonano: `./gradlew bundleRelease`
- `connectedDebugAndroidTest`: nie uruchomiono (brak `adb` w PATH / brak urządzenia-emulatora).

### Artefakty
- DEBUG APK: `C:\Users\SG0216827\IdeaProjects\LibreDisplay\release-artifacts\LibreCare-2.1.1-debug.apk` (23 414 859 B)
- RELEASE APK: `C:\Users\SG0216827\IdeaProjects\LibreDisplay\release-artifacts\LibreCare-2.1.1-release.apk` (3 161 633 B)
- RELEASE AAB: `C:\Users\SG0216827\IdeaProjects\LibreDisplay\release-artifacts\LibreCare-2.1.1-release.aab` (5 765 417 B)
- Google Play upload file: `C:\Users\SG0216827\IdeaProjects\LibreDisplay\release-artifacts\LibreCare-2.1.1-release.aab`

### Znane ograniczenia
- Na podstawie dostępnych danych nie wykonano testów instrumentacyjnych UI na podłączonym urządzeniu/emulatorze.
- Weryfikacja viewportów 360dp/384dp i fontScale 1.2 została wykonana na podstawie ograniczeń layoutu i parametrów Compose; bez zrzutów z fizycznego urządzenia w tym przebiegu.

## EN

### Version
- previous: `2.1.0 (18)`
- current: `2.1.1 (19)`

### New features
- No new features. This is a Home layout iteration.

### Fixes and improvements
- Reworked ONLY the `Home` screen look/layout with no changes to API, ViewModel, repositories, or data logic.
- Reduced top-area height with a tighter `LibreTopBar` and a single-line freshness/sensor status row.
- Added a compact range row: `Range · data until HH:mm` and `History >` action in one line.
- Reworked current glucose block: value remains dominant, trend arrow is visually attached to the reading, and duplicated status/trend meaning is removed.
- Reworked quick metrics into a flatter, denser 4-column layout (2x2 fallback on narrower widths), with inline `Edit >` action.
- Moved `Glucose history` section higher and kept Home chart as preview (~220dp) so a significant chart portion is visible on first viewport.
- Reduced Home axis label density and axis paddings (without data/threshold changes) to increase chart drawable area.

### Tests
- Executed: `./gradlew clean`
- Executed: `./gradlew testDebugUnitTest`
- Executed: `./gradlew lint`
- Executed: `./gradlew assembleDebug`
- Executed: `./gradlew assembleRelease`
- Executed: `./gradlew bundleRelease`
- `connectedDebugAndroidTest`: not executed (`adb` missing in PATH / no device-emulator).

### Artifacts
- DEBUG APK: `C:\Users\SG0216827\IdeaProjects\LibreDisplay\release-artifacts\LibreCare-2.1.1-debug.apk` (23,414,859 B)
- RELEASE APK: `C:\Users\SG0216827\IdeaProjects\LibreDisplay\release-artifacts\LibreCare-2.1.1-release.apk` (3,161,633 B)
- RELEASE AAB: `C:\Users\SG0216827\IdeaProjects\LibreDisplay\release-artifacts\LibreCare-2.1.1-release.aab` (5,765,417 B)
- Google Play upload file: `C:\Users\SG0216827\IdeaProjects\LibreDisplay\release-artifacts\LibreCare-2.1.1-release.aab`

### Known limitations
- Based on available data, instrumented UI tests were not executed on a connected device/emulator.
- 360dp/384dp and fontScale 1.2 checks are based on layout constraints and Compose parameters in this run, without physical-device screenshots.

