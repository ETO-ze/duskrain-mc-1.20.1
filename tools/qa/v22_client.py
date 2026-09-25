"""Launch a separate native client using installed PCL libraries; never writes to the user's instance."""
import argparse,json,os,re,shutil,subprocess,sys,uuid
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parents[1]))
from mod_stack import ROOT,locked,selected,obtain,write_json

MINECRAFT=Path('E:/.minecraft')
BASE=MINECRAFT/'versions/DuskRain-1.20.1'
NAME='DuskRain-1.20.1'


def allowed(entry):
    rules=entry.get('rules')
    if not rules:return True
    result=False
    for rule in rules:
        os_rule=rule.get('os',{})
        if os_rule.get('name','windows')=='windows' and os_rule.get('arch','x86_64') in ('x86_64','amd64') and not rule.get('features'):
            result=rule['action']=='allow'
    return result


def prepare(game,baseline):
    if game.exists():return
    game.mkdir(parents=True)
    for folder in ('config','resourcepacks','shaderpacks','CustomSkinLoader'):
        if (BASE/folder).exists():
            shutil.copytree(BASE/folder,game/folder,ignore=shutil.ignore_patterns('caches','ProfileCache','logs','backups'))
    if (BASE/'options.txt').exists():shutil.copy2(BASE/'options.txt',game/'options.txt')
    (game/'mods').mkdir()
    if baseline:
        for p in (BASE/'mods').glob('*.jar'):shutil.copy2(p,game/'mods'/p.name)
    else:
        for item in selected(locked(),'client'):
            p,_=obtain(item);shutil.copy2(p,game/'mods'/p.name)
        jar=ROOT/'mod/build/libs/duskrain-2.2.0-preview.jar';shutil.copy2(jar,game/'mods'/jar.name)
    shutil.copytree(ROOT/'dist/DuskRain-2.1.0-preview/server/world',game/'saves/DuskRainRemake')


def launch(game,join,baseline,online=False):
    prepare(game,baseline)
    version=json.loads((BASE/(NAME+'.json')).read_text('utf8'))
    libs=[l for l in version['libraries'] if allowed(l)]
    paths=[MINECRAFT/'libraries'/l['downloads']['artifact']['path'] for l in libs if 'artifact' in l.get('downloads',{})]
    if not all(p.exists() for p in paths):raise ValueError('Missing installed launcher library')
    cp=os.pathsep.join(str(p) for p in paths+[BASE/(NAME+'.jar')])
    values={'auth_player_name':'DuskRainDirector','version_name':NAME,'game_directory':str(game),
        'assets_root':str(MINECRAFT/'assets'),'assets_index_name':'duskrain-5',
        'auth_uuid':'6e93fc7d8abf3d21a96518b49e686b0b','auth_access_token':'0','clientid':'','auth_xuid':'',
        'user_type':'legacy','version_type':'DuskRain-QA','natives_directory':str(BASE/'natives-duskrain-qa'),
        'launcher_name':'DuskRain-QA','launcher_version':'2.2','classpath':cp,'classpath_separator':os.pathsep,
        'library_directory':str(MINECRAFT/'libraries'),'resolution_width':'1280','resolution_height':'720'}
    if online:
        if join!='nbc.rainplay.cn:42741':raise ValueError('Authenticated QA is restricted to the configured DuskRain server')
        import urllib.request
        account=json.loads((ROOT/'.private/skin-admin-account.json').read_text('utf8'))
        body=json.dumps({'username':account['username'],'password':account['password'],
            'agent':{'name':'Minecraft','version':1},'clientToken':str(uuid.uuid4()),'requestUser':True}).encode()
        req=urllib.request.Request('https://skin.duskrain.cn/auth/authenticate',body,{'Content-Type':'application/json'})
        with urllib.request.urlopen(req,timeout=30) as response:auth=json.load(response)
        if auth['selectedProfile']['id'].replace('-','')!=account['player_uuid'].replace('-',''):
            raise ValueError('Authenticated UUID differs; refusing to launch another player')
        values.update(auth_player_name=auth['selectedProfile']['name'],auth_uuid=auth['selectedProfile']['id'],
                      auth_access_token=auth['accessToken'])
    def expand(s):return re.sub(r'\$\{([^}]+)\}',lambda m:values[m[1]],s)
    def args(items):
        result=[]
        for item in items:
            if isinstance(item,str):result.append(expand(item))
            elif allowed(item):
                value=item['value'];result.extend(expand(x) for x in (value if isinstance(value,list) else [value]))
        return result
    vm=['-Xms512M','-Xmx3584M','-Dfile.encoding=UTF-8','-Dduskrain.director=true']
    if online:
        agent=ROOT/'.deps/authlib-injector-1.2.8.jar'
        if not agent.is_file():raise ValueError('Existing verified authlib injector is required')
        vm.append('-javaagent:'+str(agent)+'=https://skin.duskrain.cn/authlib-injector')
    if join:vm.append('-Dduskrain.join='+join)
    vm+=args(version['arguments']['jvm'])
    log=version.get('logging',{}).get('client')
    if log:vm.append(log['argument'].replace('${path}',str(MINECRAFT/'assets/log_configs'/log['file']['id'])))
    command=vm+[version['mainClass']]+args(version['arguments']['game'])+['--width','1280','--height','720']
    argfile=ROOT/'.private/v22-online-launch.args' if online else game/'qa-launch.args'
    argfile.write_text('\n'.join('"'+a.replace('\\','\\\\').replace('"','\\"')+'"' for a in command),encoding='utf8')
    with (game/'qa-console.log').open('w',encoding='utf8') as log:
        proc=subprocess.Popen([str(BASE/'java17/bin/javaw.exe'),'@'+str(argfile)],cwd=game,stdout=log,stderr=subprocess.STDOUT,
                              creationflags=subprocess.CREATE_NO_WINDOW)
    write_json(game/'qa-process.json',dict(pid=proc.pid,baseline=baseline,join=join,
        authentication='DuskRain HTTPS; UUID verified' if online else 'offline isolated QA only'))
    print('Isolated native client PID',proc.pid,'directory',game)


if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--baseline',action='store_true');parser.add_argument('--join',default='')
    parser.add_argument('--online',action='store_true')
    a=parser.parse_args();launch(ROOT/('mod/run-v22-client-a' if a.baseline else 'mod/run-v22-client-b'),a.join,a.baseline,a.online)
