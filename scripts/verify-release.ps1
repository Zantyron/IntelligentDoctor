param(
    [string]$JavaHome = $env:JAVA_HOME,
    [string]$BaseUrl = "http://localhost:8080",
    [switch]$SkipTests,
    [switch]$RunSmoke,
    [switch]$RunRedisLoad
)

$ErrorActionPreference = "Stop"

function Test-Java17 {
    param([string]$JavaHomePath)
    if ([string]::IsNullOrWhiteSpace($JavaHomePath) -or -not (Test-Path "$JavaHomePath\bin\java.exe")) {
        return $false
    }
    $releaseFile = Join-Path $JavaHomePath "release"
    if (Test-Path $releaseFile) {
        $versionText = Get-Content $releaseFile -Raw
        return $versionText -match 'JAVA_VERSION="(17|1[8-9]|[2-9][0-9])\.'
    }
    $versionText = cmd /c "`"$JavaHomePath\bin\java.exe`" -version 2>&1"
    return $versionText -match 'version "17\.|version "1[8-9]\.|version "[2-9][0-9]\.'
}

if (-not (Test-Java17 $JavaHome)) {
    $candidate = "C:\Program Files\Java\jdk-17"
    if (Test-Java17 $candidate) {
        $JavaHome = $candidate
    }
}
if (-not (Test-Java17 $JavaHome)) {
    throw "Java 17 is required. Set JAVA_HOME or pass -JavaHome."
}

$requiredFiles = @(
    "README.md",
    ".env.example",
    "docs\architecture.md",
    "docs\api.md",
    "docs\demo-script.md",
    "docs\deployment.md",
    "docs\final-acceptance.md",
    "docs\release-checklist.md",
    "scripts\start-deps.ps1",
    "scripts\start-app.ps1",
    "scripts\run-demo.ps1",
    "scripts\smoke-test.ps1",
    "scripts\redis-hot-slot-loadtest.ps1"
)
foreach ($file in $requiredFiles) {
    if (-not (Test-Path $file)) {
        throw "Missing release artifact: $file"
    }
}

$envText = Get-Content .env.example -Raw
if ($envText -match "sk-|pk-|AIza|AKIA|BEGIN PRIVATE KEY") {
    throw ".env.example appears to contain a real secret."
}

if (-not $SkipTests) {
    $env:JAVA_HOME = $JavaHome
    $env:Path = "$JavaHome\bin;$env:Path"
    cmd /c mvnw.cmd test
    if ($LASTEXITCODE -ne 0) {
        throw "Maven tests failed."
    }
}

if ($RunSmoke) {
    & .\scripts\smoke-test.ps1 -BaseUrl $BaseUrl
}

if ($RunRedisLoad) {
    & .\scripts\redis-hot-slot-loadtest.ps1
}

Write-Host "Release verification completed." -ForegroundColor Green
