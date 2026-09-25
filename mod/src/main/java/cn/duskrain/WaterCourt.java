package cn.duskrain;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.*;

/** Bounded, source-water garden. Depth changes happen below the retained waterline. */
public final class WaterCourt {
    public record Exit(int x,int z,int bankY,Direction outward){}
    public static final List<Exit> EXITS=new ArrayList<>();
    public static final Set<BlockPos> POOL=new LinkedHashSet<>();
    private static Map<BlockPos,BlockState> plan;
    static boolean footprint(int x,int z){for(var b:CityPlan.BUILDINGS)if(b.id().startsWith("guild")&&Math.abs(x-b.x())<=b.w()+1&&z>=b.z()-b.d()-1&&z<=b.z()+b.d()+2)return true;for(var b:PlayerMarket.SITES)if(Math.abs(x-b.x())<=5&&z>=b.z()-5&&z<=b.z()+8)return true;return false;}
    static int promenadeY(int z){var rows=PlayerMarket.SITES.stream().filter(b->b.x()>0).toList();for(int i=0;i<rows.size()-1;i++){var a=rows.get(i);var b=rows.get(i+1);if(z<=b.z())return a.y()+Math.floorDiv((b.y()-a.y())*Math.max(0,z-a.z()),b.z()-a.z());}return rows.get(rows.size()-1).y();}
    static int deck(int x,int z){
        if(x>=22&&x<=25&&z>=170&&z<=269)return promenadeY(z);
        for(var b:CityPlan.BUILDINGS)if(b.id().startsWith("guild")&&Math.abs(x-b.x())<=b.w()+1&&z>=b.z()-b.d()-1&&z<=b.z()+b.d()+2)return b.y();
        for(var b:PlayerMarket.SITES)if(Math.abs(x-b.x())<=5&&z>=b.z()-5&&z<=b.z()+8)return b.y();
        for(var r:CityPlan.ROUTES)if(!r.bridge()) {int nx=Math.max(Math.min(r.ax(),r.bx()),Math.min(Math.max(r.ax(),r.bx()),x)),nz=Math.max(Math.min(r.az(),r.bz()),Math.min(Math.max(r.az(),r.bz()),z));if(Math.abs(nx-x)+Math.abs(nz-z)<=3)return CityPlan.pathY(r,Math.max(Math.abs(nx-r.ax()),Math.abs(nz-r.az())));}
        return Math.max(Terrain.ground(x,z),63);
    }
    public static synchronized Map<BlockPos,BlockState> plan(){
        if(plan!=null)return plan;Map<BlockPos,BlockState> out=new LinkedHashMap<>();
        for(int x=26;x<=112;x++)for(int z=170;z<=277;z++)if(Terrain.ground(x,z)<62&&!footprint(x,z)&&!CityPlan.road(x,z))POOL.add(new BlockPos(x,62,z));
        Set<BlockPos> openings=new HashSet<>();
        // Short, two-block wide flights point back to each bank. Distribute by bank distance.
        for(var p:POOL)for(Direction d:Direction.Plane.HORIZONTAL){var land=p.relative(d);if(POOL.contains(land))continue;
            int by=deck(land.getX(),land.getZ());if(by>64||land.getX()==25&&(promenadeY(land.getZ()-1)!=by||promenadeY(land.getZ()+1)!=by))continue;
            Direction side=d.getClockWise();boolean room=true;for(int k=0;k<2;k++)for(int i=0;i<4;i++)if(!POOL.contains(p.relative(side,k).relative(d.getOpposite(),i)))room=false;
            for(var b:CityPlan.BUILDINGS)if(Math.abs(land.getX()-(b.x()+4))+Math.abs(land.getZ()-(b.z()+b.d()+1))<4)room=false;
            for(var b:PlayerMarket.SITES)if(Math.abs(land.getX()-(b.x()+3))+Math.abs(land.getZ()-(b.z()+6))<4)room=false;
            for(var fixture:ArchitectureFinishes.bases())if(Math.abs(land.getX()-fixture.getX())+Math.abs(land.getZ()-fixture.getZ())<5)room=false;
            if(!room||EXITS.stream().anyMatch(e->Math.abs(e.x-p.getX())+Math.abs(e.z-p.getZ())<12))continue;
            EXITS.add(new Exit(p.getX(),p.getZ(),by,d.getOpposite()));
            for(int k=0;k<2;k++)openings.add(land.relative(side,k));
        }
        for(var p:POOL){int dist=5;for(int dx=-4;dx<=4;dx++)for(int dz=-4;dz<=4;dz++)if(!POOL.contains(p.offset(dx,0,dz)))dist=Math.min(dist,Math.abs(dx)+Math.abs(dz));
            int depth=dist<=2?1:dist<=4?2:3,floor=62-depth;
            for(int y=floor-2;y<=floor;y++)out.put(new BlockPos(p.getX(),y,p.getZ()),Architecture.state((p.getX()+p.getZ())%11==0?"jade_bricks":"white_jade"));
            for(int y=63;y<=66;y++)out.put(new BlockPos(p.getX(),y,p.getZ()),Blocks.AIR.defaultBlockState());
            for(int y=floor+1;y<=62;y++)out.put(new BlockPos(p.getX(),y,p.getZ()),Blocks.WATER.defaultBlockState());
            if(dist>=3&&Math.floorMod(p.getX()*17+p.getZ()*11,71)==0)out.put(p.above(),Architecture.state(Math.floorMod(p.getX()+p.getZ(),3)==0?"lotus_lamp":"lotus"));
            for(Direction d:Direction.Plane.HORIZONTAL){var land=p.relative(d);if(POOL.contains(land))continue;int by=deck(land.getX(),land.getZ());if(by>66)continue;
                for(int y=floor-1;y<=by;y++)out.put(new BlockPos(land.getX(),y,land.getZ()),Architecture.state(y==by?"white_jade":"jade_bricks"));
                BlockPos rail=new BlockPos(land.getX(),by+1,land.getZ());
                if(!openings.contains(land))out.put(rail,Architecture.state("jade_railing").setValue(FenceBlock.NORTH,d.getAxis()==Direction.Axis.X).setValue(FenceBlock.SOUTH,d.getAxis()==Direction.Axis.X).setValue(FenceBlock.EAST,d.getAxis()==Direction.Axis.Z).setValue(FenceBlock.WEST,d.getAxis()==Direction.Axis.Z));
            }
        }
        for(var e:EXITS){Direction toward=e.outward().getOpposite(),side=toward.getClockWise();for(int k=0;k<2;k++){
            BlockPos land=new BlockPos(e.x(),e.bankY(),e.z()).relative(toward).relative(side,k);
            out.put(land,Architecture.state("jade_stairs").setValue(StairBlock.FACING,toward));for(int h=1;h<=3;h++)out.put(land.above(h),Blocks.AIR.defaultBlockState());
            for(int i=0;i<4;i++){int x=e.x()+e.outward().getStepX()*i+side.getStepX()*k,z=e.z()+e.outward().getStepZ()*i+side.getStepZ()*k,y=e.bankY()-1-i;
                out.put(new BlockPos(x,y-1,z),Architecture.state("jade_bricks"));out.put(new BlockPos(x,y,z),Architecture.state("jade_stairs").setValue(StairBlock.FACING,toward).setValue(StairBlock.WATERLOGGED,y<=62));
                for(int h=1;h<=3;h++)out.put(new BlockPos(x,y+h,z),y+h<=62?Blocks.WATER.defaultBlockState():Blocks.AIR.defaultBlockState());
            }
        }
            BlockPos light=new BlockPos(e.x(),e.bankY()+1,e.z()).relative(toward).relative(side,-1);out.put(light,Architecture.state("garden_lamp"));
        }
        for(var b:CityPlan.BUILDINGS)if(b.id().startsWith("guild"))for(int dx=-b.w()-1;dx<=b.w()+1;dx++)for(int dz=-b.d()-1;dz<=b.d()+2;dz++)if(Math.abs(dx)==b.w()+1||dz==-b.d()-1||dz==b.d()+2){
            int x=b.x()+dx,z=b.z()+dz;for(int y=59;y<b.y();y++)out.put(new BlockPos(x,y,z),Architecture.state(y==b.y()-1?"white_jade":"jade_bricks"));
        }
        // A separate three-wide promenade keeps the bank and its railing off shop walls.
        for(int z=170;z<=269;z++){int y=promenadeY(z);for(int x=22;x<=25;x++){
            for(int yy=Math.min(Terrain.ground(x,z),59);yy<y;yy++)out.put(new BlockPos(x,yy,z),Architecture.state("jade_bricks"));
            BlockState deck=Architecture.state("white_jade");
            if(x==24&&Math.floorMod(z-174,8)==0)deck=Architecture.state("jade_light");
            if(x==25&&openings.contains(new BlockPos(x,62,z)))deck=Architecture.state("jade_stairs").setValue(StairBlock.FACING,Direction.WEST);
            if(x<25&&(promenadeY(z-1)>y||promenadeY(z+1)>y))deck=Architecture.state("jade_stairs").setValue(StairBlock.FACING,promenadeY(z-1)>y?Direction.NORTH:Direction.SOUTH);
            out.put(new BlockPos(x,y,z),deck);for(int h=1;h<=3;h++)out.put(new BlockPos(x,y+h,z),Blocks.AIR.defaultBlockState());
            if(x==25&&!openings.contains(new BlockPos(x,62,z)))out.put(new BlockPos(x,y+1,z),Architecture.state("jade_railing").setValue(FenceBlock.NORTH,true).setValue(FenceBlock.SOUTH,true));
        }}
        plan=Collections.unmodifiableMap(out);return plan;
    }
    public static void paint(CityPlan.Sink s,int cx,int cz){if(cx<1||cx>7||cz<10||cz>17)return;for(var e:plan().entrySet())if(e.getKey().getX()>>4==cx&&e.getKey().getZ()>>4==cz)s.block(e.getKey().getX(),e.getKey().getY(),e.getKey().getZ(),e.getValue());}
}
