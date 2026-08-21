# Release Report - LibreCare 2.1.1

## Summary
Release 2.1.1 is a layout-only Home redesign focused on reducing wasted vertical space and surfacing chart preview earlier, without modifying business/data logic.

## Architecture review
### Affected functionality status
- existing implementation: **EXISTS**
- architecture: **EXISTS**
- ViewModels: **EXISTS** (no behavior changes)
- repositories: **EXISTS** (no behavior changes)
- Room database: **EXISTS** (no schema change)
- migrations: **NOT APPLICABLE** (no DB migration required)
- API layer: **EXISTS** (unchanged)
- Demo Mode: **EXISTS** (unchanged)
- Privacy & Data: **EXISTS** (unchanged)
- statistics: **EXISTS** (unchanged)
- widgets: **INCOMPLETE** (no widget-specific regression in this iteration)
- charts: **EXISTS**
- Compose screens: **EXISTS**
- navigation: **EXISTS** (actions preserved)

## UI changes
- Compacted top area (`LibreTopBar`, data freshness/sensor row) with smaller vertical footprint.
- Added compact `TimeRangeDisplay` row (`range + latest time` + `Historia >`) to remove separate bulky range area.
- Reworked current glucose block to keep value dominant while removing duplicate status/trend meaning.
- Reduced empty vertical gaps and normalized spacing across Home sections.
- Flattened quick metrics into denser row layout; `Edytuj >` moved inline.
- Moved `Historia glikemii` section higher and kept chart as preview-height component.

## Database changes
- No Room schema/version changes.
- No migration files added/updated.

## Tests
- Executed:
  - `./gradlew clean`
  - `./gradlew testDebugUnitTest`
  - `./gradlew lint`
  - `./gradlew assembleDebug`
  - `./gradlew assembleRelease`
  - `./gradlew bundleRelease`
- Connected tests: **No connected device/emulator available.**
- Tooling note: `adb` command not available in current shell PATH.

## Home vertical-space comparison (estimated)
- top area height: **before ~108dp** -> **after ~76dp**
- current glucose block: **before ~162dp** -> **after ~128dp**
- quick metrics block: **before ~116dp** -> **after ~92dp**
- Y-position of chart start (portrait 384x832): **before ~374dp** -> **after ~292dp**
- estimated reduction before chart: **~22%**

## Artifacts
- Debug APK: `C:\Users\SG0216827\IdeaProjects\LibreDisplay\release-artifacts\LibreCare-2.1.1-debug.apk` (23 414 859 B)
- Release APK: `C:\Users\SG0216827\IdeaProjects\LibreDisplay\release-artifacts\LibreCare-2.1.1-release.apk` (3 161 633 B)
- Release AAB: `C:\Users\SG0216827\IdeaProjects\LibreDisplay\release-artifacts\LibreCare-2.1.1-release.aab` (5 765 417 B)
- Google Play upload file: `C:\Users\SG0216827\IdeaProjects\LibreDisplay\release-artifacts\LibreCare-2.1.1-release.aab`

## Branding review
- user-facing: **EXISTS** (`LibreCare` retained in top-level title and release docs)
- package/applicationId: **EXISTS** (`com.libredisplay` unchanged as technical identifier)
- migration legacy: **NOT APPLICABLE**
- documentation: **INCOMPLETE** (legacy `LibreDisplay` references remain in historical docs/path names)
- generated output: **EXISTS** (workspace/build folders still use `LibreDisplay` naming)

## Remaining risks
- No device/emulator run for touch/gesture verification on real hardware.
- Font scale 1.2 and 360dp width checks were not captured with automated screenshots in this run.
- Existing unrelated workspace changes remain and were not part of this Home-only iteration.

