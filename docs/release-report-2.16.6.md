# LibreCare 2.16.6 / 50 — P0 candidate release report

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



