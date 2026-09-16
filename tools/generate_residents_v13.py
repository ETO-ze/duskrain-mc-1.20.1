"""Author original NPC UV atlases and elixir icons directly from garment/face geometry.

No downloaded skins or textures. Logical 128px atlas at 2x; first quadrant follows
PlayerModel UV, remaining area supplies the modeled bun, collar, belt and tassel.
"""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont
import json, math, hashlib

ROOT=Path(__file__).resolve().parents[1]
R=ROOT/'mod/src/main/resources/assets/duskrain'
ROLES=[
 ('materials','沈掌柜', (65,87,79),(214,203,171),(172,139,74),False,False),
 ('pills','青萝',(85,134,111),(233,229,202),(178,154,88),True,False),
 ('elixirs','白芷',(138,156,123),(235,230,208),(139,111,66),True,False),
 ('artifacts','墨衡',(61,72,78),(187,164,130),(183,112,64),False,False),
 ('master','松玄',(186,200,192),(240,234,211),(145,125,80),False,True),
 ('quests','闻溪',(97,124,152),(234,224,200),(154,137,92),True,False),
 ('trial','渡云',(53,75,103),(216,221,210),(180,151,87),False,False),
 ('travel','归舟',(66,129,133),(226,226,202),(160,141,87),False,False),
 ('treasures','琳琅',(116,69,86),(235,204,179),(204,170,84),True,False),
 ('guild','云岑',(60,86,111),(226,219,184),(196,166,91),False,True),
]
def shade(c,n):return tuple(max(0,min(255,v+n)) for v in c)
def atlas(role):
 ident,name,cloth,ivory,gold,female,elder=role
 skin=(220,177,145) if not female else (238,198,171)
 hair=(159,163,158) if elder else (37,35,39) if female else (46,40,35)
 im=Image.new('RGBA',(256,256));d=ImageDraw.Draw(im)
 def rect(box,c):d.rectangle(tuple(round(v*2) for v in box),fill=c)
 def ln(points,c,width=1):d.line([(int(x*2),int(y*2)) for x,y in points],fill=c,width=width)
 # Proper six-face UV regions; top/back hair and front ears share a deliberate tone.
 rect((0,0,31.5,15.5),hair);rect((8,8,15.5,15.5),skin)
 rect((.5,10,2,14),skin);rect((28,10,30,14),shade(skin,-13))
 for x in range(16,32):ln([(x,8),(x-.5,15)],shade(hair,(x%4)*5-6))
 # 16x16 face: shaded temples, one highlight in each pupil, brows, nose and lips.
 rect((8,8,15.5,9.2),hair);rect((8,9,8.5,14),shade(skin,-18));rect((15,9,15.5,14),shade(skin,-16))
 if female:ln([(9,9),(10,8.8),(11,9.3)],hair,2);ln([(13,9),(15,8.7)],hair,2)
 for x in (9.2,12.7):
  ln([(x,10.3),(x+1.6,10.1)],shade(hair,-3),2)
  rect((x,11,x+1.7,12.1),(234,226,207));rect((x+.7,11,x+1.3,12.2),(54,68,68));rect((x+.8,11,x+1.05,11.25),(255,248,217))
 ln([(11.6,11.4),(11.4,13),(12,13.2)],shade(skin,-27));ln([(11.1,14.1),(12.7,14.1)],(164,94,89) if female else (145,96,80),1)
 rect((9,13.1,10.1,13.5),shade(skin,-6));rect((13.8,13.1,14.7,13.5),shade(skin,-6))
 if elder:
  ln([(10.5,13.5),(11.2,13.2),(12.7,13.2),(13.5,13.5)],hair,2)
  ln([(10.9,14.4),(11.6,15.5),(12.4,15.5),(13,14.5)],hair,2)
 elif ident in ('materials','artifacts'):
  ln([(10.8,14.8),(12,15.2),(13.1,14.8)],shade(hair,15),2)
 # Robe torso, arms and separately mapped legs.
 for box in [(16,16,39.5,31.5),(40,16,55.5,31.5),(32,48,47.5,63.5),(0,16,15.5,31.5),(16,48,31.5,63.5)]:rect(box,cloth)
 for x0,z0,x1,z1 in [(16,20,40,32),(40,20,56,30),(32,52,48,62),(0,20,16,31),(16,52,32,63)]:
  for x in range(x0,x1):ln([(x,z0),(x,z1-.5)],shade(cloth,(-9,-4,1,4,1,-3)[x%6]))
 # Crossed lapels, inner undergarment, sash and fine cloud embroidery.
 d.polygon([(40,40),(44,40),(55,54),(51,54)],fill=ivory)
 ln([(27,20),(25.2,22.8),(20,27)],gold,2);ln([(20,20),(22,20),(27,26)],shade(ivory,9),2)
 rect((16,28,39.5,29.5),shade(cloth,-29));rect((23.3,28,25,29.5),gold)
 for center in (21,26,33,37):
  ln([(center-1,26),(center-1,25.3),(center,25),(center+1,25.6),(center+1.8,25.6)],shade(gold,10))
 for x,y in [(40,26),(32,58)]:rect((x,y,x+15.5,y+1.2),ivory);rect((x,y+1.3,x+15.5,y+1.8),gold);rect((x,y+2,x+15.5,y+5.5),skin)
 for x,y in [(0,28),(16,60)]:rect((x,y,x+15.5,y+3.5),shade(cloth,-37));ln([(x,y+.5),(x+15,y+.5)],gold)
 # Individual occupational medallion on the back; stitched borders instead of a large flat patch.
 rect((32.3,21,35.7,24.7),shade(cloth,-18));ln([(32.5,21),(35.5,21),(35.5,24.5),(32.5,24.5),(32.5,21)],gold)
 motif=ROLES.index(role)%5
 for j in range(3):ln([(33,22+j*.6),(34+j*.3,21.8+j*.6),(35,22.2+j*.6)],ivory if motif%2 else gold)
 if ident=='artifacts':rect((21,26,26.5,28),(106,74,52));ln([(21.5,26),(21.5,28)],gold)
 # Accessory material banks: each cube's UV fits within its swatch.
 rect((64,0,95,15),hair);rect((64,16,95,23),gold);rect((64,24,95,35),ivory);rect((64,36,83,55),shade(cloth,-25));rect((84,36,127,55),cloth)
 for x in range(64,96,3):ln([(x,0),(x+1,14)],shade(hair,9))
 for x in range(84,126,4):ln([(x,42),(x+1,44),(x+2,42)],gold)
 for y in (47,48):ln([(84,y),(127,y)],ivory)
 return im

