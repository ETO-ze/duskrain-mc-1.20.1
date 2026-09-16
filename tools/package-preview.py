"""Package the tested preview; keep downloaded Minecraft/Forge libraries local only."""
from pathlib import Path
import shutil,json,zipfile,hashlib

root=Path(__file__).resolve().parents[1];out=root/'dist/DuskRain-2.0.0-preview'
for name in ('site-release-duskrain-walk-audit.json','site-release-duskrain-site-audit.json','site-release-duskrain-route-network.json','site-release-duskrain-fixture-audit.json','pbr-materials.json'):
    assert json.loads((root/'docs/qa'/name).read_text('utf8'))['passed'],name
assert 'All 26 required tests passed' in (root/'docs/qa/market-light-v11-tests.log').read_text('utf8')
assert 'All 30 required tests passed' in (root/'docs/qa/newcomer-final-tests.log').read_text('utf8')
cycles=json.loads((root/'docs/qa/site-release-server.json').read_text('utf8'))
assert len(cycles)==2
jar_hash=hashlib.sha256((root/'mod/build/libs/duskrain-2.0.0.jar').read_bytes()).hexdigest()
assert all(c['exit']==0 and c['mod_sha256']==jar_hash for c in cycles),'Packaged JAR must match the server boot checks'
bridge=json.loads((root/'docs/qa/bridge-clearance-acceptance.json').read_text('utf8'))
assert bridge['passed'] and bridge['mod_sha256']==jar_hash,'Actual bridge walk and persisted lamp upgrade must pass'
newcomer=json.loads((root/'docs/qa/newcomer-client-acceptance.json').read_text('utf8'))
assert newcomer['passed']
with zipfile.ZipFile(root/'mod/build/libs/duskrain-2.0.0.jar') as z:
    assert all(hashlib.sha256(z.read(n)).hexdigest()==sha for n,sha in newcomer['validated_component_hashes'].items()),'Newcomer components must match the visually accepted build'
shutil.copytree(root/'docs',out/'docs',dirs_exist_ok=True,ignore=shutil.ignore_patterns('profile-install-sandbox','__pycache__'))
shutil.copytree(root/'mod/src',out/'source/mod/src',dirs_exist_ok=True)
shutil.copytree(root/'mod/gradle',out/'source/mod/gradle',dirs_exist_ok=True)
for name in ('gradlew','gradlew.bat','build.gradle','settings.gradle','gradle.properties','LICENSE.txt','CREDITS.txt'):
    shutil.copy2(root/'mod'/name,out/'source/mod'/name)
shutil.copytree(root/'tools/qa',out/'source/tools/qa',dirs_exist_ok=True,ignore=shutil.ignore_patterns('__pycache__'))
for p in (root/'tools').iterdir():
    if p.is_file():shutil.copy2(p,out/'source/tools'/p.name)
shutil.copytree(root/'assets/jade-city',out/'source/assets/jade-city',dirs_exist_ok=True)
shutil.copytree(root/'assets/client-config',out/'source/assets/client-config',dirs_exist_ok=True)
shutil.copytree(root/'assets/client-config',out/'client/config',dirs_exist_ok=True)
for side in ('client','server'):
    shutil.copy2(root/'mod/build/libs/duskrain-2.0.0.jar',out/side/'mods/duskrain-2.0.0.jar')
third=out/'third-party';third.mkdir(exist_ok=True)
for p in (root/'assets/third-party').glob('*.json'):shutil.copy2(p,third/p.name)
for p in (root/'assets/third-party').iterdir():
    if p.suffix not in ('.jar','.zip') or p.name.endswith('-sources.jar'):continue
    with zipfile.ZipFile(p) as z:
        for name in z.namelist():
            if 'license' in Path(name).name.lower() or Path(name).name.lower() in ('notice','notice.txt'):
                target=third/(p.stem+'-'+Path(name).name);target.write_bytes(z.read(name))

files=[]
for p in out.rglob('*'):
    if not p.is_file():continue
    rel=p.relative_to(out)
    if rel.parts[:2] in (('server','libraries'),('server','logs'),('server','crash-reports')):continue
    if 'profile-install-sandbox' in rel.parts or '__pycache__' in rel.parts:continue
    if p.name in ('session.lock','checksums.json') or p.suffix=='.gz':continue
    if rel.as_posix()=='docs/qa/package-verification.json':continue # external archive hash cannot describe its containing archive
    if p.suffix=='.log' and rel.parts[0]!='docs':continue
    files.append(p)
manifest={p.relative_to(out).as_posix():hashlib.sha256(p.read_bytes()).hexdigest() for p in sorted(files)}
(out/'checksums.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2),encoding='utf8')
archive=root/'dist/DuskRain-2.0.0-preview.zip'
if archive.exists():
    previous=root/'backups/DuskRain-2.0.0-before-ink.zip'
    if not previous.exists():shutil.copy2(archive,previous)
with zipfile.ZipFile(archive,'w',zipfile.ZIP_DEFLATED,compresslevel=6) as z:
    for p in files+[out/'checksums.json']:z.write(p,'DuskRain-2.0.0-preview/'+p.relative_to(out).as_posix())
with zipfile.ZipFile(archive) as z:
    assert z.testzip() is None
    for name,sha in manifest.items():assert hashlib.sha256(z.read('DuskRain-2.0.0-preview/'+name)).hexdigest()==sha,name
report={'archive':str(archive),'bytes':archive.stat().st_size,'files':len(files)+1,'sha256':hashlib.sha256(archive.read_bytes()).hexdigest(),'crc_and_all_manifest_hashes_verified':True,'minecraft_program_and_libraries_included':False}
(root/'docs/qa/package-verification.json').write_text(json.dumps(report,indent=2),encoding='utf8')
print(json.dumps(report))
