"""Chinese narration, bilingual captions, and a credited opening; no background music."""
from pathlib import Path
import sys,json,asyncio,subprocess,shutil,math,wave,hashlib
import numpy as np
ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'.deps/voice-python'))
import edge_tts
from edit_films import PLAN,ass_time
OLD=ROOT/'dist/DuskRain-films-20260916'
OUT=ROOT/'dist/DuskRain-films-voiced-20260916';WORK=OUT/'work';VOICE=WORK/'voice'
VOICE.mkdir(parents=True,exist_ok=True)
FF=shutil.which('ffmpeg');FP=shutil.which('ffprobe')
INTRO=[
('本视频全程由 GPT6-Astra 制作。','This video was produced entirely by GPT6-Astra.','本视频全程由 GPT 六，Astra 制作。'),
('这是一座为 DuskRain 打造的原创中式仙城。','An original Chinese fantasy city, created for DuskRain.',''),
('建筑、玩法和界面，按照服主的构想设计。','Architecture, gameplay and interfaces, shaped by the server owner\'s vision.',''),
('开发、实机录制与剪辑，汇成眼前这段烟雨仙途。','Development, in-game recording and editing bring this misty world to life.','')]
# Relative start time within each existing scene; narration leaves breathing room.
BODY=[
[(.8,'欢迎来到 DuskRain，开启你的烟雨仙途。','Welcome to DuskRain. Begin your journey through mist and rain.')],
[(.7,'庭院连接问道堂、任务殿与商街，让日常服务近在身边。','Courtyards link the academy, quest hall and market, keeping services close.')],
[(.8,'一砖一瓦，从基础到梁柱，再到屋顶与灯火。','Brick by brick: foundations, beams, roofs and lights.'),(10,'这段施工，来自游戏里的真实建造回放。','This construction sequence was recorded inside the game.'),(19,'主城之外，浮空楼阁与山水相伴。','Beyond the city, floating pavilions rise over mountains and water.')],
[(.5,'溪谷丹坊，临水而立。沿着石阶，慢慢逛一逛。','Visit the riverside alchemy hall, and wander along its stone steps.')],
[(.6,'走进书院，在卷宗与灯火之间问道修行。','Enter the academy, where scrolls and lamplight frame the path of cultivation.')],
[(.7,'挑选法衣，搭配法器。按境界与部位筛选自己的行装。','Choose robes and artifacts. Filter your gear by cultivation stage and slot.')],
[(.5,'六章主线串起拜师、探索、制作与试炼。','Six story chapters connect apprenticeship, exploration, crafting and trials.')],
[(.7,'剑修，剑气破空。回旋剑斩与突进，衔接中距离的连击。','Sword cultivators chain ranged sword energy, sweeping slashes and dashes.')],
[(.7,'术修，离火开路，寒霜成阵，再以雷诀收势。','Spell cultivators open with fire, shape frost circles, and finish with lightning.')],
[(.7,'体修，以身体为根基，冲肩、震地，护体反击。','Body cultivators charge, shake the ground, and counter behind a protective guard.')],
[(.5,'听风铃，寻阵眼。沿着光柱，寻找下一道封印。','Listen for bells. Follow the light to find the next seal.')],
[(.5,'正确敲响风铃，阵眼点亮，引导你继续前行。','Ring the correct bell to light the node and reveal the way forward.')],
[(.6,'走入野外，认领住宅。青色边界标出自己的领地。','Claim a home in the wilderness. Cyan borders mark your own land.')],
[(.5,'来到宗务堂，与道友一起，开启帮派洞天。','Visit the guild hall and open a private realm with your companions.')],
[(.2,'留一方空地，共建宗门。','A blank canvas for your guild to build together.')],
[(.6,'当灯火照亮山城，这里就是归途。','When lanterns light the mountain city, this is the way home.')],
[(.5,'DuskRain，烟雨仙途。加入交流群：205255670。','DuskRain: Journey Through Mist and Rain. Join our community: 205255670.')]]

def command(args,log):
    with (WORK/log).open('w',encoding='utf8') as f:
        subprocess.run([FF,'-hide_banner','-y','-filter_threads','2',*args],cwd=WORK,stdout=f,stderr=subprocess.STDOUT,check=True)
def duration(p):return float(subprocess.check_output([FP,'-v','error','-show_entries','format=duration','-of','default=nw=1:nk=1',str(p)],text=True))
def voice_text(cn):
    return cn.replace('205255670','二零五，二五五，六七零')
