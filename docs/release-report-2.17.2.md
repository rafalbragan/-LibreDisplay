# LibreCare 2.17.2 / 53 — API 36 release report

Implementation: 2026-10-19. Final local artifact verification: 2026-10-20.

**Local build, tests and signing: PASS. Google Play acceptance: not yet verified.**

## Identity and scope

- Previous version: **2.17.1 / 52**, compile/target SDK 35; new version: **2.17.2 / 53**, compile/target SDK **36**, minSdk **26**.
- Application ID remains **`com.libredisplay`**; user-facing brand remains **LibreCare**.
- Owner explicitly confirmed code **53** unused across uploaded bundles, tracks and drafts. This is owner confirmation, not a direct Play inventory query.
- Authorized worktree: `C:\Users\SG0216827\IdeaProjects\LibreCare-delivery-20261018`.
- Branch: `feature/libreview-chart2-play-20261018`; existing PR: https://github.com/rafalbragan/-LibreDisplay/pull/46.
- Initial local/remote/PR HEAD verified: `9a3a0224babe3c3eab1a86235cbe9b97f0afef55`; PR open, no auto-merge, initial applicable checks successful.
- No new PR, force push, merge, auto-merge, physical Samsung operation, uninstall, app-data clear or Play publication.

## Compatibility changes / architecture review

1. Compile and target API 36; AGP **8.10.1**, checksum-pinned Gradle **8.11.1**, Robolectric **4.16.1**, **JDK 21** test runtime. Java/Kotlin app bytecode remains 17. Build-tools 35.0.0 are retained as supported by AGP 8.10.
2. Handle `MonitoringService.onTimeout(startId, fgsType)`: cancel polling, remove the foreground notification, stop the service promptly rather than exceed the system dataSync grace period. Existing periodic WorkManager synchronization remains scheduled; no quota bypass/restart loop.
3. Debug-only Compose host now uses edge-to-edge like MainActivity. New instrumentation checks real system back gestures and system/cutout insets. No production UI redesign or global predictive-back opt-out.
4. Update existing Android CI workflows and environment checks to API 36/JDK 21. New dedicated emulator runner verifies host process, AVD, serial and API before every ADB action.
5. New lint rule `CredManMissingDal` remains a visible **warning** for the previously documented external DAL prerequisite. No fictitious domain affiliation or blanket lint suppression. Optional passkeys are not claimed functional without DAL.

**EXISTS/preserved:** exact Abbott CSV parser, importer repository/ViewModels, explicit patient confirmation, transactional rollback, dedup/conflicts/idempotency, retention protection, SAF file/tree selection, Chart 2.0 and navigation. **INCOMPLETE → addressed:** API 36 tooling/coverage and FGS timeout handling. **MISSING/external:** verified Play upload-certificate registration and optional passkey DAL. Full assessment: [quality review](quality-review-report-2.17.2.md).

Room remains schema **3**; no schema or migration changes, no destructive migration. Importer, chart and schema source diff is empty. Demo Mode, Privacy & Data, API, widgets, statistics, retention/polling/storage/transfer/privacy/account-reset settings and NFZ behavior were not redesigned.

## Actual tests and build evidence

All evidence paths below are relative to the authorized worktree. Only synthetic data was used; no private export was supplied.

| Check | Actual result | Evidence |
|---|---|---|
| Complete JVM suite | **617 tests, 0 failures, 0 errors, 0 skipped**, 84 XML suites | `.delivery-evidence/api36-final-junit/`, `api36-jvm-lint-summary.json` |
| Parser / repository / ViewModels | **19 / 10 / 2 PASS**, included above | Exact Abbott aliases/ketones, private-metadata redaction, preview, confirmation/hash, dedup, rollback, repeat, retention, person isolation |
| DB version/registration / explorer math / new timeout tests | **7 / 6 / 2 PASS**, included above | New timeout cases run with Robolectric API 36 |
| `clean` | **PASS** in initial guarded run | `api36-full-validation-stdout.log` |
| `testDebugUnitTest`, `lint`, `assembleDebug`, `assembleDebugAndroidTest` | **PASS**, final JDK 21 run, exit 0 | `api36-jdk21-validation-{boundary,stdout,stderr,exit}.log` |
| Lint | **0 errors, 275 warnings, 9 hints** | `app/build/reports/lint-results-debug.xml` / `.html`; not warning-free |
| API 36 delivery instrumentation | **9/9 PASS**, 67.658 s | `.delivery-evidence/api36-complete-delivery-instrumentation.txt` |
| Room migration instrumentation | **2/2 PASS**, 0.558 s, migrations 1→2 and 2→3 | `.delivery-evidence/api36-migrations-instrumentation.txt` |
| Home instrumentation | **1/1 PASS**, 3.619 s | `.delivery-evidence/api36-home-instrumentation.txt` |
| `assembleRelease`, `bundleRelease` | **PASS**, exit 0, 4m 10s | `.delivery-evidence/api36-signing-build-*.log` (allowlisted output, no secrets) |
| Workflow syntax | **actionlint PASS** | `.delivery-evidence/api36-actionlint.log`; shellcheck/pyflakes not run |

Device: **`emulator-5582` / `LibreCare_API36_20261019`**, Android **36**, gesture navigation. Verified: real file/folder picker, no-write preview, explicit profile assignment, duplicates/conflicts, repeat-import zero new rows, retained history/provenance; fullscreen pinch/pan, multi-year queries, exact local-time tooltip, range preservation, person isolation, landscape, dark/large-font, both system back gestures, system-bar/cutout clearance.

