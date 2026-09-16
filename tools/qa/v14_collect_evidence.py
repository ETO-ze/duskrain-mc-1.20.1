from pathlib import Path
import shutil,json,struct
root=Path(__file__).resolve().parents[2];dest=root/'docs/qa/v14-native';dest.mkdir(exist_ok=True)
shots={'qa':['v14-foundation-fixed.png','v14-bridge-west-joined-1080.png','v14-enchanter-front-no-shader-1080.png','v14-market-customer-confirm.png','v14-market-customer-purchased.png'],'observer':['v14-flight-released-camera.png','v14-market-two-products-720.png','v14-enchant-confirm-1080.png','v14-enchant-purchased-1080.png']}
rows=[]
for run,names in shots.items():
 for name in names:
  src=root/f'mod/run-v14-{run}/screenshots'/name
  if not src.exists():continue
  w,h=struct.unpack('>II',src.read_bytes()[16:24]);actual=f'{src.stem}-{w}x{h}.png';shutil.copy2(src,dest/actual);rows.append({'file':actual,'width':w,'height':h,'source':str(src),'native_capture':True})
for name in ('v14-palace-audit.json','native-guide-lines.json'):
 src=root/'mod/run-v14-qa'/name
 if src.exists():shutil.copy2(src,root/'docs/qa'/('v14-'+name if not name.startswith('v14-') else name))
(dest/'index.json').write_text(json.dumps(rows,indent=2),encoding='utf8');print(f'Collected {len(rows)} native frames')
