"""DuskRain V14 original, UV-authored voxel costume atlas and sculpted item models.
All pixels are authored at final resolution; no resized legacy artwork or external media.
"""
from pathlib import Path
from PIL import Image,ImageDraw,ImageFont
import math,json,random,hashlib
ROOT=Path(__file__).resolve().parents[1];R=ROOT/'mod/src/main/resources/assets/duskrain';QA=ROOT/'docs/qa'
SCALE=4
PALETTES=[((72,117,113),(229,223,197),(151,127,77)),((38,96,74),(216,229,198),(180,148,79)),((208,226,224),(38,89,103),(188,152,74)),((35,51,82),(179,207,229),(167,187,204)),((111,52,80),(246,208,186),(204,164,87)),((21,41,55),(220,235,225),(225,185,86)),((231,220,184),(40,80,92),(235,180,62))]
REALMS=['听雨布衣','青竹道袍','白玉云衣','玄霜法衣','流霞仙服','问劫天衣','太初神衣'];PARTS={'helmet':'发冠','chestplate':'上衣','leggings':'下裳','boots':'云履'}
lang=json.loads((R/'lang/zh_cn.json').read_text('utf8'))
def write(path,obj):
 p=R/path;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(obj,ensure_ascii=False,indent=2),encoding='utf8')
def save(im,path):
 p=R/path;p.parent.mkdir(parents=True,exist_ok=True);im.save(p)
def shade(c,n):return tuple(max(0,min(255,int(v+n))) for v in c)
def panel(im,rect,color,seed):
 x0,y0,x1,y1=[round(v*SCALE) for v in rect];d=ImageDraw.Draw(im)
 for y in range(y0,y1):
  for x in range(x0,x1):
   fold=math.sin((x-x0)*.29+seed)*3+math.cos((x-x0)*.075)*4
   silk=((x*7+y*11+seed)%9-4)*.36
   im.putpixel((x,y),shade(color,fold+silk)+(255,))
def curve(d,pts,color,w=1):d.line([(round(x*SCALE),round(y*SCALE)) for x,y in pts],fill=color,width=w,joint='curve')
def motif(d,x,y,tier,color,size=1):
 # Seven original families: rain stitch, bamboo, cloud scroll, sixfold frost,
 # plum blossom, thunder ribbon, solar lotus. Drawn into each mapped cloth face.
 if tier==0:
  for i in range(3):curve(d,[(x+i*.7,y),(x+i*.7-.2,y+1.2)],color)
 elif tier==1:
  curve(d,[(x,y+2),(x+.3,y),(x+.6,y-2)],color,2)
  for a in (-1,1):curve(d,[(x+.2,y),(x+a*1.1,y-.7),(x+a*.7,y-.15)],color,2)
 elif tier==2:
  pts=[(x+t*.08,y+math.sin(t*.28)*.5) for t in range(-20,21)];curve(d,pts,color,2);curve(d,[(x-.5,y),(x-.6,y-1),(x+.3,y-1.2),(x+.8,y-.6)],color)
 elif tier==3:
  for a in range(6):
   q=a*math.pi/3;curve(d,[(x,y),(x+1.5*math.cos(q),y+1.5*math.sin(q))],color,2)
 elif tier==4:
  for a in range(5):
   q=a*math.tau/5;cx=x+.75*math.cos(q);cy=y+.75*math.sin(q);d.ellipse(((cx-.55)*4,(cy-.55)*4,(cx+.55)*4,(cy+.55)*4),outline=color,width=1)
  d.ellipse((x*4-1,y*4-1,x*4+1,y*4+1),fill=color)
 elif tier==5:curve(d,[(x-1.4,y-1.7),(x+.4,y-.3),(x-.7,y+.1),(x+1.2,y+1.7)],color,2)
 else:
  for a in range(8):
   q=a*math.tau/8;pts=[(x,y),(x+1.4*math.cos(q-.2),y+1.4*math.sin(q-.2)),(x+2*math.cos(q),y+2*math.sin(q)),(x+1.4*math.cos(q+.2),y+1.4*math.sin(q+.2)),(x,y)];curve(d,pts,color,1)
  d.ellipse(((x-.5)*4,(y-.5)*4,(x+.5)*4,(y+.5)*4),outline=color,width=2)
