#requires -Version 5.1
[CmdletBinding()]
param(
    [string]$JavaHome,
    [string]$AndroidSdk,
    [switch]$Clean,
    [switch]$PrepareOnly,
    [switch]$ForceGradleDownload
)

$ErrorActionPreference = "Stop"
$GradleVersion = "9.5.1"
$CompileSdk = 36
$OutputApkName = "MemoBrain-v1.0.0-debug.apk"
$GradleBaseUrl = "https://services.gradle.org/distributions"
$GradleZipName = "gradle-$GradleVersion-bin.zip"
$GradleZipUrl = "$GradleBaseUrl/$GradleZipName"
$GradleShaUrl = "$GradleZipUrl.sha256"

function Write-Step { param([string]$Message); Write-Host ""; Write-Host "=== $Message ===" -ForegroundColor Cyan }
function Fail { param([string]$Message); Write-Host ""; Write-Host "ERROR: $Message" -ForegroundColor Red; exit 1 }

function Resolve-JavaMajorFromHome {
    param([Parameter(Mandatory = $true)][string]$JdkHomePath)
    if ([string]::IsNullOrWhiteSpace($JdkHomePath)) { return $null }
    try { $resolved = (Resolve-Path -LiteralPath $JdkHomePath -ErrorAction Stop).Path } catch { return $null }
    $javaExe = Join-Path $resolved "bin\java.exe"
    if (-not (Test-Path -LiteralPath $javaExe)) { return $null }
    $releaseFile = Join-Path $resolved "release"
    if (Test-Path -LiteralPath $releaseFile) {
        try {
            $releaseText = Get-Content -LiteralPath $releaseFile -Raw -ErrorAction Stop
            if ($releaseText -match '(?m)^JAVA_VERSION="(?<version>[^"]+)"') {
                $versionText = $Matches["version"]
                if ($versionText -match '^1\.(?<major>\d+)') { return [int]$Matches["major"] }
                if ($versionText -match '^(?<major>\d+)') { return [int]$Matches["major"] }
            }
        } catch {}
    }
    try {
        $quotedJava = '"' + $javaExe + '"'
        $output = (& $env:ComSpec /d /c "$quotedJava -version 2>&1" | Out-String)
        if ($output -match 'version\s+"1\.(?<major>\d+)') { return [int]$Matches["major"] }
        if ($output -match 'version\s+"(?<major>\d+)') { return [int]$Matches["major"] }
    } catch {}
    return $null
}

function New-JavaInfo {
    param([Parameter(Mandatory = $true)][string]$JdkHomePath)
    try { $resolved = (Resolve-Path -LiteralPath $JdkHomePath -ErrorAction Stop).Path } catch { return $null }
    $major = Resolve-JavaMajorFromHome -JdkHomePath $resolved
    if ($null -eq $major) { return $null }
    return New-Object PSObject -Property @{ JavaHome=$resolved; JavaExe=(Join-Path $resolved "bin\java.exe"); Major=[int]$major }
}

function Get-JavaCandidates {
    param([string]$ExplicitJavaHome)
    $items = New-Object System.Collections.Generic.List[string]
    if (-not [string]::IsNullOrWhiteSpace($ExplicitJavaHome)) { $items.Add($ExplicitJavaHome) }
    if ($env:JAVA_HOME) { $items.Add($env:JAVA_HOME) }
    foreach ($candidate in @(
        "$env:ProgramFiles\Android\Android Studio\jbr",
        "$env:ProgramFiles\Android\Android Studio\jre",
        "$env:LOCALAPPDATA\Programs\Android Studio\jbr",
        "$env:LOCALAPPDATA\Android\Android Studio\jbr"
    )) { if (-not [string]::IsNullOrWhiteSpace($candidate)) { $items.Add($candidate) } }
    if ($env:USERPROFILE) {
        $jdksRoot = Join-Path $env:USERPROFILE ".jdks"
        if (Test-Path -LiteralPath $jdksRoot) {
            try { Get-ChildItem -LiteralPath $jdksRoot -Directory -ErrorAction SilentlyContinue | Sort-Object Name | ForEach-Object { $items.Add($_.FullName) } } catch {}
        }
    }
    if ($env:ProgramFiles) {
        foreach ($root in @((Join-Path $env:ProgramFiles "Java"),(Join-Path $env:ProgramFiles "Eclipse Adoptium"),(Join-Path $env:ProgramFiles "Microsoft"))) {
            if (Test-Path -LiteralPath $root) {
                try { Get-ChildItem -LiteralPath $root -Directory -ErrorAction SilentlyContinue | Sort-Object Name | ForEach-Object { $items.Add($_.FullName) } } catch {}
            }
        }
    }
    try {
        $javaCmd = Get-Command java.exe -ErrorAction SilentlyContinue
        if ($javaCmd -and $javaCmd.Source) { $binDir = Split-Path -Parent $javaCmd.Source; $items.Add((Split-Path -Parent $binDir)) }
    } catch {}
    return @($items | Where-Object { $_ } | Select-Object -Unique)
}

