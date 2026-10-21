# LibreCare 2.17.0 — Google Play Internal Testing

## Current signing gate: HOLD

No bundle is declared Google Play ready until the registered **upload** certificate is verified. The Product Owner reported that no APK/AAB has previously been uploaded to this Play app and that the upload certificate is currently unavailable.

- Application ID: `com.libredisplay` (unchanged).
- Version: `2.17.0`, versionCode `51` (master was 46; reviewed P0 branch was 50).
- Existing Play app-signing SHA-256, supplied by the owner: `70AB15D3E610088BD33908D9793CA005CCA69CFE755C10AF42FC15305C0AE0A0`.
- Candidate local upload keystore: `C:\Users\SG0216827\IdeaProjects\LibreDisplay\librecare-upload-key.jks`.
- Its public certificate SHA-256, read with a successful keystore integrity check: `14D768C2ECB104A48F239B3E9BC2518D09AD1FD2041C22BEE42E9608C90655DE`.
- The two roles are different. Do not compare the upload key to the app-signing key as if they must match.
- No key was generated, rotated, modified, committed or uploaded. No private key or password was printed.

## Owner upload procedure

1. Open the existing Play application → App integrity → App signing. Obtain the SHA-256 shown under **Upload key certificate**, not App signing key certificate. It must match the local upload certificate above. If it does not, STOP: signing-key changes require separate owner authorization.
2. Preserve the existing Play app-signing key. Do not create another Play application or change the package ID.
3. Recheck the highest versionCode across every track and draft. This branch uses 51, based on the owner's confirmation of no prior uploads. Increase it if any intervening upload has used 51 or higher.
4. Confirm Play Console's current target API requirement. This branch retains compile/target SDK 35 and min SDK 26. The public policy request did not return usable target-API evidence; target API acceptance has not been independently certified. Resolve any Console requirement before uploading.
5. Once certificate identity is confirmed, supply `RELEASE_STORE_FILE`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, and `RELEASE_KEY_PASSWORD` through secure, untracked local Gradle configuration. Use the candidate upload keystore only after confirming it. Never put credentials in a command line, Git, PR, build logs or this document. Build from the verified feature worktree with its guarded Gradle launcher and the actual expected HEAD.
6. Verify the resulting release AAB with `jarsigner -verify` and `keytool -printcert -jarfile`. Check the signer fingerprint, package, versionCode, non-debuggable manifest, size and SHA-256. Use bundletool validation if available. An unsigned intermediate bundle or debug-signed bundle is not acceptable.
7. In Internal testing, create a release, upload the verified **release AAB**, paste the Polish release notes, add the test audience, and review the warnings. Publication/rollout remains a separate human action; this task does not publish anything.

## Store material

- Polish listing: `docs/google-play/store-listing-pl.md`.
- Branding PNGs: `docs/google-play/assets/LibreCare-icon-512.png` and `LibreCare-feature-1024x500.png`.
- Genuine, synthetic-only emulator captures and their dimensions/hashes: `docs/google-play/assets/manifest.json` when validation is complete.
- Internal release notes: `docs/google-play/internal-testing-release-notes-pl.txt`.
- Graphics are branding artwork, not fabricated screenshots. Screenshots must be unmodified captures of the implemented UI.

## Privacy and permissions review checklist for Console

- CSV access uses Android's document/tree picker; no broad filesystem or storage permission was added.
- CSV file content, patient names and original filenames are not logged or transmitted by the importer. Preview uses a temporary private app-cache snapshot; retained provenance contains file hash, record number, source type and timezone interpretation, not the filename or patient name from the file.
- Imported health readings, local profile names, account/session data and settings are sensitive data. Existing LibreLinkUp communication and Android/automatic backup behavior must be reflected in the Data safety and Health apps declarations. Do not claim that the entire app never communicates off-device.
- Existing permissions include Internet/network state, notification and foreground data-sync service permissions. No new production permission was added.
- Provide a real public privacy-policy URL, support contact, applicable account-deletion information and truthful health-functionality declarations. These owner-specific Console values are not fabricated by this task.
- The physical Samsung installation has a different signer. Do not install this APK/AAB on it or attempt an in-place update. No physical-device compatibility claim is made.

