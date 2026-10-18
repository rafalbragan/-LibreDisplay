# LibreCare — independent P0, signing and history audit

Date: 2026-10-18. Scope: P0 implementation only; signing and historical continuity are READ-ONLY audits.

## Independently verified GitHub baseline

- Repository: `rafalbragan/-LibreDisplay`; API-confirmed default branch: `master`.
- `git fetch origin` succeeded before changes.
- Exact `origin/master`: `5433f28b04442d5dfb2af9777801109bee730b35` (unchanged from the previously reported SHA).
- Main checkout HEAD: `c12fd2777bbacf765a616aa4f21275bfd94521fa`; many pre-existing local modifications. Not used as implementation base, cleaned, stashed, reset, rebased or overwritten.
- New clean worktree: `C:\Users\SG0216827\IdeaProjects\LibreCare-p0-20261018`.
- New branch: `fix/p0-person-recovery-20261018`, directly from the verified master SHA. No integration branch was reused.

### PR audit

All four requested PRs were OPEN, merge state CLEAN and auto-merge disabled at inspection. CLEAN means mergeability, not product acceptance.

| PR | Exact head | Findings against current master |
|---|---|---|
| #35 | `99de52bbd9258a2a1b1dd2ab71cdbe06005b3eba` | MonitoringViewModel and two pure guard/freshness tests. Saves selection before graph success; does not fix startup recovery or NaN crash. |
| #40 | `d3c634ea7e649f46fd4f62d0fdcfda8fa240e7ea` | Broad chart/API/diagnostics/Features/product-record integration. Same ViewModel blob as #35. Not a suitable P0 base. |
| #41 | `f4bffb22c2a3d3e88dca7d639074d09717021d22` | Adds reading clearing and graph-failure handling to polling, plus constructed-state tests. Still persists failed choices, resets session on bootstrap graph failure and removes startup all-person sync. No real Activity/process proof. |
| #44 | `5fa26d25a06cebff179da1d7c74ab6d0a3f7a866` | Aggregates #41 behavior with demo, DAO, backup, alert, build and device-policy changes. Its instrumentation test constructs MonitoringUiState and calls selectPersonForSnapshot; no Activity, UI click, encrypted settings or process restart. |

Exact PR changed-file metadata was obtained with `gh pr view <number> --json files,headRefOid,baseRefOid,statusCheckRollup`. Each head was also compared using `git diff --name-only <master-SHA> <head-SHA>` and a content diff of MonitoringViewModel. This distinguishes PR three-dot changes from full tree comparisons: #35/#41/#44 full-tree comparisons also expose the master-only #28 governance records; they are not changes to import into P0.

Overlap/conflicts:
- All four overlap this repair in MonitoringViewModel. #40/#44 also overlap MonitoringScreen and app versioning; #44 overlaps GlucoseRepository.
- #41/#44 clearing alone does not address stale asynchronous history/poll results or durable failed selections.
- `analytics/GlucoseMetricsCalculator.kt` is unchanged in the requested PR heads relative to master. None resolves the reproduced millisecond-denominator crash.
- No cherry-pick, merge, rebase or modification of another PR was performed.

Other open PRs at audit: #43 backup safety, #42 Mika, #39 metrics/responsiveness tests, #38 chart navigation, #36 sync pressure (all CLEAN); #32 and #8 draft/UNSTABLE. Signing/history changes from these branches were not imported.

### What green Actions actually verify

- Android CI / Fast test suite invokes `scripts/test-fast.sh`: `testDebugUnitTest`, `lint`, `assembleDebug`. No emulator or connected test task.
- Android Debug Build invokes unit tests and debug assembly. No real Activity/process instrumentation execution.
- Android APK Build creates debug and optional release APKs; packaging/signature jobs are not crash-recovery tests.
- #44 Device safety policy is static policy validation, not a UI scenario.
- Requested PR Android checks were successful at their audited heads. Example #44 runs: Android CI `39118225461`, APK Build `39118225491`, Debug Build `39118225617`.
- Latest master Android CI run found: `38927902574`, successful at `ec32f1eeb53e35ec208a5f72d30db2dc1bb74323`; master then changed via governance-only #37. Do not describe that run as a new Android test execution on `5433f28`.

