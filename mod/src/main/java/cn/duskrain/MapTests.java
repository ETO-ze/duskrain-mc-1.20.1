package cn.duskrain;

import java.util.*;
import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;

/** Optional full map generation and real chunk collision audit, in an isolated test world. */
@GameTestHolder(DuskRain.ID)
public final class MapTests {
    @GameTestGenerator public static Collection<TestFunction> generate(){
        if(!Boolean.getBoolean("duskrain.mapQA"))return List.of();
        return List.of(new TestFunction("map","duskrain.map_export_and_collision","duskrain:empty",12000,0,true,h->step(h,0,System.nanoTime())));
    }
    static void step(GameTestHelper h,int cursor,long start){
        var level=h.getLevel().getServer().getLevel(Gameplay.CITY);if(level==null){h.fail("City dimension missing");return;}
        for(int i=0;i<2&&cursor<4096;i++,cursor++)level.getChunk(cursor%64-32,cursor/64-32);
        if(cursor%256==0)DuskRain.LOG.info("MAP_EXPORT {}/4096 elapsed={}s",cursor,(System.nanoTime()-start)/1_000_000_000L);
        if(cursor<4096){int next=cursor;h.runAfterDelay(1,()->step(h,next,start));return;}
        level.save(null,true,false);
        DuskRain.LOG.info("MAP_EXPORT_COMPLETE trees={} roadsideLamps={} landmarks={} elapsed={}s",Landscape.trees().size(),Landscape.lights().size(),Landscape.LANDMARKS.size(),(System.nanoTime()-start)/1_000_000_000L);
        boolean walk=WalkAudit.run(level.getServer()),site=SiteAudit.run(level.getServer()),network=RouteNetworkAudit.run(level.getServer()),fixtures=FixtureAudit.run(level.getServer());
        h.assertTrue(walk,"Generated city collision audit failed; see duskrain-walk-audit.json");
        h.assertTrue(site,"Lighting/terrain audit failed; see duskrain-site-audit.json");
        h.assertTrue(network,"Connected routes and turning lanes failed; see duskrain-route-network.json");
        h.assertTrue(fixtures,"Lamp foundations and shop entrances failed; see duskrain-fixture-audit.json");
        h.succeed();
    }
}
