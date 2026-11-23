param([switch]$SkipBuild)
$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$stateDir = Join-Path $root '.local'
$logDir = Join-Path $stateDir 'logs'
$pidFile = Join-Path $stateDir 'processes.json'
New-Item -ItemType Directory -Force $logDir | Out-Null

if (Test-Path $pidFile) {
  $old = Get-Content $pidFile -Raw | ConvertFrom-Json
  $alive = @($old | Where-Object { Get-Process -Id $_.Pid -ErrorAction SilentlyContinue })
  if ($alive.Count) { throw 'PCC local processes already recorded; run scripts/check-local.ps1 or stop-local.ps1 first.' }
}

# Read the ignored local env file without echoing any values.
$envFile = Join-Path $root '.env.local'
if (Test-Path $envFile) {
  Get-Content $envFile | ForEach-Object {
    if ($_ -match '^\s*([A-Za-z_][A-Za-z0-9_]*)=(.*)$') {
      [Environment]::SetEnvironmentVariable($Matches[1], $Matches[2].Trim('"'), 'Process')
    }
  }
}
if (-not $env:JWT_SECRET -or $env:JWT_SECRET.Length -lt 64) {
  throw 'Set a local JWT_SECRET of at least 64 characters in ignored .env.local.'
}

# Start, but never restart, the configured local MySQL Windows service.
$mysql = Get-Service -Name 'MySQL*' -ErrorAction SilentlyContinue |
  Where-Object { $_.Status -eq 'Running' } | Select-Object -First 1
if (-not $mysql) {
  $mysql = Get-Service -Name 'MySQL*' -ErrorAction SilentlyContinue | Select-Object -First 1
  if (-not $mysql) { throw 'No MySQL Windows service was found.' }
  Start-Service $mysql.Name
  (Get-Service $mysql.Name).WaitForStatus('Running', [TimeSpan]::FromSeconds(60))
}

$services = @(
  @{ Name='identity'; Module='pcc-identity-service'; Port=9002; Url='http://localhost:9002/actuator/health' },
  @{ Name='property'; Module='pcc-property-service'; Port=9001; Url='http://localhost:9001/actuator/health' },
  @{ Name='land'; Module='pcc-land-service'; Port=9003; Url='http://localhost:9003/actuator/health' },
  @{ Name='finance'; Module='pcc-finance-service'; Port=9004; Url='http://localhost:9004/actuator/health' },
  @{ Name='document'; Module='pcc-document-service'; Port=9005; Url='http://localhost:9005/actuator/health' },
  @{ Name='workflow'; Module='pcc-workflow-service'; Port=9006; Url='http://localhost:9006/actuator/health' },
  @{ Name='reporting'; Module='pcc-reporting-service'; Port=9007; Url='http://localhost:9007/actuator/health' },
  @{ Name='gateway'; Module='pcc-api-gateway'; Port=9000; Url='http://localhost:9000/actuator/health' }
)
foreach ($svc in $services) {
  if (Get-NetTCPConnection -State Listen -LocalPort $svc.Port -ErrorAction SilentlyContinue) {
    throw "Port $($svc.Port) is already occupied; refusing to create a duplicate."
  }
}

