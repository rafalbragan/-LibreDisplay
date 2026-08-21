# LibreCare 2.2.3 - Release Notes

## PL

### Wersja
- previous: `2.2.2 (22)`
- current: `2.2.3 (23)`

### Nowe funkcje
- Dodano testy regresyjne dla importu kopii zapasowej z uszkodzonym JSON i z niezgodnym typem danych.

### Poprawki i usprawnienia
- Naprawiono przywracanie kopii zapasowej: obsluga bledow parsowania i niezgodnego ksztaltu danych eliminuje awarie typu `cannot be cast to k3`.
- W przywracaniu backupu uszkodzone rekordy sa pomijane bez przerywania calej operacji, z zapisem ostrzezen w logach diagnostycznych.
- Suwak Home dziala w stalej domenie 24h: dla 12h uchwyt zajmuje polowe toru, a przesuwanie jest przewidywalne.
- Oś czasu Home ma 5 etykiet i utrzymuje format godzinowy `HH:mm`.
- Domyslne okno danych dla Home rozszerzono do 24h, aby przycisk `24h` pokazywal pelny zakres.

### Testy
- Wykonano: `./gradlew clean` — PASS
- Wykonano: `./gradlew testDebugUnitTest` — PASS
- Wykonano: `./gradlew lint` — PASS
- Wykonano: `./gradlew assembleDebug` — PASS
- Wykonano: `./gradlew assembleRelease` — PASS
- Wykonano: `./gradlew bundleRelease` — PASS
- Testy podlaczone: `No connected device/emulator available.` (`adb` niedostepne w PATH).

### Artefakty
- `release-artifacts/LibreCare-2.2.3-debug.apk` (23 480 386 B)
- `release-artifacts/LibreCare-2.2.3-release.apk` (3 178 014 B)
- `release-artifacts/LibreCare-2.2.3-release.aab` (5 814 564 B)

### Znane ograniczenia
- Brak testow instrumentowanych (brak dostepnego urzadzenia/emulatora w srodowisku).

---

## EN

### Version
- previous: `2.2.2 (22)`
- current: `2.2.3 (23)`

### New Features
- Added regression tests for backup restore with malformed JSON and payload type mismatches.

### Fixes & Improvements
- Fixed backup restore by handling parse errors and incompatible payload shapes to prevent `cannot be cast to k3` crashes.
- Corrupted backup rows are now skipped safely (with diagnostic warnings) instead of aborting the whole restore flow.
- Home navigator now uses a fixed 24h domain, so the 12h viewport occupies half of the track.
- Home time axis now renders 5 labels while keeping `HH:mm` readability.
- Default Home source window was extended to 24h so the `24h` selector has full data coverage.

### Tests
- Ran: `./gradlew clean` — PASS
- Ran: `./gradlew testDebugUnitTest` — PASS
- Ran: `./gradlew lint` — PASS
- Ran: `./gradlew assembleDebug` — PASS
- Ran: `./gradlew assembleRelease` — PASS
- Ran: `./gradlew bundleRelease` — PASS
- Connected tests: `No connected device/emulator available.` (`adb` not in PATH).

### Artifacts
- `release-artifacts/LibreCare-2.2.3-debug.apk` (23,480,386 B)
- `release-artifacts/LibreCare-2.2.3-release.apk` (3,178,014 B)
- `release-artifacts/LibreCare-2.2.3-release.aab` (5,814,564 B)

### Known Limitations
- No instrumented tests (no connected device/emulator in this environment).

