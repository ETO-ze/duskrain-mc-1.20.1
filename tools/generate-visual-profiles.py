"""Export the exact client UI profiles into installable files; validate pinned shader options."""
from pathlib import Path
import json, re, zipfile

ROOT=Path(__file__).resolve().parents[1]
SOURCE=ROOT/'mod/src/main/resources/assets/duskrain/visual-profiles.json'
data=json.loads(SOURCE.read_text(encoding='utf8'))
with zipfile.ZipFile(ROOT/'assets/third-party'/data['pack']) as z:
    source=z.read('shaders/lib/common.glsl').decode()
for p in data['presets']:
    for key,value in p['shader'].items():
        match=re.search(r'^\s*(?://)?\s*#define\s+'+re.escape(key)+r'\b([^\r\n]*)',source,re.M)
        if not match: match=re.search(r'^\s*const \w+ '+re.escape(key)+r'\s*=([^\r\n]*)',source,re.M)
        assert match, f'Unknown shader option: {key}'
        spec=match.group(1); values=re.search(r'//\[([^]]+)\]',spec)
        if values:
            allowed=values.group(1).split()
            # Some shader menus omit the default (COLORED_LIGHTING=0).
            default=spec.split('//')[0].strip().strip(';').split()[0]
            assert value in allowed+[default], f'{key}={value} not in {allowed}'
        else: assert value in ('true','false'), f'{key} requires a boolean'
out=ROOT/'dist/DuskRain-2.0.0-preview/client'
for p in data['presets']:
    folder=out/'profiles'/p['id'];(folder/'config').mkdir(parents=True,exist_ok=True);(folder/'shaderpacks').mkdir(exist_ok=True)
    (folder/'options.txt').write_text(f'renderDistance:{p["renderDistance"]}\nsimulationDistance:6\nmaxFps:{p["maxFps"]}\nrenderClouds:false\n',encoding='utf8')
    (folder/'config/oculus.properties').write_text(f'enableShaders=true\nshaderPack={data["pack"]}\n',encoding='utf8')
    (folder/'config/duskrain-visual.json').write_text(json.dumps({'profile':p['id'],'pack':data['pack']}),encoding='utf8')
    (folder/'shaderpacks'/f'{data["pack"]}.txt').write_text('\n'.join(f'{k}={v}' for k,v in p['shader'].items())+'\n',encoding='utf8')
    (out/f'画质-{p["hardware"].replace(" ","")}.bat').write_text('@echo off\r\npowershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0apply-profile.ps1" -Profile '+p['id']+' -GameDirectory "%~dp0."\r\npause\r\n',encoding='ascii')
(out/'profiles/visual-profiles.json').write_text(json.dumps(data,ensure_ascii=False,indent=2),encoding='utf8')
print('Validated all pinned shader options and exported 3 identical UI/install profiles.')
