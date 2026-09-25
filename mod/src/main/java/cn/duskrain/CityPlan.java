package cn.duskrain;

import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.*;
import java.util.*;

/** Authored buildings placed on an immutable natural landscape; dimensions are half widths. */
public final class CityPlan {
    public record Building(String id,String name,int x,int z,int w,int d,int floors,int variant,int floorY){
        public int y(){return floorY;}
        public boolean home(){return id.startsWith("home_");}
        public int inset(){return 2;}
        public int width(int f){return w-f*2;}
        public int depth(int f){return d-f*2;}
    }
    public interface Sink {void block(int x,int y,int z,BlockState state);default void entity(CompoundTag tag){}default void stairway(int x,int y,int z,int width,int steps){}default void room(String id,int x,int y,int z,int w,int d,int floors){}default void fixture(int x,int y,int z){}}
    public static final List<Building> BUILDINGS=new ArrayList<>();
    public static final int FLOOR=8,SPAWN_Z=28;
    public static final int SPAWN_Y=Terrain.ground(0,SPAWN_Z)+2;
    public static final int[][] ISLETS=Terrain.ISLANDS;
    public static final BlockState WHITE=Blocks.SMOOTH_QUARTZ.defaultBlockState(),WALL=Blocks.CALCITE.defaultBlockState(),WOOD=Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState(),ROOF=Blocks.DEEPSLATE_TILES.defaultBlockState(),TILE=Blocks.DARK_PRISMARINE.defaultBlockState(),GOLD=Blocks.CHISELED_SANDSTONE.defaultBlockState();
    static final BlockState PLANK=Blocks.SPRUCE_PLANKS.defaultBlockState(),DARK=Blocks.DARK_OAK_PLANKS.defaultBlockState(),AIR=Blocks.AIR.defaultBlockState();
    public record Route(int ax,int az,int ay,int bx,int bz,int by,boolean bridge){}
    public static final List<Route> ROUTES=new ArrayList<>();
    static boolean ROUTES_READY;
    static {
        add("palace","烟雨仙宫",0,-192,39,23,2,0,161);
        add("study","问道书院",-34,-12,20,16,1,1,0);
        add("quests","听雨任务殿",34,-12,17,13,1,2,0);
        add("alchemy","溪云丹坊",-79,29,15,13,2,3,0);
        add("forge","玄岩器阁",79,13,17,14,1,4,0);
        add("market","听潮商街",-33,61,18,13,1,5,0);
        add("inn","枕雨客栈",38,65,17,14,2,6,0);
        add("library","松风藏经楼",-61,-67,15,13,3,7,0);
        add("trial","问劫云台",168,-118,20,16,1,8,138);
        add("warp","云渡传送阁",0,46,8,7,1,9,0);
        add("enchanter","铭灵轩",79,-23,10,9,1,21,0);
        add("arena","演武庭",84,69,18,16,1,10,0);
        add("spawn","迎仙山门",0,108,15,6,1,11,0);
        int[][] homes={{-147,64},{-169,26},{-149,-18},{-123,-54},{-118,104},{-70,115},{-40,136},{39,133},{77,121},{125,101},{135,57},{143,12},{125,-31},{99,-67},{39,-70},{-17,-80},{-199,96},{184,80}};
        String[] names={"听雨居","观澜舍","松风院","栖云斋","望月轩","青竹庐"};
        for(int i=0;i<homes.length;i++)add("home_"+(i+1),names[i%6]+"·"+(i/6+1),homes[i][0],homes[i][1],12,i%6==2?12:10,i%6==1||i%6==4?2:1,12+i%6,0);
        add("guildhall","云岑宗务堂",68,219,16,13,2,18,0);
        add("guildgate_west","接引云阁",45,256,8,7,1,19,0);
        add("guildgate_east","归宗星阁",91,256,8,7,1,20,0);
        // Public spine follows the island's grade; building entries join it by their own ramps.
        route(0,119,find("spawn").y(),0,98,find("spawn").y(),false);
        route(0,119,find("spawn").y(),0,160,Terrain.ground(0,160)+1,false);
        route(0,98,find("spawn").y(),0,55,find("warp").y(),false);
        route(0,55,find("warp").y(),14,55,Terrain.ground(14,55)+1,false);
        route(0,47,find("warp").y(),0,55,find("warp").y(),false);
        route(14,55,Terrain.ground(14,55)+1,14,28,SPAWN_Y-1,false);
        route(14,28,SPAWN_Y-1,0,28,SPAWN_Y-1,false);
        route(0,28,SPAWN_Y-1,0,-88,Terrain.ground(0,-88)+1,false);
        for(int side:new int[]{-1,1}){
            for(int z=-80;z<145;z+=15)route((side<0?-109:115),z,Terrain.ground((side<0?-109:115),z)+1,(side<0?-109:115),z+15,Terrain.ground((side<0?-109:115),z+15)+1,false);
            route(0,160,Terrain.ground(0,160)+1,(side<0?-109:115),160,Terrain.ground((side<0?-109:115),160)+1,false);
        }
        for(Building b:BUILDINGS){if(b.variant==21||b.variant==0||b.variant==8||b.variant==9||b.variant==11||b.variant>=18&&b.variant<=20)continue;
            int ez=b.z+b.d+5,ey=b.y(),hubX=Math.abs(b.x)>90?(b.x<0?-109:115):0;
            route(b.x,b.z+b.d+1,ey,b.x,ez,ey,false);
            int hy=hubHeight(hubX,ez);
            if(Math.abs(hubX-b.x)<Math.abs(ey-hy)*2+8&&Math.abs(ey-hy)>2){
                int bend=ez+Math.max(16,Math.abs(ey-hy)*2),end=hubHeight(hubX,bend),mid=end+Integer.signum(ey-end)*2;
                route(b.x,ez,ey,b.x,bend,mid,false);route(b.x,bend,mid,hubX,bend,end,false);
            }else route(b.x,ez,ey,hubX,ez,hy,false);
        }
        route(0,-88,Terrain.ground(0,-88)+1,0,-112,96,true);
        route(0,-112,96,-69,-112,117,true);route(-69,-112,117,-69,-161,147,true);
        route(-69,-161,147,0,-161,161,true);route(0,-161,161,0,-168,161,true);
        route(84,-42,Terrain.ground(84,-42)+1,0,-42,hubHeight(0,-42),false);
        route(84,-42,Terrain.ground(84,-42)+1,127,-42,112,true);route(127,-42,112,127,-93,134,true);
        route(127,-93,134,168,-93,138,true);route(168,-93,138,168,-100,138,true);
        route(-109,-80,Terrain.ground(-109,-80)+1,-149,-80,109,true);route(-149,-80,109,-149,-118,126,true);
        route(-149,-118,126,-177,-118,128,true);
        route(-177,-118,128,-177,-130,128,false);
        var tide=Landscape.LANDMARKS.get(1);var bell=Landscape.LANDMARKS.get(2);
        route(115,145,Terrain.ground(115,145)+1,115,199,Terrain.ground(115,199)+1,false);
        route(115,199,Terrain.ground(115,199)+1,196,199,tide.y(),false);
        route(196,199,tide.y(),196,193,tide.y(),false);
        route(-109,145,Terrain.ground(-109,145)+1,-109,190,Terrain.ground(-109,190)+1,false);
        route(-109,190,Terrain.ground(-109,190)+1,-222,190,bell.y(),false);
        route(-222,190,bell.y(),-222,178,bell.y(),false);
        PlayerMarket.routes();
        route(0,266,Terrain.ground(0,266)+1,0,280,Terrain.ground(0,280)+1,false);
        route(0,280,Terrain.ground(0,280)+1,115,280,Terrain.ground(115,280)+1,false);
        route(115,199,Terrain.ground(115,199)+1,115,280,Terrain.ground(115,280)+1,false);
        for(String id:List.of("guildhall","guildgate_west","guildgate_east")){var b=find(id);route(b.x,b.z+b.d+1,b.y(),b.x,280,hubHeight(0,280),false);}
        var en=find("enchanter");var fg=find("forge");route(en.x,en.z+en.d+1,en.y(),en.x,-9,en.y(),false);route(en.x,-9,en.y(),103,-9,en.y(),false);route(103,-9,en.y(),103,32,fg.y(),false);route(103,32,fg.y(),79,32,fg.y(),false);
        ROUTES_READY=true;
    }
    static void add(String id,String n,int x,int z,int w,int d,int f,int v,int y){BUILDINGS.add(new Building(id,n,x,z,w,d,f,v,y==0?Math.max(Terrain.ground(x,z)+2,Terrain.water(x,z)+2):y));}
    static int counterX(Building b){return b.id.equals("enchanter")?0:b.id.equals("quests")?6:5;}
    static int hubHeight(int x,int z){if(x==0&&z>=15&&z<=34)return SPAWN_Y-1;for(Route r:ROUTES)if(!r.bridge&&r.ax==x&&r.bx==x&&z>=Math.min(r.az,r.bz)&&z<=Math.max(r.az,r.bz))return pathY(r,Math.abs(z-r.az));return Terrain.ground(x,z)+1;}
    static void route(int x,int z,int y,int bx,int bz,int by,boolean bridge){ROUTES.add(new Route(x,z,y,bx,bz,by,bridge));}
    public static Building find(String id){return BUILDINGS.stream().filter(b->b.id.equals(id)).findFirst().orElse(BUILDINGS.get(0));}
    public static int terrace(int z){return Terrain.ground(0,z);}
    public static int height(int x,int z){return Terrain.ground(x,z);}
    public static int isletHeight(int x,int z){return Terrain.skyTop(x,z);}
    public static int islandBottom(int x,int z){return Terrain.skyBottom(x,z);}
    public static double islandRadius(int x,int z){return Math.hypot(x/345.0,(z+45)/345.0);}
    static boolean nearPlot(int x,int z,int margin){for(Building b:BUILDINGS)if(Math.abs(x-b.x)<=b.w+margin&&Math.abs(z-b.z)<=b.d+margin)return true;for(var s:PlayerMarket.SITES)if(Math.abs(x-s.x())<=7+margin&&Math.abs(z-s.z())<=7+margin)return true;return false;}
    public static boolean road(int x,int z){for(Route r:ROUTES){if(r.ax==r.bx&&Math.abs(x-r.ax)<=3&&z>=Math.min(r.az,r.bz)-3&&z<=Math.max(r.az,r.bz)+3)return true;if(r.az==r.bz&&Math.abs(z-r.az)<=3&&x>=Math.min(r.ax,r.bx)-3&&x<=Math.max(r.ax,r.bx)+3)return true;}return false;}
    public static void paint(Sink s,int cx,int cz,int phase){
        if(!Architecture.LEGACY.get())s=Architecture.wrap(s);
        int x0=cx*16,z0=cz*16;
        if(phase==0)SiteTerrain.paint(s,cx,cz);
        for(Building b:BUILDINGS)if(b.x+b.w+14>=x0&&b.x-b.w-14<x0+16&&b.z+b.d+14>=z0&&b.z-b.d-14<z0+16)building(s,b,phase);
        PlayerMarket.paint(s,cx,cz,phase);
        if(phase==4){
            for(Route r:ROUTES)path(s,cx,cz,r);
            SkyBridges.junctions(s,cx,cz);
            if(cx>=-1&&cx<=1&&cz>=0&&cz<=2){
                int y=SPAWN_Y-1;
                for(int x=-10;x<=10;x++)for(int z=15;z<=32;z++){if(Math.abs(x)>7&&z<23)continue;s.block(x,y,z,Math.floorMod(x+z,8)==0?Blocks.CHISELED_STONE_BRICKS.defaultBlockState():WHITE);for(int yy=1;yy<4;yy++)s.block(x,y+yy,z,AIR);}
                sign(s,-7,y+1,29,"DuskRain · 烟雨仙途","问道 ←  前方  → 任务","商街 ←  后方  → 客栈","群号 205255670");
                lamp(s,-10,y+1,24);lamp(s,10,y+1,24);
            }
        }
        Landscape.paint(s,cx,cz,phase);
        if(phase==5){SkyDetails.paint(s,cx,cz);for(Route r:ROUTES)path(s,cx,cz,r);SkyBridges.junctions(s,cx,cz);if(!Architecture.LEGACY.get()){WaterCourt.paint(s,cx,cz);ArchitectureFinishes.paint(s,cx,cz);CourtGarden.paint(s,cx,cz);}}
    }

