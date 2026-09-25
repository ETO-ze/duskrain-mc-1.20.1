package cn.duskrain;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.*;

/** V3 exterior design: ground, circulation and planting have separate ownership masks. */
public final class CourtGarden {
    static final ThreadLocal<Boolean> DISABLED=ThreadLocal.withInitial(()->false);
    public record Surface(int x,int z,double top,String zone){}
    static final Map<BlockPos,BlockState> blocks=new LinkedHashMap<>();
    static final Map<Long,Surface> walk=new LinkedHashMap<>();
    static final Set<BlockPos> fixtures=new LinkedHashSet<>();
    static final List<String> conflicts=new ArrayList<>();
    static final Map<Long,Integer> shoreLand=new LinkedHashMap<>();
    static final Map<Long,Integer> gardens=new LinkedHashMap<>();
    static boolean ready;
    static long key(int x,int z){return net.minecraft.world.level.ChunkPos.asLong(x,z);}
    static double half(double h){return Math.round(h*2)/2.0;}
    static boolean shopInside(int x,int z){return PlayerMarket.SITES.stream().anyMatch(b->Math.abs(x-b.x())<=5&&Math.abs(z-b.z())<=5);}
    static double shopTop(int z){
        var rows=PlayerMarket.SITES.stream().filter(b->b.x()>0).toList();
        for(int i=0;i<rows.size()-1;i++){var a=rows.get(i);var b=rows.get(i+1);if(z<=a.z()+5)return a.y()+1;if(z<b.z()-5)return half(a.y()+1+(b.y()-a.y())*(z-a.z()-5.0)/(b.z()-a.z()-10));}
        return rows.get(rows.size()-1).y()+1;
    }
    static double streetTop(int z){double sum=0;for(int dz=-2;dz<=2;dz++)sum+=CityPlan.hubHeight(0,z+dz)+1;return half(sum/5);}
    static boolean natural(BlockState s){var b=s.getBlock();return s.isAir()||b instanceof LiquidBlock||b instanceof BushBlock||b==Blocks.DIRT||b==Blocks.GRASS_BLOCK||b==Blocks.GRAVEL||b==Blocks.STONE||b==Blocks.TUFF||b==Blocks.CLAY||b==Blocks.SAND||b==Blocks.STONE_BRICKS||b==Blocks.CHISELED_STONE_BRICKS||b==Blocks.STONE_BRICK_STAIRS||b==Blocks.SEA_LANTERN;}
    static boolean obsoleteLamp(BlockPos p,BlockState s){return Math.abs(p.getX())==5&&p.getZ()>=240&&p.getZ()<=270&&p.getY()>=61&&p.getY()<=66&&(s.is(Blocks.LANTERN)||s.is(Blocks.STRIPPED_DARK_OAK_LOG)||s.is(Blocks.DARK_PRISMARINE_SLAB));}
    static boolean fixed(int x,int z){return ArchitectureFinishes.bases().stream().anyMatch(p->p.getX()==x&&p.getZ()==z);}
    static void ground(int x,int z,double top,String zone,boolean edge,boolean clear){
        int y=(int)Math.ceil(top)-1;long k=key(x,z);var old=walk.get(k);
        if(old!=null&&Math.abs(old.top()-top)>.01){conflicts.add("surface "+x+","+z+" "+old.zone()+" / "+zone);return;}
        walk.put(k,new Surface(x,z,top,zone));
        int natural=Math.min(Terrain.ground(x,z),SiteTerrain.surface(x,z));
        for(int yy=Math.max(1,Math.min(natural,59));yy<y;yy++)blocks.put(new BlockPos(x,yy,z),Architecture.state("jade_bricks"));
        boolean slab=top!=Math.rint(top);BlockState floor=Architecture.state(slab?(edge?"jade_slab":"grey_slab"):(edge?"white_jade":"grey_bricks"));
        if(!slab&&!edge&&Math.floorMod(x+z,12)==0)floor=Architecture.state("grey_carved");
        blocks.put(new BlockPos(x,y,z),floor);
        if(clear)for(int h=1;h<=3;h++)blocks.put(new BlockPos(x,y+h,z),Blocks.AIR.defaultBlockState());
    }
    static boolean nearExit(int x,int z){return WaterCourt.EXITS.stream().anyMatch(e->Math.abs(x-e.x())+Math.abs(z-e.z())<6);}
    static boolean nearWalk(int x,int z,int radius){for(int dx=-radius;dx<=radius;dx++)for(int dz=-radius;dz<=radius;dz++)if(walk.containsKey(key(x+dx,z+dz)))return true;return false;}
    static void decor(BlockPos p,BlockState state){
        var route=walk.get(key(p.getX(),p.getZ()));if(route!=null&&p.getY()>=route.top()-.01&&p.getY()<route.top()+3){conflicts.add("decoration on path "+p.toShortString());return;}
        if(nearExit(p.getX(),p.getZ()))return;blocks.put(p,state);if(!state.isAir())fixtures.add(p);
    }
    public static synchronized Map<BlockPos,BlockState> plan(){
        if(ready)return blocks;ready=true;WaterCourt.plan();
        // Connect both rows, including previously unregistered two-block slots between plots.
        for(int side:new int[]{-1,1})for(int z=170;z<=269;z++)for(int a=4;a<=24;a++){
            int x=side*a;if(shopInside(x,z)||fixed(x,z))continue;
            double top=shopTop(z);if(a<11){double street=streetTop(z);top=half(street+(top-street)*(a-3)/8.0);}
            ground(x,z,top,"shop",a==11||a>=21,false);
            int y=(int)Math.ceil(top)-1;for(int h=1;h<=3;h++)blocks.put(new BlockPos(x,y+h,z),Blocks.AIR.defaultBlockState());
        }
        for(int z=170;z<=269;z++){
            int y=(int)Math.ceil(shopTop(z))-1;var old=WaterCourt.plan().get(new BlockPos(25,WaterCourt.promenadeY(z),z));
            if(old!=null&&old.getBlock() instanceof StairBlock)continue;
            for(int yy=59;yy<=y;yy++)blocks.put(new BlockPos(25,yy,z),Architecture.state("jade_bricks"));blocks.put(new BlockPos(25,y,z),Architecture.state("white_jade"));
            for(int yy=y+1;yy<=Math.max(y+2,WaterCourt.promenadeY(z)+1);yy++)blocks.put(new BlockPos(25,yy,z),Blocks.AIR.defaultBlockState());
            blocks.put(new BlockPos(25,y+1,z),Architecture.state("jade_railing"));
            if(Math.floorMod(z-174,8)==0&&shopTop(z)==Math.rint(shopTop(z)))blocks.put(new BlockPos(24,y,z),Architecture.state("jade_light"));
        }
        // Foundations are continuous beneath occupied shops; existing finished floors remain intact.
        for(var b:PlayerMarket.SITES)for(int x=b.x()-5;x<=b.x()+5;x++)for(int z=b.z()-5;z<=b.z()+5;z++)
            for(int y=Math.max(1,Math.min(Terrain.ground(x,z),59));y<b.y()-2;y++)blocks.put(new BlockPos(x,y,z),Architecture.state("jade_bricks"));
        // Fill the entire platform envelope, not only the thin water-facing rim.
        for(var b:CityPlan.BUILDINGS)if(b.id().startsWith("guild")){
            Map<BlockPos,BlockState> body=new HashMap<>();CityPlan.Sink sink=(x,y,z,s)->body.put(new BlockPos(x,y,z),Architecture.material(s));for(int phase=0;phase<6;phase++)CityPlan.building(sink,b,phase);
            for(int x=b.x()-b.w()-1;x<=b.x()+b.w()+1;x++)for(int z=b.z()-b.d()-1;z<=b.z()+b.d()+2;z++){
                for(int y=Math.max(1,Math.min(Terrain.ground(x,z),59));y<b.y();y++)blocks.put(new BlockPos(x,y,z),Architecture.state("jade_bricks"));
                var pos=new BlockPos(x,b.y(),z);var existing=body.get(pos);var court=WaterCourt.plan().get(pos);
                if(existing!=null&&!existing.isAir()||court!=null&&court.getBlock() instanceof StairBlock)continue;
                blocks.put(pos,Architecture.state("white_jade"));
                boolean clear=true;for(int y=1;y<=3;y++){var above=body.get(pos.above(y));if(above!=null&&!above.isAir())clear=false;}
                if(clear&&!fixed(x,z)&&Math.abs(x-b.x())<b.w()+1&&z<b.z()+b.d()+2&&z>b.z()-b.d()-1)walk.put(key(x,z),new Surface(x,z,b.y()+1,"guild apron"));
            }
        }
        // Dry-side distance field keeps all planting outside source-water and building masks.
        Map<Long,Integer> distance=new HashMap<>();Deque<BlockPos> queue=new ArrayDeque<>();
        for(var p:WaterCourt.POOL){distance.put(key(p.getX(),p.getZ()),0);queue.add(p);}
        while(!queue.isEmpty()){var p=queue.remove();int d=distance.get(key(p.getX(),p.getZ()));if(d==16)continue;for(Direction dir:Direction.Plane.HORIZONTAL){var n=p.relative(dir);long k=key(n.getX(),n.getZ());if(!distance.containsKey(k)){distance.put(k,d+1);queue.add(n);}}}
        // A complete dry-side belt: the old d<2 / exit-radius exclusions left real troughs.
        // Preserve actual bank/exit columns; join their land side with half-height paving.
        Set<Long> courtColumns=new HashSet<>();for(var p:WaterCourt.plan().keySet())courtColumns.add(key(p.getX(),p.getZ()));
        for(int x=26;x<=126;x++)for(int z=154;z<=221;z++){
            long k=key(x,z);int d=distance.getOrDefault(k,99);
            // The 16-block landscape buffer is radial: Manhattan distance left diagonal gravel slots.
            if(d>16){int nearest=257;for(var water:WaterCourt.POOL){int dx=x-water.getX(),dz=z-water.getZ();nearest=Math.min(nearest,dx*dx+dz*dz);}if(nearest<=256)d=16;}
            if(d<1||d>16||WaterCourt.footprint(x,z)||CityPlan.nearPlot(x,z,1)||fixed(x,z)||courtColumns.contains(k))continue;
            int raw=Terrain.ground(x,z);if(raw<62||raw>66)continue;
            if(CityPlan.road(x,z))continue; // Existing road surfaces remain authoritative.
            double top=d<=2?64:d==3?64.5:65;
            boolean approach=false;
            for(var e:WaterCourt.EXITS){int dist=Math.abs(x-e.x())+Math.abs(z-e.z());if(dist<=5){top=Math.min(top,e.bankY()+1+Math.max(0,dist-2)*.5);approach=true;}}
            // Join the edge of existing roads without an uncovered one-block ditch.
            for(var r:CityPlan.ROUTES)if(!r.bridge()){
                int nx=Math.max(Math.min(r.ax(),r.bx()),Math.min(Math.max(r.ax(),r.bx()),x));
                int nz=Math.max(Math.min(r.az(),r.bz()),Math.min(Math.max(r.az(),r.bz()),z));
                int dist=Math.abs(x-nx)+Math.abs(z-nz);
                if(dist>=2&&dist<=5){double road=CityPlan.pathY(r,Math.max(Math.abs(nx-r.ax()),Math.abs(nz-r.az())))+1;
                    top=Math.max(road-(dist-1)*.5,Math.min(top,road+(dist-1)*.5));approach=true;
                }
            }
            top=half(top);
            boolean path=d<=9||approach;
            if(path){ground(x,z,top,"garden path",d<=4||d==5||d==9,true);shoreLand.put(k,(int)Math.ceil(top)-1);continue;}
            int y=64;gardens.put(k,y);shoreLand.put(k,y);
            for(int yy=59;yy<=y;yy++)blocks.put(new BlockPos(x,yy,z),yy==y?Blocks.GRASS_BLOCK.defaultBlockState():Blocks.DIRT.defaultBlockState());
            for(int yy=y+1;yy<=Math.max(raw,y)+2;yy++)blocks.put(new BlockPos(x,yy,z),Blocks.AIR.defaultBlockState());
        }
        for(var e:gardens.entrySet()){int x=net.minecraft.world.level.ChunkPos.getX(e.getKey()),z=net.minecraft.world.level.ChunkPos.getZ(e.getKey()),y=e.getValue();int h=Math.floorMod(x*71+z*43,29);
            if(h<6&&Terrain.noise(x+27,z-112,7)>-.15)decor(new BlockPos(x,y+1,z),(h<2?Blocks.ALLIUM:h<4?Blocks.AZURE_BLUET:Blocks.LILY_OF_THE_VALLEY).defaultBlockState());
            else if(h==7||h==8)decor(new BlockPos(x,y+1,z),Blocks.FERN.defaultBlockState());
            else if(h==12&& !nearWalk(x,z,1))decor(new BlockPos(x,y+1,z),Blocks.FLOWERING_AZALEA.defaultBlockState());
        }
        // Three small resting pockets; choose dry garden cells adjacent to the contour path.
        int[][] targets={{63,175},{36,202},{106,199}};
        for(int[] target:targets){var chosen=gardens.entrySet().stream().filter(e->{int x=net.minecraft.world.level.ChunkPos.getX(e.getKey()),z=net.minecraft.world.level.ChunkPos.getZ(e.getKey());return !nearWalk(x,z,1)&&nearWalk(x,z,4)&&!nearExit(x,z);}).min(Comparator.comparingDouble(e->Math.hypot(net.minecraft.world.level.ChunkPos.getX(e.getKey())-target[0],net.minecraft.world.level.ChunkPos.getZ(e.getKey())-target[1])));
            if(chosen.isEmpty())continue;long k=chosen.get().getKey();int x=net.minecraft.world.level.ChunkPos.getX(k),z=net.minecraft.world.level.ChunkPos.getZ(k),y=chosen.get().getValue();
            for(int dx=-2;dx<=2;dx++)for(int dz=-1;dz<=1;dz++)if(gardens.containsKey(key(x+dx,z+dz))&&!nearWalk(x+dx,z+dz,0)){for(int yy=Math.min(gardens.get(key(x+dx,z+dz)),y)-1;yy<=y;yy++)blocks.put(new BlockPos(x+dx,yy,z+dz),Architecture.state("stone_paver"));for(int h=1;h<=3;h++)blocks.put(new BlockPos(x+dx,y+h,z+dz),Blocks.AIR.defaultBlockState());}
            for(int dx=-1;dx<=1;dx++)decor(new BlockPos(x+dx,y+1,z),Architecture.state("jade_slab"));decor(new BlockPos(x-2,y+1,z),Architecture.state("garden_lamp"));decor(new BlockPos(x+2,y+1,z),Architecture.state("garden_lamp"));
        }
        int trees=0;for(var e:gardens.entrySet()){int x=net.minecraft.world.level.ChunkPos.getX(e.getKey()),z=net.minecraft.world.level.ChunkPos.getZ(e.getKey()),y=e.getValue();if(trees>=8||Math.floorMod(x*19+z*13,89)!=0||nearWalk(x,z,3)||nearExit(x,z))continue;
            boolean crowded=fixtures.stream().anyMatch(p->p.getY()>y+2&&Math.abs(p.getX()-x)<7&&Math.abs(p.getZ()-z)<7);if(crowded)continue;
            for(int dy=1;dy<=4;dy++)decor(new BlockPos(x,y+dy,z),Blocks.CHERRY_LOG.defaultBlockState());
            for(int dx=-2;dx<=2;dx++)for(int dz=-2;dz<=2;dz++)for(int dy=3;dy<=5;dy++)if(Math.abs(dx)+Math.abs(dz)+(dy==5?1:0)<=3&&(dx!=0||dz!=0||dy==5))decor(new BlockPos(x+dx,y+dy,z+dz),Blocks.CHERRY_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT,true));trees++;
        }
        for(var e:gardens.entrySet()){int x=net.minecraft.world.level.ChunkPos.getX(e.getKey()),z=net.minecraft.world.level.ChunkPos.getZ(e.getKey());if(Math.floorMod(x*11+z*7,61)==0&&nearWalk(x,z,2)&&!nearWalk(x,z,0))decor(new BlockPos(x,e.getValue()+1,z),Architecture.state("garden_lamp"));}
        int groups=0;for(var e:gardens.entrySet()){int x=net.minecraft.world.level.ChunkPos.getX(e.getKey()),z=net.minecraft.world.level.ChunkPos.getZ(e.getKey()),y=e.getValue();if(groups>=9||Math.floorMod(x*31+z*17,107)!=0||nearWalk(x,z,1)||nearExit(x,z))continue;
            if(groups%3==0){decor(new BlockPos(x,y+1,z),Architecture.state("grey_plinth"));decor(new BlockPos(x,y+2,z),Architecture.state("grey_slab"));}
            else for(int dy=1;dy<=3;dy++)decor(new BlockPos(x,y+dy,z),Blocks.BAMBOO.defaultBlockState().setValue(BambooStalkBlock.STAGE,1).setValue(BambooStalkBlock.LEAVES,dy==3?BambooLeaves.LARGE:BambooLeaves.NONE));groups++;
        }
        return blocks;
    }
    public static void paint(CityPlan.Sink sink,int cx,int cz){if(DISABLED.get()||cx<-2||cx>7||cz<9||cz>17)return;for(var e:plan().entrySet())if(e.getKey().getX()>>4==cx&&e.getKey().getZ()>>4==cz)sink.block(e.getKey().getX(),e.getKey().getY(),e.getKey().getZ(),e.getValue());}
}
