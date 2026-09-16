"""Collate only the completed, undisturbed native measurements and current guide."""
from pathlib import Path
import json
root=Path(__file__).resolve().parents[2];out=root/'docs/qa'
captures=[('rtx2060',1280,720,'1789549684059'),('rtx4060',1920,1080,'1789551439455'),('rtx5070',2560,1440,'1789551885771')]
rows=[]
for preset,w,h,stamp in captures:
 server=f'v14-load-{preset}-{w}x{h}.json';frames=f'{stamp}-v14-20actors-{preset}-{w}x{h}.json'
 s=json.loads((out/server).read_text('utf8'));f=json.loads((out/frames).read_text('utf8'))
 assert (f['width'],f['height'])==(w,h) and f['unfocusedFrames']==0 and f['framesWithMenu']==0
 assert s['participants']==20 and s['successful_casts']>=200
 rows.append(dict(preset=preset,width=w,height=h,server=server,frames=frames,gpu=f['gpu'],visual=f['visual'],vsync=f['vsync'],client_duration_seconds=f['durationSeconds'],tps=min(20,s['tps']),raw_ticks_per_wall_second=s['tps'],tick_p95_ms=s['p95_ms'],tick_max_ms=s['max_ms'],mean_fps=f['meanFps'],frame_p95_ms=f['p95Ms'],frame_max_ms=f['maxMs'],unfocused_frames=0,menu_frames=0))
(out/'v14-performance-matrix.json').write_text(json.dumps({'method':'20 simulated server players plus one actual observer; fixed preloaded region; all presets measured on RTX4060; nominal TPS capped at 20, raw catch-up rate retained','runs':rows},ensure_ascii=False,indent=2),encoding='utf8')
pages=json.loads((root/'mod/src/main/resources/data/duskrain/guide.json').read_text('utf8'))
assert len(pages)==57
text='# DuskRain · 入世手册\n\n指南版本 15；游戏内共 58 页（含最后的启程按钮）。内容与当前模组资源同步。\n\n'
for page in pages:
 title,_,body=page.partition('\n')
 text+='## '+title+'\n'+body+'\n\n---\n\n'
text+='## 准备启程\n\n阅读后，点击游戏内最后一页的【启程 · 进入烟雨主城】。按钮执行 `/dr start`。已经启程的角色使用 `/dr spawn` 回城。\n'
(root/'docs/入世手册.md').write_text(text,encoding='utf8')
print(json.dumps(rows,ensure_ascii=False,indent=2))
