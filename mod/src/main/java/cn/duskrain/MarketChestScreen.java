package cn.duskrain;

import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class MarketChestScreen extends Screen {
    MarketStock.View view;int x,y,w=400,h=268,inventorySlot=-1;UUID selected=MarketStock.NONE;EditBox quantity,price;float scale=1;static long sequence=System.currentTimeMillis();
    MarketChestScreen(MarketStock.View v){super(Component.literal(v.name()));view=v;}
    public static void receive(MarketStock.View v){var mc=Minecraft.getInstance();if(mc.screen instanceof MarketChestScreen s&&s.view.site()==v.site()){s.view=v;s.rebuildWidgets();}else mc.setScreen(new MarketChestScreen(v));}
    int number(EditBox b,int fallback){try{return Integer.parseInt(b.getValue());}catch(Exception e){return fallback;}}
    void send(String verb){Network.CHANNEL.sendToServer(new MarketStock.Request(verb,view.site(),selected,inventorySlot,number(quantity,1),number(price,0),view.revision(),++sequence));}
    MarketStock.Row row(){return view.rows().stream().filter(r->r.id().equals(selected)).findFirst().orElse(null);}
    @Override protected void init(){
        scale=Math.min(1,Math.min((width-12f)/w,(height-12f)/h));x=Math.round((width/scale-w)/2);y=Math.round((height/scale-h)/2);
        quantity=new EditBox(font,x+247,y+88,132,18,Component.literal("数量"));quantity.setFilter(s->s.matches("[0-9]*"));quantity.setMaxLength(4);quantity.setValue("1");addRenderableWidget(quantity);
        price=new EditBox(font,x+247,y+112,132,18,Component.literal("单价"));price.setFilter(s->s.matches("[0-9]*"));price.setMaxLength(9);price.setValue(row()==null?"10":Long.toString(row().price()));addRenderableWidget(price);price.setVisible(view.mine());
        int by=y+143;
        if(view.vacant())button("购铺 "+Rules.current.marketBoothPrice,"booth",x+216,by,164);
        else if(view.mine()){button("上架选中物品","list",x+216,by,164);button("补货","restock",x+216,by+24,78);button("改价","price",x+301,by+24,79);button("取回所选数量","take",x+216,by+48,164);}
        else button("确认购买","buy",x+216,by,164);
        button("刷新","refresh",x+286,y+h-29,44);addRenderableWidget(Button.builder(Component.literal("关闭"),b->onClose()).bounds(x+337,y+h-29,44,20).build());
    }
    void button(String label,String verb,int x,int y,int width){addRenderableWidget(Button.builder(Component.literal(label),b->{send(verb);b.active=false;}).bounds(x,y,width,20).build());}
    @Override public void render(GuiGraphics g,int mx,int my,float pt){
        renderBackground(g);mx=(int)(mx/scale);my=(int)(my/scale);g.pose().pushPose();g.pose().scale(scale,scale,1);InkStyle.sheet(g,x,y,w,h);
        g.drawString(font,title,x+12,y+11,InkStyle.INK,false);g.drawString(font,view.vacant()?"空置":view.mine()?"我的商铺 · 27种商品":"铺主 "+view.owner(),x+12,y+27,InkStyle.MUTED,false);
        for(int i=0;i<27;i++){int sx=x+14+i%9*20,sy=y+47+i/9*20;var r=i<view.rows().size()?view.rows().get(i):null;g.fill(sx-1,sy-1,sx+18,sy+18,r!=null&&r.id().equals(selected)?0x889F8639:0x30325450);if(r!=null){g.renderItem(r.sample(),sx,sy);g.renderItemDecorations(font,r.sample(),sx,sy,Integer.toString(r.quantity()));if(mx>=sx&&mx<sx+18&&my>=sy&&my<sy+18)g.renderTooltip(font,r.sample(),mx,my);}}
        var r=row();g.drawString(font,"灵石 "+view.money(),x+214,y+27,InkStyle.INK,false);if(r!=null){g.drawString(font,font.plainSubstrByWidth(r.sample().getHoverName().getString(),164),x+214,y+47,InkStyle.INK,false);g.drawString(font,r.legacy()?"旧批次 · 整组 "+r.price():r.price()+" / 件 · 库存 "+r.quantity(),x+214,y+61,InkStyle.MUTED,false);long total=r.legacy()?r.price():r.price()*Math.max(0,Math.min(4096,number(quantity,1)));g.drawString(font,"合计 "+total+" 灵石",x+214,y+75,InkStyle.INK,false);}
        g.drawString(font,"数量",x+214,y+93,InkStyle.INK,false);if(view.mine())g.drawString(font,"单价",x+214,y+117,InkStyle.INK,false);
        if(view.mine()){g.drawString(font,"从背包选择实物后上架",x+14,y+119,InkStyle.MUTED,false);var p=Minecraft.getInstance().player;if(p!=null)for(int i=0;i<36;i++){int sx=x+14+i%9*20,sy=y+135+i/9*20;g.fill(sx-1,sy-1,sx+18,sy+18,i==inventorySlot?0x889F8639:0x20325450);var item=p.getInventory().getItem(i);g.renderItem(item,sx,sy);g.renderItemDecorations(font,item,sx,sy);if(mx>=sx&&mx<sx+18&&my>=sy&&my<sy+18&&!item.isEmpty())g.renderTooltip(font,item,mx,my);}}
        g.drawString(font,"商品由服务端保管 · 铺主离线仍可收款",x+14,y+220,InkStyle.MUTED,false);
        if(System.currentTimeMillis()<ClientUI.messageUntil)g.drawString(font,font.plainSubstrByWidth(ClientUI.message,260),x+12,y+h-20,InkStyle.INK,false);
        super.render(g,mx,my,pt);g.pose().popPose();
    }
    @Override public boolean mouseClicked(double mx,double my,int button){mx/=scale;my/=scale;if(button==0){for(int i=0;i<view.rows().size();i++){int sx=x+14+i%9*20,sy=y+47+i/9*20;if(mx>=sx&&mx<sx+18&&my>=sy&&my<sy+18){selected=view.rows().get(i).id();rebuildWidgets();return true;}}if(view.mine())for(int i=0;i<36;i++){int sx=x+14+i%9*20,sy=y+135+i/9*20;if(mx>=sx&&mx<sx+18&&my>=sy&&my<sy+18){inventorySlot=i;return true;}}}return super.mouseClicked(mx,my,button);}
    @Override public boolean mouseReleased(double x,double y,int b){return super.mouseReleased(x/scale,y/scale,b);}
    @Override public boolean isPauseScreen(){return false;}
}
