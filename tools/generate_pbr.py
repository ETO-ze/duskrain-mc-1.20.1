"""Authored 64px architectural materials with matched geometry, LabPBR 1.3 and albedo.

No photographs or third-party pixels are inputs. Height describes carved geometry,
not image luminance. Rebuild with Python + Pillow, then generate-jade-pack.py packs it.
"""
from pathlib import Path
from PIL import Image, ImageDraw
import math, random, json

ROOT = Path(__file__).resolve().parents[1]
SIZE = 64
MATERIALS = {
    'dark_prismarine': ('tile', (54, 78, 82)),
    'deepslate_tiles': ('brick', (65, 74, 81)),
    'stone_bricks': ('brick', (143, 150, 145)),
    'polished_deepslate': ('paver', (69, 78, 84)),
    'dark_oak_planks': ('wood', (73, 55, 41)),
    'spruce_planks': ('wood', (110, 83, 55)),
    'stripped_dark_oak_log': ('column', (79, 58, 42)),
    'stripped_dark_oak_log_top': ('endgrain', (92, 72, 52)),
    'calcite': ('jade', (221, 227, 218)),
    'quartz_block_side': ('jade', (229, 231, 219)),
    'quartz_block_top': ('jade', (232, 234, 224)),
    'quartz_block_bottom': ('jade', (222, 227, 215)),
    'quartz_bricks': ('brick', (219, 225, 210)),
    'chiseled_quartz_block': ('carving', (224, 226, 209)),
    'chiseled_quartz_block_top': ('carving', (227, 230, 215)),
    'chiseled_stone_bricks': ('carving', (147, 162, 150)),
    'sea_lantern': ('lamp', (231, 238, 212)),
    'shroomlight': ('lamp', (238, 197, 125)),
    'qingwa_detail': ('tile', (54, 78, 82)),
    'carved_timber': ('woodcarving', (83, 60, 41)),
    'aged_bronze': ('bronze', (145, 121, 71)),
}


def clamp(x):
    return max(0, min(255, round(x)))


def build(name, kind, base):
    rng = random.Random('DuskRain/PBR/' + name)
    height = Image.new('L', (SIZE, SIZE), 247)
    hp = height.load()
    mask = Image.new('L', (SIZE, SIZE))
    d = ImageDraw.Draw(mask)
    if kind in ('carving', 'woodcarving', 'bronze'):
        d.rectangle((3, 3, 60, 60), outline=255, width=2)
        d.rectangle((7, 7, 56, 56), outline=190)
        for y in (18, 40):
            for x in (12, 36):
                d.line([(x, y+8), (x, y), (x+15, y), (x+15, y+13),
                        (x+6, y+13), (x+6, y+6), (x+10, y+6)], fill=255, width=2)
    if kind == 'lamp':
        d.rectangle((1, 1, 62, 62), outline=255, width=3)
        d.rectangle((7, 7, 56, 56), outline=180, width=2)
        for a in (20, 43):
            d.line((a, 7, a, 56), fill=210, width=2)
            d.line((7, a, 56, a), fill=210, width=2)
        d.polygon([(32, 23), (41, 32), (32, 41), (23, 32)], outline=210, width=2)
    for y in range(SIZE):
        for x in range(SIZE):
            if kind == 'tile':
                # Rounded overlapping tubular tiles, 4 courses wide / 2 high.
                u, v = (x+.5) % 16, (y+.5) % 32
                h = 219 + 32 * math.sin(math.pi * u / 16)
                h -= 19 * max(0, 1 - min(v, 32-v) / 2.5)
            elif kind in ('brick', 'paver'):
                pitch = 32 if kind == 'paver' else 16
                u, v = (x + (16 if int(y/pitch) % 2 else 0)+.5) % 32, (y+.5) % pitch
                bevel = min(u, 32-u, v, pitch-v)
                h = 223 + 26 * min(1, bevel/2.5)
            elif kind in ('wood', 'column', 'woodcarving'):
                u, v = (y, x) if kind == 'column' else (x, y)
                grain = math.sin(u*.20 + 1.4*math.sin(v*.22))
                h = 247 + 2.0*grain - (12 if kind == 'wood' and v % 16 < 1 else 0)
                if kind == 'woodcarving': h -= mask.getpixel((x,y)) / 255 * 15
            elif kind == 'endgrain':
                h = 246 + 2 * math.sin(math.hypot(x-32,y-32)*1.4)
            elif kind in ('carving', 'bronze'):
                h = 249 - mask.getpixel((x,y)) / 255 * (20 if kind == 'carving' else 10)
            elif kind == 'lamp':
                h = 239 + mask.getpixel((x,y)) / 255 * 13
            else:
                h = 250 + 1.2 * math.sin(x*.14 + 2*math.sin(y*.1))
            hp[x,y] = clamp(h)
    albedo = Image.new('RGB', (SIZE,SIZE)); normal = Image.new('RGBA',(SIZE,SIZE)); spec = Image.new('RGBA',(SIZE,SIZE))
    for y in range(SIZE):
        for x in range(SIZE):
            h=hp[x,y]; m=mask.getpixel((x,y))/255
            # DirectX tangent convention: image y increases down, normal opposes gradient.
            dx=(hp[(x+1)%SIZE,y]-hp[(x-1)%SIZE,y])/255 * SIZE*.25/2
            dy=(hp[x,(y+1)%SIZE]-hp[x,(y-1)%SIZE])/255 * SIZE*.25/2
            inv=1/math.sqrt(1+dx*dx+dy*dy)
            ao=clamp(255-max(0,245-h)*.65)
            normal.putpixel((x,y),(clamp(127.5-dx*inv*127.5),clamp(127.5-dy*inv*127.5),ao,h))
            n=rng.uniform(-2,2)
            shade=(h-247)*.60+n
            smooth, f0, porosity, emission = 105,10,16,0
            if kind == 'tile': smooth,porosity=153,4
            elif kind in ('wood','column','endgrain','woodcarving'): smooth,porosity=91,12
            elif kind in ('jade','carving') or name.startswith('quartz'): smooth,porosity=156,3
            elif kind=='bronze': smooth,f0,porosity=160,255,0
            if kind=='lamp':
                smooth,porosity=126,0
                emission=200 if m==0 else 0
                color=base if m==0 else (66,96,83)
                shade=n
            else: color=base
            if kind in ('carving','woodcarving','bronze'): shade-=m*12
            if kind in ('jade','carving'):
                vein=math.exp(-((y-28-4*math.sin(x*math.pi/32))/1.3)**2)
                shade-=vein*4
            albedo.putpixel((x,y),tuple(clamp(c+shade) for c in color))
            spec.putpixel((x,y),(clamp(smooth+n*2),f0,porosity,emission))
    return albedo,normal,spec,height


