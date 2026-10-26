[CmdletBinding()]
param(
    [string]$BaseUrl = 'http://127.0.0.1:8080',
    [string]$Username = 'admin',
    [string]$Password = 'Passw0rd!'
)

$ErrorActionPreference = 'Stop'
$results = New-Object System.Collections.Generic.List[object]

function Step([string]$Name, [scriptblock]$Action) {
    $sw = [System.Diagnostics.Stopwatch]::StartNew()
    try {
        $value = & $Action
        $sw.Stop()
        $results.Add([pscustomobject]@{ Step = $Name; Status = 'PASS'; Ms = [int]$sw.Elapsed.TotalMilliseconds; Detail = $value })
    } catch {
        $sw.Stop()
        $results.Add([pscustomobject]@{ Step = $Name; Status = 'FAIL'; Ms = [int]$sw.Elapsed.TotalMilliseconds; Detail = $_.Exception.Message })
    }
}

$script:token = $null
$script:headers = $null
$script:customerId = $null

Step 'health' {
    $h = Invoke-RestMethod -Uri "$BaseUrl/actuator/health" -TimeoutSec 10
    "status=$($h.status) db=$($h.components.db.status)"
}

Step 'login' {
    $body = @{ username = $Username; password = $Password } | ConvertTo-Json
    $r = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/auth/login" -ContentType 'application/json' -Body $body -TimeoutSec 10
    $script:token = $r.token
    $script:headers = @{ Authorization = "Bearer $($r.token)" }
    "user=$($r.user.username) role=$($r.user.role)"
}

Step 'login rejects bad password' {
    try {
        Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/auth/login" -ContentType 'application/json' `
            -Body (@{ username = $Username; password = 'wrong-password' } | ConvertTo-Json) -TimeoutSec 10 | Out-Null
        throw 'expected 401 but the request succeeded'
    } catch {
        $code = $_.Exception.Response.StatusCode.value__
        if ($code -ne 401) { throw "expected 401, got $code" }
        'HTTP 401 as expected'
    }
}

Step 'list customers (page 0, size 5)' {
    $r = Invoke-RestMethod -Uri "$BaseUrl/api/customers?page=0&size=5&sort=name,asc" -Headers $script:headers -TimeoutSec 10
    "items=$($r.content.Count) total=$($r.totalElements) pages=$($r.totalPages)"
}

Step 'search by keyword' {
    $r = Invoke-RestMethod -Uri "$BaseUrl/api/customers?page=0&size=5&q=Victoria" -Headers $script:headers -TimeoutSec 10
    if ($r.totalElements -lt 1) { throw "expected at least one match for q=Victoria, got $($r.totalElements)" }
    "matches=$($r.totalElements)"
}

Step 'create customer' {
    $body = @{
        name    = 'Smoke Test Ltd'
        company = 'Smoke QA'
        email   = "smoke.$([guid]::NewGuid().ToString('N').Substring(0, 8))@example.com"
        phone   = '+852 3000 0000'
        status  = 'LEAD'
        notes   = 'created by scripts/smoke.ps1'
    } | ConvertTo-Json
    $r = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/customers" -Headers $script:headers -ContentType 'application/json' -Body $body -TimeoutSec 10
    $script:customerId = $r.id
    "id=$($r.id)"
}

Step 'validation rejects blank name' {
    try {
        Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/customers" -Headers $script:headers -ContentType 'application/json' `
            -Body (@{ name = ''; email = 'not-an-email' } | ConvertTo-Json) -TimeoutSec 10 | Out-Null
        throw 'expected 400 but the request succeeded'
    } catch {
        $code = $_.Exception.Response.StatusCode.value__
        if ($code -ne 400) { throw "expected 400, got $code" }
        'HTTP 400 as expected'
    }
}

Step 'add follow-up' {
    $body = @{ content = 'Called back, asked for a quote.' } | ConvertTo-Json
    $r = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/customers/$($script:customerId)/follow-ups" `
        -Headers $script:headers -ContentType 'application/json' -Body $body -TimeoutSec 10
    "followUpId=$($r.id)"
}

Step 'customer detail' {
    $r = Invoke-RestMethod -Uri "$BaseUrl/api/customers/$($script:customerId)" -Headers $script:headers -TimeoutSec 10
    "name=$($r.name) company=$($r.company)"
}

Step 'follow-up appears in timeline' {
    $r = Invoke-RestMethod -Uri "$BaseUrl/api/customers/$($script:customerId)/follow-ups?page=0&size=100" `
        -Headers $script:headers -TimeoutSec 10
    if ($r.totalElements -lt 1) { throw 'follow-up was not returned by the timeline endpoint' }
    "entries=$($r.totalElements)"
}

Step 'report summary' {
    $r = Invoke-RestMethod -Uri "$BaseUrl/api/reports/summary" -Headers $script:headers -TimeoutSec 10
    "customers=$($r.totalCustomers) followUpsThisMonth=$($r.followUpsThisMonth)"
}

Step 'delete customer (cleanup)' {
    Invoke-RestMethod -Method Delete -Uri "$BaseUrl/api/customers/$($script:customerId)" -Headers $script:headers -TimeoutSec 10 | Out-Null
    $script:customerId = $null
    'deleted'
}

$results | Format-Table -AutoSize | Out-String -Width 160 | Write-Host
$failed = @($results | Where-Object Status -eq 'FAIL')
Write-Host ("{0} passed / {1} failed / {2} ms total" -f ($results.Count - $failed.Count), $failed.Count, ($results | Measure-Object -Property Ms -Sum).Sum)
if ($failed.Count -gt 0) { exit 1 } else { exit 0 }
