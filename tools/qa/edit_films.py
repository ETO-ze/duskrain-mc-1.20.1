"""Reproducible editorial cuts from the actual local Minecraft recordings."""
from pathlib import Path
import json,subprocess,shutil,sys,time
ROOT=Path(__file__).resolve().parents[2];RAW=ROOT/'mod/run-film-qa/duskrain-films'
OUT=ROOT/'dist/DuskRain-films-20260916';WORK=OUT/'edit';WORK.mkdir(parents=True,exist_ok=True)
FF=shutil.which('ffmpeg');FP=shutil.which('ffprobe')

def run(args,log):
    with (WORK/log).open('w',encoding='utf8') as f:
        subprocess.run([FF,'-hide_banner','-y','-filter_threads','2',*args],stdout=f,stderr=subprocess.STDOUT,check=True,cwd=WORK)

def seconds(p):return float(subprocess.check_output([FP,'-v','error','-show_entries','format=duration','-of','default=nw=1:nk=1',str(p)],text=True))
def ass_time(t):
    s=int(t);return f'{s//3600}:{s//60%60:02d}:{s%60:02d}.{int(round((t-s)*100)):02d}'
HEADER='''[Script Info]
ScriptType: v4.00+
PlayResX: 1920
PlayResY: 1080
ScaledBorderAndShadow: yes
[V4+ Styles]
Format: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, OutlineColour, BackColour, Bold, Italic, Underline, StrikeOut, ScaleX, ScaleY, Spacing, Angle, BorderStyle, Outline, Shadow, Alignment, MarginL, MarginR, MarginV, Encoding
Style: Main,KaiTi,56,&H00E7F1F3,&H000000FF,&H70233122,&H90233122,0,0,0,0,100,100,5,0,1,1,2,7,80,80,74,1
Style: Small,Microsoft YaHei,25,&H00C4D7DC,&H000000FF,&H80152220,&H80152220,0,0,0,0,100,100,2,0,1,1,1,7,84,80,146,1
Style: Brand,Constantia,24,&H00D4DDE1,&H000000FF,&H90152220,&H90152220,0,0,0,0,100,100,4,0,1,1,1,1,84,80,34,1
[Events]
Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text
'''
def subtitles(path,title,subtitle,duration,opening=False,closing=False):
    lines=[HEADER]
    def line(style,text,start=0,end=duration):lines.append(f'Dialogue: 0,{ass_time(start)},{ass_time(end)},{style},,0,0,0,,{{\\fad(650,650)}}{text}\n')
    if opening:
        line('Main',r'{\an5\pos(960,444)\fs108}烟 雨 仙 途',1,duration-1)
        line('Small',r'{\an5\pos(960,552)\fs44\fnConstantia\fsp8}DuskRain',1.4,duration-1)
        line('Small',r'{\an5\pos(960,621)\fs27}从一座山岛，启程入世。',2,duration-1)
    elif closing:
        line('Main',r'{\an5\pos(960,402)\fs96}来烟雨中，写你的仙途。',0,duration)
        line('Small',r'{\an5\pos(960,518)\fs43}DuskRain · 烟雨仙途',0,duration)
        line('Small',r'{\an5\pos(960,592)\fs44\b1}加入交流群  205255670',0,duration)
        line('Small',r'{\an5\pos(960,668)\fs23}Minecraft Java 1.20.1 · Forge 47.4.10',0,duration)
    else:
        line('Main',title);line('Small',subtitle)
    line('Brand','DUSKRAIN  /  烟雨仙途')
    path.write_text(''.join(lines),encoding='utf-8-sig')

