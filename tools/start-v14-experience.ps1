$ErrorActionPreference='Stop'
$projectPath=Split-Path -Parent $PSScriptRoot
& (Join-Path $PSScriptRoot 'start-current-preview.ps1') -Director *> (Join-Path $projectPath 'docs\qa\v14-experience.log')
exit $LASTEXITCODE
