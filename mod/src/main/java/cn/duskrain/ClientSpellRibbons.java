package cn.duskrain;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

/** Thin emissive strokes keep sword arcs and magic boundaries readable without a shader pack. */
@Mod.EventBusSubscriber(modid=DuskRain.ID,value=Dist.CLIENT)
public final class ClientSpellRibbons {
    static int budget;
    static void stroke(VertexConsumer b,Matrix4f pose,Vec3 a,Vec3 c,Vec3 side,double width,float[] color,float alpha){
        if(budget--<=0)return;Vec3 offset=side.normalize().scale(width);
        for(Vec3 v:new Vec3[]{a.add(offset),c.add(offset),c.subtract(offset),a.subtract(offset),a.subtract(offset),c.subtract(offset),c.add(offset),a.add(offset)})b.vertex(pose,(float)v.x,(float)v.y,(float)v.z).color(color[0],color[1],color[2],alpha).endVertex();
    }
    @SubscribeEvent public static void render(RenderLevelStageEvent event){
        var mc=Minecraft.getInstance();if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS||mc.player==null||mc.options.hideGui&&mc.screen!=null)return;
        var stack=event.getPoseStack();Vec3 camera=event.getCamera().getPosition();stack.pushPose();stack.translate(-camera.x,-camera.y,-camera.z);Matrix4f pose=stack.last().pose();
        var source=mc.renderBuffers().bufferSource();var buffer=source.getBuffer(RenderType.lightning());budget=new int[]{1200,720,300}[ClientSpellFx.density()];
        var list=ClientSpellFx.EVENTS;
        for(int index=list.size()-1;index>=0&&budget>0;index--){var a=list.get(index);var fx=a.event;if(mc.player.distanceToSqr(fx.from())>64*64)continue;
            float alpha=(float)(.95*Math.max(0,1-(a.age+event.getPartialTick())/fx.ticks()));float[] color=ClientSpellFx.COLORS[fx.style()];Vec3 delta=fx.to().subtract(fx.from());
            if(delta.lengthSqr()>.01){
                Vec3 direction=delta.normalize(),side=ClientSpellFx.lateral(direction),up=side.cross(direction).normalize();
                stroke(buffer,pose,fx.from(),fx.to(),side,.06,color,alpha);stroke(buffer,pose,fx.from(),fx.to(),up,.04,color,alpha);
                if(fx.style()==SpellFx.SWORD){Vec3 last=null;for(int j=0;j<=12;j++){double t=-1.2+j*.2;Vec3 point=fx.to().add(side.scale(Math.sin(t)*.48)).subtract(direction.scale((1-Math.cos(t))*.5));if(last!=null)stroke(buffer,pose,last,point,up,.05,color,alpha);last=point;}}
                continue;
            }
            int style=fx.style();if(style!=SpellFx.SWORD&&style!=SpellFx.BODY&&style!=SpellFx.FROST&&style!=SpellFx.LIGHTNING&&style!=SpellFx.GUARD)continue;
            double radius=fx.radius(),phase=a.age*.12;int points=ClientSpellFx.density()==2?20:36;
            for(int j=0;j<points;j++){
                double t=j*Math.PI*2/points,u=(j+1)*Math.PI*2/points;
                if(style==SpellFx.SWORD&&j%12>8)continue;
                Vec3 x=fx.from().add(Math.cos(t+phase)*radius,style==SpellFx.GUARD?Math.sin(t*2)*.65:.04,Math.sin(t+phase)*radius),y=fx.from().add(Math.cos(u+phase)*radius,style==SpellFx.GUARD?Math.sin(u*2)*.65:.04,Math.sin(u+phase)*radius);
                stroke(buffer,pose,x,y,style==SpellFx.GUARD?new Vec3(0,1,0):x.subtract(fx.from()),.055,color,Math.max(.18f,alpha));
            }
            if(style==SpellFx.LIGHTNING&&fx.number()!=-1){Vec3 last=fx.from();for(int j=1;j<=16;j++){Vec3 next=fx.from().add(Math.sin(j*17+a.age)*.36,j*.6,Math.cos(j*13+a.age)*.36);stroke(buffer,pose,last,next,new Vec3(1,0,0),.10,color,alpha);if(j%4==0)stroke(buffer,pose,next,next.add(Math.sin(j)*1.6,-1.1,Math.cos(j)*1.6),new Vec3(1,0,0),.025,color,alpha);last=next;}}
        }
        source.endBatch(RenderType.lightning());stack.popPose();
    }
}
