#requires -Version 5.1
[CmdletBinding()]
param(
    [switch]$Clean
)

$ErrorActionPreference = "Stop"

function Fail {
    param([string]$Message)
    Write-Host ""
    Write-Host ("ERROR: " + $Message) -ForegroundColor Red
    exit 1
}

if (-not [string]::IsNullOrWhiteSpace($PSScriptRoot)) {
    $ProjectRoot = $PSScriptRoot
}
elseif ($MyInvocation.MyCommand.Path) {
    $ProjectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
}
else {
    $ProjectRoot = (Get-Location).Path
}

$ProjectRoot = (Resolve-Path -LiteralPath $ProjectRoot).Path
$gradlew = Join-Path $ProjectRoot "gradlew.bat"
$releaseSecrets = Join-Path $ProjectRoot "release-secrets.properties"
$signingSecrets = Join-Path $ProjectRoot "signing-secrets.properties"

Write-Host "MemoBrain Signed Release APK Builder v1.0.0" -ForegroundColor Green
Write-Host ("Project: " + $ProjectRoot)

if (-not (Test-Path -LiteralPath $gradlew)) {
    Fail "gradlew.bat was not found. Run git pull and try again."
}

if (-not (Test-Path -LiteralPath $releaseSecrets)) {
    Fail "release-secrets.properties was not found. Copy release-secrets.properties.example and set the production AdMob IDs."
}

if (-not (Test-Path -LiteralPath $signingSecrets)) {
    Fail "signing-secrets.properties was not found. Copy signing-secrets.properties.example and set the signing values."
}

Push-Location $ProjectRoot
try {
    if ($Clean) {
        & $gradlew --no-daemon clean
        if ($LASTEXITCODE -ne 0) {
            Fail "Gradle clean failed."
        }
    }

    & $gradlew --no-daemon :app:assembleRelease --stacktrace
    if ($LASTEXITCODE -ne 0) {
        Fail "Release APK build failed."
    }

    $sourceApk = Join-Path $ProjectRoot "app\build\outputs\apk\release\app-release.apk"
    if (-not (Test-Path -LiteralPath $sourceApk)) {
        Fail ("Signed release APK was not found: " + $sourceApk)
    }

    $outputDir = Join-Path $ProjectRoot "output"
    New-Item -ItemType Directory -Path $outputDir -Force | Out-Null

    $outputApk = Join-Path $outputDir "MemoBrain-v1.0.0-release.apk"
    Copy-Item -LiteralPath $sourceApk -Destination $outputApk -Force

    $hash = (Get-FileHash -LiteralPath $outputApk -Algorithm SHA256).Hash.ToLowerInvariant()
    $shaFile = Join-Path $outputDir "SHA256SUMS.txt"
    Set-Content -LiteralPath $shaFile -Encoding ASCII -Value ("{0}  {1}" -f $hash, (Split-Path -Leaf $outputApk))

    $sdkCandidates = @()
    if ($env:ANDROID_SDK_ROOT) {
        $sdkCandidates += $env:ANDROID_SDK_ROOT
    }
    if ($env:ANDROID_HOME) {
        $sdkCandidates += $env:ANDROID_HOME
    }
    if ($env:LOCALAPPDATA) {
        $sdkCandidates += (Join-Path $env:LOCALAPPDATA "Android\Sdk")
    }

    $apksigner = $null
    foreach ($sdk in ($sdkCandidates | Select-Object -Unique)) {
        if (-not (Test-Path -LiteralPath $sdk)) {
            continue
        }

        $buildTools = Join-Path $sdk "build-tools"
        if (-not (Test-Path -LiteralPath $buildTools)) {
            continue
        }

        $candidate = Get-ChildItem -LiteralPath $buildTools -Directory -ErrorAction SilentlyContinue |
            Sort-Object Name -Descending |
            ForEach-Object { Join-Path $_.FullName "apksigner.bat" } |
            Where-Object { Test-Path -LiteralPath $_ } |
            Select-Object -First 1

        if ($candidate) {
            $apksigner = $candidate
            break
        }
    }

    if ($apksigner) {
        Write-Host ""
        Write-Host "=== APK signature verification ===" -ForegroundColor Cyan
        & $apksigner verify --verbose --print-certs $outputApk
        if ($LASTEXITCODE -ne 0) {
            Fail "APK signature verification failed."
        }
    }
    else {
        Write-Warning "apksigner.bat was not found. Automatic signature verification was skipped."
    }

    Write-Host ""
    Write-Host "Signed release APK created successfully." -ForegroundColor Green
    Write-Host ("APK    : " + $outputApk)
    Write-Host ("SHA256 : " + $hash)
    Write-Host ("SUMS   : " + $shaFile)
}
finally {
    Pop-Location
}
