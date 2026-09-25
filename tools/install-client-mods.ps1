param([string]$GameDirectory=$PSScriptRoot, [switch]$Sound, [switch]$SkinLayers, [switch]$Shaders)
$ErrorActionPreference='Stop'
[Net.ServicePointManager]::SecurityProtocol=[Net.SecurityProtocolType]::Tls12
$game=[IO.Path]::GetFullPath($GameDirectory)
if (-not (Test-Path -LiteralPath $game -PathType Container)) { throw 'Choose an existing, stopped Minecraft instance.' }
$lock=Get-Content -LiteralPath (Join-Path $PSScriptRoot 'client-mods.lock.json') -Raw -Encoding UTF8 | ConvertFrom-Json
$mods=Join-Path $game 'mods'
New-Item -ItemType Directory -Path $mods -Force | Out-Null
$items=@($lock.entries)
$ownName='duskrain-'+$lock.version+'.jar'
$ownSource=Join-Path $PSScriptRoot ('mods/'+$ownName)
$checksums=Get-Content -LiteralPath (Join-Path $PSScriptRoot 'checksums.json') -Raw -Encoding UTF8 | ConvertFrom-Json
$ownHash=$checksums.('mods/'+$ownName)
if (-not $ownHash -or (Get-FileHash -LiteralPath $ownSource -Algorithm SHA256).Hash -ne $ownHash) { throw 'DuskRain package is incomplete or modified' }
foreach ($item in $lock.optional) {
    if (($Sound -and $item.project -eq 'sound-physics-remastered') -or ($SkinLayers -and $item.project -eq '3dskinlayers') -or ($Shaders -and $item.kind -eq 'shader')) { $items += $item }
}
Add-Type -AssemblyName System.IO.Compression.FileSystem
$existingIds=@{}
foreach ($file in Get-ChildItem -LiteralPath $mods -File -Filter '*.jar') {
    $jar=[IO.Compression.ZipFile]::OpenRead($file.FullName)
    try {
        $entry=$jar.GetEntry('META-INF/mods.toml')
        if ($entry) {
            $reader=New-Object IO.StreamReader($entry.Open())
            try { $toml=$reader.ReadToEnd() } finally { $reader.Dispose() }
            # Dependency sections also have modId: inspect only [[mods]] blocks.
            foreach ($block in [regex]::Matches($toml,'(?ms)^\s*\[\[mods\]\](.*?)(?=^\s*\[|\z)')) {
                $id=[regex]::Match($block.Groups[1].Value,'(?m)^\s*modId\s*=\s*["'']([^"'']+)["'']').Groups[1].Value
                if ($id) {
                    if ($existingIds.ContainsKey($id)) { throw ('Duplicate installed mod ID: '+$id) }
                    $existingIds[$id]=$file.Name
                }
            }
        }
    } finally { $jar.Dispose() }
}
$staged=@()
if ($existingIds.ContainsKey('duskrain') -and $existingIds['duskrain'] -ne $ownName) { throw ('Preserved old DuskRain: '+$existingIds['duskrain']+'. Move it out of mods before retrying.') }
$ownTarget=Join-Path $mods $ownName
if ((Test-Path -LiteralPath $ownTarget) -and (Get-FileHash -LiteralPath $ownTarget -Algorithm SHA256).Hash -ne $ownHash) { throw 'Installed DuskRain has a different hash; preserved' }
# Preflight all conflicts before downloading or installing anything.
foreach ($item in $items) {
    if (@($item.sides) -notcontains 'client') { throw ('Wrong side: '+$item.file) }
    if ([IO.Path]::GetFileName($item.file) -ne $item.file -or $item.file -match '[:/\\]') { throw 'Unsafe filename' }
    if (([uri]$item.url).Scheme -ne 'https' -or ([uri]$item.url).Host -ne 'cdn.modrinth.com') { throw 'Expected official Modrinth CDN' }
    foreach ($id in $item.mod_ids) {
        if ($existingIds.ContainsKey($id) -and $existingIds[$id] -ne $item.file) { throw ('Preserved conflicting version: '+$existingIds[$id]+'. Move it out of mods before retrying.') }
    }
    $folder=if($item.kind -eq 'shader'){Join-Path $game 'shaderpacks'}else{$mods}
    $target=Join-Path $folder $item.file
    if (Test-Path -LiteralPath $target) {
        if ((Get-FileHash -LiteralPath $target -Algorithm SHA512).Hash -ne $item.sha512 -or (Get-FileHash -LiteralPath $target -Algorithm SHA256).Hash -ne $item.sha256) { throw ('Existing file hash mismatch; preserved '+$target) }
    }
}
foreach ($item in $items) {
    $folder=if($item.kind -eq 'shader'){Join-Path $game 'shaderpacks'}else{$mods}
    New-Item -ItemType Directory -Path $folder -Force | Out-Null
    $target=Join-Path $folder $item.file
    if (Test-Path -LiteralPath $target) { continue }
    $tmp=Join-Path $folder ($item.file+'.'+[guid]::NewGuid().ToString('N')+'.download')
    Write-Output ('Downloading '+$item.project+' '+$item.version)
    Invoke-WebRequest -UseBasicParsing -Uri $item.url -OutFile $tmp
    if ((Get-FileHash -LiteralPath $tmp -Algorithm SHA512).Hash -ne $item.sha512 -or (Get-FileHash -LiteralPath $tmp -Algorithm SHA256).Hash -ne $item.sha256 -or (Get-Item -LiteralPath $tmp).Length -ne $item.size) { throw ('Download failed validation: '+$item.file) }
    $staged += [pscustomobject]@{Temp=$tmp;Target=$target}
}
# Promote only after every download passed; failed .download files cannot be loaded by Forge.
foreach ($file in $staged) { Move-Item -LiteralPath $file.Temp -Destination $file.Target }
if (-not (Test-Path -LiteralPath $ownTarget)) { Copy-Item -LiteralPath $ownSource -Destination $ownTarget }
$defaults=Join-Path $PSScriptRoot 'defaults'
if (Test-Path -LiteralPath $defaults) {
    foreach ($preset in Get-ChildItem -LiteralPath $defaults -Recurse -File) {
        $relative=$preset.FullName.Substring($defaults.Length).TrimStart('\','/')
        $target=Join-Path $game $relative
        if (-not (Test-Path -LiteralPath $target)) {
            New-Item -ItemType Directory -Path (Split-Path -Parent $target) -Force | Out-Null
            Copy-Item -LiteralPath $preset.FullName -Destination $target
        }
    }
}
Write-Output 'Official client mods verified. Use Java 17 / Minecraft 1.20.1 / Forge 47.4.10.'
Write-Output 'External login: https://skin.duskrain.cn/authlib-injector'
Write-Output 'Server: nbc.rainplay.cn:42741'
