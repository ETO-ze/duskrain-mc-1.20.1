package cn.duskrain;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.block.*;
import net.minecraftforge.gametest.*;
@GameTestHolder(DuskRain.ID) @PrefixGameTestTemplate(false)
public final class CourtGardenTests {
 @GameTest(template="empty",timeoutTicks=400) public static void all_shop_slots_have_floor_and_support(GameTestHelper h){
  var plan=CourtGarden.plan();for(var b:PlayerMarket.SITES)for(int dx=-5;dx<=5;dx++)for(int dz=6;dz<=10;dz++){
   int x=b.x()+dx,z=b.z()+dz;if(CourtGarden.fixed(x,z))continue;var s=CourtGarden.walk.get(CourtGarden.key(x,z));h.assertTrue(s!=null,"Unregistered shop slot "+x+","+z);var p=new BlockPos(x,(int)Math.ceil(s.top())-1,z);h.assertTrue(plan.containsKey(p)&&!plan.get(p).isAir(),"Missing floor "+p);h.assertTrue(plan.containsKey(p.below())&&!plan.get(p.below()).isAir(),"Unsupported floor "+p);
  }h.succeed();
 }
 @GameTest(template="empty",timeoutTicks=400) public static void no_decoration_blocks_path_and_half_steps_only(GameTestHelper h){
  CourtGarden.plan();h.assertTrue(CourtGarden.conflicts.isEmpty(),"Design conflicts "+CourtGarden.conflicts);for(var s:CourtGarden.walk.values())for(var d:new Direction[]{Direction.EAST,Direction.SOUTH}){var next=CourtGarden.walk.get(CourtGarden.key(s.x()+d.getStepX(),s.z()+d.getStepZ()));if(next!=null&&next.zone().equals(s.zone()))h.assertTrue(Math.abs(s.top()-next.top())<=.51,"Abrupt surface "+s+" -> "+next);}h.succeed();
 }
 @GameTest(template="empty",timeoutTicks=400) public static void occupied_walls_and_water_are_reserved(GameTestHelper h){
  var plan=CourtGarden.plan();for(var b:PlayerMarket.SITES)for(int x=b.x()-5;x<=b.x()+5;x++)for(int z=b.z()-5;z<=b.z()+5;z++)for(int y=b.y();y<=b.y()+5;y++)h.assertTrue(!plan.containsKey(new BlockPos(x,y,z)),"Occupied shop facade or inventory must not be overwritten");
  for(var p:WaterCourt.POOL)h.assertTrue(!plan.containsKey(p),"Source water reserved "+p);h.assertTrue(!CourtGarden.natural(Blocks.CHEST.defaultBlockState())&&!CourtGarden.natural(Blocks.DIAMOND_BLOCK.defaultBlockState()),"Player containers and replacements cannot be treated as terrain");h.succeed();
 }
 @GameTest(template="empty",timeoutTicks=400) public static void shoreline_has_solid_foundations_outside_walk_mask(GameTestHelper h){
  var plan=CourtGarden.plan();h.assertTrue(CourtGarden.shoreLand.size()>1500,"Missing dry shoreline coverage");
  for(var e:CourtGarden.shoreLand.entrySet()){int x=net.minecraft.world.level.ChunkPos.getX(e.getKey()),z=net.minecraft.world.level.ChunkPos.getZ(e.getKey());
   for(int y=59;y<=e.getValue();y++){var p=new BlockPos(x,y,z);h.assertTrue(plan.containsKey(p)&&!plan.get(p).isAir()&&plan.get(p).getFluidState().isEmpty(),"Shoreline cavity "+p);}
  }h.succeed();
 }
}
