param([switch]$Director)
$ErrorActionPreference='Stop'
$projectPath=Split-Path -Parent $PSScriptRoot
$env:DUSKRAIN_VISUALS='true'
$env:DUSKRAIN_DIRECTOR=if($Director){'true'}else{'false'}
$env:DUSKRAIN_CLIENT_DIR='run-v14-preview'
$env:DUSKRAIN_USERNAME='DuskRainDirector'
if($Director){[IO.File]::WriteAllText((Join-Path $projectPath 'mod\run-v14-preview\duskrain-director.txt'),'')}
& (Join-Path $PSScriptRoot 'gradle.ps1') runClient --offline
exit $LASTEXITCODE
