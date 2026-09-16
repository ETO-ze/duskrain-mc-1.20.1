package cn.duskrain;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Deliberately virtual selection: no item is put into an unpersisted client container. */
public final class EnchanterScreen extends Screen {
    Enchanting.View view;int x,y,w=360,h=248;int selected=-1;float scale=1;
    EnchanterScreen(Enchanting.View v){super(Component.literal("铭灵轩 · 云篆"));view=v;}
    public static void receive(Enchanting.View v){var mc=Minecraft.getInstance();if(mc.screen instanceof EnchanterScreen s){s.view=v;s.rebuildWidgets();}else mc.setScreen(new EnchanterScreen(v));}
    @Override protected void init(){scale=Math.min(1,Math.min((width-12f)/w,(height-12f)/h));x=Math.round((width/scale-w)/2);y=Math.round((height/scale-h)/2);int row=0;for(var c:view.choices()){int id=c.id();addRenderableWidget(Button.builder(Component.literal(MysticEnchants.NAMES[id]+" → "+c.next()),b->{selected=id;rebuildWidgets();}).bounds(x+104,y+38+row++*22,92,20).build());}
        var choice=view.choices().stream().filter(c->c.id()==selected).findFirst().orElse(null);
        Button apply=Button.builder(Component.literal("确认铭刻"),b->{Network.CHANNEL.sendToServer(new Enchanting.Request(true,view.slot(),selected,view.quote()));b.active=false;}).bounds(x+216,y+129,120,20).build();apply.active=choice!=null&&choice.enabled();addRenderableWidget(apply);
        addRenderableWidget(Button.builder(Component.literal("返回"),b->onClose()).bounds(x+w-53,y+8,43,18).build());
    }
    @Override public void render(GuiGraphics g,int mx,int my,float pt){renderBackground(g);mx=(int)(mx/scale);my=(int)(my/scale);g.pose().pushPose();g.pose().scale(scale,scale,1);InkStyle.sheet(g,x,y,w,h);g.drawString(font,title,x+13,y+12,InkStyle.INK,false);g.drawString(font,"灵石 "+view.money(),x+205,y+13,InkStyle.MUTED,false);
        g.fill(x+21,y+44,x+83,y+105,0x30274649);if(!view.item().isEmpty()){g.pose().pushPose();g.pose().translate(x+36,y+58,0);g.pose().scale(2,2,1);g.renderItem(view.item(),0,0);g.pose().popPose();g.drawString(font,font.plainSubstrByWidth(view.item().getHoverName().getString(),85),x+12,y+112,InkStyle.INK,false);}
        var c=view.choices().stream().filter(q->q.id()==selected).findFirst().orElse(null);if(c!=null){String[] lines={MysticEnchants.DESCRIPTIONS[c.id()]+(c.id()==3?" +"+(int)c.effect():" "+Math.round(c.effect()*100)+"%"),"费用 "+c.price()+" 灵石","青金石 × "+c.lapis()+" · 灵纹砂 × "+c.sand(),"原版经验 "+c.levels()+" 级",Rules.realm(c.stage())+"可铭刻",c.enabled()?"保留原附魔 · 必定成功":c.reason()};for(int i=0;i<lines.length;i++)g.drawString(font,font.plainSubstrByWidth(lines[i],145),x+205,y+39+i*14,i==5&&!c.enabled()?0xA13C35:InkStyle.INK,false);}
        g.drawString(font,"选中背包装备；确认后原位铭刻",x+13,y+155,InkStyle.MUTED,false);
        var p=Minecraft.getInstance().player;if(p!=null)for(int i=0;i<36;i++){int sx=x+16+(i%18)*18,sy=y+172+(i/18)*20;g.fill(sx-1,sy-1,sx+17,sy+17,i==view.slot()?0x909E853E:0x28354F4A);ItemStack stack=p.getInventory().getItem(i);g.renderItem(stack,sx,sy);g.renderItemDecorations(font,stack,sx,sy);if(mx>=sx&&mx<sx+17&&my>=sy&&my<sy+17&&!stack.isEmpty())g.renderTooltip(font,stack,mx,my);}
        if(System.currentTimeMillis()<ClientUI.messageUntil)g.drawString(font,font.plainSubstrByWidth(ClientUI.message,w-24),x+12,y+h-20,InkStyle.INK,false);super.render(g,mx,my,pt);g.pose().popPose();
    }
    @Override public boolean mouseClicked(double mx,double my,int button){mx/=scale;my/=scale;if(button==0)for(int i=0;i<36;i++){int sx=x+16+i%18*18,sy=y+172+i/18*20;if(mx>=sx&&mx<sx+17&&my>=sy&&my<sy+17){selected=-1;Network.CHANNEL.sendToServer(new Enchanting.Request(false,i,0,0));return true;}}return super.mouseClicked(mx,my,button);}
    @Override public boolean mouseReleased(double x,double y,int b){return super.mouseReleased(x/scale,y/scale,b);}
    @Override public boolean isPauseScreen(){return false;}
}
