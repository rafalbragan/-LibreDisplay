# Release Report – LibreCare 2.2.2

Data: 2026-08-21

## Podsumowanie

Wydanie 2.2.2 jest wydaniem stabilizacyjnym. Nie wprowadzono nowych funkcji ani zmian schematu bazy danych. Cały cykl build (testy, lint, APK, AAB) zakończył się sukcesem.

## Przegląd architektury

- Bez zmian architektonicznych w tej wersji.
- ViewModel, repozytoria, Room, API, Demo Mode – bez modyfikacji.

## Zmiany UI

- Brak zmian UI.

## Zmiany bazy danych

- Brak. Wersja Room bez zmian.
- Migracje: nie wymagane.

## Testy

| Test | Status |
|------|--------|
| `testDebugUnitTest` | PASS |
| `lint` | PASS |
| `assembleDebug` | PASS |
| `assembleRelease` | PASS |
| `bundleRelease` | PASS |
| Testy instrumentowane | Brak urządzenia |

## Artefakty

| Artefakt | Ścieżka | Rozmiar |
|----------|---------|---------|
| Debug APK | `release-artifacts/LibreCare-2.2.2-debug.apk` | 22,39 MB |
| Release APK | `release-artifacts/LibreCare-2.2.2-release.apk` | 3,03 MB |
| Release AAB | `release-artifacts/LibreCare-2.2.2-release.aab` | 5,55 MB |
| Google Play Upload | `release-artifacts/LibreCare-2.2.2-release.aab` | 5,55 MB |

## Wersjonowanie

- Poprzednia wersja: `2.2.1 (21)`
- Nowa wersja: `2.2.2 (22)`

## Pozostałe ryzyka

- Brak testów instrumentowanych (brak emulatora/urządzenia w środowisku CI).
- Brak weryfikacji podpisu release na urządzeniu fizycznym.

