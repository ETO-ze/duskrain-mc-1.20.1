"""Verify setup and two boots in the new release directory with publisher-hashed cache."""
from pathlib import Path
import subprocess,time,json,hashlib,shutil,urllib.request
root=Path(__file__).resolve().parents[2];old=root/'dist/DuskRain-2.0.0-preview';out=root/'dist/DuskRain-2.1.0-preview';server=out/'server';qa=root/'docs/qa'
hits=0
for row in json.loads((out/'server-downloads.json').read_text('utf8')):
 src=old/'server/libraries'/row['path'];dst=server/'libraries'/row['path']
 if src.exists() and hashlib.sha1(src.read_bytes()).hexdigest()==row['sha1']:
  dst.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(src,dst);hits+=1
with (qa/'v14-release-setup.log').open('w',encoding='utf8') as f:
 subprocess.run(['powershell.exe','-NoProfile','-ExecutionPolicy','Bypass','-File',str(out/'setup-server.ps1')],cwd=out,stdout=f,stderr=subprocess.STDOUT,check=True,timeout=240)
results=[]
for cycle in range(2):
 log=qa/f'v14-release-boot-{cycle+1}.log'
 with log.open('w',encoding='utf8') as f:
  p=subprocess.Popen(['powershell.exe','-NoProfile','-ExecutionPolicy','Bypass','-File',str(server/'start-server.ps1')],cwd=server,stdin=subprocess.PIPE,stdout=f,stderr=subprocess.STDOUT,text=True,encoding='utf8')
  try:
   limit=time.monotonic()+150
   while time.monotonic()<limit:
    text=log.read_text('utf8',errors='replace')
    if p.poll() is not None:raise RuntimeError(text[-3000:])
    if 'Done (' in text and 'group 205255670 | started' in text:break
    time.sleep(.5)
   else:raise TimeoutError('Server did not become ready')
   time.sleep(2)
   subprocess.run(['python',str(root/'tools/qa/server_status.py'),str(qa/f'v14-server-status-{cycle+1}.json')],check=True)
   data=urllib.request.urlopen('http://127.0.0.1:25567/DuskRain-Jade-City.zip',timeout=10).read();assert hashlib.sha1(data).digest()==hashlib.sha1((server/'resourcepacks/DuskRain-Jade-City.zip').read_bytes()).digest()
   time.sleep(15)
   p.stdin.write('save-all flush\nstop\n');p.stdin.flush();p.wait(timeout=75)
   text=log.read_text('utf8',errors='replace');assert p.returncode==0 and 'All dimensions are saved' in text
   results.append({'cycle':cycle+1,'exit':p.returncode,'status':True,'resource_pack_hash_matches':True,'saved_cleanly':True,'mod_sha256':hashlib.sha256((server/'mods/duskrain-2.1.0-preview.jar').read_bytes()).hexdigest()})
   print(json.dumps(results[-1]),flush=True)
  finally:
   if p.poll() is None:
    p.stdin.write('stop\n');p.stdin.flush();p.wait(timeout=75)
(qa/'v14-release-server.json').write_text(json.dumps({'installed_in_new_directory':True,'publisher_hashed_cached_dependencies':hits,'cycles':results},indent=2),encoding='utf8')
