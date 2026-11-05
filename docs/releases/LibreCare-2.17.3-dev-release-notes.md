# LibreCare 2.17.3-dev - local test integration

## PL

- A: zachowano istniejący reset tokenu, danych logowania, osoby i nawigacji po wylogowaniu.
- B: ciągłość linii z zachowaniem luk, przybliżanie/przesuwanie i podpowiedź nad wykresem historii.
- C: nawigator historii i etykieta rzeczywistego zakresu wykresu; zachowanie zakresu przy odświeżeniu sesji.
- D: lokalna strefa czasowa i polskie etykiety czasu, także miesięcy i lat.
- E: Mika DEMO z deterministyczną historią 14 dni i lukami; odrzucanie obcych identyfikatorów w aktywnym scenariuszu.
- Poprawki izolacji: brak zastępowania nieznanej osoby przypadkowym rekordem; czyszczenie danych dashboardu po zmianie sesji/scenariusza.
- Testy: 49 testów JVM bez błędów; kompilacja `:app:assembleDev` pomyślna.
- Artefakt: `release-artifacts\local-test\LibreCare-2.17.3-dev.apk`; dokładne dane w raporcie wydania.
- Ograniczenia: bez testów urządzenia/emulatora, pełnego lint i walidacji wydania; PR roboczy do przeglądu człowieka.
- Produkcja: 2.17.3/54 bez zmian; wariant lokalny 2.17.3-dev/54. Bez publikacji Google Play.

## EN

- A: preserve existing logout token, credential, patient and navigation reset.
- B: history line continuity without joining real gaps, zoom/pan and tooltip above the chart.
- C: history navigator and actual dashboard viewport label; preserve range on session refresh.
- D: device-local timezone and Polish time labels, including months and years.
- E: deterministic 14-day Mika DEMO history with gaps; reject unrelated IDs in an active scenario.
- Isolation fixes: no arbitrary cached-person fallback; clear dashboard data on session/scenario refresh.
- Tests: 49 targeted JVM tests passed; `:app:assembleDev` passed.
- Artifact: `release-artifacts\local-test\LibreCare-2.17.3-dev.apk`; exact metadata in release report.
- Limitations: no device/emulator UI tests, full lint or release validation; draft PR awaiting human review.
- Production stays 2.17.3/54; local variant is 2.17.3-dev/54. No Google Play publishing.
