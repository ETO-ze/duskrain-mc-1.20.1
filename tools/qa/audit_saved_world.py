"""Audit saved region files and draw a top-down inventory map; not a game screenshot."""
from pathlib import Path
from collections import Counter
from read_world import chunks, packed, state
from PIL import Image, ImageDraw, ImageFont
import json, sys

region,out=Path(sys.argv[1]),Path(sys.argv[2]);out.mkdir(parents=True,exist_ok=True)
image=Image.new('RGB',(1024,1024),(40,66,79));pix=image.load()
counts=Counter();seen=set();versions=Counter();signs=[];wrong=[];heights={}
def color(name):
    if 'leaves' in name:return (139,175,108) if 'cherry' not in name else (223,174,183)
    if any(v in name for v in ('quartz','calcite')):return (219,217,193)
    if any(v in name for v in ('prismarine','roof_end')):return (51,86,80)
    if any(v in name for v in ('deepslate','ridge_beast')):return (54,67,70)
    if any(v in name for v in ('water','lily_pad')):return (56,100,121)
    if any(v in name for v in ('planks','log','fence','slab','stairs')):return (112,98,78)
    if any(v in name for v in ('grass','fern','moss','azalea','bamboo')):return (105,139,86)
    if any(v in name for v in ('lantern','torch','bell')):return (224,195,105)
    if any(v in name for v in ('bluet','daisy','cornflower','allium','valley')):return (183,169,156)
    return (124,128,119)

for ch in chunks(region):
    cx,cz=ch['xPos'],ch['zPos'];seen.add((cx,cz));versions[ch.get('DataVersion')]+=1
    for sec in ch['sections']:
        bs=sec.get('block_states',{});p=bs.get('palette',[])
        wanted={i:item['Name'] for i,item in enumerate(p) if any(k in item['Name'] for k in ('leaves','log','lantern','grass','fern','bluet','daisy','cornflower','allium','valley','sign'))}
        if not wanted:continue
        if len(p)==1:counts[p[0]['Name']]+=4096;continue
        bits=max(4,(len(p)-1).bit_length());data=bs['data'];freq=Counter(packed(data,i,bits) for i in range(4096))
        for idx,name in wanted.items():counts[name]+=freq[idx]
    for be in ch.get('block_entities',[]):
        if be.get('id') not in ('minecraft:sign','minecraft:hanging_sign'):continue
        text={side:[json.loads(v).get('text','') for v in be.get(side,{}).get('messages',[])] for side in ('front_text','back_text')}
        row=dict(x=be['x'],y=be['y'],z=be['z'],text=text,waxed=be.get('is_waxed',False))
        signs.append(row)
        if not state(ch,be['x'],be['y'],be['z'])['Name'].endswith('sign'):wrong.append(row)
    hm=ch.get('Heightmaps',{}).get('WORLD_SURFACE')
    if hm is not None:
        for z in range(16):
            for x in range(16):
                y=packed(hm,z*16+x,9)-1;wx=cx*16+x;wz=cz*16+z
                if -512<=wx<512 and -512<=wz<512:
                    pix[wx+512,wz+512]=color(state(ch,wx,y,wz)['Name']);heights[wx,wz]=y

blank=[s for s in signs if not any(line.strip() for side in s['text'].values() for line in side)]
missing=sorted(set((x,z) for x in range(-32,32) for z in range(-32,32))-seen)
report=dict(region=str(region.resolve()),generated_chunks=len(seen),missing_chunks=missing,data_versions=dict(versions),blocks=dict(counts),signs=len(signs),blank_signs=blank,orphan_sign_nbt=wrong)
(out/'saved-world-audit.json').write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf-8')
(out/'saved-signs.json').write_text(json.dumps(signs,ensure_ascii=False,indent=2),encoding='utf-8')
for (x,z),y in heights.items():
    difference=y-heights.get((x-1,z-1),y);factor=max(.76,min(1.18,1+difference*.027));c=pix[x+512,z+512];pix[x+512,z+512]=tuple(min(255,int(v*factor)) for v in c)
canvas=Image.new('RGB',(1280,1180),(239,236,223));canvas.paste(image,(128,96));d=ImageDraw.Draw(canvas)
font=ImageFont.truetype('C:/Windows/Fonts/msyh.ttc',25);small=ImageFont.truetype('C:/Windows/Fonts/msyh.ttc',16)
d.text((40,25),'DuskRain · 仙城存档平面核对图',font=font,fill=(36,60,62));d.text((40, 64),'由实际 Anvil 存档解析绘制，非游戏截图；范围 1024 × 1024 方块',font=small,fill=(69,86,83))
for x,z,label in [(0,-192,'烟雨仙宫'),(-177,-145,'揽月浮屠'),(196,184,'听潮灯塔'),(-222,169,'松涛钟阁'),(0,28,'中央庭院'),(-79,29,'溪云丹坊')]:
    px,pz=x+640,z+608;d.ellipse((px-4,pz-4,px+4,pz+4),fill=(227,180,71),outline=(40,50,43));box=d.textbbox((px+7,pz-12),label,font=small);d.rectangle(box,fill=(239,236,223));d.text((px+7,pz-12),label,font=small,fill=(30,54,57))
d.text((40,1140),'DuskRain / 烟雨仙途 / 群号 205255670',font=small,fill=(50,70,70));canvas.save(out/'saved-world-plan.png')
print(json.dumps({k:report[k] for k in ('generated_chunks','missing_chunks','signs','blank_signs','orphan_sign_nbt')},ensure_ascii=False))
