"""Isolated packaged-mod dedicated server validation, never targets production."""
from pathlib import Path
import sys,subprocess,time,shutil,hashlib,json,re,argparse
sys.path.insert(0,str(Path(__file__).resolve().parents[1]))
from mod_stack import ROOT,locked,selected,obtain,write_json

BASE=ROOT/'dist/DuskRain-2.1.0-preview/server'
SERVER=ROOT/'mod/run-v22-server'
QA=ROOT/'docs/qa'
LABEL='v22'
JAVA=next((ROOT/'.deps/java').glob('*/bin/java.exe'))


def tree_hashes(root):
    return {p.relative_to(root).as_posix():hashlib.sha256(p.read_bytes()).hexdigest()
            for p in root.rglob('*') if p.is_file() and p.name!='session.lock'}


def prepare():
    if SERVER.exists():raise ValueError('Existing QA world preserved; use --run to resume cycles')
    SERVER.mkdir(parents=True)
    for folder in ('libraries','config','defaultconfigs','world'):
        if (BASE/folder).exists():shutil.copytree(BASE/folder,SERVER/folder)
    shutil.copytree(ROOT/'assets/v22-server-defaults',SERVER,dirs_exist_ok=True)
    (SERVER/'mods').mkdir()
    for item in selected(locked(),'server'):
        p,_=obtain(item);shutil.copy2(p,SERVER/'mods'/p.name)
    jar=ROOT/'mod/build/libs/duskrain-2.2.0-preview.jar'
    if not jar.exists():raise ValueError('Build new JAR first')
    shutil.copy2(jar,SERVER/'mods'/jar.name)
    (SERVER/'eula.txt').write_text('eula=true\n',encoding='utf8')
    props=(BASE/'server.properties').read_text('utf8')
    overrides={'server-ip':'127.0.0.1','server-port':'25586','online-mode':'false',
               'resource-pack':'','resource-pack-sha1':'','enable-rcon':'false','enable-query':'false',
               'view-distance':'6','simulation-distance':'4','max-players':'20'}
    for k,v in overrides.items():
        props=re.sub(r'^'+re.escape(k)+r'=.*$',k+'='+v,props,flags=re.M)
    (SERVER/'server.properties').write_text(props,encoding='utf8')
    write_json(QA/f'{LABEL}-source-world-before.json',tree_hashes(BASE/'world'))
    write_json(QA/f'{LABEL}-test-world-before.json',tree_hashes(SERVER/'world'))


def run():
    result=[]
    for n in range(1,3):
        log=QA/f'{LABEL}-server-cycle-{n}.log'
        if log.exists():raise ValueError('Existing cycle evidence preserved; inspect it before rerunning')
        started=time.monotonic()
        with log.open('w',encoding='utf8') as output:
            cmd=[str(JAVA),'-Xms512M','-Xmx2560M','-Dfile.encoding=UTF-8',
                 '@libraries/net/minecraftforge/forge/1.20.1-47.4.10/win_args.txt','nogui']
            proc=subprocess.Popen(cmd,cwd=SERVER,stdin=subprocess.PIPE,stdout=output,stderr=subprocess.STDOUT,text=True,encoding='utf8')
            try:
                deadline=time.monotonic()+240
                while time.monotonic()<deadline:
                    text=log.read_text('utf8',errors='replace')
                    if proc.poll() is not None:raise RuntimeError('Server exited: '+text[-2400:])
                    if 'Done (' in text:break
                    time.sleep(1)
                else:raise TimeoutError('Readiness timeout')
                startup=time.monotonic()-started
                # Existing guilds re-check 256 chunk stamps after boot (about 13 seconds).
                # Let recovery finish before comparing persisted generation state.
                time.sleep(20)
                proc.stdin.write('list\nspark tps\nsave-all flush\n');proc.stdin.flush()
                time.sleep(2)
                proc.stdin.write('stop\n');proc.stdin.flush();proc.wait(timeout=90)
                text=log.read_text('utf8',errors='replace')
                if proc.returncode or 'All dimensions are saved' not in text:raise RuntimeError(text[-3000:])
                result.append(dict(cycle=n,exit=proc.returncode,startup_seconds=round(startup,2),saved_cleanly=True,
                    mods={p.name:hashlib.sha256(p.read_bytes()).hexdigest() for p in (SERVER/'mods').glob('*.jar')},
                    source_world_unchanged=tree_hashes(BASE/'world')==json.loads((QA/f'{LABEL}-source-world-before.json').read_text('utf8'))))
                write_json(QA/f'{LABEL}-server-cycles.json',{'cycles':result,'online_auth_tested':False,'world':'isolated copy of previous release'})
                print(json.dumps(result[-1]),flush=True)
            finally:
                if proc.poll() is None:
                    try:proc.stdin.write('stop\n');proc.stdin.flush();proc.wait(timeout=45)
                    except (OSError,subprocess.TimeoutExpired):proc.terminate();proc.wait(timeout=20)
    if not all(r['source_world_unchanged'] for r in result):raise ValueError('Source world changed')


if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--label',default='v22');parser.add_argument('--run',action='store_true')
    args=parser.parse_args()
    if not re.fullmatch(r'v22(?:-[a-z0-9-]+)?',args.label):raise ValueError('Invalid isolated QA label')
    LABEL=args.label;SERVER=ROOT/'mod'/('run-'+LABEL+'-server')
    if not args.run:prepare()
    run()
