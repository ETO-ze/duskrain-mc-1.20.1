"""Original cross-collar robes, cloud hems, service NPC skins and guide assets."""
from pathlib import Path
from PIL import Image,ImageDraw
import json,math
ROOT=Path(__file__).resolve().parents[1]
R=ROOT/'mod/src/main/resources/assets/duskrain'
PALETTES=[((91,135,130),(224,223,200),(125,104,66)),((54,105,83),(207,221,190),(170,148,86)),((211,221,213),(55,99,101),(170,146,89)),((52,70,107),(174,195,219),(142,171,193)),((126,73,97),(231,204,190),(190,151,89)),((38,55,69),(222,230,219),(211,177,90))]
NAMES=['听雨布衣','青竹道袍','白玉云衣','玄霜法衣','流霞仙服','问劫天衣']
TYPES=['helmet','chestplate','leggings','boots']
SLOTS=['发冠','上衣','下裳','云履']
def write(path,data):
 p=R/path;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(data,ensure_ascii=False,indent=2),encoding='utf8')
def darker(c,n=18):return tuple(max(0,v-n) for v in c)
def save(im,path):
 p=R/path;p.parent.mkdir(parents=True,exist_ok=True);im.resize((im.width*2,im.height*2),Image.Resampling.NEAREST).save(p)
def borders(d,box,color,lines=1):
 x0,y0,x1,y1=box
 for i in range(lines):d.line((x0,y1-i*2,x1,y1-i*2),fill=color)
