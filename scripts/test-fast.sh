#!/usr/bin/env bash

# LibreCare Fast Test Script
# Runs quick tests for fast feedback on changes

set -euo pipefail

cd "$(dirname "$0")/.." || exit 1

LOG_DIR="ci-artifacts"
LOG_FILE="${LOG_DIR}/gradle-fast-suite.log"

mkdir -p "${LOG_DIR}"
: > "${LOG_FILE}"

run_gradle_logged() {
  local label="$1"
  shift

  echo "${label}"
  echo "------------------------------------------"
  set +e
  "$@" 2>&1 | tee -a "${LOG_FILE}"
  local status=${PIPESTATUS[0]}
  set -e
  echo ""
  return "${status}"
}

echo "=========================================="
echo "LibreCare Fast Tests"
echo "=========================================="
echo ""

# Step 1: Environment verification
echo "Step 1: Verifying environment..."
if [ -f "./scripts/verify-environment.sh" ]; then
  bash ./scripts/verify-environment.sh || true
fi
echo ""

# Step 2: Unit tests
echo "Step 2: Running unit tests..."
run_gradle_logged "Running unit tests..." ./gradlew testDebugUnitTest --stacktrace

# Step 3: Lint
echo "Step 3: Running Lint..."
run_gradle_logged "Running Lint..." ./gradlew lint --stacktrace

# Step 4: Debug build
echo "Step 4: Building debug APK..."
run_gradle_logged "Building debug APK..." ./gradlew :app:assembleDebug --stacktrace

echo "=========================================="
echo "Fast Tests Complete"
echo "=========================================="

