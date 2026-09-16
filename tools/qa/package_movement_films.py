"""Package this update only; never rebuild or replace a player's world."""
from pathlib import Path
import json,shutil,hashlib,zipfile
ROOT=Path(__file__).resolve().parents[2];DIST=ROOT/'dist'
PATCH=DIST/'DuskRain-Jump-fix-20260916';FILMS=DIST/'DuskRain-films-20260916'
def sha(p):
    with p.open('rb') as f:return hashlib.file_digest(f,'sha256').hexdigest()
jar=ROOT/'mod/build/libs/duskrain-2.0.0.jar'
for side in ('client','server'):
    target=PATCH/side/'mods'/jar.name;target.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(jar,target);assert sha(jar)==sha(target)
shutil.copy2(ROOT/'docs/跳跃修复与录像说明.md',PATCH/'docs/跳跃修复与录像说明.md')
for name in ('movement-final-tests.log','movement-native-results.json','film-original-save-preservation.json','film-delivery-validation.json'):
    shutil.copy2(ROOT/'docs/qa'/name,PATCH/'docs/qa'/name)
scripts=[*(ROOT/'tools/qa').glob('film_*.py'),*(ROOT/'tools/qa').glob('*films.py')]
scripts=list(dict.fromkeys(scripts))
source_files=[p for p in (ROOT/'mod/src').rglob('*') if p.is_file()]+[p for p in (ROOT/'mod/gradle').rglob('*') if p.is_file()]
source_files += [ROOT/'mod'/n for n in ('build.gradle','settings.gradle','gradle.properties','gradlew','gradlew.bat','LICENSE.txt','CREDITS.txt')]
source_files += scripts
with zipfile.ZipFile(PATCH/'source.zip','w',zipfile.ZIP_DEFLATED) as z:
    for p in source_files:z.write(p,p.relative_to(ROOT).as_posix())
with zipfile.ZipFile(PATCH/'source.zip') as z:assert z.testzip() is None
manifest={p.relative_to(PATCH).as_posix():sha(p) for p in PATCH.rglob('*') if p.is_file() and p.name!='SHA256.json'}
(PATCH/'SHA256.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2),encoding='utf8')
patch_zip=DIST/(PATCH.name+'.zip')
with zipfile.ZipFile(patch_zip,'w',zipfile.ZIP_DEFLATED) as z:
    for p in PATCH.rglob('*'):
        if p.is_file():z.write(p,p.relative_to(PATCH).as_posix())
with zipfile.ZipFile(patch_zip) as z:assert z.testzip() is None
film_zip=DIST/'DuskRain-宣传素材-20260916.zip'
with zipfile.ZipFile(film_zip,'w',zipfile.ZIP_DEFLATED) as z:
    for p in FILMS.iterdir():
        if p.is_file():z.write(p,p.name,compress_type=zipfile.ZIP_STORED if p.suffix in ('.mp4','.wav') else zipfile.ZIP_DEFLATED)
    for p in scripts:z.write(p,'制作脚本/'+p.name)
    for p in (FILMS/'edit').glob('*.ass'):z.write(p,'可编辑字幕/'+p.name)
with zipfile.ZipFile(film_zip) as z:assert z.testzip() is None
receipt={'jar_sha256':sha(jar),'archives':[{'file':str(p),'sha256':sha(p),'bytes':p.stat().st_size,'zip_crc_pass':True} for p in (patch_zip,film_zip)]}
(ROOT/'docs/qa/movement-film-packaging.json').write_text(json.dumps(receipt,ensure_ascii=False,indent=2),encoding='utf8')
print(json.dumps(receipt,ensure_ascii=False,indent=2))
