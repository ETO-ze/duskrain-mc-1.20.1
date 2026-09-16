param([switch]$PackOnly,[switch]$CheckHost)
$ErrorActionPreference='Stop'
Set-Location -LiteralPath $PSScriptRoot
$java=Join-Path $PSScriptRoot '..\runtime\java17\bin\java.exe'
$settings=Get-Content -LiteralPath 'resourcepack-host.json' -Raw -Encoding UTF8 | ConvertFrom-Json
$pack=Join-Path $PSScriptRoot 'resourcepacks\DuskRain-Jade-City.zip'
$probe=[Net.Sockets.TcpListener]::new([Net.IPAddress]::Parse($settings.bindAddress),[int]$settings.port)
try{$probe.Start()}finally{$probe.Stop()}
$sha=[Security.Cryptography.SHA1]::Create()
try{$hash=([BitConverter]::ToString($sha.ComputeHash([IO.File]::ReadAllBytes($pack)))).Replace('-','').ToLowerInvariant()}finally{$sha.Dispose()}
$publicUrl=$settings.publicBaseUrl.TrimEnd('/')+'/DuskRain-Jade-City.zip'
if(-not [Uri]::IsWellFormedUriString($publicUrl,[UriKind]::Absolute) -or $publicUrl -notmatch '^https?://'){throw 'publicBaseUrl must be a reachable HTTP or HTTPS base URL.'}
$lines=@(Get-Content -LiteralPath 'server.properties' -Encoding UTF8)
$updated=@($lines | Where-Object {$_ -notmatch '^(resource-pack|resource-pack-sha1|resource-pack-prompt)='})
$updated+=@(('resource-pack='+$publicUrl),('resource-pack-sha1='+$hash),'resource-pack-prompt={"text":"DuskRain Jade City - original textures"}')
if(($updated -join "`n") -ne ($lines -join "`n")){
    $backup='server.properties.'+[DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()+'.bak'
    Copy-Item -LiteralPath 'server.properties' -Destination $backup
    [IO.File]::WriteAllLines((Join-Path $PSScriptRoot 'server.properties'),[string[]]$updated,[Text.UTF8Encoding]::new($false))
}
$serverArgs=@('--add-modules','jdk.httpserver',('"'+(Join-Path $PSScriptRoot 'ResourcePackServer.java')+'"'),('"'+$pack+'"'),$settings.bindAddress,[string]$settings.port)
$helper=Start-Process -FilePath $java -ArgumentList $serverArgs -WindowStyle Hidden -PassThru -RedirectStandardOutput 'resourcepack-host.log' -RedirectStandardError 'resourcepack-host-error.log'
try{
    $ready=$false
    for($attempt=0;$attempt -lt 30;$attempt++){
        if($helper.HasExited){throw 'Resource pack host stopped. Check resourcepack-host-error.log.'}
        try{$response=Invoke-WebRequest -Uri ('http://127.0.0.1:'+$settings.port+'/DuskRain-Jade-City.zip') -Method Head -UseBasicParsing -TimeoutSec 1;if($response.StatusCode -eq 200){$ready=$true;break}}catch{}
        Start-Sleep -Milliseconds 300
    }
    if(-not $ready){throw 'Resource pack host readiness check timed out.'}
    Write-Host ('Resource pack: '+$publicUrl+' SHA1 '+$hash)
    if($CheckHost){Write-Host 'Resource pack host check passed.';return}
    if($PackOnly){Wait-Process -Id $helper.Id;exit 0}
    if(-not (Test-Path -LiteralPath 'libraries\net\minecraftforge\forge\1.20.1-47.4.10\win_args.txt')){throw 'Run setup-server.ps1 before starting the server.'}
    & $java '@user_jvm_args.txt' '@libraries/net/minecraftforge/forge/1.20.1-47.4.10/win_args.txt' nogui
    $serverExit=$LASTEXITCODE
}finally{
    if(-not $helper.HasExited){Stop-Process -Id $helper.Id}
}
exit $serverExit
