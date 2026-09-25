package cn.duskrain;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.gui.overlay.ForgeGui;

/** The right corner stays outside the vanilla hotbar, offhand and survival indicators. */
public final class SkillHud {
    static final int CELL=20,HEIGHT=20,GAP=2;
    public record Layout(int x,int y,int columns,int width,int height) {}
    public static Layout layout(int w,int h,int vanillaHeight){
        int available=w/2-126,columns=available>=3*CELL+2*GAP?3:1;
        int width=columns*CELL+(columns-1)*GAP,height=(3/columns)*HEIGHT+(3/columns-1)*GAP;
        int bottom=h-7;
        if(available<CELL)bottom=Math.min(bottom,h-vanillaHeight-7);
        return new Layout(w-width-7,Math.max(7,bottom-height),columns,width,height);
    }
    static int top(int w,int h,int vanillaHeight){return layout(w,h,vanillaHeight).y();}
    static void draw(ForgeGui gui,GuiGraphics g,int w,int h,Network.Snapshot r){
        if(r.school()==0)return;var f=Minecraft.getInstance().font;
        Layout at=layout(w,h,Math.max(gui.leftHeight,gui.rightHeight));
        int[] cds={r.cooldown0(),r.cooldown1(),r.cooldown2()};boolean transparent=HudLayout.compact();
        for(int i=0;i<3;i++){
            int x=at.x()+i%at.columns()*(CELL+GAP),y=at.y()+i/at.columns()*(HEIGHT+GAP);
            if(transparent)HudLayout.panel(g,x,y,CELL,HEIGHT);
            else{g.fill(x,y,x+CELL,y+HEIGHT,0xDCEEE8D4);g.fill(x,y,x+CELL,y+1,0xAAAE9565);}
            boolean locked=r.stage()<i;
            int color=transparent?(locked?0x94A8A3:0xF4DEAC):(locked?InkStyle.MUTED:InkStyle.INK);
            String key=ClientUI.SKILL[i].getTranslatedKeyMessage().getString();
            float scale=Math.min(.8f,(CELL-4f)/Math.max(1,f.width(key)));
            g.pose().pushPose();g.pose().translate(x+CELL/2f,y+3,0);g.pose().scale(scale,scale,1);
            g.drawString(f,key,-f.width(key)/2,0,color,transparent);g.pose().popPose();
            if(locked){g.fill(x+CELL/2-3,y+13,x+CELL/2+3,y+17,0xAA889793);g.fill(x+CELL/2-2,y+11,x+CELL/2+2,y+12,0xAA889793);}
            else if(cds[i]>0){String cd=cds[i]>99?"99+":Integer.toString(cds[i]);g.pose().pushPose();g.pose().translate(x+CELL/2f,y+12,0);g.pose().scale(.7f,.7f,1);g.drawString(f,cd,-f.width(cd)/2,0,color,transparent);g.pose().popPose();}
            else g.fill(x+CELL/2-4,y+16,x+CELL/2+4,y+17,transparent?0xE6D5BD83:0xC06B938B);
        }
    }
}
