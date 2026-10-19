# LibreCare 2.17.1 — focused quality review and compliance

**Scope:** PR #46 exact Abbott CSV compatibility fix, not a new importer/chart implementation or broad audit. **Overall signed-release status: HOLD.** A mandatory FAIL below is not reported as complete delivery.

## Affected-functionality review

| Area | Before change | Disposition |
|---|---|---|
| Parser / exact Polish Abbott aliases / mixed ketone units | INCOMPLETE | Root causes reproduced RED and fixed GREEN; existing aliases retained |
| Metadata-safe diagnostics | INCOMPLETE | Default summary string exposed metadata; now redacted, parser errors sanitized |
| Repository / confirmed patient assignment / preview / rollback / duplicates | EXISTS | Preserved; all 10 repository tests pass |
| Import/analysis ViewModels, Demo Mode, Privacy & Data, Compose/navigation | EXISTS | Existing state and error handling reviewed; complete JVM suite rerun |
| Room 3 / migrations / imported-history retention | EXISTS | No schema/migration change; existing migration and retention regressions rerun |
| Chart 2.0 / bounded historical queries / tooltip / gestures | EXISTS | Kept unchanged; existing instrumentation retained |
| API / widgets / statistics / polling / storage / transfer / reset / NFZ | EXISTS (prior review) | Unchanged; not claimed newly audited or enhanced |
| Signing properties | MISSING | All four worktree/environment release properties absent |
| Registered upload certificate / Play version inventory | INCOMPLETE | Requires Play Console owner verification; no fabricated compatibility or version claim |

## Evidence and interpretation

- Parser RED: 19 tests, six expected failures against the unchanged parser; GREEN: 19/19 against the fix. Full JVM: **615/615**, no errors/skips. Exact test evidence and artifact metadata: [release report](release-report-2.17.1.md).
- Synthetic fixture rows only; exact six user-supplied headers plus a synthetic unrelated mmol/L ketone column and notes. No private CSV was located, copied, logged, added to test reports, committed or uploaded. Private-file integration remains unverified.
- Invalid/missing/unsupported glucose data fails or is counted explicitly; no conversion, invented values, ketone fallback or type-6 measurements. Old timestamp, quoting, UTF-8, DST and cancellation contracts retained.
- Summary metadata remains available to the private preview, but not automatic `toString()` diagnostics. Sanitized parser exceptions have no private-row cause attached. Import provenance does not include patient names or source filenames.
- UI and schema unchanged. Existing chart source/tests remain intact. No new permissions, dependencies, external APIs, database version changes, destructive migration or NFZ claims.
- Lint passes with **307 warnings and 9 informational findings**, not a warning-free certification. IDE Gradle script inspection reports unresolved Gradle DSL/classpath symbols in this external, unsynced worktree; actual guarded Gradle debug/release compilation and lint succeed. No unrelated DSL rewrite.
- Initial headless AVD guard stop occurred before installation; restarted the same owned AVD in the guard's expected windowed mode without changing guards or wiping data. The first seven-test delivery run had one system-folder-picker timeout; all chart tests and actual document-import/idempotency checks passed. Follow-up run evidence is recorded in the release report, not silently substituted for the initial failure.

## Mandatory compliance checklist

