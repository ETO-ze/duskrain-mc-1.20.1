"""Original low-poly Chinese timber/roof details using Minecraft's material atlas."""
from pathlib import Path
import json
from PIL import Image,ImageDraw
import random,math
R=Path(__file__).resolve().parents[1]/'mod/src/main/resources/assets/duskrain'
def write(path,obj):
    p=R/path;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(obj,ensure_ascii=False,indent=2),encoding='utf-8')
def box(a,b,tex):
    return {'from':a,'to':b,'faces':{f:{'texture':'#'+tex} for f in ['north','south','east','west','up','down']}}
models={}
lattice=[box([0,0,0],[2,16,16],'wood'),box([14,0,0],[16,16,16],'wood'),box([0,0,0],[16,2,16],'wood'),box([0,14,0],[16,16,16],'wood')]
for y in [4,8,12]:lattice.append(box([2,y,6],[14,y+1,10],'wood'))
for x in [4,8,12]:lattice.append(box([x,2,6],[x+1,14,10],'wood'))
models['lattice_window']=lattice
models['roof_end']=[box([0,12,1],[16,16,15],'tile'),box([4,4,0],[12,13,3],'tile'),box([6,6,0],[10,11,1],'gold')]
models['ridge_beast']=[box([4,0,3],[12,3,13],'tile'),box([5,3,5],[11,8,12],'tile'),box([4,8,2],[12,13,8],'tile'),box([3,12,3],[5,16,5],'gold'),box([11,12,3],[13,16,5],'gold'),box([7,6,12],[9,13,15],'tile')]
models['jade_lantern']=[box([3,0,3],[13,2,13],'wood'),box([4,3,4],[12,12,12],'jade'),box([2,12,2],[14,14,14],'tile'),box([7,14,7],[9,16,9],'gold')]+[box([x,2,z],[x+1,12,z+1],'wood') for x in [3,12] for z in [3,12]]
models['ink_screen']=[box([0,0,6],[2,16,10],'wood'),box([14,0,6],[16,16,10],'wood'),box([2,3,7],[14,14,9],'paper'),box([2,14,6],[14,16,10],'wood')]
models['eave_tip']=[box([0,0,0],[16,3,16],'tile'),box([0,3,0],[16,6,12],'tile'),box([1,6,0],[15,9,8],'tile'),box([2,9,0],[14,12,5],'tile'),box([3,12,0],[13,16,3],'tile'),box([6,3,13],[10,10,16],'gold')]
models['ridge_tile']=[box([0,0,0],[16,3,16],'tile'),box([0,3,2],[16,7,14],'tile'),box([0,7,4],[16,12,12],'tile'),box([0,12,6],[16,16,10],'tile'),box([7,0,0],[9,3,16],'gold')]
models['dougong']=[box([5,0,5],[11,5,11],'wood'),box([2,5,4],[14,8,12],'wood'),box([0,8,5],[16,11,11],'wood'),box([2,11,2],[5,14,14],'wood'),box([11,11,2],[14,14,14],'wood'),box([0,14,0],[16,16,16],'wood')]
texture=R/'textures/block';texture.mkdir(parents=True,exist_ok=True)
for name,base in [('qingwa_detail',(51,73,78)),('carved_timber',(75,55,38)),('aged_bronze',(130,110,66))]:
    rng=random.Random(name);im=Image.new('RGB',(64,64));pix=im.load()
    for y in range(64):
        for x in range(64):
            n=rng.randint(-3,3)+(int(5*math.sin(x*math.pi/8)) if name=='qingwa_detail' else int(3*math.sin(y*.9+x*.1)))
            pix[x,y]=tuple(max(0,min(255,c+n)) for c in base)
    d=ImageDraw.Draw(im)
    if name=='qingwa_detail':
        for x in range(0,64,16):
            d.line((x,0,x,63),fill=(27,46,52));d.line((x+3,0,x+3,63),fill=(83,107,105));d.line((x+4,0,x+4,63),fill=(65,92,94))
        for y in [0,31,63]:d.line((0,y,63,y),fill=(31,51,55));d.line((0,max(0,y-1),63,max(0,y-1)),fill=(97,116,109))
    elif name=='carved_timber':
        for y in [2,59]:d.line((0,y,63,y),fill=(143,111,61));d.line((0,y+2,63,y+2),fill=(42,35,29))
        for x in range(0,64,16):d.line([(x+2,42),(x+2,22),(x+12,22),(x+12,35),(x+7,35),(x+7,29)],fill=(132,99,57),width=2)
    else:
        d.rectangle((2,2,61,61),outline=(183,162,103),width=2);d.rectangle((7,7,56,56),outline=(74,83,63))
        for x,y in [(20,20),(44,44)]:d.arc((x-8,y-8,x+8,y+8),20,330,fill=(194,169,104),width=2)
    im.save(texture/(name+'.png'))
for name,elements in models.items():
    write(Path('models/block')/(name+'.json'),{'textures':{'particle':'duskrain:block/dark_oak_planks','wood':'duskrain:block/carved_timber','tile':'duskrain:block/qingwa_detail','gold':'duskrain:block/aged_bronze','jade':'duskrain:block/sea_lantern','paper':'duskrain:block/silk'},'elements':elements})
    write(Path('blockstates')/(name+'.json'),{'variants':{f'facing={d}':{'model':'duskrain:block/'+name,'y':a} for d,a in [('north',0),('east',90),('south',180),('west',270)]}})
    write(Path('models/item')/(name+'.json'),{'parent':'duskrain:block/'+name})
lang=R/'lang/zh_cn.json';texts=json.loads(lang.read_text(encoding='utf-8')) if lang.exists() else {}
texts.update({'block.duskrain.'+k:v for k,v in zip(models,['木构花格窗','青瓦瓦当','吻兽屋脊','白玉灯笼','水墨屏风','重檐翘角','青瓦筒脊','如意斗拱'])})
write(Path('lang/zh_cn.json'),texts)
print('Original architecture models:',len(models))
