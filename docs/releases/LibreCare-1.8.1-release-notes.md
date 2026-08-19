# LibreCare 1.8.1 – Release Notes

## PL

### Wersja
- versionName: 1.8.1
- versionCode: 13

### Naprawione
- Naprawiono interpretację znaczników czasu Libre bez jawnej strefy czasowej.
- Dane typu `2026-08-19T12:10:00` oraz `2026-07-06 23:10:00` są teraz interpretowane w strefie czasowej telefonu, zamiast być wymuszane jako UTC.
- W efekcie wykres, etykiety osi czasu, tooltipy i komunikaty o czasie pokazują poprawny lokalny czas użytkownika.
- Poprawiono komunikat o ostatniej synchronizacji dla danych lokalnych — zamiast technicznego formatu `Instant` używany jest czytelny format użytkownika.

### Usprawnienia
- Formatowanie czasu na wykresie pozostaje oparte o lokalną strefę urządzenia, ale teraz działa poprawnie także dla timestampów bez offsetu.
- Dodano test regresyjny dla strefy `Europe/Warsaw`.

### Testy
- `LibreTimestampParserTest`
- `PolishFormattersTest`
- `testDebugUnitTest`
- `lint`
- `assembleDebug`
- `assembleRelease`
- `bundleRelease`

### Artefakty
- Debug APK: `release-artifacts/LibreCare-1.8.1-debug.apk`
- Release APK: `release-artifacts/LibreCare-1.8.1-release.apk`
- Release AAB: `release-artifacts/LibreCare-1.8.1-release.aab`

### Znane ograniczenia
- Jeżeli zewnętrzne źródło przekaże błędną strefę czasową już w samych danych, aplikacja nadal pokaże czas zgodny z otrzymanym timestampem.

---

## EN

### Version
- versionName: 1.8.1
- versionCode: 13

### Fixed
- Fixed Libre timestamps without an explicit timezone.
- Values such as `2026-08-19T12:10:00` and `2026-07-06 23:10:00` are now interpreted in the phone timezone instead of being forced to UTC.
- As a result, the chart, axis labels, tooltips, and user-facing time messages now show the correct local device time.
- Improved the local-history sync message to use a readable user-facing time format instead of raw `Instant` output.

### Improvements
- Chart time formatting already used the device timezone; with this fix it now also works correctly for timestamps that do not include an explicit offset.
- Added a `Europe/Warsaw` regression test.

### Tests
- `LibreTimestampParserTest`
- `PolishFormattersTest`
- `testDebugUnitTest`
- `lint`
- `assembleDebug`
- `assembleRelease`
- `bundleRelease`

### Artifacts
- Debug APK: `release-artifacts/LibreCare-1.8.1-debug.apk`
- Release APK: `release-artifacts/LibreCare-1.8.1-release.apk`
- Release AAB: `release-artifacts/LibreCare-1.8.1-release.aab`

### Known limitations
- If an external source provides an incorrect timezone already encoded in the timestamp, the app will still display the time implied by that source data.

