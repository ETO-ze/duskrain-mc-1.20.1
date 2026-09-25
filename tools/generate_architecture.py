"""Original DuskRain building palette. All generated pixels are in duskrain, never minecraft."""
from pathlib import Path
import json, zipfile, math, shutil
from PIL import Image, ImageDraw
import generate_pbr
R=Path(__file__).resolve().parents[1]
A=R/'mod/src/main/resources/assets/duskrain'
DATA=R/'mod/src/main/resources/data'
def write(p,obj):
 p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(obj,ensure_ascii=False,indent=2),encoding='utf8')
def cube(a,b,t):return {'from':a,'to':b,'faces':{f:{'texture':'#'+t} for f in ('north','south','east','west','up','down')}}
SPECS={
'white_jade':('smooth_quartz','quartz_block_top','汉白玉'), 'jade_bricks':('quartz_bricks','quartz_bricks','汉白玉砖'),
'jade_carved':('chiseled_quartz_block','chiseled_quartz_block','云纹白玉'), 'plaster':('calcite','calcite','白灰墙'),
'grey_bricks':('stone_bricks','stone_bricks','青灰城砖'), 'grey_carved':('chiseled_stone_bricks','chiseled_stone_bricks','回纹青砖'),
'qingwa':('dark_prismarine','qingwa_detail','青瓦铺面'), 'roof_board':('deepslate_tiles','qingwa_detail','叠瓦铺面'),
'cedar_boards':('spruce_planks','spruce_planks','杉木地板'), 'dark_boards':('dark_oak_planks','dark_oak_planks','深木壁板'),
'timber':('stripped_dark_oak_log','stripped_dark_oak_log','深木梁柱'), 'red_timber':('stripped_mangrove_log','red_timber','朱褐梁柱'),
'grey_paver':('polished_deepslate','polished_deepslate','青石铺地'), 'stone_paver':('smooth_stone','polished_deepslate','细磨青石'),
'grey_plinth':('polished_andesite','stone_bricks','青石柱础'), 'gilt_carved':('chiseled_sandstone','aged_bronze','鎏金雕花'),
'jade_light':('sea_lantern','sea_lantern','玉纹地灯'), 'warm_light':('shroomlight','shroomlight','暖玉灯砖'),
'jade_stairs':('quartz_stairs','quartz_block_top','白玉阶'), 'jade_slab':('quartz_slab','quartz_block_top','白玉半砖'),
'jade_smooth_slab':('smooth_quartz_slab','quartz_block_top','白玉压顶'), 'grey_stairs':('stone_brick_stairs','stone_bricks','青砖阶'),
'grey_slab':('stone_brick_slab','stone_bricks','青砖半砖'), 'qingwa_stairs':('dark_prismarine_stairs','qingwa_detail','筒瓦坡面'),
'qingwa_slab':('dark_prismarine_slab','qingwa_detail','板瓦檐面'), 'roof_stairs':('deepslate_tile_stairs','qingwa_detail','叠瓦坡面'),
'roof_slab':('deepslate_tile_slab','qingwa_detail','叠瓦檐面'), 'cedar_stairs':('spruce_stairs','spruce_planks','杉木阶'),
'cedar_slab':('spruce_slab','spruce_planks','杉木半板'), 'dark_stairs':('dark_oak_stairs','dark_oak_planks','深木阶'),
'dark_slab':('dark_oak_slab','dark_oak_planks','深木半板'), 'jade_railing':('oak_fence','quartz_block_top','白玉望柱栏杆'),
'carved_railing':('spruce_fence','carved_timber','木雕栏杆'), 'carved_door':('spruce_door','door','花格木门'),
'palace_lantern':('lantern','silk','六方绢面宫灯')}
SPECIAL={'lamp_base':'宫灯白玉底座','lamp_shaft':'宫灯细柱','lamp_crown':'宫灯灯头','garden_lamp':'白玉庭灯','lotus_lamp':'莲花水灯','lotus':'粉荷'}

