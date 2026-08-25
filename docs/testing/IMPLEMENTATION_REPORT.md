# LibreCare Testing Infrastructure Implementation Report

_Last updated: 2026-08-24. This report reflects code that actually exists and was executed in this environment._

## Summary

The missing testing infrastructure has now been implemented and executed locally (JVM/Robolectric).
Screenshot/golden testing, the deterministic scenario engine, the reusable CGM generator, the
database >24h persistence regression, the statistics/backup regression suites, the responsive UI
matrix, and the code-derived navigation suite all exist in the repository and run green.

Instrumented (on-device) Compose tests are compiled and packaged into the debug androidTest APK, but
are not executed here because this environment has no device/emulator. They are reported as
`SKIPPED_NEEDS_DEVICE`, never as PASS.

## What EXISTS now (implemented + executed)

### Deterministic test data + CGM generator
- `app/src/sharedTest/java/com/libredisplay/testing/scenario/GlucoseScenarioEngine.kt`
  - `GlucoseScenario` covers: NORMAL_STABLE, NORMAL_RISING, NORMAL_FALLING, HIGH_STABLE, HIGH_RISING,
    HIGH_FAST_RISING, VERY_HIGH_STABLE, VERY_HIGH_RISING, LOW_STABLE, LOW_FALLING, LOW_FAST_FALLING,
    VERY_LOW, NO_DATA, STALE_DATA, ONLY_1H/3H/6H/12H_DATA, FULL_24H/48H/3D_DATA, DATA_WITH_GAPS,
    DATA_CROSSING_MIDNIGHT, SENSOR_EXPIRING/EXPIRED, GMI_UNAVAILABLE, LONG_METRIC_VALUES,
    BACKUP_SAMPLE_DATA.
  - `DeterministicCgmGenerator.generate(startTime, duration, sampleInterval, startGlucose, pattern)`
    with patterns: FLAT, GRADUAL_RISE/FALL, FAST_RISE/FALL, HYPO_EPISODE, HYPER_EPISODE,
    IRREGULAR_INTERVALS, GAPS, CROSSING_MIDNIGHT. Fully deterministic (fixed seed-free arithmetic).
- Release isolation: the engine lives in the `sharedTest` source set which is wired ONLY into the
  `test` and `androidTest` source sets (see `app/build.gradle.kts`). It is structurally impossible to
  include or toggle it from a release build.

### Database >24h persistence regression (RUN, green)
- `app/src/test/java/com/libredisplay/data/repository/LocalGlucoseHistoryRepositoryTest.kt`
  - generate 48h of data -> persist to Room -> close DB -> reopen -> assert oldest/newest timestamp,
    record count, and available span > 24h.
  - two-profile reopen + retention/cleanup variant keeps both profiles.
  - `loadStoredRange` reports the full end-to-end span even with gaps and beyond the loaded window.

### Statistics + episodes (RUN, green)
- `app/src/test/java/com/libredisplay/analytics/GlucoseMetricsCalculatorStatisticsTest.kt`:
  min/max/average, below/in-range/above %, invariants `min <= average <= max` and
  `below + inRange + above ~= 100%`, for empty / one / all-low / all-high / mixed / gapped data.
- `app/src/test/java/com/libredisplay/ui/monitoring/EpisodeCountingTest.kt`: 0/1/many episodes and
  episode interrupted by a return to normal range.

### Backup / restore regression (RUN, green)
- `AppDataBackupRepositoryTest.kt` (27 tests), `BackupCodecTest.kt` (10 tests),
  `BackupMergeEngineTest.kt`, `LocalBackupStoreTest.kt`, `AppDataBackupRepositoryLegacyRestoreTest.kt`.
- Key invariant covered: `snapshotBefore` -> failed restore (unsupported schema / malformed rows /
  missing fields) -> `snapshotAfter` == `snapshotBefore`. A failed restore never mutates existing data.

### Compose UI test wiring (RUN, green)
- Compose UI test dependencies wired for JVM (`androidx.compose.ui:ui-test`, `ui-test-junit4`,
  `ui-test-manifest`) and Robolectric. `assembleDebug` and `assembleDebugAndroidTest` both PASS.
- Production testability seams added: `LibreCareTestTags`, `testTag`/semantics on current-glucose
  value/unit/trend/severity, top-bar items, metric tiles, range chips, and bottom nav.

