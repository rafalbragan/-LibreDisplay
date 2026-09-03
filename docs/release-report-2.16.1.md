# LibreCare Release Report - 2.16.1

**Date**: 2026-09-02  
**Version**: 2.16.1 (versionCode 45)  
**Previous version**: 2.16.0 (versionCode 44)

---

## Summary

Automation Phase 1 is completed for accepted `REQ-0002` and Product Inbox Polish communication.
The caregiver multi-person monitoring cards use a centralized relative freshness formatter and do not expose raw ISO-8601 as the primary user-facing label.
Product Inbox human-facing GitHub communication remains Polish while preserving machine-readable enums and traceability from Inbox `#1` -> `REQ-0002` -> implementation -> tests.
Bug automation bootstrap is now resilient for Issue #5-shaped reports: canonical LibreCare bug forms are routed by title/body signature even when the `librecare-bug` label is missing.
BUG-0004 Android CI compilation regression is fixed by restoring trend projection logic and wiring the configured trend window through both monitoring layouts.

---

## Architecture Review (affected functionality)

| Area | Status | Notes |
|------|--------|-------|
| Existing implementation | EXISTS | `RelativeTimeFormatter`, `CaregiverAttentionView`, Product Inbox CLI/workflow and issue form already existed. |
| Architecture | EXISTS | Root-cause handling stays centralized in one reusable formatter; Product Inbox architecture was not redesigned. |
| ViewModels | EXISTS | `MonitoringViewModel` / `MonitoringUiState` flow preserved; no behavior redesign. |
| Repositories | EXISTS | No repository contract changes required. |
| Room database | EXISTS | No schema changes. |
| Migrations | NOT APPLICABLE | No Room version or migration update required. |
| API layer | EXISTS | No API contract changes. |
| Demo Mode | EXISTS | No new demo-mode behavior introduced. |
| Privacy & Data | EXISTS | No new data movement or storage introduced. |
| Statistics | EXISTS | Unchanged. |
| Widgets | EXISTS | Unchanged. |
| Charts | EXISTS | Unchanged; no chart code touched. |
| Compose screens | EXISTS | `CaregiverAttentionView` continues to render `Wymaga uwagi` / `Pozostali` with shared freshness text. |
| Navigation | EXISTS | Person-selection/navigation behavior unchanged. |

### Bug automation review

| Area | Status | Notes |
|------|--------|-------|
| Existing implementation | EXISTS | `.github/workflows/librecare-implementation-automation.yml`, `scripts/product/automation_cli.py`, and `scripts/product/tests/test_automation_cli.py` already existed. |
| Workflow routing | EXISTS | Job gating now uses canonical LibreCare bug-form signature instead of label-only routing. |
| CLI classification | EXISTS | `is_librecare_bug_issue(...)` accepts canonical bug-form issues without labels and excludes Product Inbox / implementation issues. |
| Deduplication | EXISTS | Reopened issue intake reuses the same canonical bug record via source deduplication. |
| Product Inbox exclusion | EXISTS | Product Inbox issue shape remains out of bug-import scope. |

---

## UI Changes

- Primary caregiver cards under `Wymaga uwagi` and `Pozostali` use the shared relative freshness formatter.
- Expected Polish examples remain covered: `przed chwilą`, `1 min temu`, `2 min temu`, `5 min temu`, `15 min temu`, `59 min temu`.
- Older data still uses a concise localized representation (`godz.`, `dzień/dni`).
- Missing timestamp is shown honestly (`brak czasu pomiaru` where configured).
- Invalid ISO text is handled honestly by the shared formatter (`nieprawidłowy czas`) without introducing fake data.
- Future clock skew remains clamped to `przed chwilą` and does not change stale classification.

---

## Database Changes

No Room schema/version/migration changes.

---

## Product Inbox / Traceability

- Preserved traceability: `Product Inbox #1` -> human `ACCEPT` -> `REQ-0002` -> implementation -> tests.
- No fake observation was created.
- No `TESTRUN` was created because Firebase was not executed in this phase.
- Human-facing Product Inbox GitHub text is Polish in:
  - review comment template,
  - decision output comment content,
  - workflow fallback decision text,
  - issue form labels/placeholders.
- Bug automation intake is now traceable for both label-backed bug issues and canonical bug-form issues that arrive without `librecare-bug`.
- Machine-readable values remain unchanged:
  - classifications (`PRODUCT_PROBLEM`, `PRODUCT_OPPORTUNITY`, `SAFETY_GAP`, `VALIDATED_CAPABILITY`, `TEST_COVERAGE_GAP`, `INCONCLUSIVE`)
  - decisions (`ACCEPT`, `HOLD`, `REJECT`)
  - commands (`/accept`, `/hold`, `/reject`)
  - IDs (`REQ-*`, `INBOX-*`, `CAND-*`).

---

## Tests

| Command / scope | Result |
|-----------------|--------|
| `python scripts/product/product_cli.py validate` | PASS |
| `python -m unittest scripts.product.tests.test_automation_cli` | PASS |
| Targeted Product Foundation Polish/traceability tests (`8` selected unittest cases) | PASS |
| `./gradlew clean` | PASS |
| `./gradlew testDebugUnitTest` | PASS |
| `./gradlew lint` | PASS |
| `./gradlew assembleDebug` | PASS |
| `./gradlew assembleRelease` | PASS |
| `./gradlew bundleRelease` | PASS |
| Firebase | NOT RUN |
| Connected device / emulator tests | NOT RUN |

**Notes**
- A broader `python -m unittest scripts.product.tests.test_product_cli` sweep was observed to contain pre-existing failures outside Automation Phase 1 scope (later-phase PR/bug automation expectations). Those were not expanded in this task per scope constraints.
- No connected device/emulator available.

### Added/updated tests
- `app/src/test/java/com/libredisplay/ui/monitoring/RelativeTimeFormatterTest.kt`
- `scripts/product/tests/test_product_cli.py`
- `scripts/product/tests/test_automation_cli.py`

---

## Artifacts

| Artifact | Path | Size |
|----------|------|------|
| Debug APK | `C:\Users\SG0216827\IdeaProjects\LibreDisplay\release-artifacts\LibreCare-2.16.1-debug.apk` | `23,924,698 B` |
| Release APK | `C:\Users\SG0216827\IdeaProjects\LibreDisplay\release-artifacts\LibreCare-2.16.1-release.apk` | `3,712,135 B` |
| Release AAB | `C:\Users\SG0216827\IdeaProjects\LibreDisplay\release-artifacts\LibreCare-2.16.1-release.aab` | `6,733,328 B` |
| Lint HTML report | `C:\Users\SG0216827\IdeaProjects\LibreDisplay\app\build\reports\lint-results-debug.html` | `398,246 B` |

---

## Branding Verification

| Search target | Classification | Result |
|---------------|----------------|--------|
| `LibreDisplay` in user-facing Product Inbox text | user-facing | not used in new human-facing copy |
| `com.libredisplay` package/applicationId | package/applicationId | unchanged technical identifier |
| Existing `LibreDisplay` package declarations | technical | unchanged |
| `LibreDisplay` in older design/ideation docs | documentation legacy | present in historical docs; not changed by this automation fix |
| `LibreCare` in reports/comments | documentation/user-facing | preserved |

---

## Remaining Risks

1. The workspace contains many unrelated in-progress modifications from other efforts; this report covers only the verified automation-repair scope and current 2.16.1 artifacts.
2. Firebase and connected-device verification were not run in this environment.
3. Git push is still blocked from this working tree until unrelated local changes are isolated and the local `master` branch is rebased onto `origin/master`.