out=R/'textures/entity';out.mkdir(parents=True,exist_ok=True)
manifest=[]
for role in ROLES:
 p=out/f'resident_{role[0]}.png';im=atlas(role);im.save(p)
 manifest.append({'id':role[0],'name':role[1],'texture':str(p.relative_to(R)),'sha256':hashlib.sha256(p.read_bytes()).hexdigest(),'author':'DuskRain original','dimensions':[256,256]})
font=ImageFont.truetype('C:/Windows/Fonts/msyh.ttc',20)
sheet=Image.new('RGB',(1200,480),(229,226,208));d=ImageDraw.Draw(sheet)
for i,role in enumerate(ROLES):
 x=i%5*240;y=i//5*240;im=atlas(role)
 face=im.crop((16,16,32,32)).resize((112,112),Image.Resampling.NEAREST)
 sheet.paste(face,(x+64,y+24),face);d.text((x+58,y+151),role[1],font=font,fill=(35,65,67))
 d.text((x+32,y+188),role[0],font=font,fill=(113,96,61))
sheet.save(ROOT/'docs/qa/resident-faces-v13.png')
(ROOT/'docs/qa/resident-atlases-v13.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2),encoding='utf-8')
lang=json.loads((R/'lang/zh_cn.json').read_text(encoding='utf-8'))
for i,(name,color) in enumerate(zip(['轻身露','养元散','破锋散','回灵露'],[(75,174,151),(222,133,121),(218,170,77),(107,149,210)])):
 im=Image.new('RGBA',(32,32));d=ImageDraw.Draw(im);d.rounded_rectangle((8,9,24,29),4,fill=(37,66,69),outline=(196,180,126),width=1);d.rectangle((12,4,20,10),fill=(174,145,82));d.rectangle((11,5,21,7),fill=(230,212,161));d.rounded_rectangle((10,13,22,26),3,fill=color);d.line((12,14,12,21),fill=(241,237,207),width=1);d.rectangle((14,16,19,22),fill=(229,222,189));d.line((15,18,18,18),fill=(79,100,91));d.line((16,17,16,21),fill=(79,100,91));p=R/f'textures/item/elixir_{i}.png';im.resize((64,64),Image.Resampling.NEAREST).save(p)
 (R/f'models/item/elixir_{i}.json').write_text(json.dumps({'parent':'minecraft:item/generated','textures':{'layer0':f'duskrain:item/elixir_{i}'}}))
 lang[f'item.duskrain.elixir_{i}']=name
(R/'lang/zh_cn.json').write_text(json.dumps(lang,ensure_ascii=False,indent=2),encoding='utf-8')
print('Created 10 original 256px NPC atlases, 4 elixir icons, and atlas manifest.')
