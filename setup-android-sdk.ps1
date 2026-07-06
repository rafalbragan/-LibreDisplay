param(
    [string]$SdkDir = "$PSScriptRoot\.android-sdk",
    [string]$CmdlineToolsUrl = "https://dl.google.com/android/repository/commandlinetools-win-13114758_latest.zip"
)

$ErrorActionPreference = "Stop"

Write-Host "== LibreDisplay Android SDK setup =="
Write-Host "Target SDK dir: $SdkDir"

New-Item -ItemType Directory -Path $SdkDir -Force | Out-Null
$runId = [Guid]::NewGuid().ToString("N")
$tempZip = Join-Path $env:TEMP "android-cmdline-tools-$runId.zip"
$tempExtract = Join-Path $env:TEMP "android-cmdline-tools-extract-$runId"

if (Test-Path $tempExtract) {
    Remove-Item -Recurse -Force $tempExtract
}

Write-Host "Downloading command line tools..."
Invoke-WebRequest -Uri $CmdlineToolsUrl -OutFile $tempZip

Write-Host "Extracting command line tools..."
Expand-Archive -Path $tempZip -DestinationPath $tempExtract -Force

$cmdlineRoot = Join-Path $SdkDir "cmdline-tools"
$latestDir = Join-Path $cmdlineRoot "latest"
New-Item -ItemType Directory -Path $cmdlineRoot -Force | Out-Null
if (Test-Path $latestDir) {
    Remove-Item -Recurse -Force $latestDir
}

Move-Item -Path (Join-Path $tempExtract "cmdline-tools") -Destination $latestDir

$sdkManager = Join-Path $latestDir "bin\sdkmanager.bat"
if (-not (Test-Path $sdkManager)) {
    throw "sdkmanager.bat not found at $sdkManager"
}

$env:ANDROID_HOME = $SdkDir
$env:ANDROID_SDK_ROOT = $SdkDir

Write-Host "Accepting licenses..."
cmd /c "echo y|`"$sdkManager`" --sdk_root=`"$SdkDir`" --licenses" | Out-Null

Write-Host "Installing required SDK packages..."
& $sdkManager --sdk_root="$SdkDir" "platform-tools" "platforms;android-35" "build-tools;35.0.0" "cmdline-tools;latest"

$escapedSdkDir = $SdkDir.Replace("\", "\\")
$localPropertiesPath = Join-Path $PSScriptRoot "local.properties"
"sdk.dir=$escapedSdkDir" | Set-Content -Path $localPropertiesPath -Encoding ASCII

Write-Host "Done. local.properties updated: $localPropertiesPath"
Write-Host "Run build commands:"
Write-Host "  .\gradlew clean --stacktrace"
Write-Host "  .\gradlew test --stacktrace"
Write-Host "  .\gradlew assembleDebug --stacktrace"

if (Test-Path $tempZip) {
    Remove-Item -Force $tempZip
}
if (Test-Path $tempExtract) {
    Remove-Item -Recurse -Force $tempExtract
}

