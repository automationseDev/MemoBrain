#requires -Version 5.1
<##>
[CmdletBinding()]
param(
    [string]$JavaHome,
    [string]$AndroidSdk,
    [switch]$ForceGradleDownload
)

$ErrorActionPreference = "Stop"

if (-not [string]::IsNullOrWhiteSpace($PSScriptRoot)) {
    $Root = $PSScriptRoot
}
elseif ($MyInvocation.MyCommand.Path) {
    $Root = Split-Path -Parent $MyInvocation.MyCommand.Path
}
else {
    $Root = (Get-Location).Path
}

$Builder = Join-Path $Root "Build-MemoBrainApk.ps1"

if (-not (Test-Path -LiteralPath $Builder)) {
    Write-Host "ERROR: Build-MemoBrainApk.ps1 が見つかりません。" -ForegroundColor Red
    exit 1
}

$params = @{ PrepareOnly = $true }
if (-not [string]::IsNullOrWhiteSpace($JavaHome)) { $params["JavaHome"] = $JavaHome }
if (-not [string]::IsNullOrWhiteSpace($AndroidSdk)) { $params["AndroidSdk"] = $AndroidSdk }
if ($ForceGradleDownload) { $params["ForceGradleDownload"] = $true }

& $Builder @params
