$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$pidFile = Join-Path $root '.local/processes.json'
if (-not (Test-Path $pidFile)) { Write-Host 'No PCC local process manifest found.'; exit 0 }
$entries = Get-Content $pidFile -Raw | ConvertFrom-Json
foreach ($entry in @($entries) | Sort-Object -Property Name -Descending) {
  $proc = Get-CimInstance Win32_Process -Filter "ProcessId = $($entry.Pid)" -ErrorAction SilentlyContinue
  if (-not $proc) { continue }
  $cmd = [string]$proc.CommandLine
  $owned = if ($entry.Name -eq 'frontend') {
    $cmd.Contains([string]$entry.Jar) -and $cmd.Contains('--port 3000')
  } else {
    $cmd.Contains([string]$entry.Jar) -and $cmd.Contains('--spring.profiles.active=local')
  }
  if ($owned) {
    Stop-Process -Id $entry.Pid -Force
    Write-Host "Stopped PCC local $($entry.Name) (PID $($entry.Pid))."
  } else {
    Write-Warning "PID $($entry.Pid) no longer matches the recorded PCC command; left untouched."
  }
}
Remove-Item $pidFile -Force
Write-Host 'MySQL was not stopped; databases are left untouched.'
