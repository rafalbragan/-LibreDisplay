# LibreCare 2.16.3 - Release Notes

## PL

### Wersja
- VersionName: `2.16.3`
- VersionCode: `47`
- Build channel: `TEST` (domyslnie; nadpisywane przez `LIBRECARE_BUILD_CHANNEL`)

### Nowe funkcje
- Diagnostyka: dodano akcje `Zapisz log do pliku` (Storage Access Framework) i podglad `Ostatni raport awarii` po restarcie aplikacji.
- About/Features: widoczne metadane builda (`COMMIT_SHA`, `BUILD_CHANNEL`) oraz kompaktowa historia wersji.

### Ulepszenia
- Chart 2.0 na Home: drag do panowania viewportu, pinch-to-zoom, double-tap zoom, strzalki nawigacji i `Najnowsze`.
- Wejscie do pelnego ekranu jest jawne (`Pelny ekran`).
- Konflikt gestow ograniczony: wybor punktu tylko przez long press + drag.

### Naprawy
- Ograniczono ryzyko pokazywania nieaktualnego odczytu jako biezacej glikemii (`Brak aktualnych danych`).
- Eksport/udostepnianie logow wykorzystuje redakcje danych wrazliwych.

### Testy
- Uruchomiono: `./gradlew clean`, `./gradlew testDebugUnitTest`, `./gradlew lint`, `./gradlew assembleDebug`, `./gradlew assembleRelease`, `./gradlew bundleRelease`.
- Status: **PASS** (lokalny proces uruchomiony na JDK 17).
- Testy urzadzeniowe: `./gradlew connectedDebugAndroidTest` -> **FAIL** (`HomeInstrumentedUiTest`: `NoSuchMethodException: android.hardware.input.InputManager.getInstance` na `SM-S948B - 17`).

### Artefakty
- `release-artifacts/LibreCare-2.16.3-debug.apk` (`23,924,701 B`, SHA-256: `80B305AD335F007A628947A0A09661D8E60BC4E4FCD37BE3B202106AEB6D1BC3`)
- `release-artifacts/LibreCare-2.16.3-release.apk` (`3,695,754 B`, SHA-256: `714103C716485F30FE5975B109B71BA68E362791B2E701BF99CE45C77CAAFAB8`)
- `release-artifacts/LibreCare-2.16.3-release.aab` (`6,721,080 B`, SHA-256: `D7BF96D467B529913A53210BFB558754589CE86C77CE889A9E538CA781EBB324`)

### Znane ograniczenia
- Gesty wykresu i scenariusz #29 wymagaja walidacji manualnej na danych produkcyjnych/realnym koncie.
- Failing connected test wymaga dostosowania testu instrumentacyjnego do Android 17 (`InputManager.getInstance`).

## EN

### Version
- VersionName: `2.16.3`
- VersionCode: `47`
- Build channel: `TEST` by default (overridable via `LIBRECARE_BUILD_CHANNEL`)

### New features
- Diagnostics: added `Save log to file` (Storage Access Framework) and `Last crash report` preview after app restart.
- About/Features: build metadata (`COMMIT_SHA`, `BUILD_CHANNEL`) and compact version history are now visible.

### Improvements
- Chart 2.0 on Home: drag-to-pan viewport, pinch-to-zoom, double-tap zoom, arrow navigation and `Latest` action.
- Full-screen chart entry is now explicit (`Fullscreen`).
- Gesture conflict reduction: point selection now uses long-press + drag only.

### Fixes
- Reduced stale-data risk by not presenting outdated readings as current glucose (`No current data`).
- Log share/export now uses sensitive-data redaction.

### Tests
- Executed: `./gradlew clean`, `./gradlew testDebugUnitTest`, `./gradlew lint`, `./gradlew assembleDebug`, `./gradlew assembleRelease`, `./gradlew bundleRelease`.
- Status: **PASS** (local run switched to JDK 17).
- Device tests: `./gradlew connectedDebugAndroidTest` -> **FAIL** (`HomeInstrumentedUiTest`: `NoSuchMethodException: android.hardware.input.InputManager.getInstance` on `SM-S948B - 17`).

### Artifacts
- `release-artifacts/LibreCare-2.16.3-debug.apk` (`23,924,701 B`, SHA-256: `80B305AD335F007A628947A0A09661D8E60BC4E4FCD37BE3B202106AEB6D1BC3`)
- `release-artifacts/LibreCare-2.16.3-release.apk` (`3,695,754 B`, SHA-256: `714103C716485F30FE5975B109B71BA68E362791B2E701BF99CE45C77CAAFAB8`)
- `release-artifacts/LibreCare-2.16.3-release.aab` (`6,721,080 B`, SHA-256: `D7BF96D467B529913A53210BFB558754589CE86C77CE889A9E538CA781EBB324`)

### Known limitations
- Chart gestures and issue #29 still require manual validation on realistic account data.
- Failing connected test needs instrumentation compatibility update for Android 17 (`InputManager.getInstance`).

