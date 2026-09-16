param([string]$TestDirectory='run-siteqa-v11',[switch]$Preview,[switch]$Release)
$ErrorActionPreference='Stop'
$root=(Resolve-Path -LiteralPath (Split-Path -Parent $PSScriptRoot)).Path
$testPath=Join-Path $root ('mod\'+$TestDirectory)
$source=Join-Path $testPath 'world\dimensions\duskrain\city'
foreach($name in @('duskrain-walk-audit.json','duskrain-site-audit.json','duskrain-route-network.json','duskrain-fixture-audit.json')){
    $r=Get-Content -LiteralPath (Join-Path $testPath $name) -Raw -Encoding UTF8 | ConvertFrom-Json
    if(-not $r.passed){throw ('Failed audit: '+$name)}
}
if($Preview -and (Get-CimInstance Win32_Process -Filter "Name='java.exe'" | Where-Object {$_.CommandLine -match 'forgeclientuserdev'})){throw 'Close the preview client normally before replacing its city.'}
function FileHash([string]$path){$algo=[Security.Cryptography.SHA256]::Create();try{return [BitConverter]::ToString($algo.ComputeHash([IO.File]::ReadAllBytes($path))).Replace('-','')}finally{$algo.Dispose()}}
function OtherFiles([string]$world){$table=@{};foreach($f in Get-ChildItem -LiteralPath $world -Recurse -File){$rel=$f.FullName.Substring($world.Length+1);if(-not $rel.StartsWith('dimensions\duskrain\city\')){$table[$rel]=FileHash $f.FullName}};return $table}
$stamp=[DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds().ToString();$results=@()
$targets=@();if($Preview){$targets+=@{name='preview';world=(Join-Path $root 'mod\run\saves\DuskRainRemake')}};if($Release){$targets+=@{name='release';world=(Join-Path $root 'dist\DuskRain-2.0.0-preview\server\world')}}
foreach($t in $targets){
    $destination=Join-Path $t.world 'dimensions\duskrain\city';$backup=Join-Path $root ('backups\city-'+$stamp+'-'+$t.name)
    foreach($path in @($source,$destination,$backup)){if(-not [IO.Path]::GetFullPath($path).StartsWith($root+'\')){throw 'City path is outside project'}}
    $before=OtherFiles $t.world
    if(Test-Path -LiteralPath $destination){Move-Item -LiteralPath $destination -Destination $backup}
    Copy-Item -LiteralPath $source -Destination $destination -Recurse
    $after=OtherFiles $t.world
    if($before.Count -ne $after.Count){throw 'Unexpected change outside city'}
    foreach($key in $before.Keys){if($before[$key] -ne $after[$key]){throw ('Changed outside city: '+$key)}}
    $results+=@{target=$t.name;source=$source;backup=$backup;outsideCityFiles=$after.Count;outsideCityUnchanged=$true}
}
$results | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath (Join-Path $root ('docs\qa\city-install-'+$TestDirectory+'.json')) -Encoding UTF8
Write-Output ($results | ConvertTo-Json -Compress)
