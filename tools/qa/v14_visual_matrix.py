"""Measure the same 20 simulated participants at three preset/resolution pairs."""
from pathlib import Path
import json,time,shutil,sys
root=Path(__file__).resolve().parents[2];run=root/'mod/run-v14-qa';out=root/'docs/qa'
def command(s): (run/'duskrain-director.txt').write_text(s,encoding='utf8')
results=[]
for preset,w,h in [('rtx2060',1280,720),('rtx4060',1920,1080),('rtx5070',2560,1440)]:
 if len(sys.argv)>1 and preset not in sys.argv[1:]:continue
 sizing='fullscreen true' if h==1440 else f'window {w} {h}'
 command(f'close\nquality {preset}\n{sizing}\ngui 3\ncmd time set 6000');time.sleep(12)
 command(f'qa load\nprobe v14-20actors-{preset}-{w}x{h}')
 print(f'{preset}: simulated load and frame recording started',flush=True)
 time.sleep(25);command(f'shot v14-20actors-{preset}-{w}x{h}');time.sleep(40)
 data=json.loads((run/'v14-load-20.json').read_text('utf8'))
 assert data['participants']==20 and data['successful_casts']>=200,data
 target=out/f'v14-load-{preset}-{w}x{h}.json';target.write_text(json.dumps(data,indent=2),encoding='utf8')
 frames=sorted((run/'duskrain-benchmarks').glob(f'*-v14-20actors-{preset}-{w}x{h}.json'))[-1];shutil.copy2(frames,out/frames.name)
 f=json.loads(frames.read_text('utf8'));assert f['width']==w and f['height']==h
 results.append({'preset':preset,'width':w,'height':h,'server':str(target.name),'frames':frames.name,'tps':data['tps'],'tick_p95_ms':data['p95_ms'],'tick_max_ms':data['max_ms'],'mean_fps':f['meanFps'],'frame_p95_ms':f['p95Ms'],'frame_max_ms':f['maxMs'],'unfocused_frames':f['unfocusedFrames']})
 print(json.dumps(results[-1]),flush=True)
(out/'v14-performance-matrix.json').write_text(json.dumps(results,indent=2),encoding='utf8')
