"""Stage a new release directory. Never overwrite the V13 world or earlier releases."""
from pathlib import Path
import json,shutil,zipfile,hashlib,sys
root=Path(__file__).resolve().parents[1];base=root/'dist/DuskRain-2.0.0-preview';out=root/'dist/DuskRain-2.1.0-preview'
def copy(src,dst):
 dst.parent.mkdir(parents=True,exist_ok=True)
 if src.is_dir():shutil.copytree(src,dst,dirs_exist_ok=True,ignore=shutil.ignore_patterns('__pycache__','session.lock','profile-install-sandbox'))
 else:shutil.copy2(src,dst)
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
if '--archive' not in sys.argv:
 assert not out.exists(),'Stage into a new directory; inspect an existing stage before reusing it.'
 assert 'All 48 required tests passed' in (root/'docs/qa/v14-gametests.log').read_text('utf8')
 jar=root/'mod/build/libs/duskrain-2.1.0-preview.jar';assert jar.exists()
 out.mkdir()
 for n in ('runtime','setup-server.ps1','server-downloads.json','forge-1.20.1-47.4.10-installer.jar'):copy(base/n,out/n)
 copy(root/'tools/setup-release-server.ps1',out/'setup-server.ps1')
 for n in ('client','server'):
  for folder in ('config','defaultconfigs','resourcepacks','profiles','shaderpacks'):
   if (base/n/folder).exists():copy(base/n/folder,out/n/folder)
  (out/n/'mods').mkdir(parents=True,exist_ok=True)
  for p in (base/n).iterdir():
   if p.is_file() and p.suffix in ('.ps1','.bat','.txt','.properties','.java','.json') and p.name in ('options.txt','apply-profile.ps1','start-server.ps1','start-server.bat','server.properties','user_jvm_args.txt','ResourcePackServer.java','resourcepack-host.json','eula.txt','ops.json','whitelist.json','banned-ips.json','banned-players.json') or p.is_file() and p.name.startswith('画质-'):copy(p,out/n/p.name)
  for p in (base/n/'mods').glob('*.jar'):
   if not p.name.startswith(('duskrain-','skinlayers')):copy(p,out/n/'mods'/p.name)
  copy(jar,out/n/'mods'/jar.name)
  for p in (root/'mod/run-v14-qa/config').glob('duskrain*.json'):copy(p,out/n/'config'/p.name)
 for p in (root/'assets/third-party').glob('*.jar'):
  if 'sources' not in p.name and not p.name.startswith('skinlayers'):copy(p,out/'client/mods'/p.name)
 copy(root/'mod/src',out/'source/mod/src');copy(root/'mod/gradle',out/'source/mod/gradle')
 for n in ('gradlew','gradlew.bat','build.gradle','settings.gradle','gradle.properties','LICENSE.txt','CREDITS.txt'):copy(root/'mod'/n,out/'source/mod'/n)
 copy(root/'tools',out/'source/tools');copy(root/'assets/client-config',out/'source/assets/client-config');copy(root/'assets/jade-city',out/'source/assets/jade-city')
 copy(root/'docs',out/'docs')
 for p in (out/'docs/qa').glob('*.png'):pass
 copy(base/'third-party',out/'third-party')
 for p in (root/'assets/third-party').glob('*.json'):copy(p,out/'third-party'/p.name)
 copy(root/'tools/install-skinlayers.ps1',out/'client/install-skinlayers.ps1')
 copy(root/'docs/V14-README.md',out/'README.md')
 # Preserve the original player's ledger, tasks, claims, inventory and overworld.
 # Test actors and test purchases are confined to run-v14-qa and are not published.
 old=root/'mod/run-v13-qa/saves/DuskRainRemake';test=root/'mod/run-v14-qa/saves/DuskRainRemake';world=out/'server/world'
 copy(old,world)
 for dim in ('city','guild'):copy(test/'dimensions/duskrain'/dim,world/'dimensions/duskrain'/dim)
 copy(test/'data/duskrain_guilds.dat',world/'data/duskrain_guilds.dat')
 important=['data/duskrain_state.dat','playerdata/6e93fc7d-8abf-3d21-a965-18b49e686b0b.dat']
 preservation={n:{'before':sha(old/n),'after':sha(world/n),'same':sha(old/n)==sha(world/n)} for n in important if (old/n).exists()}
 assert preservation and all(x['same'] for x in preservation.values())
 (root/'docs/qa/v14-package-preservation.json').write_text(json.dumps(preservation,indent=2),encoding='utf8')
 print(f'Staged {out}',flush=True)
else:
 assert out.exists()
 jar=root/'mod/build/libs/duskrain-2.1.0-preview.jar';jar_hash=sha(jar)
 resources=json.loads((root/'docs/qa/v14-resource-check.json').read_text('utf8'))
 release=json.loads((root/'docs/qa/v14-release-server.json').read_text('utf8'))
 assert resources['passed'] and resources['jar_sha256']==jar_hash
 assert len(release['cycles'])==2 and all(c['exit']==0 and c['status'] and c['resource_pack_hash_matches'] and c['saved_cleanly'] and c['mod_sha256']==jar_hash for c in release['cycles'])
 assert json.loads((root/'docs/qa/v14-post-boot-preservation.json').read_text('utf8'))['passed']
 assert all(sha(out/n/'mods'/jar.name)==jar_hash for n in ('client','server'))
 # Refresh reports after installed-server validation, without replacing world/player data.
 copy(root/'docs',out/'docs');copy(root/'docs/V14-README.md',out/'README.md')
 copy(root/'mod/src',out/'source/mod/src');copy(root/'tools',out/'source/tools')
 obsolete=out/'source/mod/src/main/java/cn/duskrain/mixin/QaPlayers.java'
 if obsolete.exists():obsolete.unlink()
 files=[]
 for p in out.rglob('*'):
  if not p.is_file():continue
  rel=p.relative_to(out)
  if rel.parts[:2] in (('server','libraries'),('server','logs'),('server','crash-reports'),('server','backups')) or 'profile-install-sandbox' in rel.parts or '__pycache__' in rel.parts:continue
  if p.name in ('session.lock','checksums.json','v14-package-verification.json') or p.name.endswith('.bak') or p.suffix=='.gz':continue
  if p.suffix=='.log' and rel.parts[0]!='docs':continue
  if p.name.startswith('skinlayers') and p.suffix=='.jar':raise AssertionError('No Skin Layers redistribution')
  files.append(p)
 manifest={p.relative_to(out).as_posix():sha(p) for p in sorted(files)}
 (out/'checksums.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2),encoding='utf8')
 archive=root/'dist/DuskRain-2.1.0-preview.zip'
 with zipfile.ZipFile(archive,'w',zipfile.ZIP_DEFLATED,compresslevel=6) as z:
  for p in files+[out/'checksums.json']:z.write(p,out.name+'/'+p.relative_to(out).as_posix())
 with zipfile.ZipFile(archive) as z:
  assert z.testzip() is None
  for n,h in manifest.items():assert hashlib.sha256(z.read(out.name+'/'+n)).hexdigest()==h,n
 result={'archive':str(archive),'bytes':archive.stat().st_size,'files':len(files)+1,'sha256':sha(archive),'crc_and_all_hashes_verified':True,'release_status':'preview; acceptance gaps listed in docs/V14-验收记录.md'}
 (root/'docs/qa/v14-package-verification.json').write_text(json.dumps(result,indent=2),encoding='utf8');print(json.dumps(result),flush=True)
