# LibreCare Release Report - 2.16.3

## Summary
- Scope implemented in this iteration: Home Chart 2.0 gesture stabilization, diagnostics file export (SAF), crash-context persistence, About screen build metadata/version history, stale-current-data guard.
- Added dedicated `Features` screen with feature statuses and release history (About remains focused on build metadata).
- Local build pipeline unblocked by process-level JDK 17 configuration.

## Architecture Review (EXISTS / INCOMPLETE / MISSING)
- ViewModels: **EXISTS** (`MonitoringViewModel` stale data handling present).
- Repositories/API layer: **EXISTS** (no API contract changes in this patch).
- Room DB/migrations: **NOT APPLICABLE** (schema unchanged).
- Compose/navigation: **EXISTS** (gesture behavior, diagnostics UI, and Features navigation updated).
- Demo mode/privacy/statistics/widgets/charts: **INCOMPLETE** (chart interactions updated; full scenario validation on production-like account pending).

## UI Changes
- Home chart: pan, pinch zoom, double-tap zoom, arrows + latest button, explicit fullscreen button.
- Current glucose card: stale/inactive state no longer shown as current value.
- Diagnostics screen: added `Zapisz log do pliku`, status messages, crash report section.
- About screen: now shows commit/build channel and uses shared version catalog.
- New Features screen: statusy funkcji (`PLANOWANE/W ROZWOJU/DO TESTOW/WDROZONE/WSTRZYMANE`) + historia wersji.

## Database Changes
- None.

## Tests
- Executed command:
  - `./gradlew clean`
  - `./gradlew testDebugUnitTest`
  - `./gradlew lint`
  - `./gradlew assembleDebug`
  - `./gradlew assembleRelease`
  - `./gradlew bundleRelease`
- Result: **PASS**.
- Device tests: `./gradlew connectedDebugAndroidTest` -> **FAIL** (`HomeInstrumentedUiTest`, `NoSuchMethodException: android.hardware.input.InputManager.getInstance` on `SM-S948B - 17`).

## Artifacts
- `release-artifacts/LibreCare-2.16.3-debug.apk` (`23,924,701 B`, SHA-256 `80B305AD335F007A628947A0A09661D8E60BC4E4FCD37BE3B202106AEB6D1BC3`)
- `release-artifacts/LibreCare-2.16.3-release.apk` (`3,695,754 B`, SHA-256 `714103C716485F30FE5975B109B71BA68E362791B2E701BF99CE45C77CAAFAB8`)
- `release-artifacts/LibreCare-2.16.3-release.aab` (`6,721,080 B`, SHA-256 `D7BF96D467B529913A53210BFB558754589CE86C77CE889A9E538CA781EBB324`)

## Remaining Risks
- Issue #29 needs device/logcat reproduction with newest build to confirm final root-cause closure.
- Gesture conflicts must be validated on real phone (Home + full-screen + landscape).
- Connected test compatibility gap on Android 17 device (`InputManager.getInstance`) remains unresolved.

