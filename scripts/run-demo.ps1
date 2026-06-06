param(
    [string]$JavaHome = $env:JAVA_HOME,
    [int]$Port = 8080
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

$env:JAVA_HOME = $JavaHome
$env:Path = "$JavaHome\bin;$env:Path"
$env:SPRING_PROFILES_ACTIVE = "test"
$env:SERVER_PORT = "$Port"

Write-Host "Starting offline demo on http://localhost:$Port with test profile." -ForegroundColor Cyan
$jar = Get-ChildItem -Path "target" -Filter "intelligent-doctor-*.jar" -ErrorAction SilentlyContinue |
    Where-Object { $_.Name -notlike "*.original" } |
    Sort-Object LastWriteTime -Descending |
    Select-Object -First 1

if ($jar) {
    & "$JavaHome\bin\java.exe" -jar $jar.FullName --spring.profiles.active=test --server.port=$Port
} else {
    cmd /c mvnw.cmd spring-boot:run
}
