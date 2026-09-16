package cn.duskrain;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.*;

/** Original timber galleries inspired by Chinese covered arch bridges, with resting bays. */
public final class SkyBridges {
    static void detail(CityPlan.Sink s,int cx,int cz,CityPlan.Route r){
        int n=Math.max(Math.abs(r.bx()-r.ax()),Math.abs(r.bz()-r.az()));int dx=Integer.signum(r.bx()-r.ax()),dz=Integer.signum(r.bz()-r.az());
        for(int i=0;i<=n;i++){
            int x=r.ax()+dx*i,z=r.az()+dz*i,y=CityPlan.pathY(r,i);
            if(Math.abs(x-(cx*16+8))>14||Math.abs(z-(cz*16+8))>14)continue;
            // A continuous dark timber arch below each short span, with braced side beams.
            int sag=(int)Math.round(4*(1-Math.sin(Math.PI*(i%16)/16.0)));
            for(int side:new int[]{-1,1}){
                int px=x+dz*side*3,pz=z+dx*side*3;
                if(CityPlan.crossingWalkway(px,pz,y,r))continue;
                s.block(px,y-2-sag,pz,CityPlan.WOOD);
                if(i%8==0){
                    CityPlan.box(s,px,y-2-sag,pz,px,y+4,pz,CityPlan.WOOD);
                    s.block(px,y+1,pz,Blocks.CHISELED_STONE_BRICKS.defaultBlockState());
                    s.block(px-dz*side,y+5,pz-dx*side,Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING,true));
                }
            }
            // Narrow tiled roof; 4+ blocks of walking clearance, raised eaves and ridge caps.
            for(int w=-4;w<=4;w++){
                int yy=y+6+(3-Math.abs(w))/2;Direction face=dz!=0?(w>0?Direction.WEST:Direction.EAST):(w>0?Direction.NORTH:Direction.SOUTH);
                s.block(x+dz*w,yy,z+dx*w,Math.abs(w)==4?CityPlan.stairs(Blocks.DARK_PRISMARINE_STAIRS,face.getOpposite(),false):CityPlan.stairs(Blocks.DEEPSLATE_TILE_STAIRS,face,false));
                if(w==0)s.block(x,yy+1,z,Blocks.POLISHED_DEEPSLATE.defaultBlockState());
            }
        }
    }
    private static final java.util.Map<net.minecraft.server.level.ServerLevel,java.util.Set<Long>> PENDING=new java.util.concurrent.ConcurrentHashMap<>();
    private static final class Lamps {
        static final java.util.Map<Long,java.util.List<net.minecraft.core.BlockPos>> BY_CHUNK=index();
        private static java.util.Map<Long,java.util.List<net.minecraft.core.BlockPos>> index(){
            var out=new java.util.HashMap<Long,java.util.List<net.minecraft.core.BlockPos>>();
            for(var r:CityPlan.ROUTES){if(!r.bridge())continue;int n=Math.max(Math.abs(r.bx()-r.ax()),Math.abs(r.bz()-r.az())),dx=Integer.signum(r.bx()-r.ax()),dz=Integer.signum(r.bz()-r.az());
                for(int i=0;i<=n;i+=8){int x=r.ax()+dx*i,z=r.az()+dz*i,y=CityPlan.pathY(r,i);
                    for(int side:new int[]{-1,1}){
                        if(CityPlan.crossingWalkway(x+dz*side*3,z+dx*side*3,y,r))continue;
                        var old=new net.minecraft.core.BlockPos(x+dz*side*2,y+3,z+dx*side*2);
                        out.computeIfAbsent(net.minecraft.world.level.ChunkPos.asLong(old.getX()>>4,old.getZ()>>4),k->new java.util.ArrayList<>()).add(old);
                    }
                }
            }
            return out;
        }
    }
    /** Chunk Load fires before its FULL future completes. Never read blocks in this callback. */
    public static void loaded(net.minecraftforge.event.level.ChunkEvent.Load event){
        if(!(event.getLevel() instanceof net.minecraft.server.level.ServerLevel level)||!level.dimension().equals(Gameplay.CITY))return;
        long chunk=event.getChunk().getPos().toLong();
        if(Lamps.BY_CHUNK.containsKey(chunk))PENDING.computeIfAbsent(level,k->java.util.concurrent.ConcurrentHashMap.newKeySet()).add(chunk);
    }
    /** Migrate a bounded number after chunk loading has unwound, without requesting new chunks. */
    public static void tick(net.minecraftforge.event.TickEvent.ServerTickEvent event){
        if(event.phase!=net.minecraftforge.event.TickEvent.Phase.END)return;
        var level=event.getServer().getLevel(Gameplay.CITY);if(level==null)return;var pending=PENDING.get(level);
        if(pending==null)return;int budget=8;
        for(long key:pending){
            if(budget--<=0)break;var chunk=new net.minecraft.world.level.ChunkPos(key);
            pending.remove(key);
            if(level.getChunkSource().getChunkNow(chunk.x,chunk.z)==null)continue;
            int moved=0;
            for(var old:Lamps.BY_CHUNK.get(key)){
                if(liftLantern(level,old))moved++;
            }
            if(moved>0)DuskRain.LOG.info("BRIDGE_LANTERN_CLEARANCE chunk={} raised={}",chunk,moved);
        }
    }
    public static void reset(net.minecraft.server.MinecraftServer server){
        PENDING.keySet().removeIf(level->level.getServer()==server);
    }
    static boolean liftLantern(net.minecraft.server.level.ServerLevel level,net.minecraft.core.BlockPos old){
        var state=level.getBlockState(old);var target=old.above(2);
        if(!state.is(Blocks.LANTERN)||!state.getValue(LanternBlock.HANGING)||!level.getBlockState(target).isAir())return false;
        level.setBlock(old,Blocks.AIR.defaultBlockState(),3);level.setBlock(target,state,3);return true;
    }
    public record Joint(int x,int z,int y){}
    public static java.util.List<Joint> joints(){
        var out=new java.util.LinkedHashSet<Joint>();
        for(var a:CityPlan.ROUTES)for(var b:CityPlan.ROUTES){
            if(a==b||!a.bridge()||!b.bridge()||(a.ax()==a.bx())==(b.ax()==b.bx()))continue;
            if(a.bx()==b.ax()&&a.bz()==b.az()&&a.by()==b.ay())out.add(new Joint(a.bx(),a.bz(),a.by()));
        }
        return java.util.List.copyOf(out);
    }
    /** A final, shared landing takes precedence over the two adjoining galleries. */
    static void junctions(CityPlan.Sink s,int cx,int cz){for(var j:joints()){
        int x=j.x,z=j.z,y=j.y;if(Math.abs(x-(cx*16+8))>16||Math.abs(z-(cz*16+8))>16)continue;
        CityPlan.box(s,x-4,y-1,z-4,x+4,y-1,z+4,CityPlan.WOOD);
        CityPlan.box(s,x-4,y,z-4,x+4,y,z+4,CityPlan.PLANK);
        CityPlan.box(s,x-3,y+1,z-3,x+3,y+7,z+3,CityPlan.AIR);
        for(int dx=-4;dx<=4;dx++)for(int dz=-4;dz<=4;dz++)if(Math.abs(dx)==4||Math.abs(dz)==4){
            boolean opening=CityPlan.crossingWalkway(x+dx,z+dz,y,null);
            CityPlan.box(s,x+dx,y+1,z+dz,x+dx,y+7,z+dz,CityPlan.AIR);
            if(!opening)s.block(x+dx,y+1,z+dz,Blocks.DARK_OAK_FENCE.defaultBlockState().setValue(FenceBlock.EAST,true).setValue(FenceBlock.WEST,true).setValue(FenceBlock.NORTH,true).setValue(FenceBlock.SOUTH,true));
        }
        for(int dx:new int[]{-4,4})for(int dz:new int[]{-4,4}){
            CityPlan.box(s,x+dx,y-1,z+dz,x+dx,y+7,z+dz,CityPlan.WOOD);s.block(x+dx,y+5,z+dz,Blocks.SHROOMLIGHT.defaultBlockState());
        }
        CityPlan.roof(s,x,y+8,z,5,5,-1,-1,0);
    }}
}
