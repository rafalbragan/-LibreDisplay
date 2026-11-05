# LibreCare 2.17.3-dev focused compliance checklist

This is the owner's local A-E integration scope, not a full release certification.
Explicit task overrides take precedence over the general full-release checklist.

| Rule | Status | Evidence / limitation |
|---|---|---|
| Review existing affected implementation before edits | PASS | Existing commit and pending diff read; focused isolation/compile review |
| Fresh worktree / never continue dirty checkout | NOT APPLICABLE | Owner explicitly required preserving and continuing this existing worktree |
| Fetch, verify default branch, master and task HEAD | PASS | Fetch completed; master SHA and starting HEAD in release report |
| Verify related PR and Actions | PASS | No existing PR; starting master Actions inspected; final HEAD state checked at handoff |
| Preserve existing A-E work; minimal scope | PASS | Existing logout commit retained; targeted chart/DEMO/isolation/signing completion |
| Root-cause session/patient isolation | PASS | No arbitrary local patient fallback; active scenario ID validation; stale UI state cleared |
| Preserve production identity/signing/version | PASS | Only dev package/signing variant added; production 2.17.3/54 unchanged |
| Thresholds, insulin and medical recommendations | NOT APPLICABLE | No changes |
| Database version/migration/registration/test | NOT APPLICABLE | No schema changes |
| Settings retention/polling/statistics/privacy/reset | PASS | No settings removed; session reset retained |
| Statistics and estimation values | NOT APPLICABLE | No changes |
| Polish user-facing additions; LibreCare branding | PASS | Added UI labels are Polish; APK label verified as LibreCare DEV |
| Legacy LibreDisplay naming classification | PASS | Remaining changed-file references are package/import/class names or repository paths, not new product labels |
| Compact dashboard/current glucose/single person | PASS | Existing layout preserved; one small viewport label |
| Chart gesture/window/gap/range math tests | PASS | Targeted viewport/window tests pass |
| Tooltip not under finger / local timezone | PASS | Tooltip above history canvas; device-zone formatting preserved |
| Min/max markers, target shading and grid | PASS | Existing canvas rendering preserved |
| Phone, landscape, dark theme, accessibility, runtime chart/navigation checks | NOT APPLICABLE | Device/emulator/large UI verification explicitly deferred; no runtime UI success claim |
| NFZ status/reason/recommendation/dialog | NOT APPLICABLE | No changes |
| Unit/repository tests reviewed and updated | PASS | 49 final targeted tests passed, including session/DEMO/local cache isolation |
| ViewModel behavior reviewed | PASS | Refresh and scenario changes invalidate generations and clear prior state |
| Full suite, clean, lint, debug/release builds, AAB | NOT APPLICABLE | Explicitly replaced by targeted tests and assembleDev |
| Connected tests / emulator / physical device | NOT APPLICABLE | Explicitly prohibited; device availability was not probed |
| Required local build | PASS | assembleDev exit 0 |
| APK existence, size, SHA256, applicationId, debug signing | PASS | Actual APK inspected using aapt/apksigner; release report |
| Named artifact / ignored binary | PASS | release-artifacts\local-test\LibreCare-2.17.3-dev.apk; directory and APK ignored |
| Google Play upload file / publishing | NOT APPLICABLE | Explicitly prohibited |
| Version increase | NOT APPLICABLE | Local variant only; owner prohibited production changes |
| PL/EN release notes and changelog | PASS | Updated for 2.17.3-dev |
| Release/quality reports and remaining risks | PASS | Local validation limits documented |
| Final diff/status / secret and generated-file exclusion | PASS | Explicit source/test/doc staging only; ignored artifact/config/keystore patterns verified |
| Commit/push / integration PR | PASS | Final branch and PR independently checked at handoff |
| No master modification / force push / auto-merge / merge | PASS | Feature branch only; normal push; draft PR; no merge |
| Human governance / acceptance | PASS | No acceptance or implementation-readiness decision persisted; review remains human |

No production-release completeness is claimed. Runtime UI verification remains deferred;
the integration PR is DRAFT.
