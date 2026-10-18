# LibreCare 2.16.3 - Release Notes

## PL

### Wersja
- versionName: `2.16.3`
- versionCode: `47`
- Data: `2026-10-18`

### Nowe funkcje
- Dodano profil DEMO `Mika` z deterministycznym generatorem historii glikemii obejmujacej ok. 18 miesiecy.
- Dodano stale oznaczenie na Home/Monitoring: `MIKA - DANE SYMULOWANE / DEMO`.

### Poprawki
- Naprawiono P0 crash-loop po wybraniu osoby z nieaktualnymi/brakujacymi danymi: aplikacja przechodzi do stanu bezpiecznego `Brak aktualnych danych`.
- Dodano zabezpieczenie backupu lokalnego przed nadpisaniem ostatniej dobrej kopii pustym stanem.
- Rozdzielono sygnal `plik zapisany` od `kopia nadaje sie do przywrocenia` (status `restorable`).

### Ulepszenia
- Dodano testy regresyjne dla fallbacku selectedPatientId, stanu recovery po crashu i ochrony backupu.
- Profil Mika jest aktywowany tylko przy swiadomym build-flag (`ENABLE_MIKA_DEMO_PROFILE=true`).

### Testy
- `./gradlew testDebugUnitTest` - PASS
- `./gradlew lint` - PASS
- `./gradlew assembleDebug` - PASS
- `./gradlew assembleRelease` - PASS
- `./gradlew bundleRelease` - PASS
- Connected tests: `No connected device/emulator available.`

### Artefakty
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
- Release APK: `app/build/outputs/apk/release/app-release.apk`
- Release AAB: `app/build/outputs/bundle/release/app-release.aab`
- Google Play upload file: `app/build/outputs/bundle/release/app-release.aab`

### Ograniczenia / Blockery
- Google Sign-In (Credential Manager) - **IN PROGRESS** (wymaga finalnej konfiguracji OAuth po stronie Google Cloud).
- Google Drive backup/restore - **IN PROGRESS** (nie wdrozono pelnego przeplywu OAuth + szyfrowanie + restore po reinstall).

## EN

### Version
- versionName: `2.16.3`
- versionCode: `47`
- Date: `2026-10-18`

### New Features
- Added DEMO profile `Mika` with deterministic synthetic glucose history (~18 months).
- Added persistent Home/Monitoring label: `MIKA - DANE SYMULOWANE / DEMO`.

### Fixes
- Fixed P0 crash-loop when selecting a person with stale/missing data; app now shows safe `No current data` state.
- Added local backup protection against overwriting the last good backup with an empty snapshot.
- Separated `file written` from `backup restorable` diagnostics (`restorable` status).

### Improvements
- Added regression tests for selectedPatientId fallback, crash-recovery state, and backup overwrite guard.
- Mika profile is exposed only with explicit test-build flag (`ENABLE_MIKA_DEMO_PROFILE=true`).

### Tests
- `./gradlew testDebugUnitTest` - PASS
- `./gradlew lint` - PASS
- `./gradlew assembleDebug` - PASS
- `./gradlew assembleRelease` - PASS
- `./gradlew bundleRelease` - PASS
- Connected tests: `No connected device/emulator available.`

### Artifacts
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
- Release APK: `app/build/outputs/apk/release/app-release.apk`
- Release AAB: `app/build/outputs/bundle/release/app-release.aab`
- Google Play upload file: `app/build/outputs/bundle/release/app-release.aab`

### Known Limitations / Blockers
- Google Sign-In (Credential Manager) - **IN PROGRESS** (requires production OAuth configuration in Google Cloud).
- Google Drive backup/restore - **IN PROGRESS** (full OAuth + encryption + reinstall restore flow pending).

