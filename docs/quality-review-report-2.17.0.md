# LibreCare 2.17.0 — quality review

## Scope and pre-implementation review

| Area | Initial state | Review / resulting scope |
|---|---|---|
| Architecture / repositories | EXISTS | Existing Application wiring, Room and repositories retained; dedicated CSV parser/repository and import ViewModel added. |
| Monitoring ViewModel / API / authentication | EXISTS, INCOMPLETE | Reviewed exact PR #45 error wrapping, session handling and regression tests; integrated its P0 source with recorded provenance, not a merge. |
| Room / migrations | EXISTS | Unique patient/timestamp index reused; additive 2→3 migration adds nullable provenance. No destructive fallback. |
| LibreView import | MISSING | Streaming parser, SAF file/tree selection, explicit profile/name confirmation, no-write preview and atomic insert implemented. |
| Retention | INCOMPLETE for imported history | Automatic age cleanup excludes LibreViewCSV and LibreViewScan; explicit privacy deletion remains available. |
| Backup | EXISTS | Only optional provenance DTO/codec/entity mapping changed. PR #43 is not integrated. |
| Demo Mode | EXISTS | Live and DEMO profiles are separated by existing ID conventions; synthetic test data only. |
| Privacy & Data / Compose / navigation | EXISTS; import entry MISSING | Visible importer and offline history entry points added without removing reset, deletion, statistics or privacy controls. |
| Statistics / analysis | EXISTS, INCOMPLETE | Local inactive profiles exposed; custom-period data loaded from the actual range instead of the 400-day buffer. |
| Charts | EXISTS, INCOMPLETE | New bounded database viewport, gestures and exact point query; PR #38 inspected but not copied wholesale. |
| Widgets / alerts / medical calculations / NFZ | EXISTS | No feature changes to these areas. No insulin dosing, treatment advice or NFZ eligibility claim introduced. |

## Safety and data contracts

- The CSV parser reads one logical record at a time using univocity-parsers 2.9.1. Quoted commas/multiline fields, BOM/CRLF, UTF-8 validity, required header uniqueness, units, type 0/1 versus type 6, invalid values/dates and DST are tested. No private export was inspected or committed.
- Source fields are stored per accepted reading: original file SHA-256, logical record number, type, device-local timestamp, chosen timezone/offset and ambiguity policy. Patient names are never used to infer LibreLinkUp IDs.
- Preview reads a private snapshot and performs no database write. Import rechecks the snapshot hash, requires the exact confirmed profile name, compares actual counts with preview inside a Room transaction, and uses batches of 400 `INSERT IGNORE` rows. Cancellation or count changes roll back the whole transaction.
- Existing values win; within a file the first valid row at a timestamp wins. Later equal values count as duplicates, different values as conflicts. This policy is visible before confirmation.
- Normal sync already uses `insertIgnore`; imported readings are not overwritten by routine live sync. Import source protection applies to ordinary retention, not explicit account/history reset.
- Data analysis uses existing metric implementations. The change fixes dataset selection rather than medical thresholds or formulas.
- Chart buckets are computed in SQLite and bounded to 801 results. Min–max bars represent intervals; scan diamonds indicate intervals containing scans. Selected values/timestamps are fetched from actual rows, not interpolated points.
- Time coordinates subtract the domain origin before floating-point conversion. Invalid gesture values are rejected. Lines are not drawn across gaps over 20 minutes; multi-row bins do not invent time-series connections.
- Application target remains API 35, package `com.libredisplay`. New production storage permissions: none. The synthetic DocumentsProvider exists only in the test APK; the isolated Compose host exists only in debug.

## Test review and interpretation

See the release report for final counts, artifact hashes and emulator evidence. Coverage includes parser scale (49,767 synthetic rows), repository rollback/idempotency/conflicts, retention, backup provenance, imported-profile/custom-range ViewModels, viewport precision, actual Android document/tree pickers and gesture/rendering tests. Existing P0 and migration regressions are rerun separately.

Earlier failures were not hidden: parser Java setter mismatch, a mixed-line-ending synthetic fixture, invalid JUnit setup return type, test-provider Kotlin-runtime availability, test-host recreation, animated picker node staleness and virtualized test scrolling were corrected. The full core five-test emulator run passed before the final expanded rendering checks. These harness failures are not counted as product RED proof or passed tests.

## Remaining risks / release gates