# 180 seconds, selected playable systems; no fabricated crowds or combat outcomes.
PLAN=[
('01-palace',1,12,'','',True,False),
('02-courtyard',2,10,'01  山岛之间','青瓦白墙，廊桥相连；从主城步入烟雨。',False,False),
('27-construction-complete',0,28,'02  一砖一瓦，云阙初成','实际游戏逐块施工回放 · 此段加速展示',False,False),
('03-water',2,8,'03  溪畔烟火','丹坊、庭院与临水街市。',False,False),
('06-study',3,7,'04  问道入门','书院、卷宗与案几，踏上六境仙途。',False,False),
('15-outfits',4,12,'05  以灵石，换一身行装','法衣按境界与部位筛选；商店服务端校验。',False,False),
('13-quests',3,8,'06  六章纪事','跟随主线目标，探索、炼制、解阵与试炼。',False,False),
('18-sword',0,12,'07  剑修 · 剑气破空','破空 / 回风 / 追星 · 练功场实录',False,False),
('19-mage',0,12,'08  术修 · 符阵引雷','离火灵弹 / 寒霜符阵 / 九霄雷诀',False,False),
('20-body',0,10,'09  体修 · 撼岳守身','冲肩突进、震地与金刚护体。',False,False),
('26-trial-hint',2,9,'10  听风铃，寻阵眼','青竹迷踪 · 寻找下一道发光阵眼',False,False),
('28-bell-interaction',17,9,'10  一鸣点亮，循光前行','实际交互 · 正确敲击后点亮风铃',False,False),
('22-claim-border',2,10,'11  野外立足','住宅边界可见；野外允许 PVP。',False,False),
('08-guildhall',2,8,'12  同袍共建，宗门洞天','由宗务堂启程，前往专属建设空间。',False,False),
('24-guild',2,4,'12  建设你们的宗门','256 × 256 空地；留待道友共建。',False,False),
('09-night',3,11,'13  万家灯火，等你归来','主城、安全归途与自由探索。',False,False),
('01-palace',12,10,'','',False,True)]

def promo(start_index=0):
    outputs=[]
    for i,(clip,start,dur,title,sub,opening,closing) in enumerate(PLAN):
        if i<start_index:
            dest=WORK/f'promo-{i:02d}.mp4';assert dest.exists();outputs.append(dest);continue
        src=RAW/(clip+'.mp4')
        until=time.time()+900
        while not src.exists() or not src.with_suffix('.json').exists():
            if time.time()>until:raise RuntimeError('Missing completed clip '+clip)
            time.sleep(2)
        actual=seconds(src);take=min(dur,actual-start-.1)
        if clip=='27-construction-complete':start=0;take=actual-.2
        subfile=WORK/f'promo-{i:02d}.ass';subtitles(subfile,title,sub,dur,opening,closing)
        factor=dur/take
        timing=f'setpts={factor}*(PTS-STARTPTS)'
        if clip in ['18-sword','19-mage','20-body']:
            start=0;take=17
            ranges='between(t,1.5,4.5)+between(t,5.5,9.5)+between(t,11.5,16.5)' if clip!='20-body' else 'between(t,1.5,4.5)+between(t,5.5,8.5)+between(t,11.5,15.5)'
            timing=f"select='{ranges}',setpts=N/(30*TB)"
        layout='crop=1920:1040:0:40,scale=1536:832,pad=1920:1080:192:210:color=0x152c2d' if clip in ['15-outfits','13-quests'] else 'crop=1920:1020:0:60,scale=1920:1080' if clip in ['18-sword','19-mage','20-body','28-bell-interaction'] else 'scale=1920:1080'
        filters=f"{timing},fps=30,{layout},setsar=1,eq=saturation=1.04:contrast=1.015,subtitles=promo-{i:02d}.ass,fade=t=in:st=0:d=0.30,fade=t=out:st={dur-.3}:d=0.30"
        dest=WORK/f'promo-{i:02d}.mp4';run(['-ss',str(start),'-t',str(take),'-i',str(src),'-vf',filters,'-t',str(dur),'-an','-c:v','libx264','-preset','fast','-threads','4','-crf','19','-pix_fmt','yuv420p',str(dest)],f'promo-{i:02d}.log');outputs.append(dest)
        print('Edited',clip,flush=True)
    listing=WORK/'promo-concat.txt';listing.write_text(''.join("file '"+p.name+"'\n" for p in outputs),encoding='utf8')
    run(['-f','concat','-safe','0','-i',str(listing),'-i',str(OUT/'烟雨序-原创配乐.wav'),'-t','180','-map','0:v','-map','1:a','-c:v','copy','-af','afade=t=out:st=173:d=7,loudnorm=I=-18:TP=-1.5:LRA=10','-c:a','aac','-b:a','192k','-movflags','+faststart',str(OUT/'DuskRain-烟雨仙途-3分钟宣传片.mp4')],'promo-final.log')
    (OUT/'宣传片分镜.json').write_text(json.dumps(PLAN,ensure_ascii=False,indent=2),encoding='utf8')

