# LibreCare Release Report — 2.11.2

**Date**: 2026-08-26  
**Version**: 2.11.2 (versionCode 37)  
**Previous version**: 2.11.1 (versionCode 36)

---

## Summary

Phase 2 clean-up and Firebase App Testing Agent enablement:

1. Removed two unjustified product observations (OBS-20260826-01, OBS-20260826-02).
2. Consolidated test-run schema to a single canonical file (`test_run.schema.json`, TESTRUN-YYYY-NNN pattern).
3. Extended `observation.schema.json` with `linked_test_runs` (validated TESTRUN-YYYY-NNN cross-references).
4. Rewrote `product_cli.py` with full test coverage (13/13 tests pass).
5. Implemented DEBUG-only runtime demo scenario selector enabling Firebase App Testing Agents to select controlled glucose scenarios via normal UI interaction.
6. Created importable Firebase YAML test cases.

---

## Architecture Review

### Demo Mode Data Flow (after this change)

```
Demo mode enabled
    ↓
GlucoseRepository.fetchMonitoringSnapshot()
    ↓
ScenarioAwareMockLibreLinkUpClient
    ├── DemoScenarioController.currentScenario == null
    │       → delegates to MockLibreLinkUpClient (unchanged behaviour)
    └── DemoScenarioController.currentScenario != null (DEBUG only)
            → ScenarioDataGenerator.generateReading(scenario, patientId, Instant.now())
```

### Key classes

| Class | Location | Purpose |
|-------|----------|---------|
| `DemoScenario` | `main/data/demo` | Enum of 9 scenarios |
| `DemoScenarioController` | `main/data/demo` | Thread-safe singleton; no-op in release |
| `ScenarioDataGenerator` | `main/data/demo` | Pure Kotlin data generator (no Android deps) |
| `ScenarioAwareMockLibreLinkUpClient` | `main/data/demo` | LibreLinkUpClient wrapping scenario logic |
| `DemoScenarioSelectorCard` | `main/ui/monitoring` | Compose UI selector (debug + demo only) |

### Release build safety

- `DemoScenarioController.selectScenario()` has `if (!BuildConfig.DEBUG) return` guard.
- `DemoScenarioSelectorCard` has `if (!BuildConfig.DEBUG) return` guard.
- R8/ProGuard eliminates all dead code paths in release APK.
- Live mode (non-demo) is unaffected in both debug and release.

---

## UI Changes

- New card "TEST / DEMO SCENARIO" visible in Home screen only when:
  - `BuildConfig.DEBUG == true` AND `state.isDemoMode == true`
- Production UI: **unchanged**.
- Release UI: selector is **absent** (dead code eliminated).

---

## Database Changes

No schema changes. No new migrations required.

---

## Product Schema Changes

| Change | Description |
|--------|-------------|
| `observation.schema.json` | Added `linked_test_runs` array with `TESTRUN-YYYY-NNN` pattern |
| `test_run.schema.json` | Canonical schema (unchanged) |
| `test-run.schema.json` | **Deleted** (was duplicate with old TEST- pattern) |

---

## Tests

| Test suite | Result |
|------------|--------|
| `DemoScenarioSelectorTest` (14 cases) | PASS |
| Product CLI self-tests (13 cases) | PASS |
| `./gradlew testDebugUnitTest` | PASS |
| `./gradlew lint` | PASS |

---

## Artifacts

| Artifact | Path | Size |
|----------|------|------|
| Debug APK | `release-artifacts/LibreCare-2.11.2-debug.apk` | 22.71 MB |
| Release APK | `release-artifacts/LibreCare-2.11.2-release.apk` | 3.51 MB |
| Release AAB | `release-artifacts/LibreCare-2.11.2-release.aab` | 6.34 MB |

**Google Play Upload File**: `release-artifacts/LibreCare-2.11.2-release.aab`

---

## Remaining Risks

1. **Local DB mixing**: When a Firebase agent switches between scenarios, the local Room DB may retain
   readings from the previous scenario. The UI shows the new reading's current value correctly, but
   the history chart may show a mix of old and new scenario data. Mitigation: agents should restart
   the app between distinct history scenarios.

2. **MISSING_DATA implementation**: Returns a reading with a 72-hour-old timestamp rather than null,
   to avoid throwing `SelectedPersonGraphException`. The ViewModel shows a stale-data state rather
   than a "no data" error. This is correct for Firebase testing purposes.

3. **connected device tests**: No connected device/emulator available — instrumented tests not run.

