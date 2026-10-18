# LibreCare 2.16.6 / 50 — P0 candidate release report

## Current HTTP follow-up — 2026-10-18

**Scoped P0 follow-up, NOT a release approval.** Current evidence below supersedes historical paths/counts later in this document. Previous/new version: **2.16.6 / 50 → 2.16.6 / 50**, unchanged by explicit minimal-scope authorization. CHANGELOG remains unchanged; existing bilingual notes and both reports are updated.

### Execution boundary and provenance

- Protected checkout: `C:\Users\SG0216827\IdeaProjects\LibreDisplay`.
- Authorized separate worktree: `C:\Users\SG0216827\IdeaProjects\LibreCare-p0-http-20261018`.
- Verified canonical path, Git toplevel, worktree registration and detached parent HEAD: **`e71d1d12473a151909d112d95e6d57f130578e9b`** (PR #45 before this follow-up).
- Remote base: **`5433f28b04442d5dfb2af9777801109bee730b35`**. Read-only PR #43 head: **`1547badb7a1a9f87b290c04925638c6505db46d1`**.
- Delivery destination only: `origin/fix/p0-person-recovery-20261018`, existing draft PR #45. The exact resulting commit and independently checked Actions are recorded in the final handoff; no earlier run is used as proof for a new SHA.
- **Original execution-boundary incident:** Gradle ran from the protected checkout. Generated or ignored files there **may have changed**. Git status does not prove otherwise. The earlier “No tests found” is not RED evidence. No claim is made that the protected checkout was untouched across that incident.
- During this resume every Gradle command independently checked current directory, Git toplevel, detached branch and exact HEAD, then launched the absolute wrapper with an explicit process working directory. Helper and per-invocation boundary logs: `C:\Users\SG0216827\IdeaProjects\LibreCare-p0-http-20261018\build\p0-http-evidence\`.
- No clean was run during recovery. No Gradle ran from the protected checkout, no physical-device ADB operation occurred, and Samsung SM-S948B was not modified. No merge, auto-merge or release publication.
- Protected tracked/untracked status comparison after validation: **0 differences** from the resume snapshot; this does **not** audit ignored/generated files. Source fingerprint comparison: **0 differences** from `build/p0-http-evidence/final-validated-source.csv` during final tests/builds/emulator execution.

### Implementation / architecture

Added a foreground typed causal-chain classifier preserving original exception, HTTP metadata and selected-person context. Explicit 401/403 invalidates sessions without deleting history; 429/430 preserves sessions and cooldown; 5xx/network failures retain cached data with visible stale/error state and existing backoff. Only typed missing measurement is no-data. Parsing/unexpected failures remain visible and require manual retry. Cancellation is rethrown and loading is cleared.

Startup, manual connection, poll and queued person switch share the fetch boundary. Old person results cannot replace new identity; account-wide rejection/limits are still processed. Valid sessions are reused; rejected requests do not silently relogin. Cached history does not prove authentication; invalid timestamps are rejected instead of fabricated as now. Tests also cover retry cleanup, startup handoff and visible cooldown after screen re-entry.

No schema/migration, backup implementation, privacy/statistics policy, chart/numerical calculation, NFZ/medical/alarm, DEMO generator, signing, CI workflow, version or CHANGELOG implementation changes. Existing startup all-person sync and background-service classifier policy remain separate, not claimed as comprehensively redesigned or verified by this foreground suite.

### Actual commands and evidence

All Gradle commands below ran in the authorized worktree, with fresh expected-HEAD assertions; Windows `gradlew.bat` is the wrapper equivalent.

| Phase | Actual result |
|---|---|
| Retained test discovery | Class compiled/discovered; initial scheduler-drain harness hang stopped in the verified worktree worker; NOT RED |
| Original focused RED | **41 tests, 37 assertion failures, 0 errors/skips**, unchanged production source; `red-assertions.xml` |
| Additional lifecycle/cooldown RED | 65 tests/3 failures; then 58 tests/4 failures; preserved XML |
| Final focused `testDebugUnitTest --tests ...` | **105 passed**, including 58 ViewModel / 8 classifier / 3 repository / 18 auth / 18 real-client tests; 35s |
| `testDebugUnitTest lint assembleDebug assembleDebugAndroidTest` | **PASS**, 2m13s; **578 JVM tests, 0 failures/errors/skips**; lint/debug/test APK complete |
| Dedicated P0 instrumentation, first pass | All 7 method files PASS; outer PowerShell stderr handling interrupted clean harness reporting after screenshot pull; not final wrapper PASS evidence |
| Dedicated P0 instrumentation, final repeat | **PASS 7/7**, process exit 0, receiver explicitly restored to default; `http-final` evidence |
| Existing `RoomMigrationTest` on verified emulator | **PASS 1/1**; real SQLite migration preserves people/readings/settings |
| `assembleRelease bundleRelease --continue` | **FAIL**, 2m57s: both `packageRelease` and `packageReleaseBundle` fail on missing signing properties |
| Clean / generic connected-device Gradle task / physical-device tests | NOT RUN — recovery/target restrictions |

Target: **emulator-5580**, **LibreCare_P0_20261018**, **sdk_gphone64_x86_64**, **qemu=1**, API35. Every test operation uses an explicit emulator serial and identity guard. The seven existing methods include preparation and three separate-process restoration paths; this is not seven independent end-to-end user journeys. Synthetic transport is injected after Application construction; BootReceiver is temporarily disabled on the emulator and restored. No boot/autostart, physical Samsung, production-account/network or complete background-service guarantee is implied.

Evidence paths (all existing under the authorized root):

- `build\p0-http-evidence\red-assertions.xml`, `red-edge-cases.xml`, `red-context-cooldown.xml`.
- `build\p0-http-evidence\final-focused-xml\` and `final-focused-stdout.log`.
- `build\p0-http-evidence\final-full-stdout.log`, `final-full-exit.log`, `final-full-boundary.log`.
- `app\build\reports\tests\testDebugUnitTest\index.html`, `app\build\test-results\testDebugUnitTest\`.
- `app\build\reports\lint-results-debug.html` (PASS does not mean all pre-existing warnings removed).
- `build\p0-evidence\http-final\`: seven method results and `p0-evidence\` screenshots.
- `build\p0-http-evidence\emulator-final-stdout.log`, `emulator-final-stderr.log`, `emulator-final-exit.log`, `RoomMigrationTest.txt`.
- `build\p0-http-evidence\release-verification-stdout.log`, `release-verification-stderr.log`, `release-verification-boundary.log`.

### Current artifacts — local/debug only

**DEBUG APK:** `C:\Users\SG0216827\IdeaProjects\LibreCare-p0-http-20261018\release-artifacts\LibreCare-2.16.6-debug.apk` — **23,832,220 bytes**.

Gradle output: `C:\Users\SG0216827\IdeaProjects\LibreCare-p0-http-20261018\app\build\outputs\apk\debug\app-debug.apk`, same size/hash. SHA-256: `f867e5e9c48e33580a2dfc0a70eed5d88205dca2d91c3e3f2ac3db874d09e4e3`. APK DEX independently contains the new `MonitoringFailure` class. Public signature verification passed; debug certificate SHA-256 `962cb41fd3f7ff5dd9c64d71385f459eebf7a8a2446e06ccb02e35572958bd07`. This is **not** a production-compatible update or a Samsung install authorization.

Instrumentation APK: `C:\Users\SG0216827\IdeaProjects\LibreCare-p0-http-20261018\app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk` — **1,122,765 bytes**, emulator-only.

**RELEASE APK:** unavailable; size not applicable. **RELEASE AAB:** unavailable; size not applicable. **GOOGLE PLAY UPLOAD FILE:** **NONE**, size not applicable. Missing properties: `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`, `RELEASE_STORE_FILE`, `RELEASE_STORE_PASSWORD`. No secret import or signing bypass was performed. Recursive APK/AAB search found only the two debug outputs/named copy and a historical PR #43 CI debug APK under `build\p0-http-evidence\pr43-ci\`; that downloaded file is not this follow-up's deliverable. No AAB found.

### Exact follow-up files / Git / recommendations

Expected follow-up diff against `e71d1d12473a151909d112d95e6d57f130578e9b` (relative to the authorized root):

1. `app/src/main/java/com/libredisplay/data/api/LibreLinkUpClient.kt`
2. `app/src/main/java/com/libredisplay/data/api/LibreLinkUpExceptions.kt`
3. `app/src/main/java/com/libredisplay/data/repository/AuthRepository.kt`
4. `app/src/main/java/com/libredisplay/data/repository/GlucoseRepository.kt`
5. `app/src/main/java/com/libredisplay/ui/monitoring/MonitoringFailure.kt`
6. `app/src/main/java/com/libredisplay/ui/monitoring/MonitoringViewModel.kt`
7. `app/src/test/java/com/libredisplay/data/api/RealLibreLinkUpClientTest.kt`
8. `app/src/test/java/com/libredisplay/ui/monitoring/MonitoringFailureTest.kt`
9. `app/src/test/java/com/libredisplay/ui/monitoring/P0HttpRecoveryViewModelTest.kt`
10. `docs/releases/LibreCare-2.16.6-release-notes.md`
11. `docs/release-report-2.16.6.md`
12. `docs/quality-review-report-2.16.6.md`

Only these source/test/documentation files may be staged. Existing ignore rules cover build/, APK, AAB, JKS, keystore and local.properties. Branding remains LibreCare; package/applicationId, legacy database/class identifiers, repository paths and historical/generated references retain technical LibreDisplay naming. No dependency added. Diff scope and unchanged remote parent must be reverified immediately before the sole delivery push to PR #45; resulting SHA/Actions are reported in handoff.

**PR #43: HOLD** — read-only review at `1547bad…`, with independent CI XML (499 passing tests, 57 backup tests). Conditional write race, overstated restore readiness and fallback/shared-file mismatch need scoped correction; inherited checksum/atomicity/REPLACE limitations are separately classified in the quality report. No PR #43 change.

**PR #45: scoped ACCEPT recommendation after exact-head delivery/Actions verification**, not merge/publication authority. Remaining risks: original boundary incident's ignored/generated-file uncertainty; release signing/Play compatibility; real-account/background-service/autostart behavior; Samsung attribution; full UI/theme/scaling/accessibility/landscape acceptance; independent PR #43 backup risks. Complete release acceptance remains blocked. The quality report contains PASS / FAIL / NOT APPLICABLE compliance statuses.

## Historical original P0 report (retained; different worktree/evidence)

2026-10-18 — **NOT RELEASE READY / wydanie zablokowane**. Controlled P0 regressions pass; exact Samsung incident attribution, full UI acceptance and signed release delivery remain incomplete. No Product Owner acceptance, merge or publication is implied.

## Source provenance and scope

- Application: LibreCare; applicationId remains `com.libredisplay`.
- Worktree: `C:\Users\SG0216827\IdeaProjects\LibreCare-p0-20261018`.
- Branch: `fix/p0-person-recovery-20261018`.
- Independently fetched/reverified base `origin/master`: `5433f28b04442d5dfb2af9777801109bee730b35`.
- Previous committed version: **2.16.2 / 46**. Observed pre-existing local artifacts through **2.16.5 / 49**. Candidate: **2.16.6 / 50**; Play availability of 50 is unknown.
- Debug output metadata: `2.16.6-debug`, versionCode 50, minSdk 26. Existing debug suffix is preserved.
- The dirty main checkout was not used as implementation base or reset/stashed/rebased/overwritten. No integration branch was reused.
- Final source fingerprint: `C:\Users\SG0216827\IdeaProjects\LibreCare-p0-20261018\release-artifacts\p0-evidence\source-final-start.csv`. **Zero source changes** during final clean build and emulator run.
- Implementation commit: `b060549198f5198df2a591778e910fe750ac1d8f`, pushed only to `origin/fix/p0-person-recovery-20261018`; remote SHA independently matched. Implementation commit diff: 18 files, 1,380 insertions, 46 deletions. Subsequent changes are documentation-only verification metadata; exact final tip is reported in the handoff.
- GitHub Actions query on the implementation SHA returned no runs (`[]`); no green CI claim is made for it. No PR was created, merged or configured for auto-merge. No direct master push.

## Summary / architecture review

[Independent audit](p0-independent-audit-2026-10-18.md) records exact PR #35/#40/#41/#44 heads, overlaps, CI limitations and EXISTS / INCOMPLETE / MISSING findings. None of those PR heads repairs the reproduced metric denominator crash. Their helper-state tests are not substituted for the new Activity/process tests.

P0 changes:

1. GlucoseRepository preserves cancellation and attaches fetched persons to SelectedPersonGraphException.
2. MonitoringViewModel treats a switch as tentative until a successful snapshot, clears previous-person reading/history/derived state, rejects obsolete asynchronous results, waits for an explicit switch behind a poll, and renders recoverable selected-person graph failures without discarding the session.
3. MonitoringScreen retains the actual switcher while loading or without readings, with Polish messages.
4. GlucoseMetricsCalculator guards zero millisecond denominators in range distribution and sensor activity when API/Room timestamp precision differs.
5. GlucoseChart uses the same Long-difference time fraction for drawing, axes, grid and hit-testing. Casting absolute epochs to Float previously collapsed short domains and produced NaN coordinates.

### UI / database / exclusions

No card sizing, typography, visual redesign, chart interpolation/zoom feature or DEMO generator change. No Room version/schema/migration change: v2 and registered migration 1→2 remain. No destructive release migration introduced. Existing polling frequency, startup all-person sync, retention, privacy/account reset, storage/transfer statistics and widgets remain supported.

No alarm behavior, NFZ thresholds, medical decision logic, signing configuration, Google Play release configuration or backup/historical recovery implementation changed. Full visual/accessibility/chart-interaction acceptance remains explicitly incomplete in the [quality checklist](quality-review-report-2.16.6.md).

## Tests and builds

JDK: `C:\Program Files\Microsoft\jdk-17.0.20.101-hotspot`.
SDK: `C:\Users\SG0216827\IdeaProjects\LibreDisplay\.android-sdk`.
Commands ran from the P0 worktree using Windows PowerShell and `gradlew.bat` (Windows equivalent of the required Gradle wrapper commands).

| Command / check | Result |
|---|---|
| `gradlew.bat clean testDebugUnitTest lint assembleDebug assembleDebugAndroidTest --console=plain` | PASS, 1m39s; 87 tasks: 55 executed, 31 cached, 1 up-to-date |
| `clean` | PASS |
| `testDebugUnitTest` | PASS: **512 tests, 0 failures, 0 errors, 0 skipped** |
| `lint` | PASS; no claim that all pre-existing warnings are removed |
| `assembleDebug` | PASS |
| `assembleDebugAndroidTest` | PASS |
| `scripts\test-p0-emulator.ps1 -Serial emulator-5580 -Adb <SDK>\platform-tools\adb.exe -Phase All -EvidenceLabel final-p0` | PASS: **7/7 instrumentation methods** |
| Repeated complete P0 suite (`final-repeat`) | PASS: another **7/7**, unchanged source fingerprints |
| `RoomMigrationTest` via guarded emulator instrumentation | PASS: 1 existing row-preservation test |
| Physical-target rejection with nonexistent ADB executable | PASS: rejected before any ADB invocation |
| `assembleRelease` | FAIL, exit 1 after 6m38s, `:app:packageRelease`: signing unavailable |
| `bundleRelease` | FAIL, exit 1 after 2s, `:app:packageReleaseBundle`: signing unavailable |
| Physical Samsung test / unspecified connectedAndroidTest | NOT RUN — prohibited |

Both final release attempts fail closed with: `Release signing is not configured. Missing properties: RELEASE_KEY_ALIAS, RELEASE_KEY_PASSWORD, RELEASE_STORE_FILE, RELEASE_STORE_PASSWORD.` No secrets were read/imported and no signing bypass was added. There is no authorized basis to produce a compatible signed release in this isolated worktree.

### Real Android evidence

Target: **emulator-5580**, AVD **LibreCare_P0_20261018**, API35, model **sdk_gphone64_x86_64**, qemu=1. The host verifies serial, qemu, model and dedicated AVD before commands. No physical-phone-specific command was issued.

| Instrumentation method | Final result |
|---|---|
| inactiveSelection_canReturnWithoutMisattribution | PASS |
| afterProcessRestart_lastGoodSelectionStarts | PASS |
| prepareLegacyInactiveSelectionForProcessRestart | PASS |
| afterProcessRestart_legacyInactiveSelectionIsRecoverable | PASS |
| stalePersonNeverDisplaysActivePersonsReading | PASS |
| afterProcessRestart_staleSelectionDoesNotCrash | PASS |
| pendingPolling_cannotHideSwitcherOrOverwriteLatestChoice | PASS |

Real UI selects the inactive fixture, asserts no foreign 147 reading/history, returns to active reading/history, selects inactive again, and is relaunched in a distinct process. Legacy invalid and own-stale persisted selections are also exercised. Restart tests preserve settings/session/Room and check different persisted PIDs; they do not reseed.

**Boundary:** synthetic HTTP is injected after Application construction. To prevent Android 15 BOOT_COMPLETED un-stop delivery from launching Activity before injection, the runner temporarily disables BootReceiver under the debug app UID, restoring default state in finally. This is not proof of boot autostart, uninstrumented launch, production background-service behavior, real network/account behavior or Samsung firmware. Earlier pre-injection session failures and source-rewrite-invalidated runs are not final PASS evidence.

Exact Samsung crash stack trace remains unavailable. Three actual emulator exceptions and their numerical producers are documented in the independent audit; resemblance is not incident attribution. Before-fix precision suites failed **1/3**, **2/6**, **2/3** cases respectively; all now pass. Screenshots and real UI assertions—not constructed MonitoringUiState alone—support the controlled P0 result.

## Exact local evidence paths

- Build log: `C:\Users\SG0216827\IdeaProjects\LibreCare-p0-20261018\p0-final-build.log`.
- UI log: `C:\Users\SG0216827\IdeaProjects\LibreCare-p0-20261018\p0-final-ui.log`.
- Release APK retry log: `C:\Users\SG0216827\IdeaProjects\LibreCare-p0-20261018\assembleRelease-p0-retry.log`.
- Release bundle retry log: `C:\Users\SG0216827\IdeaProjects\LibreCare-p0-20261018\bundleRelease-p0-retry.log`.
- JVM report: `C:\Users\SG0216827\IdeaProjects\LibreCare-p0-20261018\app\build\reports\tests\testDebugUnitTest\index.html`.
- Lint report: `C:\Users\SG0216827\IdeaProjects\LibreCare-p0-20261018\app\build\reports\lint-results-debug.html`.
- Preserved final method results: `C:\Users\SG0216827\IdeaProjects\LibreCare-p0-20261018\release-artifacts\p0-evidence\final-p0`.
- Repeat and migration results: `C:\Users\SG0216827\IdeaProjects\LibreCare-p0-20261018\release-artifacts\p0-evidence\final-repeat` (7 P0 methods plus `RoomMigrationTest.txt`).
- Seven screenshot files: `C:\Users\SG0216827\IdeaProjects\LibreCare-p0-20261018\release-artifacts\p0-evidence\final-p0\p0-evidence`.
- Before-fix and earlier invalidated/harness evidence: `C:\Users\SG0216827\IdeaProjects\LibreCare-p0-20261018\release-artifacts\p0-evidence` (see quality report for classification).

## Artifacts — not published, no phone installation

### DEBUG APK

- Named copy: `C:\Users\SG0216827\IdeaProjects\LibreCare-p0-20261018\release-artifacts\LibreCare-2.16.6-debug.apk`.
- Gradle output: `C:\Users\SG0216827\IdeaProjects\LibreCare-p0-20261018\app\build\outputs\apk\debug\app-debug.apk`.
- Size: **23,832,220 bytes**.
- SHA-256: `8be1821de5b340454dc9d960b2707dcea4aeb8db5e39cd2c9cbc6c7807eb758a`.
- Public APK signature verification: PASS (apksigner exit 0); certificate SHA-256 `962cb41fd3f7ff5dd9c64d71385f459eebf7a8a2446e06ccb02e35572958bd07`. This is **different** from the inspected existing release signer; do not treat this debug APK as a compatible production update.
- Debug/test artifact only. **Not a certified production update; do not install on the protected Samsung.**

### Instrumentation APK

- `C:\Users\SG0216827\IdeaProjects\LibreCare-p0-20261018\app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk`.
- Size: **1,122,765 bytes**. Contains synthetic fixtures; emulator only.

### RELEASE APK

**Not generated successfully. Path: unavailable. Size: not applicable.** Intended named output would be `C:\Users\SG0216827\IdeaProjects\LibreCare-p0-20261018\release-artifacts\LibreCare-2.16.6-release.apk`; it is not an existing deliverable.

### RELEASE AAB

**Not generated successfully. Path: unavailable. Size: not applicable.** Intended named output would be `C:\Users\SG0216827\IdeaProjects\LibreCare-p0-20261018\release-artifacts\LibreCare-2.16.6-release.aab`; it is not an existing deliverable. Any intermediate bundle is not a signed release and must not be relabeled as one.

### GOOGLE PLAY UPLOAD FILE

**NONE — blocked. Path/size: not applicable.** Only a verified, authorized release AAB could satisfy this requirement. Debug APK, test APK and unsigned intermediates cannot substitute for it.

## READ-ONLY signing and historical continuity

The independent audit records public signer SHA-256 `14d768c2ecb104a48f239b3e9bc2518d09ad1fd2041c22bee42e9608c90655de` shared by the inspected existing 2.16.1/2.16.4/2.16.5 release APKs. Google Play app-signing certificate, upload certificate, rotation lineage, installed Samsung signer and version-code inventory are **unverified**. A local matching signer alone is not proof of Play update compatibility.

History audit: Room patient/timestamp identity and additive migration preserve existing rows on compatible updates; sourceAccountId is metadata. API backfill is only 12/24h, so it cannot reconstruct deleted months. App-private automatic backup and `.previous` do not prove cloud durability; baseline empty-overwrite risk was reported, not repaired. No user backup was opened or restored; no historical row counts were invented. Never use uninstall/reinstall as a compatibility workaround.

## Branding / Git / remaining risks

Branding search covers LibreDisplay / LIBREDISPLAY / libredisplay (case-insensitive). Android resource scan has no legacy user-facing label; app label is LibreCare. The manifest class `.LibreDisplayApp`, Kotlin package/type/theme identifiers and applicationId are technical legacy names. `libredisplay.db` is migration/storage legacy and is deliberately unchanged. Repository paths, old docs, CI artifact labels and generated test output are documentation/generated-output references, not new app branding. New notes/artifact names use LibreCare.

Existing .gitignore excludes build/, APK, AAB, JKS, keystore and local.properties. Only explicitly selected source/tests/docs/reports may be committed. Local generated APK, screenshots/logs/source fingerprints and transient validation TSV are not staged. Main checkout and other PRs remain untouched. Final branch commit/push and exact remote HEAD must be verified in the handoff; no merge or auto-merge.

Commit/push verification succeeded for the implementation SHA above. Post-commit tracked working tree was clean; only local `release-artifacts/p0-evidence/` and `validation-p0.tsv` remained untracked. `git diff --cached --check` reported only extra blank lines at EOF in new tests/docs; these cosmetic warnings were retained rather than modifying fingerprinted test inputs after execution. No generated binary or private signing file is in the commit.

Main-worktree status comparison after implementation: **0 differences**. Expected source/doc scope (all relative to the absolute P0 worktree above):

- `app/build.gradle.kts`
- `app/src/main/java/com/libredisplay/analytics/GlucoseMetricsCalculator.kt`
- `app/src/main/java/com/libredisplay/data/repository/GlucoseRepository.kt`
- `app/src/main/java/com/libredisplay/ui/monitoring/GlucoseChart.kt`
- `app/src/main/java/com/libredisplay/ui/monitoring/MonitoringScreen.kt`
- `app/src/main/java/com/libredisplay/ui/monitoring/MonitoringViewModel.kt`
- `app/src/androidTest/java/com/libredisplay/ui/monitoring/P0PersonRecoveryInstrumentedTest.kt`
- `app/src/sharedTest/java/com/libredisplay/ui/monitoring/P0RecoveryFixture.kt`
- `app/src/test/java/com/libredisplay/analytics/RangeDistributionPrecisionRegressionTest.kt`
- `app/src/test/java/com/libredisplay/analytics/SensorActivityPrecisionRegressionTest.kt`
- `app/src/test/java/com/libredisplay/ui/monitoring/ChartTimestampPrecisionRegressionTest.kt`
- `app/src/test/java/com/libredisplay/ui/monitoring/P0RepositoryFailureTest.kt`
- `scripts/test-p0-emulator.ps1`
- `CHANGELOG.md`
- `docs/p0-independent-audit-2026-10-18.md`
- `docs/releases/LibreCare-2.16.6-release-notes.md`
- `docs/release-report-2.16.6.md`
- `docs/quality-review-report-2.16.6.md`

Remaining risks: unavailable signed release/Play evidence; exact Samsung attribution; controlled-fixture lifecycle exclusions; incomplete dark/scaled/accessibility/landscape/tooltip/full-history UI acceptance; known backup durability/account-isolation limitations from the read-only audit. Mika and pinch zoom are not implemented or accepted by this P0 branch.

[Mandatory PASS / FAIL / NOT APPLICABLE checklist](quality-review-report-2.16.6.md). **Overall completion remains blocked while mandatory FAIL items remain.**



