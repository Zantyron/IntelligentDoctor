param(
    [switch]$WithKafka
)

$ErrorActionPreference = "Stop"
$composeArgs = @("compose")
if ($WithKafka) {
    $composeArgs += @("--profile", "kafka")
}
$composeArgs += @("up", "-d")

docker @composeArgs
docker compose ps
