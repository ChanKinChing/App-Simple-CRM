[CmdletBinding()]
param(
    [switch]$IncludeDatabase
)

$ErrorActionPreference = 'Continue'
$root = Split-Path -Parent $PSScriptRoot
$pgHome = if ($env:PG_HOME) { $env:PG_HOME } else { $env:CRM_PG_HOME }

function Stop-ByPort([int]$Port, [string]$Label) {
    $conns = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue
    if (-not $conns) { Write-Host "$Label - not running"; return }
    foreach ($procId in ($conns.OwningProcess | Select-Object -Unique)) {
        $proc = Get-Process -Id $procId -ErrorAction SilentlyContinue
        if (-not $proc) { continue }
        Write-Host "$Label - stopping $($proc.ProcessName) (pid $procId)"
        & taskkill /PID $procId /T /F | Out-Null
    }
    Start-Sleep -Milliseconds 800
    if (Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue) {
        Write-Host "$Label - still listening on $Port"
    } else {
        Write-Host "$Label - stopped"
    }
}

Stop-ByPort 5173 'Frontend'
Stop-ByPort 8080 'Backend '

Remove-Item -LiteralPath (Join-Path $env:TEMP 'crm-be.pid') -ErrorAction SilentlyContinue
Remove-Item -LiteralPath (Join-Path $env:TEMP 'crm-fe.pid') -ErrorAction SilentlyContinue

if ($IncludeDatabase) {
    if (-not $pgHome) { Write-Host 'Database - set PG_HOME to a folder containing stop-pg.bat'; return }
    $bat = Join-Path $pgHome 'stop-pg.bat'
    if (Test-Path -LiteralPath $bat) { & $bat | Out-Null; Write-Host 'Database - stopped' }
} else {
    Write-Host 'Database - left running (use -IncludeDatabase to stop it)'
}
