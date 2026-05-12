$ErrorActionPreference = "Stop"

$ProjectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$LibDir = Join-Path $ProjectRoot "..\lib"

if (-not (Test-Path $LibDir)) {
    throw "No se encontro la carpeta lib con los jars de Oracle."
}

$JarFiles = Get-ChildItem $LibDir -Filter *.jar | Where-Object { $_.Name -notmatch "Javadoc" }
if (-not $JarFiles) {
    throw "No encontre jars de ejecucion en lib."
}

$CompileJar = Join-Path $LibDir "ojdbc11.jar"
if (-not (Test-Path $CompileJar)) {
    $CompileJar = ($JarFiles | Select-Object -First 1).FullName
}

$RootDir = Join-Path $ProjectRoot ".."
Push-Location $RootDir
try {
    $RuntimeClasspath = ($JarFiles | ForEach-Object { $_.FullName }) -join ";"
    Write-Host "Compilando TestConexion.java..."
    javac -cp $CompileJar -d java-test java-test\TestConexion.java
    Write-Host "Ejecutando TestConexion..."
    java -cp "java-test;$RuntimeClasspath" TestConexion
}
finally {
    Pop-Location
}
