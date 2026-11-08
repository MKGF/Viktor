param(
  [string]$Destination = 'target'
)

$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$target = Join-Path $root 'target'
[xml]$pom = Get-Content -LiteralPath (Join-Path $root 'pom.xml') -Raw
$version = [string]$pom.project.version
if ($version -cnotmatch '^[0-9A-Za-z][0-9A-Za-z._-]*$') {
  throw "Invalid project version in pom.xml: $version"
}
$appName = "ViktorUCI-$version"
$destinationDir = if ([System.IO.Path]::IsPathRooted($Destination)) {
  $Destination
} else {
  Join-Path $root $Destination
}
$output = Join-Path $destinationDir $appName
if (Test-Path -LiteralPath $output) {
  throw "The output directory already exists: $output. Move or remove it before rebuilding."
}
New-Item -ItemType Directory -Force -Path $target | Out-Null
New-Item -ItemType Directory -Force -Path $destinationDir | Out-Null
$stage = Join-Path $target ("uci-build-" + [guid]::NewGuid().ToString())
$classes = Join-Path $stage 'classes'
$inputDir = Join-Path $stage 'input'
try {
  New-Item -ItemType Directory -Path $classes, $inputDir -Force | Out-Null
  & mvn -q '-DincludeScope=runtime' "-DoutputDirectory=$inputDir" dependency:copy-dependencies
  if ($LASTEXITCODE -ne 0) { throw 'Could not resolve runtime dependencies with Maven.' }
  $sources = Get-ChildItem (Join-Path $root 'src\main\java') -Filter '*.java' -Recurse |
      ForEach-Object { $_.FullName }
  & javac --release 21 -encoding UTF-8 -cp (Join-Path $inputDir '*') -d $classes $sources
  if ($LASTEXITCODE -ne 0) { throw 'Java compilation failed.' }
  [System.IO.File]::WriteAllText((Join-Path $classes 'viktor-version.properties'),
      "version=$version`n", [System.Text.Encoding]::ASCII)
  $jar = Join-Path $inputDir 'viktor-uci.jar'
  $manifest = Join-Path $stage 'manifest.mf'
  $dependencyNames = Get-ChildItem -LiteralPath $inputDir -Filter '*.jar' | ForEach-Object { $_.Name }
  [System.IO.File]::WriteAllText($manifest,
      "Manifest-Version: 1.0`nClass-Path: $($dependencyNames -join ' ')`n`n",
      [System.Text.Encoding]::ASCII)
  & jar --create --file $jar --manifest $manifest --main-class com.desierto.uci.UciMain -C $classes .
  if ($LASTEXITCODE -ne 0) { throw 'JAR creation failed.' }
  & jpackage --type app-image --name $appName --input $inputDir --main-jar viktor-uci.jar `
      --dest $destinationDir --win-console
  if ($LASTEXITCODE -ne 0) { throw 'UCI application packaging failed.' }
  Copy-Item -LiteralPath (Join-Path $root 'THIRD-PARTY-NOTICES.txt') -Destination $output
  $book = Join-Path $root 'books\openings.bin'
  if (Test-Path -LiteralPath $book -PathType Leaf) {
    $bookDirectory = Join-Path $output 'books'
    New-Item -ItemType Directory -Path $bookDirectory | Out-Null
    Copy-Item -LiteralPath $book -Destination $bookDirectory
  } else {
    Write-Warning "Opening book not found: $book. The packaged engine will search normally."
  }
  $tablebaseDirectory = Join-Path $root 'tablebases'
  $tablebaseFiles = @()
  if (Test-Path -LiteralPath $tablebaseDirectory -PathType Container) {
    $tablebaseFiles = @(Get-ChildItem -LiteralPath $tablebaseDirectory -File |
        Where-Object { $_.Name -cmatch '^K[QRBNP]*vK[QRBNP]*\.rtb[zw]$' -and
            ($_.BaseName -creplace 'v', '').Length -le 4 })
  }
  if ($tablebaseFiles.Count -gt 0) {
    $packagedTables = Join-Path $output 'tablebases'
    New-Item -ItemType Directory -Path $packagedTables | Out-Null
    foreach ($table in $tablebaseFiles) {
      Copy-Item -LiteralPath $table.FullName -Destination $packagedTables
    }
  }
  if ($tablebaseFiles.Count -ne 70) {
    Write-Warning "Found $($tablebaseFiles.Count) of the 70 three- and four-piece tablebase files; the packaged engine requires the complete set."
  }
  Write-Host "UCI engine: $(Join-Path $output "$appName.exe")"
} finally {
  if (Test-Path -LiteralPath $stage) {
    Remove-Item -LiteralPath $stage -Recurse -Force
  }
}