## Architecture / pre-implementation review

| Area | Status | Evidence / affectedness |
|---|---|---|
| Real Activity/navigation | EXISTS | MainActivity:69–106 resolves monitoring from stored session/credentials; MonitoringScreen uses its real ViewModel. Navigation implementation unchanged. |
| Selection persistence | INCOMPLETE | Baseline MonitoringViewModel:400 saves before fetch; applyDashboardSnapshot:928 only saves when UI identity differs, unsuitable for delayed persistence without correction. |
| Person/data isolation | INCOMPLETE | Baseline switch:408–418 retains prior reading and derived fields; async history:165–174 and polling:626 lack request-generation guards. |
| Bootstrap/error recovery | INCOMPLETE | Baseline bootstrap:297–313 resets session on any graph failure; failed snapshot never supplies person list to startup UI. |
| API/repository | EXISTS / INCOMPLETE | Real parser rejects missing current measurement at LibreLinkUpClient:109–110. GlucoseRepository:143–149 wraps it. Recovery needs fetched persons, not a fabricated reading. |
| Compose loading UI | INCOMPLETE | MonitoringScreen:228 replaces the whole dashboard, including person selector, while loading without a reading. |
| Range / sensor metrics | INCOMPLETE | Baseline range calculation tests Duration.isZero but divides by truncated milliseconds. Sensor activity also divides by truncated milliseconds after checking Instant ordering only. Positive sub-ms durations cause NaN/roundToInt exceptions in both paths. |
| Room and migrations | EXISTS | Schema v2, migration 1→2 registered. No schema or migration change in this repair. |
| Sync | EXISTS | GlucoseSyncRepository and app startup sync reviewed; behavior/cadence preserved. |
| Demo Mode | EXISTS | Separate synthetic client plus source/patient filtering. No demo generator or chart feature changes. Test fixtures are test-source-only and never use a real account. |
| Privacy & Data/settings/statistics | EXISTS | Retention, polling, account reset, storage/transfer reporting present; source reviewed, unchanged. Full acceptance of estimates is not asserted by this P0 audit. |
| Widgets/charts/NFZ | EXISTS / INCOMPLETE | Consumers of readings/metrics; chart coordinate arithmetic also loses short domains after casting epochs to Float. Coordinate and metric fixes only; gestures, thresholds, eligibility wording and alarm policy unchanged. |
| Activity + process regression | MISSING on baseline | Added in this repair; previous state/helper tests are not E2E evidence. |

## Reproduced P0 failures

1. Active → inactive with no graph measurement: real UI retains active `147` under inactive identity on baseline. Instrumentation assertion fails. The parser throws `NonRetryableLibreLinkUpException("Brak aktualnego pomiaru glukozy w odpowiedzi graph")`, wrapped by SelectedPersonGraphException.
2. Persisted legacy inactive selection → new process: baseline has no selected person / usable selector after failed bootstrap. Separate runner invocations and persisted PID comparison prove distinct processes, not Activity recreation.
3. Stale reading with empty graphData → Room/API copies with sub-ms difference: real Compose throws `IllegalArgumentException: Cannot round NaN value` at `GlucoseMetricsCalculator.calculateRangeDistribution$pct` (baseline line 105), invoked from GlucoseChartCard. The same stored stale selection reproduces the exception after process restart. A three-test precision regression has one failure before the arithmetic fix.
4. Once the range calculation was protected, the same real-Activity stale scenario reached a second NaN consumer: `GlucoseChartCard` at MonitoringScreen:1007 rounds `sensorActivityFromHistory(...).activityPercent`. `calculateSensorActivity` accepted a positive sub-ms period but divided by `total.toMillis() == 0`. New six-case tests produced **two failures before the sensor fix**. Equal/reversed periods already worked; the Instant-order comparison was not the defect. The fix guards the actual millisecond denominator and preserves normal-interval calculations and NFZ thresholds.
5. After both metric guards, the stale scenario reached `calculateSafeTextConstraints` at GlucoseChart:121 through axis-label drawing (baseline lines 652/895). Absolute epoch timestamps cast to Float collapse a one-millisecond domain; `minTime + 1f` also equals minTime at that magnitude. Both rendering and hit testing divided by zero. Three coordinate regressions produced **two failures before the chart fix**. Shared `chartTimeFraction` now subtracts Long timestamps before floating-point conversion, with a positive millisecond denominator, for points, axes, grid and hit testing. No catch around drawing, fabricated history, interpolation/zoom feature or medical change was added.

