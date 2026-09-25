package cn.duskrain;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.shapes.*;
import net.minecraftforge.gametest.*;
@GameTestHolder(DuskRain.ID) @PrefixGameTestTemplate(false)
public final class ArchitectureTests {
 @GameTest(template="empty") public static void identical_pending_repairs_are_safe_to_resume(GameTestHelper h){
  var log=new net.minecraft.nbt.CompoundTag();var edits=new net.minecraft.nbt.ListTag();var edit=new net.minecraft.nbt.CompoundTag();edit.putLong("pos",new BlockPos(23,67,235).asLong());
  edit.put("before",net.minecraft.nbt.NbtUtils.writeBlockState(Blocks.DARK_PRISMARINE.defaultBlockState()));edit.put("after",net.minecraft.nbt.NbtUtils.writeBlockState(Blocks.AIR.defaultBlockState()));edits.add(edit);edits.add(edit.copy());log.put("edits",edits);
  h.assertTrue(ArchitectureRepair.compactJournal(log),"Duplicated unapplied edit is consolidated");h.assertTrue(log.getList("edits",10).size()==1,"One restoration record remains");h.assertTrue(log.getList("edits",10).getCompound(0).equals(edit),"Before and after preserved");
  var invalid=edit.copy();invalid.put("before",net.minecraft.nbt.NbtUtils.writeBlockState(Blocks.DIRT.defaultBlockState()));log.getList("edits",10).add(invalid);boolean rejected=false;try{ArchitectureRepair.compactJournal(log);}catch(IllegalStateException expected){rejected=true;}h.assertTrue(rejected,"Unrelated edits still stop for review");h.succeed();
 }
 @GameTest(template="empty") public static void repair_journal_keeps_original_rollback(GameTestHelper h){
  var log=new net.minecraft.nbt.CompoundTag();var edits=new net.minecraft.nbt.ListTag();var states=List.of(Blocks.STONE.defaultBlockState(),Architecture.state("white_jade"),Architecture.state("jade_stairs"));
  for(int i=0;i<2;i++){var edit=new net.minecraft.nbt.CompoundTag();edit.putLong("pos",1);edit.put("before",net.minecraft.nbt.NbtUtils.writeBlockState(states.get(i)));edit.put("after",net.minecraft.nbt.NbtUtils.writeBlockState(states.get(i+1)));edits.add(edit);}log.put("edits",edits);
  h.assertTrue(ArchitectureRepair.compactJournal(log),"Repeated position must consolidate");var compact=log.getList("edits",10);h.assertTrue(compact.size()==1,"One transaction per position");h.assertTrue(compact.getCompound(0).getCompound("before").equals(net.minecraft.nbt.NbtUtils.writeBlockState(states.get(0))),"Rollback retains original stone");h.assertTrue(compact.getCompound(0).getCompound("after").equals(net.minecraft.nbt.NbtUtils.writeBlockState(states.get(2))),"Replay uses final stair");h.assertTrue(!ArchitectureRepair.compactJournal(log),"Compaction is idempotent");h.succeed();
 }
 @GameTest(template="empty") public static void palette_preserves_direction_and_water(GameTestHelper h){
  var raw=Blocks.QUARTZ_STAIRS.defaultBlockState().setValue(StairBlock.FACING,Direction.EAST).setValue(StairBlock.HALF,Half.TOP).setValue(StairBlock.WATERLOGGED,true);
  var converted=Architecture.material(raw);h.assertTrue(converted.is(Architecture.get("jade_stairs"))&&converted.getValue(StairBlock.FACING)==Direction.EAST&&converted.getValue(StairBlock.HALF)==Half.TOP&&converted.getFluidState().isSource(),"Shape state must survive palette replacement");
  h.assertTrue(Architecture.material(Blocks.CHEST.defaultBlockState()).is(Blocks.CHEST),"Inventory blocks keep identity");
  h.assertTrue(Architecture.material(Blocks.GRASS_BLOCK.defaultBlockState()).is(Blocks.GRASS_BLOCK),"Natural terrain retains identity");h.succeed();
 }
 @GameTest(template="empty") public static void migration_keeps_user_edits(GameTestHelper h){
  var stair=Blocks.QUARTZ_STAIRS.defaultBlockState();h.assertTrue(ArchitectureRepair.matches(stair.setValue(StairBlock.SHAPE,StairsShape.INNER_LEFT),stair),"Neighbour shape is derived");
  h.assertTrue(!ArchitectureRepair.matches(stair.setValue(StairBlock.FACING,Direction.EAST),stair.setValue(StairBlock.FACING,Direction.NORTH)),"Player rotation is preserved");
  h.assertTrue(ArchitectureRepair.matches(Blocks.OAK_FENCE.defaultBlockState().setValue(FenceBlock.NORTH,true),Blocks.OAK_FENCE.defaultBlockState()),"Journal replay accepts neighbour-derived fence connections");
  h.assertTrue(!ArchitectureRepair.matches(Blocks.DIAMOND_BLOCK.defaultBlockState(),Blocks.STONE_BRICKS.defaultBlockState()),"Player replacement is preserved");h.succeed();
 }
 @GameTest(template="empty") public static void railing_blocks_fall_without_full_cube(GameTestHelper h){
  var l=h.getLevel();var p=h.absolutePos(new BlockPos(2,2,2));var rail=Architecture.state("jade_railing");
  var shape=rail.getCollisionShape(l,p);h.assertTrue(shape.max(Direction.Axis.Y)>=1.5,"Railing stops normal walking/jumping");
  var shaft=Architecture.state("lamp_shaft").getCollisionShape(l,p);h.assertTrue(shaft.max(Direction.Axis.X)-shaft.min(Direction.Axis.X)<=.26,"Lamp shaft is at most quarter block wide");h.succeed();
 }
 @GameTest(template="architecture_space") public static void jade_railing_connects_and_disconnects(GameTestHelper h){
  var l=h.getLevel();var center=h.absolutePos(new BlockPos(3,2,3));var rail=Architecture.state("jade_railing");
  l.setBlock(center,rail,3);l.setBlock(center.east(),rail,3);l.setBlock(center.west(),rail,3);
  h.assertTrue(l.getBlockState(center).getValue(FenceBlock.EAST)&&l.getBlockState(center).getValue(FenceBlock.WEST),"Straight jade rails must join in both directions");
  l.setBlock(center.north(),rail,3);h.assertTrue(l.getBlockState(center).getValue(FenceBlock.NORTH),"T junction joins");
  l.setBlock(center.south(),Architecture.state("white_jade"),3);h.assertTrue(l.getBlockState(center).getValue(FenceBlock.SOUTH),"Jade column joins");
  l.setBlock(center.east(),Blocks.AIR.defaultBlockState(),3);h.assertTrue(!l.getBlockState(center).getValue(FenceBlock.EAST),"Removing a rail removes the arm");
  h.assertTrue(l.getBlockState(center.west()).getValue(FenceBlock.EAST),"Neighbour has reciprocal connection");h.succeed();
 }
 @GameTest(template="empty") public static void water_bank_never_cuts_shop_walls(GameTestHelper h){
  var plan=WaterCourt.plan();for(var b:PlayerMarket.SITES)for(int dx=-5;dx<=5;dx++)for(int dz=-5;dz<=5;dz++)if(Math.abs(dx)==5||Math.abs(dz)==5)for(int y=1;y<=5;y++)h.assertTrue(!plan.containsKey(new BlockPos(b.x()+dx,b.y()+y,b.z()+dz)),"Watercourt must not overwrite shop facades");h.succeed();
 }
 @GameTest(template="empty",timeoutTicks=400) public static void every_water_component_has_two_exits(GameTestHelper h){
  WaterCourt.plan();Set<BlockPos> unseen=new HashSet<>(WaterCourt.POOL);int components=0;
  while(!unseen.isEmpty()){var first=unseen.iterator().next();Set<BlockPos> group=new HashSet<>();Deque<BlockPos> q=new ArrayDeque<>();q.add(first);unseen.remove(first);
   while(!q.isEmpty()){var p=q.remove();group.add(p);for(Direction d:Direction.Plane.HORIZONTAL)if(unseen.remove(p.relative(d)))q.add(p.relative(d));}
   long exits=WaterCourt.EXITS.stream().filter(e->group.contains(new BlockPos(e.x(),62,e.z()))).count();h.assertTrue(exits>=2,"Water component "+first+" size="+group.size()+" exits="+exits);components++;
  }h.assertTrue(components>0,"Water retained");h.succeed();
 }
 @GameTest(template="empty",timeoutTicks=400) public static void water_exits_have_continuous_steps(GameTestHelper h){
  var plan=WaterCourt.plan();for(var e:WaterCourt.EXITS){Direction toward=e.outward().getOpposite(),side=toward.getClockWise();for(int k=0;k<2;k++)for(int i=0;i<4;i++){
   var p=new BlockPos(e.x()+e.outward().getStepX()*i+side.getStepX()*k,e.bankY()-1-i,e.z()+e.outward().getStepZ()*i+side.getStepZ()*k);
   var state=plan.get(p);h.assertTrue(state!=null&&state.is(Architecture.get("jade_stairs")),"Missing stair "+p);h.assertTrue(state.getValue(StairBlock.FACING)==toward,"Stair faces land");
   for(int y=1;y<=2;y++){var above=plan.get(p.above(y));h.assertTrue(above==null||above.isAir()||above.is(Blocks.WATER),"No head obstruction on escape flight");}
  }for(int k=0;k<2;k++){var landing=plan.get(new BlockPos(e.x(),e.bankY(),e.z()).relative(toward).relative(side,k));h.assertTrue(landing!=null&&landing.is(Architecture.get("jade_stairs"))&&landing.getValue(StairBlock.FACING)==toward,"Top landing must also be a stair, not a one-block ledge");}}h.succeed();
 }
}