async def synthesize():
    texts=[spoken or voice_text(cn) for cn,en,spoken in INTRO]+[voice_text(cn) for group in BODY for off,cn,en in group]
    gate=asyncio.Semaphore(1)
    async def one(i,text):
        p=VOICE/f'{i:02d}.mp3';key=hashlib.sha256(text.encode()).hexdigest();stamp=p.with_suffix('.sha256')
        if p.exists() and stamp.exists() and stamp.read_text()==key:return
        async with gate:
            for attempt in range(4):
                try:
                    await edge_tts.Communicate(text,'zh-CN-YunxiNeural',rate='-4%',receive_timeout=40).save(str(p),str(p.with_suffix('.jsonl')))
                    break
                except edge_tts.exceptions.NoAudioReceived:
                    if attempt==3:raise
                    await asyncio.sleep(2+attempt)
            stamp.write_text(key);print('VOICE',i,round(duration(p),2),flush=True)
            await asyncio.sleep(.5)
    await asyncio.gather(*(one(i,t) for i,t in enumerate(texts)))

CAPTION_HEADER='''[Script Info]
ScriptType: v4.00+
PlayResX: 1920
PlayResY: 1080
WrapStyle: 2
ScaledBorderAndShadow: yes
[V4+ Styles]
Format: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, OutlineColour, BackColour, Bold, Italic, Underline, StrikeOut, ScaleX, ScaleY, Spacing, Angle, BorderStyle, Outline, Shadow, Alignment, MarginL, MarginR, MarginV, Encoding
Style: CN,Microsoft YaHei,38,&H00E8F0F1,&H000000FF,&H00121E1C,&H00121E1C,0,0,0,0,100,100,1,0,1,0,0,8,70,70,0,1
Style: EN,Segoe UI,28,&H00BDD0CF,&H000000FF,&H00121E1C,&H00121E1C,0,0,0,0,100,100,0,0,1,0,0,8,70,70,0,1
Style: Title,KaiTi,64,&H00E8F0F1,&H000000FF,&H50203028,&H70203028,0,0,0,0,100,100,3,0,1,1,1,5,70,70,0,1
[Events]
Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text
'''
def event(style,text,start,end,override=''):
    return f'Dialogue: 0,{ass_time(start)},{ass_time(end)},{style},,0,0,0,,{{\\fad(140,180){override}}}{text}\n'
def srt_time(t):
    ms=round(t*1000);return f'{ms//3600000:02d}:{ms//60000%60:02d}:{ms//1000%60:02d},{ms%1000:03d}'
def prepare():
    clips=[];cursor=.8
    for i,(cn,en,spoken) in enumerate(INTRO):
        d=duration(VOICE/f'{i:02d}.mp3');clips.append(dict(index=i,start=cursor,duration=d,cn=cn,en=en,speed=1));cursor+=d+.65
    intro=math.ceil((cursor+.5)*30)/30
    cursor=intro;index=len(INTRO)
    for spec,group in zip(PLAN,BODY):
        length=spec[2]
        for j,(off,cn,en) in enumerate(group):
            d=duration(VOICE/f'{index:02d}.mp3');next_start=group[j+1][0] if j+1<len(group) else length-.3
            allowed=next_start-off-.2;speed=max(1,d/allowed)
            assert speed<1.45,(index,speed)
            clips.append(dict(index=index,start=cursor+off,duration=d/speed,cn=cn,en=en,speed=speed));index+=1
        cursor+=length
    total=cursor
    pcm=np.zeros((math.ceil(total*48000),2),dtype=np.float32)
    ass=CAPTION_HEADER;srt=[]
    for n,c in enumerate(clips):
        audio=VOICE/f"{c['index']:02d}.mp3"
        data=subprocess.check_output([FF,'-v','error','-i',str(audio),'-af',f"atempo={c['speed']:.8f},highpass=f=70",'-f','f32le','-ac','2','-ar','48000','pipe:1'])
        arr=np.frombuffer(data,dtype='<f4').reshape(-1,2);start=round(c['start']*48000);pcm[start:start+len(arr)]+=arr
        end=c['start']+len(arr)/48000;c['end']=end
        ass+=event('CN',c['cn'],c['start'],end,r'\pos(960,952)')+event('EN',c['en'],c['start'],end,r'\pos(960,1006)')
        srt.append(f"{n+1}\n{srt_time(c['start'])} --> {srt_time(end)}\n{c['cn']}\n{c['en']}\n")
    peak=float(np.max(np.abs(pcm)));pcm*=.87/max(peak,.001)
    with wave.open(str(WORK/'narration.wav'),'wb') as f:
        f.setparams((2,2,48000,0,'NONE','not compressed'));f.writeframes((np.clip(pcm,-1,1)*32767).astype('<i2').tobytes())
    (OUT/'中英双语字幕.srt').write_text('\n'.join(srt),encoding='utf-8-sig')
    (WORK/'bilingual.ass').write_text(ass,encoding='utf-8-sig')
    title=CAPTION_HEADER
    split=clips[1]['start']
    title+=event('Title','DUSKRAIN  /  烟雨仙途',.4,intro-.2,r'\pos(960,270)\fs30\fnMicrosoft YaHei')
    title+=event('Title','本视频全程由 GPT6-Astra 制作',.6,split,r'\pos(960,440)\fs60\fnMicrosoft YaHei')
    title+=event('Title','THIS VIDEO WAS PRODUCED ENTIRELY BY GPT6-ASTRA',.9,split,r'\pos(960,530)\fs26\fnSegoe UI\fsp1')
    title+=event('Title','从一座山岛，到一方仙城',split,intro-.3,r'\pos(960,430)\fs76')
    title+=event('Title','FROM A MOUNTAIN ISLAND TO A CULTIVATION CITY',split+.2,intro-.3,r'\pos(960,525)\fs26\fnSegoe UI\fsp1')
    title+=event('Title','原创建筑  /  修仙玩法  /  界面设计  /  实机录制与剪辑',split+.3,intro-.3,r'\pos(960,625)\fs28\fnMicrosoft YaHei')
    title+=event('Title','Minecraft Java 1.20.1 · Forge 47.4.10 · AI 合成配音 / AI-generated voice',.8,intro-.4,r'\pos(960,828)\fs22\fnMicrosoft YaHei\fsp0')
    (WORK/'opening.ass').write_text(title,encoding='utf-8-sig')
    info={'intro_seconds':intro,'total_seconds':total,'voice':'zh-CN-YunxiNeural','music':False,'clips':clips}
    (OUT/'配音与字幕时间线.json').write_text(json.dumps(info,ensure_ascii=False,indent=2),encoding='utf8')
    (OUT/'配音稿-中英对照.md').write_text('# DuskRain 配音稿\n\n中文 AI 合成配音；英文为翻译字幕。无背景音乐。\n\n'+'\n\n'.join(c['cn']+'\n\n'+c['en'] for c in clips)+'\n',encoding='utf8')
    print('TIMELINE',intro,total,flush=True)
    return info

