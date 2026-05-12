$ErrorActionPreference = "Stop"

$ErrorActionPreference = "Stop"

$ProjectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$Ojdbc = Join-Path $ProjectRoot "..\lib\ojdbc11.jar"

if (-not (Test-Path $Ojdbc)) {
    throw "No se encontró el jar de Oracle en lib\ojdbc11.jar. Copia ojdbc11.jar a aa1-web\lib o define GESTIONDB_OJDBC."
}

$RootDir = Join-Path $ProjectRoot ".."
Push-Location $RootDir
try {
    Write-Host "Compilando TestConexion.java..."
    javac -cp "$Ojdbc" -d java-test java-test\TestConexion.java
    Write-Host "Ejecutando TestConexion..."
    java -cp "java-test;$Ojdbc" TestConexion
}
finally {
    Pop-Location
}
