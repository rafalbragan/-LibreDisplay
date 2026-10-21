# LibreCare 2.17.1 — PR #46 focused delivery report

Date: 2026-10-19. **Signed release / Google Play readiness: HOLD.**

- Previous version: **2.17.0 / 51**; new version: **2.17.1 / 52**.
- Application ID: `com.libredisplay`; min/target/compile SDK: 26/35/35.
- Authoritative worktree: `C:\Users\SG0216827\IdeaProjects\LibreCare-delivery-20261018`.
- Branch: `feature/libreview-chart2-play-20261018`; existing PR: https://github.com/rafalbragan/-LibreDisplay/pull/46.
- Initial local and GitHub HEAD independently verified: `28d74421979d8f2ecf2185ae4d0c1aaea8bb5cd9`; clean worktree, PR open, no auto-merge, initial applicable checks successful (optional release job is not signing proof).
- No new PR, merge, force push, physical Samsung operation, application uninstall or Play publication.

## Confirmed root cause and fix

The parser lacked four exact Polish Abbott aliases and rejected every header containing `mmol`, including unrelated ketone columns. The exact required columns now work:

- `Urządzenie`
- `Numer seryjny`
- `Znacznik czasu w urządzeniu`
- `Rodzaj zapisu`
- `Historyczne stężenia glukozy mg/dL`
- `Stężenie glukozy na podstawie skanowania mg/dL`

Existing Polish/English aliases are preserved. Both selected glucose columns must match a supported name with **mg/dL** units; ketone units are irrelevant. Unsupported/missing glucose units fail before any measurement is emitted. There is no conversion, rounding, fallback to another column or fabricated glucose. Invalid values are counted in `invalid` and `skipped`; type 6 remains excluded even when its glucose fields are populated.

Streaming, two-row metadata/header layout, BOM/UTF-8, quoted commas/escaped quotes/multiline fields, timestamps, DST reporting and coroutine cancellation are unchanged. `CsvSummary.toString()` now redacts private metadata; parser error sanitization remains intact. Repository provenance excludes patient metadata. Only synthetic fixtures were used. **No authorized private export path was supplied: private-file integration remains unverified.**

## Architecture / scope

- Parser: **INCOMPLETE → fixed** for the confirmed format. Synthetic picker provider updated to the exact layout with ketones and refreshes its cache to prevent a false positive using old headers.
- Import repository, ViewModels, Room 3 and registered migrations, retention protection, Demo/live separation, Privacy & Data, Compose/navigation and Chart 2.0: **EXISTS**, retained. Unit/repository/ViewModel tests reviewed; no transaction, patient-assignment or chart rewrite.
- UI and database schema changes: **none**. API, widgets, statistics, NFZ, polling/storage/transfer/privacy/reset settings: unchanged; no broad new audit or feature claims.
- Release signing configuration: **MISSING** in the worktree/environment; registered upload-certificate and Play version inventory: **INCOMPLETE**.
- Detailed compliance and risks: [quality report](quality-review-report-2.17.1.md).

## RED/GREEN and build evidence

Evidence paths below are relative to the authoritative worktree. All Gradle runs used the existing `scripts/Invoke-DeliveryGradle.ps1` with explicit expected HEAD, canonical worktree/Git/branch/reparse-point checks and an absolute wrapper path plus explicit child working directory. No Gradle build ran in the protected main checkout.

| Check | Result / evidence |
|---|---|
| Focused RED on unfixed parser | **19 tests, 6 expected failures**; `.delivery-evidence/abbott-header-red.xml`, `abbott-header-red-{boundary,stdout,stderr}.log` |
| Focused GREEN | **19 tests, 0 failures/errors**; `.delivery-evidence/abbott-header-green.xml`, `abbott-header-green-{boundary,stdout,stderr,exit}.log` |
| Complete JVM suite | **615 tests, 0 failures, 0 errors, 0 skipped**; `app/build/test-results/testDebugUnitTest/`, `.delivery-evidence/abbott-jvm-summary.json` |
| Repository | **10/10** in complete suite: no-write preview, confirmed name/hash, duplicates/conflicts, isolation, idempotency, cancellation after batch rollback, retention, bounded chart queries/provenance |
| `clean`, `lint`, `assembleDebug`, `assembleDebugAndroidTest` | **PASS**; `.delivery-evidence/abbott-full-delivery-stdout.log` |
| Lint findings | **0 errors**, 307 warnings, 9 informational findings; `app/build/reports/lint-results-debug.html` and `.xml`; not claimed warning-free |
| `assembleRelease`, `bundleRelease` | **FAIL at package/signing gates only**: four release-signing properties missing. Release compile/R8/resource shrinking completed. Combined run exit 1; `abbott-full-delivery-stderr.log` |
| Dedicated emulator delivery | **7/7 PASS on full repeat**, 43.095 s; `.delivery-evidence/abbott-delivery-repeat-instrumentation.txt`. First run: 6/7, one system-folder-picker timeout, preserved in `abbott-delivery-instrumentation.txt` |
| Migration instrumentation | **2/2 PASS**, 0.295 s; `.delivery-evidence/abbott-migrations-instrumentation.txt` |

