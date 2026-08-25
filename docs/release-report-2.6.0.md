# LibreCare 2.6.0 – Release Report

**Date:** 2026-08-25
**versionName:** 2.6.0 · **versionCode:** 28 (poprzednio 2.5.0 / 27)
**Scope:** Bug fixes (rotation, person switch) + CV metric + history chart readability. Stable checkpoint before the new Analysis screen.

---

## Summary

This release closes the critical bugs reported for the dashboard and adds the CV
stability metric, plus history-chart readability improvements. It is an intentional
"stable checkpoint" produced and tested before starting the larger Analysis screen /
Excel export / landscape redesign work.

## Architecture review

- **Auth / lifecycle:** `MainActivity` now holds `AppLockRepository` as a field; a process-scoped
  session flag (`isSessionUnlocked`) plus `android:configChanges` on the Activity prevent Activity
  recreation on rotation from dropping the unlocked state. Session is cleared in `onStop()` only
  when `!isChangingConfigurations`, preserving the "lock on real exit" guarantee.
- **MonitoringViewModel:** person switching is now single-flight via a cancellable `personSwitchJob`
  with stale-result guards and explicit `CancellationException` propagation.
- **Metrics:** CV extracted into `GlucoseMetricsCalculator.calculateCoefficientOfVariation()` and
  reused by both the home tiles and the history stats card (removed duplicated inline formula).
- **Chart:** rendering density, day-change emphasis and edge-label placement handled inside
  `GlucoseChart`; no changes to `calculateChartArea` defaults, so layout tests remain valid.

## UI changes

- New CV tile on the home metrics strip (toggle + reorder supported, Polish labels everywhere).
- History chart: denser line, bold/larger date at day change + vertical day-boundary line,
  larger right touch margin, edge time labels on a lower baseline.

## Database changes

- None. No Room schema/version change; no migration required.

## Tests

- `GlucoseMetricsCalculatorStatisticsTest` – 3 new CV tests (insufficient data → null,
  flat readings → 0%, known SD → 16.67%).
- `QuickMetricConfigTest`, `RedesignedMetricsTest` – updated for the CV tile / ordering.
- `./gradlew :app:testDebugUnitTest` – **PASS**.
- `./gradlew :app:lintDebug` – **PASS** (BUILD SUCCESSFUL).
- Connected/instrumented tests: no local device/emulator available in this environment
  (Firebase Test Lab workflow prepared separately).

## Artifacts

| Type | Path | Size |
|------|------|------|
| Debug APK | `release-artifacts/LibreCare-2.6.0-debug.apk` | 23 695 262 B (22,6 MB) |
| Release APK | `release-artifacts/LibreCare-2.6.0-release.apk` | 3 490 227 B (3,33 MB) |
| Release AAB (Google Play upload) | `release-artifacts/LibreCare-2.6.0-release.aab` | 6 339 235 B (6,05 MB) |

## Remaining risks / follow-ups

- Landscape redesign (sidebar + vertical menu + "last update" relocation) not yet done.
- New "Analiza" screen (metrics table × periods, weekly stacked bars, 14-day overlay,
  night-only filter) and raw-data Excel export are the next milestone.
- Rotation fix validated by unit tests + reasoning; an instrumented rotation test is recommended
  once a device/emulator is available.

