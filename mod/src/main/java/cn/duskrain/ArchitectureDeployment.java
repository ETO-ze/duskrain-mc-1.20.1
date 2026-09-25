package cn.duskrain;

import java.nio.file.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

/** A dedicated-server administrator must explicitly stage the request after a cold backup. */
public final class ArchitectureDeployment {
    private static Path request;
    public static void start(MinecraftServer server){
        request=null;if(!server.isDedicatedServer())return;
        Path file=server.getServerDirectory().toPath().resolve("duskrain-architecture-v3.request");
        if(!Files.exists(file))return;
        try{
            if(!Files.readString(file).trim().equals("all"))throw new IllegalArgumentException("Expected explicit all request");
            Path progress=server.getWorldPath(LevelResource.ROOT).resolve("duskrain-architecture-v3/progress.json");
            boolean resume=false;
            if(Files.exists(progress)){
                var saved=com.google.gson.JsonParser.parseString(Files.readString(progress)).getAsJsonObject();
                resume=!saved.get("complete").getAsBoolean();
                if(resume&&saved.get("rollback").getAsBoolean())throw new IllegalStateException("An unfinished rollback requires manual review");
            }
            ArchitectureRepair.start(server,resume?"resume":"all");request=file;
            DuskRain.LOG.info("DEPLOYMENT architecture-v3 explicitly requested; resume={}",resume);
        }catch(Exception ex){DuskRain.LOG.error("DEPLOYMENT architecture-v3 request paused for manual review",ex);}
    }
    public static void completed(MinecraftServer server){
        if(request==null)return;
        try{
            Files.move(request,request.resolveSibling("duskrain-architecture-v3.applied"),StandardCopyOption.REPLACE_EXISTING);
            request=null;DuskRain.LOG.info("DEPLOYMENT architecture-v3 applied; future startup will not rebuild");
        }catch(Exception ex){DuskRain.LOG.error("Cannot acknowledge architecture deployment",ex);}
    }
}
