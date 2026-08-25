# LibreCare 2.8.0 – Release Notes

**Date:** 2026-08-25
**versionName:** 2.8.0 · **versionCode:** 30 (poprzednio 2.7.0 / 29)

---

## PL

### Zmiany w aplikacji
- **Domyślny zakres wykresu**: 12 godz. w pionie, 24 godz. w poziomie (jeśli są dane; inaczej największy dostępny).
- **Zapamiętanie zakresu w sesji**: po ręcznej zmianie zakres jest utrzymywany, także po obrocie ekranu (nie wraca do domyślnego).
- **Widok poziomy**: metryki zajmują całą szerokość ekranu pod wierszem glukoza/wykres.

### CI / Firebase Test Lab
- Instrumentation tests uruchamiane na **macierzy 3 telefonów wirtualnych** (SMALL/STANDARD/LARGE) w jednym przebiegu; każdy model+API walidowany na żywo względem katalogu Test Lab; awaria dowolnego urządzenia = FAIL.

### Testy
- `./gradlew testDebugUnitTest` — PASS.
- `./gradlew lint` — PASS.

### Znane ograniczenia
- Pełny ekran „Analiza" i eksport surowych danych do Excela — planowane w kolejnych wydaniach.

### Artefakty
- `release-artifacts/LibreCare-2.8.0-debug.apk`
- `release-artifacts/LibreCare-2.8.0-release.apk`
- `release-artifacts/LibreCare-2.8.0-release.aab` (plik do Google Play)

---

## EN

### App changes
- **Default chart range**: 12h portrait, 24h landscape (when data exists; otherwise the largest available).
- **Range persists within the session**: after a manual change the range is kept, including across rotation (no revert to default).
- **Landscape**: metrics span the full screen width below the glucose/chart row.

### CI / Firebase Test Lab
- Instrumentation tests run on a **3 virtual-phone matrix** (SMALL/STANDARD/LARGE) in one run; each model+API validated live against the Test Lab catalog; any device failure = FAIL.

### Tests
- `./gradlew testDebugUnitTest` — PASS.
- `./gradlew lint` — PASS.

### Known limitations
- The full "Analysis" screen and raw-data Excel export are planned for later releases.

### Artifacts
- `release-artifacts/LibreCare-2.8.0-debug.apk`
- `release-artifacts/LibreCare-2.8.0-release.apk`
- `release-artifacts/LibreCare-2.8.0-release.aab` (Google Play upload file)

