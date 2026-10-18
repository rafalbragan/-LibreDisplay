# LibreCare 2.16.6 / 50 — quality review and compliance checklist

Date: 2026-10-18. **NOT RELEASE READY.** PASS below is scoped evidence, not Product Owner acceptance. FAIL can mean missing verification rather than an observed product defect. No mandatory FAIL is being waived.

## Summary / architecture

The controlled P0 scenario passes on the real MainActivity, MonitoringViewModel, repository/parser, Room and encrypted settings. Seven instrumentation methods pass, including three distinct-process restoration paths. The final JVM suite contains **512 tests, 0 failures, 0 errors, 0 skipped**. Source fingerprints did not change during final build/UI validation.

A second complete run passed **7/7** again, followed by **1/1 existing Room migration test** on the same guarded emulator. A fake physical target with a nonexistent ADB executable was rejected before any ADB invocation. Repeated-run source differences: zero. Results are under `release-artifacts/p0-evidence/final-repeat/` in the P0 worktree.

[Independent audit](p0-independent-audit-2026-10-18.md) records EXISTS / INCOMPLETE / MISSING before implementation across Activity/navigation, ViewModel, repositories/API, Room/migrations, sync, Demo Mode, Privacy & Data, statistics, widgets, charts and Compose. [Release report](release-report-2.16.6.md) records commands, local artifacts and blockers.

Implementation is limited to identity/data isolation, durable successful selection, recoverable selected-person graph failure, and three numerical crash producers. No schema, migration, alarm policy, treatment logic, NFZ thresholds, backup implementation, retention policy, signing configuration, DEMO generation or pinch zoom changed.

## Reproduction and regression evidence

Evidence root: `C:\Users\SG0216827\IdeaProjects\LibreCare-p0-20261018\release-artifacts\p0-evidence`.

| Failure | Before | Correction | After |
|---|---|---|---|
| Active reading attributed to inactive identity | `before-confirmed/inactiveSelection_canReturnWithoutMisattribution.txt`: real UI still held 147 | Clear prior-person data/history; reject obsolete async results | `final-p0/inactiveSelection_canReturnWithoutMisattribution.txt`: PASS; inactive no-data and active-restored screenshots |
| Legacy inactive selection prevents recovery | `before-legacy/`: real new process has selected ID null instead of inactive fixture | Carry actual persons in graph exception; preserve session and selector; clear invalid persisted choice | `final-p0/afterProcessRestart_legacyInactiveSelectionIsRecoverable.txt`: PASS |
| Range distribution divides 0ms by 0ms | `precision-before.xml`: 3 tests, 1 failure; real stale UI stack at calculateRangeDistribution/pct | Guard actual covered milliseconds | 3 precision tests PASS; stale UI and process restart PASS |
| Sensor activity divides 0ms by 0ms | `before-sensor-precision/` and `p0-before-sensor-precision.log`: 6 tests, 2 failures; UI stack at MonitoringScreen:1007 | Guard actual period milliseconds; no measurable period returns NO_DATA | 6 precision tests PASS; stale UI and restart PASS |
| Chart converts absolute epoch to Float, collapsing a short domain | `chart-precision-before.xml`: 3 tests, 2 failures; `pre-retry/retry-own-uid/stalePersonNeverDisplaysActivePersonsReading.txt`: stack at calculateSafeTextConstraints, GlucoseChart:121 | Shared chartTimeFraction subtracts Long timestamps before conversion; drawing/grid/labels/hit-testing agree | 3 precision tests PASS; same real stale UI and restart PASS |

Seven final instrumentation methods: inactive selection/return, last-good restart, legacy preparation, legacy restart/recovery, stale selection/return, stale restart, and switching while polling is in flight. Each file under `final-p0/` contains `OK (1 test)`. Preparation is one of the seven methods, not a separate recovery claim.

Screenshots under `final-p0/p0-evidence/`: `inactive-no-data.png`, `active-restored.png`, `process-restarted.png`, `legacy-startup-no-data.png`, `inactive-stale.png`, `stale-process-restarted.png`, `rapid-switch-no-misattribution.png`. Assertions verify actual rendered text and clickable person selection; no claim of a manual pixel-level visual review is made.

### Test boundaries

