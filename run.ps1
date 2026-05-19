$ErrorActionPreference = "Stop"

$javaHome = if ($env:JAVA_HOME) { $env:JAVA_HOME } else { "" }
$javac = if ($javaHome) { Join-Path $javaHome "bin\javac.exe" } else { "javac" }
$java = if ($javaHome) { Join-Path $javaHome "bin\java.exe" } else { "java" }

New-Item -ItemType Directory -Force -Path "out" | Out-Null
$sources = Get-ChildItem -Recurse -Filter "*.java" -Path "src\main\java" | ForEach-Object { $_.FullName }
[System.IO.File]::WriteAllLines((Resolve-Path "out").Path + "\sources.txt", $sources, [System.Text.UTF8Encoding]::new($false))
& $javac -encoding UTF-8 -d "out\classes" "@out\sources.txt"
& $java -cp "out\classes" ua.codex.repaircalc.Main
