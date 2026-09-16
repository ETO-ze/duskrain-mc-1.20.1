"""Offline, byte-preserving numeric grant for the explicitly authorized preview character."""
from pathlib import Path
import gzip,struct,json,shutil,os
from read_world import Reader

ROOT=Path(__file__).resolve().parents[2]
world=ROOT/'mod/run/saves/DuskRainRemake'
uuid='6e93fc7d-8abf-3d21-a965-18b49e686b0b'
source=world/'data/duskrain_state.dat'
receipt=ROOT/'docs/qa/test-character-grant-20260915.json'
if receipt.exists():
    raise SystemExit('This one-time authorized grant has already been applied; no change.')
class Locations(Reader):
    def __init__(self,data):
        super().__init__(data);self.path=[];self.fields={}
    def payload(self,t):
        if t==10:
            d={}
            while (kind:=self.number('B')):
                name=self.text();self.path.append(name);d[name]=self.payload(kind);self.path.pop()
            return d
        if tuple(self.path[:-1])==('data','players',uuid) and self.path[-1] in ('money','stage'):
            self.fields[self.path[-1]]=(self.f.tell(),t)
        return super().payload(t)

raw=gzip.decompress(source.read_bytes());reader=Locations(raw);old=reader.root()
profile=old['data']['players'][uuid]
assert profile['knownName']=='DuskRainDirector'
new_money=profile['money']+10_000_000
assert new_money<=1_000_000_000_000
patched=bytearray(raw)
assert reader.fields['money'][1]==4 and reader.fields['stage'][1]==3
struct.pack_into('>q',patched,reader.fields['money'][0],new_money)
struct.pack_into('>i',patched,reader.fields['stage'][0],17)
expected=json.loads(json.dumps(old,default=lambda x:list(x)))
expected['data']['players'][uuid]['money']=new_money
expected['data']['players'][uuid]['stage']=17
parsed=Reader(bytes(patched)).root()
assert json.loads(json.dumps(parsed,default=lambda x:list(x)))==expected
backup=ROOT/'backups/before-guild-combat-20260915/duskrain_state-before-test-grant.dat'
assert not backup.exists()
shutil.copy2(source,backup)
temp=source.with_suffix('.dat.grant-new');temp.write_bytes(gzip.compress(bytes(patched)))
os.replace(temp,source)
report={'passed':True,'player':'DuskRainDirector','uuid':uuid,'money_before':profile['money'],
        'money_added':10_000_000,'money_after':new_money,'stage_before':profile['stage'],'stage_after':17,
        'all_other_nbt_values_preserved':True,'world':str(world),'backup':str(backup)}
receipt.write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf8')
print(json.dumps(report))
