package cn.duskrain;

import net.minecraft.client.Minecraft;
import java.nio.file.*;
import java.util.*;

/** Explicit opt-in native player input measurements in an isolated QA world. */
public final class DirectorProbe {
    static String name;static int elapsed,total;static boolean jump,twice,sprint;static net.minecraft.world.phys.Vec3 last;
    static final List<Map<String,Object>> samples=new ArrayList<>();
    static void start(String args){String[] a=args.split(" ");name=a[0];total=a.length>1?Integer.parseInt(a[1]):100;total=Math.max(50,Math.min(200,total));jump=name.contains("jump");twice=name.contains("double");sprint=name.contains("sprint");elapsed=0;samples.clear();last=Minecraft.getInstance().player.position();}
    static void tick(){if(name==null)return;var mc=Minecraft.getInstance();var p=mc.player;if(p==null||mc.screen!=null){finish(true);return;}var v=p.position();samples.add(Map.of("tick",elapsed,"x",v.x,"y",v.y,"z",v.z,"distance",v.subtract(last).horizontalDistance(),"ground",p.onGround(),"speedAttribute",p.getSpeed(),"sprinting",p.isSprinting(),"velocity",List.of(p.getDeltaMovement().x,p.getDeltaMovement().y,p.getDeltaMovement().z)));last=v;
        mc.options.keyUp.setDown(!name.contains("release")||elapsed<40);mc.options.keySprint.setDown(sprint);mc.options.keyJump.setDown((jump||twice)&&elapsed==30||twice&&elapsed==38);
        if(++elapsed>=total)finish(false);
    }
    static void finish(boolean aborted){var mc=Minecraft.getInstance();mc.options.keyUp.setDown(false);mc.options.keySprint.setDown(false);mc.options.keyJump.setDown(false);try{Files.writeString(mc.gameDirectory.toPath().resolve("motion-"+name+".json"),Rules.JSON.toJson(Map.of("name",name,"aborted",aborted,"samples",samples)));}catch(Exception ex){DuskRain.LOG.error("Motion probe",ex);}name=null;}
    static void book(){var mc=Minecraft.getInstance();var pages=Onboarding.book().getTag().getList("pages",8);List<Object> results=new ArrayList<>();boolean passed=true;for(int i=0;i<pages.size();i++){var component=net.minecraft.network.chat.Component.Serializer.fromJson(pages.getString(i));int lines=mc.font.split(component,114).size();boolean fits=lines<=14;passed&=fits;results.add(Map.of("page",i+1,"lines",lines,"fits",fits));}try{Files.writeString(mc.gameDirectory.toPath().resolve("native-guide-lines.json"),Rules.JSON.toJson(Map.of("passed",passed,"pages",results)));}catch(Exception ex){DuskRain.LOG.error("Guide probe",ex);}}
}
