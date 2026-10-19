# LibreCare 2.17.0 — delivery and release report

## Status

**Functional debug delivery verified; signed release / Google Play readiness: HOLD.** This is not a claim that all mandatory release gates passed.

- Date: 2026-10-19.
- Previous master: `2.16.2`, versionCode `46`. Reviewed P0 source: `2.16.6`, code `50`. Feature: **2.17.0 / 51**.
- Application ID: `com.libredisplay`; min/compile/target API: `26 / 35 / 35`.
- Worktree: `C:\Users\SG0216827\IdeaProjects\LibreCare-delivery-20261018`.
- Branch: `feature/libreview-chart2-play-20261018`.
- P0 integration commit: `bddc3e50d98cb787e0a19636fbfd0e7f0911ae45`.
- Final APK-input fingerprint inventory: `.delivery-evidence/delivery-validated-source.csv`; initial post-build comparison: **0 source changes**. Afterward only the inherited P0 PowerShell runner was hardened with a per-command Windows process/port/AVD check and rerun; APK inputs remain unchanged. Final feature commit / push / PR / CI verification remained pending at the end of the preserved local run.

## Milestones

| Milestone | Result |
|---|---|
| 1 — P0 review/integration | Exact PR #45 head reverified and scoped recovery/precision changes integrated with provenance. Source PR remains draft/open, no auto-merge. Final emulator P0 regression status below. |
| 2 — CSV parser/importer | Implemented; parser/repository/ViewModel tests and actual Android file/tree picker flows pass. |
| 3 — Chart 2.0 | Implemented; actual pinch, pan, exact point lookup, bounded multi-year rendering, person isolation, initial range, landscape and dark/large-font checks pass. |
| 4 — Tests | 608 JVM tests pass; seven delivery instrumentation tests pass twice; two migration and one Home tests pass. |
| 5 — Debug artifact | Built, signature/package/version checked, installed only on the dedicated emulator. |
| 6 — Signed release/AAB | **BLOCKED**: package tasks fail closed because release signing is not configured; registered Play upload certificate unavailable. |
| 7 — Play materials | Polish listing, release text, 512×512 icon, 1024×500 feature graphic and seven genuine synthetic-data UI captures. No publication. |

## Source and scope review

Independently reverified source PR heads and master base:

- Master: `5433f28b04442d5dfb2af9777801109bee730b35`.
- PR #45: `d58e3bb095b54158955c7098a2593c4949eeeae3` — selected P0 production files, unit/instrumentation tests and emulator script integrated in a provenance-bearing commit. Its old release/version/QA metadata was not copied as this release's evidence.
- PR #38: `58446f99b54b9cb08f95d24bc6ca21b5432a39c1` — reviewed only. Its in-memory complete-history load and additional overlapping drag handlers were not integrated; the new explorer uses database-bounded queries and independent tap/transform gestures.
- PR #43: `1547badb7a1a9f87b290c04925638c6505db46d1` — untouched, not integrated, still HOLD.

Architecture review and EXISTS/INCOMPLETE/MISSING classification: `docs/quality-review-report-2.17.0.md`.

## UI, database and import behavior

- Importer is visible in **Prywatność i dane**. Users select an existing profile or explicitly create a local one; display names never infer external patient IDs.
- Supported format: metadata/header structure, comma CSV, UTF-8/BOM, CRLF, quoted commas/multiline values, Polish/English column names, mg/dL historical type 0 and scan type 1. Type 6/other events and malformed numeric/date values are reported as skipped.
- Preview reports all row classifications, date ranges, chosen zone/DST treatment and projected inserts. No write occurs before exact profile-name confirmation.
- Imported records are inserted in atomic batches; conflicting existing values are preserved. SHA-256/record/type/local-time/zone provenance is retained without the original file name or metadata patient name.
- Synthetic end-to-end import: **6 rows**, **3 accepted unique readings**, **1 event skipped**, **1 duplicate**, **1 conflict**. First import inserts **3**, second inserts **0**; all **3 survive routine retention**.
- Room version **2→3**, additive nullable provenance column, registered migration and exported schema. Backup encode/decode/restore mapping retains the field; destructive fallback removed. No unrelated backup-security rewrite.
- CSV profiles are accessible in analysis, and a custom period queries its actual date range rather than the 400-day buffer.
- The chart renders at most **801** database buckets, with visible min/max information, target shading/grid, gap separation, scan-interval markers and exact actual-row tooltip lookup. Labels use local timezone. Horizontal pan/zoom and selection remain person-scoped.

## Build and test evidence

All Gradle invocations used `scripts/Invoke-DeliveryGradle.ps1`, explicit canonical worktree/Git/branch/HEAD checks, rejection of reparse points, explicit child process working directory and absolute `-p` path. No sprint Gradle invocation used the protected checkout as its project or process working directory.

