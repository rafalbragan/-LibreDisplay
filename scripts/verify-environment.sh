#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
FAILURES=0

section() {
  printf '\n=== %s ===\n' "$1"
}

pass() {
  printf '[PASS] %s\n' "$1"
}

fail() {
  printf '[FAIL] %s\n' "$1"
  FAILURES=$((FAILURES + 1))
}

check_command() {
  local name="$1"
  if command -v "$name" >/dev/null 2>&1; then
	pass "$name available: $(command -v "$name")"
  else
	fail "$name not found in PATH"
  fi
}

check_path() {
  local label="$1"
  local path="$2"
  if [ -e "$path" ]; then
	pass "$label: $path"
  else
	fail "$label missing: $path"
  fi
}

section "LibreCare environment"
echo "Repository root: $ROOT_DIR"
echo "JAVA_HOME: ${JAVA_HOME:-NOT SET}"
echo "ANDROID_HOME: ${ANDROID_HOME:-NOT SET}"
echo "ANDROID_SDK_ROOT: ${ANDROID_SDK_ROOT:-NOT SET}"

section "Core commands"
check_command java
check_command bash
check_command git
check_command adb
check_command sdkmanager

section "Gradle wrapper"
check_path "gradlew" "$ROOT_DIR/gradlew"

section "Java runtime"
if command -v java >/dev/null 2>&1; then
  java -version 2>&1 | head -n 2
fi

section "Android SDK"
SDK_ROOT="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}"
if [ -z "$SDK_ROOT" ] && [ -f "$ROOT_DIR/local.properties" ]; then
  SDK_ROOT="$(grep -E '^sdk\.dir=' "$ROOT_DIR/local.properties" | head -n 1 | cut -d= -f2- | tr -d '\r')"
fi

if [ -z "$SDK_ROOT" ]; then
  fail "Android SDK root not configured"
else
  echo "Effective SDK root: $SDK_ROOT"
  check_path "platform-tools" "$SDK_ROOT/platform-tools"
  check_path "platform android-35" "$SDK_ROOT/platforms/android-35"
  check_path "build-tools 35.0.0" "$SDK_ROOT/build-tools/35.0.0"
  if [ -x "$SDK_ROOT/cmdline-tools/latest/bin/sdkmanager" ]; then
	pass "sdkmanager installed in cmdline-tools"
  elif command -v sdkmanager >/dev/null 2>&1; then
	pass "sdkmanager available in PATH"
  else
	fail "sdkmanager not available in cmdline-tools or PATH"
  fi
fi

section "Build configuration"
echo "compileSdk=35"
echo "targetSdk=35"
echo "minSdk=26"
echo "versionName=2.5.0"
echo "versionCode=27"

section "Summary"
if [ "$FAILURES" -gt 0 ]; then
  printf 'Environment verification failed with %s issue(s).\n' "$FAILURES"
  exit 1
fi

echo "Environment verification passed."

