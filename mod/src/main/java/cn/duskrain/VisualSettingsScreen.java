package cn.duskrain;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.opengl.GL11;

/** Responsive, local-only quality settings. Applying a preset never sends a server command. */
public final class VisualSettingsScreen extends Screen {
    final Screen parent; int left,top,pw,ph,row; Runnable pending; int delay;
    public VisualSettingsScreen(Screen parent){super(Component.literal("烟雨画境 · 画质与显示"));this.parent=parent;}
    @Override protected void init(){
        pw=Math.min(width-12,600);ph=Math.min(height-12,348);left=(width-pw)/2;top=(height-ph)/2;row=Math.min(72,(ph-152)/3);
        for(int i=0;i<VisualProfiles.DEFINITION.presets().length;i++){
            var p=VisualProfiles.DEFINITION.presets()[i];int y=top+50+i*row;
            var b=new PresetButton(left+12,y,pw-24,row-4,p,()->schedule(()->VisualProfiles.apply(p.id())));b.active=VisualProfiles.available();addRenderableWidget(b);
        }
        int y=top+50+3*row+6;int w=(pw-30)/2;
        addRenderableWidget(button(left+12,y,w,"关闭光影",()->schedule(()->VisualProfiles.apply("off"))));
        addRenderableWidget(button(left+18+w,y,w,"恢复上次设置",()->schedule(VisualProfiles::restore)));
        addRenderableWidget(button(left+12,y+24,w,"侧栏设置",()->{if(Minecraft.getInstance().player!=null)Network.action("dr sidebar");}));
        var test=button(left+18+w,y+24,w,"记录 30 秒帧率",()->{if(Minecraft.getInstance().level!=null){FrameProbe.start("manual",30);Minecraft.getInstance().setScreen(null);}});test.active=Minecraft.getInstance().level!=null;addRenderableWidget(test);
        addRenderableWidget(button(left+12,y+48,w,HudLayout.compact()?"HUD：轻量 · 点击展开":"HUD：完整 · 点击收起",()->{HudLayout.toggle();rebuildWidgets();}));
        addRenderableWidget(button(left+18+w,y+48,w,ClientHud.enabled()?"左上 HUD：显示":"左上 HUD：隐藏",()->{ClientHud.toggle();rebuildWidgets();}));
        addRenderableWidget(button(left+pw-58,top+9,45,"返回",this::onClose));
        if(pw>=380)addRenderableWidget(button(left+pw-164,top+9,98,"特效："+new String[]{"细致","均衡","精简"}[ClientSpellFx.density()],()->{ClientSpellFx.cycleDensity();rebuildWidgets();}));
    }
    Button button(int x,int y,int w,String label,Runnable action){return new ClientUI.Tile(x,y,w,20,new Network.Entry(label,""),action,true);}
    void schedule(Runnable action){pending=action;delay=3;VisualProfiles.result="正在应用画质，编译光影时会短暂停顿…";children().forEach(c->{if(c instanceof Button b)b.active=false;});}
    @Override public void tick(){if(pending!=null&&--delay<=0){var action=pending;pending=null;action.run();rebuildWidgets();}}
    @Override public void onClose(){if(pending==null)Minecraft.getInstance().setScreen(parent);}
    @Override public boolean isPauseScreen(){return false;}
    @Override public void render(GuiGraphics g,int mx,int my,float pt){
        g.fill(0,0,width,height,0xAD112329);g.fill(left,top,left+pw,top+ph,ClientUI.PAPER);g.fill(left,top,left+pw,top+2,ClientUI.GOLD);
        g.drawString(font,"DuskRain · 烟雨画境",left+12,top+13,ClientUI.INK,false);
        String gpu=GL11.glGetString(GL11.GL_RENDERER);g.drawString(font,font.plainSubstrByWidth(gpu==null?"选择适合你的画质":gpu,pw-24),left+12,top+33,ClientUI.MUTED,false);
        int y=top+50+3*row+81;String status=VisualProfiles.available()?VisualProfiles.result:"光影组件未齐备，请使用完整客户端包。";
        for(var line:font.split(Component.literal(status),pw-24)){if(y+9>top+ph-2)break;g.drawString(font,line,left+12,y,ClientUI.TEAL,false);y+=10;}
        super.render(g,mx,my,pt);
    }
    static final class PresetButton extends Button {
        final VisualProfiles.Preset p;
        PresetButton(int x,int y,int w,int h,VisualProfiles.Preset p,Runnable action){super(x,y,w,h,Component.literal(p.title()),b->action.run(),DEFAULT_NARRATION);this.p=p;
            setTooltip(Tooltip.create(Component.literal(p.hardware()+" · "+p.resolution()+"\n"+p.detail()+"\n显卡档位为设计参考，实际帧率受分辨率和场景影响。")));}
        @Override public void renderWidget(GuiGraphics g,int mx,int my,float pt){var f=Minecraft.getInstance().font;int x=getX(),y=getY(),w=getWidth(),h=getHeight();boolean selected=p.id().equals(VisualProfiles.selected());
            g.fill(x,y,x+w,y+h,isHoveredOrFocused()?0xFFE2DDC8:0xFFF8F5E9);g.fill(x,y,x+3,y+h,selected?ClientUI.TEAL:ClientUI.GOLD);
            g.drawString(f,p.title()+"  /  "+p.hardware(),x+10,y+6,ClientUI.INK,false);
            String info=p.resolution()+" · 视距 "+p.renderDistance()+" 区块";g.drawString(f,f.plainSubstrByWidth(info,w-72),x+10,y+19,ClientUI.MUTED,false);
            if(h>=48){int yy=y+34;for(var line:f.split(Component.literal(p.detail()),w-26)){if(yy+9>y+h-3)break;g.drawString(f,line,x+10,yy,ClientUI.TEAL,false);yy+=10;}}
            g.drawString(f,selected?"已选择":"应用",x+w-45,y+19,ClientUI.TEAL,false);
        }
    }
}
