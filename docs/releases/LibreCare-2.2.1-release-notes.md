# LibreCare 2.2.1 - Release Notes

## PL

### Wersja
- previous: `2.2.0 (20)`
- current: `2.2.1 (21)`

### Nowe funkcje
- Dodano tekstowy score UX do raportu audytu UI (`ui-audit-report.md`), z oceną per ekran i oceną globalną.
- Dodano flagi ryzyka dla warunków podnoszących ryzyko overlapów lub clippingu, np. wąski viewport albo większy `fontScale`.
- Rozszerzono tryb demo do 5 osób, aby dało się testować przewijanie i overflow `+N` w przełączniku monitorowanych osób.

### Poprawki i usprawnienia
- Przywrócono ikonę aparatu w nagłówku Home i uproszczono użycie audytu UI bez dodatkowej blokady adresu e-mail.
- Przebudowano nagłówek ekranu głównego: po lewej nazwa aplikacji, wersja i skrót zasięgu bazy (`DB`), po prawej ostatni odczyt, przewidywany koniec działania sensora i skrót zakresu danych.
- Lista monitorowanych osób pokazuje teraz do 2 szerokich boksów, a pozostałe osoby grupuje w chipie `+N` z rozwijaną przewijaną listą.
- Blok `Aktualna glikemia` i sekcja trendu działają teraz w jednym wierszu, z lepszą pozycją strzałki oraz czytelniejszym opisem statusu.
- Sekcja `Historia glikemii` pokazuje realny skrót bazy danych (`12h / 3d / 7d / 30d` oraz `Okno Xh · baza Yd`) zamiast sugerować, że historia kończy się po ~12h.
- Navigator Home pod wykresem używa teraz tylko jednego suwaka viewportu o szerokości proporcjonalnej do `1h / 3h / 6h / 9h / 12h`, bez dodatkowego dolnego suwaka systemowego.
- Oś X na Home pokazuje dzień i godzinę, dzięki czemu przy oknach godzinowych wiadomo, z którego dnia pochodzi punkt.
- Metryki Home zostały przeniesione pod wykres i działają jako przewijany poziomo pasek z możliwością ukrywania/pokazywania metryk w ustawieniach.

### Testy
- Wykonano: `./gradlew clean`
- Wykonano: `./gradlew testDebugUnitTest`
- Wykonano: `./gradlew lint`
- Wykonano: `./gradlew assembleDebug`
- Wykonano: `./gradlew assembleRelease`
- Wykonano: `./gradlew bundleRelease`
- Dodatkowo uruchomiono: `UiAuditExporterTest`, `HomeChartModelsTest`, `PolishFormattersTest`, `DataCoverageModelTest`, `RedesignedMetricsTest`
- `connectedDebugAndroidTest`: nie uruchomiono — **No connected device/emulator available.**

### Artefakty
- DEBUG APK: `C:\Users\SG0216827\IdeaProjects\LibreDisplay\release-artifacts\LibreCare-2.2.1-debug.apk` (23 464 012 B)
- RELEASE APK: `C:\Users\SG0216827\IdeaProjects\LibreDisplay\release-artifacts\LibreCare-2.2.1-release.apk` (3 178 011 B)
- RELEASE AAB: `C:\Users\SG0216827\IdeaProjects\LibreDisplay\release-artifacts\LibreCare-2.2.1-release.aab` (5 808 931 B)
- Google Play upload file: `C:\Users\SG0216827\IdeaProjects\LibreDisplay\release-artifacts\LibreCare-2.2.1-release.aab` (5 808 931 B)

### Znane ograniczenia
- W tym środowisku nie ma dostępnego `adb`, więc nie wykonano testów na urządzeniu/emulatorze.
- Brak screenshot/golden tests dla pełnej macierzy `360dp / 384dp / 480dp` oraz `fontScale 1.0 / 1.3 / 1.5`.
- Tekstowy score UX jest heurystyczny i wspiera audyt, ale nie zastępuje ręcznej oceny interakcji dotykowych.

## EN

### Version
- previous: `2.2.0 (20)`
- current: `2.2.1 (21)`

### New features
- Added a textual UX score to the UI audit report (`ui-audit-report.md`) with per-screen and global evaluation.
- Added risk flags for conditions that increase overlap/clipping probability, such as narrow viewports or larger `fontScale` values.
- Expanded Demo Mode to 5 people so the monitored-person switcher and `+N` overflow can be tested.

### Fixes and improvements
- Restored the camera icon in the Home header and simplified UI audit access by removing the extra e-mail restriction.
- Reworked the Home header: app name, version and DB span on the left; last reading, estimated sensor end, and data-range summary on the right.
- The monitored-people list now shows up to 2 wide boxes and groups the rest under a `+N` chip with an expandable horizontal list.
- The `Current glucose` block and the trend section now stay in one row, with improved arrow placement and a clearer status label.
- The `Glucose history` section now reflects the real local database span (`12h / 3d / 7d / 30d` and `Window Xh · DB Yd`) instead of implying history only goes back ~12h.
- The Home navigator below the chart now uses a single viewport slider whose width is proportional to `1h / 3h / 6h / 9h / 12h`, without the extra system slider below it.
- The Home X-axis now shows both date and time so hourly windows are easier to interpret.
- Home metrics were moved below the chart and rebuilt as a horizontally scrollable strip with show/hide controls in settings.

### Tests
- Executed: `./gradlew clean`
- Executed: `./gradlew testDebugUnitTest`
- Executed: `./gradlew lint`
- Executed: `./gradlew assembleDebug`
- Executed: `./gradlew assembleRelease`
- Executed: `./gradlew bundleRelease`
- Additionally ran: `UiAuditExporterTest`, `HomeChartModelsTest`, `PolishFormattersTest`, `DataCoverageModelTest`, `RedesignedMetricsTest`
- `connectedDebugAndroidTest`: not executed — **No connected device/emulator available.**

### Artifacts
- DEBUG APK: `C:\Users\SG0216827\IdeaProjects\LibreDisplay\release-artifacts\LibreCare-2.2.1-debug.apk` (23,464,012 B)
- RELEASE APK: `C:\Users\SG0216827\IdeaProjects\LibreDisplay\release-artifacts\LibreCare-2.2.1-release.apk` (3,178,011 B)
- RELEASE AAB: `C:\Users\SG0216827\IdeaProjects\LibreDisplay\release-artifacts\LibreCare-2.2.1-release.aab` (5,808,931 B)
- Google Play upload file: `C:\Users\SG0216827\IdeaProjects\LibreDisplay\release-artifacts\LibreCare-2.2.1-release.aab` (5,808,931 B)

### Known limitations
- `adb` is not available in the current environment, so device/emulator tests were not executed.
- No screenshot/golden tests were added for the full `360dp / 384dp / 480dp` and `fontScale 1.0 / 1.3 / 1.5` matrix.
- The textual UX score is heuristic and supports audits, but it does not replace manual touch-interaction validation.