def robe(stage,layer):
 tier=stage//3;base,ivory,gold=PALETTES[tier];im=Image.new('RGBA',(512,512));d=ImageDraw.Draw(im)
 for box in [(16,16,40,32),(40,16,56,32),(0,16,16,32)]:panel(im,box,base,stage)
 # actual body UV (20..28,20..32), contrasting asymmetric crossing collar
 d.polygon([(80,80),(88,80),(111,108),(102,108)],fill=ivory);curve(d,[(27.5,20),(25,23.4),(20,27.7)],gold,3)
 panel(im,(16,28,40,30),shade(base,-29),stage);curve(d,[(16,28.1),(40,28.1)],gold,2);curve(d,[(16,29.5),(40,29.5)],ivory,1)
 for x,y in [(24,25.5),(34,24),(45,24),(52,24),(5,25),(12,25)]:motif(d,x,y,tier,gold)
 for y in range(stage%3+1):curve(d,[(0,30+y*.5),(16,30+y*.5)],gold,1)
 panel(im,(0,28,16,32),shade(base,-26),stage);curve(d,[(0,29),(16,29)],gold,2)
 for rect,c in [((64,0,95,16),shade(base,-13)),((64,16,95,31),gold),((64,32,96,47),ivory),((64,48,96,64),shade(base,-30)),((96,0,110,16),base),((96,16,110,32),(65,162,139)),((80,68,126,86),base),((80,88,124,119),base)]:panel(im,rect,c,stage)
 for x in range(82,125,5):motif(d,x,77,tier,gold);curve(d,[(x,69),(x,84)],shade(base,-8))
 for y in (82,84,114,116):curve(d,[(80,y),(124,y)],gold,2)
 for x in (86,99,112):motif(d,x,102,tier,gold)
 if layer==2:
  d.rectangle((16*4,16*4,40*4-1,28*4-1),fill=(0,0,0,0))
 return im
