# LibreCare 1.2.1 Release Notes

## Polski

### Wersja
- versionName: 1.2.1
- versionCode: 4
- data wydania: 2026-08-18
- branch: master
- commit hash: bc7f2e8

### Najważniejsze zmiany
- Naprawiono przepływ trybu Demo i Live
- Poprawiono polską lokalizację na ekranie startowym i ekranach prywatności
- Naprawiono procedurę czyszczenia i ponownego logowania
- Ulepszono testy routowania i obsługi sesji

### Nowe funkcje
- Widoczna akcja przełączania z trybu Demo na Live na ekranie monitorowania
- Polskie komunikaty potwierdzenia i przewodnictwa w przepływach logowania i resetowania

### Poprawki błędów
- Tryb Demo nie miał widocznego przełącznika na Live
- Tryb Live mógł routować nieprawidłowo i pomijać ekran logowania gdy sesja była brakująca
- Przepływ "Wyczyść zapisany token i zaloguj ponownie" nie egzekwował konsekwentnie przełączenia na LIVE + logowanie
- Przepływy resetowania/czyszczenia mogły zostawić stale stanu trybu/nawigacji, które ponownie otwierały Demo
- Kilka angielskich napisów pozostawało widocznych na kluczowych ekranach

### Zmiany techniczne
- Routowanie startowe wzmocnione w `app/src/main/java/com/libredisplay/AppLaunchResolver.kt`
- Obsługa stanu startowego/nawigacji zaktualizowana w `app/src/main/java/com/libredisplay/MainActivity.kt`
- Ekran monitorowania zaktualizowany w `app/src/main/java/com/libredisplay/ui/monitoring/MonitoringScreen.kt`
- Logika prywatności wzmocniona w `app/src/main/java/com/libredisplay/data/repository/PrivacyRepository.kt`
- Zaktualizowano interfejs prywatności i ViewModel
- Ulepszona obsługa logowania w ustawieniach

### Testy
- `./gradlew clean`: PASS
- `./gradlew testDebugUnitTest`: PASS
- `./gradlew lint`: PASS
- `./gradlew connectedDebugAndroidTest`: Nie uruchomiono, ponieważ nie było podłączonego urządzenia ani emulatora.
- `./gradlew assembleDebug`: PASS
- `./gradlew assembleRelease`: PASS
- `./gradlew bundleRelease`: PASS

### Artefakty
Debug APK:
`C:\Users\SG0216827\IdeaProjects\LibreDisplay\app\build\outputs\apk\debug\app-debug.apk`
22,960,342 bytes (21.9 MiB)

Release APK:
`C:\Users\SG0216827\IdeaProjects\LibreDisplay\app\build\outputs\apk\release\app-release.apk`
2,933,630 bytes (2.8 MiB)

Release AAB:
`C:\Users\SG0216827\IdeaProjects\LibreDisplay\app\build\outputs\bundle\release\app-release.aab`
5,352,329 bytes (5.1 MiB)

Google Play upload artifact:
`C:\Users\SG0216827\IdeaProjects\LibreDisplay\app\build\outputs\bundle\release\app-release.aab`
5,352,329 bytes (5.1 MiB)

LibreCare-named artifact copies:
- `release-artifacts/LibreCare-1.2.1-debug.apk` (21.9 MiB)
- `release-artifacts/LibreCare-1.2.1-release.apk` (2.8 MiB)
- `release-artifacts/LibreCare-1.2.1-release.aab` (5.1 MiB)

### Znane ograniczenia
- Brak urządzenia fizycznego podłączonego do testów connected
- Wymaga testu na rzeczywistym urządzeniu/emulatorze przed publikacją w Google Play
- Wymaga ręcznej weryfikacji przepływu logowania i czyszczenia sesji w Google Play Console
- Niektóre scenariusze UI nie są pokryte testami end-to-end

