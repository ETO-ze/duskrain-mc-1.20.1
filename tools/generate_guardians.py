from pathlib import Path
import json, math, random
from PIL import Image,ImageDraw
R=Path(__file__).resolve().parents[1]/'mod/src/main/resources/assets/duskrain'
def write(p,v):
    f=R/p;f.parent.mkdir(parents=True,exist_ok=True);f.write_text(json.dumps(v,ensure_ascii=False,indent=2),encoding='utf-8')
def cube(origin,size,uv=(0,0),inflate=0):return dict(origin=origin,size=size,uv=list(uv),inflate=inflate)
def bone(name,pivot,cubes,parent='root',rotation=None):
    d=dict(name=name,pivot=pivot,cubes=cubes)
    if parent:d['parent']=parent
    if rotation:d['rotation']=rotation
    return d
for kind,palette in [('wood_guardian',((58,77,54),(104,136,82),(178,207,121))),('rock_guardian',((49,62,70),(83,104,112),(110,214,216))),('storm_dragon',((37,64,88),(62,115,137),(210,193,129)))]:
    dragon=kind=='storm_dragon';rock=kind=='rock_guardian'
    bones=[bone('root',[0,0,0],[],None)]
    bones+=[bone('body',[0,22,0],[cube([-8,14,-5],[16,18,10]),cube([-6,19,-7],[12,9,2],(32,0))]),
            bone('head',[0,32,0],[cube([-6,31,-6],[12,11,11]),cube([-4,32,-10 if dragon else -7],[8,4,6 if dragon else 2],(32,18))]),
            bone('left_arm',[10,30,0],[cube([8,13,-4],[7,19,8]),cube([8,11,-5],[8,6,10],(0,32))]),
            bone('right_arm',[-10,30,0],[cube([-15,13,-4],[7,19,8]),cube([-16,11,-5],[8,6,10],(0,32))]),
            bone('left_leg',[5,14,0],[cube([1,2,-4],[7,13,8]),cube([1,0,-6],[8,4,11],(0,32))]),
            bone('right_leg',[-5,14,0],[cube([-8,2,-4],[7,13,8]),cube([-9,0,-6],[8,4,11],(0,32))])]
    if kind=='wood_guardian':
        for side in [-1,1]:
            bones.append(bone('branch'+str(side),[side*5,38,0],[cube([side*8-2,38,-1],[3,14,3]),cube([side*13-2,44,-1],[3,9,3]),cube([side*8-4,45,-4],[9,5,8],(32,32))],'head',[0,0,side*23]))
        bones.append(bone('roots',[0,3,0],[cube([-12,0,-3],[24,4,6]),cube([-3,0,-11],[6,3,23])]))
    elif rock:
        bones.append(bone('shoulders',[0,29,0],[cube([-20,26,-7],[10,10,14],(32,32)),cube([10,26,-7],[10,10,14],(32,32))]))
        bones.append(bone('crown',[0,41,0],[cube([-7,41,-4],[3,6,8]),cube([4,41,-4],[3,6,8]),cube([-2,41,-2],[4,9,4],(32,32))],'head'))
    else:
        bones.append(bone('tail',[0,16,5],[cube([-4,9,5],[8,8,15]),cube([-3,7,19],[6,6,14]),cube([-2,6,31],[4,4,12],(32,32))]))
        for side in [-1,1]:
            bones.append(bone('horn'+str(side),[side*5,39,0],[cube([side*6-1,38,-1],[3,12,3]),cube([side*9-1,44,0],[3,5,7])],'head',[0,0,side*24]))
            bones.append(bone('whisker'+str(side),[side*4,34,-7],[cube([side*5-1,33,-9],[2,2,14],(32,32))],'head',[0,side*45,side*20]))
    write(Path('geo')/(kind+'.geo.json'),{'format_version':'1.12.0','minecraft:geometry':[{'description':{'identifier':'geometry.'+kind,'texture_width':64,'texture_height':64,'visible_bounds_width':6,'visible_bounds_height':6,'visible_bounds_offset':[0,2,0]},'bones':bones}]})
    rng=random.Random(205255670);im=Image.new('RGBA',(64,64));px=im.load()
    for y in range(64):
        for x in range(64):
            base=palette[2] if x>34 and y>34 and (x+y)%9<2 else palette[1] if (x//4+y//6)%3==0 else palette[0];n=rng.randint(-12,12);px[x,y]=tuple(max(0,min(255,c+n)) for c in base)+(255,)
    d=ImageDraw.Draw(im);d.rectangle((32,18,39,20),fill=palette[2]+(255,));d.rectangle((43,18,50,20),fill=palette[2]+(255,))
    p=R/'textures/entity'/f'{kind}.png';p.parent.mkdir(parents=True,exist_ok=True);im.save(p)
animations={'format_version':'1.8.0','animations':{
    'idle':{'loop':True,'animation_length':3,'bones':{'body':{'rotation':[0,0,'math.sin(query.anim_time * 120) * 1.5']},'head':{'rotation':[0,'math.sin(query.anim_time * 80) * 6',0]},'tail':{'rotation':[0,'math.sin(query.anim_time * 100) * 10',0]}}},
    'walk':{'loop':True,'animation_length':1,'bones':{b:{'rotation':[f'math.sin(query.anim_time * 360) * {v}',0,0]} for b,v in [('left_leg',25),('right_leg',-25),('left_arm',-20),('right_arm',20)]}},
    'charge':{'loop':True,'animation_length':1.5,'bones':{'left_arm':{'rotation':[-110,0,-20]},'right_arm':{'rotation':[-110,0,20]},'head':{'rotation':[-12,0,0]},'body':{'position':[0,'math.sin(query.anim_time * 720) * .35',0]}}}}}
write(Path('animations/guardian.animation.json'),animations)
print('Three original guardian models, textures and animations generated.')
