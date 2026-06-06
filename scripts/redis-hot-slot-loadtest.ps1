param(
    [string]$RedisHost = "localhost",
    [int]$RedisPort = 6379,
    [string]$SlotId = "loadtest-hot-slot",
    [int]$Stock = 50,
    [int]$Concurrency = 200,
    [string]$RedisContainer = "intelligent-doctor-redis"
)

$ErrorActionPreference = "Stop"
$stockKey = "intelligent-doctor:slot-stock:$SlotId"
$redisCli = Get-Command redis-cli -ErrorAction SilentlyContinue
$useDockerRedisCli = $null -eq $redisCli
if ($useDockerRedisCli) {
    docker ps --format "{{.Names}}" | Select-String -SimpleMatch $RedisContainer | Out-Null
    if ($LASTEXITCODE -ne 0) {
        throw "redis-cli was not found and Docker container '$RedisContainer' is not running. Start dependencies with .\scripts\start-deps.ps1 first."
    }
}

function Invoke-RedisCli {
    param([string[]]$Arguments)
    if ($useDockerRedisCli) {
        docker exec $RedisContainer redis-cli @Arguments
    } else {
        redis-cli @Arguments
    }
}

$script = @"
local current = tonumber(redis.call('GET', KEYS[1]) or '-1')
local quantity = tonumber(ARGV[1])
if current < 0 then
    return -2
end
if current < quantity then
    return -1
end
redis.call('DECRBY', KEYS[1], quantity)
return current - quantity
"@

Invoke-RedisCli @("-h", $RedisHost, "-p", "$RedisPort", "SET", $stockKey, "$Stock") | Out-Null
$luaFile = Join-Path $env:TEMP "intelligent-doctor-reserve.lua"
Set-Content -Path $luaFile -Value $script -Encoding utf8
$containerLuaFile = "/tmp/intelligent-doctor-reserve.lua"
if ($useDockerRedisCli) {
    docker cp $luaFile "${RedisContainer}:$containerLuaFile" | Out-Null
}
$started = Get-Date
$jobs = 1..$Concurrency | ForEach-Object {
    Start-Job -ScriptBlock {
        param($hostName, $port, $luaPath, $key, $useDocker, $container)
        if ($useDocker) {
            docker exec $container redis-cli -h $hostName -p $port --eval $luaPath $key , 1
        } else {
            redis-cli -h $hostName -p $port --eval $luaPath $key , 1
        }
    } -ArgumentList $RedisHost, $RedisPort, $(if ($useDockerRedisCli) { $containerLuaFile } else { $luaFile }), $stockKey, $useDockerRedisCli, $RedisContainer
}
$results = $jobs | Receive-Job -Wait -AutoRemoveJob
$elapsed = ((Get-Date) - $started).TotalMilliseconds
$success = @($results | Where-Object { [int]$_ -ge 0 }).Count
$insufficient = @($results | Where-Object { [int]$_ -eq -1 }).Count
$missing = @($results | Where-Object { [int]$_ -eq -2 }).Count
$remaining = [int](Invoke-RedisCli @("-h", $RedisHost, "-p", "$RedisPort", "GET", $stockKey))

[pscustomobject]@{
    slotId = $SlotId
    initialStock = $Stock
    concurrency = $Concurrency
    success = $success
    insufficient = $insufficient
    missingStockKey = $missing
    remaining = $remaining
    elapsedMs = [math]::Round($elapsed, 2)
    noOversell = (($success + $remaining) -eq $Stock -and $success -le $Stock -and $remaining -ge 0)
} | ConvertTo-Json -Depth 4