def main():
 # Keep the same authored height-field workflow, but never use the global vanilla namespace.
 generate_pbr.MATERIALS['red_timber']=('column',(113,60,43))
 generate_pbr.generate()
 tex=A/'textures/block';tex.mkdir(parents=True,exist_ok=True)
 for name,base in [('silk',(226,210,162)),('lotus',(222,148,174)),('leaf',(58,102,77)),('door',(72,51,36))]:
  im=Image.new('RGB',(64,64),base);d=ImageDraw.Draw(im)
  if name=='silk':
   for y in range(0,64,2):d.line((0,y,63,y),fill=(218,202,153))
   d.rectangle((4,4,59,59),outline=(136,102,51),width=2)
   for x,y in [(22,22),(43,39)]:d.arc((x-9,y-5,x+9,y+5),10,310,fill=(168,140,92),width=2)
  elif name=='door':
   d.rectangle((3,3,60,60),outline=(136,108,61),width=3)
   for k in range(12,56,10):d.line((9,k,54,k),fill=(158,137,95),width=2);d.line((k,9,k,54),fill=(158,137,95),width=2)
  else:
   for k in range(4,64,8):d.line((32,32,k,1),fill=tuple(min(255,c+18) for c in base))
  im.save(tex/(name+'.png'))
  if name=='door':
   for suffix in ('_top','_bottom'):im.save(tex/(name+suffix+'.png'))
 jar=Path('E:/.minecraft/versions/DuskRain-1.20.1/DuskRain-1.20.1.jar')
 with zipfile.ZipFile(jar) as vanilla:
  for name,(src,texture,label) in SPECS.items():
   state=json.loads(vanilla.read(f'assets/minecraft/blockstates/{src}.json')); models={}
   def visit(v):
    if isinstance(v,dict):
     if 'model' in v:
      old=v['model'].split('/')[-1];suffix=old[len(src):] if old.startswith(src) else '_double'
      model=name+suffix;models[model]=old;v['model']='duskrain:block/'+model
     for value in v.values():visit(value)
    elif isinstance(v,list):
     for value in v:visit(value)
   visit(state);write(A/f'blockstates/{name}.json',state)
   for model,old in models.items():
    obj=json.loads(vanilla.read('assets/minecraft/models/block/'+old+'.json'))
    obj['textures']={key:'duskrain:block/'+(texture+'_top' if name=='carved_door' and key=='top' else texture+'_bottom' if name=='carved_door' and key=='bottom' else texture) for key in obj.get('textures',{'all':''})}
    if name in ('timber','red_timber'):obj['textures']['end']='duskrain:block/stripped_dark_oak_log_top'
    if name.startswith(('qingwa_','roof_')) and ('stairs' in name or 'slab' in name):
     parent=obj.get('parent','').split('/')[-1]
     try:geometry=json.loads(vanilla.read('assets/minecraft/models/block/'+parent+'.json'))['elements']
     except (KeyError,TypeError):geometry=[]
     if geometry:
      obj.pop('parent',None);obj['elements']=geometry
      # Shallow raised cover-tile ribs fit within the voxel, including outer corners.
      for el in list(geometry):
       if 'up' not in el.get('faces',{}):continue
       a,b=el['from'],el['to'];top=b[1]
       if top>=16:continue
       for z in range(2,16,5):
        if a[2]<=z and z+2<=b[2]:obj['elements'].append(cube([a[0],top,z],[b[0],min(16,top+1),z+2],'top'))
      obj['ambientocclusion']=True
    write(A/f'models/block/{model}.json',obj)
   item={'parent':'duskrain:block/'+name}
   if name.endswith('railing'):
    write(A/f'models/block/{name}_inventory.json',{'parent':'minecraft:block/fence_inventory','textures':{'texture':'duskrain:block/'+texture}});item['parent']+='_inventory'
   if name=='carved_door':item={'parent':'minecraft:item/generated','textures':{'layer0':'duskrain:block/door'}}
   write(A/f'models/item/{name}.json',item)
 # Static geometry for slim four-block lanterns and low lights.
 textures={'jade':'duskrain:block/quartz_block_top','wood':'duskrain:block/carved_timber','gold':'duskrain:block/aged_bronze','silk':'duskrain:block/silk','leaf':'duskrain:block/leaf','pink':'duskrain:block/lotus','particle':'duskrain:block/carved_timber'}
 details={
 'lamp_base':[cube([2,0,2],[14,3,14],'jade'),cube([4,3,4],[12,8,12],'jade'),cube([5,8,5],[11,10,11],'gold'),cube([6,10,6],[10,16,10],'wood')],
 'lamp_shaft':[cube([6,0,6],[10,16,10],'wood'),cube([5.5,0,5.5],[10.5,1,10.5],'gold')],
 'garden_lamp':[cube([2,0,2],[14,3,14],'jade'),cube([4,3,4],[12,11,12],'silk'),cube([2,11,2],[14,13,14],'jade'),cube([5,13,5],[11,15,11],'gold')],
 'lotus':[cube([1,0,1],[15,1,15],'leaf'),cube([5,1,5],[11,3,11],'pink'),cube([6,3,6],[10,5,10],'pink')],
 'lotus_lamp':[cube([1,0,1],[15,1,15],'leaf'),cube([3,1,3],[13,3,13],'pink'),cube([5,3,5],[11,5,11],'silk')]}
 for name,els in details.items():
  write(A/f'models/block/{name}.json',{'textures':textures,'elements':els})
 # Hexagonal timber-and-silk lanterns: true six-sided static OBJ, no ticking renderer.
 for name,offset in [('palace_lantern',0),('palace_lantern_hanging',0),('lamp_crown',0)]:
  verts=[];faces=[]
  def prism(radius,y0,y1,mat):
   start=len(verts)
   for y in (y0,y1):
    for i in range(6):verts.append((.5+radius*math.cos(i*math.pi/3),y,.5+radius*math.sin(i*math.pi/3)))
   for i in range(6):faces.append((mat,[start+i,start+(i+1)%6,start+(i+1)%6+6,start+i+6]))
   faces.append((mat,[start+i for i in reversed(range(6))]));faces.append((mat,[start+6+i for i in range(6)]))
  prism(.26,.2,.73,'silk');prism(.34,.14,.22,'wood');prism(.34,.73,.80,'wood');prism(.27,.80,.88,'gold');prism(.16,.88,.95,'wood');prism(.04,.95,1,'gold')
  if name=='lamp_crown':prism(.10,0,.15,'wood')
  # Narrow timber posts around the six silk panels.
  for i in range(6):
   cx=.5+.27*math.cos(i*math.pi/3);cz=.5+.27*math.sin(i*math.pi/3);start=len(verts)
   for y in (.19,.76):
    for dx,dz in [(-.018,-.018),(.018,-.018),(.018,.018),(-.018,.018)]:verts.append((cx+dx,y,cz+dz))
   for j in range(4):faces.append(('wood',[start+j,start+(j+1)%4,start+(j+1)%4+4,start+j+4]))
  obj=['mtllib palace_lantern.mtl']+[f'v {x:.5f} {y:.5f} {z:.5f}' for x,y,z in verts]+['vt 0 0','vt 1 0','vt 1 1','vt 0 1']
  for mat,vs in faces:
   obj.append('usemtl '+mat)
   for j in range(1,len(vs)-1):obj.append('f '+f'{vs[0]+1}/1 {vs[j]+1}/2 {vs[j+1]+1}/3')
  (A/f'models/block/{name}.obj').write_text('\n'.join(obj),encoding='utf8')
  write(A/f'models/block/{name}.json',{'loader':'forge:obj','model':f'duskrain:models/block/{name}.obj','flip_v':True,'automatic_culling':False,'shade_quads':True,'textures':{'particle':'duskrain:block/carved_timber'}})
 (A/'models/block/palace_lantern.mtl').write_text('\n'.join(f'newmtl {m}\nKd 1 1 1\nmap_Kd duskrain:block/{t}' for m,t in [('wood','carved_timber'),('gold','aged_bronze'),('silk','silk')]),encoding='utf8')
 for name in SPECIAL:
  write(A/f'blockstates/{name}.json',{'variants':{'':{'model':'duskrain:block/'+name}}});write(A/f'models/item/{name}.json',{'parent':'duskrain:block/'+name})
 labels={**{k:v[2] for k,v in SPECS.items()},**SPECIAL}
 for lang in ('zh_cn','en_us'):
  p=A/f'lang/{lang}.json';data=json.loads(p.read_text('utf8'));data.update({'block.duskrain.'+k:v if lang=='zh_cn' else k.replace('_',' ').title() for k,v in labels.items()});write(p,data)
 for name in labels:
  write(DATA/f'duskrain/loot_tables/blocks/{name}.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'duskrain:'+name}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
  base='minecraft:quartz' if 'jade' in name else 'minecraft:clay_ball' if 'wa' in name or 'roof' in name else 'minecraft:stick'
  write(DATA/f'duskrain/recipes/{name}.json',{'type':'minecraft:crafting_shapeless','ingredients':[{'item':base},{'item':'minecraft:amethyst_shard'}],'result':{'item':'duskrain:'+name,'count':8}})
 for tag,values in [('fences',['duskrain:jade_railing','duskrain:carved_railing']),('wooden_fences',['duskrain:carved_railing'])]:
  write(DATA/f'minecraft/tags/blocks/{tag}.json',{'replace':False,'values':values})
 for tag in ('mineable/pickaxe','mineable/axe'):
  names=[n for n in labels if any(k in n for k in ('timber','cedar','door','dark_','carved_railing')) == (tag=='mineable/axe')]
  write(DATA/f'minecraft/tags/blocks/{tag}.json',{'replace':False,'values':['duskrain:'+n for n in names]})
 write(R/'docs/qa/architecture-catalog.json',{'blocks':labels,'namespace':'duskrain','vanilla_texture_overrides':0})
 print('Architecture assets:',len(labels),'custom blocks')

if __name__=='__main__':main()
