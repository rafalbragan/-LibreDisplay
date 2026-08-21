# LibreCare 2.1.0 - Release Notes

## PL

### Wersja
- previous: `2.0.1 (17)`
- current: `2.1.0 (18)`

### Nowe funkcje
- Dodano rozdzielone sekcje ustawien: `Ustawienia glowne`, `Ustawienia monitoringu`, `Ustawienia konta`.
- Dodano dodatkowe testy regresji dla historii i backupu.
- Dodano sekcje pokrycia danych per osoba na ekranie `Informacje i statystyki` (14/30/60/90/360 dni).

### Poprawki i usprawnienia
- Usprawniono przeplyw backupu LIVE + ustawienia (bez danych demo).
- Usprawniono plynność wykresu przez interpolacje minutowa warstwy prezentacji przy rzadszych punktach.
- Uporzadkowano nawigacje ustawien, aby szybciej trafic do sekcji monitoringu/konta.
- Zapewniono dociaganie opoznionych punktow z okna do 12 godzin podczas synchronizacji.
- Poprawiono semantyke koloru trendu na ekranie glownym (wysoka glikemia + trend spadkowy = zielony).
- Naprawiono routing ustawien monitoringu: `Zakres`, `Metryki` i `HbA1c` maja osobne destination z wlasnymi tytulami i Back.
- Przebudowano gestosc i hierarchie Home/Historia/Settings (mniej duzych blokow, mniej duplikacji, bardziej kompaktowe sekcje).

### Testy
- Wykonano: `./gradlew clean`
- Wykonano: `./gradlew testDebugUnitTest`
- Wykonano: `./gradlew lint`
- Wykonano: `./gradlew assembleDebug`
- Wykonano: `./gradlew assembleRelease`
- Wykonano: `./gradlew bundleRelease`
- `connectedDebugAndroidTest`: nie uruchomiono (brak `adb` w PATH / brak urzadzenia).

### Artefakty
- DEBUG APK: `C:\Users\SG0216827\IdeaProjects\LibreDisplay\release-artifacts\LibreCare-2.1.0-debug.apk` (23 414 839 B)
- RELEASE APK: `C:\Users\SG0216827\IdeaProjects\LibreDisplay\release-artifacts\LibreCare-2.1.0-release.apk` (3 145 248 B)
- RELEASE AAB: `C:\Users\SG0216827\IdeaProjects\LibreDisplay\release-artifacts\LibreCare-2.1.0-release.aab` (5 759 431 B)
- Google Play upload file: `C:\Users\SG0216827\IdeaProjects\LibreDisplay\release-artifacts\LibreCare-2.1.0-release.aab`

### Znane ograniczenia
- Android nie pozwala wyswietlic pytania podczas odinstalowania aplikacji; backup nalezy wykonac przed odinstalowaniem.
- Na podstawie dostepnych danych nie wykonano testow instrumentacyjnych na fizycznym urzadzeniu/emulatorze.

## EN

### Version
- previous: `2.0.1 (17)`
- current: `2.1.0 (18)`

### New features
- Added split settings sections: `Main settings`, `Monitoring settings`, `Account settings`.
- Added additional regression tests for history and backup.
- Added a per-person coverage section on `Statistics` (14/30/60/90/360 day windows).

### Fixes and improvements
- Improved LIVE + settings backup flow (excluding demo data).
- Improved chart smoothness by adding minute-level interpolation for sparse point streams.
- Refined settings navigation for faster access to monitoring/account sections.
- Ensured delayed-point backfill ingestion for the rolling 12-hour sync window.
- Fixed Home trend color semantics (high glucose + falling trend now uses favorable green).
- Fixed monitoring-settings routing: `Target range`, `Metrics`, and `HbA1c` are now separate destinations with their own title/back behavior.
- Reworked Home/History/Settings density and hierarchy (fewer oversized blocks, fewer duplicated labels, tighter section spacing).

### Tests
- Executed: `./gradlew clean`
- Executed: `./gradlew testDebugUnitTest`
- Executed: `./gradlew lint`
- Executed: `./gradlew assembleDebug`
- Executed: `./gradlew assembleRelease`
- Executed: `./gradlew bundleRelease`
- `connectedDebugAndroidTest`: not executed (`adb` unavailable in PATH / no device).

### Artifacts
- DEBUG APK: `C:\Users\SG0216827\IdeaProjects\LibreDisplay\release-artifacts\LibreCare-2.1.0-debug.apk` (23 414 839 B)
- RELEASE APK: `C:\Users\SG0216827\IdeaProjects\LibreDisplay\release-artifacts\LibreCare-2.1.0-release.apk` (3 145 248 B)
- RELEASE AAB: `C:\Users\SG0216827\IdeaProjects\LibreDisplay\release-artifacts\LibreCare-2.1.0-release.aab` (5 759 431 B)
- Google Play upload file: `C:\Users\SG0216827\IdeaProjects\LibreDisplay\release-artifacts\LibreCare-2.1.0-release.aab`

### Known limitations
- Android does not allow uninstall-time confirmation prompts; backup must be executed before uninstall.
- Based on available data, instrumented tests were not run on a connected device/emulator.

