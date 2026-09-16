param([switch]$Visuals,[switch]$NoShaders,[switch]$Director)
$ErrorActionPreference='Stop'
$env:DUSKRAIN_DIRECTOR=if($Director){'true'}else{'false'}
$env:DUSKRAIN_VISUALS=if($NoShaders){'false'}else{'true'}
& (Join-Path $PSScriptRoot 'gradle.ps1') runClient --offline
exit $LASTEXITCODE
