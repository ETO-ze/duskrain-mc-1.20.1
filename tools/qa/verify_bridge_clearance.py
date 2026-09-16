"""Check native stair-walk evidence and the actual persisted release-city lamp migration."""
from pathlib import Path
import hashlib,json,zlib
from read_world import Reader,state

ROOT=Path(__file__).resolve().parents[2]
QA=ROOT/'docs/qa'
jar=ROOT/'mod/build/libs/duskrain-2.0.0.jar'
walks=[json.loads((QA/n).read_text('utf8')) for n in
       ('bridge-lamp-clearance-after.json','bridge-lamp-clearance-next-span.json')]
for w in walks:
    assert not w['aborted'] and not w['flying'] and w['onGround'],w
    assert w['start'][2]-w['end'][2]>9 and w['end'][1]>w['start'][1],w
    assert abs(w['start'][0]-w['end'][0])<.01,w
region=ROOT/'dist/DuskRain-2.0.0-preview/server/world/dimensions/duskrain/city/region/r.-1.-1.mca'
raw=region.read_bytes();cx,cz=-5,-8;slot=(cx%32)+(cz%32)*32
offset=int.from_bytes(raw[slot*4:slot*4+3],'big')*4096
length=int.from_bytes(raw[offset:offset+4],'big')
assert raw[offset+4]==2
chunk=Reader(zlib.decompress(raw[offset+5:offset+4+length])).root()
old=state(chunk,-71,123,-120);new=state(chunk,-71,125,-120)
assert old['Name']=='minecraft:air',old
assert new['Name']=='minecraft:lantern' and new['Properties']['hanging']=='true',new
report={'passed':True,'mod_sha256':hashlib.sha256(jar.read_bytes()).hexdigest(),
        'method':'Two native client forward-walk probes in survival, no flight; read release Anvil after two server boots',
        'walks':walks,'persisted_old_position':[-71,123,-120],'persisted_old_state':old,
        'persisted_new_position':[-71,125,-120],'persisted_new_state':new,
        'regression':'Chunk Load block reads re-entered the unresolved FULL chunk future. The load hook now only queues positions; a bounded END tick performs edits on fully loaded chunks.'}
(QA/'bridge-clearance-acceptance.json').write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf8')
print(json.dumps(report))
