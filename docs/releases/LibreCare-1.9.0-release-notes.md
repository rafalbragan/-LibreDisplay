# LibreCare 1.9.0 – Release Notes

## PL

### Wersja
- versionName: 1.9.0
- versionCode: 14

### Najważniejsza zmiana
Ta iteracja przebudowuje layout LibreCare tak, aby aplikacja przestała wyglądać jak zestaw dużych kart ułożonych jedna pod drugą.

### Zmiany
- Usunięto większość dużych pełnoekranowych kart z ekranu głównego i historii.
- Sekcja aktualnej glikemii została spłaszczona — hierarchia opiera się teraz głównie o typografię, whitespace i małe akcenty zamiast dużego kontenera.
- Quick metrics zostały przebudowane do kompaktowego pasa z delikatnymi pionowymi separatorami.
- Person switcher jest lżejszy wizualnie i nie wygląda już jak rząd dużych pills.
- Sekcja historii glikemii i sekcja „Czas w zakresach” zostały uproszczone do płaskich sekcji.
- Karty szczegółów punktu i statystyk na ekranie historii zostały zastąpione znacznie lżejszymi blokami.
- Zachowano istniejącą logikę danych i ViewModel — zmiany dotyczą głównie struktury layoutu i prezentacji.

### Usprawnienia
- Odzyskano pionowe miejsce na ekranie.
- Zmniejszono liczbę warstw tła i dużych rounded boxes.
- Interfejs jest bardziej płaski i mniej „pudełkowy”.

### Testy
- Dodano `RedesignedMetricsTest`.
- Uruchomiono `clean`, `testDebugUnitTest`, `lint`, `assembleDebug`, `assembleRelease`, `bundleRelease`.

### Artefakty
- Debug APK: `release-artifacts/LibreCare-1.9.0-debug.apk`
- Release APK: `release-artifacts/LibreCare-1.9.0-release.apk`
- Release AAB: `release-artifacts/LibreCare-1.9.0-release.aab`

### Znane ograniczenia
- Nie uruchomiono testów na urządzeniu/emulatorze — brak podłączonego urządzenia.

---

## EN

### Version
- versionName: 1.9.0
- versionCode: 14

### Main change
This iteration rebuilds the LibreCare layout so the app no longer looks like a stack of large rounded cards.

### Changes
- Removed most large full-width cards from Home and History.
- Flattened the current glucose section so hierarchy now comes mainly from typography, whitespace, and small accents instead of a large container.
- Rebuilt quick metrics into a compact strip with subtle vertical separators.
- Made the person switcher visually lighter so it no longer looks like a row of large pills.
- Simplified the glucose history and “Time in ranges” sections into flat sections.
- Replaced history point/detail cards and stats cards with much lighter blocks.
- Preserved existing data and ViewModel logic — changes are focused on layout structure and presentation.

### Improvements
- Recovered vertical screen space.
- Reduced stacked background layers and large rounded boxes.
- Made the interface flatter and less box-heavy.

### Tests
- Added `RedesignedMetricsTest`.
- Ran `clean`, `testDebugUnitTest`, `lint`, `assembleDebug`, `assembleRelease`, `bundleRelease`.

### Artifacts
- Debug APK: `release-artifacts/LibreCare-1.9.0-debug.apk`
- Release APK: `release-artifacts/LibreCare-1.9.0-release.apk`
- Release AAB: `release-artifacts/LibreCare-1.9.0-release.aab`

### Known limitations
- No device/emulator tests were run because no connected device was available.

