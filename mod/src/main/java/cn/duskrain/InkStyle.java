package cn.duskrain;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/** Original textured surfaces; text is rendered separately at the normal GUI scale. */
public final class InkStyle {
    public static final int INK=0xFF243F3E,MUTED=0xFF526E67,GOLD=0xFF9C8049;
    static void paint(GuiGraphics g,String name,int tw,int th,int x,int y,int w,int h){
        g.pose().pushPose();g.pose().translate(x,y,0);g.pose().scale(w/(float)tw,h/(float)th,1);
        g.blit(new ResourceLocation("duskrain","textures/gui/"+name+".png"),0,0,0,0,tw,th,tw,th);g.pose().popPose();
    }
    public static void scroll(GuiGraphics g,int x,int y,int w,int h){paint(g,"ink_scroll",492,648,x,y,w,h);}
    public static void hud(GuiGraphics g,int x,int y,int w,int h){paint(g,"ink_hud",540,150,x,y,w,h);}
    public static void target(GuiGraphics g,int x,int y,int w,int h){paint(g,"ink_target",540,114,x,y,w,h);}
    public static void sheet(GuiGraphics g,int x,int y,int w,int h){paint(g,"ink_sheet",1200,800,x,y,w,h);}
}
