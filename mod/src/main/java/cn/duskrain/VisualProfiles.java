package cn.duskrain;

import com.google.gson.Gson;
import net.minecraft.client.Minecraft;
import net.minecraft.client.CloudStatus;
import net.minecraftforge.fml.ModList;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Client-only settings bridge for the pinned Oculus 1.8.0 API. No server dependency. */
public final class VisualProfiles {
    public record Preset(String id, String title, String hardware, String resolution, String detail,
                         int renderDistance, int maxFps, Map<String,String> shader) {}
    public record Definition(String pack, Preset[] presets) {}
    public static final Definition DEFINITION=readDefinition();
    public static String result="三档均内置光影；可随时关闭或恢复上次设置。";
    private static final String STATE="config/duskrain-visual.json";
    private static String selection;
    private static final String[] FILES={"options.txt","config/oculus.properties",
        "shaderpacks/ComplementaryReimagined_r5.3.zip.txt",STATE};
    static Definition readDefinition(){
        try(var in=VisualProfiles.class.getResourceAsStream("/assets/duskrain/visual-profiles.json")){
            return new Gson().fromJson(new InputStreamReader(Objects.requireNonNull(in),StandardCharsets.UTF_8),Definition.class);
        }catch(Exception e){throw new IllegalStateException("DuskRain visual presets unavailable",e);}
    }
    public static boolean available(){return ModList.get().isLoaded("oculus")&&Files.isRegularFile(root().resolve("shaderpacks/"+DEFINITION.pack));}
    public static String selected(){if(selection==null){try{selection=new Gson().fromJson(Files.readString(root().resolve(STATE)),Map.class).get("profile").toString();}catch(Exception e){selection="custom";}}return selection;}
    public static Path root(){return Minecraft.getInstance().gameDirectory.toPath();}
    static Class<?> iris()throws Exception{return Class.forName("net.irisshaders.iris.Iris");}
    static Object call(String name)throws Exception{return iris().getMethod(name).invoke(null);}
    static void enabled(boolean enabled)throws Exception{
        Object config=call("getIrisConfig");
        config.getClass().getMethod("setShaderPackName",String.class).invoke(config,DEFINITION.pack);
        config.getClass().getMethod("setShadersEnabled",boolean.class).invoke(config,enabled);
        config.getClass().getMethod("save").invoke(config);
    }
    public static Map<String,Object> diagnostic(){
        Map<String,Object> m=new LinkedHashMap<>();m.put("profile",selected());
        try{m.put("pack",call("getCurrentPackName"));m.put("inUse",call("isPackInUseQuick"));m.put("fallback",call("isFallback"));m.put("error",call("getStoredError").toString());}catch(Exception e){m.put("oculus","unavailable");}
        return m;
    }
    static void backup()throws IOException{
        Minecraft.getInstance().options.save();
        Path folder=root().resolve("duskrain-visual-backups/"+System.currentTimeMillis());Files.createDirectories(folder);
        List<String> present=new ArrayList<>();
        for(String f:FILES){Path p=root().resolve(f);if(Files.isRegularFile(p)){Path b=folder.resolve(f);Files.createDirectories(b.getParent());Files.copy(p,b);present.add(f);}}
        Files.write(folder.resolve("present.txt"),present);
        Files.createDirectories(root().resolve("config"));Files.writeString(root().resolve("config/duskrain-visual-backup.txt"),folder.getFileName().toString());
    }
    static void state(String id)throws IOException{Files.writeString(root().resolve(STATE),new Gson().toJson(Map.of("profile",id,"pack",DEFINITION.pack)));selection=id;}
    public static boolean apply(String id){
        Preset p=Arrays.stream(DEFINITION.presets).filter(v->v.id.equals(id)).findFirst().orElse(null);
        if(!id.equals("off")&&p==null){result="未知画质档位。";return false;}
        if(!available()){result="请安装客户端包内的 Oculus 和 Complementary 光影。";return false;}
        boolean backed=false;
        try{
            backup();backed=true;
            if(p!=null){
                // Merge only our named options. Preserve the player's unrelated shader customization.
                @SuppressWarnings("unchecked") var queue=(Map<String,String>)call("getShaderPackOptionQueue");
                queue.clear();queue.putAll(p.shader);
            }
            enabled(p!=null);call("reload");
            if(p!=null&&(!(Boolean)call("isPackInUseQuick")||(Boolean)call("isFallback")||((Optional<?>)call("getStoredError")).isPresent()))throw new IOException("Shader compilation failed");
            var mc=Minecraft.getInstance();
            if(p!=null){mc.options.renderDistance().set(p.renderDistance);mc.options.framerateLimit().set(p.maxFps);mc.options.cloudStatus().set(CloudStatus.OFF);mc.options.simulationDistance().set(6);mc.options.save();}
            state(id);result=p==null?"已关闭光影；可重新选择任意档位。":"已应用「"+p.title+"」并保存。";
            return true;
        }catch(Exception e){
            DuskRain.LOG.error("Applying visual profile {} failed",id,e);
            boolean restored=backed&&restore();
            result=restored?"光影加载失败，已恢复上次设置；详情见日志。":"光影加载失败，请关闭光影或用启动脚本恢复。";
            return false;
        }
    }
    public static boolean restore(){
        try{
            String name=Files.readString(root().resolve("config/duskrain-visual-backup.txt")).trim();
            if(!name.matches("[0-9]+"))throw new IOException("Invalid backup");
            Path folder=root().resolve("duskrain-visual-backups/"+name);
            Set<String> present=new HashSet<>(Files.readAllLines(folder.resolve("present.txt")));
            for(String f:FILES){Path target=root().resolve(f);if(present.contains(f)){Files.createDirectories(target.getParent());Files.copy(folder.resolve(f),target,StandardCopyOption.REPLACE_EXISTING);}else Files.deleteIfExists(target);}
            Minecraft.getInstance().options.load();
            selection=null;
            if(ModList.get().isLoaded("oculus")){call("clearShaderPackOptionQueue");call("reload");}
            result="已恢复上次切换前的设置。";return true;
        }catch(Exception e){DuskRain.LOG.warn("Visual settings restore failed",e);result="没有可恢复的设置，或恢复失败；详情见日志。";return false;}
    }
}
