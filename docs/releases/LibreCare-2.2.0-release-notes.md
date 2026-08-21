# LibreCare 2.2.0 - Release Notes

## PL

### Wersja
- previous: `2.1.1 (19)`
- current: `2.2.0 (20)`

### Nowe funkcje
- Dodano niezależny od `Historia` selektor zakresu wykresu Home: `1h`, `3h`, `6h`, `9h`, `12h`.
- Dodano subtelny navigator czasu pod wykresem Home do przesuwania widocznego okna w obrębie dostępnych 12 godzin.
- Dodano dostępność danych `12 h / 24 h` przed wykresem Home.

### Poprawki i usprawnienia
- Przebudowano Home tak, aby najpierw rozwiązać strukturę layoutu i responsywność, zamiast dalszego zmniejszania fontów.
- Zwiększono typografię kluczowych sekcji: tytułu aplikacji, statusu danych, bloku aktualnej glikemii, metryk i etykiet osi.
- Blok `Aktualna glikemia` zawija trend do kolejnej linii, gdy szerokość ekranu jest zbyt mała, zamiast ściskać wartość i jednostkę.
- Metryki Home używają teraz adaptacyjnego układu `3 + 2` na węższych ekranach i nie ścinają wartości medycznych ani czasów.
- Home chart używa oddzielnego stanu zakresu od `Historia`, wspiera pinch-to-zoom osi X z dosnapowaniem do `1/3/6/9/12h` i double tap do powrotu do najnowszego fragmentu.
- `Główna`, `Historia` i `Ustawienia` działają jak top-level destinations z przewidywalnym back stackiem.

### Testy
- Wykonano: `./gradlew clean`
- Wykonano: `./gradlew testDebugUnitTest`
- Wykonano: `./gradlew lint`
- Wykonano: `./gradlew assembleDebug`
- Wykonano: `./gradlew assembleRelease`
- Wykonano: `./gradlew bundleRelease`
- `connectedDebugAndroidTest`: nie uruchomiono — **No connected device/emulator available.**

### Artefakty
- DEBUG APK: `C:\Users\SG0216827\IdeaProjects\LibreDisplay\release-artifacts\LibreCare-2.2.0-debug.apk` (23 447 625 B)
- RELEASE APK: `C:\Users\SG0216827\IdeaProjects\LibreDisplay\release-artifacts\LibreCare-2.2.0-release.apk` (3 161 630 B)
- RELEASE AAB: `C:\Users\SG0216827\IdeaProjects\LibreDisplay\release-artifacts\LibreCare-2.2.0-release.aab` (5 797 368 B)
- Google Play upload file: `C:\Users\SG0216827\IdeaProjects\LibreDisplay\release-artifacts\LibreCare-2.2.0-release.aab`

### Znane ograniczenia
- Nie wykonano walidacji ręcznej na emulatorze/urządzeniu dla `360dp / 384dp / 411dp` i `fontScale 1.0 / 1.3 / 1.5`, ponieważ `adb` nie jest dostępne w bieżącym środowisku.
- Nie dodano pełnych screenshot/golden tests; obecna walidacja layoutu opiera się na testach logiki, regresjach i pełnym buildzie.

## EN

### Version
- previous: `2.1.1 (19)`
- current: `2.2.0 (20)`

### New features
- Added a Home-only chart range selector: `1h`, `3h`, `6h`, `9h`, `12h`.
- Added a subtle time navigator below the Home chart to pan the visible window within the available 12-hour timeline.
- Added explicit `12 h / 24 h` data availability above the Home chart.

### Fixes and improvements
- Reworked Home to solve layout structure and responsiveness first, instead of shrinking fonts further.
- Increased typography for the app title, data status, current glucose block, metrics, and axis labels.
- The `Current glucose` block now wraps trend information onto the next line when width is tight instead of compressing the value and unit.
- Home metrics now use an adaptive `3 + 2` layout on narrower screens and do not clip medical values or durations.
- Home chart now uses a range state independent from `History`, supports X-axis pinch-to-zoom snapped to `1/3/6/9/12h`, and double tap to jump back to the latest segment.
- `Home`, `History`, and `Settings` now behave as top-level destinations with a predictable back stack.

### Tests
- Executed: `./gradlew clean`
- Executed: `./gradlew testDebugUnitTest`
- Executed: `./gradlew lint`
- Executed: `./gradlew assembleDebug`
- Executed: `./gradlew assembleRelease`
- Executed: `./gradlew bundleRelease`
- `connectedDebugAndroidTest`: not executed — **No connected device/emulator available.**

### Artifacts
- DEBUG APK: `C:\Users\SG0216827\IdeaProjects\LibreDisplay\release-artifacts\LibreCare-2.2.0-debug.apk` (23,447,625 B)
- RELEASE APK: `C:\Users\SG0216827\IdeaProjects\LibreDisplay\release-artifacts\LibreCare-2.2.0-release.apk` (3,161,630 B)
- RELEASE AAB: `C:\Users\SG0216827\IdeaProjects\LibreDisplay\release-artifacts\LibreCare-2.2.0-release.aab` (5,797,368 B)
- Google Play upload file: `C:\Users\SG0216827\IdeaProjects\LibreDisplay\release-artifacts\LibreCare-2.2.0-release.aab`

### Known limitations
- Manual emulator/device validation for `360dp / 384dp / 411dp` and `fontScale 1.0 / 1.3 / 1.5` could not be executed because `adb` is not available in the current environment.
- Full screenshot/golden coverage was not added in this iteration; current layout verification relies on logic tests, regressions, and the full build sequence.

