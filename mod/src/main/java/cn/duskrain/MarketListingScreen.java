package cn.duskrain;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class MarketListingScreen extends Screen {
    final Screen parent;EditBox price,amount;Button submit;int left,top,w,h;
    public MarketListingScreen(Screen parent){super(Component.literal("商铺 · 上架商品"));this.parent=parent;}
    @Override protected void init(){
        w=Math.min(width-12,340);h=Math.min(height-12,214);left=(width-w)/2;top=(height-h)/2;
        price=new EditBox(font,left+100,top+64,w-118,20,Component.literal("整组售价"));price.setMaxLength(9);price.setFilter(s->s.matches("[0-9]*"));price.setValue("10");addRenderableWidget(price);
        amount=new EditBox(font,left+100,top+96,w-118,20,Component.literal("上架数量"));amount.setMaxLength(2);amount.setFilter(s->s.matches("[0-9]*"));amount.setValue("1");addRenderableWidget(amount);
        submit=Button.builder(Component.literal("确认上架"),b->{try{int cost=Integer.parseInt(price.getValue()),count=Integer.parseInt(amount.getValue());if(cost<1||cost>100000000||count<1||count>64)return;Network.action("dr stalls sell "+cost+" "+count);}catch(NumberFormatException ignored){}}).bounds(left+18,top+h-34,(w-42)/2,20).build();addRenderableWidget(submit);
        addRenderableWidget(Button.builder(Component.literal("取消"),b->onClose()).bounds(left+24+(w-42)/2,top+h-34,(w-42)/2,20).build());setInitialFocus(price);
    }
    @Override public void tick(){price.tick();amount.tick();var mc=Minecraft.getInstance();try{int p=Integer.parseInt(price.getValue()),a=Integer.parseInt(amount.getValue());submit.active=mc.player!=null&&p>=1&&p<=100000000&&a>=1&&a<=64&&mc.player.getMainHandItem().getCount()>=a;}catch(NumberFormatException e){submit.active=false;}}
    @Override public void render(GuiGraphics g,int mx,int my,float pt){g.fill(0,0,width,height,0x770D2124);InkStyle.sheet(g,left,top,w,h);g.drawString(font,"烟雨小集 · 寄售实物",left+18,top+16,InkStyle.INK,false);
        var p=Minecraft.getInstance().player;if(p!=null)g.drawString(font,font.plainSubstrByWidth("主手："+p.getMainHandItem().getHoverName().getString()+" × "+p.getMainHandItem().getCount(),w-36),left+18,top+37,InkStyle.MUTED,false);
        g.drawString(font,"整组售价",left+18,top+70,InkStyle.INK,false);g.drawString(font,"上架数量",left+18,top+102,InkStyle.INK,false);
        g.drawString(font,"上架后实物存入货柜；售出直接到账。",left+18,top+130,InkStyle.MUTED,false);
        if(System.currentTimeMillis()<ClientUI.messageUntil)g.drawString(font,font.plainSubstrByWidth(ClientUI.message,w-36),left+18,top+146,0x9A553E,false);super.render(g,mx,my,pt);
    }
    @Override public void onClose(){Minecraft.getInstance().setScreen(parent);}
    @Override public boolean isPauseScreen(){return false;}
}
