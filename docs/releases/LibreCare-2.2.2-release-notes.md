# LibreCare 2.2.2 - Release Notes

## PL

### Wersja
- previous: `2.2.1 (21)`
- current: `2.2.2 (22)`

### Nowe funkcje
- Brak nowych funkcji – wydanie skoncentrowane na stabilności i jakości artefaktów.

### Poprawki i usprawnienia
- Pełny cykl release: testy jednostkowe, lint, assembleRelease, bundleRelease – wszystkie zakończone sukcesem.
- Artefakty zweryfikowane i skopiowane do `release-artifacts/`.

### Testy
- Wykonano: `./gradlew testDebugUnitTest` — PASS (BUILD SUCCESSFUL)
- Wykonano: `./gradlew lint` — PASS (BUILD SUCCESSFUL, brak błędów)
- Wykonano: `./gradlew assembleDebug` — PASS
- Wykonano: `./gradlew assembleRelease` — PASS
- Wykonano: `./gradlew bundleRelease` — PASS
- Testy podłączone: brak urządzenia/emulatora.

### Artefakty
- `release-artifacts/LibreCare-2.2.2-debug.apk` (22,39 MB)
- `release-artifacts/LibreCare-2.2.2-release.apk` (3,03 MB)
- `release-artifacts/LibreCare-2.2.2-release.aab` (5,55 MB)

### Znane ograniczenia
- Brak testów instrumentowanych (brak urządzenia/emulatora w środowisku CI).

---

## EN

### Version
- previous: `2.2.1 (21)`
- current: `2.2.2 (22)`

### New Features
- No new features – stability and artifact quality release.

### Fixes & Improvements
- Full release cycle: unit tests, lint, assembleRelease, bundleRelease – all passed.
- Artifacts verified and copied to `release-artifacts/`.

### Tests
- Ran: `./gradlew testDebugUnitTest` — PASS (BUILD SUCCESSFUL)
- Ran: `./gradlew lint` — PASS (BUILD SUCCESSFUL, no errors)
- Ran: `./gradlew assembleDebug` — PASS
- Ran: `./gradlew assembleRelease` — PASS
- Ran: `./gradlew bundleRelease` — PASS
- Connected tests: no device/emulator available.

### Artifacts
- `release-artifacts/LibreCare-2.2.2-debug.apk` (22.39 MB)
- `release-artifacts/LibreCare-2.2.2-release.apk` (3.03 MB)
- `release-artifacts/LibreCare-2.2.2-release.aab` (5.55 MB)

### Known Limitations
- No instrumented tests (no device/emulator available in build environment).