Earlier failures are preserved, not counted as passes: JDK 17 cannot execute the API 36 Robolectric sandbox; the new DAL lint finding initially failed lint. Three emulator attempts were obstructed by a System UI ANR (and recorded Pixel Launcher input timeout). After recovering only the owned emulator and closing the exact System UI ANR outside the tests, one file-preview timeout remained; the isolated file test passed, followed by the full **9/9** suite above. No test/assertion was removed or weakened. See the quality report for evidence and reproducibility risk.

## Verified artifacts

| Artifact | Absolute path | Exact bytes | SHA-256 |
|---|---|---:|---|
| DEBUG APK | `C:\Users\SG0216827\IdeaProjects\LibreCare-delivery-20261018\release-artifacts\LibreCare-2.17.2-debug.apk` | **24,110,791** | `39069CFD735111FF8B95E53A1808A0CA430CE8BFC44528401A3A16FEB927D69E` |
| RELEASE APK | `C:\Users\SG0216827\IdeaProjects\LibreCare-delivery-20261018\release-artifacts\LibreCare-2.17.2-release.apk` | **3,618,687** | `60293A9282178E3576CAB16F1F7A4939CCA7FDF9105D33FD03E49F2D358E6EED` |
| RELEASE AAB / GOOGLE PLAY UPLOAD FILE | `C:\Users\SG0216827\IdeaProjects\LibreCare-delivery-20261018\release-artifacts\LibreCare-2.17.2-release.aab` | **6,671,711** | `7AF8978061F0CE94196BA8ADC7E13E4BBDE88D4515FEA2A42412269D1586D0D0` |

- **Actual AAB manifest**, read by official bundletool 1.18.3: `com.libredisplay`, versionName **2.17.2**, versionCode **53**, targetSdk **36**, minSdk **26**. `bundletool validate`: PASS.
- JAR signature verified by both Java JarFile payload verification (**526 signed payload entries**) and `jarsigner -verify`. Signer SHA-256: **`14D768C2ECB104A48F239B3E9BC2518D09AD1FD2041C22BEE42E9608C90655DE`**.
- APK manifests/signatures independently checked using aapt/apksigner; release is non-debuggable and signed by the same upload certificate. Debug uses Android Debug certificate `962CB41FD3F7FF5DD9C64D71385F459EEBF7A8A2446E06CCB02E35572958BD07` and version suffix `-debug`.
- Bundle requests **PAGE_ALIGNMENT_16K**; all **8** native libraries have ELF LOAD alignment ≥16 KB. This is static compatibility verification, not a physical 16 KB device run.
- Self-signed/no-timestamp JAR certificate warnings are recorded; they do not negate the cryptographic verification and are not proof of Play registration.
- Verification: `.delivery-evidence/api36-bundle-validation-1792477287.json`, `api36-apk-verification.json`. Named AAB equals the actual Gradle output byte-for-byte.
- Signing used the already verified local upload key through process-scoped environment only. Main-checkout local configuration and keystore remained byte-identical; no secrets were printed or written to reports.
- Build ran before committing the focused changes. `.delivery-evidence/api36-signed-source-snapshot.txt` records the actual app/Gradle source-file hashes; these match the verified working tree. Initial HEAD is not misrepresented as already containing the API 36 changes. Final Git/CI verification is recorded in the PR delivery follow-up/final response.

## Remaining Google Play / product checks

1. **Registered Play upload certificate remains unverified.** Compare the Play Console Upload Key SHA-256 with the signer above. It is a different role from the Play App Signing certificate; do not change Google's app-signing key.
2. Manually upload the verified AAB to Internal Testing only after owner review. Complete any current health/data-safety/foreground-service/store declarations and account requirements. No Play upload or acceptance was performed here.
3. Emulator System UI/timing instability required recovery/retries. Final suite passes, but a wider device/foldable/tablet matrix and six-hour background soak are not claimed. Background quotas and notification permissions still apply.
4. Optional passkeys require verified DAL; private real-CSV integration remains unverified. These are existing limitations, not hidden by the API upgrade.

## Documentation and final compliance checklist

- Release notes (PL/EN): `docs/releases/LibreCare-2.17.2-release-notes.md`.
- Changelog (PL/EN): `CHANGELOG.md`, 2.17.2 at top.
- Quality/architecture review: `docs/quality-review-report-2.17.2.md`.

| Mandatory rule / gate | Status |
|---|---|
| Verify existing implementation; root cause; preserve functionality; bounded scope | PASS |
| Affected architecture/ViewModels/repositories/Room/API/Demo/privacy/statistics/widgets/charts/screens/navigation review | PASS — affected paths reviewed; unrelated behavior unchanged |
| UI readability/scaling/dark/accessibility/insets/phone/landscape and chart interaction review | PASS — scoped automated evidence above; broader manual accessibility audit not claimed |
| NFZ-specific changes and new statistics/settings features | NOT APPLICABLE — unchanged |
| Room version/migration changes | NOT APPLICABLE — no schema change; existing migration tests PASS |
| New/updated tests; complete JVM, repository/ViewModel and relevant device tests | PASS |
| Required clean/test/lint/debug/release/bundle tasks and available-emulator tests | PASS — final results above; earlier failed attempts retained |
| Named APK/AAB, full paths/sizes/hashes, Google Play upload file identified | PASS |
| Version review/increase and owner confirmation of unused code | PASS |
| PL/EN release notes/changelog, release/quality reports, remaining risks | PASS |
| LibreCare branding; legacy package/database/documentation/generated references classified | PASS |
| Protected checkout/no Samsung/no uninstall or data clear/no new PR/merge/publication | PASS |
| `.gitignore` excludes builds, APK/AAB, keystores, local.properties and local evidence | PASS |
| Commit/push existing branch, remote/PR SHA and final Actions | Pending at report creation; verify in final Git/CI follow-up before declaring delivery complete |
| Google Play acceptance/publication | NOT APPLICABLE to local validation — external acceptance remains unverified; publication prohibited |
