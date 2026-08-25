# LibreCare BrowserStack Manual Upload

## Status

Manual only. No BrowserStack credentials are stored in this repository.

## When to use

Use BrowserStack only when you need:

- an external device matrix beyond Firebase Test Lab
- manual exploratory validation
- a screenshot comparison that is not yet automated in CI

## Required artifacts

Upload the already-built artifacts from:

- `app/build/outputs/apk/debug/`
- `app/build/outputs/apk/release/`
- `app/build/outputs/bundle/release/`

## Recommended manual flow

1. Build the app locally or in GitHub Actions.
2. Download the APK or AAB artifact.
3. Upload it to BrowserStack App Live / App Automate.
4. Run only non-secret test scenarios.
5. Record findings in the repo documentation.

## Security rules

- Never commit BrowserStack credentials.
- Never embed access keys in workflow files.
- Keep all BrowserStack setup outside the repository.

