"""Capture authored buildings using the game's opt-in QA interface, in the isolated world only."""
from pathlib import Path
import json,re,time,math
ROOT=Path(__file__).resolve().parents[2]
game=ROOT/'mod/run-v22-client-b'
command=game/'duskrain-director.txt'
source=(ROOT/'mod/src/main/java/cn/duskrain/CityPlan.java').read_text(encoding='utf8')
rows=json.loads((game/'duskrain-walk-audit.json').read_text(encoding='utf8'))['buildings']
floors={r['id']:r['floor'] for r in rows}
buildings=[]
for ident,name,x,z,w,d,f,v,y in re.findall(r'add\("(\w+)","([^"]+)",(-?\d+),(-?\d+),(\d+),(\d+),(\d+),(\d+),(\d+)\)',source):
 buildings.append((ident,name,int(x),int(z),int(w),int(d),int(f)))
homes=re.search(r'int\[\]\[\] homes=\{(.+?)\};',source)[1]
for i,(x,z) in enumerate(re.findall(r'\{(-?\d+),(-?\d+)\}',homes)):
 ident=f'home_{i+1}';buildings.append((ident,ident,int(x),int(z),12,12 if i%6==2 else 10,2 if i%6 in (1,4) else 1))
for ident,x,z in [('moon_tower',-177,-145),('tide_tower',196,184),('wind_bell',-222,169)]:buildings.append((ident,ident,x,z,9,9,3))
results=[]
import sys
walking='--walk' in sys.argv
command.write_text(('cmd gamemode survival' if walking else 'cmd gamemode spectator')+'\ncmd gamerule sendCommandFeedback false\ncmd gamerule doDaylightCycle false\ncmd weather clear\ncmd time set 6000\nhidehud\n',encoding='utf8')
time.sleep(3)
for ident,name,x,z,w,d,f in buildings:
 prefix='walk' if walking else 'survey'
 out=game/'screenshots'/f'{prefix}-{ident}.png';start=time.time()
 distance=max(18,w+8)
 if walking:
  command.write_text(f'cmd tp @s {x+.5} {floors[ident]+1} {z+d+3.5} 180 0\nkey sprint false\nkey jump false\n',encoding='utf8');time.sleep(2)
  probe=game/'duskrain-walk-probe.json';probe_start=time.time()
  command.write_text(f'walk 35\nshot_after walk-{ident} 44\n',encoding='utf8')
 else:
  command.write_text(f'cmd tp @s {x+.5} {floors[ident]+7+f*2} {z+d+distance+.5} 180 16\nshot_after survey-{ident} 50\n',encoding='utf8')
 while not out.exists() or out.stat().st_mtime<start:
  if time.time()-start>25:raise TimeoutError(ident)
  time.sleep(.3)
 row=dict(id=ident,name=name,file=str(out),floor=floors[ident])
 if walking:
  if not probe.exists() or probe.stat().st_mtime<probe_start:raise RuntimeError('Missing walk evidence')
  row['walk']=json.loads(probe.read_text(encoding='utf8'));w=row['walk'];w['horizontalDistance']=math.hypot(w['end'][0]-w['start'][0],w['end'][2]-w['start'][2]);row['passed']=not w['aborted'] and not w['flying'] and w['horizontalDistance']>=4
 results.append(row)
 (ROOT/('docs/qa/v22-building-walk.json' if walking else 'docs/qa/v22-building-survey.json')).write_text(json.dumps(results,ensure_ascii=False,indent=2),encoding='utf8')
 print(ident,flush=True)
command.write_text('showhud\n',encoding='utf8')
