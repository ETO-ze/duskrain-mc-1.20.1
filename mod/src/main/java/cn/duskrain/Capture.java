package cn.duskrain;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.network.chat.Component;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.nio.file.*;

/** Captures real framebuffer images at ten frames per simulated second. */
public final class Capture {
    static boolean recording;static int ticks,frame;static File output;
    public static void start(boolean on){Minecraft mc=Minecraft.getInstance();if(on&&!recording){String stamp=DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(LocalDateTime.now());output=new File(mc.gameDirectory,"duskrain-recordings/"+stamp);new File(output,"screenshots").mkdirs();frame=0;ticks=0;recording=true;message("开始录制真实游戏帧："+output);}
        else if(!on&&recording){recording=false;try{Files.writeString(output.toPath().resolve("capture.txt"),"DuskRain real Minecraft framebuffer capture\nframes="+frame+"\nfps=10\nFrames sampled every two client ticks. Encode using tools/encode-video.ps1.\n");}catch(Exception ignored){}message("录制结束，共 "+frame+" 帧："+output);}}
    static void message(String s){var p=Minecraft.getInstance().player;if(p!=null)p.sendSystemMessage(Component.literal("[DuskRain 摄影] "+s));}
    public static void tick(){if(!recording)return;if(++ticks%2!=0)return;Minecraft mc=Minecraft.getInstance();Screenshot.grab(output,String.format("frame-%06d.png",frame++),mc.getMainRenderTarget(),c->{});if(frame>=1800)start(false);}
}
