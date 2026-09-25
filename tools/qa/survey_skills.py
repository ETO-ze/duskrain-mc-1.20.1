from pathlib import Path
import json,time
ROOT=Path(__file__).resolve().parents[2];game=ROOT/'mod/run-v22-client-b';command=game/'duskrain-director.txt'
def send(text):command.write_text(text,encoding='utf8')
def shot(name,text,ticks):
 out=game/'screenshots'/f'{name}.png';start=time.time();send(text+f'\nshot_after {name} {ticks}\n')
 while not out.exists() or out.stat().st_mtime<start:
  if time.time()-start>20:raise TimeoutError(name)
  time.sleep(.2)
 print(name,flush=True)
a=json.loads((game/'duskrain-walk-audit.json').read_text(encoding='utf8'));floor=next(r['floor'] for r in a['buildings'] if r['id']=='arena')
send('window 1920 1080\ngui 3\nshowhud\nperspective 1\ncmd time set 13000\ncmd weather clear\n');time.sleep(3)
for school,name in [(1,'sword'),(3,'body'),(2,'mage')]:
 for slot,key in enumerate(['skill1','skill2','skill3']):
  send(f'qa fxschool {school}\ncmd tp @s 84.5 {floor+1} 82.5 180 0\n');time.sleep(2)
  shot(f'fx-{name}-{slot}',f'press key.duskrain.{key}',28 if school==2 and slot==2 else 4)
  time.sleep(2)
send(f'qa fxschool 1\ncmd tp @s 84.5 {floor+1} 82.5 180 0\n');time.sleep(3)
shot('fx-normal-sword','qa normalqi',4)
send('qa fxrestore\nperspective 0\ncmd gamemode survival\ncmd tp @s 0.5 82 28.5 180 5\n');time.sleep(3)
shot('hud-slot20-1080','showhud',2)
