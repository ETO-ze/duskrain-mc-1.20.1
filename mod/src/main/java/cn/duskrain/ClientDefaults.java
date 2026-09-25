package cn.duskrain;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import java.nio.file.Files;

/** Only resolves known default conflicts on the first launch; later player bindings are preserved. */
@Mod.EventBusSubscriber(modid=DuskRain.ID,value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
public final class ClientDefaults {
    @SubscribeEvent public static void setup(FMLClientSetupEvent event){event.enqueueWork(()->{
        var mc=Minecraft.getInstance();
        if(mc.options.resourcePacks.removeIf(id->id.equals("file/DuskRain-Jade-City.zip"))){mc.options.save();mc.getResourcePackRepository().setSelected(mc.options.resourcePacks);mc.reloadResourcePacks();}
        var marker=mc.gameDirectory.toPath().resolve("config/duskrain-v22-key-defaults.txt");
        if(Files.exists(marker))return;
        boolean changed=false;
        for(var key:mc.options.keyMappings){
            if(key.getName().equals("key.journeymap.toggle_entity_names")&&key.getKey().getValue()==org.lwjgl.glfw.GLFW.GLFW_KEY_G){
                key.setKey(InputConstants.UNKNOWN);changed=true;
            }
        }
        if(changed){KeyMapping.resetMapping();mc.options.save();}
        try{Files.createDirectories(marker.getParent());Files.writeString(marker,"Default JourneyMap G binding cleared; DuskRain G opens the menu.\n");}
        catch(Exception ex){DuskRain.LOG.warn("Cannot save keybinding migration marker",ex);}
    });}
}