### Home UI + matrix + overlap + value/trend/metric coverage (RUN, green)
- `MonitoringResponsiveUiTest.kt`:
  - responsive matrix (widths 360/384/411/480, font scales 1.0/1.2/1.3/1.5) for NORMAL_STABLE,
    HIGH_RISING, VERY_HIGH_RISING, LOW_FAST_FALLING, VERY_LOW, NO_DATA, STALE_DATA, LONG_METRIC_VALUES.
  - `assertNoOverlap(nodeA, nodeB)` helper (in `MonitoringUiTestSupport.kt`) using Compose unclipped
    bounds, applied to glucose value vs unit, glucose vs trend, top-bar title vs version, adjacent
    metric tiles, and adjacent range chips. Failure message names both elements + bounds + context.
  - every supported integer glucose value 1..600 is rendered and asserted (value/unit/trend/severity).
  - every `GlucoseTrend` enum entry (6) is rendered and asserted.
  - metric stress values (0m, 59m, 23g 59m, 1%, 99%, 100%, long status text) render without truncation.

### Screenshot / golden framework (IMPLEMENTED, RUN, green)
- Framework: Roborazzi 1.42.0 + Robolectric NATIVE graphics (JVM, no device needed).
- `app/src/test/java/com/libredisplay/ui/monitoring/HomeScreenshotTest.kt` (8 golden tests).
- Baselines committed under `app/src/test/screenshots/`:
  Home_Normal_360, Home_Normal_384, Home_Font130, Home_Font150, Home_VeryHigh, Home_Low, Home_Stale,
  Home_NoData.
- Record: `./gradlew :app:recordRoborazziDebug` (executed, produced 8 PNG baselines).
- Verify: `./gradlew :app:verifyRoborazziDebug` (executed, PASS). On a mismatch Roborazzi writes
  expected/actual/diff under `app/build/outputs/roborazzi/`.

### Navigation matrix + navigation suite (RUN, green)
- `docs/testing/NAVIGATION_MATRIX.md` is derived from `navigationGraphEdges()` in
  `app/src/main/java/com/libredisplay/AppNavigationState.kt` (routes are generated by
  `allMaxDepthRoutesFrom(...)`, not hand-copied).
- `AppNavigationStateTest.kt` (7 tests): every code-derived max-depth route is traversed forward and
  back (pop exactly one screen at a time), top-level switching keeps expected back targets, and
  bottom-navigation ordering never creates unexpected duplicates.

### Instrumented (on-device) Compose test — compiled + packaged
- `app/src/androidTest/java/com/libredisplay/ui/monitoring/HomeInstrumentedUiTest.kt`.
- Compiles and is packaged into `app-debug-androidTest.apk`. Execution: `SKIPPED_NEEDS_DEVICE`.

## Numbers (verified)

- Total unit tests: 440 (0 failures, 0 errors).
- Golden baselines: 8; golden tests: 8.
- Database persistence tests in `LocalGlucoseHistoryRepositoryTest`: 6 (plus multi-person, startup,
  version, and the instrumented Room migration test).
- Backup regression tests: 27 (repository) + 10 (codec) + merge/store/legacy suites.
- Statistics tests: 6; episode tests: 4; scenario-engine tests: 4; range-option tests: 16.
- Responsive UI tests: 4; instrumented UI tests (androidTest): 1.
- Navigation tests: 7.
- Glucose values rendered + asserted: 600 (integer range 1..600).
- Trend states covered: 6 (all `GlucoseTrend` entries).

## Execution results (this environment)

- `./gradlew :app:testDebugUnitTest` -> PASS (440 tests, 0 failed).
- `./gradlew :app:recordRoborazziDebug` -> PASS (8 baselines produced).
- `./gradlew :app:verifyRoborazziDebug` -> PASS (golden compare clean).
- `./gradlew :app:lintDebug` -> PASS (0 errors, 305 warnings).
- `./gradlew :app:assembleDebug` -> PASS.
- `./gradlew :app:assembleDebugAndroidTest` -> PASS (androidTest APK built).
- `scripts/test-fast.sh`, `scripts/test-all.sh` -> the scripts are POSIX shell and call the Unix
  `./gradlew` wrapper; no `bash` interpreter is available on this Windows shell, so the scripts were
  not invoked directly. Every gradle step they run was executed individually and passed
  (testDebugUnitTest, lint, assembleDebug, assembleDebugAndroidTest, assembleRelease).

## Artifacts

- Fresh debug APK: `app/build/outputs/apk/debug/app-debug.apk` (~23.3 MB, built from current sources;
  the changes in this iteration are test-only and do not alter the app binary).
- androidTest APK: `app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk` (~1.07 MB).
- Named copy: `release-artifacts/LibreCare-2.5.0-debug.apk`.

## UI / behavior changes

None. Only testability seams (`testTag`/semantics/`internal` visibility) were added to production UI.
Application version is unchanged (`2.5.0`, versionCode 27). No Room schema change was introduced.

## Remaining external dependencies

- On-device / instrumented Compose test execution requires a device or emulator (Firebase Test Lab or
  a local/CI emulator). The instrumentation is compiled and packaged; only execution is external.
- Firebase Test Lab still requires external credentials (intentionally not configured in this task).

READY_FOR_FIREBASE_SETUP: YES
