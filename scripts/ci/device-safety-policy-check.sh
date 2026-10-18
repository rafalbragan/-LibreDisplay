#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT_DIR"

fail() {
  echo "[FAIL] $1"
  exit 1
}

pass() {
  echo "[PASS] $1"
}

require_pattern() {
  local file="$1"
  local pattern="$2"
  local label="$3"
  if grep -Eq "$pattern" "$file"; then
    pass "$label"
  else
    fail "$label (missing pattern: $pattern in $file)"
  fi
}

require_pattern "build.gradle.kts" "protectedDefaults\s*=\s*setOf\(\"R5GL1397F1M\"\)" "PO device serial is hard-protected"
require_pattern "build.gradle.kts" "gradle\.taskGraph\.whenReady" "pre-execution task-graph guard is enabled"
require_pattern "build.gradle.kts" "isRiskyDeviceTaskName" "risky device task classifier exists"
require_pattern "build.gradle.kts" "adb\",\s*\"-s\",\s*targetSerial.*uninstall" "removeApp uses explicit serial targeting"
require_pattern "build.gradle.kts" "adb\",\s*\"-s\",\s*targetSerial.*am\",\s*\"start" "installAndRun uses explicit serial targeting"
require_pattern "build.gradle.kts" "safePhysicalDeviceSerials|SAFE_PHYSICAL_DEVICE_SERIALS" "physical device allowlist policy is supported"

# CI policy: no workflow may execute connectedAndroidTest without explicit emulator serial targeting.
WORKFLOW_MATCHES="$(grep -RInE "connected(Debug)?AndroidTest" .github/workflows || true)"
if [[ -n "$WORKFLOW_MATCHES" ]]; then
  while IFS= read -r line; do
    file="${line%%:*}"
    if ! grep -Eq "ANDROID_SERIAL=.*emulator-|deviceSerial=emulator-" "$file"; then
      fail "Workflow $file references connectedAndroidTest without emulator-only targeting"
    fi
  done <<< "$(echo "$WORKFLOW_MATCHES" | cut -d: -f1 | sort -u)"
fi
pass "workflow connectedAndroidTest policy check"

echo "Device safety policy check passed."