def long_video(names,filename):
    shots=json.loads((ROOT/'docs/qa/film-shot-log.json').read_text(encoding='utf8'));descs={x['name']:x['description'] for x in shots}
    files=[];chapter=[];offset=0
    for i,name in enumerate(names):
        src=RAW/(name+'.mp4');start=17 if name=='28-bell-interaction' else 0;dur=10 if start else seconds(src);key=filename[:3]+f'-{i:02d}';subtitles(WORK/(key+'.ass'),descs.get(name,name),'实际游戏录制 · 测试角色演示',dur)
        layout='crop=1920:1040:0:40,scale=1536:832,pad=1920:1080:192:210:color=0x152c2d,' if name in ['12-menu','13-quests','14-daily','15-outfits','16-elixir','17-treasure','21-claim-menu','23-guild-menu','25-trial-menu'] else 'crop=1920:1020:0:60,scale=1920:1080,' if name in ['18-sword','19-mage','20-body','28-bell-interaction'] else ''
        dest=WORK/(key+'.mp4');run(['-ss',str(start),'-t',str(dur),'-i',str(src),'-vf',f'{layout}subtitles={key}.ass','-an','-c:v','libx264','-preset','veryfast','-threads','4','-crf','20','-pix_fmt','yuv420p',str(dest)],key+'.log');files.append(dest);chapter.append({'start_seconds':round(offset,2),'title':descs.get(name,name)});offset+=dur
    listing=WORK/(filename[:3]+'-concat.txt');listing.write_text(''.join("file '"+p.name+"'\n" for p in files),encoding='utf8')
    run(['-f','concat','-safe','0','-i',str(listing),'-stream_loop','-1','-i',str(OUT/'烟雨序-原创配乐.wav'),'-t',str(offset),'-map','0:v','-map','1:a','-c:v','copy','-af',f'volume=0.6,afade=t=out:st={offset-5}:d=5','-c:a','aac','-b:a','160k','-movflags','+faststart',str(OUT/filename)],filename[:3]+'-final.log')
    (OUT/(filename+'.chapters.json')).write_text(json.dumps(chapter,ensure_ascii=False,indent=2),encoding='utf8')
    print('Completed',filename,offset,flush=True)

if __name__=='__main__':
    if sys.argv[-1]=='promo':promo(int(sys.argv[1]) if len(sys.argv)>2 else 0)
    elif sys.argv[-1]=='construction':long_video(['27-construction-complete'],'仙宫逐块建造-完整实录.mp4')
    elif sys.argv[-1]=='tour':
        names=['01-palace','02-courtyard','12-menu','13-quests','14-daily','15-outfits','16-elixir','17-treasure','18-sword','19-mage','20-body','25-trial-menu','26-trial-hint','28-bell-interaction','21-claim-menu','22-claim-border','23-guild-menu','24-guild','03-water','05-market','06-study','07-inn','08-guildhall','09-night','10-pavilion']
        long_video(names,'服务器玩法与主城导览-实录.mp4')
