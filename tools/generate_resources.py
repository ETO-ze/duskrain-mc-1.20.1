"""Deterministic DuskRain content assets, recipes, dimensions and distribution metadata."""
from pathlib import Path
import json, struct, zlib, math

ROOT=Path(__file__).resolve().parents[1]
RES=ROOT/'mod/src/main/resources'
def write(path,data):
    p=RES/path;p.parent.mkdir(parents=True,exist_ok=True)
    p.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
def png(path,pixels):
    p=RES/path;p.parent.mkdir(parents=True,exist_ok=True)
    h=len(pixels);w=len(pixels[0])
    def chunk(t,b):return struct.pack('>I',len(b))+t+b+struct.pack('>I',zlib.crc32(t+b)&0xffffffff)
    raw=b''.join(b'\0'+bytes(c for px in row for c in px) for row in pixels)
    p.write_bytes(b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('>IIBBBBB',w,h,8,6,0,0,0))+chunk(b'IDAT',zlib.compress(raw,9))+chunk(b'IEND',b''))

base={"ultrawarm":False,"natural":False,"coordinate_scale":1,"bed_works":False,"respawn_anchor_works":False,"has_skylight":True,"has_ceiling":False,"ambient_light":0.12,"piglin_safe":True,"has_raids":False,"min_y":0,"height":256,"logical_height":256,"infiniburn":"#minecraft:infiniburn_overworld","effects":"minecraft:overworld","monster_spawn_block_light_limit":0,"monster_spawn_light_level":0}
write(Path('data/duskrain/dimension_type/city.json'),base)
write(Path('data/duskrain/dimension_type/trial.json'),dict(base,fixed_time=18000))
for name,kind in [('city','city'),('construction','demo'),('trial','trial')]:
    write(Path(f'data/duskrain/dimension/{name}.json'),{'type':'duskrain:trial' if name=='trial' else 'duskrain:city','generator':{'type':'duskrain:city','kind':kind,'biome_source':{'type':'minecraft:fixed','biome':'minecraft:cherry_grove'}}})

realms=['炼气','筑基','金丹','元婴','化神','渡劫'];schools=['','剑修','术修','体修']
weapons=['','听雨剑','凝霜杖','镇岳印'];pills=['回灵丹','九转回灵丹','生肌丹','玉露生肌丹','培元丹','紫府培元丹']
lang={'itemGroup.duskrain':'DuskRain · 烟雨仙途','key.categories.duskrain':'DuskRain 修仙','key.duskrain.menu':'打开烟雨仙途菜单','key.duskrain.skill1':'第一主动技能','key.duskrain.skill2':'第二主动技能','key.duskrain.skill3':'第三主动技能'}
gems=['amethyst_shard','iron_ingot','gold_ingot','diamond','ender_pearl','nether_star']
colors=[(153,214,213),(139,186,225),(231,198,105),(191,158,230),(233,169,147),(236,226,196)]
for school in range(1,4):
    for tier in range(6):
        name=f'artifact_{school}_{tier}';lang['item.duskrain.'+name]=realms[tier]+'·'+weapons[school]
        write(Path(f'assets/duskrain/models/item/{name}.json'),{'parent':'minecraft:item/handheld','textures':{'layer0':'duskrain:item/'+name}})
        pix=[[(0,0,0,0) for _ in range(32)] for _ in range(32)]
        col=colors[tier]
        def dot(x,y,c):
            if 0<=x<32 and 0<=y<32:pix[y][x]=(*c,255)
        if school==1:
            for i in range(5,26):
                for o in range(-1,2):dot(i+o,31-i,col if o else (232,250,245))
            for i in range(4,11):dot(i,31-i,(108,76,54));dot(i+1,31-i,(203,179,100))
            for j in range(-4,5):dot(10+j,21+j,(213,179,94))
        elif school==2:
            for i in range(4,24):dot(i,31-i,(132,91,57));dot(i+1,31-i,(220,185,99))
            for y in range(1,15):
                for x in range(18,31):
                    d=((x-24)/5)**2+((y-7)/6)**2
                    if d<1:dot(x,y,tuple(min(255,int(c*(1.15-.4*d))) for c in col))
            for x,y in [(24,0),(24,14),(17,7),(31,7)]:dot(x,y,(255,231,148))
        else:
            for y in range(11,27):
                for x in range(6,27):dot(x,y,col if x not in (6,26) and y not in (11,26) else (221,189,105))
            for y in range(5,12):
                for x in range(12,21):dot(x,y,(221,189,105))
            for i in range(12,22):dot(i,18,(247,244,217));dot(16,i-1,(247,244,217))
        png(Path(f'assets/duskrain/textures/item/{name}.png'),pix)
        middle='minecraft:stick' if tier==0 else f'duskrain:artifact_{school}_{tier-1}'
        write(Path(f'data/duskrain/recipes/{name}.json'),{'type':'minecraft:crafting_shaped','pattern':[' G ','GMG',' S '],'key':{'G':{'item':'minecraft:'+gems[tier]},'M':{'item':middle},'S':{'item':'minecraft:blaze_powder' if tier>2 else 'minecraft:stick'}},'result':{'item':'duskrain:'+name}})
