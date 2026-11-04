# LibreCare Google Play — release handoff (2026-11-03)

**Release status: HOLD until verified device tests, human merges, Play Console data and signed AAB.**

## PR merge order and owner

1. **PO manually merges #56** after reviewing its eight screenshots. Firebase API 36 history visual review PASS: https://github.com/rafalbragan/-LibreDisplay/actions/runs/41679449818.
2. Update #57 against the new master, resolve any overlapping test-file changes, and rerun required checks. The focused gesture instrumentation PASS on API 36: https://github.com/rafalbragan/-LibreDisplay/actions/runs/41677269389. **PO manually merges #57** after the updated checks PASS.
3. **PO manually merges #58** after its latest release workflow/preflight checks PASS. Release workflow must remain manual and restricted to protected master.
4. Independently verify protected master SHA and required checks after these merges.
5. **PR #55 is diagnostic/test-only and not a production-code prerequisite for the Play AAB.** PO may merge it separately, after rebasing and reconciling the shared DeliveryInstrumentedTest.kt with the already merged #56/#57 tests. Its process-death scenarios have a separate host-orchestrated PASS reported from IntelliJ; do not present the ordinary Firebase matrix as covering real process termination.

**Never run auto-merge, merge with an AI tool, or disable status checks.**

## Two values to read in Google Play Console

1. The **highest versionCode ever uploaded** for package `com.libredisplay`, including releases on internal, closed, open and production tracks. Inspect App bundle explorer and release history. The current repo default is `versionCode=53`, `versionName=2.17.2`; do not assume 54 is safe.
2. The **upload certificate SHA-256** under **App integrity** (Upload key certificate). Distinguish this from the **app signing key certificate**. Copy fingerprint without any private key.

Do not paste the keystore, its passwords, authentication tokens or service account credentials into chat or source control.

## Version bump and upload key

- After verifying the highest used code, create one minimal version bump PR with a strictly larger code and chosen version name, pass CI, then PO manually merges.
- Use the existing upload key, never regenerate it casually: otherwise Play will reject the update.
- In GitHub repository **Settings > Secrets and variables > Actions**, ensure the secrets `RELEASE_KEYSTORE_BASE64`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD` exist. Never put their values in workflow inputs.
- After all relevant PRs are merged, open **Actions > Google Play AAB release candidate > Run workflow**, choose protected **master**.
- Supply `highest_play_version_code` and `expected_upload_cert_sha256` as the two **nonsecret** workflow inputs. The workflow rejects unchanged version codes and mismatched upload certificates.
- Download `LibreCare-GooglePlay-release-AAB` from the successful Actions run. Record the exact master commit, the run link and SHA256.
- Inspect release package name, version and installed-upgrade behavior on a disposable test device before uploading.

## Google Play release

- Select internal testing track first, upload the signed AAB and review all Play Console warnings and declarations, including health-related content, permissions, data safety and privacy policy.
- Confirm the update can install over the previous version without deleting local users, history, settings or backup configuration.
- PO reviews the staged release, test feedback and rollout scope. Publishing to broader tracks needs a separate human decision.

## Release-blocking gates

- A failed mandatory test, an unverified visualization, a missing Android 36 interaction test, uncertain versionCode or certificate, missing upload secret, data-loss upgrade, or an unbuilt/unverified AAB all mean **HOLD**.
- CI compilation and a debug APK do not imply Play Store release readiness.
