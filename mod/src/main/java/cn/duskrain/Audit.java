package cn.duskrain;

import net.minecraft.server.MinecraftServer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import java.nio.file.*;
import java.util.*;

public final class Audit {
    public static void run(MinecraftServer server){
        List<String> errors=new ArrayList<>();Rules.current.validate();
        if(CityPlan.BUILDINGS.size()!=33)errors.add("Expected 30 original buildings plus 3 guild buildings");
        for(var b:CityPlan.BUILDINGS)if(b.y()+b.floors()*8+25>=256)errors.add("Building height: "+b.id());
        if(Content.ARTIFACTS.size()!=18||Content.PILLS.size()!=6)errors.add("Content counts");
        for(var q:Rules.current.main)if(q.type().equals("submit")&&!ForgeRegistries.ITEMS.containsKey(new ResourceLocation(q.target())))errors.add("Missing quest material "+q.target());
        for(var o:Rules.current.shops)if(!ForgeRegistries.ITEMS.containsKey(new ResourceLocation(o.item())))errors.add("Missing shop item "+o.item());
        for(var key:List.of(Gameplay.CITY,Gameplay.TRIAL,Gameplay.DEMO))if(server.getLevel(key)==null)errors.add("Missing dimension "+key.location());
        try{Path out=server.getServerDirectory().toPath().resolve("duskrain-audit.json");Map<String,Object> report=new LinkedHashMap<>();report.put("server","DuskRain");report.put("group","205255670");report.put("buildings",CityPlan.BUILDINGS.size());report.put("artifacts",Content.ARTIFACTS.size());report.put("pills",Content.PILLS.size());report.put("mainQuests",Rules.current.main.size());report.put("dailyTemplates",Rules.current.daily.size());report.put("errors",errors);report.put("passed",errors.isEmpty());Files.writeString(out,Rules.JSON.toJson(report));DuskRain.LOG.info("DUSKRAIN_AUDIT {}: {}",errors.isEmpty()?"PASS":"FAIL",out);}catch(Exception e){throw new RuntimeException(e);}
    }
}
