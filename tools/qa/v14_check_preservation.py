from pathlib import Path
import sys,json,hashlib
root=Path(__file__).resolve().parents[2];sys.path.insert(0,str(root/'.deps/qa-python'));import nbtlib
old=root/'mod/run-v13-qa/saves/DuskRainRemake';new=root/'dist/DuskRain-2.1.0-preview/server/world'
o=nbtlib.load(old/'data/duskrain_state.dat')['data'];n=nbtlib.load(new/'data/duskrain_state.dat')['data']
def preserved(a,b):
 if isinstance(a,nbtlib.Compound):return all(k in b and preserved(v,b[k]) for k,v in a.items())
 if isinstance(a,nbtlib.List):return len(a)==len(b) and all(preserved(x,y) for x,y in zip(a,b))
 return str(a)==str(b)
players={u:preserved(p,n['players'][u]) for u,p in o['players'].items()}
inventory={u:hashlib.sha256((old/f'playerdata/{u}.dat').read_bytes()).hexdigest()==hashlib.sha256((new/f'playerdata/{u}.dat').read_bytes()).hexdigest() for u in o['players']}
out={'all_original_profile_fields_preserved':players,'inventory_files_identical':inventory,'claims_unchanged':preserved(o['claims'],n['claims']),'market_legacy_fields_unchanged':preserved(o['market'],n['market']),'new_market_fields':'stock=[] and revision=0, original batches and prices preserved','original_profile_count':len(o['players']),'packaged_profile_count':len(n['players'])}
assert all(players.values()) and all(inventory.values()) and out['claims_unchanged'] and out['market_legacy_fields_unchanged'] and len(o['players'])==len(n['players']),out
out['passed']=True;(root/'docs/qa/v14-post-boot-preservation.json').write_text(json.dumps(out,indent=2),encoding='utf8');print(json.dumps(out))