for stage in range(21):
 for layer in (1,2):save(robe(stage,layer),f'textures/models/armor/robe_{stage}_layer_{layer}.png')
 for part,zh in PARTS.items():
  name=f'robe_{stage}_{part}';base,ivory,gold=PALETTES[stage//3];im=Image.new('RGBA',(64,64));d=ImageDraw.Draw(im)
  if part=='helmet':
   d.polygon([(9,32),(12,26),(49,26),(54,33),(50,39),(13,39)],fill=gold);d.rectangle((17,27,47,35),fill=base);h=14+stage//3*2;d.polygon([(25,29),(24,h),(31,h-6),(38,h),(37,29)],fill=base,outline=gold);d.line([(48,35),(51,56),(46,56)],fill=ivory,width=3)
  elif part=='chestplate':
   d.polygon([(16,10),(26,13),(37,13),(47,10),(59,29),(51,35),(45,26),(45,57),(17,57),(17,26),(11,35),(3,29)],fill=base,outline=shade(base,-25));d.line([(20,11),(40,31)],fill=ivory,width=5);d.line([(44,11),(22,33)],fill=gold,width=2);d.rectangle((17,40,45,44),fill=gold)
   for xx in (20,27,34,41):d.line((xx,45,xx-1,55),fill=shade(base,-16),width=1)
  elif part=='leggings':
   d.polygon([(17,9),(46,9),(52,57),(34,57),(31,31),(28,57),(12,57)],fill=base,outline=ivory);d.rectangle((17,9,46,15),fill=gold)
   for xx in (20,27,37,43):d.line((xx,18,xx-1,55),fill=shade(base,-20),width=2)
  else:
   for x in (11,35):d.polygon([(x,17),(x+13,17),(x+13,44),(x+18,52),(x+16,57),(x-5,57),(x-4,51),(x,46)],fill=base,outline=gold);d.line((x,21,x+13,21),fill=ivory,width=3)
  # Fine identifying medallion fits icons instead of a flat palette swap.
  if part!='helmet':
   tier=stage//3
   for j in range(tier+1):d.point((19+j*3,52),fill=gold)
  save(im,f'textures/item/{name}.png');write(f'models/item/{name}.json',{'parent':'minecraft:item/generated','textures':{'layer0':'duskrain:item/'+name}});lang['item.duskrain.'+name]=REALMS[stage//3]+'·'+['初境','中境','后境'][stage%3]+'·'+zh

def cube(a,b,tex,rotation=None):
 e={'from':a,'to':b,'faces':{f:{'texture':'#'+tex,'uv':[0,0,16,16]} for f in ('up','down','east','west','north','south')}}
 if rotation:e['rotation']=rotation
 return e
for tier in range(7):
 base,ivory,gold=PALETTES[tier]
 for kind,c in [('steel',ivory),('jade',base),('gold',gold),('ribbon',shade(base,-20))]:
  im=Image.new('RGBA',(64,64));d=ImageDraw.Draw(im)
  for y in range(64):
   for x in range(64):im.putpixel((x,y),shade(c,math.cos((x-32)*.08)*10+(x+y)%3-1)+(255,))
  for y in (5,58):d.line((0,y,63,y),fill=shade(c,20),width=1)
  for x in (16,48):d.line((x,0,x,63),fill=shade(c,-12),width=1)
  save(im,f'textures/item/v14_{kind}_{tier}.png')
 for school in (1,2,3):
  elems=[]
  if school==1:
   elems=[cube([7.35,1,7.3],[8.65,7,8.7],'jade'),cube([4.5,6.4,6.9],[11.5,7.5,9.1],'gold'),cube([7,7.5,7.7],[9,25+tier*.4,8.3],'steel'),cube([7.7,8,7.4],[8.3,24+tier*.4,8.6],'gold'),cube([7.5,0,7.5],[8.5,1,8.5],'gold'),cube([10.6,3,7.7],[11.2,7,8.3],'ribbon')]
   elems+=[cube([7.4,25+tier*.4,7.8],[8.6,27+tier*.4,8.2],'steel'),cube([10.3,-2,7.7],[11.5,3,8.3],'ribbon')]
   for side in (-1,1):elems.append(cube([8+side*3-.7,7,7.2],[8+side*3+.7,9+tier*.25,8.8],'jade'))
  elif school==2:
   elems=[cube([7.35,-3,7.35],[8.65,22,8.65],'jade'),cube([6.5,2,6.5],[9.5,3,9.5],'gold'),cube([6.5,17,6.5],[9.5,18,9.5],'gold'),cube([6,22,6],[10,26,10],'steel',{'origin':[8,24,8],'axis':'y','angle':45})]
   for side in (-1,1):elems+=[cube([8+side*4-.7,20,7.3],[8+side*4+.7,28,8.7],'gold'),cube([8+side*2-.5,25,7.5],[8+side*2+.5,29.5,8.5],'ribbon')]
  else:
   elems=[cube([2,1,2],[14,4,14],'gold'),cube([3,4,3],[13,7,13],'jade'),cube([4.5,7,4.5],[11.5,9,11.5],'steel'),cube([6,9,6],[10,12,10],'gold')]
   for side in (-1,1):elems.append(cube([8+side*3-1,9,7],[8+side*3+1,13,9],'jade'))
  if school==1:
   for j in range(tier):elems.append(cube([7.65,9+j*2,7.55],[8.35,10+j*2,7.85],'gold'))
  model={'gui_light':'front','textures':{t:f'duskrain:item/v14_{t}_{tier}' for t in ('steel','jade','gold','ribbon')},'elements':elems,'display':{'gui':{'rotation':[15,-30,-35 if school==1 else 0],'translation':[0,-3 if school<3 else 0,0],'scale':[.66,.66,.66]},'firstperson_righthand':{'rotation':[0,-90,20],'translation':[1,1,0],'scale':[.65,.65,.65]},'firstperson_lefthand':{'rotation':[0,90,-20],'translation':[1,1,0],'scale':[.65,.65,.65]},'thirdperson_righthand':{'rotation':[0,90,0],'translation':[0,3,1],'scale':[.65,.65,.65]},'thirdperson_lefthand':{'rotation':[0,-90,0],'translation':[0,3,1],'scale':[.65,.65,.65]},'ground':{'translation':[0,3,0],'scale':[.32,.32,.32]},'fixed':{'rotation':[0,0,0],'translation':[0,-6,0],'scale':[1,1,1]}}}
  write(f'models/item/artifact_{school}_{tier}.json',model);lang[f'item.duskrain.artifact_{school}_{tier}']=['炼气','筑基','金丹','元婴','化神','渡劫','登神'][tier]+'·'+['','听雨剑','凝霜杖','镇岳印'][school]

ROLES=[('materials','沈掌柜',0,0),('pills','青萝',1,1),('elixirs','白芷',2,1),('artifacts','墨衡',3,0),('master','松玄',2,2),('quests','闻溪',3,1),('trial','渡云',5,0),('travel','归舟',0,0),('treasures','琳琅',4,1),('guild','云岑',5,2),('enchanter','云篆',6,0)]
faces=Image.new('RGB',(6*180,2*200),(225,221,203));fd=ImageDraw.Draw(faces);font=ImageFont.truetype('C:/Windows/Fonts/msyh.ttc',18)
for idx,(ident,name,tier,sex) in enumerate(ROLES):
 base,ivory,gold=PALETTES[tier];im=Image.new('RGBA',(512,512));d=ImageDraw.Draw(im);skin=(224,183,151) if sex!=1 else (237,200,177);hair=(166,171,166) if sex==2 else (30+idx%3*4,30,35)
 panel(im,(0,0,32,16),hair,idx)
 panel(im,(8,8,16,16),skin,idx)
 # Faceted contour follows the front-face boundary, with painted soft brow arches.
 curve(d,[(8,9),(8.2,14),(9.2,15.6),(14.7,15.6),(15.8,14),(15.8,9)],shade(skin,-16),2)
 curve(d,[(8,8.5),(10,8.1),(11.3,9),(12.8,8.2),(15.8,8.7)],hair,5)
 for ex in (9.15,12.7):
  brow_y=10.28+(idx%4)*.13
  curve(d,[(ex,brow_y+.1),(ex+.65,brow_y-.12),(ex+1.8,brow_y+.05)],hair,1 if sex==1 else 2)
  d.polygon([(ex*4,11.1*4),((ex+.7)*4,10.98*4),((ex+1.8)*4,11.2*4),((ex+1.6)*4,11.85*4),((ex+.35)*4,11.85*4)],fill=(242,232,211))
  d.rectangle(((ex+.8)*4,11.05*4,(ex+1.3)*4,11.9*4),fill=(48,66+idx*2,64));d.point(((ex+.9)*4,11.1*4),fill=(250,242,210));curve(d,[(ex-.1,11.05),(ex+.9,10.98),(ex+1.8,11.1)],shade(hair,-5),1)
 curve(d,[(11.6,11.8),(11.4,13.1),(12.3,13.35)],shade(skin,-23),1);curve(d,[(11,14.2),(11.7,14.35),(12.5,14.27),(13,14.1)],(146,81,76) if sex==1 else (152,103,89),1)
 if sex==2:curve(d,[(10.7,13.8),(11.5,13.65),(12.5,13.65),(13.3,13.8)],hair,2);curve(d,[(11,14.7),(11.7,15.8),(12.6,15.8),(13,14.7)],hair,3)
 if ident=='artifacts':curve(d,[(10.5,14.9),(11.5,15.4),(13,15.1)],shade(hair,20),2)
 if ident=='materials':
  for ex in (10.5,12.5):curve(d,[(ex,13.6),(ex-.2,14),(ex+.7,13.9)],hair,2)
 if ident in ('pills','treasures','quests'):
  d.ellipse((8.7*4,12.2*4,10.2*4,12.7*4),fill=shade(skin,-10));d.ellipse((13.5*4,12.2*4,15*4,12.7*4),fill=shade(skin,-10))
 if ident=='enchanter':
  curve(d,[(8,8.3),(9.8,8),(11.8,8.5),(13,10),(13.6,8.3),(15.8,8.2)],hair,3)
  curve(d,[(11.8,9.2),(12.2,8.9),(12.6,9.3),(12.2,9.65),(11.8,9.2)],gold,1)
 if ident=='travel':curve(d,[(8,8.1),(10,8.2),(9,10.2)],hair,4)
 if ident=='trial':curve(d,[(13.8,9.7),(14.4,10.9),(14.5,12.1)],shade(skin,-27),1)
 if ident=='elixirs':d.point((13.9*4,13*4),fill=(95,59,48))
 for rect in [(16,16,40,32),(40,16,56,32),(32,48,48,64),(0,16,16,32),(16,48,32,64)]:panel(im,rect,base,idx)
 d.polygon([(80,80),(87,80),(111,108),(104,108)],fill=ivory);curve(d,[(27.5,20),(25,23),(20,27.5)],gold,3)
 panel(im,(16,28,40,30),shade(base,-28),idx);curve(d,[(16,28.1),(40,28.1)],gold,2)
 for ex,ey in [(24,25),(34,24),(46,25),(39,57)]:motif(d,ex,ey,tier,gold)
 for rect in [(40,29,56,32),(32,61,48,64)]:panel(im,rect,skin,idx)
 for rect in [(0,29,16,32),(16,61,32,64)]:panel(im,rect,shade(base,-35),idx)
 for rect,col in [((64,0,95,16),hair),((64,16,95,24),gold),((64,24,95,36),ivory),((64,36,84,58),shade(base,-26)),((84,36,128,58),base),((96,64,128,96),ivory)]:panel(im,rect,col,idx)
 for x in range(86,124,5):motif(d,x,46,tier,gold)
 for y in (51,53):curve(d,[(84,y),(127,y)],gold,2)
 for y in range(66,90,2):curve(d,[(97,y),(126,y)],shade(ivory,-20),1)
 save(im,f'textures/entity/resident_{ident}.png')
 face=im.crop((32,32,64,64)).resize((128,128),Image.Resampling.NEAREST);faces.paste(face,(idx%6*180+26,idx//6*200+15),face);fd.text((idx%6*180+53,idx//6*200+158),name,font=font,fill=(38,72,73))
faces.save(QA/'v14-npc-uv-faces.png')
# Original ingredient icons and deterministic shaped recipe.
for name,color in [('rune_sand',(69,172,167)),('tribulation_core',(156,130,235)),('divine_crystal',(231,192,85))]:
 im=Image.new('RGBA',(64,64));d=ImageDraw.Draw(im)
 for n in range(3):
  x=17+n*13;y=21+(n%2)*10;d.polygon([(x,y-12),(x+10,y),(x+6,y+21),(x-7,y+20),(x-10,y)],fill=color,outline=shade(color,45));d.line([(x,y-12),(x-2,y+17),(x+6,y+21)],fill=shade(color,-40),width=2)
 save(im,f'textures/item/{name}.png');write(f'models/item/{name}.json',{'parent':'minecraft:item/generated','textures':{'layer0':'duskrain:item/'+name}})
lang.update({'item.duskrain.rune_sand':'灵纹砂','item.duskrain.tribulation_core':'劫雷灵核','item.duskrain.divine_crystal':'神纹晶','entity.duskrain.divine_sentinel':'天门神将','entity.duskrain.flying_sword':'御风飞剑','key.duskrain.flight':'起剑 / 收剑'})
for ident,name in zip(['sword_echo','arcane','mountain','spirit','renewal','heartguard','cloudstep','windrider'],['剑鸣','通玄','镇岳','聚灵','归元','护心','踏云','御风']):lang['enchantment.duskrain.'+ident]=name
write('lang/zh_cn.json',lang)
recipe=ROOT/'mod/src/main/resources/data/duskrain/recipes/rune_sand.json';recipe.write_text(json.dumps({'type':'minecraft:crafting_shaped','pattern':['ALA','LPL','ALA'],'key':{'A':{'item':'minecraft:amethyst_shard'},'L':{'item':'minecraft:lapis_lazuli'},'P':{'item':'minecraft:paper'}},'result':{'item':'duskrain:rune_sand','count':2}}),encoding='utf8')
# Distinct ceremonial guardian with broad sleeves, a tiered crown and halo.
bones=[{'name':'body','pivot':[0,16,0],'cubes':[{'origin':[-6,10,-3.5],'size':[12,20,7],'uv':[0,0]},{'origin':[-7,4,-4],'size':[14,10,8],'uv':[0,30]}]}, {'name':'head','parent':'body','pivot':[0,30,0],'cubes':[{'origin':[-4,30,-4],'size':[8,8,8],'uv':[48,0]},{'origin':[-6,38,-2],'size':[12,2,4],'uv':[48,18]},{'origin':[-2,40,-2],'size':[4,5,4],'uv':[64,24]}]}]
for side in [-1,1]:bones.append({'name':'left_arm' if side>0 else 'right_arm','parent':'body','pivot':[side*7,28,0],'cubes':[{'origin':[5 if side>0 else -12,13,-4],'size':[7,16,8],'uv':[80,0]}]})
for side in [-1,1]:bones.append({'name':'left_leg' if side>0 else 'right_leg','parent':'body','pivot':[side*3,12,0],'cubes':[{'origin':[.5 if side>0 else -5.5,0,-3],'size':[5,12,6],'uv':[0,52]}]})
bones.append({'name':'tail','parent':'body','pivot':[0,20,4],'cubes':[{'origin':[-1,7,4],'size':[2,13,1],'uv':[100,40]}]})
for j in range(12):
 a=j*math.tau/12;bones[0]['cubes'].append({'origin':[math.cos(a)*17-1,math.sin(a)*17+27,5],'size':[2,3,1],'uv':[116,0]})
write('geo/divine_sentinel.geo.json',{'format_version':'1.12.0','minecraft:geometry':[{'description':{'identifier':'geometry.divine_sentinel','texture_width':128,'texture_height':128,'visible_bounds_width':5,'visible_bounds_height':6,'visible_bounds_offset':[0,2,0]},'bones':bones}]})
im=Image.new('RGBA',(512,512));panel(im,(0,0,128,128),PALETTES[6][0],5);d=ImageDraw.Draw(im)
for x in range(4,120,8):
 for y in range(6,120,10):motif(d,x,y,6,PALETTES[6][2])
save(im,'textures/entity/divine_sentinel.png')
manifest={'author':'DuskRain original code-authored art','uv_pixels':512,'armors':84,'artifact_models':21,'npcs':11,'generator':'tools/generate_v14_art.py','references':['https://www.dpm.org.cn/lemmas/241941.html','https://www.dpm.org.cn/collection/embroider/228698.html'],'reference_images_bundled':False}
(QA/'v14-art-manifest.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2),encoding='utf8')
print('Authored 84 robe icons, 42 512px UV atlases, 21 sculpted weapons, 11 NPC UV atlases, and divine guardian.')
