$ErrorActionPreference = "Stop"

$javaHome = if ($env:JAVA_HOME) { $env:JAVA_HOME } else { "" }
$javac = if ($javaHome) { Join-Path $javaHome "bin\javac.exe" } else { "javac" }
$jar = if ($javaHome) { Join-Path $javaHome "bin\jar.exe" } else { "jar" }

New-Item -ItemType Directory -Force -Path "out\classes", "build" | Out-Null
$sources = Get-ChildItem -Recurse -Filter "*.java" -Path "src\main\java" | ForEach-Object { $_.FullName }
[System.IO.File]::WriteAllLines((Resolve-Path "out").Path + "\sources.txt", $sources, [System.Text.UTF8Encoding]::new($false))
& $javac -encoding UTF-8 -d "out\classes" "@out\sources.txt"

$manifest = "out\MANIFEST.MF"
Set-Content -Path $manifest -Encoding ASCII -Value @(
    "Manifest-Version: 1.0"
    "Main-Class: ua.codex.repaircalc.Main"
    ""
)
& $jar --create --file "build\RenovaCalc.jar" --manifest $manifest -C "out\classes" .
Write-Host "Created build\RenovaCalc.jar"
