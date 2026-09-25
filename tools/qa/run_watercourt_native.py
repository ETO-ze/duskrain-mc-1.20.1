import json,time
from pathlib import Path
r=Path.cwd();g=r/'mod/run-watercourt-r6';launched=(g/'qa-process.json').stat().st_mtime;cmd=g/'duskrain-director.txt'
end=time.time()+90
while time.time()<end:
 try:
  d=json.loads((g/'duskrain-director-status.json').read_text('utf8'))
  if 'player' in d and (g/'duskrain-director-status.json').stat().st_mtime>launched:break
 except (OSError,ValueError):pass
 time.sleep(1)
else:raise RuntimeError('client did not load')
cmd.write_text('close\ncmd gamerule sendCommandFeedback false\ncmd time set 6000\ncmd gamerule doDaylightCycle false\ncmd weather clear\ncmd dr admin architecture all\ncmd gamemode spectator\ncmd tp @s 58 68 269 155 18\n',encoding='utf8')
start=time.time();p=g/'saves/DuskRainRemake/duskrain-architecture-v2/progress.json'
while time.time()-start<600:
 try:
  d=json.loads(p.read_text('utf8'))
  if p.stat().st_mtime>start and d['complete']:break
 except (OSError,ValueError):pass
 time.sleep(2)
else:raise RuntimeError('migration timeout')
print('migration',d,flush=True)
cmd.write_text('cmd dr admin architecture audit\ncmd dr admin architecture auditbuildings\nhidehud\nshot_after water-r6 25\n',encoding='utf8')
start=time.time();p=g/'duskrain-building-audit.json'
while time.time()-start<180:
 if p.exists() and p.stat().st_mtime>start:break
 time.sleep(2)
else:raise RuntimeError('audit timeout')
a=json.loads(p.read_text('utf8'));w=json.loads((g/'duskrain-watercourt-audit.json').read_text('utf8'))
print('buildings',len(a['buildings']),'differences',a['differences'],'water',w['passed'],'water_issues',w['issues'],flush=True)