    public static void building(Sink s,Building b,int phase){
        int x=b.x,z=b.z,y=b.y(),w=b.w,d=b.d;
        switch(b.variant){
            case 0 -> {hall(s,b,x,z-5,20,12,2,0,false,phase);hall(s,b,x-30,z,8,12,1,1,false,phase);hall(s,b,x+30,z,8,12,1,1,false,phase);}
            case 1 -> {hall(s,b,x,z-6,12,8,1,1,false,phase);hall(s,b,x-15,z+3,4,10,1,1,false,phase);hall(s,b,x+15,z+3,4,10,1,1,false,phase);court(s,b,phase,9,13);}
            case 2 -> {hall(s,b,x,z-3,14,9,1,0,false,phase);veranda(s,x-15,z+9,x+15,z+12,y,phase);}
            case 3 -> {hall(s,b,x+2,z-2,10,10,2,1,false,phase);veranda(s,x-13,z-9,x-9,z+12,y,phase);if(phase==3){for(int a=0;a<6;a++){s.block(x-11,y+1,z-7+a*3,Blocks.COMPOSTER.defaultBlockState());s.block(x-11,y+2,z-7+a*3,Blocks.POTTED_FERN.defaultBlockState());}s.block(x+8,y+1,z-5,Blocks.CAULDRON.defaultBlockState());}}
            case 4 -> {hall(s,b,x,z-4,14,9,1,1,false,phase);veranda(s,x-15,z+7,x+15,z+12,y,phase);if(phase==3){for(int a=-8;a<=8;a+=8){box(s,x+a-1,y+1,z-9,x+a+1,y+3,z-7,Blocks.DEEPSLATE_BRICKS.defaultBlockState());s.block(x+a,y+1,z-6,Blocks.BLAST_FURNACE.defaultBlockState().setValue(FurnaceBlock.FACING,Direction.SOUTH).setValue(FurnaceBlock.LIT,true));box(s,x+a,y+4,z-8,x+a,y+14,z-8,Blocks.POLISHED_BASALT.defaultBlockState());}s.block(x+9,y+1,z+3,Blocks.SMITHING_TABLE.defaultBlockState());s.block(x+5,y+1,z+3,Blocks.ANVIL.defaultBlockState());}}
            case 5 -> {hall(s,b,x-11,z-2,5,10,1,1,true,phase);hall(s,b,x+11,z-2,5,10,1,1,true,phase);if(phase==3)for(int side:new int[]{-1,1})for(int dz:new int[]{-7,1,8})stall(s,x+side*10,y+1,z+dz,side);}
            case 6 -> {hall(s,b,x,z-4,14,9,2,1,false,phase);hall(s,b,x+12,z+8,4,5,1,1,true,phase);if(phase==3){table(s,x+3,y+1,z+5,3);kitchen(s,x+12,y+1,z-9);}}
            case 7 -> {hall(s,b,x,z-2,11,10,3,0,false,phase);if(phase==3)for(int f=0;f<3;f++){int iy=y+FLOOR*f;bookcase(s,x+3,iy+1,z-8,5);bookcase(s,x+3,iy+1,z-1,5);table(s,x+3,iy+1,z+3,3);}}
            case 8 -> {hall(s,b,x,z-4,14,11,1,2,true,phase);if(phase==3){ring(s,x,y+1,z-4,7,Blocks.AMETHYST_BLOCK.defaultBlockState());for(int side:new int[]{-1,1}){column(s,x+side*17,y,z-4,10);s.block(x+side*17,y+11,z-4,Blocks.END_ROD.defaultBlockState());}}}
            case 9 -> {hall(s,b,x,z,6,5,1,2,true,phase);if(phase==3){ring(s,x,y+1,z,3,Blocks.SEA_LANTERN.defaultBlockState());s.block(x,y+1,z,Blocks.LODESTONE.defaultBlockState());}}
            case 10 -> {if(phase==0)platform(s,x,y,z,w,d);if(phase==3){ring(s,x,y+1,z,11,Blocks.POLISHED_ANDESITE.defaultBlockState());for(int dx:new int[]{-14,14})for(int zz=-11;zz<11;zz+=3)s.block(x+dx,y+1,z+zz,stairs(Blocks.QUARTZ_STAIRS,dx<0?Direction.WEST:Direction.EAST,false));}if(phase==2){hall(s,b,x,z-12,11,3,1,1,true,phase);}}
            case 11 -> {hall(s,b,x-10,z,4,4,1,0,true,phase);hall(s,b,x+10,z,4,4,1,0,true,phase);if(phase==1){beamX(s,x-8,x+8,y+6,z);beamX(s,x-8,x+8,y+5,z);}if(phase==2)roof(s,x,y+7,z,10,4,-1,-1,1);}
            case 21 -> {hall(s,b,x,z-2,8,7,1,1,false,phase);if(phase==3){bookcase(s,x-6,y+1,z-7,4);bookcase(s,x+2,y+1,z-7,5);s.block(x,y+1,z-5,Blocks.ENCHANTING_TABLE.defaultBlockState());s.block(x-5,y+1,z+1,Blocks.ANVIL.defaultBlockState());s.block(x-3,y+1,z+1,Blocks.GRINDSTONE.defaultBlockState());table(s,x+3,y+1,z+1,2);}}
            case 18 -> {hall(s,b,x,z-3,12,9,2,0,false,phase);veranda(s,x-14,z+8,x+14,z+11,y,phase);if(phase==3){bookcase(s,x+4,y+1,z-9,6);table(s,x+5,y+1,z+1,3);bookcase(s,x+3,y+9,z-8,5);table(s,x+4,y+9,z+1,3);}}
            case 19,20 -> {hall(s,b,x,z,6,5,1,b.variant==19?1:2,true,phase);if(phase==3){ring(s,x,y,z-1,3,Blocks.SEA_LANTERN.defaultBlockState());s.block(x,y+1,z-1,Blocks.LODESTONE.defaultBlockState());sign(s,x+3,y+1,z+3,b.name,"右键传送阵 · 帮派洞天","DuskRain · 云岑宗务堂","群号 205255670");}}
            default -> {
                int type=(b.variant-12)%6;
                switch(type){
                    case 0 -> {hall(s,b,x,z-3,10,6,1,1,false,phase);veranda(s,x-10,z+4,x+10,z+8,y,phase);}
                    case 1 -> hall(s,b,x,z-1,9,8,2,0,false,phase);
                    case 2 -> {hall(s,b,x,z-6,10,5,1,1,false,phase);hall(s,b,x-8,z+4,3,6,1,1,false,phase);court(s,b,phase,5,9);}
                    case 3 -> {hall(s,b,x+2,z-2,7,7,1,1,false,phase);veranda(s,x-10,z-6,x-5,z+8,y,phase);}
                    case 4 -> {hall(s,b,x-2,z-2,8,7,2,1,false,phase);hall(s,b,x+8,z+4,3,4,1,2,true,phase);}
                    case 5 -> {hall(s,b,x,z-2,8,7,1,0,false,phase);if(phase==5)for(int dx:new int[]{-10,10})for(int dz=-6;dz<8;dz+=3){s.block(x+dx,y,z+dz,Blocks.MOSS_BLOCK.defaultBlockState());for(int h=1;h<6;h++)s.block(x+dx,y+h,z+dz,Blocks.BAMBOO.defaultBlockState().setValue(BambooStalkBlock.STAGE,1));}}
                }
            }
        }
        if(phase==0){
            // Entry apron and a continuous central path, only within this building's plot.
            if(!b.home())platform(s,x+5,y,z+d-4,3,3);
            if(b.id.equals("alchemy")||b.id.equals("market"))platform(s,x-5,y,z+d-4,3,3);
            for(int ix=-2;ix<=2;ix++)for(int iz=0;iz<=d+4;iz++){SiteTerrain.foundation(s,x+ix,y,z+iz);s.block(x+ix,y,z+iz,WHITE);for(int h=1;h<=4;h++)s.block(x+ix,y+h,z+iz,AIR);}
        }
        if(phase==3){
            sign(s,x+4,y+1,z+d+1,b.name,"入内参观 · 楼梯通往上层","DuskRain · 烟雨仙途","群号 205255670");
            if(!b.home()){s.block(x+counterX(b),y+1,z+d-3,Blocks.BARREL.defaultBlockState());s.block(x+counterX(b)+1,y+1,z+d-3,Blocks.BARREL.defaultBlockState());s.block(x+counterX(b)+1,y+2,z+d-3,Blocks.LANTERN.defaultBlockState());}
            if(b.id.equals("alchemy")||b.id.equals("market")){for(int dx=-7;dx<=-4;dx++)for(int dz=d-5;dz<=d;dz++)for(int h=1;h<=3;h++)s.block(x+dx,y+h,z+dz,AIR);s.block(x-5,y+1,z+d-3,Blocks.BARREL.defaultBlockState());s.block(x-4,y+1,z+d-3,Blocks.BARREL.defaultBlockState());s.block(x-4,y+2,z+d-3,Blocks.LANTERN.defaultBlockState());}
        }
        if(phase==4){lamp(s,x-w,y+1,z+d);lamp(s,x+w,y+1,z+d);}
        if(phase==5){if(!road(x-w+2,z+d-1))s.block(x-w+2,y+1,z+d-1,Blocks.POTTED_AZALEA.defaultBlockState());if(!road(x+w-2,z+d-1))s.block(x+w-2,y+1,z+d-1,Blocks.POTTED_FERN.defaultBlockState());}
    }
    static void platform(Sink s,int x,int y,int z,int w,int d){
        for(int dx=-w;dx<=w;dx++)for(int dz=-d;dz<=d;dz++){
            int natural=Math.max(Terrain.ground(x+dx,z+dz),Terrain.skyTop(x+dx,z+dz));
            SiteTerrain.foundation(s,x+dx,y,z+dz);
            s.block(x+dx,y,z+dz,Math.abs(dx)==w||Math.abs(dz)==d?WHITE:PLANK);
        }
    }
    static void hall(Sink s,Building owner,int x,int z,int w,int d,int floors,int roofType,boolean open,int phase){
        int y=owner.y();
        if(phase==0)s.room(owner.id(),x,y,z,w,d,floors);
        if(phase==0)for(int f=0;f<floors-1;f++)s.stairway(x-w+3,y+f*FLOOR,z-d+3,3,FLOOR);
        if(phase==0){platform(s,x,y,z,w+1,d+2);box(s,x-w,y+1,z-d,x+w,y+floors*FLOOR+5,z+d,AIR);}
        for(int f=0;f<floors;f++){
            int fy=y+f*FLOOR,fw=w-f,fd=d-f;
            if(phase==1){
                if(f>0)box(s,x-fw,fy,z-fd,x+fw,fy,z+fd,PLANK);
                if(!open)for(int yy=1;yy<=6;yy++){
                    for(int ix=-fw;ix<=fw;ix++)for(int side:new int[]{-1,1}){
                        if(f==0&&side>0&&Math.abs(ix)<=2&&yy<=4)continue;
                        s.block(x+ix,fy+yy,z+side*fd,window(ix+fw,yy));
                    }
                    for(int iz=-fd;iz<=fd;iz++)for(int side:new int[]{-1,1})s.block(x+side*fw,fy+yy,z+iz,window(iz+fd,yy));
                }
                for(int ix=-fw;ix<=fw;ix+=4)for(int side:new int[]{-1,1})if(!(side>0&&Math.abs(ix)<=2)){column(s,x+ix,fy,z+side*fd,6);bracket(s,x+ix,fy+5,z+side*fd);}
                for(int side:new int[]{-1,1}){column(s,x+side*fw,fy,z+fd,6);beamZ(s,z-fd,z+fd,fy+6,x+side*fw);beamX(s,x-fw,x+fw,fy+6,z+side*fd);}
                if(!open)box(s,x-fw+1,fy+7,z-fd+1,x+fw-1,fy+7,z+fd-1,DARK);
                for(int ix=-fw+2;ix<=fw-2;ix+=4)s.block(x+ix,fy+5,z+fd+1,Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING,true));
            }
            if(phase==2){roof(s,x,fy+7,z,fw+3,fd+3,f<floors-1?fw: -1,f<floors-1?fd:-1,roofType);
                if(!Architecture.LEGACY.get()&&!open){
                    // Enclose only wall lines; room ceilings and staircase cutouts stay separate.
                    for(int ix=-fw;ix<=fw;ix++)for(int iz=-fd;iz<=fd;iz++)if(Math.abs(ix)==fw||Math.abs(iz)==fd){
                        int dep=roofType==1?fd+3-Math.abs(iz):Math.min(fw+3-Math.abs(ix),fd+3-Math.abs(iz));
                        int top=fy+6+Math.max(0,(dep-1)/2);
                        if(f<floors-1)top=fy+7;
                        for(int yy=fy+7;yy<=top;yy++)s.block(x+ix,yy,z+iz,yy==fy+7?DARK:WALL);
                    }
                }
            }
            if(phase==3){
                for(int lx=-fw+1;lx<=fw-1;lx+=4)for(int lz=-fd+1;lz<=fd-1;lz+=4){
                    s.block(x+lx,fy,z+lz,Blocks.SEA_LANTERN.defaultBlockState());
                    s.block(x+lx,fy+1,z+lz,Blocks.LIGHT_GRAY_CARPET.defaultBlockState());
                }
                for(int lx:lightAxis(fw))for(int lz:lightAxis(fd)){
                    if(floors>1&&lx>=-w+2&&lx<=-w+6&&lz>=-d+2&&lz<=-d+FLOOR+5)continue;
                    s.block(x+lx,fy+3,z+lz,Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING,true));box(s,x+lx,fy+4,z+lz,x+lx,fy+6,z+lz,Blocks.CHAIN.defaultBlockState());
                }
            }
            if(phase==3&&!open){
                if(owner.home()||owner.id.equals("inn")){if(f==0){table(s,x+2,fy+1,z+1,3);kitchen(s,x+fw-1,fy+1,z-fd+2);}else bedroom(s,x+fw-2,fy+1,z-fd+3,owner.variant%3);}
                else if(owner.id.equals("study")){bookcase(s,x+2,fy+1,z-fd+1,Math.max(1,fw-3));s.block(x,fy+1,z-fd+3,Blocks.LECTERN.defaultBlockState().setValue(LecternBlock.FACING,Direction.SOUTH));table(s,x+2,fy+1,z+1,3);}
                else if(owner.id.equals("quests")){bookcase(s,x+5,fy+1,z-fd+1,5);for(int ix=-8;ix<=8;ix+=8){s.block(x+ix,fy+1,z+1,Blocks.LECTERN.defaultBlockState().setValue(LecternBlock.FACING,Direction.SOUTH));wallSign(s,x+ix,fy+3,z-fd+1,"烟雨六章","拜师 · 探索 · 炼制","问心 · 证道 · 渡劫","按 G 查看仙途录");}}
                else if(owner.id.equals("alchemy")){for(int iz=-fd+2;iz<fd-3;iz+=4){box(s,x+fw-3,fy+1,z+iz,x+fw-1,fy+1,z+iz,Blocks.POLISHED_ANDESITE.defaultBlockState());s.block(x+fw-2,fy+2,z+iz,Blocks.BREWING_STAND.defaultBlockState());}bookcase(s,x+2,fy+1,z-fd+1,4);}
                else if(owner.id.equals("palace")){box(s,x-4,fy+1,z-fd+2,x+4,fy+1,z-fd+5,WHITE);s.block(x,fy+2,z-fd+3,stairs(Blocks.QUARTZ_STAIRS,Direction.NORTH,false));box(s,x-4,fy+2,z-fd+1,x+4,fy+5,z-fd+1,Blocks.CYAN_TERRACOTTA.defaultBlockState());for(int side:new int[]{-1,1})table(s,x+side*8,fy+1,z,3);}
                s.block(x+3,fy+5,z,Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING,true));
            }
            if(phase==3)Interiors.paint(s,owner,x,fy,z,fw,fd,f,floors,open);
        }
        // Apply after both roofs and furniture so stair openings remain unobstructed.
        if(floors>1&&(phase==1||phase==2||phase==3)){
            int sx=x-w+3,sz=z-d+3;
            for(int f=0;f<floors-1;f++){int fy=y+f*FLOOR;
                for(int j=0;j<FLOOR;j++){box(s,sx,fy+j+2,sz+j,sx+2,fy+j+4,sz+j,AIR);for(int k=0;k<3;k++)s.block(sx+k,fy+j+1,sz+j,stairs(Blocks.SPRUCE_STAIRS,Direction.SOUTH,false));}
                box(s,sx,fy+FLOOR,sz+FLOOR,sx+2,fy+FLOOR,sz+FLOOR+2,PLANK);box(s,sx,fy+FLOOR+1,sz+FLOOR,sx+2,fy+FLOOR+3,sz+FLOOR+2,AIR);
                for(int j=0;j<FLOOR;j+=2)s.block(sx-1,fy+j+2,sz+j,Blocks.SHROOMLIGHT.defaultBlockState());
                s.block(sx+4,fy+3,sz+10,Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING,true));
                box(s,sx+4,fy+4,sz+10,sx+4,fy+6,sz+10,Blocks.CHAIN.defaultBlockState());
                sign(s,sx+3,fy+1,sz+2,"楼上参观 ↑","扶梯通行","请勿拥挤","");
            }
        }
    }
    static int[] lightAxis(int radius){java.util.stream.IntStream.Builder a=java.util.stream.IntStream.builder();for(int i=-radius+2;i<=radius-2;i+=5)a.add(i);a.add(radius-2);return a.build().distinct().toArray();}
    static BlockState window(int bay,int y){int k=Math.floorMod(bay,4);if(y==1)return Blocks.POLISHED_ANDESITE.defaultBlockState();if(y==6)return WOOD;if(y==2||y==5)return DARK;if(k==0)return WOOD;if(k==1)return WALL;return Decor.LATTICE.get().defaultBlockState();}
    static void roof(Sink s,int x,int y,int z,int rw,int rd,int iw,int id,int type){
        for(int ix=-rw;ix<=rw;ix++)for(int iz=-rd;iz<=rd;iz++){
            if(iw>=0&&Math.abs(ix)<=iw&&Math.abs(iz)<=id)continue;
            int dx=rw-Math.abs(ix),dz=rd-Math.abs(iz),dep=type==1?dz:Math.min(dx,dz);
            int half=dep==0?1:dep==1?0:dep-1;
            if(dx<3&&dz<3)half+=Math.max(0,4-dx-dz);
            int yy=y+half/2;Direction facing=type!=1&&dx<dz?(ix>0?Direction.WEST:Direction.EAST):(iz>0?Direction.NORTH:Direction.SOUTH);
            Block full=dep==0?Blocks.DARK_PRISMARINE:Blocks.DEEPSLATE_TILES;
            s.block(x+ix,yy-1,z+iz,full.defaultBlockState());
            s.block(x+ix,yy,z+iz,half%2==0?stairs(dep==0?Blocks.DARK_PRISMARINE_STAIRS:Blocks.DEEPSLATE_TILE_STAIRS,facing,false):slab(dep==0?Blocks.DARK_PRISMARINE_SLAB:Blocks.DEEPSLATE_TILE_SLAB,true));
            if(dx==0&&dz==0)s.block(x+ix,yy+1,z+iz,Decor.EAVE.get().defaultBlockState().setValue(HorizontalDirectionalBlock.FACING,facing.getOpposite()));
            if(dep==0&&Math.floorMod(ix+iz,2)==0)s.block(x+ix,yy-1,z+iz,Decor.WADANG.get().defaultBlockState().setValue(HorizontalDirectionalBlock.FACING,facing.getOpposite()));
        }
        if(iw<0){int ry=y+(rd-1)/2+1,rl=type==1?rw-2:Math.max(1,rw-rd);box(s,x-rl,ry,z,x+rl,ry,z,Decor.RIDGE_TILE.get().defaultBlockState());
            for(int side:new int[]{-1,1}){s.block(x+side*rl,ry+1,z,Decor.RIDGE.get().defaultBlockState());s.block(x+side*(rl+1),ry+1,z,stairs(Blocks.DARK_PRISMARINE_STAIRS,side>0?Direction.EAST:Direction.WEST,false));}
            if(type==1)for(int side:new int[]{-1,1})for(int iz=-rd+2;iz<rd-1;iz++){int rise=Math.max(0,(rd-Math.abs(iz)-1)/2);for(int h=0;h<rise;h++)s.block(x+side*(rw-3),y+h,z+iz,Math.floorMod(iz,4)==0?WOOD:WALL);}
        }
    }
    static void court(Sink s,Building b,int phase,int w,int d){if(phase==0){for(int x=-w;x<=w;x++)for(int z=0;z<=d;z++)s.block(b.x+x,b.y(),b.z+z,Math.abs(x)<3?WHITE:Blocks.GRAVEL.defaultBlockState());}if(phase==3){table(s,b.x+4,b.y()+1,b.z+5,3);s.block(b.x-5,b.y()+1,b.z+5,Blocks.WATER_CAULDRON.defaultBlockState());}}
    static void veranda(Sink s,int ax,int az,int bx,int bz,int y,int phase){
        if(phase==0)platform(s,(ax+bx)/2,y,(az+bz)/2,(bx-ax)/2,(bz-az)/2);
        if(phase==1)for(int x=ax;x<=bx;x+=4){column(s,x,y,az,5);column(s,x,y,bz,5);}
        if(phase==2)roof(s,(ax+bx)/2,y+6,(az+bz)/2,(bx-ax)/2+2,(bz-az)/2+2,-1,-1,1);
    }
    static void ring(Sink s,int x,int y,int z,int r,BlockState b){for(int dx=-r;dx<=r;dx++)for(int dz=-r;dz<=r;dz++){double d=Math.hypot(dx,dz);if(d>=r-.7&&d<=r+.2)s.block(x+dx,y-1,z+dz,b);}}
    static int pathY(Route r,int i){return ROUTES_READY?RoadGrades.y(r,i):designPathY(r,i);}
    static int designPathY(Route r,int i){int n=Math.max(Math.abs(r.bx-r.ax),Math.abs(r.bz-r.az));if(n==0)return r.ay;
        int a=Math.abs(r.ax)<=10&&r.az>=15&&r.az<=34?14:4,b=Math.abs(r.bx)<=10&&r.bz>=15&&r.bz<=34?14:4,space=n-Math.abs(r.by-r.ay);
        if(a+b>space){double factor=Math.max(0,space)/(double)(a+b);a=(int)(a*factor);b=(int)(b*factor);}
        return r.ay+(int)Math.round((r.by-r.ay)*Math.max(0,Math.min(n-a-b,i-a))/(double)Math.max(1,n-a-b));
    }
    static boolean crossingWalkway(int x,int z,int y,Route own){for(Route r:ROUTES){if(r==own)continue;int nx=Math.max(Math.min(r.ax,r.bx),Math.min(Math.max(r.ax,r.bx),x)),nz=Math.max(Math.min(r.az,r.bz),Math.min(Math.max(r.az,r.bz),z));if(Math.hypot(x-nx,z-nz)<=2.1&&Math.abs(pathY(r,Math.max(Math.abs(nx-r.ax),Math.abs(nz-r.az)))-y)<=2)return true;}return false;}
    static void path(Sink s,int cx,int cz,Route r){
        int n=Math.max(Math.abs(r.bx-r.ax),Math.abs(r.bz-r.az));if(n==0)return;int dx=Integer.signum(r.bx-r.ax),dz=Integer.signum(r.bz-r.az);
        for(int i=0;i<=n;i++){int x=r.ax+dx*i,z=r.az+dz*i,y=pathY(r,i),ny=pathY(r,Math.min(n,i+1)),py=pathY(r,Math.max(0,i-1));
            if(Math.abs(x-cx*16-8)>12||Math.abs(z-cz*16-8)>12)continue;
            Direction dir=dx<0?Direction.WEST:dx>0?Direction.EAST:dz<0?Direction.NORTH:Direction.SOUTH;
            for(int w=-3;w<=3;w++){int px=x+dz*w,pz=z+dx*w;
                if(!r.bridge&&Math.abs(w)>2)continue;
                s.block(px,y-1,pz,r.bridge?WOOD:Blocks.STONE_BRICKS.defaultBlockState());
                s.block(px,y,pz,py<y?stairs(r.bridge?Blocks.SPRUCE_STAIRS:Blocks.STONE_BRICK_STAIRS,dir,false):ny<y?stairs(r.bridge?Blocks.SPRUCE_STAIRS:Blocks.STONE_BRICK_STAIRS,dir.getOpposite(),false):r.bridge?PLANK:Math.floorMod(x+z,7)==0?Blocks.CHISELED_STONE_BRICKS.defaultBlockState():Blocks.STONE_BRICKS.defaultBlockState());
                for(int h=1;h<=3;h++)s.block(px,y+h,pz,AIR);
                if(r.bridge&&Math.abs(w)==3&&!crossingWalkway(px,pz,y,r)){s.block(px,y+1,pz,Blocks.DARK_OAK_FENCE.defaultBlockState().setValue(FenceBlock.EAST,true).setValue(FenceBlock.WEST,true).setValue(FenceBlock.NORTH,true).setValue(FenceBlock.SOUTH,true));if(i%12==6)s.block(px,y+2,pz,Blocks.LANTERN.defaultBlockState());}
                if(!r.bridge)SiteTerrain.foundation(s,px,y-1,pz);
                if(!r.bridge&&Math.abs(w)==2&&Terrain.ground(px,pz)<Terrain.water(px,pz)&&y<=Terrain.water(px,pz)+4&&!crossingWalkway(px,pz,y,r)&&!nearPlot(px,pz,2)){
                    s.block(px,y,pz,Blocks.CHISELED_STONE_BRICKS.defaultBlockState());
                    s.block(px,y+1,pz,Blocks.STONE_BRICK_WALL.defaultBlockState());
                    if(i%12==6)s.block(px,y+2,pz,Blocks.LANTERN.defaultBlockState());
                }
                if(!r.bridge&&Math.abs(w)==2&&i%4==2&&ny==y&&py==y)s.block(px,y,pz,Blocks.SEA_LANTERN.defaultBlockState());
            }
        }
        if(r.bridge)SkyBridges.detail(s,cx,cz,r);
    }
    static void pine(Sink s,int x,int y,int z,boolean flowering){
        if(flowering){tree(s,x,y,z,true);return;}
        box(s,x,y,z,x,y+10,z,Blocks.SPRUCE_LOG.defaultBlockState());
        for(int level=0;level<4;level++){int cy=y+4+level*2,r=level==3?2:4-level/2;for(int dx=-r;dx<=r;dx++)for(int dz=-r;dz<=r;dz++)if(Math.abs(dx)+Math.abs(dz)<=r+1)s.block(x+dx,cy,z+dz,Blocks.SPRUCE_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT,true));}
    }
    static void column(Sink s,int x,int y,int z,int h){s.block(x,y+1,z,Blocks.CHISELED_STONE_BRICKS.defaultBlockState());box(s,x,y+2,z,x,y+h,z,WOOD);}
    static void bracket(Sink s,int x,int y,int z){s.block(x,y,z,Decor.DOUGONG.get().defaultBlockState());s.block(x-1,y,z,stairs(Blocks.SPRUCE_STAIRS,Direction.EAST,true));s.block(x+1,y,z,stairs(Blocks.SPRUCE_STAIRS,Direction.WEST,true));s.block(x,y,z+1,stairs(Blocks.SPRUCE_STAIRS,Direction.NORTH,true));beamX(s,x-2,x+2,y+1,z);s.block(x,y+1,z+2,slab(Blocks.SPRUCE_SLAB,true));}
    static void beamX(Sink s,int a,int b,int y,int z){box(s,a,y,z,b,y,z,WOOD.setValue(RotatedPillarBlock.AXIS,Direction.Axis.X));}
    static void beamZ(Sink s,int a,int b,int y,int x){box(s,x,y,a,x,y,b,WOOD.setValue(RotatedPillarBlock.AXIS,Direction.Axis.Z));}
    static BlockState stairs(Block b,Direction d,boolean top){return b.defaultBlockState().setValue(StairBlock.FACING,d).setValue(StairBlock.HALF,top?Half.TOP:Half.BOTTOM);}
    static BlockState slab(Block b,boolean top){return b.defaultBlockState().setValue(SlabBlock.TYPE,top?SlabType.TOP:SlabType.BOTTOM);}
    static BlockState fence(){return Blocks.SPRUCE_FENCE.defaultBlockState().setValue(FenceBlock.EAST,true).setValue(FenceBlock.WEST,true);}
    static void table(Sink s,int x,int y,int z,int n){for(int i=0;i<n;i++){s.block(x+i,y,z,slab(Blocks.SPRUCE_SLAB,true));if(i%2==0){s.block(x+i,y,z+1,stairs(Blocks.SPRUCE_STAIRS,Direction.SOUTH,false));s.block(x+i,y,z-1,stairs(Blocks.SPRUCE_STAIRS,Direction.NORTH,false));}}s.block(x+n/2,y+1,z,Blocks.LANTERN.defaultBlockState());}
    static void bookcase(Sink s,int x,int y,int z,int n){for(int i=0;i<n;i++){box(s,x+i,y,z,x+i,y+2,z,Blocks.BOOKSHELF.defaultBlockState());s.block(x+i,y+3,z,slab(Blocks.SPRUCE_SLAB,false));}}
    static void bedroom(Sink s,int x,int y,int z,int v){Block bed=v==0?Blocks.CYAN_BED:v==1?Blocks.GRAY_BED:Blocks.WHITE_BED;s.block(x,y,z,bed.defaultBlockState().setValue(BedBlock.FACING,Direction.NORTH).setValue(BedBlock.PART,BedPart.FOOT));s.block(x,y,z-1,bed.defaultBlockState().setValue(BedBlock.FACING,Direction.NORTH).setValue(BedBlock.PART,BedPart.HEAD));s.block(x+1,y,z-1,Blocks.BARREL.defaultBlockState());s.block(x+1,y+1,z-1,Blocks.LANTERN.defaultBlockState());box(s,x-1,y,z-2,x+1,y+2,z-2,DARK);s.block(x-1,y,z+1,Blocks.CYAN_CARPET.defaultBlockState());}
    static void kitchen(Sink s,int x,int y,int z){s.block(x,y,z,Blocks.SMOKER.defaultBlockState());s.block(x,y,z+1,Blocks.CRAFTING_TABLE.defaultBlockState());s.block(x,y,z+2,Blocks.WATER_CAULDRON.defaultBlockState());s.block(x,y+1,z,Blocks.POLISHED_DEEPSLATE.defaultBlockState());s.block(x,y,z+3,Blocks.BARREL.defaultBlockState());}
    static void stall(Sink s,int x,int y,int z,int v){for(int ix=-2;ix<=2;ix++){s.block(x+ix,y,z,Blocks.BARREL.defaultBlockState());s.block(x+ix,y+3,z,(v%2==0?Blocks.CYAN_WOOL:Blocks.WHITE_WOOL).defaultBlockState());s.block(x+ix,y+3,z+1,slab(Blocks.SPRUCE_SLAB,false));}for(int side:new int[]{-1,1})box(s,x+side*2,y+1,z,x+side*2,y+2,z,fence());s.block(x,y+1,z,Blocks.LANTERN.defaultBlockState());}
    static void lamp(Sink s,int x,int y,int z){
        if(road(x,z)){boolean moved=false;int originX=x,originZ=z;for(int radius=1;radius<=7&&!moved;radius++)for(int dz=radius;dz>=-radius&&!moved;dz--)for(int dx=-radius;dx<=radius;dx++){if(Math.max(Math.abs(dx),Math.abs(dz))!=radius)continue;int nx=originX+dx,nz=originZ+dz;if(!road(nx,nz)&&!nearPlot(nx,nz,0)){x=nx;z=nz;moved=true;break;}}if(!moved)return;}
        s.fixture(x,y,z);SiteTerrain.foundation(s,x,y,z);if(!Architecture.LEGACY.get()){Architecture.lamp(s,x,y,z);return;}s.block(x,y-1,z,Blocks.STONE_BRICKS.defaultBlockState());s.block(x,y,z,Blocks.CHISELED_STONE_BRICKS.defaultBlockState());box(s,x,y+1,z,x,y+4,z,WOOD);beamX(s,x-1,x+1,y+4,z);for(int side:new int[]{-1,1})s.block(x+side,y+3,z,Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING,true));for(int i=-1;i<=1;i++)s.block(x+i,y+5,z,slab(Blocks.DARK_PRISMARINE_SLAB,false));}
    static void garden(Sink s,int cx,int cz,int x,int z,int rx,int rz){
        if(cx*16>x+rx+5||cx*16+15<x-rx-5||cz*16>z+rz+5||cz*16+15<z-rz-5)return;int y=terrace(z);
        for(int ix=-rx;ix<=rx;ix++)for(int iz=-rz;iz<=rz;iz++){double r=ix*ix/(double)(rx*rx)+iz*iz/(double)(rz*rz);if(r<.75){s.block(x+ix,y-1,z+iz,(ix+iz)%7==0?Blocks.SEA_LANTERN.defaultBlockState():Blocks.CLAY.defaultBlockState());s.block(x+ix,y,z+iz,Blocks.WATER.defaultBlockState());s.block(x+ix,y+1,z+iz,AIR);if(Math.floorMod(ix*7+iz*11,43)==0)s.block(x+ix,y+1,z+iz,Blocks.LILY_PAD.defaultBlockState());}else if(r<1){s.block(x+ix,y,z+iz,Blocks.MOSSY_STONE_BRICKS.defaultBlockState());if(Math.floorMod(ix+iz,11)==0)s.block(x+ix,y+1,z+iz,Blocks.AZALEA.defaultBlockState());}}
        for(int ix=-rx;ix<=rx;ix++)for(int iz=-1;iz<=1;iz++){s.block(x+ix,y+1,z+iz,PLANK);if(Math.abs(iz)==1)s.block(x+ix,y+2,z+iz,fence());}
        for(int side:new int[]{-1,1})s.block(x+side*(rx+1),y+1,z,stairs(Blocks.SPRUCE_STAIRS,side<0?Direction.EAST:Direction.WEST,false));
    }
    static void tree(Sink s,int x,int y,int z,boolean cherry){BlockState log=(cherry?Blocks.CHERRY_LOG:Blocks.SPRUCE_LOG).defaultBlockState();box(s,x,y,z,x,y+6,z,log);s.block(x+1,y+4,z,log.setValue(RotatedPillarBlock.AXIS,Direction.Axis.X));s.block(x-1,y+5,z,log.setValue(RotatedPillarBlock.AXIS,Direction.Axis.X));for(int dx=-4;dx<=4;dx++)for(int dz=-4;dz<=4;dz++)for(int dy=0;dy<=3;dy++)if(dx*dx+dz*dz+dy*dy<19)s.block(x+dx,y+5+dy,z+dz,(cherry?Blocks.CHERRY_LEAVES:Blocks.AZALEA_LEAVES).defaultBlockState().setValue(LeavesBlock.PERSISTENT,true));}
    public static void box(Sink s,int x1,int y1,int z1,int x2,int y2,int z2,BlockState v){for(int x=x1;x<=x2;x++)for(int z=z1;z<=z2;z++)for(int y=y1;y<=y2;y++)s.block(x,y,z,v);}
    static void sign(Sink s,int x,int y,int z,String... text){SiteTerrain.foundation(s,x,y-1,z);s.block(x,y-1,z,Blocks.CHISELED_STONE_BRICKS.defaultBlockState());s.block(x,y,z,Blocks.SPRUCE_SIGN.defaultBlockState());signData(s,x,y,z,text);}
    static void wallSign(Sink s,int x,int y,int z,String... text){s.block(x,y,z,Blocks.SPRUCE_WALL_SIGN.defaultBlockState().setValue(WallSignBlock.FACING,Direction.SOUTH));signData(s,x,y,z,text);}
    static void signData(Sink s,int x,int y,int z,String... text){CompoundTag n=new CompoundTag();n.putString("id","minecraft:sign");n.putInt("x",x);n.putInt("y",y);n.putInt("z",z);CompoundTag front=new CompoundTag();ListTag lines=new ListTag();for(int i=0;i<4;i++)lines.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal(i<text.length?text[i]:""))));front.put("messages",lines);front.putString("color","white");front.putBoolean("has_glowing_text",true);n.put("front_text",front);n.put("back_text",front.copy());n.putBoolean("is_waxed",true);s.entity(n);}
}
