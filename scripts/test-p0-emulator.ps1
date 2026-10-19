param(
    [Parameter(Mandatory = $true)][string]$Serial,
    [Parameter(Mandatory = $true)][string]$Adb,
    [ValidateSet('All', 'Switch', 'Legacy', 'Stale', 'StaleRestart')][string]$Phase = 'All',
    [string]$EvidenceLabel = 'after'
)
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$evidence = Join-Path $root "build\p0-evidence\$EvidenceLabel"
New-Item -ItemType Directory -Force -Path $evidence | Out-Null

function Assert-Emulator {
    if ($Serial -notmatch '^emulator-[0-9]+$') { throw 'Only an explicitly named emulator is allowed.' }
    $port = [regex]::Escape($Serial.Substring('emulator-'.Length))
    $emulatorProcesses = @(Get-CimInstance Win32_Process | Where-Object {
        $_.Name -like 'qemu-system-*' -and
        $_.CommandLine -match ('-port\s+' + $port + '(?:\s|$)') -and
        $_.CommandLine -match '-avd\s+"?LibreCare_P0_'
    })
    if ($emulatorProcesses.Count -ne 1) { throw 'Dedicated emulator process command line is not verified.' }
    $state = & $Adb -s $Serial get-state 2>&1
    if ($LASTEXITCODE -ne 0 -or "$state".Trim() -ne 'device') { throw 'Selected emulator is unavailable.' }
    $qemu = & $Adb -s $Serial shell getprop ro.kernel.qemu
    $avd = & $Adb -s $Serial emu avd name
    $model = & $Adb -s $Serial shell getprop ro.product.model
    if ("$qemu".Trim() -ne '1' -or "$avd" -notmatch 'LibreCare_P0_' -or "$model" -match 'SM-|Samsung') {
        throw 'Target is not a verified dedicated LibreCare P0 emulator.'
    }
    $avdName = [regex]::Escape(([string]@($avd)[0]).Trim())
    if ($emulatorProcesses[0].CommandLine -notmatch ('-avd\s+"?' + $avdName + '(?:"|\s|$)')) {
        throw 'Emulator AVD identity differs from the verified process.'
    }
}

function Invoke-Emulator([string[]]$Arguments) {
    Assert-Emulator
    # Windows PowerShell 5.1 treats native stderr (even blank lines) as errors.
    # Capture it without terminating early; the native exit code remains mandatory.
    $previousPreference = $ErrorActionPreference
    try {
        $ErrorActionPreference = 'Continue'
        $output = & $Adb -s $Serial @Arguments 2>&1
        $exitCode = $LASTEXITCODE
    } finally {
        $ErrorActionPreference = $previousPreference
    }
    if ($exitCode -ne 0) { throw "Emulator command failed (exit $exitCode): $output" }
    return $output
}

function Run-Test([string]$Method) {
    $output = Invoke-Emulator @('shell', 'am', 'instrument', '-w', '-r', '-e', 'class',
        "com.libredisplay.ui.monitoring.P0PersonRecoveryInstrumentedTest#$Method",
        'com.libredisplay.test/androidx.test.runner.AndroidJUnitRunner')
    $output | Set-Content -Encoding UTF8 (Join-Path $evidence "$Method.txt")
    $output | Write-Output
    if ("$output" -notmatch 'OK \(1 test\)' -or "$output" -match 'FAILURES|INSTRUMENTATION_FAILED|Process crashed') {
        throw "P0 regression failed: $Method (see $evidence)"
    }
}

function Restart-Process {
    Invoke-Emulator @('shell', 'am', 'force-stop', 'com.libredisplay') | Out-Null
    $pidOutput = Invoke-Emulator @('shell', 'sh', '-c', "'pidof com.libredisplay || true'")
    if ("$pidOutput".Trim()) { throw 'Application process did not terminate.' }
}

Assert-Emulator
"Verified target: $Serial (dedicated LibreCare P0 AVD)" | Tee-Object (Join-Path $evidence 'target.txt')
Invoke-Emulator @('install', '-r', (Join-Path $root 'app\build\outputs\apk\debug\app-debug.apk')) | Write-Output
Invoke-Emulator @('install', '-r', (Join-Path $root 'app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk')) | Write-Output
$inComponentList = $false
foreach ($line in (Invoke-Emulator @('shell', 'dumpsys', 'package', 'com.libredisplay'))) {
    if ($line.Trim() -in @('enabledComponents:', 'disabledComponents:')) { $inComponentList = $true }
    elseif ($inComponentList) {
        if ($line.Trim() -eq 'com.libredisplay.receiver.BootReceiver') {
            throw 'P0 tests require the receiver default state; refusing to overwrite a pre-existing override.'
        }
        if ($line -notmatch '^\s+[\w.$]+\s*$') { $inComponentList = $false }
    }
}
$receiver = 'com.libredisplay/.receiver.BootReceiver'
$receiverDisabled = $false
try {
    # Android 15 can send BOOT_COMPLETED when a force-stopped package is un-stopped.
    # Prevent an Activity launch BEFORE test transport injection; production boot behavior is unchanged.
    # Component changes require the owning debug app UID, not Android's restricted shell UID.
    Invoke-Emulator @('shell', 'run-as', 'com.libredisplay', 'pm', 'disable', '--user', '0', $receiver) | Write-Output
    $receiverDisabled = $true
    if ($Phase -in @('All', 'Switch')) {
        Run-Test 'inactiveSelection_canReturnWithoutMisattribution'
        Restart-Process
        Run-Test 'afterProcessRestart_lastGoodSelectionStarts'
    }
    if ($Phase -in @('All', 'Legacy')) {
        Run-Test 'prepareLegacyInactiveSelectionForProcessRestart'
        Restart-Process
        Run-Test 'afterProcessRestart_legacyInactiveSelectionIsRecoverable'
    }
    if ($Phase -in @('All', 'Stale')) { Run-Test 'stalePersonNeverDisplaysActivePersonsReading' }
    if ($Phase -in @('All', 'Stale', 'StaleRestart')) {
        Restart-Process
        Run-Test 'afterProcessRestart_staleSelectionDoesNotCrash'
    }
    if ($Phase -eq 'All') { Run-Test 'pendingPolling_cannotHideSwitcherOrOverwriteLatestChoice' }
    Invoke-Emulator @('pull', '/sdcard/Android/data/com.libredisplay/files/p0-evidence', $evidence) | Write-Output
} finally {
    if ($receiverDisabled) {
        Invoke-Emulator @('shell', 'run-as', 'com.libredisplay', 'pm', 'default-state', '--user', '0', $receiver) | Write-Output
    }
}
