$ErrorActionPreference = 'Continue'
$checks = @(
  @{ Name='Gateway'; Port=9000; Url='http://localhost:9000/actuator/health' },
  @{ Name='Property'; Port=9001; Url='http://localhost:9001/actuator/health' },
  @{ Name='Identity'; Port=9002; Url='http://localhost:9002/actuator/health' },
  @{ Name='Land'; Port=9003; Url='http://localhost:9003/actuator/health' },
  @{ Name='Finance'; Port=9004; Url='http://localhost:9004/actuator/health' },
  @{ Name='Document'; Port=9005; Url='http://localhost:9005/actuator/health' },
  @{ Name='Workflow'; Port=9006; Url='http://localhost:9006/actuator/health' },
  @{ Name='Reporting'; Port=9007; Url='http://localhost:9007/actuator/health' },
  @{ Name='Frontend'; Port=3000; Url='http://localhost:3000/' }
)
$failed = $false
foreach ($check in $checks) {
  try {
    $response = Invoke-WebRequest $check.Url -TimeoutSec 5
    $state = if ($check.Name -eq 'Frontend') { 'HTTP ' + [int]$response.StatusCode }
             else { (Invoke-RestMethod $check.Url -TimeoutSec 5).status }
    Write-Host ("{0,-10} localhost:{1} {2}" -f $check.Name, $check.Port, $state)
    if ($check.Name -ne 'Frontend' -and $state -ne 'UP') { $failed = $true }
  } catch {
    Write-Host ("{0,-10} localhost:{1} DOWN" -f $check.Name, $check.Port)
    $failed = $true
  }
}
if ($failed) { exit 1 }
