# LibreCare 2.16.2 - Release Notes

## PL

### Wersja
- VersionName: `2.16.2`
- VersionCode: `46`

### Fixes
- Dodano brakujacy zakres `1h` dla historii Monitoring.
- Dodano mini-nawigator z widocznym oknem zakresu i przesuwaniem okna czasu.
- Dodano przycisk `Wroc do najnowszych danych` dla szybkiego powrotu do aktualnego okna historii.

### New Features
- Zaimplementowano zaakceptowany zakres REQ-0004 dla nawigacji wykresow Home/Monitoring.

### Improvements
- Ograniczono selektor zakresow Monitoring do `1h`, `3h`, `6h`, `12h`, `24h` zgodnie z decyzja PO.
- Zachowano rozdzielenie stanu viewport/zoom/selection od danych medycznych.

### Tests
- `python scripts/product/product_cli.py validate` - PASS
- `python scripts/product/product_cli.py summary` - PASS
- `./gradlew clean` - PASS
- `./gradlew testDebugUnitTest` - PASS
- `./gradlew lint` - PASS
- `./gradlew assembleDebug` - PASS
- `./gradlew assembleRelease` - PASS
- `./gradlew bundleRelease` - PASS
- `./gradlew assembleDebugAndroidTest` - PASS
- No connected device/emulator available.

### Artifacts
- Debug APK: `release-artifacts/LibreCare-2.16.2-debug.apk` (`23,891,942 B`)
- Release APK: `release-artifacts/LibreCare-2.16.2-release.apk` (`3,695,754 B`)
- Release AAB: `release-artifacts/LibreCare-2.16.2-release.aab` (`6,683,403 B`)
- Google Play Upload File: `release-artifacts/LibreCare-2.16.2-release.aab`

### Known Limitations
- Brak podlaczonego emulatora/urzadzenia - testy connected UI nie zostaly uruchomione.

## EN

### Version
- VersionName: `2.16.2`
- VersionCode: `46`

### Fixes
- Added missing `1h` range for Monitoring history.
- Added a mini navigator with a visible draggable viewport window.
- Added a `Wroc do najnowszych danych` button to return to the latest history window.

### New Features
- Implemented accepted REQ-0004 scope for Home/Monitoring chart navigation.

### Improvements
- Constrained Monitoring range selector to `1h`, `3h`, `6h`, `12h`, `24h` per Product Owner decision.
- Preserved separation of viewport/zoom/selection state from medical data.

### Tests
- `python scripts/product/product_cli.py validate` - PASS
- `python scripts/product/product_cli.py summary` - PASS
- `./gradlew clean` - PASS
- `./gradlew testDebugUnitTest` - PASS
- `./gradlew lint` - PASS
- `./gradlew assembleDebug` - PASS
- `./gradlew assembleRelease` - PASS
- `./gradlew bundleRelease` - PASS
- `./gradlew assembleDebugAndroidTest` - PASS
- No connected device/emulator available.

### Artifacts
- Debug APK: `release-artifacts/LibreCare-2.16.2-debug.apk` (`23,891,942 B`)
- Release APK: `release-artifacts/LibreCare-2.16.2-release.apk` (`3,695,754 B`)
- Release AAB: `release-artifacts/LibreCare-2.16.2-release.aab` (`6,683,403 B`)
- Google Play Upload File: `release-artifacts/LibreCare-2.16.2-release.aab`

### Known Limitations
- No connected emulator/device was available, so connected UI tests were not executed.

