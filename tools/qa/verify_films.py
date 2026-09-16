"""Validate complete encoded deliverables, not just sample screenshots."""
from pathlib import Path
import subprocess,json,hashlib,shutil
ROOT=Path(__file__).resolve().parents[2]
OUT=ROOT/'dist/DuskRain-films-20260916'
QA=ROOT/'docs/qa'
FF=shutil.which('ffmpeg');FP=shutil.which('ffprobe')
result=[]
for p in sorted(OUT.glob('*.mp4')):
    info=json.loads(subprocess.check_output([FP,'-v','error','-show_format','-show_streams','-of','json',str(p)],text=True,encoding='utf8'))
    v=next(x for x in info['streams'] if x['codec_type']=='video')
    a=next(x for x in info['streams'] if x['codec_type']=='audio')
    assert (v['width'],v['height'],v['r_frame_rate'])==(1920,1080,'30/1'),v
    duration=float(info['format']['duration'])
    if '3分钟' in p.name:assert abs(duration-180)<.15,duration
    decoded=subprocess.run([FF,'-v','error','-threads','4','-i',str(p),'-map','0:v','-map','0:a','-f','null','-'],capture_output=True)
    assert decoded.returncode==0 and not decoded.stderr,decoded.stderr.decode(errors='replace')
    result.append({'file':p.name,'seconds':duration,'resolution':[1920,1080],'fps':v['r_frame_rate'],'video':v['codec_name'],'audio':a['codec_name'],'full_decode_pass':True,'bytes':p.stat().st_size,'sha256':hashlib.file_digest(p.open('rb'),'sha256').hexdigest()})
    print('PASS',p.name,duration,flush=True)
assert len(result)==3,result
(OUT/'视频校验.json').write_text(json.dumps(result,ensure_ascii=False,indent=2),encoding='utf8')
(QA/'film-delivery-validation.json').write_text(json.dumps(result,ensure_ascii=False,indent=2),encoding='utf8')
