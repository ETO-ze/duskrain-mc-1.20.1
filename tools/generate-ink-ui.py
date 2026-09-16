"""Authored parchment, ink mountains, cloud borders and a hand-drawn rain seal."""
from PIL import Image,ImageDraw,ImageFilter
from pathlib import Path
import random,math
ROOT=Path(__file__).resolve().parents[1];OUT=ROOT/'mod/src/main/resources/assets/duskrain/textures/gui';OUT.mkdir(parents=True,exist_ok=True)
random.seed(205255670)
def rain_seal(d,x,y,size):
    d.rectangle((x,y,x+size,y+size),fill=(140,49,42,240));d.rectangle((x+2,y+2,x+size-2,y+size-2),outline=(225,192,151,190),width=1)
    p=size/10;c=(237,220,184,235);line=lambda a:d.line([(x+int(xx*p),y+int(yy*p)) for xx,yy in a],fill=c,width=max(1,int(p/2)))
    line([(2,2),(8,2)]);line([(5,2),(5,8)]);line([(2,8),(2,4),(8,4),(8,8)])
    for a in [(3,5),(6,5),(3,7),(6,7)]:line([a,(a[0]+1,a[1])])
def paper(w,h,kind):
    im=Image.new('RGBA',(w,h),(0,0,0,0));d=ImageDraw.Draw(im)
    d.rounded_rectangle((5,8,w-2,h-1),radius=5,fill=(11,30,31,100))
    d.polygon([(7,5),(w-8,7),(w-5,h-8),(8,h-5),(5,h//2)],fill=(235,231,210,244))
    for _ in range(w*h//5):
        x=random.randint(8,w-9);y=random.randint(9,h-11);n=random.randint(-3,3)
        d.point((x,y),fill=(235+n,231+n,210+n,244))
    # Dry-brush mountain ridges kept low behind the footer.
    layer=Image.new('RGBA',(w,h));ld=ImageDraw.Draw(layer)
    base=h*.85
    for k,(col,level) in enumerate([((90,124,119,23),.66),((62,95,94,30),.77),((41,75,75,40),.87)]):
        points=[(5,h-8)]
        for x in range(5,w-5,5):
            ridge=math.sin(x/w*11+k*2)*.04+math.sin(x/w*27+k)*.016
            points.append((x,int(h*(level+ridge))))
        points.append((w-5,h-8));ld.polygon(points,fill=col)
    im=Image.alpha_composite(im,layer);d=ImageDraw.Draw(im)
    # Edge strokes and restrained key-pattern corners.
    for x in [10,w-11]:d.line((x,13,x,h-14),fill=(118,107,76,110),width=1)
    for x,sgn in [(13,1),(w-14,-1)]:
        for y,sy in [(16,1),(h-17,-1)]:
            d.line([(x+sgn*20,y),(x,y),(x,y+sy*20),(x+sgn*11,y+sy*20),(x+sgn*11,y+sy*10)],fill=(151,130,84,165),width=2)
    if kind=='scroll':
        for y in [3,h-11]:
            d.rounded_rectangle((1,y,w-2,y+8),radius=3,fill=(70,72,53,255));d.line((8,y+2,w-9,y+2),fill=(177,157,100,255),width=1)
            for x in [5,w-10]:d.rectangle((x,y-1,x+4,y+9),fill=(111,98,62,255))
        rain_seal(d,w-62,40,26)
    else:rain_seal(d,w-44,16,22)
    # Calligraphic twig: fine tapered branches on the lower right, below text.
    if h>200:
        for i in range(4):
            x=w-26-i*5;y=h-28-i*11;d.line((w-30,h-20,x,y),fill=(49,77,70,75),width=2)
            d.ellipse((x-11,y-3,x,y+1),fill=(40,72,61,67));d.ellipse((x,y-7,x+9,y-3),fill=(40,72,61,55))
    return im
for name,w,h,kind in [('ink_scroll',492,648,'scroll'),('ink_hud',540,150,'compact'),('ink_target',540,114,'compact'),('ink_sheet',1200,800,'sheet')]:
    paper(w,h,kind).save(OUT/(name+'.png'))
print('Created four original ink UI surfaces.')
