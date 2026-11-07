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
  $sources = Get-ChildItem (Join-Path $root 'src\main\java') -Filter '*.java' -Recurse |
      ForEach-Object { $_.FullName }
  & javac --release 21 -encoding UTF-8 -d $classes $sources
  if ($LASTEXITCODE -ne 0) { throw 'Java compilation failed.' }
  [System.IO.File]::WriteAllText((Join-Path $classes 'viktor-version.properties'),
      "version=$version`n", [System.Text.Encoding]::ASCII)
  $jar = Join-Path $inputDir 'viktor-uci.jar'
  & jar --create --file $jar --main-class com.desierto.uci.UciMain -C $classes .
  if ($LASTEXITCODE -ne 0) { throw 'JAR creation failed.' }
  & jpackage --type app-image --name $appName --input $inputDir --main-jar viktor-uci.jar `
      --dest $destinationDir --win-console
  if ($LASTEXITCODE -ne 0) { throw 'UCI application packaging failed.' }
  Write-Host "UCI engine: $(Join-Path $output "$appName.exe")"
} finally {
  if (Test-Path -LiteralPath $stage) {
    Remove-Item -LiteralPath $stage -Recurse -Force
  }
}
