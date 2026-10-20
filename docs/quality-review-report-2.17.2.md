# LibreCare 2.17.2 / 53 — Android 16 compatibility review

Date: 2026-10-19. Scope: existing PR #46, API 36 compatibility only.

## Pre-implementation review

| Area | Initial state | Decision / evidence |
|---|---|---|
| compileSdk/targetSdk | MISSING API 36 (35/35) | Set both to 36; minSdk remains 26; applicationId remains `com.libredisplay`. |
| Toolchain | INCOMPLETE | AGP 8.5.2 → 8.10.1; Gradle 8.9 → 8.11.1, official distribution checksum pinned. Build-tools 35.0.0 retained as supported by AGP 8.10. |
| JVM Android 16 runtime | MISSING | Robolectric 4.14.1 → 4.16.1, JDK 21 for its API 36 sandbox. Android bytecode remains Java/Kotlin 17. |
| MainActivity / edge-to-edge | EXISTS | `enableEdgeToEdge()`, Material3 Scaffold/TopAppBar insets; no legacy edge-to-edge opt-out. Debug test host now enables the same behavior. |
| Back navigation | EXISTS | Compose `BackHandler` through AndroidX dispatcher, not legacy `onBackPressed()`/KEYCODE_BACK overrides. Added actual edge-swipe tests for importer and fullscreen history. |
| Fullscreen history | EXISTS | Existing pinch/pan, multi-year bounded queries, exact-point tooltip, local timezone, target shading, min/max, gap guards, ranges and person isolation retained. Production chart source unchanged. |
| File/folder import | EXISTS | SAF OpenDocument/OpenDocumentTree; exact Polish Abbott aliases and unrelated ketone columns retained. No broad storage permission or guessed file paths. |
| Background work | INCOMPLETE | Periodic constrained WorkManager sync/retry already exists. Missing dataSync FGS timeout callback added: cancel polling, remove foreground notification, stop service promptly. |
| Notifications | EXISTS / runtime behavior limited | Declared POST_NOTIFICATIONS, notification channel, immutable activity PendingIntent. No new notification permission exemption is assumed; user/OS restrictions still apply. |
| Room / migrations / repositories / ViewModels | EXISTS | Room schema remains 3; migration registry and importer transaction, retention, confirmation, rollback, dedup and idempotency preserved. No destructive migration. |
| Demo / Privacy & Data / statistics / widgets / API | EXISTS, unchanged | Only synthetic test data. No unrelated UI/settings/statistics/NFZ/API redesign. Existing retention/polling/privacy/account controls retained. |
| Signing | EXISTS | Previously verified local upload keystore; configuration read in memory, only child-process environment receives secrets. No keystore or password copied into Git/worktree configuration. |
| Play version inventory | Owner-confirmed | Owner explicitly confirmed code 53 unused across uploaded bundles, tracks and drafts. Not represented as direct Play API verification. |

## Android 16 behavior decisions

- **Mandatory edge-to-edge:** retain existing inset-aware Compose layouts; test toolbar, chart and bottom navigation controls against actual system/cutout insets. No opt-out or padding redesign.
- **Predictive back:** exercise the system edge gesture, not only a toolbar click. Supported AndroidX back callbacks remain in use; no global opt-out.
- **Large screens/orientation:** Android 16 can ignore orientation/resizability restrictions on sw600dp+ displays. The chart already reacts to configuration and uses weighted sizing; phone portrait/landscape and font/dark-mode tests are in scope. A foldable/tablet/desktop matrix is not claimed.
- **Job quotas/FGS:** Android 16 job runtime quotas also apply with an FGS. Existing periodic work is constrained and retryable, not a promise of exact polling. The existing dataSync time budget (Android 15+) must stop cleanly; two API 36 unit tests cover this callback. No quota evasion or automatic FGS restart was introduced. A six-hour real-time soak is not claimed.
- **SAF:** no media-picker migration is required for CSV documents. File and tree selection must be tested through real DocumentsUI, using only the synthetic provider.
- **16 KB pages:** native ELF alignment and bundle packaging alignment are checked on the final AAB; no claim of a physical 16 KB device test.
- **New lint finding:** `CredManMissingDal` is explicitly retained as a warning in `app/lint.xml`, not hidden or globally ignored. This is the previously documented external `librecare.app` DAL prerequisite (see 2.5.0 report). No fictitious domain affiliation is added. Optional passkeys remain unverified; biometric/PIN and existing local password fallback are unchanged.

