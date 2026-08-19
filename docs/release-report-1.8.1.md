# Release Report – LibreCare 1.8.1

## Summary
LibreCare 1.8.1 fixes incorrect presentation of Libre timestamps that arrive without an explicit timezone. The app now interprets such timestamps in the phone timezone, which corrects chart times and user-facing time labels.

## Architecture review
- **API layer**: changed `LibreTimestampParser` so timezone-less timestamps are interpreted in `ZoneId.systemDefault()`.
- **UI formatting**: existing chart and user-facing formatters already used the device timezone; no architectural rewrite was required.
- **ViewModels**: improved one stale/local-history message to use localized formatting.
- **Database / migrations**: no changes.
- **Demo mode / privacy / statistics / widgets / navigation**: unaffected.

## Affected functionality status
- Existing implementation review: **EXISTS**
- Architecture support for timezone-aware formatting: **EXISTS**
- Central parsing of raw API timestamps: **EXISTS**
- User-facing localized time formatting: **EXISTS**
- Regression tests for naive timestamps in phone timezone: **INCOMPLETE** before change, now **EXISTS**
- DB / migration changes required: **MISSING / NOT APPLICABLE**

## UI changes
- Chart times now align with the phone timezone for naive Libre timestamps.
- Local-history sync message now uses a readable localized time label.

## Database changes
- None.

## Tests
- Focused tests:
  - `LibreTimestampParserTest`
  - `PolishFormattersTest`
- Full validation:
  - `./gradlew clean`
  - `./gradlew testDebugUnitTest`
  - `./gradlew lint`
  - `./gradlew assembleDebug`
  - `./gradlew assembleRelease`
  - `./gradlew bundleRelease`
- Connected tests: No connected device/emulator available.

## Artifacts
- Debug APK: `release-artifacts/LibreCare-1.8.1-debug.apk`
- Release APK: `release-artifacts/LibreCare-1.8.1-release.apk`
- Release AAB: `release-artifacts/LibreCare-1.8.1-release.aab`
- Google Play upload file: `release-artifacts/LibreCare-1.8.1-release.aab`

## Remaining risks
- If upstream data includes an explicit but incorrect offset, the app will preserve that explicit timestamp meaning.
- No device/emulator verification was possible in this run.

