"""Operate the game's opt-in director interface, checking each resulting native screen."""
import sys,json,time
from pathlib import Path
root=Path(__file__).resolve().parents[2]
target=sys.argv[1];assert target in ('qa','observer','preview')
run=root/f'mod/run-v14-{target}'
steps=json.loads(Path(sys.argv[2]).read_text('utf8'))
result=[]
for step in steps:
 cmd=step['command'];(run/'duskrain-director.txt').write_text(cmd,encoding='utf8')
 time.sleep(step.get('wait',2.8))
 state=json.loads((run/'duskrain-director-status.json').read_text('utf8'))
 if 'screen' in step:assert state['screen']==step['screen'],(cmd,state['screen'])
 if 'vehicle' in step:assert state.get('vehicle')==step['vehicle'],(cmd,state)
 if 'button' in step:assert any(w['label']==step['button'] and w['active'] for w in state.get('widgets',[])),(cmd,step['button'],state)
 result.append({'command':cmd,'screen':state['screen'],'position':state.get('position'),'vehicle':state.get('vehicle'),'titles':state.get('titles')})
 print(json.dumps(result[-1],ensure_ascii=False),flush=True)
(root/f'docs/qa/v14-{target}-{Path(sys.argv[2]).stem}-result.json').write_text(json.dumps(result,ensure_ascii=False,indent=2),encoding='utf8')
