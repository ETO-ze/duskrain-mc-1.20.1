package cn.duskrain;

import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;

/** Opt-in frame interval measurement, including stalls; ten-second warm-up before samples. */
public final class FrameProbe {
    static long warmup,end,last;static String label;static final List<Double> samples=new ArrayList<>();static int menus,unfocused;
    public static void start(String name,int seconds){label=name.replaceAll("[^a-zA-Z0-9_-]","_");samples.clear();last=0;menus=unfocused=0;warmup=System.nanoTime()+10_000_000_000L;end=warmup+Math.max(5,Math.min(120,seconds))*1_000_000_000L;}
    public static void frame(){if(end==0)return;long now=System.nanoTime();if(now<warmup){last=now;return;}if(last==0){last=now;return;}
        samples.add((now-last)/1e6);last=now;var mc=Minecraft.getInstance();if(mc.screen!=null)menus++;if(!mc.isWindowActive())unfocused++;if(now<end)return;end=0;
        try{var sorted=samples.stream().sorted().toList();double sum=samples.stream().mapToDouble(Double::doubleValue).sum();Map<String,Object> data=new LinkedHashMap<>();
            data.put("time",Instant.now().toString());data.put("scene",label);data.put("gpu",GL11.glGetString(GL11.GL_RENDERER));data.put("driver",GL11.glGetString(GL11.GL_VERSION));data.put("visual",VisualProfiles.diagnostic());
            data.put("width",mc.getWindow().getWidth());data.put("height",mc.getWindow().getHeight());data.put("renderDistance",mc.options.renderDistance().get());data.put("fpsLimit",mc.options.framerateLimit().get());data.put("vsync",mc.options.enableVsync().get());data.put("warmupSeconds",10);data.put("durationSeconds",sum/1000);data.put("frames",samples.size());data.put("meanFps",samples.size()*1000/sum);data.put("p50Ms",percentile(sorted,.50));data.put("p95Ms",percentile(sorted,.95));data.put("maxMs",sorted.get(sorted.size()-1));data.put("framesWithMenu",menus);data.put("unfocusedFrames",unfocused);data.put("heapUsedMiB",(Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory())/1048576);data.put("frameIntervalsMs",samples);
            Path folder=mc.gameDirectory.toPath().resolve("duskrain-benchmarks");Files.createDirectories(folder);Path file=folder.resolve(System.currentTimeMillis()+"-"+label+".json");Files.writeString(file,Rules.JSON.toJson(data));VisualProfiles.result="帧率记录已保存到 duskrain-benchmarks 文件夹。";DuskRain.LOG.info("Frame probe saved: {}",file);
        }catch(Exception e){DuskRain.LOG.error("Frame measurement failed",e);}
    }
    static double percentile(List<Double> s,double q){return s.get(Math.max(0,(int)Math.ceil(q*s.size())-1));}
}
