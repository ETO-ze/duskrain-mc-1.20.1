package cn.duskrain;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import java.nio.file.*;

/** Compact by default; F9 restores the full original scroll without dropping its fields. */
public final class HudLayout {
    static Boolean compact;
    public static boolean compact(){if(compact==null){try{compact=!Files.readString(file()).trim().equals("full");}catch(Exception e){compact=true;}}return compact;}
    static Path file(){return Minecraft.getInstance().gameDirectory.toPath().resolve("config/duskrain-hud-layout.txt");}
    public static void toggle(){compact=!compact();try{Files.createDirectories(file().getParent());Files.writeString(file(),compact?"compact":"full");}catch(Exception e){DuskRain.LOG.warn("HUD layout preference",e);}ClientUI.notice(compact?"透明水墨 HUD · 右下技能条同步收卷 · F9 展开":"完整水墨卷轴 · 右下技能条同步展开 · F9 透明");}
    static void panel(GuiGraphics g,int x,int y,int w,int h){
        g.fill(x,y,x+w,y+h,0x70203739);
        int gold=0xAABDAB79;g.fill(x,y,x+9,y+1,gold);g.fill(x,y,x+1,y+7,gold);
        g.fill(x+w-9,y+h-1,x+w,y+h,gold);g.fill(x+w-1,y+h-7,x+w,y+h,gold);
    }
    static void sidebar(GuiGraphics g,int screenW,int screenH,Network.Snapshot r){
        var f=Minecraft.getInstance().font;float scale=Math.min(Math.max(.65f,r.scale()),Math.min(1f,(screenH-30f)/126));
        g.pose().pushPose();g.pose().scale(scale,scale,1);int w=122,h=126,x=(int)(screenW/scale)-w-5,y=Math.max(8,(int)(screenH/scale-h)/2);
        panel(g,x,y,w,h);g.drawString(f,"DuskRain  ·  烟雨",x+7,y+6,0xF0DCAD,true);
        String[] lines={Rules.realm(r.stage())+" · "+Rules.current.schools[r.school()],"修为 "+r.xp()+" / "+(r.needed()==0?"圆满":r.needed()),"灵力 "+r.mana()+" / "+r.manaMax(),"灵石 "+r.money(),r.region(),(r.pvp()?"§cPVP开启":"§a安全")+" · 在线 "+r.online(),"群 205255670"};
        for(int i=0;i<lines.length;i++)g.drawString(f,f.plainSubstrByWidth(lines[i],w-14),x+7,y+23+i*12,0xE4EADB,true);
        g.drawString(f,"F9 展卷  ·  G 菜单",x+7,y+h-12,0xAEBFB2,true);g.pose().popPose();
    }
}
