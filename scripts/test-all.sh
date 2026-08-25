#!/usr/bin/env bash

# LibreCare Full Test Script
# Runs comprehensive tests including UI, navigation, and backup tests

set -euo pipefail

cd "$(dirname "$0")/.." || exit 1

echo "=========================================="
echo "LibreCare Full Test Suite"
echo "=========================================="
echo ""

# Step 1: Environment verification
echo "Step 1: Verifying environment..."
if [ -f "./scripts/verify-environment.sh" ]; then
  bash ./scripts/verify-environment.sh || true
fi
echo ""

# Step 2: Clean build
echo "Step 2: Cleaning build..."
./gradlew clean --quiet
echo ""

# Step 3: Unit tests
echo "Step 3: Running unit tests..."
./gradlew testDebugUnitTest --stacktrace
echo ""

# Step 4: Lint
echo "Step 4: Running Lint..."
./gradlew lint --stacktrace
echo ""

# Step 5: Debug build
echo "Step 5: Building debug APK..."
./gradlew :app:assembleDebug --stacktrace
echo ""

# Step 6: Android Test APK (for Firebase)
echo "Step 6: Building Android test APK..."
./gradlew :app:assembleDebugAndroidTest --stacktrace
echo ""

# Step 7: Release outputs
echo "Step 7: Building release APK and bundle..."
./gradlew :app:assembleRelease :app:bundleRelease --stacktrace
echo ""

echo "=========================================="
echo "Full Test Suite Complete"
echo "=========================================="
echo ""
echo "Next steps:"
echo "- Review unit test results"
echo "- Check lint reports in app/build/reports/"
echo "- Run Firebase Test Lab tests via GitHub Actions"
echo "- Check debug APK at: app/build/outputs/apk/debug/"