| Check | Result / evidence under worktree |
|---|---|
| `clean` | Preserved evidence unavailable after recovery; no surviving `.delivery-evidence/clean-*.log` was found. |
| `testDebugUnitTest` | **608 tests, 0 failures, 0 errors, 0 skipped** — `app/build/test-results/testDebugUnitTest/`, `app/build/reports/tests/testDebugUnitTest/index.html` |
| `lint` | PASS — `app/build/reports/lint-results-debug.html`; pre-existing warnings are not misreported as zero warnings. |
| `assembleDebug` | PASS — verified artifact below. |
| `assembleDebugAndroidTest` | PASS — test APK below. |
| `assembleRelease` / `bundleRelease` | **FAIL at package/signing gates**; release Kotlin/Java compilation, R8 and resource shrinking complete. No unsigned/debug-signed workaround. |
| Final combined task run | `.delivery-evidence/delivery-validated-source-{boundary,stdout,stderr,exit}.log`; exit **1**, solely the two explicit signing-configuration failures. |
| Delivery instrumentation | **7/7 PASS twice** — `.delivery-evidence/delivery-final-instrumentation.txt`, `delivery-repeat-instrumentation.txt` |
| Room migration instrumentation | **2/2 PASS** — `.delivery-evidence/room-migrations-instrumentation.txt` |
| Existing Home current-glucose regression | **1/1 PASS** — `.delivery-evidence/home-regression-instrumentation.txt` |
| P0 MainActivity / process-death regression | **7/7 PASS** — `build/p0-evidence/delivery-regression/`; a second run with the strengthened process guard is recorded in `build/p0-evidence/delivery-guarded/`. |
| Dependency review | univocity-parsers 2.9.1 CVE validation reported no known vulnerabilities. |
| Source integrity | `.delivery-evidence/final-source-comparison.txt`: `VALIDATED_SOURCE_CHANGES=0` |

Emulator: `emulator-5580`, AVD `LibreCare_P0_20261018`, verified qemu/model/AVD identity before operations. No Samsung operation or device auto-selection. The display was temporarily set to 1080×1920 for genuine, uncropped Play captures and restored afterward. Dark/1.4× font configuration is exercised on production Compose screens by the debug-only test host, not by fabricated image artwork.

## Actual artifacts

| Artifact | Full path | Size |
|---|---|---:|
| DEBUG APK (named delivery copy) | `C:\Users\SG0216827\IdeaProjects\LibreCare-delivery-20261018\release-artifacts\LibreCare-2.17.0-debug.apk` | **25,003,576 bytes** |
| DEBUG APK (Gradle output) | `C:\Users\SG0216827\IdeaProjects\LibreCare-delivery-20261018\app\build\outputs\apk\debug\app-debug.apk` | **25,003,576 bytes** |
| Instrumentation APK | `C:\Users\SG0216827\IdeaProjects\LibreCare-delivery-20261018\app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk` | **2,064,314 bytes** |
| RELEASE APK | **NOT PRODUCED** — signing gate | N/A |
| RELEASE AAB | **NOT PRODUCED** — signing gate | N/A |
| GOOGLE PLAY UPLOAD FILE | **NONE — no verified release AAB** | N/A |

Debug APK SHA-256: `D4BFF5EC36BB4572B1930267143DC7D87108F9E1DFD8916D4B9C81D3CBBF85AE`.

Test APK SHA-256: `9784BBC467742C5D9A4695C758D58DBDA298A015668E5BBB2E10D4F8924622FA`.

Debug APK v2 signature verifies. Debug signer certificate SHA-256: `962CB41FD3F7FF5DD9C64D71385F459EEBF7A8A2446E06CCB02E35572958BD07`. This is **not** a Play upload certificate or a claim of compatibility with the physical Samsung installation. APK/AAB inventory was performed recursively over this worktree; no release APK/AAB was found.

## Play assets and signing

- Asset folder: `C:\Users\SG0216827\IdeaProjects\LibreCare-delivery-20261018\docs\google-play\assets`.
- `manifest.json` records filename, dimensions, bytes and SHA-256 for all nine PNGs. Seven screenshots are direct captures from the passing `delivery-final` run; only synthetic DEMO profiles/values were used. Six are 1080×1920; landscape is 1920×1080. Branding graphics are explicitly distinguished from screenshots.
- Listing: `docs/google-play/store-listing-pl.md`; tester text: `internal-testing-release-notes-pl.txt`; upload procedure/privacy/permissions checklist: `internal-testing-upload.md`.
- Existing upload keystore public certificate verified with successful integrity check: `14D768C2ECB104A48F239B3E9BC2518D09AD1FD2041C22BEE42E9608C90655DE`.
- Owner-supplied **app-signing** fingerprint: `70AB15D3E610088BD33908D9793CA005CCA69CFE755C10AF42FC15305C0AE0A0`. These certificates have different roles and are not treated as interchangeable.
- The owner reported no earlier Play uploads and no available upload-certificate metadata. No signing-key creation/rotation, credential printing, Play publication or physical-phone installation occurred.

## Documentation / remaining risks

Release notes: `docs/releases/LibreCare-2.17.0-release-notes.md` (PL/EN). Changelog: `CHANGELOG.md` (new top PL/EN entry). Mandatory PASS/FAIL/NOT APPLICABLE checklist and full remaining-risk list: `docs/quality-review-report-2.17.0.md`.

Primary remaining gates: registered upload-certificate identity, signed release artifacts, current Play target-API acceptance and owner Console/privacy declarations. Real-file compatibility, multi-zone travel interpretation, broader legacy background/backup policy and exhaustive physical-device/accessibility testing remain explicitly limited. Release HOLD is intentional; no merge or auto-merge is requested.


