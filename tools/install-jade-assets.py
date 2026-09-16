"""Stage the authored pack and compatible optimization mod without touching saves."""
from pathlib import Path
import shutil,json,hashlib,zipfile
R=Path(__file__).resolve().parents[1];D=R/'dist/DuskRain-2.0.0-preview'
pack=R/'assets/DuskRain-Jade-City.zip';jar=R/'assets/third-party/ferritecore-6.0.1-forge.jar'
assert hashlib.sha256(jar.read_bytes()).hexdigest()=='9c2c9396a49e796d88497758caa4637d2bcbb433c318e2dd9cebcffbaf0f6c54'
for side in ['client','server']:
    (D/side/'resourcepacks').mkdir(exist_ok=True)
    shutil.copy2(pack,D/side/'resourcepacks'/pack.name);shutil.copy2(jar,D/side/'mods'/jar.name)
for file,dest in [('ResourcePackServer.java','ResourcePackServer.java'),('start-server-with-pack.ps1','start-server.ps1')]:
    shutil.copy2(R/'tools'/file,D/'server'/dest)
config=D/'server/resourcepack-host.json'
if not config.exists():config.write_text(json.dumps({'bindAddress':'127.0.0.1','port':25567,'publicBaseUrl':'http://127.0.0.1:25567'},indent=2),encoding='utf8')
for folder in [D/'client',R/'mod/run']:
    (folder/'resourcepacks').mkdir(exist_ok=True);shutil.copy2(pack,folder/'resourcepacks'/pack.name)
    options=folder/'options.txt';text=options.read_text('utf8') if options.exists() else '';lines=text.splitlines()
    current=next((s.split(':',1)[1] for s in lines if s.startswith('resourcePacks:')),'[]')
    packs=json.loads(current);name='file/'+pack.name
    if name not in packs:packs.append(name)
    if folder==R/'mod/run' and options.exists():shutil.copy2(options,R/'backups/terrain-services-20260915-161902/options-before-jade.txt')
    lines=[s for s in lines if not s.startswith('resourcePacks:')]+['resourcePacks:'+json.dumps(packs)]
    options.write_text('\n'.join(lines)+'\n',encoding='utf8')
print('Staged Jade City in client/server, selected in client and preview, installed FerriteCore in both release sides.')
