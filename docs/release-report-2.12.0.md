# LibreCare Release Report — 2.12.0

**Date**: 2026-08-27  
**Version**: 2.12.0 (versionCode 38)  
**Previous version**: 2.11.2 (versionCode 37)

---

## Summary

This release adds a new top-level `Futures` tab so product ideas and future-facing concepts can be reviewed inside the app without replacing the current production flows.

Delivered in this release:

1. New 4th top-level destination: `Futures`.
2. New `Futures` screen with prototype cards covering analysis improvements and broader roadmap concepts.
3. Audience filter for `Pacjent`, `Senior`, `Opiekun`, and `Lekarz`.
4. Navigation updates in Home/Analysis/Settings bottom navigation and landscape rail.
5. Test coverage for the new screen and navigation changes.
6. Documentation pack for `DEC-0007` and the Futures rollout path.

---

## Architecture Review

### Navigation

The app continues to use the existing stack-based navigation model.

```text
Monitoring / Analytics / Futures / Settings
        ↓
TopLevelNavigationBar + SideNavigationRail
        ↓
MainActivity AppScreen routing
        ↓
AppNavigationState top-level switching
```

### Key files added or updated

| File | Purpose |
|------|---------|
| `app/src/main/java/com/libredisplay/ui/futures/FuturesScreen.kt` | New prototype screen UI |
| `app/src/main/java/com/libredisplay/ui/futures/FuturesViewModel.kt` | State for audience filter and expandable cards |
| `app/src/main/java/com/libredisplay/ui/monitoring/TopLevelNavigationBar.kt` | 4th top-level navigation item |
| `app/src/main/java/com/libredisplay/MainActivity.kt` | Screen routing for `AppScreen.Futures` |
| `app/src/main/java/com/libredisplay/AppNavigationState.kt` | Futures registered as top-level destination |
| `app/src/main/java/com/libredisplay/ui/monitoring/LibreCareTestTags.kt` | Added Futures test tags |

### Data / Repository impact

- No Room schema changes.
- No repository contract changes.
- No API-layer changes.
- No background sync changes.
- No widget changes.

---

## Pre-Implementation Review (affected areas)

| Area | Status | Notes |
|------|--------|-------|
| Existing implementation | EXISTS | Home, Analysis, and Settings already use a shared top-level navigation pattern. |
| Architecture | EXISTS | Stack-based routing via `AppNavigationState` and `MainActivity`. |
| ViewModels | EXISTS | Existing StateFlow pattern reused; new lightweight `FuturesViewModel` added. |
| Repositories | EXISTS | Available, but not required for this prototype-only tab. |
| Room database | EXISTS | No DB changes needed for this release. |
| Migrations | EXISTS | Existing migration approach remains untouched. |
| API layer | EXISTS | Available, not affected. |
| Demo Mode | EXISTS | Preserved; Futures does not interfere with demo scenarios. |
| Privacy & Data | EXISTS | No extra data collection added. |
| Statistics | INCOMPLETE | Futures ideas mention future stats expansion, but current release only documents them. |
| Widgets | EXISTS | Existing widget support preserved and unchanged. |
| Charts | INCOMPLETE | Analysis improvements are documented in Futures, not yet moved into production charts. |
| Compose screens | EXISTS | New Compose screen added consistently with existing theme/components. |
| Navigation | EXISTS | Futures integrated as a new top-level destination. |

---

## UI Changes

### Added
- New `Futures` tab in bottom navigation.
- New `Futures` entry in the landscape side rail.
- New `Futures` screen with:
  - hero explanation block,
  - persona filter,
  - grouped idea cards,
  - expandable details,
  - roadmap summary.

### Adjusted
- Top-level navigation labels were reduced to `11sp` and constrained to a single line with ellipsis so the new 4-tab layout fits more safely on compact phones.

### Preserved
- `Główna`, `Analiza`, and `Ustawienia` keep their current workflows.
- No production dashboard card was replaced in this release.

---

## Database Changes

No schema changes. No migration required.

---

## Tests

| Test suite / command | Result |
|----------------------|--------|
| `FuturesViewModelTest` | PASS |
| `FuturesScreenTest` | PASS |
| `AppNavigationStateTest` | PASS |
| `./gradlew clean` | PASS |
| `./gradlew testDebugUnitTest` | PASS |
| `./gradlew lint` | PASS |
| `./gradlew assembleDebug` | PASS |
| `./gradlew assembleRelease` | PASS |
| `./gradlew bundleRelease` | PASS |
| Connected device / emulator tests | NOT RUN |

**Debug unit test XML summary**: `483` tests, `0` failures, `0` errors, `0` skipped.

---

## Artifacts

| Artifact | Path | Size |
|----------|------|------|
| Debug APK | `C:\Users\SG0216827\IdeaProjects\LibreDisplay\release-artifacts\LibreCare-2.12.0-debug.apk` | `23859171` B (`22,75 MB`) |
| Release APK | `C:\Users\SG0216827\IdeaProjects\LibreDisplay\release-artifacts\LibreCare-2.12.0-release.apk` | `3695756` B (`3,52 MB`) |
| Release AAB | `C:\Users\SG0216827\IdeaProjects\LibreDisplay\release-artifacts\LibreCare-2.12.0-release.aab` | `6688026` B (`6,38 MB`) |

**Google Play Upload File**: `C:\Users\SG0216827\IdeaProjects\LibreDisplay\release-artifacts\LibreCare-2.12.0-release.aab`

---

## Branding Verification

| Search target | Classification | Result |
|---------------|----------------|--------|
| `LibreDisplay` in new user-facing UI | user-facing | not introduced |
| `LibreDisplay` package / applicationId | package/applicationId legacy | unchanged, acceptable |
| `LibreCare` in release docs | documentation/user-facing | used |

---

## Remaining Risks

1. `Futures` currently presents prototypes and roadmap language, not live analytical calculations for every card.
2. The new 4-item navigation layout should still be observed on small phones in manual QA, especially label fit.
3. `adb` was unavailable in this environment, so connected/instrumented execution could not be verified in this session.

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
| NFZ wording changes required | NOT APPLICABLE |
| Connected tests run | NOT APPLICABLE |

**Connected tests note**: No connected device/emulator available in this environment; `adb` command was unavailable.