def armor(stage,layer):
 base,ivory,gold=PALETTES[stage//3];step=stage%3
 im=Image.new('RGBA',(64,32));d=ImageDraw.Draw(im)
 if layer==1:
  # The headpiece is an open face band and hair ribbon, not a face-covering helmet.
  d.rectangle((0,0,31,7),fill=darker(base));d.rectangle((0,8,31,9),fill=gold)
  d.rectangle((8,8,15,15),fill=(0,0,0,0));d.line((8,8,15,8),fill=gold)
  d.rectangle((19,10,21,15),fill=base)
  d.rectangle((16,16,39,31),fill=base);d.rectangle((40,16,55,31),fill=base)
  # Crossed collar on the torso's front UV rectangle (20,20)-(27,31).
  d.polygon([(20,20),(22,20),(27,26),(27,29)],fill=ivory)
  d.line([(27,20),(25,23),(22,25),(20,27)],fill=gold,width=1)
  d.rectangle((16,28,39,29),fill=darker(base,35));d.rectangle((23,28,25,29),fill=gold)
  borders(d,(40,20,55,31),ivory,step+1)
  d.rectangle((0,20,15,31),fill=darker(base,22));borders(d,(0,20,15,31),gold,step+1)
 else:
  d.rectangle((16,16,39,31),fill=base);d.rectangle((0,16,15,31),fill=base)
  d.line((5,20,5,31),fill=ivory);d.line((9,20,9,31),fill=darker(base,25))
  borders(d,(0,20,15,31),gold,step+1)
 # Different realm motifs: bamboo / clouds / frost / petals / thunder, mirrored hems.
 for x in range(17,39,5):
  if stage//3 in (0,1):d.line([(x,24),(x+1,22),(x+2,24)],fill=ivory)
  elif stage//3 in (2,3):d.line([(x,24),(x,22),(x+2,22),(x+2,24)],fill=gold)
  else:d.line([(x,21),(x+2,23),(x,25)],fill=gold)
 return im
lang=json.loads((R/'lang/zh_cn.json').read_text('utf8'));rows=[]
for stage in range(18):
 for layer in (1,2):save(armor(stage,layer),Path(f'textures/models/armor/robe_{stage}_layer_{layer}.png'))
 for i,t in enumerate(TYPES):
  name=f'robe_{stage}_{t}';base,ivory,gold=PALETTES[stage//3]
  im=Image.new('RGBA',(32,32));d=ImageDraw.Draw(im)
  if t=='helmet':d.arc((6,5,25,24),180,360,fill=gold,width=4);d.rectangle((8,11,23,14),fill=base);d.rectangle((22,14,25,27),fill=ivory)
  elif t=='chestplate':
   d.polygon([(8,5),(13,7),(18,7),(23,5),(30,15),(25,20),(23,17),(23,28),(8,28),(8,17),(6,20),(1,15)],fill=base)
   d.line([(10,6),(19,16),(21,21)],fill=ivory,width=2);d.line([(21,6),(15,14),(9,19)],fill=gold,width=2);d.rectangle((8,22,23,24),fill=gold)
  elif t=='leggings':d.polygon([(8,4),(23,4),(25,28),(17,28),(16,16),(14,28),(6,28)],fill=base);d.rectangle((8,4,23,7),fill=gold)
  else:
   for x in (6,18):d.polygon([(x,8),(x+7,8),(x+7,23),(x+9,28),(x-2,28),(x-2,25),(x,23)],fill=base);d.line((x,10,x+7,10),fill=gold,width=2)
  for j in range(stage%3+1):d.point((29,27-j*3),fill=gold)
  save(im,Path('textures/item')/(name+'.png'));write(Path('models/item')/(name+'.json'),{'parent':'minecraft:item/generated','textures':{'layer0':'duskrain:item/'+name}})
  lang['item.duskrain.'+name]=NAMES[stage//3]+'·'+['初境','中境','后境'][stage%3]+'·'+SLOTS[i]
  if t=='chestplate':rows.append(im)
# Seven human skins use the standard player UV, including arms and a hair overlay.
for index,id in enumerate(['materials','pills','artifacts','master','quests','trial','travel']):
 base,ivory,gold=PALETTES[index%6];skin=(217,174,141);hair=(41,34,32)
 im=Image.new('RGBA',(64,64));d=ImageDraw.Draw(im)
 d.rectangle((0,0,31,15),fill=hair);d.rectangle((8,9,15,15),fill=skin);d.rectangle((0,9,7,14),fill=skin);d.rectangle((16,9,23,14),fill=skin)
 d.point((10,12),fill=(39,39,37));d.point((13,12),fill=(39,39,37));d.line((11,14,12,14),fill=(159,105,82));d.line((8,9,15,9),fill=gold)
 d.rectangle((16,16,39,31),fill=base);d.rectangle((40,16,55,31),fill=base);d.rectangle((32,48,47,63),fill=base)
 d.polygon([(20,20),(22,20),(27,26),(27,29)],fill=ivory);d.line([(27,20),(25,23),(20,27)],fill=gold)
 d.rectangle((16,28,39,29),fill=gold);d.rectangle((0,16,15,31),fill=base);d.rectangle((16,48,31,63),fill=base)
 for box in [(0,29,15,31),(16,61,31,63)]:d.rectangle(box,fill=hair)
 for box in [(40,29,55,31),(32,61,47,63)]:d.rectangle(box,fill=skin)
 d.rectangle((40,8,47,9),fill=darker(base));d.rectangle((43,6,44,8),fill=gold)
 # Over-robe around waist and sleeves, with individual service details.
 # Keep the outer chest transparent so it does not conceal the crossed collar.
 # Raised lapel, sash and hem sit over the matching inner layer UV.
 d.line([(20,36),(22,36),(27,42)],fill=ivory,width=1)
 d.line([(27,36),(25,39),(20,43)],fill=gold,width=1)
 d.rectangle((16,44,39,45),fill=(*darker(base,35),255))
 d.rectangle((23,44,25,45),fill=(*gold,255))
 d.rectangle((16,46,39,47),fill=(*base,255))
 for x in (21,26,32):d.line([(x,46),(x+1,47),(x+2,46)],fill=ivory)
 # Occupation-specific embroidered seal on the upper back, visible from the aisle.
 d.rectangle((33,38,35,41),outline=gold)
 d.point((34,39+index%2),fill=ivory)
 save(im,Path(f'textures/entity/resident_{id}.png'))
lang.update({'item.duskrain.wayfinder':'归云罗盘','block.duskrain.exploration_torch':'长明探索火把','entity.duskrain.resident':'烟雨仙城居民'})
write(Path('lang/zh_cn.json'),lang)
# Vanilla compass angular variants retained; custom item property registered client-side.
write(Path('models/item/wayfinder.json'),{'parent':'minecraft:item/compass'})
write(Path('dynamiclights/item/exploration_torch.json'),{'item':'duskrain:exploration_torch','luminance':15,'water_sensitive':False})
def box(a,b,tex):return {'from':a,'to':b,'faces':{f:{'texture':'#'+tex} for f in ('up','down','east','west','north','south')}}
write(Path('models/block/exploration_torch.json'),{'textures':{'particle':'duskrain:block/carved_timber','wood':'duskrain:block/carved_timber','light':'minecraft:block/sea_lantern','gold':'duskrain:block/aged_bronze'},'elements':[box([7,0,7],[9,11,9],'wood'),box([5,9,5],[11,14,11],'light'),box([4,14,4],[12,16,12],'gold'),box([4,8,4],[12,10,12],'gold')]})
write(Path('blockstates/exploration_torch.json'),{'variants':{'':{'model':'duskrain:block/exploration_torch'}}})
write(Path('models/item/exploration_torch.json'),{'parent':'duskrain:block/exploration_torch','display':{
 'firstperson_righthand':{'rotation':[0,0,0],'translation':[0,-1,-1],'scale':[.35,.35,.35]},
 'firstperson_lefthand':{'rotation':[0,0,0],'translation':[0,-1,-1],'scale':[.35,.35,.35]},
 'thirdperson_righthand':{'rotation':[0,90,0],'translation':[0,3,1],'scale':[.45,.45,.45]},
 'thirdperson_lefthand':{'rotation':[0,-90,0],'translation':[0,3,1],'scale':[.45,.45,.45]},
 'gui':{'rotation':[15,-25,0],'translation':[0,0,0],'scale':[.9,.9,.9]},
 'ground':{'translation':[0,2,0],'scale':[.35,.35,.35]}}})
loot=ROOT/'mod/src/main/resources/data/duskrain/loot_tables/blocks/exploration_torch.json';loot.write_text(json.dumps({'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'duskrain:exploration_torch'}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]}),encoding='utf8')
sheet=Image.new('RGB',(6*144,3*176),(28,43,46));d=ImageDraw.Draw(sheet)
for stage,im in enumerate(rows):
 x=stage//3*144;y=stage%3*176;sheet.paste(im.resize((128,128),Image.Resampling.NEAREST),(x+8,y+8),im.resize((128,128),Image.Resampling.NEAREST));d.text((x+8,y+143),f'Realm {stage//3+1} / {stage%3+1}',fill=(222,207,167))
sheet.save(ROOT/'docs/qa/outfits-eighteen-stages.png')
print('Generated 72 wearable items, 36 armor layers, 7 original human NPC skins.')
