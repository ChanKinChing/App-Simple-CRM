[CmdletBinding()]
param(
    [switch]$SkipFrontend,
    [int]$TimeoutSeconds = 90
)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-17.0.16.8-hotspot'
$pgHome = '%PG_HOME%'
$jar = Join-Path $root 'backend\target\simple-crm-0.0.1-SNAPSHOT.jar'
$beLog = Join-Path $env:TEMP 'crm-be.log'
$feLog = Join-Path $env:TEMP 'crm-fe.log'

function Test-Port([int]$Port) {
    [bool](Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue)
}

function Wait-Port([int]$Port, [int]$Seconds) {
    $sw = [System.Diagnostics.Stopwatch]::StartNew()
    while ($sw.Elapsed.TotalSeconds -lt $Seconds) {
        if (Test-Port $Port) { return $true }
        Start-Sleep -Milliseconds 500
        Write-Host "." -NoNewline
    }
    return $false
}

Write-Host "[1/3] PostgreSQL 5432" -NoNewline
if (Test-Port 5432) {
    Write-Host " - already listening"
} else {
    Write-Host " - starting portable instance" -NoNewline
    & (Join-Path $pgHome 'start-pg.bat') | Out-Null
    if (-not (Wait-Port 5432 $TimeoutSeconds)) { throw 'PostgreSQL did not start' }
    Write-Host " ok"
}

Write-Host "[2/3] Backend 8080" -NoNewline
if (Test-Port 8080) {
    Write-Host " - already listening"
} else {
    if (-not (Test-Path -LiteralPath $jar)) { throw "Missing jar: $jar (run backend\mvnw.cmd package)" }
    $proc = Start-Process -FilePath (Join-Path $env:JAVA_HOME 'bin\java.exe') `
        -ArgumentList '-jar', $jar `
        -WorkingDirectory (Join-Path $root 'backend') `
        -RedirectStandardOutput $beLog -RedirectStandardError ($beLog -replace '\.log$', '.err') `
        -WindowStyle Hidden -PassThru
    $proc.Id | Set-Content (Join-Path $env:TEMP 'crm-be.pid')
    Write-Host " - pid $($proc.Id), waiting for health" -NoNewline
    $ready = $false
    $sw = [System.Diagnostics.Stopwatch]::StartNew()
    while ($sw.Elapsed.TotalSeconds -lt $TimeoutSeconds) {
        try {
            $health = Invoke-RestMethod -Uri 'http://127.0.0.1:8080/actuator/health' -TimeoutSec 3
            if ($health.status -eq 'UP') { $ready = $true; break }
        } catch { }
        Start-Sleep -Milliseconds 800
        Write-Host "." -NoNewline
    }
    if (-not $ready) { throw "Backend not healthy after $TimeoutSeconds s (see $beLog)" }
    Write-Host " ok ($([int]$sw.Elapsed.TotalSeconds)s)"
}

Write-Host "[3/3] Frontend 5173" -NoNewline
if ($SkipFrontend) {
    Write-Host " - skipped"
} elseif (Test-Port 5173) {
    Write-Host " - already listening"
} else {
    $fe = Start-Process -FilePath 'cmd.exe' `
        -ArgumentList '/c', "npm run dev > `"$feLog`" 2>&1" `
        -WorkingDirectory (Join-Path $root 'frontend') `
        -WindowStyle Hidden -PassThru
    $fe.Id | Set-Content (Join-Path $env:TEMP 'crm-fe.pid')
    Write-Host " - starting vite (pid $($fe.Id))" -NoNewline
    if (-not (Wait-Port 5173 $TimeoutSeconds)) { throw "Frontend did not start (see $feLog)" }
    Write-Host " ok"
}

Write-Host ''
Write-Host 'Frontend : http://localhost:5173'
Write-Host 'Backend  : http://localhost:8080/api'
Write-Host 'Swagger  : http://localhost:8080/swagger-ui.html'
Write-Host 'Login    : admin / Passw0rd!  (or sales / Passw0rd!)'
