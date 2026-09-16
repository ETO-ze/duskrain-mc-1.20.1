$ErrorActionPreference = 'Stop'
$projectPath = [IO.Path]::GetFullPath((Split-Path -Parent $PSScriptRoot))
$worldPath = Join-Path $projectPath 'mod\run\saves\DuskRainRemake'
$cityPath = Join-Path $worldPath 'dimensions\duskrain\city'
$sourcePath = Join-Path $projectPath 'mod\run-mapqa-v3\world\dimensions\duskrain\city'
$auditPath = Join-Path $projectPath 'mod\run-mapqa-v3\duskrain-walk-audit.json'
if (-not (Get-Content -LiteralPath $auditPath -Raw | ConvertFrom-Json).passed) { throw 'Map audit must pass before installation.' }
foreach ($target in @($worldPath, $cityPath, $sourcePath)) {
    $absolute = [IO.Path]::GetFullPath($target)
    if (-not $absolute.StartsWith($projectPath + '\', [StringComparison]::OrdinalIgnoreCase)) { throw "Path outside project: $absolute" }
    if (-not (Test-Path -LiteralPath $absolute)) { throw "Missing path: $absolute" }
}
$backupPath = Join-Path $projectPath ('backups\detail-preview-' + (Get-Date -Format 'yyyyMMdd-HHmmss'))
$stagePath = Join-Path $worldPath 'dimensions\duskrain\city-detail-staged'
if (Test-Path -LiteralPath $stagePath) { throw 'Staging directory already exists; inspect it before retrying.' }
function Protected-Hashes {
    $hashes = [ordered]@{}
    Get-ChildItem -LiteralPath $worldPath -Recurse -File | Where-Object {
        -not $_.FullName.StartsWith($cityPath + '\', [StringComparison]::OrdinalIgnoreCase) -and
        -not $_.FullName.StartsWith($stagePath + '\', [StringComparison]::OrdinalIgnoreCase) -and $_.Name -ne 'session.lock'
    } | Sort-Object FullName | ForEach-Object { $hashes[$_.FullName.Substring($worldPath.Length)] = (Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash }
    return ($hashes | ConvertTo-Json -Compress)
}
# Hold the world session lock for the complete offline update.
$worldLock = [IO.File]::Open((Join-Path $worldPath 'session.lock'), 'Open', 'ReadWrite', 'None')
try {
    $before = Protected-Hashes
    New-Item -ItemType Directory -Path $backupPath | Out-Null
    Copy-Item -LiteralPath (Join-Path $worldPath 'level.dat') -Destination $backupPath
    foreach ($name in @('playerdata','data')) { $path = Join-Path $worldPath $name; if (Test-Path -LiteralPath $path) { Copy-Item -LiteralPath $path -Destination $backupPath -Recurse } }
    Copy-Item -LiteralPath $sourcePath -Destination $stagePath -Recurse
    # All final move targets are fixed descendants of the verified project and backup.
    $oldCityBackup = [IO.Path]::GetFullPath((Join-Path $backupPath 'city'))
    if (-not $oldCityBackup.StartsWith($projectPath + '\backups\')) { throw 'Unexpected backup target.' }
    Move-Item -LiteralPath $cityPath -Destination $oldCityBackup
    try { Move-Item -LiteralPath $stagePath -Destination $cityPath }
    catch { Move-Item -LiteralPath $oldCityBackup -Destination $cityPath; throw }
    $after = Protected-Hashes
    if ($before -ne $after) { throw 'Files outside the city dimension changed. Inspect before continuing.' }
    $record = [ordered]@{ world=$worldPath; source=$sourcePath; backup=$backupPath; protectedFilesUnchanged=$true; scope='Only duskrain:city replaced; UUID data, survival, claims and demo retained'; installed=(Get-Date -Format o) }
    $record | ConvertTo-Json | Set-Content -Encoding UTF8 -LiteralPath (Join-Path $projectPath 'docs\qa\preview-install.json')
    $record | ConvertTo-Json
} finally { $worldLock.Dispose() }
