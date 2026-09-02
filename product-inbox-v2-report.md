# LibreCare Product Inbox v2 Report

Date: 2026-09-02

## Scope

Implemented Product Inbox v2 hardening for Product Foundation only. Android production code was not modified by this task. Firebase was not executed.

## Pre-Implementation Review (affected areas)

- existing implementation: EXISTS
- architecture (Product Foundation pipeline): EXISTS
- ViewModels: NOT APPLICABLE
- repositories: NOT APPLICABLE
- Room database: NOT APPLICABLE
- migrations: NOT APPLICABLE
- API layer: NOT APPLICABLE
- Demo Mode: NOT APPLICABLE
- Privacy & Data: INCOMPLETE (no dedicated privacy report update in this task)
- statistics: NOT APPLICABLE
- widgets: NOT APPLICABLE
- charts: NOT APPLICABLE
- Compose screens: NOT APPLICABLE
- navigation: NOT APPLICABLE

## What Changed

1. Removed deterministic Product Inbox conclusions from `scripts/product/product_cli.py`.
2. Kept deterministic governance for:
   - issue import and canonicalization
   - schema validation
   - idempotent ID handling
   - hard safety overrides
   - human decision gate
3. Added strict AI output contract:
   - `product/schema/inbox_ai_review.schema.json`
4. Added deterministic AI prompt generation command:
   - `python scripts/product/product_cli.py inbox-build-ai-prompt --issue-number <N> --output-file <path>`
5. Added AI review intake command with validation + overrides:
   - `python scripts/product/product_cli.py inbox-apply-ai-review --issue-number <N> --ai-review-file <path>`
6. Updated issue review comment rendering to use persisted AI review only.
7. Updated workflow:
   - label self-bootstrapping by `[Product Inbox]` title prefix
   - `copilot-requests: write`, `contents: write`, `issues: write`
   - install `@github/copilot`
   - Copilot invocation restricted to read-only tools (`read_file`, `file_search`, `grep_search`, `list_dir`)
   - persistence commit/push for review path and decision path
   - concurrency group to serialize Product Inbox state updates
8. Updated issue form title prefix in `.github/ISSUE_TEMPLATE/product-inbox.yml`.

## Required v2 Assertions

REQ0001_ACCEPTANCE: PRESENT_IN_REPO (existing accepted requirement state)
REAL_TEST_RUNS: 7
REAL_REQUIREMENTS: 1

PRODUCT_INBOX_FORM: `.github/ISSUE_TEMPLATE/product-inbox.yml` with title prefix `[Product Inbox]`
PRODUCT_INBOX_SELF_BOOTSTRAPPING_LABEL: IMPLEMENTED_IN_WORKFLOW

REAL_AI_REVIEW: NO (wired in workflow, not executed against real GitHub issue in this local run)
AI_PROVIDER: GITHUB_COPILOT_CLI
AI_AUTH: GITHUB_TOKEN
AI_READ_ONLY: YES (workflow arguments restrict to read tools only)

AI_JSON_SCHEMA_VALIDATION: YES
DETERMINISTIC_SAFETY_OVERRIDE: YES

INBOX_STATE_PERSISTED_TO_REPO: YES (workflow commit step present)
DECISION_STATE_PERSISTED_TO_REPO: YES (workflow commit step present)
WORKFLOW_CONCURRENCY: YES

HUMAN_ACCEPT_COMMAND: /accept
HUMAN_HOLD_COMMAND: /hold
HUMAN_REJECT_COMMAND: /reject
AUTOMATIC_REQUIREMENT_ACCEPTANCE: NO

PRODUCT_VALIDATION: PASS (`python scripts/product/product_cli.py validate`)
PRODUCT_TESTS: PASS (`python -m unittest scripts.product.tests.test_product_cli`)

ANDROID_CODE_CHANGED: NO (by this task)
FIREBASE_EXECUTED: NO

READY_FOR_FIRST_REAL_PRODUCT_INBOX_ITEM: NO

Reason READY is NO:
- Real Copilot CLI invocation was statically wired but not executed in a live GitHub Actions run in this task.
- First real readiness requires one end-to-end Actions dry run on repository branch to validate exact CLI flags in runner environment.