- Fixtures use synthetic HTTP, not a live account. Real classes and persistence remain in use; transport injection occurs after Application construction.
- The dedicated emulator runner temporarily disables BootReceiver using the debug app UID and restores default state in finally. Earlier Android 15 un-stop broadcasts could launch Activity before injection and invalidate synthetic session assertions. Production receiver/startup code is unchanged.
- Distinct PIDs and persisted session/selection are checked after host force-stop. Restart methods do not reseed data. This is not merely Activity recreation.
- These tests do not prove reboot/autostart, production background-service behavior, uninstrumented launcher startup, real network/account behavior, or the exact protected Samsung incident.
- Earlier harness failures and results from the unexpectedly reverted ViewModel are retained but are not authoritative final PASS evidence.

## Mandatory compliance checklist

### General and pre-implementation review

| Rule | Status | Evidence / limitation |
|---|---|---|
| Verify existing implementation first | PASS | Independently fetched baseline and source audit |
| Fix root causes, not symptoms | PASS | Selection/persistence and three numerical producers, not UI catch-all |
| Preserve existing functionality | PASS | Focused diff; 512 JVM tests; P0 UI scenario; not an assertion of full product acceptance |
| Add/update tests | PASS | Repository, precision and real-Activity/process regressions |
| Build application | PASS | Clean debug assembly; release blockers separately marked FAIL |
| Generate/update reports | PASS | Independent audit, release and quality reports |
| Update release notes | PASS | Bilingual 2.16.6 candidate notes |
| Update changelog | PASS | Bilingual candidate at top |
| Report exact artifact locations | PASS | Release report; missing release files explicitly reported |
| Review existing implementation and architecture | PASS | Audit architecture table |
| Review ViewModels | PASS | Bootstrap, selection, poll/manual fetch, history callbacks |
| Review repositories and API | PASS | Real parser exception, session behavior and fetched person context |
| Review Room and migrations | PASS | Schema v2 and preservation migration reviewed; no schema change |
| Review Demo Mode | PASS | Existing isolation reviewed; no generator edits |
| Review Privacy & Data | PASS | Source-only continuity audit; no user data accessed/restored |
| Review statistics | PASS | Existing diagnostics/estimates and metric consumers reviewed |
| Review widgets | PASS | Existing consumer paths; no widget implementation edits |
| Review charts | PASS | Rendering/hit-testing precision, metrics and existing tests reviewed |
| Review Compose screens and navigation | PASS | Real Activity entry, switcher and loading/no-data paths |
| Report EXISTS / INCOMPLETE / MISSING | PASS | Independent audit table and signing/history findings |

### UI and chart requirements

| Rule | Status | Evidence / limitation |
|---|---|---|
| Current glucose most important | PASS | Existing current-glucose card retained; actual 147/231 assertions |
| Monitored person appears only once | NOT APPLICABLE | No identity-layout redesign; full existing layout acceptance not claimed |
| Avoid duplicated information | NOT APPLICABLE | No new duplicated identity/value presentation |
| Keep dashboard compact | PASS | No added card or padding; no-data/loading text only |
| Readable typography | FAIL | Existing typography retained; full visual acceptance not performed |
| Avoid oversized padding | PASS | No padding changes |
| Avoid oversized cards | PASS | No new/enlarged cards |
| Readable chart | FAIL | Stale chart renders without crash; complete visual acceptance still missing |
| Touch interactions work | PASS | Actual person-switch clicks and in-flight switching verified; chart gestures separately below |
| All new user-facing strings Polish | PASS | Polish loading/no-data messages |
| Verify scaling | FAIL | No full font/display-scale matrix |
| Verify dark theme | FAIL | No independent dark/light acceptance matrix |
| Verify accessibility | FAIL | Compose semantics exercised, not full TalkBack/touch-target audit |
| Verify readability | FAIL | Text assertions and screenshots do not replace visual review |
| Verify phone-sized screen | PASS | sdk_gphone64_x86_64 dedicated API35 AVD, real Activity |
| Verify landscape history | FAIL | Landscape/full-history acceptance not performed |
| Chart tap opens full-screen history | FAIL | Callback reviewed; no actual full-screen chart-tap regression executed |
| Chart tooltip works | FAIL | Hit-testing precision tests pass; actual tooltip gesture not verified |
| Tooltip not hidden under finger | FAIL | Placement source reviewed; no touch/visual acceptance |
| Local timezone used | PASS | Existing device zone and PolishDateTimeFormatter retained; JVM suite |
| Min/max markers visible | FAIL | Existing chart renders selected-point marker, not independently verified min/max markers; no feature added outside P0 |
| Target range shading visible | FAIL | Existing drawBand preserved; screenshot captured, no pixel-level acceptance |
| Grid lines readable | FAIL | Shared finite coordinate mapping repaired; no full contrast review |
| Selected person preserved | PASS | UI identity and durable successful selection covered across process restart |
| Selected chart range preserved | FAIL | Range logic unchanged, but no real full-screen transition assertion |
| Do not mark chart complete if interaction unverified/broken | PASS | No complete chart/zoom acceptance claimed |

