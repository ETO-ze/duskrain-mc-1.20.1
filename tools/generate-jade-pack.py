"""Original jade city pack. Final 64px LabPBR materials are authored by generate_pbr."""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont
import random, math, json, zipfile, hashlib, shutil
ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'assets/jade-city'; BLOCK=OUT/'assets/minecraft/textures/block'
BLOCK.mkdir(parents=True,exist_ok=True)
random.seed(205255670)
images={}
def canvas(base,grain=3):
    im=Image.new('RGB',(32,32));p=im.load()
    for y in range(32):
        for x in range(32):
            n=random.randint(-grain,grain);p[x,y]=tuple(max(0,min(255,c+n)) for c in base)
    return im
def save(name,im):
    im.save(BLOCK/(name+'.png'));images[name]=im
for name,base in [('dark_prismarine',(53,76,79)),('deepslate_tiles',(57,68,78))]:
    im=canvas(base);d=ImageDraw.Draw(im)
    for y in range(0,32,8):
        for x in range(-8,32,8):
            xx=x+(4 if y%16 else 0)
            d.line((xx,y,xx+7,y),fill=tuple(c-18 for c in base))
            d.line((xx,y+1,xx,y+7),fill=tuple(c-12 for c in base))
            d.line((xx+2,y+2,xx+2,y+6),fill=tuple(c+14 for c in base))
            d.line((xx+1,y+7,xx+7,y+7),fill=tuple(c-9 for c in base))
    save(name,im)
for name,base in [('dark_oak_planks',(65,52,43)),('spruce_planks',(100,78,57)),('stripped_dark_oak_log',(70,53,40))]:
    im=canvas(base,4);d=ImageDraw.Draw(im)
    for y in range(0,32,8):
        if name.endswith('planks'):
            d.line((0,y,31,y),fill=tuple(c-17 for c in base));xx=(y*3+7)%32;d.line((xx,y,xx,y+7),fill=tuple(c-12 for c in base))
        for i in range(4):
            xx=random.randrange(32);yy=y+random.randint(2,6);d.line((xx,yy,min(31,xx+random.randint(3,12)),yy),fill=tuple(c+random.choice([-9,7]) for c in base))
    if name=='stripped_dark_oak_log':im=im.transpose(Image.Transpose.ROTATE_90)
    save(name,im)
im=canvas((80,62,46));d=ImageDraw.Draw(im)
for inset in [2,6,10,14]:d.rectangle((inset,inset,31-inset,31-inset),outline=(100-inset,78-inset,56-inset))
save('stripped_dark_oak_log_top',im)
for name,base in [('calcite',(222,227,217)),('quartz_block_side',(225,230,220)),('quartz_block_top',(228,233,224)),('quartz_block_bottom',(218,224,215))]:
    im=canvas(base,2);p=im.load()
    for x in range(32):
        yy=int(10+3*math.sin(x*math.pi/16))%32;p[x,yy]=tuple(c-6 for c in base)
        if x%3:p[x,(yy+1)%32]=tuple(c-3 for c in base)
    save(name,im)
im=canvas((137,151,143));d=ImageDraw.Draw(im)
d.rectangle((1,1,30,30),outline=(102,119,111));d.rectangle((3,3,28,28),outline=(173,182,165))
for yy in [9,20]:
    d.line([(6,yy+3),(6,yy),(11,yy),(11,yy+5),(20,yy+5),(20,yy),(25,yy),(25,yy+3)],fill=(82,104,96),width=2)
save('chiseled_stone_bricks',im)
for name,base in [('sea_lantern',(227,232,200)),('shroomlight',(225,183,113))]:
    im=canvas(base,2);d=ImageDraw.Draw(im)
    d.rectangle((0,0,31,31),outline=(64,85,77),width=2);d.rectangle((3,3,28,28),outline=(131,141,106))
    for a in [9,22]:d.line((a,3,a,28),fill=(100,118,94));d.line((3,a,28,a),fill=(100,118,94))
    d.polygon([(16,11),(21,16),(16,21),(11,16)],outline=(112,129,96))
    save(name,im)
from generate_pbr import generate
pbr_materials=generate()
images={p.stem:Image.open(p).copy() for p in BLOCK.glob('*.png') if not p.stem.endswith(('_n','_s'))}
(OUT/'pack.mcmeta').write_text(json.dumps({'pack':{'pack_format':15,'description':'DuskRain · 青瓦白玉 / 原创 64× LabPBR 立体材质 · 1.20.1'}},ensure_ascii=False),encoding='utf8')
(OUT/'CREDITS.txt').write_text('DuskRain Jade City 2.0\nOriginal pixel artwork and height fields authored for DuskRain.\nCreated from shapes and palettes by tools/generate_pbr.py.\nNo vanilla or third-party texture images were used as inputs.\nThese original textures: CC0-1.0.\nLabPBR 1.3 specification: https://shaderlabs.org/wiki/LabPBR_Material_Standard\nRecommended renderer: Oculus + Complementary Reimagined r5.3 (RP_MODE=3).\nComplementary by EminGT: https://www.complementary.dev/\n',encoding='utf8')
icon=Image.new('RGB',(128,128),(29,52,55));d=ImageDraw.Draw(icon)
d.polygon([(14,71),(64,39),(114,71),(103,75),(64,51),(25,75)],fill=(115,147,140));d.rectangle((29,76,99,104),fill=(223,226,207));d.rectangle((58,81,70,104),fill=(29,52,55));d.text((40,14),'DUSKRAIN',fill=(220,192,130));icon.save(OUT/'pack.png')
zip_path=ROOT/'assets/DuskRain-Jade-City.zip'
with zipfile.ZipFile(zip_path,'w',zipfile.ZIP_DEFLATED) as z:
    for f in sorted(OUT.rglob('*')):
        if f.is_file():z.write(f,f.relative_to(OUT).as_posix())
sheet=Image.new('RGB',(800,((len(images)+3)//4)*232),(23,39,43));d=ImageDraw.Draw(sheet)
for i,(name,im) in enumerate(images.items()):
    x=(i%4)*200;y=(i//4)*232;sheet.paste(im.resize((192,192),Image.Resampling.NEAREST),(x+4,y+4));d.text((x+4,y+202),name,fill=(224,225,208))
sheet.save(ROOT/'docs/qa/jade-textures.png')
report={'pack':zip_path.name,'materials':len(pbr_materials),'textures':len(pbr_materials)*3,'resolution':64,'standard':'labPBR 1.3','format':15,'sha1':hashlib.sha1(zip_path.read_bytes()).hexdigest(),'sha256':hashlib.sha256(zip_path.read_bytes()).hexdigest(),'bytes':zip_path.stat().st_size,'original':True}
(ROOT/'docs/qa/jade-pack.json').write_text(json.dumps(report,indent=2),encoding='utf8')
print(json.dumps(report))
