"""Chapter, validate and package the music-free bilingual revision."""
from voice_promo import *
import zipfile
from PIL import ImageFont
info=json.loads((OUT/'配音与字幕时间线.json').read_text(encoding='utf8'))
p=OUT/'DuskRain-烟雨仙途-双语配音宣传片.mp4'
chapters=[{'start':0,'title':'制作介绍 / Production introduction'}];cursor=info['intro_seconds']
for clip,start,length,title,sub,opening,closing in PLAN:
    chapters.append({'start':cursor,'title':title or ('烟雨仙途 / DuskRain' if opening else '加入我们 / Join DuskRain')});cursor+=length
meta=';FFMETADATA1\ntitle=DuskRain - Bilingual narrated trailer\nartist=DuskRain\ncomment=Chinese AI narration with Chinese-English burned-in subtitles. No music.\n'
for i,ch in enumerate(chapters):
    end=chapters[i+1]['start'] if i+1<len(chapters) else info['total_seconds']
    meta+=f"[CHAPTER]\nTIMEBASE=1/1000\nSTART={round(ch['start']*1000)}\nEND={round(end*1000)}\ntitle={ch['title']}\n"
(WORK/'chapters.ffmeta').write_text(meta,encoding='utf8')
command(['-i',str(p),'-i',str(WORK/'chapters.ffmeta'),'-map','0:v','-map','0:a','-map_metadata','1','-map_chapters','1','-c','copy','-movflags','+faststart',str(WORK/'chaptered.mp4')],'chapters.log')
(WORK/'chaptered.mp4').replace(p)
(OUT/'宣传片章节.json').write_text(json.dumps(chapters,ensure_ascii=False,indent=2),encoding='utf8')

cn=ImageFont.truetype('C:/Windows/Fonts/msyh.ttc',38);en=ImageFont.truetype('C:/Windows/Fonts/segoeui.ttf',28)
assert all(cn.getlength(c['cn'])+len(c['cn'])<1780 and en.getlength(c['en'])<1780 for c in info['clips'])
assert all(a['end']<b['start'] for a,b in zip(info['clips'],info['clips'][1:]))
with wave.open(str(WORK/'narration.wav')) as f:
    data=np.frombuffer(f.readframes(f.getnframes()),dtype='<i2').reshape(-1,2);sample_rate=f.getframerate()
allowed=np.zeros(len(data),dtype=bool)
for c in info['clips']:allowed[max(0,round(c['start']*sample_rate)-2):math.ceil(c['end']*sample_rate)+2]=True
assert not data[~allowed].any(),'Unexpected audio outside the narration clips'

checks=[]
def stream_hash(file):return subprocess.check_output([FF,'-v','error','-i',str(file),'-map','0:v:0','-c','copy','-f','hash','-hash','sha256','-'],text=True).strip()
for file in sorted(OUT.glob('*.mp4')):
    probe=json.loads(subprocess.check_output([FP,'-v','error','-show_format','-show_streams','-show_chapters','-of','json',str(file)],text=True,encoding='utf8'))
    v=next(x for x in probe['streams'] if x['codec_type']=='video');audio=[x for x in probe['streams'] if x['codec_type']=='audio']
    assert (v['width'],v['height'],v['r_frame_rate'])==(1920,1080,'30/1')
    seconds=float(probe['format']['duration']);same=None
    if file==p:assert len(audio)==1 and abs(seconds-info['total_seconds'])<.1
    else:
        assert not audio
        old=OLD/(file.stem.removesuffix('-无配乐')+'.mp4');same=stream_hash(old)==stream_hash(file);assert same
    decode=subprocess.run([FF,'-v','error','-threads','4','-i',str(file),'-map','0:v:0','-map','0:a?','-f','null','-'],capture_output=True)
    assert decode.returncode==0 and not decode.stderr,decode.stderr.decode(errors='replace')
    with file.open('rb') as f:sha=hashlib.file_digest(f,'sha256').hexdigest()
    checks.append({'file':file.name,'duration':seconds,'width':1920,'height':1080,'fps':30,'audio_tracks':len(audio),'music':False,'original_video_packets_unchanged':same,'chapters':len(probe.get('chapters',[])),'full_decode_pass':True,'sha256':sha,'bytes':file.stat().st_size})
    print('PASS',file.name,seconds,flush=True)
