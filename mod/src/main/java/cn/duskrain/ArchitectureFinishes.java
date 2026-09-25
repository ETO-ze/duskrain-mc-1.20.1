package cn.duskrain;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
/** Supports for registered public fixtures, generated after paths and the water garden. */
public final class ArchitectureFinishes {
 private static Set<BlockPos> bases;
 private static final Set<BlockPos> lamps=new LinkedHashSet<>();
 static boolean insideRoom(int x,int y,int z){return CityPlan.BUILDINGS.stream().anyMatch(b->Math.abs(x-b.x())<b.w()&&Math.abs(z-b.z())<b.d()&&y>=b.y());}
 static synchronized Set<BlockPos> bases(){if(bases!=null)return bases;Set<BlockPos> found=new LinkedHashSet<>();
  CityPlan.Sink collect=new CityPlan.Sink(){public void block(int x,int y,int z,BlockState s){}public void fixture(int x,int y,int z){found.add(new BlockPos(x,y,z));lamps.add(new BlockPos(x,y,z));}public void entity(CompoundTag n){if(n.getString("id").equals("minecraft:sign")&&!insideRoom(n.getInt("x"),n.getInt("y"),n.getInt("z")))found.add(new BlockPos(n.getInt("x"),n.getInt("y")-1,n.getInt("z")));}};
  for(var b:CityPlan.BUILDINGS){CityPlan.building(collect,b,3);CityPlan.building(collect,b,4);}for(var b:Landscape.LANDMARKS)Landscape.landmark(collect,b,4);
  for(var b:Landscape.lights()){var p=new BlockPos(b.x(),b.y(),b.z());found.add(p);lamps.add(p);}
  for(int cx=-2;cx<=1;cx++)for(int cz=10;cz<=17;cz++)PlayerMarket.paint(collect,cx,cz,3);
  bases=Collections.unmodifiableSet(found);return bases;
 }
 public static void paint(CityPlan.Sink s,int cx,int cz){for(var p:bases())if(p.getX()>>4==cx&&p.getZ()>>4==cz){
  int bottom=Terrain.ground(p.getX(),p.getZ()),sky=Terrain.skyTop(p.getX(),p.getZ());if(sky<p.getY())bottom=Math.max(bottom,sky);
  if(lamps.contains(p))Architecture.lamp(s,p.getX(),p.getY(),p.getZ());
  for(int y=Math.max(bottom+1,p.getY()-24);y<p.getY();y++)s.block(p.getX(),y,p.getZ(),Architecture.state(y>=p.getY()-2?"jade_bricks":"grey_bricks"));
 }}
}
