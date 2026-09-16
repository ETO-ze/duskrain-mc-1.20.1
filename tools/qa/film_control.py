"""Local opt-in director control, always targets the isolated film world."""
from pathlib import Path
import json,time,math
ROOT=Path(__file__).resolve().parents[2]
RUN=ROOT/'mod/run-film-qa'
MANIFEST=ROOT/'docs/qa/film-shot-log.json'

def status():
    return json.loads((RUN/'duskrain-director-status.json').read_text(encoding='utf8'))

def send(text,wait=1.3):
    (RUN/'duskrain-director.txt').write_text(text,encoding='utf8')
    time.sleep(wait)

def wait_ready():
    start=time.time()
    while time.time()-start<40:
        if not status().get('cinemaEncoding'):return
        time.sleep(.5)
    raise RuntimeError('Encoder still busy')

def begin(name):
    wait_ready();send('cinema record '+name)
    if not status().get('cinemaRecording'):
        time.sleep(1.2)
        assert status().get('cinemaRecording'),status()

def end(name,description):
    send('cinema stop');wait_ready()
    meta=json.loads((RUN/'duskrain-films'/f'{name}.json').read_text(encoding='utf8'))
    assert meta['encoderExit']==0 and meta['width']==1920 and meta['height']==1080,meta
    shots=json.loads(MANIFEST.read_text(encoding='utf8')) if MANIFEST.exists() else []
    shots.append(dict(name=name,description=description,**meta))
    MANIFEST.write_text(json.dumps(shots,ensure_ascii=False,indent=2),encoding='utf8')
    print(f"RECORDED {name}: {meta['frames']/30:.1f}s",flush=True)

def orient(x,y,z,tx,ty,tz):
    return [x,y,z,math.degrees(math.atan2(tz-z,tx-x))-90,-math.degrees(math.atan2(ty-y,math.hypot(tx-x,tz-z)))]

def scene(name,description,a,b,seconds=16,dimension='duskrain:city',clock=6000):
    send('close\ncinema off\ncmd gamemode spectator\ncmd execute in '+dimension+' run tp @s '+' '.join(map(str,a))+'\ncmd time set '+str(clock)+'\nhidehud',4)
    begin(name)
    send('cinema path '+' '.join(map(str,a+b+[seconds])))
    time.sleep(seconds)
    end(name,description)

def ui(name,description,commands,seconds=14):
    send('close\ncinema off\nshowhud\n'+commands,3)
    begin(name);time.sleep(seconds);end(name,description);send('close')

if __name__=='__main__':
    scene('01-palace','浮空仙宫近景',orient(78,178,-104,0,173,-190),orient(42,180,-101,0,173,-190),22)
    scene('02-courtyard','中央庭院与青瓦楼阁',orient(52,108,65,0,83,-12),orient(30,103,48,0,83,-12),20)
    scene('03-water','溪谷丹坊与临水景致',orient(-120,91,64,-79,84,29),orient(-112,87,40,-79,83,29),20)
    scene('04-island','问劫浮岛与步桥',orient(120,147,-61,168,143,-118),orient(112,151,-81,168,143,-118),20)
    scene('05-market','听潮商街',orient(-40,81,84,-33,81,61),orient(-33,81,77,-29,81,61),18)
    scene('06-study','问道书院室内',orient(-39,83,-1,-34,83,-18),orient(-35,83,-7,-28,83,-18),18)
    scene('07-inn','枕雨客栈内饰',orient(34,76,73,42,77,64),orient(34,76,67,43,77,61),18)
    scene('08-guildhall','宗务堂与传送阁',orient(106,91,259,68,77,219),orient(95,89,249,68,77,219),20)
    scene('09-night','烟雨主城夜景',orient(40,105,101,0,86,12),orient(25,105,82,0,86,12),24,clock=13500)
    scene('10-pavilion','浮空桥与灯带',orient(-98,134,-141,-63,140,-152),orient(-85,137,-147,-58,141,-155),20,clock=13500)
    send('cmd time set 6000\ncmd dr build speed 160\ncmd dr build start palace',3)
    # Reset is intentionally retained in raw footage; editorial trim starts at foundation.
    send('cmd dr build camera off\ncinema off\ncmd execute in duskrain:construction run tp @s 68 202 -99 143 22\nhidehud',5)
    log=ROOT/'docs/qa/film-client.log';marker=log.stat().st_size
    begin('11-construction')
    started=time.time()
    while time.time()-started<300:
        time.sleep(3)
        with log.open('rb') as f:f.seek(marker);tail=f.read().decode('utf8',errors='replace')
        if 'DUSKRAIN_BUILD_COMPLETE' in tail:time.sleep(8);break
    else:raise RuntimeError('Construction not completed within capture budget')
    end('11-construction','独立演示维度内真实逐块建造仙宫')
