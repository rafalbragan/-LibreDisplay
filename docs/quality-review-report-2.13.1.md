# LibreCare Quality Review Report — 2.13.1

**Date**: 2026-09-03  
**Version**: 2.13.1 (versionCode 40)  
**Previous version**: 2.13.0 (versionCode 39)

---

## Summary

This quality review report evaluates the bugfix implemented for CI failure in GitHub Actions job "Fast test suite".

---

## Architecture Review

### Analysis & Root Cause

The GitHub Actions job "Fast test suite" failed on `DashboardUiLogicTest > buildTrendProjection_usesNextRelevantRisingThreshold`.

The test expected `minutesToThreshold` to be `2` when:
- `reading.timestamp` = 10:15:00Z
- `now` = 10:16:00Z (1 minute elapsed)
- Reading value = 170 mg/dL, target threshold = 180 mg/dL
- Trend rate = +3.0 mg/dL/min (+45 mg/dL / 15 min)

Total minutes from reading timestamp to target = (180 - 170) / 3.0 = 3.333 minutes.
When `now` is 10:16:00Z, 1 minute has already elapsed.
Remaining minutes from `now` = 3.333 - 1.0 = 2.333 minutes -> rounds to 2 minutes.

Without subtracting elapsed time, `3.333` was rounded to `3` minutes, causing the test failure.

### Resolution

In `DashboardUiLogic.kt`:
1. Calculate `elapsedMinutes = elapsedDuration.toMillis().toDouble() / 60000.0`.
2. Compute `remainingMinutes = totalMinutesToTarget - elapsedMinutes`.
3. If `remainingMinutes <= 0.0`, return `null`.
4. Return `TrendProjection` with `minutesToThreshold = remainingMinutes.roundToInt().coerceAtLeast(1)`.

---

## UI Changes

- No visual UI layout changes. Polish language formatting (`formatTrendProjectionMessage`) remains preserved.

---

## Database Changes

- None.

---

## Tests

- Added and verified unit tests in `DashboardUiLogicTest.kt`:
  - `buildTrendProjection_usesNextRelevantRisingThreshold` - PASS
  - `buildTrendProjection_usesNextRelevantFallingThreshold` - PASS
  - `buildTrendProjection_returnsNullWhenRemainingMinutesExpired` - PASS
- Code review performed via `code_review` tool: PASS (0 issues).
- CodeQL security scan checked via `codeql_checker` tool: PASS.

---

## Artifacts

- `release-artifacts/LibreCare-2.13.1-debug.apk`
- `release-artifacts/LibreCare-2.13.1-release.apk`
- `release-artifacts/LibreCare-2.13.1-release.aab`

---

## Remaining Risks

- Connected device tests require physical hardware or an active Android emulator.