## Evidence and limitations

- Complete local JVM suite: **617 tests, 0 failures, 0 errors, 0 skipped**, 84 XML suites; `.delivery-evidence/api36-final-junit/` and `api36-jvm-lint-summary.json`.
- Includes parser 19, import repository 10, imported-history ViewModels 2, database-version/registration 7, explorer math 6, new timeout tests 2.
- Lint: **0 errors, 275 warnings, 9 hints**. Not warning-free.
- First attempt on JDK 17: API 36 sandbox rejected the runtime; the initial new DAL lint error was also preserved. Final JDK 21 run passed JVM, lint, debug and androidTest builds.
- Final API 36 delivery run: **9/9 PASS**, 67.658 seconds; migrations **2/2 PASS**, 0.558 seconds. Exact transcripts: `.delivery-evidence/api36-complete-delivery-instrumentation.txt` and `api36-migrations-instrumentation.txt`. Covers real file/folder pickers, preview/assignment/dedup/repeat/retention, pinch/pan/exact tooltip/multi-year history, person switching, initial range, portrait/landscape/dark/large-font and both system back gestures/insets.
- Earlier attempts remain preserved: first run, reboot and renderer/RAM retry each 5/9; picker/current-window XML showed **System UI isn't responding**, while `lastanr` also recorded Pixel Launcher input-dispatch timeout. Only the owned emulator was restarted; then the specifically identified System UI ANR was closed outside the tests. The next run was 8/9 with a 15-second document-preview timeout; the isolated picker test passed, followed by the entire 9/9 suite. No test/assertion was skipped, weakened or replaced, and no app/AVD data was cleared. This environment/UI timing instability remains a reproducibility risk, not a claim of first-attempt success.
- Changed Android workflows pass actionlint (shellcheck/pyflakes disabled because this check covers workflow syntax only).
- Private real-export integration remains unverified; no authorized private file was supplied. All parser/provider records are synthetic.
- Play acceptance, registered upload-certificate compatibility, store/health/privacy declarations and account requirements remain external checks. No upload/publication is performed.

## Branding / safety classification

Production resources contain no `LibreDisplay`/`LIBREDISPLAY`/`libredisplay` branding strings. Remaining matches are intentional package/applicationId, legacy class/database/work names, repository/documentation references or generated legacy artifact labels. User-facing application brand and new named outputs remain LibreCare.

SDK, emulator image and JDK additions are confined to ignored directories in the authorized worktree. Every Gradle call verifies absolute path, Git toplevel, branch and expected HEAD. API 36 ADB calls require `emulator-5582`, `LibreCare_API36_20261019`, emulator host identity and API 36; physical Samsung and the previous API 35 emulator are not operated on.

## Compliance checklist

| Rule | Status |
|---|---|
| Existing implementation/affected architecture reviewed before changes | PASS |
| Focused compatibility fix, importer/chart preserved, Polish user-facing text | PASS |
| Version increased 2.17.1/52 → 2.17.2/53; code availability confirmed by owner | PASS |
| Unit/ViewModel/repository/migration-test review and added tests | PASS |
| New DB schema/migration, NFZ changes, new statistics features | NOT APPLICABLE — unchanged |
| No private health data/secrets in source/tests/evidence intended for Git | PASS |
| Build/emulator/artifact/release-note/changelog/Git final gates | See final checklist in release report |
| New PR, merge, auto-merge, publication, physical-device operations | NOT APPLICABLE — prohibited and not performed |

## Reference sources

- https://developer.android.com/about/versions/16/setup-sdk
- https://developer.android.com/about/versions/16/behavior-changes-16
- https://developer.android.com/about/versions/16/behavior-changes-all
- https://developer.android.com/develop/background-work/services/fgs/timeout
- https://developer.android.com/build/releases/past-releases/agp-8-10-0-release-notes
- https://github.com/robolectric/robolectric/blob/robolectric-4.16.1/robolectric/src/main/java/org/robolectric/plugins/DefaultSdkProvider.java
