$env:DUSKRAIN_CLIENT_DIR='run-v14-qa'
$env:DUSKRAIN_USERNAME='DuskRainDirector'
$env:DUSKRAIN_DIRECTOR='true'
$env:DUSKRAIN_VISUALS='true'
[IO.File]::WriteAllText('H:\playfround\duskrain-mc-1.20.1\mod\run-v14-qa\duskrain-director.txt','')
& 'H:\playfround\duskrain-mc-1.20.1\tools\gradle.ps1' runClient --offline *> 'H:\playfround\duskrain-mc-1.20.1\docs\qa\v14-client.log'
