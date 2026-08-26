# LibreCare 2.11.2 — Notatki wydania

**Data wydania**: 2026-08-26  
**Wersja**: 2.11.2 (versionCode 37)  
**Poprzednia wersja**: 2.11.1 (versionCode 36)

---

## PL — Polski

### Nowe funkcjonalności

- **Selektor scenariuszy TEST / DEMO SCENARIO** (tylko debugowe APK w trybie Demo):
  Agenci testowi Firebase App Testing Agent mogą teraz wybierać kontrolowane scenariusze danych
  bezpośrednio z interfejsu aplikacji. Dostępne scenariusze:
  - Normalny
  - Szybki wzrost
  - Szybki spadek
  - Hipoglikemia
  - Ciężka hipoglikemia
  - Hiperglikemia
  - Stare dane
  - Brak danych
  - Kilka osób – jedna w ryzyku
- **ScenarioAwareMockLibreLinkUpClient**: nowy klient demo uwzględniający scenariusze.
  Gdy brak wybranego scenariusza (domyślnie), deleguje do MockLibreLinkUpClient bez zmian.
- **DemoScenarioController**: bezpieczny singleton zarządzający stanem scenariusza.
  W wersji release metoda `selectScenario` jest ścisłym no-op (funkcja strażnika BuildConfig.DEBUG).

### Naprawa

- Usunięto obserwacje OBS-20260826-01 i OBS-20260826-02 — były bezpodstawne dla trzech
  pozytywnych testów bazowych Firebase (TESTRUN-2026-001/002/003).
- Usunięto zduplikowany schemat `test-run.schema.json` (format TEST-YYYY-NNN).
  Kanonicznym schematem jest teraz `test_run.schema.json` (format TESTRUN-YYYY-NNN).
- Dodano do `observation.schema.json` pole `linked_test_runs` z walidacją wzorca `TESTRUN-YYYY-NNN`.
- Naprawiono `product_cli.py`:
  - Dodano `CURRENT_FOCUS_FILE`, `TEST_RUNS_DIR`.
  - Dodano `collect_split` dla rozróżnienia rekordów rzeczywistych od przykładów.
  - Dodano walidację `CURRENT_FOCUS.yaml` (priority_weights ≤ 10).
  - Dodano walidację punktacji wymagań (max 10).
  - Dodano sprawdzanie krzyżowe `linked_test_runs` → istniejące TESTRUN IDs.
  - Nowa struktura `cmd_summary`: "Real backlog" i "Examples".
  - Naprawiono obsługę typów `["string", "null"]` w walidatorze JSON Schema.

### Ulepszenia

- Importowalne pliki YAML dla Firebase App Testing Agent pod `testing/app-testing-agent/firebase/`.
- Selektor scenariuszy używa testTagów dla niezawodnej interakcji z agentami testowymi.

### Testy

- Nowy test: `DemoScenarioSelectorTest` (14 przypadków testowych) weryfikuje:
  - NORMAL → glukoza w normie
  - HYPO → glukoza < 70 mg/dL
  - STALE_DATA → timestamp > 24 h temu
  - MISSING_DATA → timestamp > 48 h temu
  - MULTIPLE_PATIENTS_ONE_AT_RISK → ≥ 2 osoby z różnymi stanami
  - reset() → powrót do null
- 13 testów product_cli (wszystkie przechodzą).
- Wszystkie testy jednostkowe: PASS.

### Znane ograniczenia

- Selektor scenariuszy jest widoczny wyłącznie w debugowym APK w trybie Demo.
- W release APK kod selektora jest eliminowany przez R8/ProGuard (BuildConfig.DEBUG = false).
- Baza danych demo może zawierać dane poprzednich scenariuszy (historia lokalna); dotyczy tylko agentów testowych.

---

## EN — English

### New Features

- **TEST / DEMO SCENARIO selector** (debug APK in Demo mode only):
  Firebase App Testing Agents can now select controlled demo scenarios via on-screen UI.
  Available scenarios: NORMAL, RAPID_RISE, RAPID_FALL, HYPO, SEVERE_HYPO, HYPER,
  STALE_DATA, MISSING_DATA, MULTIPLE_PATIENTS_ONE_AT_RISK.
- **ScenarioAwareMockLibreLinkUpClient**: new demo client that respects scenario selection.
  Falls back to MockLibreLinkUpClient when no scenario is selected.
- **DemoScenarioController**: thread-safe singleton managing scenario state.
  In release builds `selectScenario` is a strict no-op guarded by `BuildConfig.DEBUG`.

### Fixes

- Removed OBS-20260826-01 and OBS-20260826-02 — unjustified for three positive Firebase
  baseline test runs (TESTRUN-2026-001/002/003).
- Removed duplicate `test-run.schema.json` (TEST- pattern).
  Canonical schema is now `test_run.schema.json` (TESTRUN- pattern).
- Added `linked_test_runs` to `observation.schema.json` with TESTRUN-YYYY-NNN pattern validation.
- Fixed `product_cli.py`:
  - Added `CURRENT_FOCUS_FILE`, `TEST_RUNS_DIR`.
  - Added `collect_split` to separate real vs example records.
  - Added `CURRENT_FOCUS.yaml` validation (priority_weights ≤ 10).
  - Added requirement score validation (max 10).
  - Added `linked_test_runs` cross-reference validation.
  - New `cmd_summary` structure: "Real backlog" and "Examples" sections.
  - Fixed JSON Schema `["string", "null"]` list-type handling.

### Improvements

- Firebase App Testing Agent YAML files at `testing/app-testing-agent/firebase/`.
- Scenario selector uses testTags for reliable agent interaction.

### Tests

- New: `DemoScenarioSelectorTest` (14 test cases).
- 13 product_cli tests: all PASS.
- All unit tests: PASS.

### Known Limitations

- Scenario selector is visible only in debug APK in Demo mode.
- Release APK code is eliminated by R8/ProGuard.
- Local DB may retain data from previous scenarios between scenario switches.

---

## Artifacts

| File | Path | Size |
|------|------|------|
| Debug APK | `release-artifacts/LibreCare-2.11.2-debug.apk` | 22.71 MB |
| Release APK | `release-artifacts/LibreCare-2.11.2-release.apk` | 3.51 MB |
| Release AAB | `release-artifacts/LibreCare-2.11.2-release.aab` | 6.34 MB |

**Google Play Upload File**: `release-artifacts/LibreCare-2.11.2-release.aab`

