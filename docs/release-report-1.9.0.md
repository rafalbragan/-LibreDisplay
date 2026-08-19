# Release Report – LibreCare 1.9.0

## Summary
LibreCare 1.9.0 focuses on layout structure rather than another cosmetic radius/color pass. The Home and History screens were rebuilt to use flatter sections, typography, whitespace, and subtle dividers instead of a long stack of large cards.

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
- Home screen now relies on shared background and dividers instead of stacking large full-width cards.
- Current glucose section no longer uses an outer card.
- Quick metrics use a compact flat strip with subtle separators.
- Person switcher is visually lighter and uses underline selection instead of pill-heavy emphasis.
- NFZ summary was flattened to a section instead of a boxed card.
- History chart section, selected point details, time-in-range section, and stats use flatter presentation.

## Large containers removed
Estimated removed or structurally flattened large containers in primary paths:
- Home: 4 major full-width containers flattened (`RedesignedCurrentGlucoseCard`, `DemoModeBanner`, `NfzStatusCompactCard`, `EmptyChartState` / error style simplification)
- History: 4 major full-width containers flattened (chart section, selected point details, range distribution section, stat cards style simplification)

## Database changes
- None.

## Tests
- Added `RedesignedMetricsTest`.
- Full validation completed:
  - `./gradlew clean`
  - `./gradlew testDebugUnitTest`
  - `./gradlew lint`
  - `./gradlew assembleDebug`
  - `./gradlew assembleRelease`
  - `./gradlew bundleRelease`
- Connected tests: No connected device/emulator available.

## Artifacts
- Debug APK: `release-artifacts/LibreCare-1.9.0-debug.apk`
- Release APK: `release-artifacts/LibreCare-1.9.0-release.apk`
- Release AAB: `release-artifacts/LibreCare-1.9.0-release.aab`
- Google Play upload file: `release-artifacts/LibreCare-1.9.0-release.aab`

## Remaining risks
- Visual QA on a physical 360dp device and landscape history view was not possible in this environment.
- Some legacy card-based composables still exist in source but are not part of the primary flattened path.

