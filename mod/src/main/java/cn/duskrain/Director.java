package cn.duskrain;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import java.nio.file.*;
import java.util.*;

/** Opt-in local integrated-server QA harness; never active on production servers. */
public final class Director {
    static boolean attempted;static long seen;static int ticks,walkTicks;static net.minecraft.world.phys.Vec3 walkStart;static boolean walkAborted;
    public static void tick(){
        if(!Boolean.getBoolean("duskrain.director"))return;Minecraft mc=Minecraft.getInstance();ticks++;DirectorProbe.tick();
        if(walkTicks>0&&mc.player!=null){
            if(mc.screen!=null){walkAborted=true;walkTicks=1;}
            mc.options.keyUp.setDown(!walkAborted);
            if(--walkTicks==0){mc.options.keyUp.setDown(false);try{var p=mc.player;Files.writeString(mc.gameDirectory.toPath().resolve("duskrain-walk-probe.json"),Rules.JSON.toJson(Map.of("start",List.of(walkStart.x,walkStart.y,walkStart.z),"end",List.of(p.getX(),p.getY(),p.getZ()),"distance",p.position().distanceTo(walkStart),"onGround",p.onGround(),"flying",p.getAbilities().flying,"aborted",walkAborted)));}catch(Exception ex){DuskRain.LOG.error("Walk probe",ex);}}
        }
        if(!attempted&&mc.screen instanceof TitleScreen&&ticks>60){attempted=true;
            mc.options.languageCode="zh_cn";mc.options.pauseOnLostFocus=false;mc.options.save();mc.resizeDisplay();
            if(!System.getProperty("duskrain.join", "").isEmpty()){String address=System.getProperty("duskrain.join");net.minecraft.client.gui.screens.ConnectScreen.startConnecting(mc.screen,mc,net.minecraft.client.multiplayer.resolver.ServerAddress.parseString(address),new net.minecraft.client.multiplayer.ServerData("DuskRain 本地联调",address,false),false);}
            else if(Files.exists(mc.gameDirectory.toPath().resolve("saves/DuskRainRemake/level.dat")))mc.createWorldOpenFlows().loadLevel(mc.screen,"DuskRainRemake");
            else mc.createWorldOpenFlows().createFreshLevel("DuskRainRemake",new LevelSettings("DuskRain · 烟雨仙城",GameType.CREATIVE,false,Difficulty.NORMAL,true,new GameRules(),WorldDataConfiguration.DEFAULT),new WorldOptions(1946372105L,true,false),WorldPresets::createNormalWorldDimensions);
        }
        if(ticks%20!=0)return;
        try{
            Map<String,Object> status=new LinkedHashMap<>();status.put("screen",mc.screen==null?"game":mc.screen.getClass().getSimpleName());status.put("player",mc.player==null?null:mc.player.getName().getString());status.put("frameTime",mc.getFrameTime());status.put("recording",Capture.recording);status.put("frames",Capture.frame);
            if(mc.player!=null){status.put("dimension",mc.player.level().dimension().location().toString());status.put("position",List.of(mc.player.getX(),mc.player.getY(),mc.player.getZ()));}
            var construction=Construction.job;status.put("constructionPhase",construction==null?99:construction.phase);status.put("constructionPaused",Construction.paused);status.put("cinemaRecording",Cinema.recording);status.put("cinemaEncoding",Cinema.encoding);status.put("cinemaFrames",Cinema.frames);status.put("compactHud",HudLayout.compact());status.put("window",List.of(mc.getWindow().getWidth(),mc.getWindow().getHeight()));status.put("gui",List.of(mc.getWindow().getGuiScaledWidth(),mc.getWindow().getGuiScaledHeight()));
            if(mc.screen!=null){status.put("widgets",mc.screen.children().stream().filter(c->c instanceof net.minecraft.client.gui.components.AbstractWidget).map(c->{var w=(net.minecraft.client.gui.components.AbstractWidget)c;return Map.of("label",w.getMessage().getString(),"x",w.getX(),"y",w.getY(),"width",w.getWidth(),"height",w.getHeight(),"active",w.active);}).toList());}
            if(mc.player!=null){status.put("vehicle",mc.player.getVehicle()==null?"none":mc.player.getVehicle().getType().toString());status.put("mana",ClientUI.snapshot==null?0:ClientUI.snapshot.mana());status.put("titles",FlightClient.titles.values());}
            if(mc.screen instanceof MarketChestScreen s){status.put("market",Map.of("money",s.view.money(),"revision",s.view.revision(),"owner",s.view.owner(),"rows",s.view.rows().stream().map(r->Map.of("name",r.sample().getHoverName().getString(),"quantity",r.quantity(),"price",r.price(),"legacy",r.legacy())).toList()));}
            Files.writeString(mc.gameDirectory.toPath().resolve("duskrain-director-status.json"),Rules.JSON.toJson(status));
            Path commandFile=mc.gameDirectory.toPath().resolve("duskrain-director.txt");if(!Files.exists(commandFile))return;long t=Files.getLastModifiedTime(commandFile).toMillis();if(t==seen)return;seen=t;
            for(String command:Files.readAllLines(commandFile)){
                if(command.equals("prepare_film_build")){var id=mc.player.getUUID();var server=mc.getSingleplayerServer();server.execute(()->{var p=server.getPlayerList().getPlayer(id);if(p!=null){Construction.start(p,"palace");Construction.pauseAfterReset=true;Construction.budget=8000;}});}
                else if(command.equals("close"))mc.setScreen(null);
                else if(command.startsWith("cinema "))Cinema.command(command.substring(7));
                else if(command.startsWith("motion "))DirectorProbe.start(command.substring(7));
                else if(command.equals("bookcheck"))DirectorProbe.book();
                else if(command.startsWith("qa ")&&mc.getSingleplayerServer()!=null){String op=command.substring(3);var p=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());mc.getSingleplayerServer().execute(()->V14Audit.run(p,op));}
                else if(command.startsWith("perspective ")){mc.options.setCameraType(net.minecraft.client.CameraType.values()[Integer.parseInt(command.substring(12))]);}
                else if(command.startsWith("look ")){var a=command.substring(5).split(" ");mc.player.setYRot(Float.parseFloat(a[0]));mc.player.setXRot(Float.parseFloat(a[1]));}
                else if(command.startsWith("key ")){var a=command.substring(4).split(" ");var key=switch(a[0]){case "up"->mc.options.keyUp;case "jump"->mc.options.keyJump;case "shift"->mc.options.keyShift;default->mc.options.keySprint;};key.setDown(Boolean.parseBoolean(a[1]));}
                else if(command.startsWith("click ")&&mc.screen!=null){var a=command.substring(6).split(" ");double x=Double.parseDouble(a[0]),y=Double.parseDouble(a[1]);mc.screen.mouseClicked(x,y,0);mc.screen.mouseReleased(x,y,0);}
                else if(command.startsWith("ui_input ")&&mc.screen!=null){var a=command.substring(9).split(" ",2);for(var child:mc.screen.children())if(child instanceof net.minecraft.client.gui.components.EditBox b&&b.getMessage().getString().equals(a[0]))b.setValue(a[1]);}
                else if(command.startsWith("ui_slot ")&&mc.screen instanceof EnchanterScreen s){int i=Integer.parseInt(command.substring(8));s.mouseClicked((s.x+24+i%18*18)*s.scale,(s.y+180+i/18*20)*s.scale,0);}
                else if(command.startsWith("ui_stock ")&&mc.screen instanceof MarketChestScreen s){int i=Integer.parseInt(command.substring(9));s.mouseClicked((s.x+22+i%9*20)*s.scale,(s.y+55+i/9*20)*s.scale,0);}
                else if(command.startsWith("ui_inventory ")&&mc.screen instanceof MarketChestScreen s){int i=Integer.parseInt(command.substring(13));s.mouseClicked((s.x+22+i%9*20)*s.scale,(s.y+143+i/9*20)*s.scale,0);}
                else if(command.startsWith("ui_click ")&&mc.screen!=null){String label=command.substring(9);var widgets=mc.screen.children().stream().filter(c->c instanceof net.minecraft.client.gui.components.AbstractWidget w&&w.getMessage().getString().equals(label)&&w.active).toList();if(widgets.size()!=1)throw new IllegalArgumentException("Expected unique active button: "+label);var w=(net.minecraft.client.gui.components.AbstractWidget)widgets.get(0);float sc=mc.screen instanceof EnchanterScreen s?s.scale:mc.screen instanceof MarketChestScreen s?s.scale:1;mc.screen.mouseClicked((w.getX()+w.getWidth()/2d)*sc,(w.getY()+w.getHeight()/2d)*sc,0);mc.screen.mouseReleased((w.getX()+w.getWidth()/2d)*sc,(w.getY()+w.getHeight()/2d)*sc,0);}
                else if(command.startsWith("tab "))mc.options.keyPlayerList.setDown(Boolean.parseBoolean(command.substring(4)));
                else if(command.startsWith("walk ")){walkStart=mc.player.position();walkAborted=false;walkTicks=Math.max(1,Math.min(400,Integer.parseInt(command.substring(5))));}
                else if(command.equals("visuals"))mc.setScreen(new VisualSettingsScreen(null));
                else if(command.startsWith("quality "))VisualProfiles.apply(command.substring(8));
                else if(command.startsWith("probe "))FrameProbe.start(command.substring(6),30);
                else if(command.startsWith("window ")){String[] wh=command.substring(7).split(" ");if(mc.getWindow().isFullscreen())mc.getWindow().toggleFullScreen();org.lwjgl.glfw.GLFW.glfwRestoreWindow(mc.getWindow().getWindow());org.lwjgl.glfw.GLFW.glfwSetWindowSize(mc.getWindow().getWindow(),Integer.parseInt(wh[0]),Integer.parseInt(wh[1]));}
                else if(command.startsWith("distance ")){mc.options.renderDistance().set(Integer.parseInt(command.substring(9)));mc.options.save();}
                else if(command.startsWith("gui ")){mc.options.guiScale().set(Integer.parseInt(command.substring(4)));mc.resizeDisplay();}
                else if(command.startsWith("fullscreen ")){boolean wanted=Boolean.parseBoolean(command.substring(11));if(mc.getWindow().isFullscreen()!=wanted)mc.getWindow().toggleFullScreen();}
                else if(command.equals("hidehud"))mc.options.hideGui=true;
                else if(command.equals("showhud"))mc.options.hideGui=false;
                else if(command.startsWith("shot "))Screenshot.grab(mc.gameDirectory,command.substring(5)+".png",mc.getMainRenderTarget(),c->{});
                else if(command.startsWith("record "))Capture.start(Boolean.parseBoolean(command.substring(7)));
                else if(command.startsWith("cmd ")){String cmd=command.substring(4);var id=mc.player.getUUID();var server=mc.getSingleplayerServer();if(server==null)mc.player.connection.sendCommand(cmd);else server.execute(()->{var p=server.getPlayerList().getPlayer(id);if(p!=null){if(cmd.startsWith("publish "))server.setUsesAuthentication(false);server.getCommands().performPrefixedCommand(p.createCommandSourceStack().withPermission(4),cmd);}});}
                else if(command.equals("exit")){Capture.start(false);Cinema.stop();mc.stop();}
            }
        }catch(Exception e){DuskRain.LOG.error("Director command error",e);}
    }
}
