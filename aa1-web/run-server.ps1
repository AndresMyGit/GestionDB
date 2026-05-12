$ErrorActionPreference = "Stop"

$ProjectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$LibDir = Join-Path $ProjectRoot "lib"

if (-not (Test-Path $LibDir)) {
    throw "No encontre la carpeta lib con los jars de Oracle."
}

$JarFiles = Get-ChildItem $LibDir -Filter *.jar | Where-Object { $_.Name -notmatch "Javadoc" }
if (-not $JarFiles) {
    throw "No encontre jars de ejecucion en lib."
}

$CompileJar = Join-Path $LibDir "ojdbc11.jar"
if (-not (Test-Path $CompileJar)) {
    $CompileJar = ($JarFiles | Select-Object -First 1).FullName
}

$OutDir = Join-Path $ProjectRoot "server-out"
if (-not (Test-Path $OutDir)) {
    New-Item -ItemType Directory -Path $OutDir | Out-Null
}

Push-Location $ProjectRoot
try {
    $RuntimeClasspath = ($JarFiles | ForEach-Object { $_.FullName }) -join ";"
    javac -cp $CompileJar -d $OutDir .\backend\*.java
    if (-not $env:PORT -and -not $env:GESTIONDB_PORT) {
        $env:GESTIONDB_PORT = "8081"
    }
    java -cp "$OutDir;$RuntimeClasspath" GestionDBServer
}
finally {
    Pop-Location
}
