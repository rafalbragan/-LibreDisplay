# LibreCare Automation Preflight Report

Date: 2026-09-03
Workspace: `C:\Users\SG0216827\IdeaProjects\LibreDisplay-temp-handoff-fix-dev`
Scope: product / bug / implementation automation only

## Verification Legend

- **STATICALLY VERIFIED** — checked by source audit, schema review, YAML parsing, or `actionlint`
- **SIMULATED** — verified by unit or contract tests with mocked GitHub / Copilot interactions
- **LIVE VERIFIED** — proven against a real GitHub run in this audit
- **STILL REQUIRES LIVE TEST** — cannot be fully proven without GitHub-hosted execution

No claims in this report are marked LIVE VERIFIED unless explicitly stated.

## 1. Workflows audited

**STATICALLY VERIFIED**

Audited all workflow files under `.github/workflows/`:

1. `android-build.yml`
2. `android-ci.yml`
3. `android-debug-build.yml`
4. `download-app-testing-results.yml`
5. `firebase-test-lab.yml`
6. `librecare-implementation-automation.yml`
7. `product-inbox.yml`
8. `product-quality.yml`

Key repairs applied:

- removed invalid `metadata: read`
- removed unsupported `copilot-requests` permission keys
- added `models: read` only where read-only Copilot CLI review runs
- added explicit `issues: write` to `master-ci-regression-intake`
- added manual `workflow_dispatch` resume path for confirmed bugs
- removed required-path `|| true` from PR tracking and issue-triggered bug handoff
- added structured failure gates for required Copilot handoff / reassignment stages
- preserved no-auto-merge behavior

## 2. GitHub permissions matrix

**STATICALLY VERIFIED + SIMULATED**

| Operation | Workflow / job | Token | Required permission | Actual permission | Result |
|---|---|---|---|---|---|
| Read/create labels for Product Inbox routing | `product-inbox.yml` / `inbox-review` | `GITHUB_TOKEN` | `issues: write` | `issues: write` | PASS |
| Post/update Product Inbox review comments | `product-inbox.yml` / `inbox-review`, `inbox-decision`, `implementation-ci-repair-loop` | `GITHUB_TOKEN` | `issues: write` | `issues: write` | PASS |
| Create/reuse implementation issue after `/accept` | `product-inbox.yml` / `inbox-decision` | `GITHUB_TOKEN` | `issues: write` | `issues: write` | PASS |
| Read-only Copilot CLI analysis for inbox | `product-inbox.yml` / `inbox-review` | `GITHUB_TOKEN` | `models: read` | `models: read` | PASS |
| Read jobs for CI metadata | `product-inbox.yml` / `implementation-ci-repair-loop` | `GITHUB_TOKEN` | `actions: read` | `actions: read` | PASS |
| Persist Product Foundation records via git push | `product-inbox.yml` / multiple jobs | `GITHUB_TOKEN` | `contents: write` | `contents: write` | PASS |
| Read/create bugfix and bug labels | `librecare-implementation-automation.yml` / bug handoff jobs | `GITHUB_TOKEN` | `issues: write` | `issues: write` | PASS |
| Create/reuse `[Naprawa] BUG-XXXX` issue | `librecare-implementation-automation.yml` / bug handoff jobs | `GITHUB_TOKEN` | `issues: write` | `issues: write` | PASS |
| Read-only Copilot CLI bug triage | `librecare-implementation-automation.yml` / `bug-intake-triage`, `master-ci-regression-intake` | `GITHUB_TOKEN` | `models: read` | `models: read` | PASS |
| Read workflow jobs / artifacts / logs for CI regression evidence | `librecare-implementation-automation.yml` / `master-ci-regression-intake` | `GITHUB_TOKEN` | `actions: read` | `actions: read` | PASS |
| Actual Copilot issue assignment / reassignment | `product-inbox.yml` + `librecare-implementation-automation.yml` handoff and repair jobs | `COPILOT_AGENT_USER_TOKEN` | user token only | user token only | PASS |
| Human review / merge | all flows | n/a | must remain manual | no merge API/path | PASS |

Notes:

- `GITHUB_TOKEN` remains limited to ordinary repository operations.
- `COPILOT_AGENT_USER_TOKEN` is now used only for actual Copilot assignment and reassignment.
- `copilot-requests` was removed because `actionlint` flagged it as unsupported; read-only Copilot CLI jobs now use `models: read`.

