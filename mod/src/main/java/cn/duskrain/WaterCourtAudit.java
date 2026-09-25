package cn.duskrain;
import java.util.*;
import java.nio.file.Files;
import net.minecraft.core.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.*;
public final class WaterCourtAudit {
 public static void run(MinecraftServer server){
  var l=server.getLevel(Gameplay.CITY);var out=new LinkedHashMap<String,Object>();var errors=new ArrayList<String>();var exits=new ArrayList<Map<String,Object>>();var plan=WaterCourt.plan();int rail=0,water=0,floors=0;var effective=new HashMap<BlockPos,net.minecraft.world.level.block.state.BlockState>(plan);for(int cx=1;cx<=7;cx++)for(int cz=10;cz<=17;cz++)ArchitectureFinishes.paint((x,y,z,state)->effective.put(new BlockPos(x,y,z),state),cx,cz);
  effective.putAll(CourtGarden.plan());
  for(var e:effective.entrySet()){
   if(e.getValue().isAir()&&!l.getBlockState(e.getKey()).getCollisionShape(l,e.getKey()).isEmpty())errors.add("head clearance "+e.getKey().toShortString());
   if(e.getKey().getY()<=62&&(e.getValue().is(Architecture.get("white_jade"))||e.getValue().is(Architecture.get("jade_bricks")))){floors++;if(!l.getBlockState(e.getKey()).equals(e.getValue()))errors.add("jade bed "+e.getKey().toShortString());}
   if(e.getValue().is(Architecture.get("jade_railing"))){rail++;if(!l.getBlockState(e.getKey()).is(Architecture.get("jade_railing")))errors.add("railing "+e.getKey().toShortString());}
   if(e.getKey().getY()==62&&e.getValue().is(Blocks.WATER)){water++;if(!l.getFluidState(e.getKey()).isSource())errors.add("waterline "+e.getKey().toShortString());}
  }
  for(var e:WaterCourt.EXITS){var faults=new ArrayList<String>();Direction toward=e.outward().getOpposite(),side=toward.getClockWise();for(int k=0;k<2;k++)for(int i=0;i<4;i++){
   var p=new BlockPos(e.x()+e.outward().getStepX()*i+side.getStepX()*k,e.bankY()-1-i,e.z()+e.outward().getStepZ()*i+side.getStepZ()*k);
   var s=l.getBlockState(p);if(!s.is(Architecture.get("jade_stairs"))||s.getValue(StairBlock.FACING)!=toward)faults.add("stair "+p.toShortString());
   for(int h=1;h<=2;h++)if(!l.getBlockState(p.above(h)).getCollisionShape(l,p.above(h)).isEmpty())faults.add("headroom "+p.above(h).toShortString());
  }
   for(int k=0;k<2;k++){var p=new BlockPos(e.x(),e.bankY()+1,e.z()).relative(toward).relative(side,k);var landing=l.getBlockState(p.below());if(!landing.is(Architecture.get("jade_stairs"))||landing.getValue(StairBlock.FACING)!=toward)faults.add("landing step "+p.toShortString());if(Double.isNaN(WalkAudit.surface(l,p.getX(),p.getZ(),p.getY())))faults.add("landing "+p.toShortString());}
   exits.add(Map.of("x",e.x(),"z",e.z(),"bankY",e.bankY(),"outward",e.outward().getName(),"passed",faults.isEmpty(),"issues",faults));errors.addAll(faults);
  }
  out.put("floorSamples",floors);out.put("waterSamples",water);out.put("railSamples",rail);out.put("exits",exits);out.put("issues",errors);out.put("passed",errors.isEmpty());out.put("method","Actual saved source water, continuous two-wide stairs, headroom and dry bank landings. Native swimming tested separately.");
  try{Files.writeString(server.getServerDirectory().toPath().resolve("duskrain-watercourt-audit.json"),Rules.JSON.toJson(out));}catch(Exception e){throw new IllegalStateException(e);}DuskRain.LOG.info("WATERCOURT_AUDIT exits={} issues={}",exits.size(),errors.size());
 }
}
