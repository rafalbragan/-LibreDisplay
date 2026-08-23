# Release Report 2.3.0

## Summary
- Scope: domkniecie krytycznych brakow backup/Home po wdrozeniu 2.3.0.
- Focus: restore staging/preview, selective restore/export, viewport-based Home metrics, TIR row, drag reorder.

## Architecture review (EXISTS / INCOMPLETE / MISSING)
- ViewModels: EXISTS (`MonitoringViewModel`, `PrivacyDataViewModel`)
- Repositories: EXISTS (`AppDataBackupRepository`, sync/local repos)
- Room DB + migrations: EXISTS (brak zmian schematu w tym kroku)
- API layer: EXISTS
- Demo mode: EXISTS
- Privacy & data: EXISTS (wzmocnione flow backup)
- Statistics/widgets/charts/navigation: INCOMPLETE (czesc testow e2e brakuje)

## UI changes
- Home: metryki zalezne od widocznego viewportu wykresu.
- Home: dodany kompaktowy pasek TIR pod wykresem.
- Home: metryki mozna przestawiac long-press drag.
- Privacy: eksport i import kopii dostal etap podsumowania i wyboru zakresu.

## Database changes
- Brak zmian schematu Room.
- Brak migracji.

## Backup changes
- Restore działa stagingowo (preview -> confirm -> apply).
- Tryb `Merge` / `Replace` per osoba.
- Brak globalnego kasowania danych LIVE podczas restore.
- Dla porzadku usuwane sa tylko rekordy demo przy restore.

## Tests
- `./gradlew clean` — PASS
- `./gradlew testDebugUnitTest` — PASS
- `./gradlew lint` — PASS
- `./gradlew assembleDebug` — PASS
- `./gradlew assembleRelease` — PASS
- `./gradlew bundleRelease` — PASS
- Connected tests: `No connected device/emulator available.`

## Artifacts
- DEBUG APK: `release-artifacts/LibreCare-2.3.0-debug.apk` (23 545 943 B)
- RELEASE APK: `release-artifacts/LibreCare-2.3.0-release.apk` (3 194 389 B)
- RELEASE AAB: `release-artifacts/LibreCare-2.3.0-release.aab` (5 895 399 B)
- Google Play upload file: `release-artifacts/LibreCare-2.3.0-release.aab`

## Remaining risks
- Brak kompletu instrumentation tests dla Home (360/384/411/480dp + fontScale matrix).
- Brak pelnych testow nawigacji A->B->C->D z walidacja stacka back.
- Brak formalnego test run release+R8 z dedykowanym scenariuszem cast regression.

