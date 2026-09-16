"""Opt-in QA harness clicks real Screen widgets and checks server-returned catalogue packets."""
from pathlib import Path
import json,time
ROOT=Path(__file__).resolve().parents[2]
RUN=ROOT/'mod/run-shop-hud-qa'
STATUS=RUN/'duskrain-director-status.json'
def status():
    try:return json.loads(STATUS.read_text(encoding='utf8'))
    except (OSError,ValueError):return {}
def command(text,predicate):
    started=time.time();(RUN/'duskrain-director.txt').write_text(text,encoding='utf8')
    while time.time()-started<12:
        s=status()
        if STATUS.stat().st_mtime>started+1 and predicate(s):return s
        time.sleep(.2)
    raise AssertionError((text,status()))
def catalogue(s):return s.get('menu',{}).get('catalog') or {}
def check_layout(s):
    w,h=s['gui'];widgets=s['widgets']
    for e in widgets:assert e['x']>=0 and e['y']>=0 and e['x']+e['width']<=w and e['y']+e['height']<=h,e
    for i,a in enumerate(widgets):
        for b in widgets[i+1:]:assert a['x']+a['width']<=b['x'] or b['x']+b['width']<=a['x'] or a['y']+a['height']<=b['y'] or b['y']+b['height']<=a['y'],(a,b)
report={'method':'Real client Screen.mouseClicked + C2S action + Brigadier + server catalogue + S2C decode','pages':[]}
def visit(kind,total):
    s=status();q=catalogue(s);assert q['kind']==kind and q['page']==0 and q['total']==total
    seen=set()
    for page in range(q['pages']):
        if page:s=command('ui_click 下页 →',lambda s:catalogue(s).get('kind')==kind and catalogue(s).get('page')==page)
        check_layout(s);es=s['menu']['entries'];assert len(es)<=8
        for e in es:assert e['action'] not in seen;seen.add(e['action'])
        report['pages'].append({'kind':kind,'page':page+1,'items':[e['action'] for e in es],'gui':s['gui']})
    assert len(seen)==total
    assert not next(w['active'] for w in s['widgets'] if w['label']=='下页 →')
    return s
try:
    s=visit('all',90)
    command('ui_click ← 上页',lambda s:catalogue(s).get('page')==10)
    command('ui_click 全部类别',lambda s:catalogue(s).get('kind')=='artifact')
    command('ui_click 法器',lambda s:catalogue(s).get('kind')=='robe')
    s=visit('robe',72)
    command('shot armor-page9',lambda s:catalogue(s).get('page')==8)
    label=s['menu']['entries'][0]['label']
    command('ui_click '+label,lambda s:s.get('menu',{}).get('title','').startswith('商品'))
    s=status();back=next(w['label'] for w in s['widgets'] if '目录' in w['label'] or '返回' in w['label'])
    command('ui_click '+back,lambda s:catalogue(s).get('kind')=='robe' and catalogue(s).get('page')==8)
    report['detail_return_preserves_filter_and_page']=True
    command('ui_click 全部部位',lambda s:catalogue(s).get('part')=='helmet' and catalogue(s).get('total')==18)
    command('ui_click 全部境界',lambda s:catalogue(s).get('realm')==0 and catalogue(s).get('total')==3)
    report['filters_passed']=True;report['passed']=True
finally:
    (ROOT/'docs/qa/native-shop-pages.json').write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf8')
print(json.dumps({'passed':report.get('passed',False),'page_count':len(report['pages'])}))
