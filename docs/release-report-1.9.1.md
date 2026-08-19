# Release Report – LibreCare 1.9.1

## Summary
LibreCare 1.9.1 continues the flat-layout direction by compacting the Home top area and replacing the history range buttons with a flat text selector.

## Architecture review
### Affected functionality status
- existing implementation: **EXISTS**
- architecture: **EXISTS**
- ViewModels: **EXISTS**
- repositories: **EXISTS**
- Room database: **NOT APPLICABLE**
- migrations: **NOT APPLICABLE**
- API layer: **NOT APPLICABLE**
- Demo Mode: **EXISTS**
- Privacy & Data: **NOT APPLICABLE**
- statistics: **EXISTS**
- widgets: **NOT APPLICABLE**
- charts: **EXISTS**
- Compose screens: **EXISTS**
- navigation: **EXISTS**

## UI changes
- Home top area uses less elevated chrome and fewer block-like surfaces.
- Freshness and sensor status are presented in a tighter text-first arrangement.
- Home time-range row is more compact and less control-like.
- History range selection no longer relies on button-like pills.

## Database changes
- None.

## Tests
- Added `HistorySelectorModelTest`.
- Full validation completed:
  - `./gradlew clean`
  - `./gradlew testDebugUnitTest`
  - `./gradlew lint`
  - `./gradlew assembleDebug`
  - `./gradlew assembleRelease`
  - `./gradlew bundleRelease`
- Connected tests: No connected device/emulator available.

## Artifacts
- Debug APK: `release-artifacts/LibreCare-1.9.1-debug.apk`
- Release APK: `release-artifacts/LibreCare-1.9.1-release.apk`
- Release AAB: `release-artifacts/LibreCare-1.9.1-release.aab`
- Google Play upload file: `release-artifacts/LibreCare-1.9.1-release.aab`

## Remaining risks
- Manual UX verification on 360dp and landscape history still requires a device/emulator.
- Some legacy card-based code paths still remain in source outside the main path.