result={'videos':checks,'subtitle_lines':len(info['clips']),'cn_en_caption_fit':True,'caption_overlap':False,'narration_only_wav_confirmed':True,'narration_peak_dbfs':float(20*np.log10(np.max(np.abs(data.astype(float)))/32768))}
(OUT/'验证记录.json').write_text(json.dumps(result,ensure_ascii=False,indent=2),encoding='utf8')
(ROOT/'docs/qa/voiced-film-validation.json').write_text(json.dumps(result,ensure_ascii=False,indent=2),encoding='utf8')

total=round(info['total_seconds']);intro=info['intro_seconds']
readme=f'''# DuskRain · 中英双语配音版

## 本次更新

- 宣传片新增 {intro:.1f} 秒开场，按要求显示“本视频全程由 GPT6-Astra 制作”，介绍原创仙城建筑、玩法、界面、开发、录制和剪辑。
- 中文 AI 合成男声解说；中文字幕在上、英文翻译在下。双语字幕烧录于底部独立区域，避免遮挡游戏 HUD，并附可修改 SRT 和中英配音稿。
- 完整保留原 3 分钟正片；新宣传片总长 {total//60}:{total%60:02d}。输出 1080p / 30 fps，内含播放器章节。
- 三份视频均移除了原背景音乐。宣传片仅有中文解说；两份长实录无音轨。长实录的视频压缩数据与上一版逐字节一致，没有因去配乐而损失画质。

## 文件

1. `DuskRain-烟雨仙途-双语配音宣传片.mp4`：制作介绍 + 玩法解说，中文配音、中英双语字幕。
2. `仙宫逐块建造-完整实录-无配乐.mp4`：1:03，仙宫单体从基础到完工。
3. `服务器玩法与主城导览-实录-无配乐.mp4`：8:34.7，较长玩法与场景导览；没有新增解说。
4. `中英双语字幕.srt`、`配音稿-中英对照.md`、`配音与字幕时间线.json`：可供继续修改。
5. `验证记录.json`：完整解码、时长、音轨、字幕排版及哈希记录。

旧版配乐成片仍保留于相邻 `DuskRain-films-20260916`，作为历史备份；请使用本目录的新版本。

## 制作说明

正文画面来自上一版实际 Minecraft 游戏录制。长版为功能导览，不是全部 24 项主线任务通关；建造回放仅为仙宫单体。Minecraft、Forge、光影及模组依赖保留各自作者署名，开场制作署名不替代这些基础资源的原作者。

配音使用 Microsoft 的 zh-CN-YunxiNeural 合成音色，通过 [edge-tts](https://github.com/rany2/edge-tts) 生成；未模仿或克隆真人声音。配音稿、时间线、字幕与剪辑脚本由本项目制作，未混入原配乐、麦克风或系统声音。
'''
(OUT/'README.md').write_text(readme,encoding='utf8')
archive=ROOT/'dist/DuskRain-双语配音宣传素材-20260916.zip'
with zipfile.ZipFile(archive,'w',zipfile.ZIP_DEFLATED) as z:
    for file in OUT.iterdir():
        if file.is_file():z.write(file,file.name,compress_type=zipfile.ZIP_STORED if file.suffix=='.mp4' else zipfile.ZIP_DEFLATED)
    for name in ('bilingual.ass','opening.ass','narration.wav'):
        z.write(WORK/name,'可编辑素材/'+name,compress_type=zipfile.ZIP_STORED if name.endswith('.wav') else zipfile.ZIP_DEFLATED)
    for name in ('voice_promo.py','finish_voiced_promo.py','edit_films.py'):z.write(ROOT/'tools/qa'/name,'制作脚本/'+name)
with zipfile.ZipFile(archive) as z:assert z.testzip() is None
print('PACKAGED',archive,archive.stat().st_size,flush=True)
