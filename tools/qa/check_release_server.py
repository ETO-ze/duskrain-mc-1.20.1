"""Boot the actual packaged scripts, query native status, audit saved city, and restart."""
from pathlib import Path
import subprocess,time,json,hashlib,urllib.request,os
ROOT=Path(__file__).resolve().parents[2];SERVER=ROOT/'dist/DuskRain-2.0.0-preview/server';QA=ROOT/'docs/qa'
results=[]
for cycle in range(2):
    log=QA/f'site-release-{cycle+1}.log'
    with log.open('w',encoding='utf8') as out:
        proc=subprocess.Popen(['powershell.exe','-NoProfile','-ExecutionPolicy','Bypass','-File',str(SERVER/'start-server.ps1')],cwd=SERVER,stdin=subprocess.PIPE,stdout=out,stderr=subprocess.STDOUT,text=True,encoding='utf8')
        try:
            deadline=time.monotonic()+120
            while time.monotonic()<deadline:
                body=log.read_text('utf8',errors='replace')
                if proc.poll() is not None:raise RuntimeError(f'Server stopped ({proc.returncode}): {body[-2500:]}')
                if 'Done (' in body:break
                time.sleep(.5)
            else:raise TimeoutError('Server readiness timeout')
            print(f'Cycle {cycle+1}: server ready',flush=True)
            subprocess.run(['python',str(ROOT/'tools/qa/server_status.py'),str(QA/f'site-release-status-{cycle+1}.json')],check=True,stdout=subprocess.DEVNULL)
            pack=urllib.request.urlopen('http://127.0.0.1:25567/DuskRain-Jade-City.zip',timeout=10).read()
            assert hashlib.sha1(pack).digest()==hashlib.sha1((SERVER/'resourcepacks/DuskRain-Jade-City.zip').read_bytes()).digest()
            if cycle==0:
                proc.stdin.write('dr admin walk_audit\ndr admin site_audit\ndr admin route_audit\ndr admin fixture_audit\n');proc.stdin.flush()
                deadline=time.monotonic()+90
                while time.monotonic()<deadline:
                    body=log.read_text('utf8',errors='replace')
                    if 'FIXTURE_AUDIT' in body:break
                    if proc.poll() is not None:raise RuntimeError('Server stopped during audit')
                    time.sleep(.5)
                else:raise TimeoutError('Site audit timeout')
                for name in ['duskrain-walk-audit.json','duskrain-site-audit.json','duskrain-route-network.json','duskrain-fixture-audit.json']:
                    data=json.loads((SERVER/name).read_text('utf8'));assert data['passed'],data
                    (QA/('site-release-'+name)).write_text(json.dumps(data,ensure_ascii=False,indent=2),encoding='utf8')
            proc.stdin.write('save-all flush\nstop\n');proc.stdin.flush();proc.wait(timeout=50)
            body=log.read_text('utf8',errors='replace')
            assert proc.returncode==0 and 'All dimensions are saved' in body
            debug=(SERVER/'logs/debug.log').read_text('utf8',errors='replace')
            assert 'Creating FMLModContainer instance for malte0811.ferritecore.ModMainForge' in debug
            assert 'Mixing BlockStateCacheMixin from ferritecore.blockstatecache.mixin.json' in debug
            (QA/f'site-release-ferrite-{cycle+1}.txt').write_text('\n'.join(line for line in debug.splitlines() if 'ferritecore' in line),encoding='utf8')
            results.append({'cycle':cycle+1,'exit':proc.returncode,'native_status':True,'resource_pack_hash':True,'all_dimensions_saved':True,
                'mod_sha256':hashlib.sha256((SERVER/'mods/duskrain-2.0.0.jar').read_bytes()).hexdigest()})
            print(f'Cycle {cycle+1}: saved and stopped normally',flush=True)
        finally:
            if proc.poll() is None:
                try:proc.stdin.write('stop\n');proc.stdin.flush();proc.wait(timeout=45)
                except Exception:print(f'Process {proc.pid} did not stop; inspect before retry',flush=True)
(QA/'site-release-server.json').write_text(json.dumps(results,indent=2),encoding='utf8')
