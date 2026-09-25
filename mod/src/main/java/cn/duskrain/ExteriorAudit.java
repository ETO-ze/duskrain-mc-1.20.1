package cn.duskrain;
import java.util.*;
import java.nio.file.*;
import net.minecraft.core.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.phys.AABB;

/** Checks intended walkable AREAS, including holes never present in the block blueprint. */
public final class ExteriorAudit {
 public static void run(MinecraftServer server){
  CourtGarden.plan();var l=server.getLevel(Gameplay.CITY);var holes=new ArrayList<String>();var blocked=new ArrayList<String>();var steps=new ArrayList<String>();var rail=new ArrayList<String>();var unsupported=new ArrayList<String>();int samples=0;
  for(var s:CourtGarden.walk.values()){
   samples++;double top=s.top();var foot=new BlockPos(s.x(),(int)Math.ceil(top)-1,s.z());var shape=l.getBlockState(foot).getCollisionShape(l,foot);
   if(shape.isEmpty()||Math.abs(foot.getY()+shape.max(Direction.Axis.Y)-top)>.01)holes.add(foot.toShortString());
   // Sample the full width, not just centerline; 0.6-wide player needs clear headroom.
   for(double dx:new double[]{.31,.69})for(double dz:new double[]{.31,.69})if(!l.noCollision(new AABB(s.x()+dx-.29,top+.01,s.z()+dz-.29,s.x()+dx+.29,top+1.81,s.z()+dz+.29))){blocked.add(s.x()+","+top+","+s.z());break;}
   for(Direction dir:List.of(Direction.EAST,Direction.SOUTH)){var other=CourtGarden.walk.get(CourtGarden.key(s.x()+dir.getStepX(),s.z()+dir.getStepZ()));if(other!=null&&other.zone().equals(s.zone())&&Math.abs(top-other.top())>.51)steps.add(s.x()+","+s.z()+" -> "+other.x()+","+other.z());}
   for(int y=(int)Math.ceil(top);y<top+2;y++)if(l.getBlockState(new BlockPos(s.x(),y,s.z())).is(Architecture.get("jade_railing")))rail.add(s.x()+","+y+","+s.z());
  }
  // All dry shoreline columns, including planted ground not present in the walk mask.
  for(var e:CourtGarden.shoreLand.entrySet()){
   int x=net.minecraft.world.level.ChunkPos.getX(e.getKey()),z=net.minecraft.world.level.ChunkPos.getZ(e.getKey());
   for(int y=59;y<=e.getValue();y++){var p=new BlockPos(x,y,z);if(l.getBlockState(p).getCollisionShape(l,p).isEmpty())holes.add("shore support "+p.toShortString());}
  }
  for(var p:CourtGarden.fixtures){var st=l.getBlockState(p);if(st.isAir())continue;boolean needs=st.is(Architecture.get("garden_lamp"))||st.getBlock() instanceof net.minecraft.world.level.block.BushBlock||st.getBlock() instanceof net.minecraft.world.level.block.SlabBlock;if(needs&&l.getBlockState(p.below()).getCollisionShape(l,p.below()).isEmpty())unsupported.add(p.toShortString());}
  var result=new LinkedHashMap<String,Object>();result.put("walkableCells",samples);result.put("shoreLandColumns",CourtGarden.shoreLand.size());result.put("groundHoles",holes);result.put("blocked",blocked);result.put("abruptSteps",steps);result.put("railConflicts",rail);result.put("unsupportedDecor",unsupported);result.put("designConflicts",CourtGarden.conflicts);result.put("passed",holes.isEmpty()&&blocked.isEmpty()&&steps.isEmpty()&&rail.isEmpty()&&unsupported.isEmpty()&&CourtGarden.conflicts.isEmpty());
  result.put("method","Actual collision checks across complete registered exterior walkable areas, not only emitted blocks or route centerlines.");
  try{Files.writeString(server.getServerDirectory().toPath().resolve("duskrain-exterior-audit.json"),Rules.JSON.toJson(result));}catch(Exception ex){throw new IllegalStateException(ex);}DuskRain.LOG.info("EXTERIOR_AUDIT cells={} holes={} blocked={} steps={}",samples,holes.size(),blocked.size(),steps.size());
 }
}
