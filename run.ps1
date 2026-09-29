# SentinelX - one-stop local launcher (no Docker / no PostgreSQL required)
# Uses Spring's embedded H2 in-memory database for the backend.
#
# Usage:
#   powershell -ExecutionPolicy Bypass -File run.ps1            # start all services
#   powershell -ExecutionPolicy Bypass -File run.ps1 -Build     # rebuild backend jar first
#   powershell -ExecutionPolicy Bypass -File run.ps1 -Stop      # stop all SentinelX services
#
# Defaults to the maven under TEMP\opencode\maven; or use MAVEN_HOME if set.
param(
    [switch]$Build,
    [switch]$Stop
)

$ErrorActionPreference = 'SilentlyContinue'
$root = $PSScriptRoot
$tmp  = "$env:LOCALAPPDATA\Temp\opencode"
New-Item -ItemType Directory -Path $tmp -Force | Out-Null

# --- resolve maven ---
$mvn = "$tmp\maven\apache-maven-3.9.9\bin\mvn.cmd"
if (-not (Test-Path $mvn)) {
    Write-Host "[sentinelx] Maven not found under $mvn - downloading..." -ForegroundColor Yellow
    Invoke-WebRequest -Uri "https://archive.apache.org/dist/maven/maven-3/3.9.9/binaries/apache-maven-3.9.9-bin.zip" `
        -OutFile "$tmp\maven.zip" -UseBasicParsing -TimeoutSec 180
    Expand-Archive -Path "$tmp\maven.zip" -DestinationPath "$tmp\maven" -Force
}
Write-Host "[sentinelx] Using Maven: $mvn" -ForegroundColor Green

function Log($name) { return "$tmp\$name.log" }
function Err($name) { return "$tmp\$name.err.log" }

# --- stop mode ---
if ($Stop) {
    Write-Host "[sentinelx] Stopping services on ports 8000, 8080, 5173..." -ForegroundColor Yellow
    foreach ($port in 8000, 8080, 5173) {
        netstat -ano | Select-String ":$port\s" | ForEach-Object {
            if ($_ -match "\s(\d+)\s*$") {
                $pidToKill = $Matches[1]
                Stop-Process -Id $pidToKill -Force -ErrorAction SilentlyContinue
            }
        }
    }
    Write-Host "[sentinelx] Stopped." -ForegroundColor Green
    exit
}

# --- build ---
if ($Build -or -not (Test-Path "$root\backend\target\sentinelx-backend-1.0.0.jar")) {
    Write-Host "[sentinelx] Building backend jar..." -ForegroundColor Yellow
    Push-Location "$root\backend"
    & $mvn -q -DskipTests package
    $code = $LASTEXITCODE
    Pop-Location
    if ($code -ne 0) { Write-Host "[sentinelx] Maven build failed (exit $code)." -ForegroundColor Red; exit 1 }
    Write-Host "[sentinelx] Backend built." -ForegroundColor Green
}

# --- ml-service :8000 ---
Write-Host "[sentinelx] Starting ML service on :8000..." -ForegroundColor Cyan
Start-Process python -ArgumentList "-m","uvicorn","main:app","--host","0.0.0.0","--port","8000" `
    -WorkingDirectory "$root\ml-service" -WindowStyle Hidden `
    -RedirectStandardOutput (Log "ml") -RedirectStandardError (Err "ml")

# --- backend :8080 (H2 profile -> no PostgreSQL needed) ---
Write-Host "[sentinelx] Starting backend on :8080 (H2 in-memory)..." -ForegroundColor Cyan
$jar = "$root\backend\target\sentinelx-backend-1.0.0.jar"
Start-Process java -ArgumentList "-jar", "`"$jar`"", "--spring.profiles.active=h2" `
    -WorkingDirectory "$root\backend" -WindowStyle Hidden `
    -RedirectStandardOutput (Log "backend") -RedirectStandardError (Err "backend")

# --- frontend :5173 ---
Write-Host "[sentinelx] Starting frontend on :5173..." -ForegroundColor Cyan
Push-Location "$root\frontend"
if (-not (Test-Path "node_modules")) { & npm.cmd install }
$fe = Start-Process "C:\Program Files\nodejs\npm.cmd" -ArgumentList "run","dev" `
    -WorkingDirectory "$root\frontend" -WindowStyle Hidden `
    -RedirectStandardOutput (Log "frontend") -RedirectStandardError (Err "frontend")
Pop-Location

Write-Host ""
Write-Host "===========================================================" -ForegroundColor Green
Write-Host " SentinelX is starting. Give it ~10-15s to be ready."          -ForegroundColor Green
Write-Host "   Dashboard : http://localhost:5173  (admin/Admin@123)"       -ForegroundColor Green
Write-Host "   API       : http://localhost:8080"                           -ForegroundColor Green
Write-Host "   ML health : http://localhost:8000/health"                    -ForegroundColor Green
Write-Host " Logs: $tmp\{ml|backend|frontend}.log"                         -ForegroundColor Green
Write-Host "===========================================================" -ForegroundColor Green
