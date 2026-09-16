param([Parameter(Mandatory=$true)][string]$ModsDirectory)
$ErrorActionPreference='Stop'
$file='skinlayers3d-forge-1.11.2-mc1.20.1.jar'
$url='https://cdn.modrinth.com/data/zV5r3pPn/versions/4oWvC9eo/skinlayers3d-forge-1.11.2-mc1.20.1.jar'
$expected='87F06A52665D5EEC7CF7D9DCD73A94BA9AA2D0F1651578D3852AFAB128E17836CFF76E40AA5661915BA106910CEF0104A00EF04883977AF74DE1ACC02BCECC1D'
New-Item -ItemType Directory -Path $ModsDirectory -Force | Out-Null
$destination=Join-Path ([IO.Path]::GetFullPath($ModsDirectory)) $file
if(Test-Path -LiteralPath $destination){if((Get-FileHash -LiteralPath $destination -Algorithm SHA512).Hash -eq $expected){Write-Output '3D Skin Layers 1.11.2 already verified.';exit 0}else{throw 'An existing file has a different hash; preserve it and inspect before installing.'}}
$temporary=$destination+'.download'
Invoke-WebRequest -Uri $url -OutFile $temporary
if((Get-FileHash -LiteralPath $temporary -Algorithm SHA512).Hash -ne $expected){throw 'Official artifact hash mismatch; installation stopped.'}
Move-Item -LiteralPath $temporary -Destination $destination
Write-Output ('Installed and SHA-512 verified: '+$destination)
