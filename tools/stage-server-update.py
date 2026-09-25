"""Stage a cold-start update over SFTP. Credentials stay in an ignored local JSON file.

Does not stop the server. The existing JVM/authentication arguments are preserved.
"""
import argparse, hashlib, json, shlex, shutil, subprocess, tempfile
from pathlib import Path
import paramiko
from mod_stack import ROOT, locked, selected, obtain

LABEL='courtyard-20260925'

def stage(credentials):
    secret=json.loads(credentials.read_text('utf8'))
    client=paramiko.SSHClient();client.load_system_host_keys()
    known=ROOT/'.private/rainplay-known-hosts'
    if known.exists():client.load_host_keys(str(known))
    client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    client.connect(**secret,allow_agent=False,look_for_keys=False,timeout=20)
    client.save_host_keys(str(known));s=client.open_sftp()
    remote='/server/duskrain-update-'+LABEL
    backup=ROOT/'backups'/('server-stage-'+LABEL);backup.mkdir(parents=True,exist_ok=True)
    local=ROOT/'dist'/('server-stage-'+LABEL);local.mkdir(parents=True,exist_ok=True)
    def mkdir(p):
        try:s.stat(p)
        except FileNotFoundError:
            mkdir(p.rsplit('/',1)[0] or '/');s.mkdir(p)
    def put(data,p):
        try:
            with s.open(p,'rb') as f:
                if f.read()==data:return
        except FileNotFoundError:pass
        mkdir(p.rsplit('/',1)[0]);tmp=p+'.upload'
        with s.open(tmp,'wb') as f:f.write(data)
        with s.open(tmp,'rb') as f:assert hashlib.sha256(f.read()).digest()==hashlib.sha256(data).digest()
        s.posix_rename(tmp,p)
    oldmods=[]
    for name in s.listdir('/server/mods'):
        if not name.endswith('.jar'):continue
        with s.open('/server/mods/'+name,'rb') as f:data=f.read()
        (backup/name).write_bytes(data);oldmods.append((name,hashlib.sha256(data).hexdigest()))
    rows=selected(locked(),'server');jars=[obtain(row)[0] for row in rows]
    jars.append(ROOT/'mod/build/libs/duskrain-2.2.0-preview.jar')
    sums=[]
    for jar in jars:
        data=jar.read_bytes();put(data,remote+'/mods/'+jar.name)
        sums.append(hashlib.sha256(data).hexdigest()+'  mods/'+jar.name)
    put(('\n'.join(sums)+'\n').encode(),remote+'/SHA256SUMS')
    preflight='\n'.join("printf '%s\\n' "+shlex.quote(digest+'  mods/'+name)+" | sha256sum -c -" for name,digest in oldmods)
    oldmove='\n'.join('if [ -f '+shlex.quote('mods/'+name)+' ]; then mv '+shlex.quote('mods/'+name)+' "$BACKUP/old-mods/"; fi' for name,_ in oldmods if name.startswith('duskrain-'))
    script='''#!/usr/bin/env sh
set -eu
STAGE=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
cd "$STAGE/.."
[ ! -f "$STAGE/applied" ] || exit 0
(cd "$STAGE" && sha256sum -c SHA256SUMS)
BACKUP="../backup/duskrain-'''+LABEL+'''"
mkdir -p "$BACKUP/old-mods"
if [ ! -f "$BACKUP/backup.complete" ]; then
'''+preflight+'''
    echo '[DuskRain] Creating cold world/mod/config backup before update...'
    set -- world mods config server.properties user_jvm_args.txt ops.json
    if [ -d defaultconfigs ]; then set -- "$@" defaultconfigs; fi
    tar -czf "$BACKUP/server-before.tar.gz.partial" "$@"
    tar -tzf "$BACKUP/server-before.tar.gz.partial" >/dev/null
    mv "$BACKUP/server-before.tar.gz.partial" "$BACKUP/server-before.tar.gz"
    date -u > "$BACKUP/backup.complete"
fi
'''+oldmove+'''
for jar in "$STAGE"/mods/*.jar; do
    name=$(basename "$jar")
    cp "$jar" "mods/$name.update"
    mv "mods/$name.update" "mods/$name"
done
printf 'all\\n' > duskrain-architecture-v3.request
date -u > "$STAGE/applied"
echo '[DuskRain] Protocol 8 installed. Original authentication and world preserved.'
'''
    (local/'apply.sh').write_text(script,encoding='utf8',newline='\n')
    bash=Path('D:/Git/bin/bash.exe')
    subprocess.run([str(bash),'-n',str(local/'apply.sh')],check=True)
    # Exercise backup, atomic JAR promotion, and repeat execution before arming startup.
    smoke=Path(tempfile.mkdtemp(prefix='server-stage-smoke-',dir=ROOT/'backups'))
    sandbox=smoke/'server';sandbox.mkdir();mock=sandbox/('duskrain-update-'+LABEL);(mock/'mods').mkdir(parents=True)
    (sandbox/'mods').mkdir();(sandbox/'config').mkdir();(sandbox/'world').mkdir()
    (sandbox/'world/sentinel').write_bytes(b'unchanged-world-data')
    for name,_ in oldmods:shutil.copy2(backup/name,sandbox/'mods'/name)
    for jar in jars:shutil.copy2(jar,mock/'mods'/jar.name)
    for name in ('server.properties','user_jvm_args.txt','ops.json'):(sandbox/name).write_text('test')
    (mock/'SHA256SUMS').write_text('\n'.join(sums)+'\n',encoding='utf8',newline='\n');shutil.copy2(local/'apply.sh',mock/'apply.sh')
    for _ in range(2):subprocess.run([str(bash),str(mock/'apply.sh')],check=True,capture_output=True)
    assert (sandbox/'world/sentinel').read_bytes()==b'unchanged-world-data'
    assert (sandbox/'duskrain-architecture-v3.request').read_text().strip()=='all'
    assert len(list((sandbox/'mods').glob('duskrain-*.jar')))==1
    for jar in jars:assert (sandbox/'mods'/jar.name).read_bytes()==jar.read_bytes()
    put(script.encode(),remote+'/apply.sh')
    for p in ['/start-duskrain.sh','/server/start-duskrain.sh']:
        with s.open(p,'rb') as f:old=f.read()
        bp=backup/('root-start.sh' if p=='/start-duskrain.sh' else 'server-start.sh');bp.write_bytes(old)
        text=old.decode().replace('\r\n','\n')
        needle='JAVA_BIN=${JAVA_BIN:-java}'
        if needle not in text:raise ValueError('Unknown startup layout; preserved')
        hook='if [ -f "duskrain-update-'+LABEL+'/apply.sh" ]; then sh "duskrain-update-'+LABEL+'/apply.sh"; fi\n'
        if hook not in text:text=text.replace(needle,hook+needle)
        text=text.replace('protocol 7','protocol 8')
        put(old,p+'.before-'+LABEL);put(text.encode(),p)
    report={'remote_stage':remote,'cold_backup':'/backup/duskrain-'+LABEL+'/server-before.tar.gz',
            'installed':False,'restart_required':True,'world_uploaded':False,'hashes':sums}
    (ROOT/'docs/qa/server-staged-20260925.json').write_text(json.dumps(report,indent=2),encoding='utf8')
    s.close();client.close();print(json.dumps(report))

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--credentials',type=Path,required=True)
    stage(p.parse_args().credentials)
