# LibreCare 2.12.0 - Release Notes

Data wydania / Release date: 2026-08-27

## PL

### Wersja
- `versionName`: `2.12.0`
- `versionCode`: `38`

### Nowe funkcje
- Dodano nową zakładkę `Futures` jako 4. pozycję nawigacji głównej.
- Dodano ekran `Futures`, który zbiera w jednym miejscu prototypy i kierunki rozwoju dla:
  - `Analiza+ do wdrożenia`
  - `Wyjaśnienia skoków glikemii`
  - `Ryzyko hipoglikemii`
  - `Wzorce zmienności`
  - `Reakcja na posiłki`
  - `Tygodniowa karta postępów`
  - `Aktywność sensora i luki danych`
  - `Osiągnięcia i serie`
  - `Udostępnij lekarzowi`
  - `Ekrany Senior / Opiekun / Lekarz`
- Dodano filtr perspektywy `Wszyscy / Pacjent / Senior / Opiekun / Lekarz`, aby łatwiej ocenić pomysły dla różnych odbiorców.

### Poprawki
- Utrzymano bez zmian bieżącą funkcjonalność ekranów `Główna`, `Analiza` i `Ustawienia` — nowa karta ma charakter prototypowy i nie zastępuje istniejących przepływów.

### Ulepszenia
- Dolna nawigacja i landscape rail wspierają teraz 4. destination top-level bez zmiany istniejącej architektury stack-based.
- Dodano test tagi dla ekranu `Futures` i filtrów odbiorców, aby nowe UI dało się stabilnie testować automatycznie.
- Zaktualizowano dokumentację projektową dla `DEC-0007` i ścieżki wdrożenia funkcji eksperymentalnych.

### Testy
- `FuturesViewModelTest`
- `FuturesScreenTest`
- `AppNavigationStateTest`
- `./gradlew clean`
- `./gradlew testDebugUnitTest`
- `./gradlew lint`
- `./gradlew assembleDebug`
- `./gradlew assembleRelease`
- `./gradlew bundleRelease`
- Connected tests: nie uruchomiono — `adb` niedostępne, brak potwierdzonego urządzenia/emulatora.

### Artefakty
- DEBUG APK: `C:\Users\SG0216827\IdeaProjects\LibreDisplay\release-artifacts\LibreCare-2.12.0-debug.apk` — `23859171` B (`22,75 MB`)
- RELEASE APK: `C:\Users\SG0216827\IdeaProjects\LibreDisplay\release-artifacts\LibreCare-2.12.0-release.apk` — `3695756` B (`3,52 MB`)
- RELEASE AAB: `C:\Users\SG0216827\IdeaProjects\LibreDisplay\release-artifacts\LibreCare-2.12.0-release.aab` — `6688026` B (`6,38 MB`)
- Google Play upload file: `C:\Users\SG0216827\IdeaProjects\LibreDisplay\release-artifacts\LibreCare-2.12.0-release.aab`

### Znane ograniczenia
- Zakładka `Futures` prezentuje kierunki produktu i prototypowe treści; nie implementuje jeszcze wszystkich docelowych danych, wykresów i przepływów opisanych w kartach.
- Brak uruchomionych testów connected/instrumented na urządzeniu w tej sesji.

## EN

### Version
- `versionName`: `2.12.0`
- `versionCode`: `38`

### New features
- Added a new `Futures` tab as the 4th main navigation destination.
- Added a `Futures` screen that groups prototype ideas and roadmap directions for:
  - `Analysis+ implementation`
  - `Glucose spike explanations`
  - `Hypoglycemia risk`
  - `Variability patterns`
  - `Meal response`
  - `Weekly progress card`
  - `Sensor activity and data gaps`
  - `Achievements and streaks`
  - `Share with doctor`
  - `Senior / Caregiver / Clinician screens`
- Added the audience filter `All / Patient / Senior / Caregiver / Clinician` for faster persona-based review.

### Fixes
- Preserved the current `Home`, `Analysis`, and `Settings` functionality — the new tab is a prototype area and does not replace existing flows.

### Improvements
- Bottom navigation and landscape rail now support a 4th top-level destination without changing the existing stack-based architecture.
- Added test tags for the `Futures` screen and audience filters so the new UI can be covered with stable automated tests.
- Updated product design documentation for `DEC-0007` and the experimental-feature rollout path.

### Tests
- `FuturesViewModelTest`
- `FuturesScreenTest`
- `AppNavigationStateTest`
- `./gradlew clean`
- `./gradlew testDebugUnitTest`
- `./gradlew lint`
- `./gradlew assembleDebug`
- `./gradlew assembleRelease`
- `./gradlew bundleRelease`
- Connected tests: not run — `adb` was unavailable and no confirmed device/emulator environment was available.

### Artifacts
- DEBUG APK: `C:\Users\SG0216827\IdeaProjects\LibreDisplay\release-artifacts\LibreCare-2.12.0-debug.apk` — `23859171` B (`22.75 MB`)
- RELEASE APK: `C:\Users\SG0216827\IdeaProjects\LibreDisplay\release-artifacts\LibreCare-2.12.0-release.apk` — `3695756` B (`3.52 MB`)
- RELEASE AAB: `C:\Users\SG0216827\IdeaProjects\LibreDisplay\release-artifacts\LibreCare-2.12.0-release.aab` — `6688026` B (`6.38 MB`)
- Google Play upload file: `C:\Users\SG0216827\IdeaProjects\LibreDisplay\release-artifacts\LibreCare-2.12.0-release.aab`

### Known limitations
- The `Futures` tab currently presents product directions and prototype content; it does not yet implement every data flow, chart, and advanced workflow described in the cards.
- Connected/instrumented tests were not executed on a device in this session.


