#!/usr/bin/env bash
# Run P0 process-death scenarios in separate instrumentation invocations.
# Requires a disposable Android emulator with both debug APKs already installed.
set -euo pipefail
: "${ANDROID_SERIAL:?Set ANDROID_SERIAL to an emulator serial (e.g. emulator-5554)}"
[[ "$ANDROID_SERIAL" =~ ^emulator-[0-9]+$ ]] || { echo "Refusing non-emulator target" >&2; exit 2; }
adb -s "$ANDROID_SERIAL" wait-for-device
[[ "$(adb -s "$ANDROID_SERIAL" shell getprop ro.kernel.qemu | tr -d '\r')" == "1" ]] || { echo "Target is not a QEMU emulator" >&2; exit 2; }
[[ "$(adb -s "$ANDROID_SERIAL" shell getprop ro.build.version.sdk | tr -d '\r')" == "36" ]] || { echo "Requires Android API 36" >&2; exit 2; }
out="${1:-build/p0-process-death-results}"
mkdir -p "$out"
test_class="com.libredisplay.ui.monitoring.P0PersonRecoveryInstrumentedTest"
runner="com.libredisplay.test/androidx.test.runner.AndroidJUnitRunner"
run_one() {
  local method="$1" log="$out/$1.txt"
  adb -s "$ANDROID_SERIAL" shell am instrument -w -r -e class "$test_class#$method" "$runner" | tee "$log"
  grep -Eq 'OK \(1 test\)' "$log" || { echo "Test failed: $method" >&2; exit 1; }
}
force_stop() {
  adb -s "$ANDROID_SERIAL" shell am force-stop com.libredisplay
  local pid
  pid="$(adb -s "$ANDROID_SERIAL" shell pidof com.libredisplay || true)"
  [[ -z "${pid//[[:space:]]/}" ]] || { echo "Process survived force-stop: $pid" >&2; exit 1; }
}
for pair in \
  "test01_prepareLegacyInactiveSelectionForProcessRestart:test02_afterProcessRestart_legacyInactiveSelectionIsRecoverable" \
  "test03_prepareNormalSelectionForProcessRestart:test04_afterProcessRestart_lastGoodSelectionStarts" \
  "test05_prepareStaleSelectionForProcessRestart:test06_afterProcessRestart_staleSelectionDoesNotCrash"; do
  IFS=: read -r prepare verify <<< "$pair"
  force_stop
  run_one "$prepare"
  force_stop
  run_one "$verify"
done
echo "PASS: three independently orchestrated process-death scenarios, API 36 emulator $ANDROID_SERIAL"
