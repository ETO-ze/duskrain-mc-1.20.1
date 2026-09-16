package cn.duskrain;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import java.nio.*;
import java.nio.file.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/** Explicit local Director-only real framebuffer recorder; no desktop or microphone capture. */
public final class Cinema {
    record Frame(int index,byte[] rgba){}
    static volatile boolean recording,encoding;
    static ArrayBlockingQueue<Frame> queue;
    static long start,next;static int width,height,frames,skipped;static Path output;
    static double[] path;static long pathStart;
    public static void command(String command)throws Exception{
        if(!Boolean.getBoolean("duskrain.director"))return;
        if(command.equals("stop")){stop();return;}
        if(command.equals("off")){path=null;return;}
        if(command.startsWith("path ")){String[] a=command.substring(5).split(" ");if(a.length!=11)throw new IllegalArgumentException("path needs two xyz/yaw/pitch points and seconds");path=new double[11];for(int i=0;i<11;i++)path[i]=Double.parseDouble(a[i]);pathStart=System.nanoTime();return;}
        if(!command.startsWith("record "))return;
        String name=command.substring(7);if(!name.matches("[a-zA-Z0-9_-]{1,64}")||recording||encoding)throw new IllegalArgumentException("Busy or invalid clip name");
        String ffmpeg=System.getProperty("duskrain.ffmpeg","");if(!Files.isRegularFile(Path.of(ffmpeg)))throw new IllegalArgumentException("Set local DUSKRAIN_FFMPEG for video capture");
        var mc=Minecraft.getInstance();width=mc.getMainRenderTarget().width;height=mc.getMainRenderTarget().height;
        output=mc.gameDirectory.toPath().resolve("duskrain-films/"+name+".mp4");Files.createDirectories(output.getParent());
        var proc=new ProcessBuilder(ffmpeg,"-hide_banner","-y","-f","rawvideo","-pixel_format","rgba","-video_size",width+"x"+height,"-framerate","30","-i","pipe:0","-an","-c:v","libx264","-preset","veryfast","-crf","18","-pix_fmt","yuv420p","-movflags","+faststart",output.toAbsolutePath().toString()).redirectError(output.resolveSibling(name+"-encode.log").toFile()).start();
        queue=new ArrayBlockingQueue<>(4);frames=skipped=0;start=next=System.nanoTime();recording=encoding=true;
        new Thread(()->{int written=0;byte[] previous=null;try(var stream=new BufferedOutputStream(proc.getOutputStream(),width*height*4)){
            while(recording||!queue.isEmpty()){Frame frame=queue.poll(100,TimeUnit.MILLISECONDS);if(frame==null)continue;while(written<frame.index&&previous!=null){stream.write(previous);written++;}stream.write(frame.rgba);previous=frame.rgba;written++;}
            stream.flush();
        }catch(Exception e){DuskRain.LOG.error("Cinema encode",e);recording=false;}finally{try{int exit=proc.waitFor();Files.writeString(output.resolveSibling(name+".json"),Rules.JSON.toJson(Map.of("source","Actual Minecraft framebuffer","fps",30,"width",width,"height",height,"frames",written,"uniqueFrames",frames,"queueSkips",skipped,"encoderExit",exit,"audio","none")));}catch(Exception e){DuskRain.LOG.error("Cinema manifest",e);}encoding=false;}},"DuskRain-video-writer").start();
    }
    public static void camera(){
        if(path==null)return;var p=Minecraft.getInstance().player;if(p==null||!p.isSpectator()){path=null;return;}
        double t=Math.min(1,(System.nanoTime()-pathStart)/1e9/path[10]);double u=t*t*(3-2*t);
        p.setPos(path[0]+(path[5]-path[0])*u,path[1]+(path[6]-path[1])*u,path[2]+(path[7]-path[2])*u);
        p.setYRot((float)(path[3]+(path[8]-path[3])*u));p.setXRot((float)(path[4]+(path[9]-path[4])*u));
        p.xo=p.getX();p.yo=p.getY();p.zo=p.getZ();p.xOld=p.getX();p.yOld=p.getY();p.zOld=p.getZ();p.yRotO=p.getYRot();p.xRotO=p.getXRot();p.setDeltaMovement(0,0,0);
        if(t>=1)path=null;
    }
    public static void frame(){
        if(!recording)return;long now=System.nanoTime();if(now<next)return;int index=(int)((now-start)*30/1_000_000_000L);next=start+(index+1)*1_000_000_000L/30;
        if(queue.remainingCapacity()==0){skipped++;return;}
        var mc=Minecraft.getInstance();if(mc.getMainRenderTarget().width!=width||mc.getMainRenderTarget().height!=height){stop();return;}
        try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){var bytes=ByteBuffer.allocate(width*height*4).order(ByteOrder.LITTLE_ENDIAN);bytes.asIntBuffer().put(image.getPixelsRGBA());if(queue.offer(new Frame(index,bytes.array())))frames++;else skipped++;}
    }
    public static void stop(){recording=false;}
}
