#requires -Version 5.1
[CmdletBinding()]
param(
    [switch]$Clean
)

$ErrorActionPreference = "Stop"

function Fail {
    param([string]$Message)
    Write-Host "" 
    Write-Host "ERROR: $Message" -ForegroundColor Red
    exit 1
}

if (-not [string]::IsNullOrWhiteSpace($PSScriptRoot)) {
    $ProjectRoot = $PSScriptRoot
} elseif ($MyInvocation.MyCommand.Path) {
    $ProjectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
} else {
    $ProjectRoot = (Get-Location).Path
}

$ProjectRoot = (Resolve-Path -LiteralPath $ProjectRoot).Path
$gradlew = Join-Path $ProjectRoot "gradlew.bat"
$releaseSecrets = Join-Path $ProjectRoot "release-secrets.properties"
$signingSecrets = Join-Path $ProjectRoot "signing-secrets.properties"

Write-Host "MemoBrain Signed Release APK Builder v1.0.0" -ForegroundColor Green
Write-Host "Project: $ProjectRoot"

if (-not (Test-Path -LiteralPath $gradlew)) {
    Fail "gradlew.bat が見つかりません。GitHub から最新の MemoBrain を取得してください。"
}
if (-not (Test-Path -LiteralPath $releaseSecrets)) {
    Fail "release-secrets.properties がありません。release-secrets.properties.example をコピーして正式な AdMob ID を設定してください。"
}
if (-not (Test-Path -LiteralPath $signingSecrets)) {
    Fail "signing-secrets.properties がありません。signing-secrets.properties.example をコピーして署名情報を設定してください。"
}

Push-Location $ProjectRoot
try {
    if ($Clean) {
        & $gradlew --no-daemon clean
        if ($LASTEXITCODE -ne 0) { Fail "Gradle clean に失敗しました。" }
    }

    & $gradlew --no-daemon :app:assembleRelease --stacktrace
    if ($LASTEXITCODE -ne 0) { Fail "Release APK のビルドに失敗しました。" }

    $sourceApk = Join-Path $ProjectRoot "app\build\outputs\apk\release\app-release.apk"
    if (-not (Test-Path -LiteralPath $sourceApk)) {
        Fail "署名済み Release APK が見つかりません: $sourceApk"
    }

    $outputDir = Join-Path $ProjectRoot "output"
    New-Item -ItemType Directory -Path $outputDir -Force | Out-Null

    $outputApk = Join-Path $outputDir "MemoBrain-v1.0.0-release.apk"
    Copy-Item -LiteralPath $sourceApk -Destination $outputApk -Force

    $hash = (Get-FileHash -LiteralPath $outputApk -Algorithm SHA256).Hash.ToLowerInvariant()
    $shaFile = Join-Path $outputDir "SHA256SUMS.txt"
    Set-Content -LiteralPath $shaFile -Encoding ASCII -Value ("{0}  {1}" -f $hash, (Split-Path -Leaf $outputApk))

    $sdkCandidates = @()
    if ($env:ANDROID_SDK_ROOT) { $sdkCandidates += $env:ANDROID_SDK_ROOT }
    if ($env:ANDROID_HOME) { $sdkCandidates += $env:ANDROID_HOME }
    if ($env:LOCALAPPDATA) { $sdkCandidates += (Join-Path $env:LOCALAPPDATA "Android\Sdk") }

    $apksigner = $null
    foreach ($sdk in ($sdkCandidates | Select-Object -Unique)) {
        if (-not (Test-Path -LiteralPath $sdk)) { continue }
        $buildTools = Join-Path $sdk "build-tools"
        if (-not (Test-Path -LiteralPath $buildTools)) { continue }
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
        if ($LASTEXITCODE -ne 0) { Fail "apksigner による署名検証に失敗しました。" }
    } else {
        Write-Warning "apksigner.bat を検出できなかったため、自動署名検証はスキップしました。"
    }

    Write-Host "" 
    Write-Host "署名済み Release APK を生成しました。" -ForegroundColor Green
    Write-Host "APK    : $outputApk"
    Write-Host "SHA256 : $hash"
    Write-Host "SUMS   : $shaFile"
}
finally {
    Pop-Location
}