1. Registered Play upload certificate is unavailable. The local public upload certificate was verified, but registration is unproven. No signed release APK/AAB or Play-ready file is claimed.
2. Play Console target-API acceptance, privacy-policy URL, support contact and truthful Data safety/Health apps declarations require owner confirmation. The public policy retrieval did not provide usable current target-API evidence.
3. The real private CSV was deliberately not used. Unsupported localized header variants fail closed; supported synthetic fixtures match the stated two-header, comma-separated schema.
4. Folder discovery is nonrecursive and capped at 2000 documents. Import snapshots are limited to 128 MB. One timezone applies per file; ambiguous/nonexistent local times cannot be inferred reliably from an export without offsets.
5. Existing backup checksum/restore-transaction/security limitations from master remain; PR #43 is still HOLD. Older LibreCare releases do not provide this version's imported-history retention guarantee.
6. Foreground P0 fixes do not establish global single-flight/error policy across all background service/sync components. Legacy analysis operations outside the changed dataset-loading path were not broadly refactored.
7. No physical-device installation, historical-data recovery, live Abbott account test, TalkBack manual audit or Play publication occurred. Emulator rendering/semantics checks are not a claim of exhaustive accessibility or device compatibility certification.
8. A prior, separately disclosed P0-session boundary incident ran Gradle in the protected checkout; ignored/generated files there may have changed at that time. This sprint uses explicit, fail-closed build boundaries and does not claim that the earlier incident was undone.

## Mandatory compliance checklist

| Rule / obligation | Status | Evidence / limitation |
|---|---|---|
| Verify implementation and affected architecture before changes | PASS | Exact source and PR diffs reviewed; states above. |
| Fix causes, preserve existing functions | PASS | Typed P0 recovery reused; transactional import and bounded chart queries; existing privacy controls retained. |
| Add/update tests; review unit, ViewModel and repository tests | PASS | New parser/repository/ViewModel/viewport and existing suites. |
| Room version increment, migration registration and migration tests | PASS | Version 3; MIGRATION_2_3; exported schema and instrumentation regression. |
| No destructive release migration | PASS | Destructive fallback removed from all build types. |
| Demo/live isolation and synthetic-only test inputs | PASS | Separate IDs/mode checks; no real CSV or credentials in fixtures/assets. |
| CSV preview, explicit identity/timezone confirmation and no overwrite | PASS | Repository and real picker/import regression coverage. |
| Idempotency, cancellation rollback and retention protection | PASS | Transaction tests and repeat-import/retention emulator assertions. |
| Current glucose prominence, compact dashboard, one monitored-person identity | PASS | Dashboard structure retained; only full-history destination changed. |
| Polish new user-facing strings and consistent visual style | PASS | Material/LibreCare theme and Polish screen text. |
| UI scaling, dark theme, phone and landscape review | PASS | All seven expanded UI checks pass twice, including dark theme, 1.4× font, phone and landscape. |
| Chart point selection, timezone, min/max, target shading and grid review | PASS | Real point query; local-time labels; visible range summary/shading/grid. |
| Chart tap navigation and range/person preservation review | PASS | Dashboard/Privacy route review plus viewport/person and initial-range tests. |
| NFZ explanation/status/period rules | NOT APPLICABLE | NFZ not modified. |
| Preserve retention/polling/statistics/storage/transfer/privacy/reset settings | PASS | Existing settings paths retained; importer changes only normal retention exclusion. |
| Statistics counts/growth/usage estimates: no invented values | PASS | Existing diagnostics unchanged; imported rows use ordinary Room data paths. |
| Required clean, JVM, lint, debug and release task attempts | FAIL | Preserved recovery evidence covers JVM, lint, debug and release-task attempts, but no surviving guarded `clean` log was found. |
| Connected testing when a dedicated emulator exists | PASS | Explicit emulator-only instrumentation; no device auto-selection. |
| Successfully signed release APK and release AAB | FAIL | Upload-certificate gate and unconfigured worktree signing. |
| Google Play upload file genuinely identified and verified | FAIL | No acceptable signed release AAB exists. |
| Exact artifact locations/sizes/hashes; LibreCare-named debug artifact | PASS | Named debug APK: 25,003,576 bytes; recursive inventory and verified SHA-256/signature in release report. |
| Appropriate version increase; no versionCode decrease | PASS | Master 46 and P0 50 → feature 51; owner reports no earlier Play upload. |
| PL/EN release notes and changelog updated | PASS | Versioned release notes and top changelog entry. |
| Release and quality reports, remaining risks | PASS | This report and release report. |
| Branding classification | PASS | LibreCare in user-facing additions; package/applicationId/database legacy names retained; historical documentation/generated names not blindly renamed. |
| Git ignore: build, APK, AAB, keys, local.properties | PASS | Ignore rules verified; binaries/credentials excluded from source commit. |
| One new branch/PR, source/tests/docs committed and pushed, CI checked | FAIL | Final feature commit, push, PR creation and CI verification pending. |
| No merge, no auto-merge, no PR #43 integration, no Samsung operations | PASS | Source PRs unchanged; only guarded emulator serial used. |
| Report complete delivery only when mandatory gates pass | PASS | Overall release status remains HOLD; no Play-ready release claim. |

The FAIL rows are genuine release blockers. This checklist does not declare the whole sprint fully delivered.


