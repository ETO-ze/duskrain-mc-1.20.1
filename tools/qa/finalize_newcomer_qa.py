"""Read the saved original player and collect the actual client evidence for distribution."""
from pathlib import Path
import gzip,json,shutil,hashlib
from read_world import Reader

ROOT=Path(__file__).resolve().parents[2];QA=ROOT/'docs/qa'
world=ROOT/'mod/run/saves/DuskRainRemake'
uuid='6e93fc7d-8abf-3d21-a965-18b49e686b0b'
player=Reader(gzip.decompress((world/f'playerdata/{uuid}.dat').read_bytes())).root()
store=Reader(gzip.decompress((world/'data/duskrain_state.dat').read_bytes())).root()['data']
profile=store['players'][uuid]
assert player['Dimension']=='duskrain:city' and player['Pos']==[.5,82.,28.5],player['Pos']
for field,value in {'stage':1,'school':1,'xp':368,'money':422,'main':1,'hasHome':1}.items():
    assert profile[field]==value,(field,profile[field])
items=[{'id':s['id'],'count':s['Count'],'slot':s['Slot']} for s in player['Inventory']]
for item in ['wayfinder','exploration_torch','robe_0_helmet','robe_0_chestplate','robe_0_leggings','robe_0_boots']:
    assert sum(s['count'] for s in items if s['id']=='duskrain:'+item)==1,item
assert any(s['id']=='minecraft:written_book' and s.get('tag',{}).get('DuskRainGuide') for s in player['Inventory'])
assert any(s['id']=='minecraft:rotten_flesh' and s['count']==6 for s in items)
report={'passed':True,'player':'DuskRainDirector','uuid':uuid,'position':player['Pos'],
        'dimension':player['Dimension'],'profile':{k:profile[k] for k in ['stage','school','xp','money','main','hasHome','homeX','homeY','homeZ']},
        'inventory':items,'method':'Graceful client exit, then read persisted player and DuskRain SavedData; original progress and items retained',
        'mod_sha256':hashlib.sha256((ROOT/'mod/build/libs/duskrain-2.0.0.jar').read_bytes()).hexdigest()}
(QA/'original-player-restored.json').write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf8')
visual=json.loads((QA/'visual-acceptance.json').read_text('utf8'))
visual['bridge_turn_native_walk']=True
visual['bridge_turn_evidence']='bridge-clearance-acceptance.json'
visual['original_player_restored']='original-player-restored.json'
visual['note']='Earlier mixed-input probes remain unaccepted; final bridge checks use recorded native forward movement and no flight. Screenshot FPS is not a performance guarantee.'
for key,name in {'compact_and_jade_ring':'v12-first-render.png','wilderness':'v12-wilderness-arrival.png',
                 'guide':'v12-guide-start.png','npc_and_light':'v12-npc-and-torch-night.png','player_shop':'v12-player-shop.png',
                 'original_restored':'v12-original-restored.png'}.items():
    shutil.copy2(ROOT/'mod/run/screenshots'/name,QA/name)
    visual['v12_evidence'][key]=name
shutil.copy2(ROOT/'mod/run/screenshots/v12-quest-claimed.png',QA/'v12-quest-claimed.png')
(QA/'visual-acceptance.json').write_text(json.dumps(visual,ensure_ascii=False,indent=2),encoding='utf8')
print(json.dumps(report))
