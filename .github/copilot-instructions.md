LibreCare Development Rules

These rules apply to EVERY task, EVERY bug fix, EVERY refactor, EVERY UI change and EVERY feature.

Do not ignore these requirements.

====================================================
GENERAL PRINCIPLE
====================================================

LibreCare is a production-quality Android application.

Whenever a change is implemented:

1. Verify existing implementation first.
2. Fix root causes, not symptoms.
3. Preserve existing functionality.
4. Add or update tests.
5. Build the application.
6. Generate/update reports.
7. Update release notes.
8. Update changelog.
9. Report exact artifact locations.

A task is not complete if only the code was changed.

====================================================
REQUIRED PRE-IMPLEMENTATION REVIEW
====================================================

Before writing code:

Review:

- existing implementation
- architecture
- ViewModels
- repositories
- Room database
- migrations
- API layer
- Demo Mode
- Privacy & Data
- statistics
- widgets
- charts
- Compose screens
- navigation

Report:

EXISTS
INCOMPLETE
MISSING

for all affected functionality.

====================================================
UI REQUIREMENTS
====================================================

LibreCare follows these dashboard principles:

1. Current glucose is always the most important information.
2. Monitored person should appear only once.
3. Avoid duplicated information.
4. Keep dashboard compact.
5. Use readable typography.
6. Avoid oversized padding.
7. Avoid oversized cards.
8. Ensure chart is readable.
9. Ensure touch interactions work.
10. All user-facing strings must be Polish.

Whenever changing UI:

Verify:
- scaling
- dark theme
- accessibility
- readability
- phone-sized screens
- landscape history screen

====================================================
CHART REQUIREMENTS
====================================================

Whenever chart code is touched:

Verify:

- tap opens full-screen history
- tooltip works
- tooltip not hidden under finger
- local timezone used
- min/max markers visible
- target range shading visible
- grid lines readable
- selected person preserved
- selected range preserved

Do not mark complete if chart interaction is broken.

====================================================
NFZ REQUIREMENTS
====================================================

Whenever NFZ code is touched:

Show:

- status
- reason
- recommendation
- evaluation period
- info dialog

Never show only red/green status.

Always explain:

- why condition failed
- current value
- required value
- recommendation

Never claim final NFZ eligibility.

Use wording:

"Na podstawie dostępnych danych..."

====================================================
DATABASE REQUIREMENTS
====================================================

Whenever database schema changes:

Must:

- increase Room version
- create migration
- register migration
- add migration test

Never use destructive migration in release.

====================================================
SETTINGS REQUIREMENTS
====================================================

Always preserve support for:

- data retention
- polling frequency
- statistics
- storage info
- transfer info
- privacy controls
- account reset

If missing:
implement or clearly report.

====================================================
STATISTICS REQUIREMENTS
====================================================

App must expose:

- database size
- database growth
- reading count
- monitored people count
- transfer downloaded
- transfer uploaded
- sync count
- average usage
- storage estimate
- transfer estimate

If enough data does not exist:
show

"Za mało danych do dokładnej estymacji"

Never invent values.

====================================================
TEST REQUIREMENTS
====================================================

Every change must:

- add tests
or
- update tests

Minimum review:

- unit tests
- ViewModel tests
- repository tests
- migration tests if DB changed

When relevant:

- Compose UI tests
- chart interaction tests
- navigation tests

Do not finish task if tests were not reviewed.

====================================================
BUILD REQUIREMENTS
====================================================

Always run:

./gradlew clean

./gradlew testDebugUnitTest

./gradlew lint

./gradlew assembleDebug

./gradlew assembleRelease

./gradlew bundleRelease

Run connected tests if emulator/device exists.

If not:

Report:

"No connected device/emulator available."

====================================================
ARTIFACT REQUIREMENTS
====================================================

Always locate artifacts:

find . -name "*.apk"

find . -name "*.aab"

Always report:

DEBUG APK
RELEASE APK
RELEASE AAB

Include:

- full path
- file size

Always report:

GOOGLE PLAY UPLOAD FILE

using release AAB.

====================================================
LIBRECARE-NAMED ARTIFACTS
====================================================

Create/update:

release-artifacts/

Examples:

LibreCare-<version>-debug.apk

LibreCare-<version>-release.apk

LibreCare-<version>-release.aab

====================================================
VERSIONING REQUIREMENTS
====================================================

Whenever functionality changes:

Review:

versionName
versionCode

Increase version appropriately.

Never decrease versionCode.

Report:

Previous version
New version

====================================================
RELEASE NOTES REQUIREMENTS
====================================================

Always create or update:

docs/releases/LibreCare-<version>-release-notes.md

Must contain:

Polish section
English section

Include:

- version
- fixes
- new features
- improvements
- tests
- artifacts
- known limitations

====================================================
CHANGELOG REQUIREMENTS
====================================================

Always update:

CHANGELOG.md

Add current version at top.

Include:

PL
EN

Added
Changed
Fixed
Tests
Artifacts

====================================================
REPORT REQUIREMENTS
====================================================

Always create/update:

docs/release-report-<version>.md

and, when applicable:

docs/quality-review-report-<version>.md

Must include:

- summary
- architecture review
- UI changes
- database changes
- tests
- artifacts
- remaining risks

====================================================
BRANDING REQUIREMENTS
====================================================

Always verify:

LibreCare

Search:

LibreDisplay
LIBREDISPLAY
libredisplay

Classify:

- user-facing
- package/applicationId
- migration legacy
- documentation
- generated output

Rename user-facing references.

====================================================
GIT REQUIREMENTS
====================================================

Before finishing:

git status

Verify:

.gitignore excludes

- build/
- *.apk
- *.aab
- *.jks
- *.keystore
- local.properties

Commit:

source
tests
docs
reports

Do not commit generated APK/AAB unless explicitly requested.

Push:

git push

Or report exact blocker.

====================================================
FINAL RESPONSE FORMAT
====================================================

Every completed task must report:

Application:
LibreCare

Version:
<version>

Tests:
<status>

Build:
<status>

Release notes:
<path>

Changelog:
<path>

Release report:
<path>

Quality report:
<path>

Debug APK:
<path>
<size>

Release APK:
<path>
<size>

Release AAB:
<path>
<size>

Google Play Upload File:
<path>
<size>

Git:
<branch>
<commit>

Remaining Risks:
<list>

====================================================
HARD STOP RULE
====================================================

A task is NOT complete unless:

- code changed
- tests reviewed
- build completed
- release notes updated
- changelog updated
- reports updated
- artifacts reported
- Google Play upload file identified
- branding verified
- risks reported

MANDATORY COMPLIANCE CHECK

Before finishing any task, create a checklist.

For every rule output:

PASS
FAIL
NOT APPLICABLE

A task cannot be reported as complete while any mandatory rule is FAIL.