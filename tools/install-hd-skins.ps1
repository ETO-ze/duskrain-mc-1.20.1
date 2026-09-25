param([Parameter(Mandatory=$true)][string]$GameDirectory)
$ErrorActionPreference = 'Stop'
# Client-only. Keep authentication and all player/world data unchanged.
$game = [IO.Path]::GetFullPath($GameDirectory)
if (-not (Test-Path -LiteralPath $game -PathType Container)) { throw 'GameDirectory must be an existing Minecraft instance.' }
$mods = Join-Path $game 'mods'
$data = Join-Path $game 'CustomSkinLoader'
$file = 'CustomSkinLoader_ForgeV2-14.28.jar'
$url = 'https://cdn.modrinth.com/data/idMHQ4n2/versions/Rcbx2QhV/CustomSkinLoader_ForgeV2-14.28.jar'
$expected = '88c70a0b6fd1e8b7d6eb50776d1533bdf5f0c2459c0c473cca71669130fe046ca39985a3f7010c81fb39a3f7fc2f1a34a593a4aed96a1fa80948f7e63f3eec5b'
New-Item -ItemType Directory -Path $mods,$data -Force | Out-Null
$conflicts = @(Get-ChildItem -LiteralPath $mods -File | Where-Object { $_.Name -match '(?i)customskinloader.*\.jar$' -and $_.Name -ne $file })
if ($conflicts.Count) { throw ('Another CustomSkinLoader version exists; preserve and review it first: ' + ($conflicts.Name -join ', ')) }
$destination = Join-Path $mods $file
if (Test-Path -LiteralPath $destination) {
    if ((Get-FileHash -LiteralPath $destination -Algorithm SHA512).Hash -ne $expected) { throw 'Existing mod differs from the official hash; it was preserved.' }
} else {
    $temporary = $destination + '.' + [Guid]::NewGuid().ToString('N') + '.download'
    Invoke-WebRequest -Uri $url -OutFile $temporary
    if ((Get-FileHash -LiteralPath $temporary -Algorithm SHA512).Hash -ne $expected) { throw 'Download hash mismatch; installation stopped.' }
    Move-Item -LiteralPath $temporary -Destination $destination
}
$config = [ordered]@{
    version = '14.28'; buildNumber = 0
    loadlist = @(
        [ordered]@{ name = 'ServerProfile'; type = 'GameProfile' },
        [ordered]@{ name = 'DuskRain'; type = 'MojangAPI'; apiRoot = 'https://skin.duskrain.cn/account/'; sessionRoot = 'https://skin.duskrain.cn/session/' }
    )
    enableDynamicSkull = $true; enableTransparentSkin = $true
    forceLoadAllTextures = $false; enableCape = $true; threadPoolSize = 4
    enableLogStdOut = $false; cacheExpiry = 30; forceUpdateSkull = $false
    enableLocalProfileCache = $false; enableCacheAutoClean = $false; forceDisableCache = $false
} | ConvertTo-Json -Depth 8
$path = Join-Path $data 'CustomSkinLoader.json'
$writeConfig = $true
if (Test-Path -LiteralPath $path) {
    $old = [IO.File]::ReadAllText($path)
    # Runtime updates buildNumber; avoid backing up again for that alone.
    try {
        $normalized = $old | ConvertFrom-Json
        $normalized.buildNumber = 0
        $writeConfig = ($normalized | ConvertTo-Json -Depth 8) -ne $config
    } catch { $writeConfig = $true }
    if ($writeConfig) {
        $backup = Join-Path $data ('backups/' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff'))
        New-Item -ItemType Directory -Path $backup -Force | Out-Null
        Copy-Item -LiteralPath $path -Destination (Join-Path $backup 'CustomSkinLoader.json')
    }
}
if ($writeConfig) {
    $temporary = $path + '.new'
    [IO.File]::WriteAllText($temporary, $config, (New-Object Text.UTF8Encoding($false)))
    Move-Item -LiteralPath $temporary -Destination $path -Force
}
Write-Output ('HD skin/cape support installed and SHA-512 verified: ' + $game)
Write-Output 'Restart Minecraft. HD skins retain their original resolution; 3D Skin Layers extrusion applies to standard 64x64 skins only.'
