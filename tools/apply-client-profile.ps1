param([ValidateSet('rtx2060','rtx4060','rtx5070','fluent','standard','recording','off')][string]$Profile='rtx2060', [string]$GameDirectory=$PSScriptRoot)
$ErrorActionPreference='Stop'
$aliases=@{fluent='rtx2060';standard='rtx4060';recording='rtx5070'}
if($aliases.ContainsKey($Profile)){$Profile=$aliases[$Profile]}
$target=[IO.Path]::GetFullPath($GameDirectory)
if(-not (Test-Path -LiteralPath $target -PathType Container)){throw 'Use an existing Minecraft instance directory.'}
$source=Join-Path $PSScriptRoot ('profiles\'+$(if($Profile -eq 'off'){'rtx2060'}else{$Profile}))
if(-not (Test-Path -LiteralPath $source)){throw 'The profiles folder must be beside this script.'}
$shader='ComplementaryReimagined_r5.3.zip'
if($Profile -ne 'off' -and -not (Test-Path -LiteralPath (Join-Path $target ('shaderpacks\'+$shader)))){throw 'Copy the bundled shaderpacks folder into the instance first.'}
$stamp=([DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()).ToString()
$backup=Join-Path $target ('duskrain-visual-backups\'+$stamp)
$files=@('options.txt','config\oculus.properties',('shaderpacks\'+$shader+'.txt'),'config\duskrain-visual.json')
$present=@()
foreach($relative in $files){
    $file=Join-Path $target $relative
    if(Test-Path -LiteralPath $file){$copy=Join-Path $backup $relative;New-Item -ItemType Directory -Path (Split-Path -Parent $copy) -Force | Out-Null;Copy-Item -LiteralPath $file -Destination $copy;$present+=$relative.Replace('\','/')}
}
New-Item -ItemType Directory -Path $backup -Force | Out-Null
[IO.File]::WriteAllLines((Join-Path $backup 'present.txt'),[string[]]$present,[Text.UTF8Encoding]::new($false))
foreach($relative in $files){
    $file=Join-Path $target $relative
    New-Item -ItemType Directory -Path (Split-Path -Parent $file) -Force | Out-Null
    if($relative -eq 'config\duskrain-visual.json'){$state=@{profile=$Profile;pack=$shader}|ConvertTo-Json -Compress;[IO.File]::WriteAllText($file,$state,[Text.UTF8Encoding]::new($false));continue}
    if($Profile -eq 'off' -and $relative -ne 'config\oculus.properties'){continue}
    $current=if(Test-Path -LiteralPath $file){@(Get-Content -LiteralPath $file -Encoding utf8)}else{@()}
    $separator=if($relative -eq 'options.txt'){':'}else{'='}
    $lines=if($Profile -eq 'off'){@('enableShaders=false')}else{@(Get-Content -LiteralPath (Join-Path $source $relative) -Encoding utf8)}
    foreach($line in $lines){
        if(-not $line.Contains($separator)){continue}
        $key=$line.Split($separator)[0]
        $current=@($current | Where-Object { -not $_.StartsWith($key+$separator) })+$line
    }
    [IO.File]::WriteAllLines($file,[string[]]$current,[Text.UTF8Encoding]::new($false))
}
[IO.File]::WriteAllText((Join-Path $target 'config\duskrain-visual-backup.txt'),$stamp,[Text.UTF8Encoding]::new($false))
Write-Output "Applied $Profile. Backup: $backup. Run this script with Minecraft closed; in-game use G > Display Settings."