The stale timestamp deliberately tests supported Instant precision finer than Room's epoch milliseconds. This is a proven application crash path, **not proof that the protected Samsung encountered this exact exception**. No Samsung crash stack trace was supplied/verified in this audit. Instrumentation captures the Compose exception; it is not being mislabeled as an observed OS crash dialog on Samsung.

### Evidence integrity and test boundaries

- During the earlier session MonitoringViewModel unexpectedly reverted to baseline. Work stopped; the user explicitly authorized reapplying the P0 edits. Results from the reverted-source runs are not final evidence.
- On retry, the worktree already contained the sensor denominator fix, six sensor regression tests and newer fixture assertions not reflected in the conversation summary. These files and the before-fix failure log were read directly; no source was overwritten from the summary.
- `source-retry-start.csv` fingerprints Kotlin/XML sources, app version configuration and the emulator runner. The clean retry build reported zero source changes during validation; later runner-only fixes are recorded separately. Final outcomes are in the release and quality reports.
- Test transport is injected after the real Application is constructed. Android 15 may deliver BOOT_COMPLETED when a force-stopped package is un-stopped; the unchanged BootReceiver launches MainActivity before test injection, allowing the real client to use the intentionally unreachable synthetic endpoint. Earlier session-survival failures therefore cannot simply be reported as a fixed production persistence bug.
- The runner temporarily disables only this receiver on the dedicated emulator, using the owning debug app UID (`run-as`), and restores its default state in `finally`. It rejects pre-existing component overrides. Android's shell UID rejected the original component commands with SecurityException; PowerShell 5.1 also treated blank native stderr as a terminating exception. Native stderr is now captured and the exit code is checked explicitly.
- This exercises real Activity/ViewModel/navigation entry, Room, encrypted settings, UI clicks and distinct-process restoration under controlled HTTP. It does **not** cover uninstrumented launcher startup, boot-receiver behavior, production background-service behavior (disabled in fixture settings), real account/network behavior or Samsung firmware. No full unqualified end-to-end claim is made.
- All ADB commands use verified `emulator-5580`, qemu=1 and the dedicated LibreCare P0 AVD. No command targets the physical Samsung.

## READ-ONLY signing / Google Play audit

`applicationId` and namespace remain `com.libredisplay`. Existing build source supports external release signing properties and can also sign debug with that configuration when present. No signing configuration, keys or passwords were changed, imported or disclosed.

Public certificate inspection used `apksigner verify --print-certs` on APKs only. All three inspected APKs verify and share certificate SHA-256:

`14d768c2ecb104a48f239b3e9bc2518d09ad1fd2041c22bee42e9608c90655de`