def render(info):
    intro=info['intro_seconds'];total=info['total_seconds']
    raw=ROOT/'mod/run-film-qa/duskrain-films/09-night.mp4'
    command(['-stream_loop','-1','-i',str(raw),'-t',str(intro),'-vf',"fps=30,scale=1920:1080,drawbox=x=0:y=0:w=iw:h=ih:color=0x071e20@0.72:t=fill,drawbox=x=280:y=698:w=1360:h=2:color=0xB6A369@0.7:t=fill,subtitles=opening.ass,fade=t=in:st=0:d=0.6,fade=t=out:st="+str(intro-.45)+':d=0.45','-an','-c:v','libx264','-preset','fast','-threads','4','-crf','18','-pix_fmt','yuv420p',str(WORK/'opening.mp4')],'opening.log')
    old=OLD/'DuskRain-烟雨仙途-3分钟宣传片.mp4'
    filters="[0:v]scale=1664:936,pad=1920:1080:128:0:color=0x101E1D,setsar=1[v0];[1:v]scale=1664:936,pad=1920:1080:128:0:color=0x101E1D,setsar=1[v1];[v0][v1]concat=n=2:v=1:a=0,drawbox=x=128:y=938:w=1664:h=1:color=0xB6A369@0.55:t=fill,subtitles=bilingual.ass[v]"
    command(['-i',str(WORK/'opening.mp4'),'-i',str(old),'-i',str(WORK/'narration.wav'),'-filter_complex_threads','2','-filter_complex',filters,'-map','[v]','-map','2:a','-map_metadata','-1','-map_chapters','-1','-c:v','libx264','-preset','fast','-threads','4','-crf','18','-pix_fmt','yuv420p','-c:a','aac','-b:a','192k','-af','loudnorm=I=-18:TP=-1.5:LRA=9','-t',str(total),'-movflags','+faststart',str(OUT/'DuskRain-烟雨仙途-双语配音宣传片.mp4')],'voiced-promo.log')
    for name in ['仙宫逐块建造-完整实录','服务器玩法与主城导览-实录']:
        command(['-i',str(OLD/(name+'.mp4')),'-map','0:v:0','-map_metadata','0','-map_chapters','0','-c:v','copy','-an','-movflags','+faststart',str(OUT/(name+'-无配乐.mp4'))],name+'-silent.log')
    print('RENDER COMPLETE',flush=True)

if __name__=='__main__':
    if 'voice' in sys.argv:asyncio.run(synthesize());prepare()
    if 'render' in sys.argv:render(json.loads((OUT/'配音与字幕时间线.json').read_text(encoding='utf8')))
