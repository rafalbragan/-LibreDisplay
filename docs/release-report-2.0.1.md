# Release Report - LibreCare 2.0.1

## Summary
This release increases Home chart granularity and adds a live-data backup/restore flow that includes settings and excludes demo data.

## Architecture review
### Affected functionality status
- existing implementation: **EXISTS**
- architecture: **EXISTS**
- ViewModels: **EXISTS**
- repositories: **EXISTS**
- Room database: **EXISTS**
- migrations: **NOT APPLICABLE**
- API layer: **NOT APPLICABLE**
- Demo Mode: **EXISTS**
- Privacy & Data: **EXISTS**
- statistics: **EXISTS**
- widgets: **NOT APPLICABLE**
- charts: **EXISTS**
- Compose screens: **EXISTS**
- navigation: **EXISTS**

## UI changes
- Added backup and restore actions to `PrivacyDataScreen` with explicit scope: LIVE + settings, no demo data.
- Added uninstall limitation notice and pre-uninstall recommendation.

## Database changes
- No schema version change.
- Added new DAO operations for export/import of live subsets:
  - `GlucoseReadingDao.getAllLiveReadings`, `deleteAllLiveReadings`, `insertReplace`
  - `ObservedPersonDao.getAllLivePersons`, `deleteLivePeople`
  - `PatientSettingsDao.getAllLiveSettings`, `deleteLiveSettings`

## Tests
- Added `AppDataBackupRepositoryTest`.
- Updated `GlucoseChartLayoutLogicTest`.
- Full validation completed:
  - `./gradlew clean`
  - `./gradlew testDebugUnitTest`
  - `./gradlew lint`
  - `./gradlew assembleDebug`
  - `./gradlew assembleRelease`
  - `./gradlew bundleRelease`
- Connected tests: No connected device/emulator available (`adb` unavailable, `Phone connected (status=device): false`).

## Artifacts
- Debug APK: `C:\Users\SG0216827\IdeaProjects\LibreDisplay\release-artifacts\LibreCare-2.0.1-debug.apk` (23 365 701 B)
- Release APK: `C:\Users\SG0216827\IdeaProjects\LibreDisplay\release-artifacts\LibreCare-2.0.1-release.apk` (3 128 979 B)
- Release AAB: `C:\Users\SG0216827\IdeaProjects\LibreDisplay\release-artifacts\LibreCare-2.0.1-release.aab` (5 692 920 B)
- Google Play upload file: `C:\Users\SG0216827\IdeaProjects\LibreDisplay\release-artifacts\LibreCare-2.0.1-release.aab`

## Remaining risks
- Existing installs with old `applicationIdSuffix` debug package require one-time manual backup/restore migration.
- Full end-to-end restore validation on physical device is still pending.