## 3. Trigger / event matrix

**STATICALLY VERIFIED**

| Workflow | Triggers | Notes |
|---|---|---|
| `product-inbox.yml` | `issues`, `issue_comment`, `pull_request`, `workflow_run` | covers inbox intake, human decisions, PR tracking, requirement CI repair |
| `librecare-implementation-automation.yml` | `issues`, `pull_request`, `workflow_run`, `workflow_dispatch` | `workflow_dispatch` now provides direct confirmed-bug resume |
| `product-quality.yml` | `push`, `pull_request`, `workflow_dispatch` | now watches `product/**`, `scripts/product/**`, `.github/workflows/**`, `.github/ISSUE_TEMPLATE/**` |
| `android-ci.yml` | `push`, `pull_request`, `workflow_dispatch` | upstream deterministic CI signal source |
| `android-build.yml` | `push`, `pull_request`, `workflow_dispatch` | upstream build artifact source |
| `android-debug-build.yml` | `workflow_dispatch`, `push`, `pull_request` | debug-focused workflow |
| `firebase-test-lab.yml` | `workflow_dispatch` | manual / infra-dependent |
| `download-app-testing-results.yml` | `workflow_dispatch` | manual artifact packaging |

Event payload assumptions reviewed:

- issue payload fields for bug forms and Product Inbox forms
- issue comment payload fields for `/accept`, `/hold`, `/reject`
- pull request payload fields for canonical link tracking
- workflow_run payload fields for run id, branch, conclusion, pull requests, html url
- repository name values beginning with `-` remain passed via `--repo-name=...` form

## 4. Tokens and responsibilities

**STATICALLY VERIFIED + SIMULATED**

### `GITHUB_TOKEN`
Used for:
- issue creation
- issue lookup
- label read/create/reuse
- issue comment upsert
- workflow jobs/artifacts/logs reads
- git push of canonical Product Foundation state
- read-only Copilot CLI runs inside workflows

### `COPILOT_AGENT_USER_TOKEN`
Used only for:
- GraphQL Copilot agent assignment
- GraphQL Copilot reassignment during bounded repair
- REST verification of the real GitHub-side assignee after GraphQL mutation

Secret-handling audit result:
- no hardcoded PATs found
- no token values printed in workflows or scripts
- failure reasons redact bearer-like content
- tokens are not persisted into Product Foundation JSON/YAML records as raw secrets

## 5. Requirement flow result

**SIMULATED**

Flow audited:

Product Inbox
-> AI review
-> human `/accept`
-> canonical `REQ`
-> implementation record
-> implementation issue
-> Copilot assignment
-> branch / PR tracking
-> CI
-> bounded repair
-> `READY_FOR_HUMAN_REVIEW`
-> human merge
-> `VALIDATION_PENDING`

Result: **PASS**

Evidence:
- requirement handoff reuses existing issues instead of duplicating them
- label creation is idempotent and concurrency-safe in Python handoff client
- failed Copilot assignment now returns `HANDOFF_FAILED` and nonzero exit code
- required workflow paths now preserve state, report failure, and fail explicitly afterward
- no auto-merge path was found

## 6. Bug flow result

**SIMULATED**

Flow audited:

CI / bug evidence
-> canonical `BUG`
-> triage
-> `CONFIRMED_DEFECT` / `NEEDS_PRODUCT_DECISION` / `INCONCLUSIVE`

Confirmed-defect path:
-> fix issue
-> Copilot assignment
-> PR tracking
-> CI repair loop
-> human review

Result: **PASS**

Evidence:
- CI regression intake deduplicates by source and root cause
- CI evidence enrichment consumes workflow jobs, run logs, and artifacts
- confirmed defect handoff reuses existing local or remote fix issues
- repeated handoff is idempotent
- failed assignment records `HANDOFF_FAILED` without claiming success
- canonical bug persistence survives bounded retry strategy without destructive merge logic

## 7. CI repair flow result

**SIMULATED**

Result: **PASS**

Repairs applied:
- both requirement and bug CI repair loops now pass `COPILOT_AGENT_USER_TOKEN` only to reassignment logic
- reassignment failures no longer masquerade as success
- canonical state is still persisted before the workflow is failed
- bounded limit of 3 failed attempts remains enforced
- success transitions back to `READY_FOR_HUMAN_REVIEW`

