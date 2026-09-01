# LibreCare Release Report - 2.15.0

**Date**: 2026-08-31  
**Version**: 2.15.0 (versionCode 41)  
**Previous version**: 2.14.0 (versionCode 40)

---

## Summary

This release improves current glucose trend presentation on the Home screen by:

1. Showing numeric rate only for `RISING_FAST` / `FALLING_FAST`.
2. Deriving the displayed rate from real recent CGM readings using the same windowed slope pipeline as projection logic.
3. Projecting only the next meaningful threshold (configured low/high, `54 mg/dL`, `250 mg/dL`).
4. Suppressing ETA when data are stale, insufficient, unstable, inconsistent, or longer than `90 min`.

---

## Architecture Review

| Area | Status | Notes |
|------|--------|-------|
| Existing implementation | EXISTS | Reused existing monitoring card, trend-rate estimator, and warning pipeline. |
| Architecture | EXISTS | Added a pure shared projection model in `ui.monitoring` without changing feature boundaries. |
| ViewModels | EXISTS | Existing `MonitoringViewModel` continues to supply reading/settings; no state contract break. |
| Repositories | EXISTS | Existing `SettingsRepository` continues to provide low/high thresholds and trend window. |
| Room database | EXISTS | No schema changes. |
| Migrations | EXISTS | No migration required. |
| API layer | EXISTS | Existing LibreLinkUp acquisition untouched. |
| Demo Mode | INCOMPLETE | Projection logic is reusable in demo mode, but no dedicated demo-scenario assertions were added in this task. |
| Privacy & Data | EXISTS | No new data collection, export, or transfer introduced. |
| Statistics | EXISTS | Existing stats pipeline preserved; domain threshold alignment now matches `250 mg/dL`. |
| Widgets | EXISTS | Widget code unchanged. |
| Charts | EXISTS | Chart interaction code unchanged. |
| Compose screens | EXISTS | `RedesignedGlucoseCard` updated; logic kept out of Compose. |
| Navigation | EXISTS | No navigation changes. |

---

## UI Changes

### Home screen
- Current glucose remains the dominant element.
- Fast trends now show a separate `mg/dL/min` line.
- Projection text is shown only for short, meaningful ETAs.
- Projection no longer lives inside the medical-alert body.

### Warning behavior
- `very high glucose` warning now aligns to `>250 mg/dL`.
- For already very-high fast-rising values, the UI reports that glucose is already very high and still rising rapidly instead of projecting to arbitrary targets.

### Accessibility / readability review
- New user-facing strings added in Polish.
- Compact layout preserved; no extra card added.
- Existing semantics were extended to include rate and projection text when present.
- Landscape and phone-sized layout continue to use the same current-glucose card component.

---

## Database Changes

No Room schema changes. No migration required.

---

## Tests

| Command / scope | Result |
|-----------------|--------|
| `./gradlew clean` | PASS |
| `./gradlew testDebugUnitTest` | PASS |
| `./gradlew lint` | PASS |
| `./gradlew assembleDebug` | PASS |
| `./gradlew assembleRelease` | PASS |
| `./gradlew bundleRelease` | PASS |
| Focused monitoring tests | PASS |
| Connected tests | NOT RUN |

### Added/updated unit tests
- `app/src/test/java/com/libredisplay/ui/monitoring/TrendProjectionTest.kt`
- `app/src/test/java/com/libredisplay/ui/monitoring/DashboardUiLogicTest.kt`
- `app/src/test/java/com/libredisplay/ui/monitoring/GlucoseWarningUiTest.kt`
- `app/src/test/java/com/libredisplay/data/repository/GlucoseRepositoryTest.kt` (selected person without graph data)

**Connected tests note**: No connected device/emulator available. `adb` command was unavailable.

---

## Artifacts

| Artifact | Path | Size |
|----------|------|------|
| Debug APK | `C:\Users\SG0216827\IdeaProjects\LibreDisplay\release-artifacts\LibreCare-2.15.0-debug.apk` | `23891925` B |
| Release APK | `C:\Users\SG0216827\IdeaProjects\LibreDisplay\release-artifacts\LibreCare-2.15.0-release.apk` | `3695748` B |
| Release AAB | `C:\Users\SG0216827\IdeaProjects\LibreDisplay\release-artifacts\LibreCare-2.15.0-release.aab` | `6713459` B |

**Google Play Upload File**: `C:\Users\SG0216827\IdeaProjects\LibreDisplay\release-artifacts\LibreCare-2.15.0-release.aab`

---

## Branding Verification

| Search target | Classification | Result |
|---------------|----------------|--------|
| `LibreDisplay` in new user-facing strings | user-facing | not introduced |
| `LibreDisplay` package/applicationId | package/applicationId | unchanged technical legacy |
| `LibreDisplay` in migration legacy paths | migration legacy | unchanged |
| `LibreCare` in release docs | documentation/user-facing | used |

---

## Remaining Risks

1. The projection is a short-term linear estimate and may change quickly if trend direction changes.
2. Dedicated demo-mode assertion coverage for projection wording can still be expanded.
3. Connected/instrumented validation could not be executed in this environment.

---

## Compliance Checklist

| Rule | Status |
|------|--------|
| Code changed | PASS |
| Tests reviewed | PASS |
| Build completed | PASS |
| Release notes updated | PASS |
| Changelog updated | PASS |
| Reports updated | PASS |
| Artifacts reported | PASS |
| Google Play upload file identified | PASS |
| Branding verified | PASS |
| Risks reported | PASS |
| Database migration required | NOT APPLICABLE |
| Connected tests run | NOT APPLICABLE |

