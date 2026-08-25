#!/usr/bin/env bash

# LibreCare Fast Test Script
# Runs quick tests for fast feedback on changes

set -euo pipefail

cd "$(dirname "$0")/.." || exit 1

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
./gradlew testDebugUnitTest --stacktrace
echo ""

# Step 3: Lint
echo "Step 3: Running Lint..."
./gradlew lint --stacktrace
echo ""

# Step 4: Debug build
echo "Step 4: Building debug APK..."
./gradlew :app:assembleDebug --stacktrace
echo ""

echo "=========================================="
echo "Fast Tests Complete"
echo "=========================================="

