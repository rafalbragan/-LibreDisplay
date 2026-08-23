# LibreCare 2.3.0 Release Notes

## PL

### Wersja
- `versionName`: 2.3.0
- `versionCode`: 25

### Nowe funkcje
- Podglad kopii przed przywroceniem (osoby, liczba odczytow, wybor ustawien).
- Selektywny eksport kopii (wybor osob i ustawien przed zapisem pliku).
- Long-press drag metryk Home (zmiana kolejnosci metryk).

### Poprawki
- Restore backupu dziala selektywnie i nie kasuje globalnie danych LIVE.
- Dostepny tryb `Polacz dane` / `Zastap lokalne dane` dla osob juz istniejacych lokalnie.
- Metryki Home sa liczone z aktualnie widocznego okna wykresu.

### Usprawnienia
- Dodano zwięzly pasek TIR pod wykresem Home.
- Utrzymano szyfrowanie kopii `.librecarebackup` (AES-GCM + PBKDF2).

### Testy
- `./gradlew clean` — PASS
- `./gradlew testDebugUnitTest` — PASS
- `./gradlew lint` — PASS
- `./gradlew assembleDebug` — PASS
- `./gradlew assembleRelease` — PASS
- `./gradlew bundleRelease` — PASS
- Connected: `No connected device/emulator available.`

### Artefakty
- `release-artifacts/LibreCare-2.3.0-debug.apk` (23 545 943 B)
- `release-artifacts/LibreCare-2.3.0-release.apk` (3 194 389 B)
- `release-artifacts/LibreCare-2.3.0-release.aab` (5 895 399 B)

### Znane ograniczenia
- Brakuje pelnej macierzy testow UI/chart/navigation w `androidTest`.
- Brakuje testu release z rzeczywistym R8 regression dla scenariusza cast.

## EN

### Version
- `versionName`: 2.3.0
- `versionCode`: 25

### New Features
- Backup restore preview (people, reading counts, settings toggle).
- Selective backup export (people/settings selection before saving).
- Long-press metric reordering on Home.

### Fixes
- Backup restore applies selected scope and does not globally wipe LIVE data.
- `Merge` / `Replace local data` mode for existing people.
- Home metrics are computed from the currently visible chart window.

### Improvements
- Added a compact TIR bar under the Home chart.
- Preserved encrypted `.librecarebackup` format (AES-GCM + PBKDF2).

### Tests
- `./gradlew clean` — PASS
- `./gradlew testDebugUnitTest` — PASS
- `./gradlew lint` — PASS
- `./gradlew assembleDebug` — PASS
- `./gradlew assembleRelease` — PASS
- `./gradlew bundleRelease` — PASS
- Connected: `No connected device/emulator available.`

### Artifacts
- `release-artifacts/LibreCare-2.3.0-debug.apk` (23,545,943 B)
- `release-artifacts/LibreCare-2.3.0-release.apk` (3,194,389 B)
- `release-artifacts/LibreCare-2.3.0-release.aab` (5,895,399 B)

### Known limitations
- Full UI/chart/navigation instrumentation matrix is not yet present in `androidTest`.
- Missing explicit release/R8 cast-regression test execution evidence.