for i,name in enumerate(pills):
    id=f'pill_{i}';lang['item.duskrain.'+id]=name
    write(Path(f'assets/duskrain/models/item/{id}.json'),{'parent':'minecraft:item/generated','textures':{'layer0':'duskrain:item/'+id}})
    pix=[[(0,0,0,0) for _ in range(32)] for _ in range(32)];col=[(107,195,223),(222,117,145),(161,205,126)][i//2]
    for y in range(7,27):
        for x in range(6,27):
            d=((x-16)/9)**2+((y-17)/9)**2
            if d<1:pix[y][x]=(*(min(255,int(c*(1.2-.48*d))) for c in col),255)
    for x in range(10,23):pix[15][x]=(231,204,134,255)
    png(Path(f'assets/duskrain/textures/item/{id}.png'),pix)
    ing=['lapis_lazuli','ghast_tear','glistering_melon_slice'][i//2]
    write(Path(f'data/duskrain/recipes/{id}.json'),{'type':'minecraft:crafting_shapeless','ingredients':[{'item':'minecraft:'+ing},{'item':'minecraft:honey_bottle'},{'item':'minecraft:glowstone_dust' if i%2 else 'minecraft:sugar'}],'result':{'item':'duskrain:'+id,'count':2}})
write(Path('assets/duskrain/lang/zh_cn.json'),lang)
write(Path('assets/duskrain/lang/en_us.json'),lang)
write(Path('data/duskrain/advancements/welcome.json'),{'display':{'icon':{'item':'duskrain:artifact_1_0'},'title':'DuskRain · 烟雨仙途','description':'群号 205255670 · 六境问道，烟雨长行','background':'minecraft:textures/block/deepslate_tiles.png','show_toast':True,'announce_to_chat':False},'criteria':{'join':{'trigger':'minecraft:tick'}}})
f=RES/'data/duskrain/functions';f.mkdir(parents=True,exist_ok=True)
(f/'help.mcfunction').write_text('tellraw @s {"text":"DuskRain · 群号205255670。输入 /dr menu 打开主菜单；管理员 /dr build 打开施工摄影台。","color":"aqua"}\n',encoding='utf-8')
write(Path('pack.mcmeta'),{'pack':{'pack_format':15,'description':'DuskRain · 烟雨仙途 | 205255670'}})
print(json.dumps({'artifacts':18,'pills':6,'recipes':24,'dimensions':3,'buildings':30},ensure_ascii=False))

# Minimal native structure template for Forge's dedicated GameTest server.
import gzip
def utf(s):
    b=s.encode('utf-8');return struct.pack('>H',len(b))+b
def tag(t,n,b):return bytes([t])+utf(n)+b
body=tag(3,'DataVersion',struct.pack('>i',3465))
body+=tag(9,'size',b'\x03'+struct.pack('>i',3)+struct.pack('>iii',3,3,3))
body+=tag(9,'palette',b'\x0a'+struct.pack('>i',1)+tag(8,'Name',utf('minecraft:air'))+b'\0')
body+=tag(9,'blocks',b'\x0a'+struct.pack('>i',0))+tag(9,'entities',b'\x0a'+struct.pack('>i',0))+b'\0'
template=RES/'data/duskrain/structures/empty.nbt';template.parent.mkdir(parents=True,exist_ok=True);template.write_bytes(gzip.compress(b'\x0a\0\0'+body,mtime=0))
