# Release Report 1.6.0

## Summary
Release 1.6.0 delivers the second, deeper UI redesign for Home/History/NFZ details while preserving existing business logic, data source of truth, synchronization, LibreLinkUp integration, and medical calculations.

## Architecture review
- UI framework: Jetpack Compose
- Navigation style: screen-state routing in `MainActivity`
- Business logic moved: no
- Data models changed: no
- Repository/database contracts changed: no

## UI changes
- Home: simplified top status, redesigned glucose card, compact quick metrics, cleaned bottom navigation.
- Home: quick metrics now support long-press drag & drop reorder with persisted local order.
- History: horizontal range selector, compact summary row, simplified distribution section.
- NFZ: details moved from dialog to dedicated scrollable full screen.

## Database changes
- None.

## Tests
- `clean`: PASS
- `compileDebugKotlin`: PASS
- `testDebugUnitTest`: PASS
- `lint`: PASS
- `assembleDebug`: PASS
- `assembleRelease`: PASS
- `bundleRelease`: PASS
- Connected tests: not executed. No connected device/emulator available.

## Artifacts
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
- Release APK: `app/build/outputs/apk/release/app-release.apk`
- Release AAB: `app/build/outputs/bundle/release/app-release.aab`
- Google Play upload file: `app/build/outputs/bundle/release/app-release.aab`

## Remaining risks
- Alarm flows and extra feature routes are not part of this release scope.
- Existing global lint warning baseline remains high and should be reduced incrementally.

