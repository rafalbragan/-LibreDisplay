# Device Safety Authorization (Gradle + ADB)

## Default behavior
- Device-mutating tasks are blocked unless a safe target is selected.
- By default, only emulator targets are allowed.
- Product Owner phone serial is hard-protected: `R5GL1397F1M`.

## Protected task classes
- `connected*AndroidTest`
- `install*`
- `uninstall*`
- `installAndRun`
- `removeApp`

## How to authorize a dedicated physical test device
1. Keep Product Owner phone disconnected from test runs when possible.
2. Set allowlist locally (do not commit to Git):
   - PowerShell session env:
     - `SAFE_PHYSICAL_DEVICE_SERIALS=<SERIAL1>[,<SERIAL2>]`
3. Enable physical operations explicitly for the current command:
   - `ALLOW_PHYSICAL_DEVICE_OPS=true`
4. Select an explicit target serial:
   - `ANDROID_SERIAL=<SERIAL>` or Gradle property `-PdeviceSerial=<SERIAL>`

All four conditions are required for physical-device execution.

## Recommended local command pattern (PowerShell)
```powershell
$env:SAFE_PHYSICAL_DEVICE_SERIALS = "<DEDICATED_TEST_SERIAL>"
$env:ALLOW_PHYSICAL_DEVICE_OPS = "true"
$env:ANDROID_SERIAL = "<DEDICATED_TEST_SERIAL>"
./gradlew verifySafeDeviceGate
```

## Non-authorized examples (will be blocked)
- `-PallowPhysicalDeviceOps=true` without allowlist.
- Allowlisted serial not equal to the selected target serial.
- No explicit target serial with multiple connected devices.
- Any operation targeting protected serial `R5GL1397F1M`.

## IntelliJ / Android Studio guidance
- If using Gradle run configurations for device tasks, set environment variable `ANDROID_SERIAL` to emulator or dedicated test serial.
- Avoid generic app/device launch configurations that auto-pick "any connected device".
- This repository currently does not contain shared `.idea/runConfigurations` files, so local IDE profiles must be reviewed on each workstation.

