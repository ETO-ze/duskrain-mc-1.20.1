package cn.duskrain;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import java.nio.file.Files;
import java.util.*;

/** Checks actual built lamp bases and the twelve new public shop entrances. */
public final class FixtureAudit {
    public static boolean run(MinecraftServer server){var level=server.getLevel(Gameplay.CITY);Set<BlockPos> bases=new HashSet<>();List<String> failures=new ArrayList<>();
        CityPlan.Sink recorder=new CityPlan.Sink(){public void block(int x,int y,int z,net.minecraft.world.level.block.state.BlockState state){}public void fixture(int x,int y,int z){bases.add(new BlockPos(x,y,z));}};
        for(var b:CityPlan.BUILDINGS)CityPlan.building(recorder,b,4);for(var b:Landscape.LANDMARKS)Landscape.landmark(recorder,b,4);
        for(var lamp:Landscape.lights())bases.add(new BlockPos(lamp.x(),lamp.y(),lamp.z()));
        for(BlockPos base:bases){if(level.getBlockState(base.below()).isAir())failures.add("floating lamp "+base);if(level.getBlockState(base).isAir()||level.getBlockState(base.above()).isAir())failures.add("missing lamp base "+base);}
        for(var s:PlayerMarket.SITES){if(!level.getBlockState(s.counter()).is(net.minecraft.world.level.block.Blocks.BARREL))failures.add("missing shop counter "+s.id());for(int z=s.z()+2;z<=s.z()+7;z++)for(int x=s.x()-1;x<=s.x()+1;x++)if(Double.isNaN(WalkAudit.surface(level,x,z,s.y()+1)))failures.add("blocked shop entrance "+s.id()+" at "+x+","+z);}
        var report=Map.of("lampBases",bases.size(),"shopEntrances",PlayerMarket.SITES.size(),"issues",failures,"passed",failures.isEmpty());
        try{Files.writeString(server.getServerDirectory().toPath().resolve("duskrain-fixture-audit.json"),Rules.JSON.toJson(report));}catch(Exception e){DuskRain.LOG.error("Fixture audit",e);return false;}
        DuskRain.LOG.info("FIXTURE_AUDIT lamps={} shops={} issues={}",bases.size(),PlayerMarket.SITES.size(),failures.size());return failures.isEmpty();
    }
}
