# LibreCare — Google Play release gate

Status: **NOT READY FOR UPLOAD** until all gates below are signed off by the Product Owner. This document is a checklist, not evidence of completed device tests.

## Frozen source and scope
- [ ] PO ACCEPT for production content of PR #56 (fullscreen chart) and PR #57 (dashboard gestures).
- [ ] PR #55 diagnostic Back test is resolved independently; unresolved device issue is explicitly assessed.
- [ ] Human merges on protected master, each independently verified with GitHub.
- [ ] No synthetic test data, developer credentials, or test-only features accidentally enter the production bundle.

## Validation
- [ ] Required commit checks: Fast test suite, build, Build debug APK all PASS on final master SHA.
- [ ] Android 16 (API 36) device instrumentation executed with recorded run ID, device and navigation mode.
- [ ] Dashboard swipe and window navigation verified for 12 h, 7 days, month; point selection, full-screen, vertical scroll and return-to-latest intact.
- [ ] Full-screen history visually reviewed for day, week, month and year. Gaps >20 min must not be interpolated into fabricated glucose measurements.
- [ ] Install-and-upgrade test from previous released version preserves all local persons, glucose records, settings and backup state; no uninstall/clear data.
- [ ] Check notification permissions, background scheduling, privacy screens, login and DEMO without live patient data.

## Google Play Console (human verification)
- [ ] Verify this application matches `com.libredisplay` in Play Console.
- [ ] Verify the highest *ever uploaded* versionCode and assign a strictly higher new value. Do not blindly assume 54.
- [ ] Confirm the exact release versionName and create release/version bump PR only after those facts are confirmed.
- [ ] Confirm Play App Signing upload certificate matches the **upload key** used in GitHub, not just any locally generated keystore.
- [ ] Check completed Data safety, privacy policy URL, medical/health disclosures, store listing, testers, targetSdk and policy declarations in Console.
- [ ] Publish initially only to internal testing. Never automatically promote to production.

## Signed AAB release candidate
- [ ] PR for `.github/workflows/google-play-aab.yml` reviewed and human merged.
- [ ] Configure repository Actions secrets: `RELEASE_KEYSTORE_BASE64`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`; no secret values committed, printed or sent to AI.
- [ ] In GitHub Actions, manually run **Google Play AAB release candidate** on protected `master`.
- [ ] Record run ID and source commit SHA.
- [ ] Verify signed `app-release.aab`, SHA256, correct package/versionCode, signing certificate, size and installability of generated APKs through bundletool if available.
- [ ] PO reviews evidence, accepts upload to the internal test track and separately authorizes any later wider distribution.

## Stop conditions
Any failed CI, missing signing secret, uncertain Play upload certificate, unverifiable versionCode, unresolved crash, privacy leak, data loss on upgrade or unexecuted mandatory device validation => HOLD. Never override failing checks or merge automatically.
