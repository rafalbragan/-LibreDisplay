# LibreCare 2.16.4 - Release Report

## Summary
- Finalized pre-install corrections for DEMO Mika safety and release hardening.
- Kept Google Sign-In and Google Drive backup as separate **IN PROGRESS** scope.
- Added incident hardening for physical-device safety (Gradle gate + RCA report).
- Added allowlist-based physical-device authorization and explicit serial-target enforcement.
- Completed read-only backup audit with secured out-of-repo copy and hash verification.

## Architecture Review (EXISTS / INCOMPLETE / MISSING)
- ViewModels: **EXISTS** (`MonitoringViewModel` enforces DEMO data as non-LIVE and safe messaging).
- Repositories: **EXISTS** (`GlucoseRepository`, `GlucoseSyncRepository`, `AppDataBackupRepository` updated for DEMO isolation).
- Room database/migrations: **EXISTS**, **INCOMPLETE** (`isSynthetic` persisted field still not introduced; source-based isolation used).
- API layer: **EXISTS** (Mika served by mock/demo path; no production LibreLinkUp calls for Mika).
- Demo Mode: **EXISTS** (Mika deterministic generator with no-future timestamp guard).
- Privacy & Data: **INCOMPLETE** (Google OAuth/Drive encryption flow pending).
- Statistics/widgets/charts/navigation: **EXISTS** (DEMO source excluded from LIVE stats/backup filters).

## UI Changes
- Persistent Mika banner remains visible on Monitoring.
- About screen now includes compact in-app feature history for 2.16.4/2.16.3.
- DEMO data path no longer reports LIVE data-connection state for Mika.

## Database / Backup Changes
- DEMO source filtering now supports both `DEMO` and legacy `DemoMode` markers.
- Backup export/restore filters synthetic/demo content by patient prefix and source marker.

## Tests
- `clean` PASS
- `testDebugUnitTest` PASS
- `lint` PASS
- `assembleDebug` PASS
- `assembleRelease` PASS
- `bundleRelease` PASS
- Connected scenario test PASS (`MonitoringRecoveryScenarioInstrumentedTest` on SM-S948B)
- Safety gate check FAIL/PASS as designed (`verifySafeDeviceGate` without/with explicit override)

## Device Safety Hardening
- Gate executes before task actions via task-graph validation (`gradle.taskGraph.whenReady`).
- Protected Product Owner serial is blocked as a target (`R5GL1397F1M`).
- Physical target now requires all of: explicit serial target + allowlist + explicit authorization.
- `installAndRun` and `removeApp` now force `adb -s <serial>`.
- Authorization procedure: `docs/device-safety-authorization.md`.

## Artifacts
- Debug APK: `release-artifacts/LibreCare-2.16.4-debug.apk`
- Release APK: `release-artifacts/LibreCare-2.16.4-release.apk`
- Release AAB: `release-artifacts/LibreCare-2.16.4-release.aab`

## Remaining Risks
- Google Sign-In still requires production OAuth setup by Product Owner.
- Google Drive encrypted backup/restore flow not yet completed.
- Domain model still lacks explicit persisted `isSynthetic` field (current release uses source/prefix policy).

## Incident RCA
- `docs/incident-rca-2026-10-18-physical-device-app-removal.md`
- Non-invasive preserved-data check found local backup file: `C:\Users\SG0216827\IdeaProjects\LibreDisplay\docs\LibreCare-live-backup-1787331718332.json`.
- Backup audit details (metadata-only): `docs/backup-audit-2026-10-18.md`.