| Rule / obligation | Status | Evidence / limitation |
|---|---|---|
| Verify existing implementation and affected architecture before changes | PASS | Scoped parser/repository/ViewModel/test and prior architecture review; EXISTS/INCOMPLETE/MISSING above |
| Fix root causes; preserve functionality | PASS | Exact aliases, glucose-only unit validation; importer, chart and medical logic retained |
| Add/update and review unit, repository, ViewModel tests | PASS | Seven new parser tests, exact-layout picker fixture; full 615-test JVM suite |
| Two-row structure, streaming, quoted CSV, timestamp/DST/cancellation | PASS | Existing tests plus exact-format DST/cancellation and quoted/multiline regression |
| Type 0/1 interpretation; type 6 excluded; no fabricated glucose | PASS | Exact-format event/invalid/boundary tests; explicit counts and unit rejection |
| Confirmed patient assignment, no-write preview, duplicates/conflicts, rollback/idempotency | PASS | Repository tests and actual emulator import regression |
| Patient privacy in logs, reports, GitHub artifacts | PASS | Synthetic-only tests; redacted diagnostics; raw private export never supplied or consumed |
| Authorized real-private-file integration | NOT APPLICABLE | No authorized file path supplied; remains explicitly unverified |
| Current glucose prominence, one monitored person, compact cards/padding, typography | NOT APPLICABLE | No dashboard/UI redesign |
| Polish user-facing strings, readable chart and touch interactions | PASS | Existing Polish UI retained; no new English user-facing strings |
| Scaling, dark theme, accessibility/readability, phone and landscape checks | PASS | Existing delivery rendering/semantics coverage retained; no claim of manual TalkBack certification |
| Chart full-screen navigation, tooltip placement/timezone, extrema/shading/grid, person/range preservation | PASS | Existing Chart 2.0 unchanged; gesture/exact-point/range/person/landscape regressions; prior visual contracts retained |
| NFZ status/reason/recommendation/period/info dialog and qualified wording | NOT APPLICABLE | NFZ not changed; no final eligibility claim |
| Room version increase, new migration/registration/schema for schema changes | NOT APPLICABLE | No schema change in 2.17.1 |
| Existing migrations and no destructive release migration | PASS | Existing 1→2/2→3 migration tests retained/rerun; production migration configuration unchanged |
| Preserve retention, polling, statistics, storage/transfer/privacy/reset controls | PASS | No settings removal; retention tests and importer safeguards retained |
| Statistics sizes/growth/counts/transfer/sync/usage/estimates and insufficient-data wording | NOT APPLICABLE | Statistics untouched; no new estimates or fabricated values; no broad audit claimed |
| `clean`, full JVM, lint, debug APK and AndroidTest build | PASS | Guarded absolute-worktree full run |
| Attempt `assembleRelease` and `bundleRelease` | PASS | Both attempted, fail closed at explicit signing configuration gates |
| Successfully signed release APK/AAB | FAIL | Four signing properties absent; registered Upload Key compatibility unverified |
| Connected tests on dedicated emulator; never physical Samsung | PASS | `emulator-5580`, `LibreCare_P0_20261018`; per-operation host/qemu/AVD/model guards; no device auto-selection |
| Artifact inventory, exact full paths/sizes/hashes and LibreCare naming | PASS | Named debug APK and output inventory in release report; generated outputs ignored |
| Verified Google Play upload file | FAIL | No verified signed release AAB; no unsigned/debug relabeling |
| VersionName/versionCode review and monotonic increase | PASS | 2.17.0/51 → 2.17.1/52; `com.libredisplay` retained |
| Verify versionCode against all Play uploads | FAIL | No authenticated Play inventory; owner must check highest uploaded code before building upload artifact |
| PL/EN release notes with version/fixes/features/improvements/tests/artifacts/limits | PASS | `docs/releases/LibreCare-2.17.1-release-notes.md` |
| Changelog top version with PL/EN Added/Changed/Fixed/Tests/Artifacts | PASS | `CHANGELOG.md` |
| Release/quality reports with summary, architecture, UI/DB, tests, artifacts, risks | PASS | This report and `docs/release-report-2.17.1.md` |
| Branding LibreDisplay/LIBREDISPLAY/libredisplay classification | PASS | User-facing label/resources: LibreCare. Package/applicationId and manifest `.LibreDisplayApp`: compatibility identifiers. DB/migration legacy names retained. Historical docs/generated output not blindly renamed |
| Git ignore build/APK/AAB/JKS/keystore/local.properties | PASS | `git check-ignore` verified each; `.delivery-evidence/` and `release-artifacts/` ignored |
| Commit/push focused source/tests/docs/reports; exact diff, final SHA/Actions | PASS | Final verification is recorded externally in PR #46 delivery follow-up and final response; not a self-referential SHA inside this commit |
| Same PR/branch; no force, merge, auto-merge, uninstall, physical Samsung or Play publication | PASS | Existing #46 only; guarded emulator in-place install; no publishing/signing workaround |
| Do not claim completion while mandatory release gates FAIL | PASS | Signed release and Play readiness remain HOLD; explicit owner action in release report |

## Remaining risks

1. Signing configuration, registered Play Upload Key and Play version inventory are unresolved; see exact owner actions in the release report. App Signing and Upload Key certificates are different roles.
2. No private-file/live Abbott-account integration test. One zone per export and explicit DST handling cannot infer travel offsets; unsupported localized variants fail closed.
3. System DocumentsUI has shown an intermittent folder-root timeout. First-run failure is retained; repeat evidence is reported separately.
4. Physical-device/TalkBack coverage and current Play target-API/health/privacy declarations require owner validation. No physical Samsung was modified.
5. Existing lint warnings and unrelated legacy background/backup limitations are not addressed by this focused parser patch.
