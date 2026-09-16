from pathlib import Path
import sys,json,subprocess,shutil,os
from edit_films import PLAN,OUT,WORK
FF=shutil.which('ffmpeg');FP=shutil.which('ffprobe')
def escape(s):return s.replace('\\','\\\\').replace('=','\\=').replace(';','\\;').replace('#','\\#').replace('\n',' ')
for p in OUT.glob('*.mp4'):
    duration=float(subprocess.check_output([FP,'-v','error','-show_entries','format=duration','-of','default=nw=1:nk=1',str(p)],text=True))
    if '3分钟' in p.name:
        chapters=[];offset=0
        for clip,start,length,title,sub,opening,closing in PLAN:
            chapters.append({'start_seconds':offset,'title':title or ('烟雨仙途 · DuskRain' if opening else '加入 DuskRain · 205255670')});offset+=length
    else:chapters=json.loads(p.with_suffix(p.suffix+'.chapters.json').read_text(encoding='utf8'))
    meta=';FFMETADATA1\ntitle='+escape(p.stem)+'\nartist=DuskRain\ncomment=Actual Minecraft capture; original synthesized music.\n'
    for i,ch in enumerate(chapters):
        end=chapters[i+1]['start_seconds'] if i+1<len(chapters) else duration
        meta+=f"[CHAPTER]\nTIMEBASE=1/1000\nSTART={int(ch['start_seconds']*1000)}\nEND={int(end*1000)}\ntitle={escape(ch['title'])}\n"
    mf=WORK/(p.stem+'.ffmeta');mf.write_text(meta,encoding='utf8')
    temp=WORK/(p.stem+'-chaptered.mp4')
    subprocess.run([FF,'-v','error','-y','-i',str(p),'-i',str(mf),'-map_metadata','1','-map_chapters','1','-map','0','-c','copy','-movflags','+faststart',str(temp)],check=True)
    os.replace(temp,p)
    lines=['# '+p.stem,'']
    for ch in chapters:
        t=int(ch['start_seconds']);lines.append(f"- {t//60:02d}:{t%60:02d} {ch['title']}")
    (OUT/(p.stem+'-章节.md')).write_text('\n'.join(lines)+'\n',encoding='utf8')
    print('CHAPTERS',p.name,len(chapters),flush=True)
