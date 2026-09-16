package cn.duskrain;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Creation and invitations use the same typed server actions as /dr guild. */
public final class GuildFormScreen extends Screen {
    final Screen parent;final boolean create;final String verb;EditBox input;Button submit;int left,top,w;
    GuildFormScreen(Screen parent,String verb){super(Component.literal(verb.equals("rename")?"宗门改名":verb.equals("create")?"创立宗门":"邀请道友"));this.parent=parent;this.verb=verb;this.create=!verb.equals("invite");}
    @Override protected void init(){w=Math.min(width-16,330);left=(width-w)/2;top=(height-170)/2;input=new EditBox(font,left+16,top+68,w-32,20,Component.literal(create?"宗门名":"玩家名"));input.setMaxLength(16);addRenderableWidget(input);submit=new ClientUI.Tile(left+16,top+129,(w-40)/2,20,new Network.Entry(verb.equals("rename")?"更改宗门名":create?"创立宗门":"发出邀请",""),()->Network.action("dr guild "+verb+" "+input.getValue()),true);addRenderableWidget(submit);addRenderableWidget(new ClientUI.Tile(left+24+(w-40)/2,top+129,(w-40)/2,20,new Network.Entry("返回",""),this::onClose,true));setInitialFocus(input);}
    @Override public void tick(){input.tick();submit.active=input.getValue().matches(create?"[\\p{L}\\p{N}_]{2,16}":"[A-Za-z0-9_]{1,16}");}
    @Override public void render(GuiGraphics g,int mx,int my,float pt){g.fill(0,0,width,height,0x770D2124);InkStyle.sheet(g,left,top,w,170);g.drawString(font,title,left+16,top+15,InkStyle.INK,false);g.drawString(font,verb.equals("rename")?"中文宗名 2–16 字 · 保留成员与洞天":create?"至少渡劫后期 · 宗务堂登记 · 免费创建":"输入已入服玩家名；邀请有效两分钟",left+16,top+39,InkStyle.MUTED,false);if(System.currentTimeMillis()<ClientUI.messageUntil)g.drawString(font,font.plainSubstrByWidth(ClientUI.message,w-32),left+16,top+103,0x9A553E,false);super.render(g,mx,my,pt);}
    @Override public void onClose(){Minecraft.getInstance().setScreen(parent);}
    @Override public boolean isPauseScreen(){return false;}
}
