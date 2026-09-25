package cn.duskrain;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.nio.file.*;

@Mod.EventBusSubscriber(modid=DuskRain.ID,value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
public final class ClientHud {
    private static Boolean visible;
    static Path file(){return Minecraft.getInstance().gameDirectory.toPath().resolve("config/duskrain-info-hud.txt");}
    static boolean enabled(){if(visible==null){try{visible=!Files.readString(file()).trim().equals("false");}catch(Exception e){visible=true;}}return visible;}
    public static void toggle(){if(ClientMapHooks.toggle!=null){ClientMapHooks.toggle.run();return;}visible=!enabled();try{Files.createDirectories(file().getParent());Files.writeString(file(),visible.toString());}catch(Exception e){DuskRain.LOG.warn("HUD preference",e);}ClientUI.notice(visible?"信息 HUD 已显示 · F8 隐藏":"信息 HUD 已隐藏 · F8 显示");}
    @SubscribeEvent public static void register(RegisterGuiOverlaysEvent event){event.registerAbove(VanillaGuiOverlay.PLAYER_LIST.id(),"information",(gui,g,partial,w,h)->{
        var mc=Minecraft.getInstance();if(mc.player==null||mc.screen!=null||mc.options.hideGui||mc.options.renderDebug||!enabled()||ClientMapHooks.installed())return;
        var p=mc.player;var info=mc.getConnection()==null?null:mc.getConnection().getPlayerInfo(p.getUUID());String status=mc.getFps()+" FPS · "+(info==null?"—":info.getLatency())+" ms";
        String coords="X "+p.getBlockX()+"   Y "+p.getBlockY()+"   Z "+p.getBlockZ();int tw=Math.max(126,Math.max(mc.font.width(status),mc.font.width(coords))+24);
        boolean compact=HudLayout.compact();if(compact){tw=Math.max(mc.font.width(status),mc.font.width(coords))+14;HudLayout.panel(g,5,5,tw,28);g.drawString(mc.font,status,12,9,0xE8D7AC,true);g.drawString(mc.font,coords,12,21,0xB9C9BD,true);}else{InkStyle.hud(g,6,6,tw,49);g.drawString(mc.font,"行 旅 录",18,13,InkStyle.GOLD,false);g.drawString(mc.font,status,18,26,InkStyle.INK,false);g.drawString(mc.font,coords,18,39,InkStyle.MUTED,false);}
        String title="",hint="";
        if(mc.hitResult instanceof EntityHitResult hit){var target=hit.getEntity();var service=Services.byEntity(target);title=target.getName().getString();if(service!=null)hint="右键交谈 · "+service.site().name();else if(target instanceof LivingEntity mob)hint="生命 "+(int)Math.ceil(mob.getHealth())+" / "+(int)mob.getMaxHealth();}
        else if(mc.hitResult instanceof BlockHitResult hit){var block=mc.level.getBlockState(hit.getBlockPos());if(!block.isAir()){var service=mc.level.dimension().equals(Gameplay.CITY)?Services.byBlock(hit.getBlockPos()):null;if(!compact||service!=null){title=service==null?block.getBlock().getName().getString():service.name();hint=service==null?"DuskRain · 烟雨仙途":"右键打开 · "+service.site().name();}}}
        if(!title.isBlank()&&!net.minecraftforge.fml.ModList.get().isLoaded("jade")){
            int width=Math.min(190,Math.max(mc.font.width(title),mc.font.width(hint))+20),x=(w-width)/2;
            // At very large GUI scales, move the target panel below the left status block.
            int y=x<tw+18?59:7;InkStyle.target(g,x,y,width,36);
            g.drawCenteredString(mc.font,mc.font.plainSubstrByWidth(title,width-20),w/2,y+8,InkStyle.INK);g.drawCenteredString(mc.font,mc.font.plainSubstrByWidth(hint,width-20),w/2,y+21,InkStyle.MUTED);
        }
    });}
}