## 8. Copilot handoff result

**STATICALLY VERIFIED + SIMULATED**

Result: **PASS**

Verified characteristics:
- explicit model selection: `GPT-5.4 mini`
- GraphQL feature headers are included
- repository id, issue id, and bot actor id are resolved before mutation
- GraphQL mutation success alone is insufficient; REST assignee verification is still required
- labels alone cannot count as successful assignment
- failed assignment is recorded accurately and surfaced in workflow logs / outputs
- no auto merge is performed

## 9. Idempotency result

**SIMULATED**

Result: **PASS**

Verified against tests / source review:
- bug creation deduplicates by source and root cause
- `bugfix` label creation is idempotent
- requirement labels are now idempotent under concurrent create races
- repeated confirmed bug handoff reuses the same issue
- repeated requirement handoff reuses the same issue
- repeated CI result recording deduplicates identical `(run_id, conclusion)` correlations
- manual resume path reuses existing handoff state when already assigned

## 10. Concurrency result

**SIMULATED + STATICALLY VERIFIED**

Result: **PASS with live-only caveats**

Verified:
- shared Product Foundation workflows use non-canceling concurrency groups to serialize writes
- bug persistence retries against updated remote state rather than destructive migration
- conflicting canonical bug ids are renumbered safely when unrelated remote occupancy exists
- unrelated canonical changes correctly block automatic bug persistence instead of overwriting them

Still requires live test:
- GitHub-hosted timing of two separate workflow runners racing on remote write / push windows

## 11. Silent failure audit

**STATICALLY VERIFIED**

Required-path silent success removed or gated:
- `librecare-implementation-automation.yml` issue-triggered confirmed bug handoff
- `librecare-implementation-automation.yml` PR tracking
- `product-inbox.yml` implementation PR tracking
- requirement CI repair reassignment path
- bug CI repair reassignment path

Optional-path silent behavior retained only where the operation is advisory or summary-only:
- grep extraction of Firebase/Test Lab URLs from already-captured logs
- diagnostic snippets in failure summaries

Additional hardening:
- `download-app-testing-results.yml` now preserves the real `gcloud storage cp` exit code instead of allowing partial failure to appear successful

## 12. actionlint result

**STATICALLY VERIFIED**

Command used:
- `C:\Users\SG0216827\IdeaProjects\LibreDisplay-temp-tools\actionlint\actionlint.exe -color`

Result: **PASS**

Notable finding fixed before final pass:
- unsupported permission scopes (`copilot-requests`) were flagged and removed/replaced with supported scopes

## 13. Tests executed

**SIMULATED / STATICALLY VERIFIED**

Executed successfully:

- `python scripts/product/product_cli.py validate`
- `python -m unittest discover -s scripts/product/tests -p "test_*.py"`
- `python -m unittest scripts.product.tests.test_automation_cli`
- `python -m unittest scripts.product.tests.test_product_cli`
- `python -m compileall scripts/product`
- `actionlint` across all workflows

High-level outcomes:
- requirement happy path: PASS
- bug happy path: PASS
- product-decision path: PASS
- inconclusive path: PASS
- bounded CI repair loop: PASS
- duplicate event handling: PASS
- token / permission contract tests: PASS
- no-auto-merge assertions: PASS

## 14. Unresolved live-only risks

**STILL REQUIRES LIVE TEST**

1. GitHub-hosted acceptance of `models: read` for the read-only Copilot CLI jobs must be confirmed by a real workflow run.
2. Real GitHub Copilot cloud-agent assignment still depends on repository-side feature availability, model entitlement, and user-token scope.
3. Actual GitHub Actions job-log and artifact API behavior can vary by retention/availability and must still be observed on a live run.
4. Manual `workflow_dispatch` resume path is statically valid and simulated, but still needs one real run against `BUG-0002`.

## 15. Exact next manual action

**STILL REQUIRES LIVE TEST**

Run the workflow **LibreCare Implementation Automation** on branch `master` via **workflow_dispatch** with input:

- `bug_id=BUG-0002`

Expected purpose:
- resume the existing confirmed defect directly
- reuse or create exactly one `[Naprawa] BUG-0002` issue
- perform real Copilot cloud-agent assignment with `COPILOT_AGENT_USER_TOKEN`
- persist canonical state
- stop for human review with no automatic merge

