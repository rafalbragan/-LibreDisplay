# LibreCare 2.17.3-dev local integration report

## Scope and starting state

Continue the owner's existing worktree without reset, stash, cleanup or restructuring.
Verified master: `e49d030eab88b0eadf8957686410439b616105f2`.
Branch: `feat/stabilization-ae-20261104`.
Starting HEAD: `362628fedcf21e33bf56b6c153cc8626dbd4dc17` (existing logout commit, preserved).
One integration PR is appropriate because the chart, history and DEMO changes share surfaces.

## Focused implementation review

| Functionality | Starting classification | Result |
|---|---|---|
| A logout/session/navigation | EXISTS | Preserve existing commit; additionally clear stale dashboard/history state on refresh |
| B zoom/pan/continuity/tooltip | EXISTS | Preserve gesture/window helpers and gap boundaries; move tooltip above chart |
| C history navigator/range synchronization | EXISTS | Preserve navigator; show actual dashboard viewport |
| D Polish date/time labels | EXISTS | Preserve device timezone; add compact month/year labels |
| E Mika DEMO | INCOMPLETE | Finish profile/history, test determinism and reject unrelated patient IDs |
| Isolated test APK signing | INCOMPLETE | Explicit debug signing overrides inherited release signing |
| Local cached-patient isolation | INCOMPLETE | Require an explicit known patient; never substitute the first cached person |

No Room schema/migration, API endpoint, clinical threshold, insulin, privacy-setting,
statistics, widget or production applicationId changes. No broader architecture audit
was repeated. Production version remains 2.17.3/54; local variant is 2.17.3-dev/54.

## Executed validation

Initial targeted test attempt failed only because the worktree lacked an SDK path.
Retried using the existing installed SDK through process-local `ANDROID_HOME`;
no local configuration was committed.

Final command, with `ANDROID_HOME=C:\Users\SG0216827\IdeaProjects\LibreDisplay\.android-sdk`:

```powershell
.\gradlew.bat :app:testDevUnitTest --tests '*SettingsRepositoryDiagnosticsTest' --tests '*DemoModeIsolationTest' --tests '*DemoScenarioSelectorTest' --tests '*PolishFormattersTest' --tests '*HistoryExplorerWindowTest' --tests '*FullScreenHistoryViewportTest' --tests '*LocalGlucoseHistoryRepositoryTest.loadLatestMonitoringSnapshot_includesFull24HourWindow' :app:assembleDev --console=plain --quiet
```

Result: exit 0; 49 tests, zero failures/errors/skips; APK build passed.
Reports: `app\build\reports\tests\testDevUnitTest\index.html` and
`app\build\test-results\testDevUnitTest\TEST-*.xml` (ignored generated output).

`aapt dump badging` verified the actual APK package/version/label.
`apksigner verify --print-certs` verified signing with `CN=Android Debug`.
`git diff --check` passed.

## Final artifact

- Path: `C:\Users\SG0216827\.copilot\session-state\ecf7d027-a2f6-40f7-8ac9-11f3e7673d9c\files\worktrees\stabilization-ae-20261104\release-artifacts\local-test\LibreCare-2.17.3-dev.apk`
- Size: 24,208,031 bytes.
- SHA256: `E391333F2EEC1C768A91C9BC5313D23800BE0B88FE960BE0355458E73E441B7F`
- applicationId: `com.libredisplay.dev`; label: `LibreCare DEV`.
- Version: 2.17.3-dev / 54; minSdk 26, targetSdk 36.
- Certificate SHA256: `962cb41fd3f7ff5dd9c64d71385f459eebf7a8a2446e06ccb02e35572958bd07`.
- Separate package permits side-by-side installation with `com.libredisplay`.
- Generated APK is ignored and is not committed.
- Release APK, release AAB and Google Play upload file: NOT APPLICABLE, explicitly excluded.

## Remaining risks

Device-level installation, touch interaction, accessibility, dark theme, scaling and landscape
verification are deferred by the owner's explicit minimal-validation scope. No physical device,
emulator, instrumentation, full unit suite, lint or release validation was run.
No new claim is made about previously completed checks.
Draft integration PR; implementation is not a production release or Product Owner acceptance.
PR NOT MERGED — awaiting Product Owner review.

Compliance checklist: `docs/quality-review-report-2.17.3-dev.md`.
