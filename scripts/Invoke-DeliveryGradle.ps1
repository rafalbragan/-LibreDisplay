param(
    [Parameter(Mandatory = $true)][string]$ExpectedHead,
    [Parameter(Mandatory = $true)][string]$Label,
    [Parameter(Mandatory = $true)][string]$Tasks
)
$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
$root = 'C:\Users\SG0216827\IdeaProjects\LibreCare-delivery-20261018'
$branch = 'feature/libreview-chart2-play-20261018'
if ($ExpectedHead -notmatch '^[0-9a-f]{40}$' -or $Label -notmatch '^[a-zA-Z0-9-]+$' -or
    $Tasks -notmatch '^[a-zA-Z0-9.* _-]+$' -or $Tasks -match '(?i)connected') { throw 'STOP: invalid build arguments' }
if ((Resolve-Path -LiteralPath $root).ProviderPath -cne $root) { throw 'STOP: canonical path mismatch' }
$directory = Get-Item -LiteralPath $root
while ($null -ne $directory) {
    if ($directory.Attributes -band [IO.FileAttributes]::ReparsePoint) { throw 'STOP: reparse point' }
    $directory = $directory.Parent
}
Set-Location -LiteralPath $root
if ((Get-Location).ProviderPath -cne $root) { throw 'STOP: CWD mismatch' }
$top = (& git -C $root rev-parse --show-toplevel).Trim().Replace('/', '\')
if ($LASTEXITCODE -ne 0 -or $top -cne $root) { throw 'STOP: Git toplevel mismatch' }
$head = (& git -C $root rev-parse HEAD).Trim()
if ($LASTEXITCODE -ne 0 -or $head -ne $ExpectedHead) { throw 'STOP: HEAD mismatch' }
$actualBranch = (& git -C $root branch --show-current).Trim()
if ($LASTEXITCODE -ne 0 -or $actualBranch -ne $branch) { throw 'STOP: branch mismatch' }
$registration = (& git -C $root worktree list --porcelain) -join "`n"
if ($LASTEXITCODE -ne 0 -or -not $registration.Contains("worktree $($root.Replace('\', '/'))`nHEAD $head`nbranch refs/heads/$branch")) {
    throw 'STOP: worktree registration mismatch'
}
$jdks = @(Get-ChildItem -LiteralPath (Join-Path $root '.delivery-evidence\jdk21') -Directory | Where-Object {
    (Test-Path -LiteralPath (Join-Path $_.FullName 'bin\java.exe')) -and
    ((Get-Content -LiteralPath (Join-Path $_.FullName 'release') -Raw) -match 'JAVA_VERSION="21\.')
})
if ($jdks.Count -ne 1) { throw 'STOP: one worktree-local JDK 21 is required for Robolectric API 36' }
$env:JAVA_HOME = $jdks[0].FullName
$env:ANDROID_HOME = Join-Path $root '.android-sdk'
$env:ANDROID_SDK_ROOT = $env:ANDROID_HOME
if (-not (Test-Path -LiteralPath (Join-Path $env:ANDROID_HOME 'platforms\android-36\android.jar'))) {
    throw 'STOP: worktree-local API 36 SDK is missing'
}
# This test/debug launcher intentionally never imports release signing credentials.
$logs = Join-Path $root '.delivery-evidence'
New-Item -ItemType Directory -Path $logs -Force | Out-Null
$boundary = "BOUNDARY_PASS cwd=$root gitTop=$top branch=$branch head=$head processWorkingDirectory=$root tasks=$Tasks"
$boundary | Tee-Object -FilePath (Join-Path $logs "$Label-boundary.log")
$command = '/d /s /c ""' + $root + '\gradlew.bat" -p "' + $root + '" ' + $Tasks + ' --console=plain"'
$process = Start-Process -FilePath 'C:\Windows\System32\cmd.exe' -ArgumentList $command -WorkingDirectory $root -NoNewWindow -PassThru -Wait -RedirectStandardOutput (Join-Path $logs "$Label-stdout.log") -RedirectStandardError (Join-Path $logs "$Label-stderr.log")
Get-Content -LiteralPath (Join-Path $logs "$Label-stdout.log")
Get-Content -LiteralPath (Join-Path $logs "$Label-stderr.log")
"GRADLE_EXIT=$($process.ExitCode)" | Tee-Object -FilePath (Join-Path $logs "$Label-exit.log")
exit $process.ExitCode

