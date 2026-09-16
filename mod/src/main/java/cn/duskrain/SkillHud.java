package cn.duskrain;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.gui.overlay.ForgeGui;

/** The right corner stays outside the vanilla hotbar, offhand and survival indicators. */
public final class SkillHud {
    public record Layout(int x,int y,int columns,int width,int height) {}
    public static Layout layout(int w,int h,int vanillaHeight){
        int available=w/2-126,columns=available>=194?3:1;
        int width=columns*62+(columns-1)*4,height=(3/columns)*20+(3/columns-1)*3;
        int bottom=h-7;
        if(available<62)bottom=Math.min(bottom,h-vanillaHeight-7);
        return new Layout(w-width-7,Math.max(7,bottom-height),columns,width,height);
    }
    static int top(int w,int h,int vanillaHeight){return layout(w,h,vanillaHeight).y();}
    static void draw(ForgeGui gui,GuiGraphics g,int w,int h,Network.Snapshot r){
        if(r.school()==0)return;var f=Minecraft.getInstance().font;
        Layout at=layout(w,h,Math.max(gui.leftHeight,gui.rightHeight));
        int[] cds={r.cooldown0(),r.cooldown1(),r.cooldown2()};boolean transparent=HudLayout.compact();
        for(int i=0;i<3;i++){
            int x=at.x()+i%at.columns()*66,y=at.y()+i/at.columns()*23;
            if(transparent)HudLayout.panel(g,x,y,62,20);else InkStyle.target(g,x,y,62,20);
            String text=new String[]{"Z","X","C"}[i]+"  "+(r.stage()<i?"未解锁":cds[i]>0?cds[i]+"秒":"就绪");
            int color=transparent?(cds[i]>0||r.stage()<i?0xBCCDC8:0xF4DEAC):(cds[i]>0||r.stage()<i?InkStyle.MUTED:InkStyle.INK);
            g.drawString(f,text,x+(62-f.width(text))/2,y+6,color,transparent);
        }
    }
}
