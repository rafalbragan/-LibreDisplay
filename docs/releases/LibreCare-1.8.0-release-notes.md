# LibreCare 1.8.0 – Release Notes

---

## PL

### Wersja
1.8.0 (versionCode 12)

### Naprawione błędy

**Wykres – przepełnienie osi Y (krytyczny)**
- Dane glikemii > 420 mg/dL były rysowane poza obszarem wykresu i nachodziły na etykiety osi Y.
- Naprawiono: usunięto sztywne ograniczenie `coerceAtMost(420)` w funkcji `prepareChartData`. Wykres teraz dynamicznie dopasowuje maksimum osi do rzeczywistych danych z odpowiednim marginesem (25–40 jednostek).

**Statystyki – „W zakresie" pokazywało 0% przy widocznych danych w zakresie**
- Obliczanie procentów czasu w zakresie korzystało z ucięcia (`toInt()`), przez co wartości 0,5–0,9% były wyświetlane jako „0%".
- Naprawiono: zmieniono na zaokrąglenie (`roundToInt()`). Wartości ≥ 0,5% teraz wyświetlają się jako „1%" zamiast „0%".

**Brak automatycznej aktualizacji czasów względnych i licznika dostępności danych**
- Wyświetlane wartości „chwilę temu", „Sensor: X dni", licznik odliczający do pełnego zakresu – nie były przeliczane lokalnie między synchronizacjami sieciowymi.
- Naprawiono: dodano lokalny ticker co 30 sekund (`LaunchedEffect`) w `MonitoringScreen` i `FullScreenGlucoseChartScreen`. Nie zwiększa to częstotliwości połączeń sieciowych.

**Nieprawidłowe odliczanie do pełnego zakresu danych**
- Funkcja `computeDataCoverage` liczyła: `timeUntil = selectedRange - (newest - oldest)`. W efekcie licznik nie malał bez napływu nowych danych.
- Naprawiono: zmieniono na `timeUntil = selectedRange - (now - oldest)`. Odliczanie teraz biegnie lokalnie bez żądań sieciowych.

### Poprawione zachowania

- Wszystkie etykiety czasu względnego odświeżają się co 30 s bez nowych danych z sieci.
- Wykres poprawnie skaluje się dla glikemii > 420 mg/dL.
- Procent „W zakresie" jest zaokrąglany zamiast ucinany.

### Testy

- Zaktualizowano `DataCoverageModelTest` – testy przekazują teraz parametr `now` wprost, co czyni wyniki deterministyczne niezależnie od czasu systemowego.
- Wszystkie 274 testy jednostkowe: PASS.

### Artefakty

- `release-artifacts/LibreCare-1.8.0-debug.apk` (~22,1 MB)
- `release-artifacts/LibreCare-1.8.0-release.apk` (~2,9 MB)
- `release-artifacts/LibreCare-1.8.0-release.aab` (~5,4 MB)

---

## EN

### Version
1.8.0 (versionCode 12)

### Bug Fixes

**Chart – Y-axis overflow (critical)**
- Glucose readings > 420 mg/dL were plotted outside the chart area, overlapping Y-axis labels.
- Fixed: removed the `coerceAtMost(420)` hard cap in `prepareChartData`. Chart now dynamically scales max Y to actual data with a proportional margin (25–40 units).

**Statistics – "In range" showing 0% despite visible in-range data**
- Time-in-range percentage used integer truncation (`toInt()`), causing 0.5–0.9% values to display as "0%".
- Fixed: changed to rounding (`roundToInt()`). Values ≥ 0.5% now show as "1%" instead of "0%".

**No local refresh of relative times and data coverage countdown**
- "chwilę temu", sensor remaining time, and coverage countdown were not recalculated locally between network syncs.
- Fixed: added a 30-second local ticker (`LaunchedEffect`) in `MonitoringScreen` and `FullScreenGlucoseChartScreen`. This does NOT increase network polling frequency.

**Coverage countdown did not tick between data refreshes**
- `computeDataCoverage` calculated: `timeUntil = selectedRange - (newest - oldest)`, which did not decrease without new data.
- Fixed: changed to `timeUntil = selectedRange - (now - oldest)`. Countdown now decreases locally.

### Behaviour improvements

- All relative time labels refresh every 30 s without network requests.
- Chart correctly scales for glucose > 420 mg/dL.
- "In range" percentage is rounded instead of truncated.

### Tests

- Updated `DataCoverageModelTest` – tests now pass `now` explicitly, making results deterministic regardless of system clock.
- All 274 unit tests: PASS.

### Artifacts

- `release-artifacts/LibreCare-1.8.0-debug.apk` (~22.1 MB)
- `release-artifacts/LibreCare-1.8.0-release.apk` (~2.9 MB)
- `release-artifacts/LibreCare-1.8.0-release.aab` (~5.4 MB)

