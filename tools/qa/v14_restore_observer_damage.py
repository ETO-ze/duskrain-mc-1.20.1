"""Restore only the Director QA character from its pre-load backup after an invalid load run."""
from pathlib import Path
import sys,json,copy,shutil
root=Path(__file__).resolve().parents[2];sys.path.insert(0,str(root/'.deps/qa-python'));import nbtlib
uid='6e93fc7d-8abf-3d21-a965-18b49e686b0b';world=root/'mod/run-v14-qa/saves/DuskRainRemake';backup=root/'backups/v14-before-final-qa-20260916-165319'
assert 'All dimensions are saved' in (root/'mod/run-v14-qa/logs/latest.log').read_text('utf8')[-5000:]
p=nbtlib.load(backup/f'playerdata/{uid}.dat');shutil.copy2(backup/f'playerdata/{uid}.dat',world/f'playerdata/{uid}.dat')
level=nbtlib.load(world/'level.dat_old');level['Data']['Player']=nbtlib.Compound({k:copy.deepcopy(v) for k,v in p.items()});level.save(world/'level.dat')
old=nbtlib.load(backup/'data/duskrain_state.dat');state=nbtlib.load(world/'data/duskrain_state.dat');state['data']['players'][uid]=copy.deepcopy(old['data']['players'][uid]);state.save()
out={'restored_from':str(backup),'target':'isolated run-v14-qa only','inventory_items':len(p['Inventory']),'health':float(p['Health']),'level':int(p['XpLevel']),'original_v13_world_changed':False,'reason':'QA observer was in survival after manual play; simulated combat hit it. Load harness now forces spectator before spawning actors.'}
(root/'docs/qa/v14-qa-character-restored.json').write_text(json.dumps(out,indent=2),encoding='utf8');print(json.dumps(out))
