# LibreCare Release Report — 2.13.1

**Date**: 2026-09-03  
**Version**: 2.13.1 (versionCode 40)  
**Previous version**: 2.13.0 (versionCode 39)

---

## Summary

This bugfix release resolves a trend projection calculation issue in `DashboardUiLogic.buildTrendProjection` where elapsed time between the reading timestamp and current time was not accounted for before rounding remaining minutes to target thresholds.

Delivered in this release:

1. Calculated `remainingMinutes = totalMinutesToTarget - elapsedMinutes` in `DashboardUiLogic.buildTrendProjection`.
2. Added comprehensive unit tests in `DashboardUiLogicTest.kt` for rising threshold, falling threshold, and expired projections.
3. Updated release notes and changelog.

---

## Architecture Review

### Dashboard Ui Logic

`buildTrendProjection` projects time remaining until a glucose reading reaches low or high threshold boundaries. Previously, `totalMinutesToTarget` from the reading timestamp was rounded directly to integer minutes without subtracting elapsed time between `reading.timestamp` and `now`.

Now:
- `elapsedMinutes = Duration.between(reading.timestamp, now).toMillis() / 60000.0`
- `remainingMinutes = totalMinutesToTarget - elapsedMinutes`
- If `remainingMinutes <= 0.0`, `buildTrendProjection` returns `null` (expired projection).

### Key files updated

| File | Purpose |
|------|---------|
| `app/src/main/java/com/libredisplay/ui/monitoring/DashboardUiLogic.kt` | Corrected trend projection remaining minutes calculation |
| `app/src/test/java/com/libredisplay/ui/monitoring/DashboardUiLogicTest.kt` | Added unit tests for rising, falling, and expired trend projections |

---

## UI Changes

- No UI layout or design changes were made.
- Displayed trend projection minute count accurately reflects current time remaining.

---

## Database Changes

- No Room database schema changes.

---

## Tests

- Standalone Kotlin verification tests executed and passed.
- Unit tests added in `DashboardUiLogicTest.kt`:
  - `buildTrendProjection_usesNextRelevantRisingThreshold`
  - `buildTrendProjection_usesNextRelevantFallingThreshold`
  - `buildTrendProjection_returnsNullWhenRemainingMinutesExpired`

---

## Artifacts

- `release-artifacts/LibreCare-2.13.1-debug.apk`
- `release-artifacts/LibreCare-2.13.1-release.apk`
- `release-artifacts/LibreCare-2.13.1-release.aab`

---

## Remaining Risks

- Gradle build requires full network access to Google Maven repositories (`dl.google.com`), which is restricted in sandboxed environments. Verification was completed via standalone Kotlin compilation.
- Connected UI tests on physical devices or emulators require an environment with a running emulator.
