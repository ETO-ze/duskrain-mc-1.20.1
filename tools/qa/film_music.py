"""Original pentatonic score synthesized for this project, no sampled recordings."""
from pathlib import Path
import numpy as np,wave
ROOT=Path(__file__).resolve().parents[2]
OUT=ROOT/'dist/DuskRain-films-20260916';OUT.mkdir(exist_ok=True)
SR=48000;DURATION=192;beat=.75
mix=np.zeros((int(DURATION*SR),2),np.float32)
rng=np.random.default_rng(205255670)

def add(start,midi,length,gain=.12,pan=0,flute=False):
    n=int(length*SR);t=np.arange(n)/SR;f=440*2**((midi-69)/12)
    if flute:
        phase=2*np.pi*f*t+.018*f*np.sin(2*np.pi*4.7*t)/(4.7)
        a=(np.sin(phase)+.16*np.sin(2*phase)+.06*np.sin(3*phase))
        env=np.minimum(1,t/.3)*np.minimum(1,(length-t)/.7)*np.exp(-t/(length*4))
    else:
        a=sum((1/h**1.3)*np.sin(2*np.pi*f*h*t+(.08 if h%2==0 else 0))*np.exp(-t*(.5+h*.38)) for h in range(1,9))
        env=np.minimum(1,t/.005)*np.minimum(1,(length-t)/.2)
    a=(a*env*gain).astype(np.float32);i=int(start*SR)
    for delay,vol,reverse in [(0,1,False),(.19,.19,True),(.43,.10,False)]:
        k=i+int(delay*SR);m=min(n,len(mix)-k)
        if m<=0:continue
        pp=-pan if reverse else pan
        mix[k:k+m,0]+=a[:m]*vol*np.sqrt((1-pp)/2)
        mix[k:k+m,1]+=a[:m]*vol*np.sqrt((1+pp)/2)

motifs=[[62,66,69,74,73,69,66,64],[59,62,66,69,71,69,66,62],[57,62,64,69,66,64,62,59],[62,64,66,69,74,76,74,69]]
for bar in range(64):
    start=bar*beat*4
    if start>=DURATION:break
    base=[38,35,33,38][(bar//4)%4]
    add(start,base,5,.085,-.15,True);add(start,base+19,4,.033,.15,True)
    motif=motifs[(bar//4)%4]
    for j in range(4):
        note=motif[(bar%2)*4+j]
        strength=.07 if bar<4 else .11 if bar<48 else .09
        add(start+j*beat,note,3.1,strength,(-.28 if j%2 else .28))
        if 12<=bar<52 and j in (1,3):add(start+j*beat+.375,note+12,1.4,.021,.5 if j==1 else -.5)
    if bar%4==0 and 8<=bar<56:add(start+.3,motif[3]+12,5.3,.036,0,True)
    if 16<=bar<52:
        t=np.arange(int(.65*SR))/SR
        drum=np.sin(2*np.pi*(70*t-30*t*t))*np.exp(-t*13)*.07
        for b in (0,2):
            k=int((start+b*beat)*SR);mix[k:k+len(drum),:]+=drum[:,None]
t=np.arange(len(mix))/SR
mix*=np.minimum(1,t/5)[:,None]*np.minimum(1,(DURATION-t)/7)[:,None]
mix=np.tanh(mix*1.3)*.8
with wave.open(str(OUT/'烟雨序-原创配乐.wav'),'wb') as w:
    w.setnchannels(2);w.setsampwidth(2);w.setframerate(SR);w.writeframes((mix*32767).astype('<i2').tobytes())
print('Original score written',flush=True)