### Kroki ręczne
- Test pełnego przepływu logowania na rzeczywistym urządzeniu z produkcyjnymi danymi logowania LibreLinkUp
- Weryfikacja ścieżki czyszczenia tokenów -> ponownego logowania w stosunku do rzeczywistego zachowania API rate-limit
- Upload release AAB do Google Play Internal Testing
- Aktualizacja screenshotów/notek recenzenta jeśli wymagane
- Weryfikacja i aktualizacja Privacy Policy, jeśli wymagane
- Weryfikacja i aktualizacja Health Apps Declaration, jeśli wymagane

---

## English

### Version
- versionName: 1.2.1
- versionCode: 4
- release date: 2026-08-18
- branch: master
- commit hash: bc7f2e8

### Highlights
- Fixed Demo Mode and Live Mode flow transitions
- Fixed Polish localization on startup, privacy, and flow screens
- Fixed token clearing and forced relogin flow
- Improved tests for launch routing and session handling

### New features
- Visible switch action from Demo to Live mode on monitoring screen
- Polish confirmation messages and guidance in login and reset flows

### Bug fixes
- Demo Mode had no clear switch to Live
- Live Mode could route incorrectly and skip explicit login screen when session was missing
- Token clearing flow did not consistently enforce LIVE + login
- Reset/clear flows could leave stale mode/navigation state that reopened Demo
- Several user-facing English strings remained visible on key screens

### Technical changes
- Launch routing tightened in `app/src/main/java/com/libredisplay/AppLaunchResolver.kt`
- Startup/navigation state handling updated in `app/src/main/java/com/libredisplay/MainActivity.kt`
- Monitoring screen updated in `app/src/main/java/com/libredisplay/ui/monitoring/MonitoringScreen.kt`
- Privacy logic hardened in `app/src/main/java/com/libredisplay/data/repository/PrivacyRepository.kt`
- Privacy ViewModel and UI updated for explicit mode handling
- Settings login flow improved with Polish error messages

### Tests
- `./gradlew clean`: PASS
- `./gradlew testDebugUnitTest`: PASS
- `./gradlew lint`: PASS
- `./gradlew connectedDebugAndroidTest`: Not executed because no device or emulator was connected.
- `./gradlew assembleDebug`: PASS
- `./gradlew assembleRelease`: PASS
- `./gradlew bundleRelease`: PASS

### Artifacts
Debug APK:
`C:\Users\SG0216827\IdeaProjects\LibreDisplay\app\build\outputs\apk\debug\app-debug.apk`
22,960,342 bytes (21.9 MiB)

Release APK:
`C:\Users\SG0216827\IdeaProjects\LibreDisplay\app\build\outputs\apk\release\app-release.apk`
2,933,630 bytes (2.8 MiB)

Release AAB:
`C:\Users\SG0216827\IdeaProjects\LibreDisplay\app\build\outputs\bundle\release\app-release.aab`
5,352,329 bytes (5.1 MiB)

Google Play upload artifact:
`C:\Users\SG0216827\IdeaProjects\LibreDisplay\app\build\outputs\bundle\release\app-release.aab`
5,352,329 bytes (5.1 MiB)

LibreCare-named artifact copies:
- `release-artifacts/LibreCare-1.2.1-debug.apk` (21.9 MiB)
- `release-artifacts/LibreCare-1.2.1-release.apk` (2.8 MiB)
- `release-artifacts/LibreCare-1.2.1-release.aab` (5.1 MiB)

### Known limitations
- No physical device connected for testing
- Requires testing on real device/emulator before Google Play release
- Requires manual verification in Google Play Console for login and reset flows
- Some UI scenarios not covered by end-to-end tests

### Manual follow-up
- Test full login flow on real device with production LibreLinkUp credentials
- Verify token clear -> relogin path against real API rate-limit behavior
- Upload release AAB to Google Play Internal Testing
- Update screenshots/reviewer notes if required
- Verify and update Privacy Policy if required
- Verify and update Health Apps Declaration if required

