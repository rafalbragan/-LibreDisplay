# LibreCare Firebase Test Lab Setup

## Purpose

This document describes how to connect LibreCare to Firebase Test Lab using
GitHub Actions and **Workload Identity Federation** — without storing any
long-lived service account JSON key in the repository.

## Required repository variables

Configure these under **Settings → Secrets and variables → Actions → Variables**
(Repository Variables, not secrets):

- `GCP_PROJECT_ID` — Google Cloud / Firebase project id
- `GCP_WORKLOAD_IDENTITY_PROVIDER` — full Workload Identity Provider resource name,
  e.g. `projects/<number>/locations/global/workloadIdentityPools/<pool>/providers/<provider>`
- `GCP_SERVICE_ACCOUNT` — service account email that GitHub impersonates,
  e.g. `github-actions@<project>.iam.gserviceaccount.com`

Optional (recommended after the first successful run to stabilise the device):

- `FTL_STANDARD_MODEL` — e.g. `MediumPhone.arm`
- `FTL_STANDARD_VERSION` — e.g. `34`

Optional workflow inputs (`workflow_dispatch`):

- locale (default `pl`)
- orientation (default `portrait`)

## Deprecated / removed

The following are **no longer used** and must not be added back:

- `FIREBASE_SERVICE_ACCOUNT_JSON` (long-lived JSON key)
- `FIREBASE_PROJECT_ID` (replaced by `GCP_PROJECT_ID`)

## Authentication model

The workflow uses GitHub OIDC + Google Workload Identity Federation:

```yaml
permissions:
  contents: read
  id-token: write
```

```yaml
- uses: google-github-actions/auth@v2
  with:
    project_id: ${{ vars.GCP_PROJECT_ID }}
    workload_identity_provider: ${{ vars.GCP_WORKLOAD_IDENTITY_PROVIDER }}
    service_account: ${{ vars.GCP_SERVICE_ACCOUNT }}
```

No credentials are written to the repository or logs.

## Workflow expectations

The workflow (`.github/workflows/firebase-test-lab.yml`) will:

- build a fresh `:app:assembleDebug`
- build a fresh `:app:assembleDebugAndroidTest`
- upload both APKs (and `app/build/reports/**`) as artifacts
- authenticate with Workload Identity Federation
- discover available virtual devices via
  `gcloud firebase test android models list` / `versions list`
- run instrumentation tests on Firebase Test Lab
- fail the job if authentication, build, or Test Lab actually fail
- **natively skip** the Firebase steps (not fail) when the three
  `GCP_*` repository variables are missing
- produce a Job Summary with PASS/FAIL/SKIPPED per step, the chosen
  device model/API, locale, APK paths and the Test Lab results link

Expected APK paths:

- `app/build/outputs/apk/debug/app-debug.apk`
- `app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk`

Old APKs from `release-artifacts/` are never used.

## Device selection

1. If `FTL_STANDARD_MODEL` and `FTL_STANDARD_VERSION` are set and currently
   available, they are used.
2. Otherwise a sensible virtual phone is auto-selected from the live catalog
   (preference: `MediumPhone.arm`, `Pixel7`, `Pixel6`, `Pixel3`, `SmallPhone.arm`).
3. The Job Summary reports the exact model + API and the selection source, so
   you can promote it to `FTL_STANDARD_MODEL` / `FTL_STANDARD_VERSION`.

## Minimal device matrix

Recommended first pass:

- one standard virtual phone
- locale `pl`
- portrait orientation

Later you can widen to small / standard / large phones.

## Security rules

- Never commit credentials or JSON keys.
- Never hard-code a keystore or service account file in the repository.
- Never log secret content.
- If required variables are missing, the Firebase step is skipped cleanly and
  the Job Summary states `Firebase Test Lab: SKIPPED_EXTERNAL_CONFIGURATION`
  listing the missing variables.

## First manual run

Trigger via **Actions → Firebase Test Lab → Run workflow** (`workflow_dispatch`).
Optionally override `locale` / `orientation`.

