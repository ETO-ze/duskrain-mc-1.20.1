"""Verify the saved playable character after native flight and clean shutdown."""
from pathlib import Path
import json,sys,shutil
root=Path(__file__).resolve().parents[2];sys.path.insert(0,str(root/'.deps/qa-python'));import nbtlib
run=root/'mod/run-v14-preview';old=root/'mod/run-v13-qa/saves/DuskRainRemake';world=run/'saves/DuskRainRemake';out=root/'docs/qa'
assert 'All dimensions are saved' in (run/'logs/latest.log').read_text('utf8')
uuid='6e93fc7d-8abf-3d21-a965-18b49e686b0b'
before=nbtlib.load(old/'data/duskrain_state.dat')['data'];after=nbtlib.load(world/'data/duskrain_state.dat')['data']
p=after['players'][uuid];o=before['players'][uuid]
keys=['stage','xp','money','main','mainProgress','dailyClaimed','lastSignDay','school','trialWins','hasHome','homeDim','homeX','homeY','homeZ','upgrades']
assert all(str(p[k])==str(o[k]) for k in keys)
assert str(before['claims'])==str(after['claims'])
old_market=before['market'];new_market=after['market']
for key,s in old_market.items():
 for field in ('owner','ownerName','listings'):
  if field in s:assert str(s[field])==str(new_market[key][field])
player=nbtlib.load(world/'playerdata'/f'{uuid}.dat');baseline=nbtlib.load(old/'playerdata'/f'{uuid}.dat')
items={int(s['Slot']):s for s in player['Inventory']};prior={int(s['Slot']):s for s in baseline['Inventory']}
assert items.keys()==prior.keys()
guide_count=0
for slot,s in items.items():
 old_item=prior[slot]
 if s.get('tag',{}).get('DuskRainGuide',False):
  assert str(s['id'])==str(old_item['id']) and int(s['Count'])==int(old_item['Count'])
  assert int(s['tag']['DuskRainGuideVersion'])==15 and len(s['tag']['pages'])==58
  guide_count+=1
 else:assert str(s)==str(old_item),(slot,str(s),str(old_item))
assert guide_count==1 and str(player['Dimension'])=='duskrain:city' and int(player['playerGameType'])==0
assert 'RootVehicle' not in player
walk=json.loads((run/'duskrain-walk-probe.json').read_text('utf8'));assert walk['distance']>5 and walk['onGround'] and not walk['flying'] and not walk['aborted']
state=json.loads((run/'duskrain-director-status.json').read_text('utf8'))
report=dict(passed=True,player=uuid,world=str(world),stage=int(p['stage']),xp=int(p['xp']),money=int(p['money']),original_fields_preserved=keys,claims_preserved=True,legacy_shop_preserved=True,all_inventory_slots_preserved_except_guide_update=True,guide_count=guide_count,guide_version=15,guide_pages=58,dimension=str(player['Dimension']),position=[float(x) for x in player['Pos']],survival=True,no_vehicle=True,post_landing_walk=walk,saved_cleanly=True,method='Native original-character flight/Shift descent/second flight/recall, camera yaw+pitch and walking; then parse actual saved world and compare V13 baseline')
(out/'v14-playable-character.json').write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf8')
shutil.copy2(out/'v14-v14-flight-steps-result.json',out/'v14-preview-flight-steps-result.json')
for name in ('v14-flight-released-camera','v14-experience-ready'):
 shutil.copy2(run/f'screenshots/{name}.png',out/f'v14-native/{name}-preview-1920x1080.png')
shutil.copy2(run/'duskrain-walk-probe.json',out/'v14-preview-walk.json')
print(json.dumps(report,ensure_ascii=False,indent=2))
