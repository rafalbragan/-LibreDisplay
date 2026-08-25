# LibreCare 2.9.1 – Release Notes

**Date:** 2026-08-25
**versionName:** 2.9.1 · **versionCode:** 32 (poprzednio 2.9.0 / 31)

---

## PL

### Zmiana
- Przenoszenie metryk na ekranie głównym: **sąsiedni kafelek płynnie „ucieka" w nowe miejsce** podczas przeciągania. Strip przełączony na `LazyRow` + `animateItem`; przeciągany kafelek zachowuje animację uniesienia (skala + cień + wysunięcie).

### Testy
- `MonitoringResponsiveUiTest` dostosowany do `LazyRow` (`performScrollToNode` + sprawdzenie pełnego tekstu). `./gradlew testDebugUnitTest` — PASS.

### Artefakty
- `release-artifacts/LibreCare-2.9.1-debug.apk`
- `release-artifacts/LibreCare-2.9.1-release.apk`
- `release-artifacts/LibreCare-2.9.1-release.aab` (plik do Google Play)

---

## EN

### Changed
- Home-screen metrics reorder: the **displaced neighbour now slides smoothly** into place while dragging. Strip switched to `LazyRow` + `animateItem`; the dragged tile keeps its lift animation.

### Tests
- `MonitoringResponsiveUiTest` adapted to `LazyRow` (`performScrollToNode` + full-text check). `./gradlew testDebugUnitTest` — PASS.

### Artifacts
- `release-artifacts/LibreCare-2.9.1-debug.apk`
- `release-artifacts/LibreCare-2.9.1-release.apk`
- `release-artifacts/LibreCare-2.9.1-release.aab` (Google Play upload file)