### NFZ, database, settings and statistics

| Rule | Status | Evidence / limitation |
|---|---|---|
| NFZ status, reason, recommendation, period, info dialog | NOT APPLICABLE | NFZ UI/evaluator unchanged; shared arithmetic guard only |
| NFZ never only red/green | NOT APPLICABLE | No NFZ UI change |
| NFZ failed/current/required/recommendation explanations | NOT APPLICABLE | No NFZ decision or explanatory text changes |
| Never claim final NFZ eligibility | PASS | No eligibility claim |
| NFZ wording “Na podstawie dostępnych danych...” | NOT APPLICABLE | No NFZ wording changed |
| Increase Room version on schema change | NOT APPLICABLE | Schema v2 unchanged |
| Create migration on schema change | NOT APPLICABLE | No schema change |
| Register migration on schema change | NOT APPLICABLE | Existing migration registration retained |
| Add migration test on schema change | NOT APPLICABLE | No new migration; existing preservation test reviewed |
| Never destructive migration in release | PASS | Existing fallback debug-only; no change |
| Preserve data retention | PASS | Existing settings/policy unchanged |
| Preserve polling frequency | PASS | Existing settings/interval unchanged |
| Preserve statistics | PASS | Existing diagnostics and ordinary-interval metric behavior retained |
| Preserve storage info | PASS | Existing implementation unchanged |
| Preserve transfer info | PASS | Existing implementation unchanged |
| Preserve privacy controls | PASS | Unchanged |
| Preserve account reset | PASS | Unchanged; not invoked on a real account |
| Report missing settings support | PASS | Audited source availability; no new settings feature required |
| Expose database size | PASS | Existing diagnostics source reviewed, unchanged |
| Expose database growth | PASS | Existing diagnostics source reviewed, unchanged |
| Expose reading count | PASS | Existing DAO/statistics source, unchanged |
| Expose monitored people count | PASS | Existing DAO/statistics source, unchanged |
| Expose downloaded transfer | PASS | Existing diagnostics source, unchanged |
| Expose uploaded transfer | PASS | Existing diagnostics source, unchanged |
| Expose sync count | PASS | Existing sync/statistics source, unchanged |
| Expose average usage | PASS | Existing estimates source, unchanged; no new numeric claims |
| Expose storage estimate | PASS | Existing estimates source, unchanged |
| Expose transfer estimate | PASS | Existing estimates source, unchanged |
| Insufficient-data message / never invent values | PASS | Existing estimate fallback preserved; no invented medical/storage/transfer values |

### Tests, build, artifacts and versioning

