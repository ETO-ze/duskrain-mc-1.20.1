$env:DUSKRAIN_CLIENT_DIR='run-v14-observer'
$env:DUSKRAIN_USERNAME='DuskRainObserver'
$env:DUSKRAIN_DIRECTOR='true'
$env:DUSKRAIN_VISUALS='true'
$env:DUSKRAIN_JOIN='[::1]:25571'
[IO.File]::WriteAllText('H:\playfround\duskrain-mc-1.20.1\mod\run-v14-observer\duskrain-director.txt','')
& 'H:\playfround\duskrain-mc-1.20.1\tools\gradle.ps1' runClient --offline *> 'H:\playfround\duskrain-mc-1.20.1\docs\qa\v14-observer.log'