Seven new parser regressions cover the exact layout with unrelated ketones; legacy/English mixed units; independent rejection of unsupported units for each glucose column; invalid/boundary values without cross-column fallback; DST/cancellation; and metadata redaction/error sanitization. Scale test retains 49,767 synthetic records without retaining measurements in the parser.

The initial emulator attempt correctly failed the host-process guard before installation because `-no-window` starts `qemu-system-x86_64-headless.exe`. The same owned AVD was gracefully restarted without that flag, keeping the existing guard unchanged. No AVD data wipe or app uninstall was used.

Verified on `emulator-5580` / `LibreCare_P0_20261018`: real file/folder pickers using exact headers and ketones, preview without writes, confirmed assignment, duplicates/conflicts, three inserted readings, zero inserted on repeat, provenance and retention protection; actual pinch/pan, over-five-year ranges bounded to 801 buckets, exact local-time tooltip, initial range, person isolation, landscape and dark/large-font rendering. No chart source or gesture tests were changed. Synthetic screenshots from the passing run remain local in `.delivery-evidence/abbott-delivery-repeat/`.

## Artifacts

| Artifact | Full path | Size |
|---|---|---:|
| DEBUG APK | `C:\Users\SG0216827\IdeaProjects\LibreCare-delivery-20261018\release-artifacts\LibreCare-2.17.1-debug.apk` | **24,143,556 bytes** |
| Gradle DEBUG APK | `C:\Users\SG0216827\IdeaProjects\LibreCare-delivery-20261018\app\build\outputs\apk\debug\app-debug.apk` | **24,143,556 bytes** |
| Instrumentation APK | `C:\Users\SG0216827\IdeaProjects\LibreCare-delivery-20261018\app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk` | **1,216,359 bytes** |
| RELEASE APK | **Not produced: signing blocked** | N/A |
| RELEASE AAB | **Not produced: signing blocked** | N/A |
| GOOGLE PLAY UPLOAD FILE | **None: no verified signed release AAB** | N/A |

Debug APK SHA-256: `4D18CBE74B035D75C783C72A752071781595F29CB65A7A082E7A91EBD615141F`.

Recursive APK/AAB inventory found no release APK or AAB; filtered build-output inventory: `.delivery-evidence/abbott-artifact-inventory.json`.

`apksigner verify --verbose --print-certs` passes APK v2 signature verification. Debug signer certificate SHA-256: `962CB41FD3F7FF5DD9C64D71385F459EEBF7A8A2446E06CCB02E35572958BD07`. `aapt dump badging` confirms label **LibreCare**, package **com.libredisplay**, version **2.17.1-debug / 52**, debuggable. **This APK is not a Play upload artifact.** Evidence: `.delivery-evidence/abbott-debug-signature.txt`. Old 2.17.0 artifacts, if present, are not this release's outputs.

## Signing blocker and remaining owner action

1. In Play Console for **com.libredisplay**, obtain the **Upload Key certificate SHA-256** from App integrity/App signing (not the App Signing certificate). Compare it with the owner-supplied local upload fingerprint `14D768C2ECB104A48F239B3E9BC2518D09AD1FD2041C22BEE42E9608C90655DE`. If it differs, use the matching registered upload key or complete Google's upload-key reset procedure; do not replace the app-signing key.
2. The owner-supplied Play **App Signing** fingerprint is `70AB15D3E610088BD33908D9793CA005CCA69CFE755C10AF42FC15305C0AE0A0`. Its difference from the upload key is normal and does not prove or disprove upload compatibility. A password-free metadata read identified the local store as PKCS12 but could not re-export its protected public certificate. No secret/private key was printed, copied or uploaded.
3. Configure `RELEASE_STORE_FILE`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD` securely in the authorized build environment. All four are currently absent. Do not send passwords or keystores in chat/PRs.
4. Check **App bundle explorer and all tracks**, including Internal Testing, for the highest uploaded versionCode. **52 is greater than local 51, but Play's inventory has not been verified.** If necessary choose a higher unused code, rebuild/test, then generate and verify the signed release AAB with the guarded absolute-worktree launcher.
5. Verify bundle package/version, release status and upload signer; only then manually upload the verified release AAB to **Internal Testing**, completing current target-API, health/privacy and Console declarations. No upload/publication was performed here.

## Documentation / Git evidence

Release notes: `docs/releases/LibreCare-2.17.1-release-notes.md` (PL/EN). Changelog: `CHANGELOG.md` (2.17.1 at top). Quality/compliance: `docs/quality-review-report-2.17.1.md`.

Final commit, remote SHA and applicable CI conclusions are recorded in the PR #46 delivery follow-up and final delivery response; initial status above is not substituted for final CI. Generated binaries, raw local logs, credentials and private exports are excluded from the source commit.