function Find-Java {
    param([string]$ExplicitJavaHome)
    $found = @()
    foreach ($candidate in (Get-JavaCandidates -ExplicitJavaHome $ExplicitJavaHome)) {
        $info = New-JavaInfo -JdkHomePath $candidate
        if ($info -and $info.Major -ge 17 -and $info.Major -le 26) { $found += $info }
    }
    if ($found.Count -eq 0) { return $null }
    if (-not [string]::IsNullOrWhiteSpace($ExplicitJavaHome)) { return $found[0] }
    return ($found | Sort-Object @{ Expression={ if ($_.Major -eq 17) {0} elseif ($_.Major -eq 21) {1} else {2} } }, @{ Expression={$_.Major} } | Select-Object -First 1)
}

function Read-SdkFromLocalProperties {
    param([string]$FilePath)
    if (-not (Test-Path -LiteralPath $FilePath)) { return $null }
    try {
        foreach ($line in Get-Content -LiteralPath $FilePath) {
            if ($line -match '^\s*sdk\.dir\s*=\s*(.+?)\s*$') {
                $value=$Matches[1]; $value=$value.Replace('\:', ':'); $value=$value.Replace('\\','\')
                if (Test-Path -LiteralPath $value) { return (Resolve-Path -LiteralPath $value).Path }
            }
        }
    } catch {}
    return $null
}

function Test-AndroidSdk {
    param([string]$SdkPath)
    if ([string]::IsNullOrWhiteSpace($SdkPath)) { return $false }
    try { $resolved=(Resolve-Path -LiteralPath $SdkPath -ErrorAction Stop).Path } catch { return $false }
    return (Test-Path -LiteralPath (Join-Path $resolved ("platforms\android-"+$CompileSdk+"\android.jar")))
}

function Find-AndroidSdk {
    param([string]$ExplicitAndroidSdk,[string]$LocalPropertiesPath)
    $items = New-Object System.Collections.Generic.List[string]
    if (-not [string]::IsNullOrWhiteSpace($ExplicitAndroidSdk)) { $items.Add($ExplicitAndroidSdk) }
    $fromLocal=Read-SdkFromLocalProperties -FilePath $LocalPropertiesPath; if ($fromLocal) {$items.Add($fromLocal)}
    if ($env:ANDROID_SDK_ROOT) {$items.Add($env:ANDROID_SDK_ROOT)}
    if ($env:ANDROID_HOME) {$items.Add($env:ANDROID_HOME)}
    if ($env:LOCALAPPDATA) {$items.Add((Join-Path $env:LOCALAPPDATA "Android\Sdk"))}
    if ($env:USERPROFILE) {$items.Add((Join-Path $env:USERPROFILE "AppData\Local\Android\Sdk"))}
    foreach ($candidate in @($items | Where-Object {$_} | Select-Object -Unique)) {
        if (Test-AndroidSdk -SdkPath $candidate) { return (Resolve-Path -LiteralPath $candidate).Path }
    }
    return $null
}

function Write-LocalProperties {
    param([string]$FilePath,[string]$SdkPath)
    $escaped=$SdkPath.Replace('\','\\').Replace(':','\:')
    Set-Content -LiteralPath $FilePath -Encoding ASCII -Value @("# Auto-generated by Build-MemoBrainApk.ps1","sdk.dir=$escaped")
}

function Invoke-External {
    param([Parameter(Mandatory=$true)][string]$FilePath,[string[]]$Arguments=@(),[string]$WorkingDirectory)
    $oldLocation=Get-Location
    try {
        if ($WorkingDirectory) { Set-Location -LiteralPath $WorkingDirectory }
        Write-Host "> $FilePath $($Arguments -join ' ')" -ForegroundColor DarkGray
        & $FilePath @Arguments
        $exitCode=$LASTEXITCODE; if ($null -eq $exitCode) {$exitCode=0}
        if ($exitCode -ne 0) { throw "コマンドが終了コード $exitCode で失敗しました。" }
    } finally { Set-Location -LiteralPath $oldLocation.Path }
}

function Ensure-GradleWrapper {
    param([string]$ProjectRoot,[switch]$ForceDownload)
    $targetGradlew=Join-Path $ProjectRoot "gradlew"
    $targetGradlewBat=Join-Path $ProjectRoot "gradlew.bat"
    $targetWrapperDir=Join-Path $ProjectRoot "gradle\wrapper"
    $targetWrapperJar=Join-Path $targetWrapperDir "gradle-wrapper.jar"
    $targetWrapperProperties=Join-Path $targetWrapperDir "gradle-wrapper.properties"
    if ((Test-Path $targetGradlewBat) -and (Test-Path $targetWrapperJar) -and (Test-Path $targetWrapperProperties)) { Write-Host "Gradle Wrapper: 既存ファイルを使用"; return }

    $workRoot=Join-Path $env:TEMP ("MemoBrainGradleBootstrap-"+$GradleVersion)
    $zipPath=Join-Path $workRoot $GradleZipName
    $shaPath="$zipPath.sha256"
    $extractRoot=Join-Path $workRoot "extracted"
    $gradleHome=Join-Path $extractRoot ("gradle-"+$GradleVersion)
    $gradleBat=Join-Path $gradleHome "bin\gradle.bat"
    New-Item -ItemType Directory -Path $workRoot -Force | Out-Null
    if ($ForceDownload) { Remove-Item $zipPath,$shaPath -Force -ErrorAction SilentlyContinue; Remove-Item $extractRoot -Recurse -Force -ErrorAction SilentlyContinue }
    [Net.ServicePointManager]::SecurityProtocol=[Net.ServicePointManager]::SecurityProtocol -bor [Net.SecurityProtocolType]::Tls12
    Invoke-WebRequest -Uri $GradleShaUrl -OutFile $shaPath -UseBasicParsing
    $shaText=(Get-Content $shaPath -Raw).Trim(); if ($shaText -notmatch '(?<hash>[A-Fa-f0-9]{64})') { Fail "Gradle SHA-256を解析できませんでした。" }
    $expectedHash=$Matches["hash"].ToUpperInvariant(); $downloadRequired=$true
    if (Test-Path $zipPath) { $existingHash=(Get-FileHash $zipPath -Algorithm SHA256).Hash.ToUpperInvariant(); if ($existingHash -eq $expectedHash) {$downloadRequired=$false} else {Remove-Item $zipPath -Force} }
    if ($downloadRequired) { Invoke-WebRequest -Uri $GradleZipUrl -OutFile $zipPath -UseBasicParsing }
    if ((Get-FileHash $zipPath -Algorithm SHA256).Hash.ToUpperInvariant() -ne $expectedHash) { Fail "Gradle ZIPのSHA-256が一致しません。" }
    if (-not (Test-Path $gradleBat)) { Remove-Item $extractRoot -Recurse -Force -ErrorAction SilentlyContinue; New-Item -ItemType Directory -Path $extractRoot -Force | Out-Null; Expand-Archive $zipPath $extractRoot -Force }
    $bootstrapProject=Join-Path $workRoot "wrapper-project"; Remove-Item $bootstrapProject -Recurse -Force -ErrorAction SilentlyContinue; New-Item -ItemType Directory -Path $bootstrapProject -Force | Out-Null
    Set-Content (Join-Path $bootstrapProject "settings.gradle") "rootProject.name = 'MemoBrainWrapperBootstrap'" -Encoding ASCII
    Set-Content (Join-Path $bootstrapProject "build.gradle") "" -Encoding ASCII
    Invoke-External $gradleBat @("--no-daemon","wrapper","--gradle-version",$GradleVersion,"--distribution-type","bin") $bootstrapProject
    New-Item -ItemType Directory -Path $targetWrapperDir -Force | Out-Null
    Copy-Item (Join-Path $bootstrapProject "gradlew") $targetGradlew -Force
    Copy-Item (Join-Path $bootstrapProject "gradlew.bat") $targetGradlewBat -Force
    Copy-Item (Join-Path $bootstrapProject "gradle\wrapper\*") $targetWrapperDir -Force
    $lines=@(Get-Content $targetWrapperProperties | Where-Object {$_ -notmatch '^distributionSha256Sum='}); $lines += "distributionSha256Sum=$($expectedHash.ToLowerInvariant())"; Set-Content $targetWrapperProperties $lines -Encoding ASCII
}

if (-not [string]::IsNullOrWhiteSpace($PSScriptRoot)) {$ProjectRoot=$PSScriptRoot} elseif ($MyInvocation.MyCommand.Path) {$ProjectRoot=Split-Path -Parent $MyInvocation.MyCommand.Path} else {$ProjectRoot=(Get-Location).Path}
$ProjectRoot=(Resolve-Path -LiteralPath $ProjectRoot).Path
Write-Host "MemoBrain APK Builder v1.0.0" -ForegroundColor Green
foreach ($required in @("settings.gradle.kts","build.gradle.kts","app\build.gradle.kts")) { if (-not (Test-Path (Join-Path $ProjectRoot $required))) { Fail "必要ファイルが見つかりません: $required" } }
$javaInfo=Find-Java -ExplicitJavaHome $JavaHome; if (-not $javaInfo) { Fail "JDK 17～26を検出できませんでした。" }
$env:JAVA_HOME=$javaInfo.JavaHome; $env:Path=(Join-Path $javaInfo.JavaHome "bin")+";"+$env:Path
$localProperties=Join-Path $ProjectRoot "local.properties"; $sdkPath=Find-AndroidSdk -ExplicitAndroidSdk $AndroidSdk -LocalPropertiesPath $localProperties; if (-not $sdkPath) { Fail "Android SDK (API $CompileSdk) を検出できませんでした。" }
$env:ANDROID_SDK_ROOT=$sdkPath; $env:ANDROID_HOME=$sdkPath; Write-LocalProperties $localProperties $sdkPath
Ensure-GradleWrapper -ProjectRoot $ProjectRoot -ForceDownload:$ForceGradleDownload
$gradlewBat=Join-Path $ProjectRoot "gradlew.bat"; Invoke-External $gradlewBat @("--version") $ProjectRoot
if ($PrepareOnly) { Write-Host "準備のみ完了しました。" -ForegroundColor Green; exit 0 }
if ($Clean) { Invoke-External $gradlewBat @("--no-daemon","clean") $ProjectRoot }
Invoke-External $gradlewBat @("--no-daemon",":app:assembleDebug","--stacktrace") $ProjectRoot
$sourceApk=Join-Path $ProjectRoot "app\build\outputs\apk\debug\app-debug.apk"; if (-not (Test-Path $sourceApk)) { Fail "APKが見つかりません。" }
$outputDir=Join-Path $ProjectRoot "output"; New-Item -ItemType Directory -Path $outputDir -Force | Out-Null; $outputApk=Join-Path $outputDir $OutputApkName; Copy-Item $sourceApk $outputApk -Force
Write-Host "MemoBrain APK ビルド成功: $outputApk" -ForegroundColor Green