| Rule | Status | Evidence / limitation |
|---|---|---|
| Unit tests reviewed/run | PASS | 512/512 |
| ViewModel tests reviewed | PASS | Real ViewModel in seven instrumentation methods; existing JVM tests reviewed |
| Repository tests reviewed/run | PASS | Exact parser cause, person context, cancellation and stale identity |
| Migration tests reviewed | PASS | Existing RoomMigrationTest reviewed and executed on verified emulator: 1/1 PASS; new migration not applicable |
| Relevant Compose tests | PASS | Actual UI selection/no-data/stale assertions |
| Relevant chart interaction tests | FAIL | Pure precision hit-test regressions, not full gesture/navigation acceptance |
| Relevant navigation tests | FAIL | Real Activity navigation entry exercised, not full chart-navigation matrix |
| gradlew clean | PASS | Final clean run |
| gradlew testDebugUnitTest | PASS | Final full suite |
| gradlew lint | PASS | Debug/release lint task graph |
| gradlew assembleDebug | PASS | Final debug APK |
| gradlew assembleRelease | FAIL | Signing properties unavailable; no secret import authorized |
| gradlew bundleRelease | FAIL | Signing properties unavailable; no usable release AAB |
| Connected tests if emulator exists | PASS | Explicit verified emulator only; no unspecified connectedAndroidTest |
| “No connected device/emulator available.” when absent | NOT APPLICABLE | Dedicated emulator available |
| Locate all APK/AAB outputs | PASS | PowerShell recursive inventory in release report workflow |
| Report debug APK full path/size | PASS | Release report |
| Report release APK full path/size | FAIL | No successful release APK; absence explicitly reported |
| Report release AAB full path/size | FAIL | No successful release AAB; absence explicitly reported |
| Identify Google Play upload release AAB | FAIL | No usable file; never substitute debug APK or unsigned intermediate |
| Create/update release-artifacts | PASS | LibreCare-2.16.6-debug.apk and local evidence |
| LibreCare-named release APK/AAB | FAIL | Blocked by signing; not fabricated |
| Review versionName/versionCode | PASS | 2.16.2/46 baseline; observed local through 2.16.5/49; candidate 2.16.6/50 |
| Increase appropriately / never decrease code | PASS | Code 50 > 46 and 49; Play inventory unverified |
| Report previous and new version | PASS | Release report and bilingual notes |

### Documentation, branding, Git and safety

| Rule | Status | Evidence / limitation |
|---|---|---|
| Versioned release-notes path | PASS | docs/releases/LibreCare-2.16.6-release-notes.md |
| Release notes PL and EN | PASS | Both sections |
| Notes: version/fixes/features/improvements/tests/artifacts/limitations | PASS | Included, with links to exact outcomes |
| Changelog current version at top | PASS | Candidate 2.16.6 |
| Changelog PL/EN Added/Changed/Fixed/Tests/Artifacts | PASS | All sections |
| Versioned release report | PASS | docs/release-report-2.16.6.md |
| Applicable quality report | PASS | This file |
| Reports: summary/architecture/UI/database/tests/artifacts/risks | PASS | This file, release report and linked independent audit |
| Verify LibreCare branding and classify legacy names | PASS | Release report classification; package/database legacy names deliberately retained |
| Rename user-facing legacy names | NOT APPLICABLE | No new user-facing legacy name; app label is LibreCare |
| Final git status / focused diff review | PASS | Recorded in final handoff; generated evidence deliberately not staged |
| Ignore build/, APK, AAB, JKS, keystore, local.properties | PASS | Existing .gitignore covers all patterns |
| Commit source/tests/docs/reports and branch-only push | FAIL | Final Git outcome must be recorded after commit/push; not a readiness claim |
| Do not commit generated APK/AAB | PASS | Explicit source/docs staging only |
| No main-worktree overwrite/stash/reset/rebase | PASS | Separate P0 worktree; protected main preserved |
| No merge / auto-merge / direct master push | PASS | Dedicated branch only; human merge required |
| No physical Samsung modification | PASS | Emulator serial/qemu/AVD guards; no physical-phone operation |
| No private signing/key/password/token disclosure | PASS | Public APK certificates only; signing not imported; debug signer differs from inspected release signer |
| Signing and history audit READ-ONLY | PASS | No Play configuration, data recovery or backup changes |
| No alarm / medical decision change | PASS | Thresholds and policies unchanged |
| No Mika / zoom implementation | PASS | P0 arithmetic only; these feature requests remain outside authorization |
| Final response fields and exact artifacts/risks | PASS | Release report plus final handoff |
| Hard stop: no completion while mandatory FAIL | PASS | Overall task remains NOT RELEASE READY / acceptance incomplete |

## Remaining risks

Exact Samsung exception attribution, signed release artifacts, Google Play app-signing/upload certificates and code availability are unverified. No historical data recovery is attempted or promised; uninstall cannot be treated as an update path. Synthetic process tests exclude boot receiver and production service behavior. Full visual/accessibility/chart-gesture acceptance is incomplete. Local screenshots/logs are not uploaded automatically. Only human review can authorize merge or release.

