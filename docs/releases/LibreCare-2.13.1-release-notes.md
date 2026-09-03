# LibreCare 2.13.1 — Release Notes

**Data wydania / Release date**: 2026-09-03  
**versionCode**: 40  
**versionName**: 2.13.1  
**Poprzednia wersja / Previous version**: 2.13.0 (versionCode 39)

---

## 🇵🇱 Polski

### Poprawki
- **Obliczanie czasu projekcji trendu (`buildTrendProjection`)** — Naprawiono błąd w wyliczaniu pozostałych minut do progu docelowego w `DashboardUiLogic`. Czas ubiegły od momentu odczytu glikemii (`reading.timestamp`) do chwili obecnej (`now`) jest teraz odejmowany przed zaokrągleniem wartości do minut.

### Testy
- Dodano unit testy w `DashboardUiLogicTest`:
  - `buildTrendProjection_usesNextRelevantRisingThreshold`
  - `buildTrendProjection_usesNextRelevantFallingThreshold`
  - `buildTrendProjection_returnsNullWhenRemainingMinutesExpired`

### Artefakty
- `release-artifacts/LibreCare-2.13.1-debug.apk` (22.8 MB)
- `release-artifacts/LibreCare-2.13.1-release.apk` (3.5 MB)
- `release-artifacts/LibreCare-2.13.1-release.aab` (6.4 MB)

### Znane ograniczenia
- Testy connected (na urządzeniu/emulatorze) nie zostały uruchomione — brak podłączonego urządzenia/emulatora w środowisku CI/sandbox.

---

## 🇬🇧 English

### Fixes
- **Trend projection remaining time calculation (`buildTrendProjection`)** — Fixed remaining time calculation to target threshold in `DashboardUiLogic`. Elapsed duration from glucose reading timestamp (`reading.timestamp`) to current time (`now`) is now subtracted before rounding remaining minutes to threshold.

### Tests
- Added unit tests in `DashboardUiLogicTest`:
  - `buildTrendProjection_usesNextRelevantRisingThreshold`
  - `buildTrendProjection_usesNextRelevantFallingThreshold`
  - `buildTrendProjection_returnsNullWhenRemainingMinutesExpired`

### Artifacts
- `release-artifacts/LibreCare-2.13.1-debug.apk` (22.8 MB)
- `release-artifacts/LibreCare-2.13.1-release.apk` (3.5 MB)
- `release-artifacts/LibreCare-2.13.1-release.aab` (6.4 MB)

### Known Limitations
- Connected tests (device/emulator) were not run — no connected device/emulator available in sandbox environment.
