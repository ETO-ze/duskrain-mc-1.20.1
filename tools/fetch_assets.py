from pathlib import Path
from concurrent.futures import ThreadPoolExecutor,as_completed
import urllib.request,json,hashlib,time,sys
root=Path(__file__).resolve().parents[1]
assets=root/'.deps/gradle/caches/forge_gradle/assets'
index=json.loads((assets/'indexes/5.json').read_text())
objects={v['hash']:v for v in index['objects'].values()}
def get(entry):
    h,v=entry;p=assets/'objects'/h[:2]/h
    if p.exists() and p.stat().st_size==v['size'] and hashlib.sha1(p.read_bytes()).hexdigest()==h:return 'cached'
    p.parent.mkdir(parents=True,exist_ok=True)
    for attempt in range(4):
        try:
            with urllib.request.urlopen('https://resources.download.minecraft.net/'+h[:2]+'/'+h,timeout=25) as r:data=r.read()
            if hashlib.sha1(data).hexdigest()!=h:raise ValueError('Asset hash mismatch')
            tmp=p.with_suffix('.download');tmp.write_bytes(data);tmp.replace(p);return 'downloaded'
        except Exception as e:
            if attempt==3:return f'failed:{h}:{type(e).__name__}'
            time.sleep(attempt+1)
count=0;results={}
with ThreadPoolExecutor(max_workers=16) as pool:
    for f in as_completed([pool.submit(get,x) for x in objects.items()]):
        r=f.result();k=r.split(':')[0];results[k]=results.get(k,0)+1;count+=1
        if r.startswith('failed'):print(r,flush=True)
        if count%100==0:print(f'{count}/{len(objects)} {results}',flush=True)
(root/'asset-validation.json').write_text(json.dumps({'objects':len(objects),'result':results},indent=2))
print('ASSET_VALIDATION',results,flush=True)
sys.exit(1 if results.get('failed') else 0)
