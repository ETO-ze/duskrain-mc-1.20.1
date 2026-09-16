$ErrorActionPreference='Stop'
function Get-Sha1([string]$Path) {
    $algorithm=[Security.Cryptography.SHA1]::Create()
    $stream=[IO.File]::OpenRead($Path)
    try { return ([BitConverter]::ToString($algorithm.ComputeHash($stream))).Replace('-','').ToLowerInvariant() }
    finally { $stream.Dispose(); $algorithm.Dispose() }
}
Set-Location -LiteralPath $PSScriptRoot
$libraryRoot=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot 'server\libraries'))
New-Item -ItemType Directory -Path $libraryRoot -Force | Out-Null
$downloads=Get-Content -LiteralPath 'server-downloads.json' -Raw | ConvertFrom-Json
foreach($entry in $downloads) {
    $target=[IO.Path]::GetFullPath((Join-Path $libraryRoot $entry.path))
    if(-not $target.StartsWith($libraryRoot+'\',[StringComparison]::OrdinalIgnoreCase)){throw 'Invalid library destination.'}
    if($entry.sha1 -notmatch '^[0-9a-f]{40}$'){throw 'Missing publisher hash.'}
    if((Test-Path -LiteralPath $target) -and (Get-Sha1 $target) -eq $entry.sha1){continue}
    New-Item -ItemType Directory -Path (Split-Path -Parent $target) -Force | Out-Null
    Write-Output ('Downloading '+$entry.path)
    & curl.exe -fL --retry 2 --connect-timeout 20 --max-time 180 -A 'Mozilla/5.0' $entry.url -o ($target+'.download')
    if($LASTEXITCODE -ne 0){throw ('Download failed: '+$entry.path)}
    if((Get-Sha1 ($target+'.download')) -ne $entry.sha1){throw ('Hash mismatch: '+$entry.path)}
    Move-Item -LiteralPath ($target+'.download') -Destination $target -Force
}
$installer=Join-Path $PSScriptRoot 'forge-1.20.1-47.4.10-installer.jar'
if((Get-Sha1 $installer) -ne '66bfea9963bfa60d88bab6b2750e74a958392715'){throw 'Forge installer hash mismatch.'}
& '.\runtime\java17\bin\java.exe' -jar $installer --offline --installServer (Join-Path $PSScriptRoot 'server')
if($LASTEXITCODE -ne 0){throw 'Forge installation did not finish.'}
Write-Output 'Forge 47.4.10 server is ready. Run server/start-server.bat.'
