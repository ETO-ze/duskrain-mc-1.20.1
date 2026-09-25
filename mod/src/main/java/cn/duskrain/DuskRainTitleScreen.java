package cn.duskrain;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;
import net.minecraft.Util;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** DuskRain presentation only. Authentication remains in the launcher's external-login flow. */
@Mod.EventBusSubscriber(modid=DuskRain.ID,value=Dist.CLIENT)
public final class DuskRainTitleScreen extends TitleScreen {
    private static final String ADDRESS="nbc.rainplay.cn:42741";
    private int left,top,panelWidth,panelHeight;
    @SubscribeEvent public static void opening(ScreenEvent.Opening event){
        if(event.getNewScreen()!=null&&event.getNewScreen().getClass()==TitleScreen.class)
            event.setNewScreen(new DuskRainTitleScreen());
    }
    @Override protected void init(){
        panelWidth=Math.min(440,width-24);panelHeight=Math.min(290,height-16);
        left=(width-panelWidth)/2;top=(height-panelHeight)/2;
        int w=Math.min(240,panelWidth-36),x=(width-w)/2;
        int y=top+Math.min(104,panelHeight-125),gap=24;
        button(x,y,w,"启程 · 进入 DuskRain",()->ConnectScreen.startConnecting(this,minecraft,ServerAddress.parseString(ADDRESS),new ServerData("DuskRain · 烟雨仙途",ADDRESS,false),false));
        button(x,y+gap,(w-6)/2,"衣冠阁 · 注册与皮肤",()->Util.getPlatform().openUri("https://skin.duskrain.cn"));
        button(x+(w+6)/2,y+gap,(w-6)/2,"入世指南",()->minecraft.setScreen(new Guide(this)));
        button(x,y+gap*2,(w-6)/2,"单人世界",()->minecraft.setScreen(new SelectWorldScreen(this)));
        button(x+(w+6)/2,y+gap*2,(w-6)/2,"服务器列表",()->minecraft.setScreen(new JoinMultiplayerScreen(this)));
        button(x,y+gap*3,(w-6)/2,"设置",()->minecraft.setScreen(new OptionsScreen(this,minecraft.options)));
        button(x+(w+6)/2,y+gap*3,(w-6)/2,"退出游戏",()->minecraft.stop());
    }
    private void button(int x,int y,int w,String text,Runnable action){addRenderableWidget(new InkButton(x,y,w,22,text,action));}
    @Override public void render(GuiGraphics g,int mx,int my,float dt){
        g.fill(0,0,width,height,0xFF18383D);
        InkStyle.sheet(g,left,top,panelWidth,panelHeight);
        int titleY=top+18;
        g.pose().pushPose();g.pose().translate(width/2f,titleY,0);g.pose().scale(panelHeight<200?1f:1.6f,panelHeight<200?1f:1.6f,1);
        g.drawCenteredString(font,"DuskRain",0,0,InkStyle.INK);g.pose().popPose();
        if(panelHeight>=200)g.drawCenteredString(font,"烟 雨 仙 途",width/2,titleY+24,InkStyle.GOLD);
        if(panelHeight>=260)g.drawCenteredString(font,"青瓦映山水 · 执剑入云间",width/2,titleY+45,InkStyle.MUTED);
        for(var widget:renderables)widget.render(g,mx,my,dt);
        g.drawCenteredString(font,"群号 205255670  ·  Minecraft 1.20.1",width/2,top+panelHeight-22,InkStyle.INK);
    }
    static final class InkButton extends Button {
        InkButton(int x,int y,int w,int h,String text,Runnable action){super(x,y,w,h,Component.literal(text),b->action.run(),DEFAULT_NARRATION);}
        @Override public void renderWidget(GuiGraphics g,int mx,int my,float dt){
            g.fill(getX(),getY(),getX()+width,getY()+height,isHoveredOrFocused()?0xFF496965:0xFF264A4D);
            g.fill(getX(),getY(),getX()+2,getY()+height,InkStyle.GOLD);
            g.drawCenteredString(Minecraft.getInstance().font,getMessage(),getX()+width/2,getY()+(height-8)/2,0xFFECE5D0);
        }
    }
    private static final class Guide extends Screen {
        private final Screen parent;
        private int scroll,maxScroll;
        Guide(Screen parent){super(Component.literal("DuskRain 入世指南"));this.parent=parent;}
        @Override protected void init(){addRenderableWidget(new InkButton(width/2-60,height-32,120,22,"收卷 · 返回",this::onClose));}
        @Override public void onClose(){minecraft.setScreen(parent);}
        @Override public boolean mouseScrolled(double x,double y,double delta){scroll=Math.max(0,Math.min(maxScroll,scroll-(int)(delta*22)));return true;}
        @Override public void render(GuiGraphics g,int mx,int my,float dt){
            int w=Math.min(480,width-20),x=(width-w)/2;
            InkStyle.sheet(g,x,6,w,height-12);
            g.drawCenteredString(font,title,width/2,19,InkStyle.INK);
            String[] lines={"01  启动器选择 DuskRain 衣冠阁外置登录；先注册，再创建角色。",
                "02  点击启程进入服务器。无效会话时退出游戏，在启动器重新登录。",
                "03  G 打开仙途菜单；Z / X / C 施放技能；R 御剑（需解锁或体验资格）。",
                "04  左上角小地图；J 大地图；F8 隐藏地图；F9 切换侧栏显示。",
                "05  新人领取入世手册，点击开始进入主城。主城安全，野外允许 PVP。",
                "06  修炼、任务、商店、住宅、宗门与试炼：从 G 菜单进入。",
                "07  箱子商铺支持多商品寄售；主城附魔师提供专属词条。",
                "服务器："+ADDRESS+"  ·  群号：205255670"};
            g.enableScissor(x+8,36,x+w-8,height-40);
            int y=40-scroll;for(String line:lines){for(var part:font.split(Component.literal(line),w-32)){g.drawString(font,part,x+16,y,InkStyle.INK,false);y+=11;}y+=5;}
            maxScroll=Math.max(0,y+scroll-(height-40));g.disableScissor();
            super.render(g,mx,my,dt);
        }
    }
}
