$ErrorActionPreference = "Stop"

$ProjectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$Ojdbc = $env:GESTIONDB_OJDBC

if ([string]::IsNullOrWhiteSpace($Ojdbc)) {
    $Candidates = @(
        "C:\app\dg359\product\23ai\dbhomeFree\jdbc\lib\ojdbc17.jar",
        "C:\app\dg359\product\23ai\dbhomeFree\jdbc\lib\ojdbc11.jar",
        "C:\app\dg359\product\23ai\dbhomeFree\jdbc\lib\ojdbc8.jar"
    )
    $Ojdbc = $Candidates | Where-Object { Test-Path $_ } | Select-Object -First 1
}

if ([string]::IsNullOrWhiteSpace($Ojdbc) -or -not (Test-Path $Ojdbc)) {
    $LocalOjdbc = Join-Path $ProjectRoot "lib\ojdbc11.jar"
    if (Test-Path $LocalOjdbc) {
        $Ojdbc = $LocalOjdbc
    }
}

if ([string]::IsNullOrWhiteSpace($Ojdbc) -or -not (Test-Path $Ojdbc)) {
    throw "No encontre ojdbc. Define GESTIONDB_OJDBC con la ruta del jar ojdbc o coloca ojdbc11.jar en lib."
}

$OutDir = Join-Path $ProjectRoot "server-out"
if (-not (Test-Path $OutDir)) {
    New-Item -ItemType Directory -Path $OutDir | Out-Null
}

Push-Location $ProjectRoot
try {
    javac -cp $Ojdbc -d $OutDir .\backend\*.java
    java -cp "$OutDir;$Ojdbc" GestionDBServer
}
finally {
    Pop-Location
}
