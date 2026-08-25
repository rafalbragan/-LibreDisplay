# LibreCare 2.6.0 – Release Notes

**Date:** 2026-08-25
**versionName:** 2.6.0
**versionCode:** 28
**Previous:** 2.5.0 (versionCode 27)

---

## PL

### Nowości
- **Metryka CV (współczynnik zmienności)** na ekranie głównym. Pokazuje stabilność glikemii z progiem klinicznym: „Stabilnie (≤36%)" lub „Duża zmienność". Metrykę można włączać/wyłączać i przestawiać tak jak pozostałe.

### Poprawki błędów
- **Obrót ekranu** (pionowo ↔ poziomo) nie wymusza już ponownego logowania. Sesja odblokowania przeżywa zmianę orientacji (dodano `configChanges` do Activity + procesowa flaga sesji), a pełne zamknięcie / powrót aplikacji z tła nadal wymaga uwierzytelnienia.
- **Przełączanie monitorowanej osoby** nie zamyka już aplikacji. Poprzednie zadanie przełączenia jest anulowane, a nieaktualne wyniki są odrzucane — koniec wyścigu korutyn.

### Ulepszenia wykresu historii
- Gęstsze rysowanie danych (ok. 1 punkt na 2 px, zgodnie z rekomendacją próbkowania CGM co 5 minut).
- Wyraźne oznaczenie zmiany dnia: pogrubiona/większa data oraz pionowa linia w miejscu przejścia na kolejny dzień.
- Więcej miejsca po prawej stronie — palec dosięgnie wartości przy krawędzi.
- Skrajne etykiety osi czasu przeniesione niżej, aby podczas przewijania nie nachodziły na etykiety pośrednie.

### Testy
- `GlucoseMetricsCalculatorStatisticsTest` — 3 nowe testy CV.
- `QuickMetricConfigTest`, `RedesignedMetricsTest` — zaktualizowane o kafelek CV.
- `./gradlew testDebugUnitTest` — PASS.

### Znane ograniczenia
- Nowy ekran „Analiza" (tabela metryk × okresy, słupki tygodniowe, nakładka 14 dni, filtr nocny) oraz eksport do Excela są zaplanowane na kolejne wydanie.
- Pełna przebudowa układu poziomego (sidebar, menu pionowe) jest zaplanowana na kolejne wydanie.

### Artefakty
- `release-artifacts/LibreCare-2.6.0-debug.apk`
- `release-artifacts/LibreCare-2.6.0-release.apk`
- `release-artifacts/LibreCare-2.6.0-release.aab` (plik do Google Play)

---

## EN

### New
- **CV (Coefficient of Variation)** metric on the home screen. A glucose stability marker with a clinical threshold: "Stable (≤36%)" or "High variability". Toggleable and reorderable like the other metrics.

### Bug fixes
- **Screen rotation** (portrait ↔ landscape) no longer forces re-authentication. The unlocked session survives configuration changes (Activity `configChanges` + a process-scoped session flag), while a genuine exit / return from background still requires unlocking.
- **Switching the monitored person** no longer crashes the app. The previous switch job is cancelled and stale results are discarded, removing the coroutine race.

### History chart improvements
- Denser rendering (~1 point per 2 px, aligned with the 5-minute CGM sampling recommendation).
- Clear day-change indicator: bold/larger date and a vertical line at the day boundary.
- More room on the right so a finger can reach edge values.
- The two edge time-axis labels sit on a lower baseline so they no longer collide with intermediate labels while scrolling.

### Tests
- `GlucoseMetricsCalculatorStatisticsTest` — 3 new CV tests.
- `QuickMetricConfigTest`, `RedesignedMetricsTest` — updated for the CV tile.
- `./gradlew testDebugUnitTest` — PASS.

### Known limitations
- The new "Analiza" (Analysis) screen (metrics table × periods, weekly bars, 14-day overlay, night-only filter) and Excel export are planned for the next release.
- The full landscape redesign (sidebar, vertical menu) is planned for the next release.

### Artifacts
- `release-artifacts/LibreCare-2.6.0-debug.apk`
- `release-artifacts/LibreCare-2.6.0-release.apk`
- `release-artifacts/LibreCare-2.6.0-release.aab` (Google Play upload file)

