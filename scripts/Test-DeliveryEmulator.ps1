param(
    [Parameter(Mandatory = $true)][ValidateSet('Install', 'Test', 'Capture', 'Diagnostics', 'PlayDisplay', 'RestoreDisplay')][string]$Action,
    [string]$TestClass = '',
    [string]$Label = 'delivery'
)
$ErrorActionPreference = 'Stop'
$root = 'C:\Users\SG0216827\IdeaProjects\LibreCare-delivery-20261018'
$adb = 'C:\Users\SG0216827\IdeaProjects\LibreDisplay\.android-sdk\platform-tools\adb.exe'
$serial = 'emulator-5580'
if ((Resolve-Path -LiteralPath $root).ProviderPath -cne $root -or $Label -notmatch '^[a-zA-Z0-9-]+$') { throw 'STOP: invalid emulator execution path/label' }
Set-Location -LiteralPath $root
$evidence = Join-Path $root '.delivery-evidence'
New-Item -ItemType Directory -Path $evidence -Force | Out-Null
function Assert-HostEmulator {
    $processes = @(Get-CimInstance Win32_Process | Where-Object {
        $_.Name -eq 'qemu-system-x86_64.exe' -and $_.CommandLine -match '-avd LibreCare_P0_20261018(?:\s|$)' -and $_.CommandLine -match '-port 5580(?:\s|$)'
    })
    if ($processes.Count -ne 1 -or $serial -ne 'emulator-5580') { throw 'STOP: dedicated emulator process identity mismatch' }
}
function Raw-Adb([string[]]$Command) {
    Assert-HostEmulator
    $prior = $ErrorActionPreference
    try { $ErrorActionPreference = 'Continue'; $result = & $adb -s $serial @Command 2>&1; $code = $LASTEXITCODE }
    finally { $ErrorActionPreference = $prior }
    if ($code -ne 0) { throw "STOP: emulator command failed, exit=$code" }
    return $result
}
function Invoke-VerifiedAdb([string[]]$Command) {
    $state = Raw-Adb @('get-state')
    $qemu = Raw-Adb @('shell', 'getprop', 'ro.kernel.qemu')
    $avd = Raw-Adb @('emu', 'avd', 'name')
    $model = Raw-Adb @('shell', 'getprop', 'ro.product.model')
    if ("$state".Trim() -ne 'device' -or "$qemu".Trim() -ne '1' -or "$avd" -notmatch '^LibreCare_P0_20261018' -or "$model" -match 'SM-|Samsung') {
        throw 'STOP: dedicated emulator device identity mismatch'
    }
    "EMULATOR_BOUNDARY_PASS serial=$serial avd=LibreCare_P0_20261018" | Add-Content -LiteralPath (Join-Path $evidence "$Label-emulator-boundary.log")
    return Raw-Adb $Command
}
switch ($Action) {
    'Install' {
        Invoke-VerifiedAdb @('install', '-r', "$root\app\build\outputs\apk\debug\app-debug.apk") | Write-Output
        Invoke-VerifiedAdb @('install', '-r', "$root\app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk") | Write-Output
    }
    'Test' {
        if ($TestClass -notmatch '^com\.libredisplay\.[A-Za-z0-9_.#]+$') { throw 'STOP: invalid instrumentation class' }
        $output = Invoke-VerifiedAdb @('shell', 'am', 'instrument', '-w', '-r', '-e', 'class', $TestClass, 'com.libredisplay.test/androidx.test.runner.AndroidJUnitRunner')
        $output | Tee-Object -FilePath (Join-Path $evidence "$Label-instrumentation.txt")
        if ($TestClass -match 'DeliveryInstrumentedTest') {
            Invoke-VerifiedAdb @('pull', '/sdcard/Android/data/com.libredisplay/files/delivery-screenshots', (Join-Path $evidence $Label)) | Write-Output
        }
        if ("$output" -notmatch 'OK \([0-9]+ tests?\)' -or "$output" -match 'FAILURES|INSTRUMENTATION_FAILED|Process crashed') { throw "Instrumentation failed: $Label" }
    }
    'Capture' {
        Invoke-VerifiedAdb @('shell', 'screencap', '-p', "/sdcard/$Label.png") | Out-Null
        Invoke-VerifiedAdb @('pull', "/sdcard/$Label.png", (Join-Path $evidence "$Label.png")) | Write-Output
    }
    'Diagnostics' {
        Invoke-VerifiedAdb @('logcat', '-d', '-b', 'crash', '-t', '200') | Tee-Object -FilePath (Join-Path $evidence "$Label-crash.txt")
    }
    'PlayDisplay' {
        $size = Invoke-VerifiedAdb @('shell', 'wm', 'size')
        if ("$size" -match 'Override size:' -or "$size" -notmatch 'Physical size: 1080x2400') { throw 'STOP: unexpected emulator display configuration' }
        Invoke-VerifiedAdb @('shell', 'wm', 'size', '1080x1920') | Write-Output
    }
    'RestoreDisplay' {
        $size = Invoke-VerifiedAdb @('shell', 'wm', 'size')
        if ("$size" -notmatch 'Override size: 1080x1920') { throw 'STOP: display override does not belong to this sprint' }
        Invoke-VerifiedAdb @('shell', 'wm', 'size', 'reset') | Write-Output
    }
}