| Existing APK (not the new repair) | APK SHA-256 |
|---|---|
| `C:\Users\SG0216827\IdeaProjects\LibreDisplay-wt-release-20261017b\release-artifacts\LibreCare-2.16.4-release.apk` | `39cdbee17e607ea7cf1140e2d944a3a32953249bd93411f8b0849a906bfb9d2d` |
| `C:\Users\SG0216827\IdeaProjects\LibreDisplay\release-artifacts\LibreCare-2.16.5-release.apk` | `88b94c7121e745795527c40d564a3dd91bb877c2f1d71dc9fda879cc67305197` |
| `C:\Users\SG0216827\IdeaProjects\LibreDisplay\release-artifacts\LibreCare-2.16.1-release.apk` | `91b4f2c4ff8647e31e8c524380e7c609899e1dac8ca31b71ceeff88af8cc517c` |

Findings:
- EXISTS: matching public signer among these local artifacts. Filenames are not proof of source provenance or Google Play certification.
- MISSING: authoritative Google Play **app signing certificate**, **upload certificate**, key-rotation lineage and Play version-code inventory. No Play Console configuration was accessed or changed.
- INCOMPLETE: Play compatibility. An upload-key match is not an installed-app signer match when Play App Signing uses a different key. Local matching certificates alone cannot establish Play update compatibility.
- MISSING: independent certificate of the APK currently installed on Samsung; no phone extraction/inspection was attempted.
- APK update continuity requires compatible package/signer lineage and acceptable version code; uninstall/reinstall is not an acceptable workaround.
- Candidate code 50 exceeds inspected source/artifact codes through 49; availability in Play remains unverified. No release publication or signing implementation is authorized by this audit.

## READ-ONLY historical glucose continuity audit

- Database: `libredisplay.db`, Room v2 (`LibreDisplayDatabase:14,26–27`). `MIGRATION_1_2` adds nullable sourceAccountId without removing existing rows. Application registers ALL_MIGRATIONS; destructive fallback is debug-only, not release.
- Readings identify patient + timestamp (`LocalGlucoseHistoryRepository:56–59`); Room converts timestamps to milliseconds. DAO queries include patientId. sourceAccountId is metadata, not the identity key. Null account IDs remain possible for migrated data.
- API backfill is bounded: sync uses 12h after LOGIN and 24h otherwise (`GlucoseSyncRepository:53–60`); these windows cannot reconstruct months of removed local history.
- Retention runs in sync's finally block, including failure paths (`GlucoseSyncRepository:137–144`). Default is 720 days, allowed range 12h–10 years (`AppSettings:43–45`). Reducing retention can remove local data; this audit makes no changes.
- Observed-person refresh may mark missing people inactive without deleting their readings. Local monitoring selects active persons, so a hidden profile does not by itself prove deletion.
- Privacy methods explicitly distinguish disconnect/session clearing from history/all-data deletion (`PrivacyRepository`). Android uninstall/clear-data removes app-private Room and local backup files. An in-place compatible update is different from reinstall.
- Automatic backup: `<filesDir>/backup/librecare-backup.json`, with `.previous` and temporary file (`LocalBackupStore`). Included in Android backup/transfer XML; encrypted preferences and the raw Room database are not included directly by those include-only rules.
- `createAutomaticBackup:247–253` rebuilds/writes even an empty bundle; no empty-overwrite guard on master. Six-hour worker can repeat this. `.previous` is not indefinite historical retention. PR #43/#44 address related behavior but were not imported.
- Bundle content may include configuration and persisted session. Backups are sensitive and were **not opened, decoded, copied, restored or uploaded** during this audit. Only source was read.
- LocalBackupStore verifies decoding and has a previous-copy fallback, but its fallback writes are not an unconditional atomicity guarantee. Cloud/OEM backup availability cannot be inferred from source configuration.
- EXISTS: merge/preview restore implementation, migration tests, local coverage queries and storage/transfer statistics.
- INCOMPLETE: end-to-end backup durability and account-isolation assurance; cloud copy existence, completeness and age are unknown.
- MISSING: authorized evidence of the phone's historical row counts/time coverage before and after the incident. No invented reading counts or recovery claims.

No history repair, migration, retention change, account reset, backup recovery, alarm change, medical threshold change, Google Play change or physical-phone operation was performed.

