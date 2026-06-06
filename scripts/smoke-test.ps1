param(
    [string]$BaseUrl = "http://localhost:8080"
)

$ErrorActionPreference = "Stop"

function Invoke-Json($Method, $Path, $Body = $null) {
    $params = @{
        Method = $Method
        Uri = "${BaseUrl}${Path}"
        Headers = @{ Accept = "application/json" }
    }
    if ($null -ne $Body) {
        $params.ContentType = "application/json; charset=utf-8"
        $params.Body = ($Body | ConvertTo-Json -Depth 8)
    }
    Invoke-RestMethod @params
}

Write-Host "Checking system profile..." -ForegroundColor Cyan
$profile = Invoke-Json GET "/api/system/profile"
if (-not $profile.success) { throw "System profile failed." }
$profile.data | ConvertTo-Json -Depth 8

Write-Host "Checking diagnosis SSE endpoint..." -ForegroundColor Cyan
$diagnosisBody = @{
    sessionId = "smoke-diagnosis"
    messages = @(@{ role = "user"; content = "最近三天发热咳嗽，晚上更明显。" })
    consentToStoreHistory = $true
}
$diagnosis = Invoke-WebRequest -Method POST -Uri "${BaseUrl}/api/chat/diagnosis/stream" `
    -UseBasicParsing `
    -ContentType "application/json; charset=utf-8" `
    -Body ($diagnosisBody | ConvertTo-Json -Depth 8)
if ($diagnosis.Content -notmatch "event:result") { throw "Diagnosis stream did not return result event." }

Write-Host "Checking registration SSE endpoint and draft generation..." -ForegroundColor Cyan
$sessionId = "smoke-registration"
$registrationBody = @{
    sessionId = $sessionId
    messages = @(@{ role = "user"; content = "胸闷心慌，想挂心内科专家号。" })
    consentToStoreHistory = $true
}
$registration = Invoke-WebRequest -Method POST -Uri "${BaseUrl}/api/chat/registration/stream" `
    -UseBasicParsing `
    -ContentType "application/json; charset=utf-8" `
    -Body ($registrationBody | ConvertTo-Json -Depth 8)
if ($registration.Content -notmatch "draftId") { throw "Registration stream did not include a draft." }

$draft = Invoke-Json GET "/api/registration/draft/latest?sessionId=$sessionId"
if (-not $draft.data.draftId) { throw "Latest draft not found." }

Write-Host "Confirming registration order..." -ForegroundColor Cyan
$order = Invoke-Json POST "/api/registration/confirm" @{
    draftId = $draft.data.draftId
    sessionId = $sessionId
    idempotencyKey = "smoke-$($draft.data.draftId)"
    patientName = "张三"
    patientPhone = "13800000000"
    idCard = "310101199001011234"
}
if (-not $order.data.orderNo) { throw "Registration order was not created." }

Write-Host "Smoke test passed. OrderNo=$($order.data.orderNo)" -ForegroundColor Green
