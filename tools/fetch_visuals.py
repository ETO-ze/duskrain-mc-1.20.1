"""Fetch pinned official artifacts, verify publisher hashes, retain provenance."""
import hashlib, json, pathlib, urllib.request
ROOT = pathlib.Path(__file__).resolve().parents[1]
OUT = ROOT / 'assets/third-party'
OUT.mkdir(parents=True, exist_ok=True)
VERSIONS = [('embeddium','UTbfe5d1','client'),('oculus','iQ1SwGc3','client'),
            ('complementary-reimagined','71Hn3myZ','shader'),('geckolib','aC5KMoNg','both'),
            ('ferrite-core','DG5Fn9Sz','both'),('sodium-options-api','d0EFLitO','client'),
            ('sodium-dynamic-lights','I156ee3A','client')]
def get(url):
    return urllib.request.urlopen(urllib.request.Request(url,headers={'User-Agent':'DuskRain-build/2.0'}),timeout=60).read()
manifest=[]
for project, version, side in VERSIONS:
    meta=json.loads(get('https://api.modrinth.com/v2/version/'+version))
    f=next(x for x in meta['files'] if x['primary'])
    target=OUT/f['filename']
    if not target.exists(): target.write_bytes(get(f['url']))
    data=target.read_bytes()
    assert hashlib.sha512(data).hexdigest()==f['hashes']['sha512'], target
    pm=json.loads(get('https://api.modrinth.com/v2/project/'+project))
    manifest.append(dict(project=project,version=meta['version_number'],side=side,file=f['filename'],url=f['url'],
                         sha256=hashlib.sha256(data).hexdigest(),sha512=f['hashes']['sha512'],
                         license=pm['license'],dependencies=meta['dependencies']))
    print(project,meta['version_number'],len(data),flush=True)
    (OUT/(project+'-metadata.json')).write_text(json.dumps({'project':pm,'release':meta},ensure_ascii=False,indent=2),encoding='utf-8')
(OUT/'manifest.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2),encoding='utf-8')
