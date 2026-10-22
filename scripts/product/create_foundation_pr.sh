#!/usr/bin/env bash
# Persist Product Foundation outputs via a reviewable PR; never push to master.
set -euo pipefail

: "${GITHUB_RUN_ID:?GITHUB_RUN_ID must be set}"
: "${GH_TOKEN:?GH_TOKEN must be set}"
: "${PR_TITLE:?PR_TITLE must be set}"
: "${PR_KIND:?PR_KIND must be set}"

if [[ "${GITHUB_REF_NAME:-}" != "master" ]]; then
  echo "Refusing to persist automation state outside the expected master event context." >&2
  exit 1
fi

if [[ "$#" -eq 0 ]]; then
  echo "Explicit staged-path allowlist is required." >&2
  exit 1
fi

git config user.name "github-actions[bot]"
git config user.email "41898282+github-actions[bot]@users.noreply.github.com"
git add -- "$@"
if git diff --cached --quiet; then
  echo "No Product Foundation changes to propose."
  exit 0
fi

branch="automation/${PR_KIND}-${GITHUB_RUN_ID}-${GITHUB_RUN_ATTEMPT:-1}"
git switch -c "$branch"
git commit -m "$PR_TITLE"
git push origin "HEAD:refs/heads/$branch"

gh pr create --base master --head "$branch" --title "$PR_TITLE" \
  --body "Generated Product Foundation state, pending human review and merge. Source workflow: ${GITHUB_WORKFLOW:-unknown}; run: ${GITHUB_SERVER_URL:-https://github.com}/${GITHUB_REPOSITORY}/actions/runs/${GITHUB_RUN_ID}. No auto-merge. Generated records are advisory unless the Product Owner explicitly approves them."

# A PR created with GITHUB_TOKEN does not trigger pull_request workflows.
# Dispatch the exact required checks on the proposed branch instead.
gh workflow run android-ci.yml --ref "$branch"
gh workflow run android-debug-build.yml --ref "$branch"
gh workflow run android-build.yml --ref "$branch"
echo "Created review PR and dispatched required checks for $branch."
