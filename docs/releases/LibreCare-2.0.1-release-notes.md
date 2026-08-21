# LibreCare 2.0.1 - Release Notes

## PL

### Wersja
- versionName: `2.0.1`
- versionCode: `17`

### Nowe funkcje
- Dodano eksport kopii zapasowej `LIVE + ustawienia` (bez danych demo).
- Dodano przywracanie kopii zapasowej z pliku.

### Poprawki i usprawnienia
- Zwiekeszono gestosc punktow wykresu na ekranie glownym przez dynamiczny budzet punktow zalezny od szerokosci wykresu.
- Wprowadzono hybryde debug/release dla ciaglosci danych: jeden `applicationId` i podpis debug kluczem release (gdy dostepny).
- Przywracanie kopii filtruje rekordy demo i blokuje odtworzenie trybu `DEMO`.

### Testy
- Dodano testy: `AppDataBackupRepositoryTest`.
- Rozszerzono testy: `GlucoseChartLayoutLogicTest`.
- Wykonano: `./gradlew clean`, `./gradlew testDebugUnitTest`, `./gradlew lint`, `./gradlew assembleDebug`, `./gradlew assembleRelease`, `./gradlew bundleRelease`.
- Brak podlaczonego urzadzenia/emulatora (`adb` niedostepne).

### Artefakty
- DEBUG APK: `C:\Users\SG0216827\IdeaProjects\LibreDisplay\release-artifacts\LibreCare-2.0.1-debug.apk` (23 365 701 B)
- RELEASE APK: `C:\Users\SG0216827\IdeaProjects\LibreDisplay\release-artifacts\LibreCare-2.0.1-release.apk` (3 128 979 B)
- RELEASE AAB: `C:\Users\SG0216827\IdeaProjects\LibreDisplay\release-artifacts\LibreCare-2.0.1-release.aab` (5 692 920 B)

### Znane ograniczenia
- Android nie pozwala wyswietlic pytania w trakcie systemowego odinstalowania. Zachowanie danych odbywa sie przez backup wykonany przed odinstalowaniem.

## EN

### Version
- versionName: `2.0.1`
- versionCode: `17`

### New features
- Added `LIVE + settings` backup export (excluding demo data).
- Added backup restore from file.

### Fixes and improvements
- Increased Home chart granularity with a dynamic point budget based on chart width.
- Implemented debug/release hybrid data continuity: one `applicationId` and debug uses release signing when available.
- Restore now filters demo rows and blocks restoring `DEMO` mode.

### Tests
- Added tests: `AppDataBackupRepositoryTest`.
- Extended tests: `GlucoseChartLayoutLogicTest`.
- Executed: `./gradlew clean`, `./gradlew testDebugUnitTest`, `./gradlew lint`, `./gradlew assembleDebug`, `./gradlew assembleRelease`, `./gradlew bundleRelease`.
- No connected device/emulator available (`adb` unavailable).

### Artifacts
- DEBUG APK: `C:\Users\SG0216827\IdeaProjects\LibreDisplay\release-artifacts\LibreCare-2.0.1-debug.apk` (23 365 701 B)
- RELEASE APK: `C:\Users\SG0216827\IdeaProjects\LibreDisplay\release-artifacts\LibreCare-2.0.1-release.apk` (3 128 979 B)
- RELEASE AAB: `C:\Users\SG0216827\IdeaProjects\LibreDisplay\release-artifacts\LibreCare-2.0.1-release.aab` (5 692 920 B)

### Known limitations
- Android does not allow an uninstall-time confirmation dialog. Data preservation relies on running backup before uninstall.

