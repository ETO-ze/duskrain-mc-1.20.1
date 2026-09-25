package cn.duskrain;

import net.minecraft.core.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.*;
import java.util.*;

/** Original planting and garden architecture. All randomness depends on coordinates. */
public final class Landscape {
    public record Landmark(String id,String name,int x,int z,int y,int floors,boolean open){}
    public static final List<Landmark> LANDMARKS=List.of(
        new Landmark("moon_tower","揽月浮屠",-177,-145,128,3,false),
        new Landmark("tide_tower","听潮灯塔",196,184,Terrain.ground(196,184)+2,3,false),
        new Landmark("wind_bell","松涛钟阁",-222,169,Terrain.ground(-222,169)+2,2,true));
    public record Tree(int x,int y,int z,int type,int height,int radius){}
    public record Light(int x,int y,int z,boolean alongX){}
    private static final BlockState MOSS=Blocks.MOSSY_COBBLESTONE.defaultBlockState();
    static long hash(int x,int z){long h=x*341873128712L+z*132897987541L+205255670L;h=(h^(h>>>13))*1274126177L;return (h^(h>>>16))&Long.MAX_VALUE;}
    static double roadDistance(int x,int z){double distance=Double.POSITIVE_INFINITY;for(var r:CityPlan.ROUTES){int nx=Math.max(Math.min(r.ax(),r.bx()),Math.min(Math.max(r.ax(),r.bx()),x));int nz=Math.max(Math.min(r.az(),r.bz()),Math.min(Math.max(r.az(),r.bz()),z));distance=Math.min(distance,Math.hypot(x-nx,z-nz));}return distance;}
    static boolean reserved(int x,int z,int margin){
        if(CityPlan.nearPlot(x,z,margin))return true;
        if(x>=-12-margin&&x<=12+margin&&z>=12-margin&&z<=34+margin)return true;
        for(var b:LANDMARKS)if(Math.abs(x-b.x)<=12+margin&&Math.abs(z-b.z)<=14+margin)return true;
        return false;
    }
    // Build after CityPlan finishes registering routes, not during its static initialization.
    private static final class Planting {
        static final List<Tree> TREES=makeTrees();
        static final List<Light> LIGHTS=makeLights();
        static final Set<Long> BASES=bases();
        static Set<Long> bases(){Set<Long> out=new HashSet<>();for(var t:TREES)for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++)out.add(ChunkPosKey(t.x+dx,t.z+dz));for(var l:LIGHTS)for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++)out.add(ChunkPosKey(l.x+dx,l.z+dz));return out;}
    }
    public static List<Tree> trees(){return Planting.TREES;}
    public static List<Light> lights(){return Planting.LIGHTS;}
    static List<Tree> makeTrees(){List<Tree> out=new ArrayList<>();
        for(int gx=-492;gx<=492;gx+=12)for(int gz=-492;gz<=492;gz+=12){long h=hash(gx,gz);int x=gx+(int)(h%7)-3,z=gz+(int)(h/7%7)-3,y=SiteTerrain.surface(x,z);double near=roadDistance(x,z);int radius=3+(int)(h/49%2);
            if(y<=Terrain.water(x,z)+1||reserved(x,z,radius+2)||near<radius+4||Terrain.skyTop(x,z)>0)continue;
            // Denser groves beyond the town, with meadow openings between them.
            int density=(Math.abs(x)>180||z<-110)?86:72;if(h/343%100>=density)continue;
            if(Terrain.noise(x+700,z-600,46)<-.55&&near>18)continue;
            int slope=0;for(int dx:new int[]{-2,2})for(int dz:new int[]{-2,2})slope=Math.max(slope,Math.abs(SiteTerrain.surface(x+dx,z+dz)-y));if(slope>4)continue;
            int type=h/34300%11==0?2:h/34300%4==0?1:0;
            double creek=-104+13*Math.sin(z*.018);if(Math.abs(x-creek)<23&&z>-100&&z<185)type=3;
            int extent=radius+(type==0?0:2);if(reserved(x,z,extent+2)||near<extent+4)continue;
            out.add(new Tree(x,y+1,z,type,8+(int)(h/377300%5),radius));
        }
        // Curated groves on the three sky gardens. Small trees retain a clear skyline.
        int[][] sky={{-46,-193},{46,-193},{-30,-224},{30,-224},{140,-121},{195,-121},{-195,-136},{-194,-119},{-161,-130}};
        for(int i=0;i<sky.length;i++){int x=sky[i][0],z=sky[i][1],y=Terrain.skyTop(x,z);if(y>0&&!reserved(x,z,5)&&roadDistance(x,z)>=7)out.add(new Tree(x,y+1,z,0,7,3));}
        return List.copyOf(out);
    }
    static List<Light> makeLights(){List<Light> out=new ArrayList<>();Set<Long> occupied=new HashSet<>();
        for(var r:CityPlan.ROUTES){if(r.bridge())continue;int n=Math.max(Math.abs(r.bx()-r.ax()),Math.abs(r.bz()-r.az()));int dx=Integer.signum(r.bx()-r.ax()),dz=Integer.signum(r.bz()-r.az());
            for(int i=5;i<n-3;i+=10){int side=(i/10)%2==0?1:-1,x=r.ax()+dx*i+dz*side*4,z=r.az()+dz*i+dx*side*4,y=CityPlan.pathY(r,i);
                if(roadDistance(x,z)<3.9||reserved(x,z,1))continue;long cell=ChunkPosKey(x,z);if(!occupied.add(cell)||out.stream().anyMatch(l->Math.hypot(l.x-x,l.z-z)<8))continue;
                if(Math.abs(SiteTerrain.surface(x,z)-y)>4)continue;out.add(new Light(x,y+1,z,dx!=0));
            }
        }return List.copyOf(out);
    }
    static long ChunkPosKey(int x,int z){return ((long)x<<32)^(z&0xffffffffL);}
    public static void paint(CityPlan.Sink s,int cx,int cz,int phase){
        for(var b:LANDMARKS)if(Math.abs(b.x-(cx*16+8))<26&&Math.abs(b.z-(cz*16+8))<27)landmark(s,b,phase);
        if(phase==4)for(var light:lights())if(Math.abs(light.x-(cx*16+8))<=11&&Math.abs(light.z-(cz*16+8))<=11)lamp(s,light);
        if(phase!=5)return;
        for(var tree:trees())if(Math.abs(tree.x-(cx*16+8))<=tree.radius+11&&Math.abs(tree.z-(cz*16+8))<=tree.radius+11)tree(s,tree);
        for(int x=cx*16;x<cx*16+16;x++)for(int z=cz*16;z<cz*16+16;z++)groundCover(s,x,z);
    }
    static void groundCover(CityPlan.Sink s,int x,int z){
        if(Math.abs(x)>505||Math.abs(z)>505||reserved(x,z,2))return;
        double road=roadDistance(x,z);if(road<3.8)return;
        int y=Math.max(SiteTerrain.surface(x,z),Terrain.skyTop(x,z));
        if(y<=Terrain.water(x,z)+1)return;
        // Plants require actual natural grass, not bridge piers or paved courtyard supports.
        if(Planting.BASES.contains(ChunkPosKey(x,z)))return;
        long hash=hash(x,z);int h=(int)(hash%100);boolean verge=road<9;
        double patch=Terrain.noise(x+420,z-190,15);int density=verge?58:patch>.1?35:13;
        if(h>=density)return;
        BlockState plant;
        if(verge&&h<12){Block[] flowers={Blocks.AZURE_BLUET,Blocks.OXEYE_DAISY,Blocks.CORNFLOWER,Blocks.ALLIUM,Blocks.LILY_OF_THE_VALLEY};plant=flowers[(int)(hash/1000%flowers.length)].defaultBlockState();}
        else if(h<3&&road>7)plant=(h==0?Blocks.FLOWERING_AZALEA:Blocks.AZALEA).defaultBlockState();
        else plant=(hash/100%3==0?Blocks.FERN:Blocks.GRASS).defaultBlockState();
        s.block(x,y+1,z,plant);
        if(road>12&&h==9&&hash/100%7==0){s.block(x,y,z,MOSS);s.block(x,y+1,z,Blocks.MOSS_CARPET.defaultBlockState());}
    }
    static void tree(CityPlan.Sink s,Tree t){
        int x=t.x,y=t.y,z=t.z,height=t.height,r=t.radius;BlockState log=(t.type==2?Blocks.CHERRY_LOG:t.type==3?Blocks.DARK_OAK_LOG:Blocks.SPRUCE_LOG).defaultBlockState();
        BlockState leaves=(t.type==2?Blocks.CHERRY_LEAVES:t.type==3?Blocks.OAK_LEAVES:Blocks.SPRUCE_LEAVES).defaultBlockState().setValue(LeavesBlock.PERSISTENT,true);
        if(t.type==0){for(int cy=3;cy<=height+1;cy++){int rr=Math.max(0,Math.min(r,(height+1-cy)/2+(cy%2)));for(int dx=-rr;dx<=rr;dx++)for(int dz=-rr;dz<=rr;dz++)if(dx*dx+dz*dz<=rr*rr+1&&(dx!=0||dz!=0||cy>height))s.block(x+dx,y+cy,z+dz,leaves);}}
        else {for(int branch:new int[]{-1,1}){int by=y+height-3+(branch>0?1:0);for(int dx=1;dx<=3;dx++)s.block(x+branch*dx,by+dx/3,z,log.setValue(RotatedPillarBlock.AXIS,Direction.Axis.X));
                for(int dx=-r;dx<=r;dx++)for(int dz=-r;dz<=r;dz++)for(int dy=-1;dy<=2;dy++)if(dx*dx+dz*dz+dy*dy*3<=r*r+2)s.block(x+branch*2+dx,by+2+dy,z+dz,leaves);}
            if(t.type==3)for(int dx=-r-2;dx<=r+2;dx++)for(int dz=-r;dz<=r;dz++)if((Math.abs(dx)==r+2||Math.abs(dz)==r)&&hash(x+dx,z+dz)%3==0)for(int dy=1;dy<=3;dy++)s.block(x+dx,y+height-dy,z+dz,leaves);
        }
        // Trunks go last so layered canopies cannot cut through the load bearing stem.
        CityPlan.box(s,x,y,z,x,y+height,z,log);
        for(Direction d:Direction.Plane.HORIZONTAL){int px=x+d.getStepX(),pz=z+d.getStepZ(),gy=Math.max(SiteTerrain.surface(px,pz),Terrain.skyTop(px,pz));if(gy<y-1&&gy>=y-4)CityPlan.box(s,px,gy+1,pz,px,y-1,pz,log);}
    }
    static void lamp(CityPlan.Sink s,Light l){
        int x=l.x,y=l.y,z=l.z;if(!Architecture.LEGACY.get()){SiteTerrain.foundation(s,x,y,z);Architecture.lamp(s,x,y,z);return;}CityPlan.box(s,x,SiteTerrain.surface(x,z)+1,z,x,y,z,Blocks.STONE_BRICKS.defaultBlockState());
        s.block(x,y,z,Blocks.CHISELED_STONE_BRICKS.defaultBlockState());CityPlan.box(s,x,y+1,z,x,y+3,z,CityPlan.WOOD);
        for(int i=-1;i<=1;i++){int px=x+(l.alongX?0:i),pz=z+(l.alongX?i:0);s.block(px,y+4,pz,CityPlan.slab(Blocks.DARK_PRISMARINE_SLAB,false));if(i!=0){s.block(px,y+3,pz,CityPlan.WOOD);s.block(px,y+2,pz,Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING,true));}}
        s.block(x,y+4,z,Decor.RIDGE.get().defaultBlockState());
    }
    static void landmark(CityPlan.Sink s,Landmark b,int phase){
        var owner=new CityPlan.Building(b.id,b.name,b.x,b.z,9,9,b.floors,20,b.y);
        CityPlan.hall(s,owner,b.x,b.z,9,9,b.floors,0,b.open,phase);
        if(phase==0){for(int dx=-2;dx<=2;dx++)for(int dz=8;dz<=15;dz++){s.block(b.x+dx,b.y,b.z+dz,CityPlan.WHITE);for(int dy=1;dy<=4;dy++)s.block(b.x+dx,b.y+dy,b.z+dz,CityPlan.AIR);}}
        if(phase==1&&b.open){for(int f=1;f<b.floors;f++){int y=b.y+f*8;for(int a=-8;a<=8;a++)for(int side:new int[]{-1,1}){s.block(b.x+a,y+1,b.z+side*8,CityPlan.fence());s.block(b.x+side*8,y+1,b.z+a,CityPlan.fence());}}}
        if(phase==3){
            for(int f=0;f<b.floors;f++){int y=b.y+f*8;CityPlan.table(s,b.x+3,y+1,b.z+2,2);s.block(b.x+4,y+1,b.z-3,Blocks.LECTERN.defaultBlockState().setValue(LecternBlock.FACING,Direction.SOUTH));s.block(b.x+4,y+3,b.z,Decor.LANTERN.get().defaultBlockState());CityPlan.box(s,b.x+4,y+4,b.z,b.x+4,y+6,b.z,Blocks.CHAIN.defaultBlockState());}
            if(b.open){s.block(b.x,b.y+12,b.z,Blocks.BELL.defaultBlockState().setValue(BellBlock.ATTACHMENT,BellAttachType.CEILING));CityPlan.box(s,b.x,b.y+13,b.z,b.x,b.y+14,b.z,Blocks.CHAIN.defaultBlockState());}
            else if(b.id.equals("tide_tower")){s.block(b.x,b.y+20,b.z,Blocks.SEA_LANTERN.defaultBlockState());CityPlan.box(s,b.x,b.y+21,b.z,b.x,b.y+22,b.z,Blocks.CHAIN.defaultBlockState());}
            CityPlan.sign(s,b.x+4,b.y+1,b.z+12,b.name,"可步行登楼 · 楼梯在左侧","登临听风 · 眺望烟雨","DuskRain · 205255670");
        }
        if(phase==4){CityPlan.lamp(s,b.x-7,b.y+1,b.z+12);CityPlan.lamp(s,b.x+7,b.y+1,b.z+12);}
    }
}
