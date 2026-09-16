from pathlib import Path
import json,struct,hashlib,zipfile
root=Path(__file__).resolve().parents[2];assets=root/'mod/src/main/resources/assets/duskrain'
def size(p):
 b=p.read_bytes();assert b[:8]==b'\x89PNG\r\n\x1a\n';return struct.unpack('>II',b[16:24])
errors=[];armors=[];weapons=[];npcs=[]
for stage in range(21):
 for layer in (1,2):
  p=assets/f'textures/models/armor/robe_{stage}_layer_{layer}.png';assert size(p)==(512,512),p;armors.append(p)
 for part in ('helmet','chestplate','leggings','boots'):
  m=json.loads((assets/f'models/item/robe_{stage}_{part}.json').read_text('utf8'))
  p=assets/('textures/'+m['textures']['layer0'].split(':')[1]+'.png');assert p.exists(),p
for school in range(1,4):
 for tier in range(7):
  p=assets/f'models/item/artifact_{school}_{tier}.json';m=json.loads(p.read_text('utf8'));assert m.get('elements'),p
  for texture in m['textures'].values():
   if texture.startswith('duskrain:'):assert (assets/('textures/'+texture.split(':')[1]+'.png')).exists(),texture
  weapons.append(p)
for p in (assets/'textures/entity').glob('resident_*.png'):assert size(p)==(512,512),p;npcs.append(p)
assert len(npcs)==11
pages=json.loads((root/'mod/src/main/resources/data/duskrain/guide.json').read_text('utf8'));assert len(pages)==57
jar=root/'mod/build/libs/duskrain-2.1.0-preview.jar'
with zipfile.ZipFile(jar) as z:
 assert z.testzip() is None
 assert 'version="2.1.0-preview"' in z.read('META-INF/mods.toml').decode()
 for p in armors+npcs+weapons:
  rel='assets/duskrain/'+p.relative_to(assets).as_posix();assert z.read(rel)==p.read_bytes(),rel
result={'passed':True,'armor_items':84,'armor_textures':len(armors),'texture_size':[512,512],'sculpted_artifact_models':len(weapons),'resident_textures':len(npcs),'guide_pages':len(pages)+1,'source_and_jar_assets_identical':True,'jar_sha256':hashlib.sha256(jar.read_bytes()).hexdigest(),'method':'PNG dimensions and signatures, all equipment IDs, sculpted model texture references and byte-for-byte final JAR resources; 57 content pages plus native start page; visual poses require separate native review'}
(root/'docs/qa/v14-resource-check.json').write_text(json.dumps(result,indent=2),encoding='utf8');print(json.dumps(result))
