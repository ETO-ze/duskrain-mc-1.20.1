package cn.duskrain;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.*;

/** Authored palace plan. Per-chunk stamps travel with the blocks they acknowledge. */
public final class GuildPalace {
    record Placement(BlockPos pos,BlockState state){}
    static final Map<Long,LinkedHashMap<BlockPos,BlockState>> BLOCKS=new HashMap<>();
    static final Map<Long,List<CompoundTag>> TAGS=new HashMap<>();
    static final Set<BlockPos> BRIDGE_DECK=new HashSet<>();
    static boolean planned;
    static final BlockState JADE=Blocks.DARK_PRISMARINE.defaultBlockState(),WHITE=Blocks.SMOOTH_QUARTZ.defaultBlockState(),TIMBER=Blocks.STRIPPED_MANGROVE_LOG.defaultBlockState(),WALL=Blocks.CALCITE.defaultBlockState();
    static final CityPlan.Sink S=new CityPlan.Sink(){public void block(int x,int y,int z,BlockState b){if(Math.abs(x)>111||Math.abs(z)>111||y<62||y>160)return;var pos=new BlockPos(x,y,z);BLOCKS.computeIfAbsent(key(x>>4,z>>4),k->new LinkedHashMap<>()).put(pos,Architecture.material(b));}public void entity(CompoundTag n){TAGS.computeIfAbsent(key(n.getInt("x")>>4,n.getInt("z")>>4),k->new ArrayList<>()).add(n.copy());}};
    static long key(int x,int z){return net.minecraft.world.level.ChunkPos.asLong(x,z);}
    static void box(int ax,int ay,int az,int bx,int by,int bz,BlockState b){CityPlan.box(S,ax,ay,az,bx,by,bz,b);}
    static void block(int x,int y,int z,Block b){S.block(x,y,z,b.defaultBlockState());}
    static void lane(int ax,int az,int bx,int bz){int n=Math.max(Math.abs(bx-ax),Math.abs(bz-az));for(int i=0;i<=n;i++){int x=ax+(bx-ax)*i/Math.max(1,n),z=az+(bz-az)*i/Math.max(1,n);for(int k=-2;k<=2;k++){int px=x+(az!=bz?k:0),pz=z+(ax!=bx?k:0);block(px,64,pz,Math.abs(k)==2?Blocks.SMOOTH_QUARTZ:Blocks.STONE_BRICKS);box(px,65,pz,px,68,pz,Blocks.AIR.defaultBlockState());if(i%9==4&&Math.abs(k)==2)block(px,64,pz,Blocks.SEA_LANTERN);}}}
    static void lamp(int x,int z){if(!Architecture.LEGACY.get()){Architecture.lamp(S,x,65,z);return;}block(x,64,z,Blocks.STONE_BRICKS);block(x,65,z,Blocks.CHISELED_STONE_BRICKS);box(x,66,z,x,68,z,TIMBER);block(x,69,z,Blocks.DARK_PRISMARINE_SLAB);S.block(x+1,68,z,Blocks.LANTERN.defaultBlockState());}
    public static void plan(){if(planned)return;planned=true;
        // Existing level ground remains outside foundations; northern hills and western water change the silhouette.
        for(int x=-110;x<=110;x++)for(int z=-110;z<=-80;z++){double height=23*Math.exp(-Math.pow((x+32)/35d,2)-Math.pow((z+96)/13d,2))+18*Math.exp(-Math.pow((x-45)/29d,2)-Math.pow((z+100)/17d,2));int h=(int)height;if(h>0){box(x,64,z,x,64+h,z,Math.floorMod(x+z,7)==0?Blocks.ANDESITE.defaultBlockState():Blocks.STONE.defaultBlockState());block(x,65+h,z,Math.floorMod(x*3+z,11)==0?Blocks.MOSS_BLOCK:Blocks.GRASS_BLOCK);}}
        for(int x=-103;x<=-49;x++)for(int z=-45;z<=38;z++){double q=Math.pow((x+76)/24d,2)+Math.pow((z+4)/37d,2);if(q<1){block(x,62,z,Blocks.CLAY);block(x,63,z,Blocks.WATER);block(x,64,z,Blocks.WATER);if(Math.floorMod(x*13+z*7,83)==0)block(x,65,z,Blocks.LILY_PAD);}else if(q<1.14){block(x,64,z,Math.floorMod(x+z,4)==0?Blocks.MOSSY_COBBLESTONE:Blocks.ANDESITE);}}
        // A real cliff-fed fall drains into a level, enclosed rill. No elevated ribbon
        // of independent water sources spilling over both sides of the garden.
        for(int x=-77;x<=-57;x++)for(int z=-100;z<=-86;z++){int h=(int)(15*Math.max(0,1-Math.pow((x+66)/12d,2)-Math.pow((z+94)/10d,2)));if(h>0){box(x,64,z,x,64+h,z,Math.floorMod(x+z,5)==0?Blocks.ANDESITE.defaultBlockState():Blocks.STONE.defaultBlockState());block(x,65+h,z,Blocks.MOSS_BLOCK);}}
        for(int z=-87;z<=-36;z++){int x=-66+(int)(2*Math.sin((z+87)/8d));box(x-2,63,z,x+2,64,z,Blocks.STONE.defaultBlockState());for(int dx=-1;dx<=1;dx++)block(x+dx,64,z,Blocks.WATER);box(x-1,65,z,x+1,68,z,Blocks.AIR.defaultBlockState());for(int bank:new int[]{x-2,x+2})block(bank,64,z,Blocks.MOSSY_STONE_BRICKS);}
        box(-67,65,-88,-65,78,-88,Blocks.STONE.defaultBlockState());box(-67,65,-87,-67,78,-87,Blocks.STONE.defaultBlockState());box(-65,65,-87,-65,78,-87,Blocks.STONE.defaultBlockState());box(-66,65,-87,-66,77,-87,Blocks.AIR.defaultBlockState());block(-66,77,-87,Blocks.WATER);
        lane(0,109,0,-71);lane(-48,45,48,45);lane(-48,-38,48,-38);lane(-43,-65,-43,72);lane(43,-65,43,72);
        for(int x=-36;x<=36;x++)for(int z=17;z<=60;z++)block(x,64,z,Math.floorMod(x,9)==0||Math.floorMod(z,9)==0?Blocks.SMOOTH_QUARTZ:Blocks.SMOOTH_STONE);
        hall("合欢山门",0,87,14,6,1,"gate");hall("合欢正殿",0,-2,25,15,2,"palace");hall("凝月后寝",0,-61,18,10,1,"bed");
        hall("议事厅",-29,64,11,8,1,"council");hall("藏经阁",-31,-50,10,11,2,"books");hall("丹房",32,-47,10,9,1,"alchemy");hall("器房",66,-51,11,10,1,"forge");
        hall("听雨弟子院",-28,34,10,10,1,"bed");hall("照月弟子院",29,34,10,10,1,"bed");
        // Branches terminate at the SOUTH door; never cut through a room or its side wall.
        lane(-31,-38,-31,-36);lane(0,-36,66,-36);lane(32,-36,32,-37);lane(66,-36,66,-40);
        lane(-29,73,-29,77);lane(-43,77,0,77);lane(-43,60,0,60);lane(43,64,57,64);lane(-52,8,-43,8);
        // Zigzag bridge with two wide turning platforms and railings on its exterior only.
        bridge(-103,-11,-73,-11);bridge(-73,-11,-73,8);bridge(-73,8,-51,8);for(int[] c:new int[][]{{-73,-11},{-73,8}})for(int dx=-2;dx<=2;dx++)for(int dz=-2;dz<=2;dz++){block(c[0]+dx,65,c[1]+dz,Blocks.SPRUCE_PLANKS);BRIDGE_DECK.add(new BlockPos(c[0]+dx,65,c[1]+dz));}
        for(var pos:BRIDGE_DECK){boolean end=pos.getX()==-103&&Math.abs(pos.getZ()+11)<=1||pos.getX()==-51&&Math.abs(pos.getZ()-8)<=1;boolean edge=Arrays.stream(Direction.Plane.HORIZONTAL.stream().toArray(Direction[]::new)).anyMatch(d->!BRIDGE_DECK.contains(pos.relative(d)));box(pos.getX(),66,pos.getZ(),pos.getX(),68,pos.getZ(),Blocks.AIR.defaultBlockState());if(edge&&!end)block(pos.getX(),66,pos.getZ(),Blocks.DARK_OAK_FENCE);}
        for(int k=-1;k<=1;k++){S.block(-103,65,-11+k,CityPlan.stairs(Blocks.SPRUCE_STAIRS,Direction.EAST,false));S.block(-51,65,8+k,CityPlan.stairs(Blocks.SPRUCE_STAIRS,Direction.WEST,false));}
        lane(-107,-11,-104,-11);
        for(int side:new int[]{-1,1})for(int z=-73;z<=93;z+=12)lamp(side*47,z);
        for(int x=62;x<=100;x+=5)for(int z=-12;z<=48;z+=6){if((x+z)%3==0)continue;int h=4+Math.floorMod(x*7+z,5);for(int y=65;y<=65+h;y++)S.block(x,y,z,Blocks.BAMBOO.defaultBlockState().setValue(BambooStalkBlock.LEAVES,BambooLeaves.LARGE));}
        box(55,64,60,94,64,91,Blocks.SMOOTH_STONE.defaultBlockState());for(int x=58;x<=92;x+=8){block(x,65,87,Blocks.HAY_BLOCK);block(x,66,87,Blocks.TARGET);lamp(x,61);}
        for(int[] t:new int[][]{{-98,60},{-78,75},{-57,82},{58,98},{93,98},{25,75},{-16,74},{86,-76},{-90,-63},{-23,-78}})CityPlan.tree(S,t[0],65,t[1],true);
        for(int x=-105;x<=105;x+=7)for(int z=-72;z<=103;z+=9){if(Math.abs(x)<51||x>52&&z>-20||Math.pow((x+76)/26d,2)+Math.pow((z+4)/40d,2)<1.1||Math.abs(x+66)<6&&z<-34)continue;var base=BLOCKS.getOrDefault(key(x>>4,z>>4),new LinkedHashMap<>()).get(new BlockPos(x,64,z));if(base!=null&&!base.is(Blocks.GRASS_BLOCK)&&!base.is(Blocks.DIRT)&&!base.is(Blocks.MOSS_BLOCK))continue;block(x,65,z,Math.floorMod(x+z,3)==0?Blocks.ALLIUM:Blocks.GRASS);}
        for(int side:new int[]{-1,1}){box(side*108,65,-77,side*108,67,100,WALL);box(side*108,68,-77,side*108,68,100,JADE);}
        CityPlan.sign(S,5,65,103,"合欢宗","凝月照花 · 同道长明","山门向北 · 返回阵向南","DuskRain · 烟雨仙途");
    }
    static void bridge(int ax,int az,int bx,int bz){int n=Math.max(Math.abs(bx-ax),Math.abs(bz-az));for(int i=0;i<=n;i++)for(int k=-2;k<=2;k++){int x=ax+Integer.signum(bx-ax)*i+(az!=bz?k:0),z=az+Integer.signum(bz-az)*i+(ax!=bx?k:0);block(x,65,z,Blocks.SPRUCE_PLANKS);BRIDGE_DECK.add(new BlockPos(x,65,z));}}
    static void roof(int x,int y,int z,int w,int d,boolean lower){
        for(int dx=-w;dx<=w;dx++)for(int dz=-d;dz<=d;dz++){int edge=Math.min(w-Math.abs(dx),d-Math.abs(dz));if(lower&&edge>3)continue;int slope=Math.min(d-Math.abs(dz),edge<4?edge:100);int yy=y+Math.max(0,slope-1)/2;if(Math.abs(dx)>w-3&&Math.abs(dz)>d-3)yy++;
            S.block(x+dx,yy-1,z+dz,JADE);S.block(x+dx,yy,z+dz,CityPlan.stairs(Blocks.DARK_PRISMARINE_STAIRS,edge==w-Math.abs(dx)&&edge<4?(dx<0?Direction.EAST:Direction.WEST):(dz<0?Direction.SOUTH:Direction.NORTH),false));
            if(edge==0&&Math.floorMod(dx+dz,2)==0)S.block(x+dx,yy-1,z+dz,Decor.WADANG.get().defaultBlockState());
            if(edge<4&&w-Math.abs(dx)==d-Math.abs(dz))S.block(x+dx,yy+1,z+dz,Decor.RIDGE_TILE.get().defaultBlockState());
        }
        if(!lower){int ry=y+(d-1)/2+1;box(x-w+4,ry,z,x+w-4,ry,z,Decor.RIDGE_TILE.get().defaultBlockState());for(int side:new int[]{-1,1}){S.block(x+side*(w-4),ry+1,z,Decor.RIDGE.get().defaultBlockState());for(int dz=-d+4;dz<=d-4;dz++){int h=(d-Math.abs(dz)-2)/2;box(x+side*(w-4),y+1,z+dz,x+side*(w-4),y+h,z+dz,Math.floorMod(dz,3)==0?TIMBER:WALL);S.block(x+side*(w-4),y+h+1,z+dz,JADE);}}}
    }
    static void hall(String name,int x,int z,int w,int d,int floors,String use){
        box(x-w-2,64,z-d-2,x+w+2,64,z+d+2,WHITE);box(x-w,65,z-d,x+w,64+floors*9,z+d,Blocks.AIR.defaultBlockState());
        for(int f=0;f<floors;f++){int fy=64+f*9;box(x-w,fy,z-d,x+w,fy,z+d,Blocks.SPRUCE_PLANKS.defaultBlockState());
            for(int yy=1;yy<=7;yy++)for(int i=-w;i<=w;i++)for(int side:new int[]{-1,1}){if(side>0&&Math.abs(i)<=2&&yy<=4)continue;S.block(x+i,fy+yy,z+side*d,yy==1?WHITE:yy==7?TIMBER:Math.floorMod(i,4)==0?TIMBER:yy>=3&&yy<=5?Decor.LATTICE.get().defaultBlockState():WALL);}
            for(int yy=1;yy<=7;yy++)for(int j=-d;j<=d;j++)for(int side:new int[]{-1,1})S.block(x+side*w,fy+yy,z+j,yy==1?WHITE:Math.floorMod(j,4)==0?TIMBER:yy>=3&&yy<=5?Decor.LATTICE.get().defaultBlockState():WALL);
            for(int i=-w;i<=w;i+=5)for(int side:new int[]{-1,1}){if(side>0&&Math.abs(i)<=2)continue;box(x+i,fy+1,z+side*d,x+i,fy+7,z+side*d,TIMBER);CityPlan.bracket(S,x+i,fy+6,z+side*d);block(x+i,fy+5,z+side*(d+1),Blocks.LANTERN);}
            for(int dx=-w+3;dx<w;dx+=6)for(int dz=-d+3;dz<d;dz+=6){block(x+dx,fy,z+dz,Blocks.SEA_LANTERN);block(x+dx,fy+1,z+dz,Blocks.LIGHT_GRAY_CARPET);block(x+dx,fy+6,z+dz,Blocks.LANTERN);block(x+dx,fy+7,z+dz,Blocks.CHAIN);}
            if(use.equals("books")){CityPlan.bookcase(S,x-w+2,fy+1,z-d+2,w*2-4);CityPlan.bookcase(S,x-w+2,fy+1,z,Math.max(3,w-5));CityPlan.table(S,x+2,fy+1,z+3,4);}
            else if(use.equals("bed")){for(int xx:new int[]{-w+3,w-3}){CityPlan.bedroom(S,x+xx,fy+1,z-d+3,1);CityPlan.bookcase(S,x+xx-1,fy+1,z+1,3);}CityPlan.table(S,x+3,fy+1,z+4,3);}
            else if(use.equals("alchemy")){for(int j=-d+2;j<d-2;j+=3){block(x+w-2,fy+1,z+j,Blocks.BARREL);block(x+w-2,fy+2,z+j,Blocks.BREWING_STAND);}CityPlan.bookcase(S,x-w+2,fy+1,z-d+2,5);CityPlan.table(S,x+2,fy+1,z,3);}
            else if(use.equals("forge")){for(int j=-d+2;j<0;j+=3){block(x+w-2,fy+1,z+j,Blocks.BLAST_FURNACE);block(x+w-3,fy+1,z+j,Blocks.ANVIL);}block(x-w+3,fy+1,z-d+3,Blocks.SMITHING_TABLE);CityPlan.table(S,x+2,fy+1,z+2,3);}
            else if(!use.equals("gate")){for(int side:new int[]{-1,1})for(int j=-d+4;j<d-3;j+=5)CityPlan.table(S,x+side*(w-7),fy+1,z+j,4);box(x-4,fy+1,z-d+2,x+4,fy+4,z-d+2,Blocks.CYAN_TERRACOTTA.defaultBlockState());block(x,fy+1,z-d+4,Blocks.LECTERN);}
            roof(x,fy+8,z,w+3,d+3,f<floors-1);
            if(!Architecture.LEGACY.get())for(int ix=-w;ix<=w;ix++)for(int iz=-d;iz<=d;iz++)if(Math.abs(ix)==w||Math.abs(iz)==d)S.block(x+ix,fy+8,z+iz,TIMBER);
        }
        if(floors>1){int sx=x-w+3,sz=z-d+3;for(int j=0;j<9;j++){box(sx,66+j,sz+j,sx+2,69+j,sz+j,Blocks.AIR.defaultBlockState());for(int i=0;i<3;i++)S.block(sx+i,65+j,sz+j,CityPlan.stairs(Blocks.SPRUCE_STAIRS,Direction.SOUTH,false));}box(sx,73,sz+9,sx+2,73,sz+11,Blocks.SPRUCE_PLANKS.defaultBlockState());box(sx,74,sz+9,sx+2,77,sz+11,Blocks.AIR.defaultBlockState());}
        if(use.equals("gate")||use.equals("palace"))box(x-2,65,z-d,x+2,68,z-d,Blocks.AIR.defaultBlockState());
        CityPlan.sign(S,x+5,65,z+d+1,name,"合欢宗 · 曲水照月","入内参观 · 保持通行","");
    }
    public static void begin(ServerPlayer p){var data=GuildData.get(p.server);var g=data.guilds.get(1);if(g==null||!g.leader.equals(UUID.fromString("6e93fc7d-8abf-3d21-a965-18b49e686b0b"))||g.archived){Gameplay.say(p,"宗门编号或宗主不符合本次迁移目标，未改动。");return;}if(g.palace!=0){Gameplay.say(p,"府邸已登记施工或完成，不重复覆盖。");return;}g.name="合欢宗";g.palace=1;g.palaceCursor=0;data.setDirty();Gameplay.say(p,"合欢宗府邸开始分区施工，完成前暂时关闭洞天入口。");}
    public static void tick(MinecraftServer server){var level=server.getLevel(Guilds.DIM);if(level==null)return;var data=GuildData.get(server);for(var g:data.guilds.values()){if(g.palace!=1||g.palaceCursor>=256||!g.ready)continue;plan();int i=g.palaceCursor,cx=-8+i%16,cz=-8+i/16;BlockPos marker=new BlockPos(g.x()+cx*16+8,60,g.z()+cz*16+8);level.getChunkAt(marker);var be=level.getBlockEntity(marker);if(be==null)return;
            if(be.getPersistentData().getInt("DuskRainPalace")!=1){for(var e:BLOCKS.getOrDefault(key(cx,cz),new LinkedHashMap<>()).entrySet())level.setBlock(e.getKey().offset(g.x(),0,g.z()),e.getValue(),2);for(var original:TAGS.getOrDefault(key(cx,cz),List.of())){var n=original.copy();n.putInt("x",n.getInt("x")+g.x());n.putInt("z",n.getInt("z")+g.z());var target=level.getBlockEntity(new BlockPos(n.getInt("x"),n.getInt("y"),n.getInt("z")));if(target!=null){target.load(n);target.setChanged();}}be.getPersistentData().putInt("DuskRainPalace",1);be.setChanged();}
            g.palaceCursor++;if(g.palaceCursor==256){g.palace=2;for(UUID id:g.members){var p=server.getPlayerList().getPlayer(id);if(p!=null)Gameplay.say(p,"合欢宗府邸施工完成，/dr guild enter 入府。成员此后装修不会被自动重刷。");}}data.setDirty();return;}}
}
