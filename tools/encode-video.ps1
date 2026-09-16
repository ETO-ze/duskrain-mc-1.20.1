param([Parameter(Mandatory=$true)][string]$RecordingDirectory,[string]$Output='DuskRain-construction.mp4')
$ErrorActionPreference='Stop'
$frames=Join-Path $RecordingDirectory 'screenshots\frame-%06d.png'
$ffmpeg=(Get-Command ffmpeg.exe -ErrorAction Stop).Source
& $ffmpeg -hide_banner -y -framerate 10 -i $frames -vf 'scale=trunc(iw/2)*2:trunc(ih/2)*2' -c:v libx264 -preset slow -crf 18 -pix_fmt yuv420p -movflags +faststart $Output
if ($LASTEXITCODE -ne 0) { throw 'Video encoding failed' }
