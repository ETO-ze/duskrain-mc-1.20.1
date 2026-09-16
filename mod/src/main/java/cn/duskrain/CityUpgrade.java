package cn.duskrain;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
/** Additive migration confined to the newly approved enchanter plot and east-side approach. */
public final class CityUpgrade {
 public static void install(MinecraftServer server){var level=server.getLevel(Gameplay.CITY);if(level==null)return;var b=CityPlan.find("enchanter");
  var blocks=new LinkedHashMap<BlockPos,BlockState>();var tags=new ArrayList<CompoundTag>();CityPlan.Sink sink=new CityPlan.Sink(){public void block(int x,int y,int z,BlockState state){if(x>=65&&x<=107&&z>=-36&&z<=35)blocks.put(new BlockPos(x,y,z),state);}public void entity(CompoundTag n){tags.add(n.copy());}};
  for(int phase=0;phase<6;phase++)CityPlan.building(sink,b,phase);
  for(var route:CityPlan.ROUTES.subList(CityPlan.ROUTES.size()-4,CityPlan.ROUTES.size()))CityPlan.path(sink,0,0,route);
  // Existing chunks retain their old terrain. Foundation generation must inspect those
  // blocks instead of assuming the newly computed height field has already been painted.
  for(int x=b.x()-b.w()-2;x<=b.x()+b.w()+2;x++)for(int z=b.z()-b.d()-2;z<=b.z()+b.d()+3;z++){
   var floor=blocks.get(new BlockPos(x,b.y(),z));if(floor!=null&&!floor.isAir())support(level,sink,x,b.y(),z);
  }
  for(var route:CityPlan.ROUTES.subList(CityPlan.ROUTES.size()-4,CityPlan.ROUTES.size())){int n=Math.max(Math.abs(route.bx()-route.ax()),Math.abs(route.bz()-route.az()));for(int i=0;i<=n;i++)for(int k=-2;k<=2;k++){int x=route.ax()+Integer.signum(route.bx()-route.ax())*i+(route.az()!=route.bz()?k:0),z=route.az()+Integer.signum(route.bz()-route.az())*i+(route.ax()!=route.bx()?k:0);support(level,sink,x,CityPlan.pathY(route,i),z);}}
  Set<Long> chunks=new LinkedHashSet<>();for(var pos:blocks.keySet())chunks.add(net.minecraft.world.level.ChunkPos.asLong(pos.getX()>>4,pos.getZ()>>4));
  for(long key:chunks){var cp=new net.minecraft.world.level.ChunkPos(key);BlockPos marker=new BlockPos(cp.getMinBlockX()+8,3,cp.getMinBlockZ()+8);level.getChunkAt(marker);var be=level.getBlockEntity(marker);if(be!=null&&be.getPersistentData().getInt("DuskRainV14Enchanter")==3)continue;
   for(var e:blocks.entrySet())if((e.getKey().getX()>>4)==cp.x&&(e.getKey().getZ()>>4)==cp.z)level.setBlock(e.getKey(),e.getValue(),2);
   for(var n:tags){BlockPos pos=new BlockPos(n.getInt("x"),n.getInt("y"),n.getInt("z"));if(pos.getX()>>4!=cp.x||pos.getZ()>>4!=cp.z)continue;var tile=level.getBlockEntity(pos);if(tile!=null){tile.load(n);tile.setChanged();}}
   level.setBlock(marker,Blocks.BARREL.defaultBlockState(),2);var stamp=level.getBlockEntity(marker);stamp.getPersistentData().putInt("DuskRainV14Enchanter",3);stamp.setChanged();
  }
 }
 static void support(net.minecraft.server.level.ServerLevel level,CityPlan.Sink sink,int x,int floor,int z){for(int y=floor-1;y>=Math.max(1,floor-32);y--){var p=new BlockPos(x,y,z);var state=level.getBlockState(p);if(state.isCollisionShapeFullBlock(level,p)&&!state.is(net.minecraft.tags.BlockTags.LEAVES)&&!state.is(net.minecraft.tags.BlockTags.LOGS))break;sink.block(x,y,z,y>=floor-3?Blocks.STONE_BRICKS.defaultBlockState():Blocks.STONE.defaultBlockState());}}
}