def generate():
    report=[]; sheets=[]
    for name,(kind,base) in MATERIALS.items():
        namespace='duskrain'
        out=ROOT/f'assets/jade-city/assets/{namespace}/textures/block'; out.mkdir(parents=True,exist_ok=True)
        albedo,normal,spec,height=build(name,kind,base)
        for suffix,im in (('',albedo),('_n',normal),('_s',spec)):
            im.save(out/(name+suffix+'.png'))
            if namespace=='duskrain':
                im.save(ROOT/f'mod/src/main/resources/assets/duskrain/textures/block/{name}{suffix}.png')
        assert min(normal.getchannel('A').getextrema())>0
        assert normal.getchannel('A').getextrema()[0] < normal.getchannel('A').getextrema()[1]
        assert set(spec.getchannel('A').getdata()) <= {0,200}
        for r,g,_,_ in normal.getdata():
            assert (r/127.5-1)**2+(g/127.5-1)**2 < 1
        report.append({'texture':namespace+':'+name,'size':SIZE,'height':height.getextrema(),'kind':kind})
        if name in ('dark_prismarine','carved_timber','chiseled_stone_bricks','quartz_bricks','aged_bronze','sea_lantern'):
            sheets.append((name,albedo,normal,height,spec))
    sheet=Image.new('RGB',(840,len(sheets)*182),(27,44,45)); d=ImageDraw.Draw(sheet)
    for row,(name,*ims) in enumerate(sheets):
        d.text((8,row*182+3),name,fill=(224,213,172))
        for col,im in enumerate(ims):
            # Channel visualizations only; the actual RGBA files retain labPBR encoding.
            sheet.paste(im.convert('RGB').resize((152,152),Image.Resampling.NEAREST),(8+col*208,row*182+22))
    sheet.save(ROOT/'docs/qa/pbr-material-atlas.png')
    (ROOT/'docs/qa/pbr-materials.json').write_text(json.dumps({'standard':'labPBR 1.3','materials':report,'passed':True,'original':True,'height_not_from_luminance':True},indent=2),encoding='utf8')
    return report

if __name__ == '__main__':
    print(json.dumps(generate()))
