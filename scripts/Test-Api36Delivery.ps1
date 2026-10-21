param(
    [Parameter(Mandatory = $true)][ValidateSet('Start', 'Stop', 'Reboot', 'Install', 'Test', 'Diagnostics', 'Inspect')][string]$Action,
    [string]$TestClass = '',
    [string]$Label = 'api36-delivery'
)
$ErrorActionPreference = 'Stop'
$root = 'C:\Users\SG0216827\IdeaProjects\LibreCare-delivery-20261018'
$sdk = Join-Path $root '.android-sdk'
$adb = Join-Path $sdk 'platform-tools\adb.exe'
$serial = 'emulator-5582'
$avdName = 'LibreCare_API36_20261019'
if ((Resolve-Path $root).ProviderPath -cne $root -or $Label -notmatch '^[a-zA-Z0-9-]+$') { throw 'STOP: invalid worktree/label' }
$directory = Get-Item -LiteralPath $root
while ($null -ne $directory) {
    if ($directory.Attributes -band [IO.FileAttributes]::ReparsePoint) { throw 'STOP: reparse point' }
    $directory = $directory.Parent
}
Set-Location -LiteralPath $root
if ((& git rev-parse --show-toplevel).Trim().Replace('/', '\') -cne $root) { throw 'STOP: wrong Git root' }
$evidence = Join-Path $root '.delivery-evidence'
function Assert-HostEmulator {
    $processes = @(Get-CimInstance Win32_Process -Filter "Name='qemu-system-x86_64.exe'" | Where-Object {
        $_.CommandLine -match '-avd LibreCare_API36_20261019(?:\s|$)' -and $_.CommandLine -match '-port 5582(?:\s|$)' -and
        $_.ExecutablePath -eq (Join-Path $sdk 'emulator\qemu\windows-x86_64\qemu-system-x86_64.exe')
    })
    if ($processes.Count -ne 1) { throw 'STOP: wrong emulator host identity' }
}
function Raw-Adb([string[]]$Command) {
    Assert-HostEmulator
    $previous = $ErrorActionPreference
    try { $ErrorActionPreference = 'Continue'; $output = & $adb -s $serial @Command 2>&1; $code = $LASTEXITCODE }
    finally { $ErrorActionPreference = $previous }
    if ($code -ne 0) { throw "STOP: emulator command exit=$code" }
    return $output
}
function Verified-Adb([string[]]$Command) {
    $qemu = Raw-Adb @('shell', 'getprop', 'ro.kernel.qemu')
    $avd = Raw-Adb @('emu', 'avd', 'name')
    $api = Raw-Adb @('shell', 'getprop', 'ro.build.version.sdk')
    $model = Raw-Adb @('shell', 'getprop', 'ro.product.model')
    if ("$qemu".Trim() -ne '1' -or "$avd" -notmatch '^LibreCare_API36_20261019' -or "$api".Trim() -ne '36' -or "$model" -match 'SM-|Samsung') { throw 'STOP: wrong Android device identity' }
    "EMULATOR_BOUNDARY_PASS serial=$serial avd=$avdName api=36" | Add-Content (Join-Path $evidence "$Label-boundary.log")
    return Raw-Adb $Command
}
switch ($Action) {
    'Start' {
        if (Get-CimInstance Win32_Process -Filter "Name='qemu-system-x86_64.exe'" | Where-Object { $_.CommandLine -match '-port 5582(?:\s|$)' }) { throw 'STOP: port 5582 already in use' }
        $env:ANDROID_HOME = $sdk
        $env:ANDROID_SDK_ROOT = $sdk
        $env:ANDROID_USER_HOME = Join-Path $evidence 'android-user'
        $env:ANDROID_AVD_HOME = Join-Path $evidence 'avd'
        Start-Process -FilePath (Join-Path $sdk 'emulator\emulator.exe') -WorkingDirectory $root -ArgumentList '-avd LibreCare_API36_20261019 -port 5582 -no-snapshot -no-boot-anim -gpu swiftshader -feature -Vulkan -memory 4096 -cores 4' -RedirectStandardOutput (Join-Path $evidence "$Label-emulator-stdout.log") -RedirectStandardError (Join-Path $evidence "$Label-emulator-stderr.log") | Out-Null
        $booted = $false
        for ($attempt = 0; $attempt -lt 180; $attempt++) {
            Start-Sleep -Seconds 2
            try { $booted = "$(Raw-Adb @('shell', 'getprop', 'sys.boot_completed'))".Trim() -eq '1' } catch { continue }
            if ($booted) { break }
        }
        if (-not $booted) { throw 'STOP: emulator boot timeout' }
        Start-Sleep -Seconds 30
        Verified-Adb @('shell', 'cmd', 'overlay', 'enable-exclusive', '--category', 'com.android.internal.systemui.navbar.gestural') | Out-Null
        $navigation = Verified-Adb @('shell', 'settings', 'get', 'secure', 'navigation_mode')
        if ("$navigation".Trim() -ne '2') { throw 'STOP: gesture navigation not enabled' }
        Verified-Adb @('shell', 'input', 'keyevent', 'KEYCODE_WAKEUP') | Out-Null
        Verified-Adb @('shell', 'wm', 'dismiss-keyguard') | Out-Null
        'API36_EMULATOR_READY_GESTURE_NAVIGATION'
    }
    'Stop' {
        Verified-Adb @('emu', 'kill') | Write-Output
    }
    'Reboot' {
        # Recover only this owned emulator after a System UI ANR; never wipe app/AVD data.
        Verified-Adb @('reboot') | Out-Null
        Start-Sleep -Seconds 10
        $booted = $false
        for ($attempt = 0; $attempt -lt 180; $attempt++) {
            Start-Sleep -Seconds 2
            try { $booted = "$(Raw-Adb @('shell', 'getprop', 'sys.boot_completed'))".Trim() -eq '1' } catch { continue }
            if ($booted) { break }
        }
        if (-not $booted) { throw 'STOP: emulator reboot timeout' }
        Start-Sleep -Seconds 30
        Verified-Adb @('shell', 'input', 'keyevent', 'KEYCODE_WAKEUP') | Out-Null
        Verified-Adb @('shell', 'wm', 'dismiss-keyguard') | Out-Null
        $navigation = Verified-Adb @('shell', 'settings', 'get', 'secure', 'navigation_mode')
        if ("$navigation".Trim() -ne '2') { throw 'STOP: gesture navigation not enabled' }
        'API36_EMULATOR_REBOOTED_WITHOUT_DATA_CLEAR'
    }
    'Install' {
        Verified-Adb @('install', '-r', "$root\app\build\outputs\apk\debug\app-debug.apk") | Write-Output
        Verified-Adb @('install', '-r', "$root\app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk") | Write-Output
    }
    'Test' {
        if ($TestClass -notmatch '^com\.libredisplay\.[A-Za-z0-9_.#]+$') { throw 'STOP: invalid test class' }
        $output = Verified-Adb @('shell', 'am', 'instrument', '-w', '-r', '-e', 'class', $TestClass, 'com.libredisplay.test/androidx.test.runner.AndroidJUnitRunner')
        $output | Tee-Object -FilePath (Join-Path $evidence "$Label-instrumentation.txt")
        if ("$output" -notmatch 'OK \([0-9]+ tests?\)' -or "$output" -match 'FAILURES|INSTRUMENTATION_FAILED|Process crashed') { throw "STOP: instrumentation failed: $Label" }
        if ($TestClass -match 'DeliveryInstrumentedTest') {
            Verified-Adb @('pull', '/sdcard/Android/data/com.libredisplay/files/delivery-screenshots', (Join-Path $evidence $Label)) | Write-Output
        }
    }
    'Diagnostics' {
        Verified-Adb @('logcat', '-d', '-b', 'crash', '-t', '100') | Tee-Object -FilePath (Join-Path $evidence "$Label-crash.txt")
        Verified-Adb @('logcat', '-d', '-t', '300', '-s', 'TestRunner:I', '*:S') | Tee-Object -FilePath (Join-Path $evidence "$Label-test-progress.txt")
        Verified-Adb @('shell', 'dumpsys', 'activity', 'lastanr') | Set-Content (Join-Path $evidence "$Label-last-anr.txt")
    }
    'Inspect' {
        Verified-Adb @('shell', 'uiautomator', 'dump', "/sdcard/$Label.xml") | Out-Null
        Verified-Adb @('pull', "/sdcard/$Label.xml", (Join-Path $evidence "$Label.xml")) | Write-Output
        Verified-Adb @('pull', '/sdcard/Android/data/com.libredisplay/files/delivery-screenshots/picker-hierarchy.xml', (Join-Path $evidence "$Label-picker.xml")) | Write-Output
        Verified-Adb @('shell', 'cmd', 'overlay', 'list', '--user', 'current') | Set-Content (Join-Path $evidence "$Label-overlays.txt")
    }
}