$urls = @{
  IDENTITY_SERVICE_URL='http://localhost:9002'; PROPERTY_SERVICE_URL='http://localhost:9001'
  LAND_SERVICE_URL='http://localhost:9003'; FINANCE_SERVICE_URL='http://localhost:9004'
  DOCUMENT_SERVICE_URL='http://localhost:9005'; WORKFLOW_SERVICE_URL='http://localhost:9006'
  REPORTING_SERVICE_URL='http://localhost:9007'; CORS_ALLOWED_ORIGINS='http://localhost:3000'
  SERVER_ADDRESS='127.0.0.1'; SPRING_PROFILES_ACTIVE='local'
}
foreach ($key in $urls.Keys) { [Environment]::SetEnvironmentVariable($key, $urls[$key], 'Process') }
$started = [System.Collections.Generic.List[object]]::new()
try {
  foreach ($svc in $services) {
    [Environment]::SetEnvironmentVariable('SERVER_PORT', [string]$svc.Port, 'Process')
    $module = Join-Path $root "backend/$($svc.Module)"
    if (-not $SkipBuild) {
      & "$module/mvnw.cmd" -q -DskipTests package
      if ($LASTEXITCODE -ne 0) { throw "Build failed for $($svc.Name)." }
    }
    $jar = Get-ChildItem "$module/target/*.jar" |
      Where-Object { $_.Name -notmatch 'plain|original' } | Select-Object -First 1
    if (-not $jar) { throw "Executable jar missing for $($svc.Name)." }
    $out = Join-Path $logDir "$($svc.Name).out.log"
    $err = Join-Path $logDir "$($svc.Name).err.log"
    $proc = Start-Process java -PassThru -WorkingDirectory $root `
      -ArgumentList @("-Dpcc.local.owner=PCC-local", '-jar', "`"$($jar.FullName)`"", '--spring.profiles.active=local') `
      -RedirectStandardOutput $out -RedirectStandardError $err
    $started.Add([pscustomobject]@{ Name=$svc.Name; Pid=$proc.Id; Jar=$jar.FullName; Port=$svc.Port })
    $ready = $false
    for ($i=0; $i -lt 90; $i++) {
      Start-Sleep 2
      try {
        $r = Invoke-RestMethod $svc.Url -TimeoutSec 2
        if ($r.status -eq 'UP') { $ready = $true; break }
      } catch {}
      if (-not (Get-Process -Id $proc.Id -ErrorAction SilentlyContinue)) { break }
    }
    if (-not $ready) { throw "$($svc.Name) health check failed; see its ignored log in .local/logs." }
    $started | ConvertTo-Json | Set-Content $pidFile
  }

  $frontendPort = 3000
  if (Get-NetTCPConnection -State Listen -LocalPort $frontendPort -ErrorAction SilentlyContinue) {
    throw "Port $frontendPort is already occupied."
  }
  $vite = Join-Path $root 'frontend/node_modules/vite/bin/vite.js'
  if (-not (Test-Path $vite)) { throw 'Frontend dependencies missing; run npm ci in frontend first.' }
  $node = (Get-Command node).Source
  $front = Start-Process $node -PassThru -WorkingDirectory (Join-Path $root 'frontend') `
    -ArgumentList @($vite, '--host', '127.0.0.1', '--port', '3000', '--strictPort') `
    -RedirectStandardOutput (Join-Path $logDir 'frontend.out.log') `
    -RedirectStandardError (Join-Path $logDir 'frontend.err.log')
  $started.Add([pscustomobject]@{ Name='frontend'; Pid=$front.Id; Jar=$vite; Port=3000 })
  $started | ConvertTo-Json | Set-Content $pidFile
  for ($i=0; $i -lt 30; $i++) {
    try { Invoke-WebRequest http://localhost:3000 -TimeoutSec 2 | Out-Null; break } catch { Start-Sleep 1 }
  }
  if (-not (Get-NetTCPConnection -State Listen -LocalPort 3000 -ErrorAction SilentlyContinue)) {
    throw 'Frontend did not become ready.'
  }
  Write-Host 'PCC local stack is ready:'
  Write-Host 'Gateway   http://localhost:9000'
  Write-Host 'Property  http://localhost:9001'
  Write-Host 'Identity  http://localhost:9002'
  Write-Host 'Land      http://localhost:9003'
  Write-Host 'Finance   http://localhost:9004'
  Write-Host 'Document  http://localhost:9005'
  Write-Host 'Workflow  http://localhost:9006'
  Write-Host 'Reporting http://localhost:9007'
  Write-Host 'Frontend  http://localhost:3000'
} catch {
  $started | ConvertTo-Json | Set-Content $pidFile
  & (Join-Path $PSScriptRoot 'stop-local.ps1')
  throw
}
