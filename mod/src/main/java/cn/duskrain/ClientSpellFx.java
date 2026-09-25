package cn.duskrain;

import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.*;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;

@Mod.EventBusSubscriber(modid=DuskRain.ID,value=Dist.CLIENT)
public final class ClientSpellFx {
    static final List<Active> EVENTS=new ArrayList<>();
    static int density=-1;
    static int density(){if(density<0){density=1;try{var f=Minecraft.getInstance().gameDirectory.toPath().resolve("config/duskrain-fx-density.txt");if(java.nio.file.Files.exists(f))density=Math.max(0,Math.min(2,Integer.parseInt(java.nio.file.Files.readString(f).strip())));}catch(Exception ignored){}}return density;}
    static void cycleDensity(){density=(density()+1)%3;try{var f=Minecraft.getInstance().gameDirectory.toPath().resolve("config/duskrain-fx-density.txt");java.nio.file.Files.createDirectories(f.getParent());java.nio.file.Files.writeString(f,String.valueOf(density));}catch(Exception e){DuskRain.LOG.warn("Cannot save FX density",e);}}
    static class Active {SpellFx.Event event;int age;Active(SpellFx.Event e){event=e;}}
    static final float[][] COLORS={{.62f,1,.96f},{1,.78f,.26f},{1,.30f,.10f},{.42f,.75f,1},{.80f,.72f,1},{1,.83f,.34f},{.65f,1,.85f},{1,.16f,.14f},{.65f,1,1},{.75f,.45f,1},{1,.85f,.30f}};
    @SubscribeEvent public static void labels(net.minecraftforge.client.event.RenderLevelStageEvent e){var mc=Minecraft.getInstance();if(e.getStage()!=net.minecraftforge.client.event.RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS||mc.level==null)return;var stack=e.getPoseStack();var camera=e.getCamera().getPosition();var buffers=mc.renderBuffers().bufferSource();Set<String> seen=new HashSet<>();for(Active active:EVENTS){var fx=active.event;if(fx.style()!=SpellFx.BELL||fx.number()<1||!seen.add(fx.from().toString()))continue;stack.pushPose();stack.translate(fx.from().x-camera.x,fx.from().y-camera.y+2,fx.from().z-camera.z);stack.mulPose(e.getCamera().rotation());stack.scale(-.06f,-.06f,.06f);String text=fx.number()==4?"◆ 已鸣":"◇ 第"+fx.number()+"鸣 ◇";mc.font.drawInBatch(text,-mc.font.width(text)/2f,0,0xEEFFDC,false,stack.last().pose(),buffers,net.minecraft.client.gui.Font.DisplayMode.NORMAL,0xAA173A35,15728880);stack.popPose();}buffers.endBatch();}
    public static void receive(SpellFx.Event e){var mc=Minecraft.getInstance();if(mc.player==null||mc.player.distanceToSqr(e.from())>96*96)return;if(EVENTS.size()>=128)EVENTS.remove(0);EVENTS.add(new Active(e));}
    static int budget;
    static void dot(SpellFx.Event e,Vec3 p){if(budget--<=0)return;var l=Minecraft.getInstance().level;if(l==null)return;float[] c=COLORS[e.style()];l.addParticle(new DustParticleOptions(new Vector3f(c[0],c[1],c[2]),e.style()==SpellFx.BELL?1.8f:1.2f),true,p.x,p.y,p.z,0,0,0);}
    static Vec3 lateral(Vec3 direction){Vec3 side=direction.cross(new Vec3(0,1,0));return side.lengthSqr()<.001?new Vec3(1,0,0):side.normalize();}
    static void ring(SpellFx.Event e,Vec3 center,double radius,double phase,int count){
        for(int j=0;j<count;j++){double t=phase+j*Math.PI*2/count;dot(e,center.add(Math.cos(t)*radius,0,Math.sin(t)*radius));}
    }
    static void line(SpellFx.Event e,Vec3 a,Vec3 b,int count){for(int j=0;j<=count;j++)dot(e,a.lerp(b,j/(double)count));}
    static void projectile(SpellFx.Event e,int stride){
        Vec3 delta=e.to().subtract(e.from()),forward=delta.normalize(),side=lateral(forward),up=side.cross(forward).normalize();
        int n=Math.max(2,(int)(delta.length()*10/stride));
        // Bright spine follows the exact server-clipped segment; ornament stays close to it.
        line(e,e.from(),e.to(),n);
        if(e.style()==SpellFx.SWORD){
            for(int j=0;j<=12/stride;j++){double t=-1.15+2.3*j/(12/stride);Vec3 q=e.to().add(side.scale(Math.sin(t)*.38)).subtract(forward.scale((1-Math.cos(t))*.45));dot(e,q);dot(e,q.add(up.scale(.06)));}
            for(int j=0;j<n;j++)dot(e,e.from().lerp(e.to(),j/(double)n).add(up.scale(.09*Math.sin(j*Math.PI/n))));
        }else if(e.style()==SpellFx.FIRE){
            for(int j=0;j<n;j++){double t=j/(double)n,angle=t*Math.PI*4;Vec3 center=e.from().lerp(e.to(),t);dot(e,center.add(side.scale(Math.cos(angle)*.2)).add(up.scale(Math.sin(angle)*.2)));}
            line(e,e.to().add(up.scale(.28)),e.to().subtract(up.scale(.28)),4);
            line(e,e.to().add(side.scale(.16)),e.to().subtract(side.scale(.16)),3);
        }else if(e.style()==SpellFx.BODY){for(int j=0;j<n;j++){Vec3 q=e.from().lerp(e.to(),j/(double)n);dot(e,q.add(side.scale(.35)));dot(e,q.subtract(side.scale(.35)));}}
    }
    static void spell(Active a,int stride){
        var e=a.event;double phase=a.age*.22,r=e.radius(),life=a.age/(double)e.ticks();int n=Math.max(12,48/stride);Vec3 origin=e.from();
        if(e.style()==SpellFx.SWORD){
            // Three rotating crescents reach the authoritative area radius.
            for(int arm=0;arm<3;arm++)for(int j=0;j<n/2;j++){double t=phase+arm*Math.PI*2/3+j*Math.PI/n;dot(e,origin.add(Math.cos(t)*r,Math.sin(t*2)*.13,Math.sin(t)*r));}
            if(a.age==1)ring(e,origin,r,0,n);
        }else if(e.style()==SpellFx.BODY){
            ring(e,origin,r,0,n);ring(e,origin,r*Math.min(1,life*2),phase,n);
            for(int ray=0;ray<6;ray++){double t=ray*Math.PI/3;for(int j=1;j<=4;j++){double rr=r*j/4;dot(e,origin.add(Math.cos(t)*rr,.07*Math.sin(j+phase),Math.sin(t)*rr));}}
        }else if(e.style()==SpellFx.GUARD){
            for(int j=0;j<n;j++){double t=phase+j*Math.PI*2/n;dot(e,origin.add(Math.cos(t)*r,Math.sin(t*2)*.65,Math.sin(t)*r));dot(e,origin.add(Math.cos(t)*r*.7,Math.sin(t)*r,.15));}
        }else if(e.style()==SpellFx.FROST||e.style()==SpellFx.LIGHTNING){
            ring(e,origin,r,0,n);ring(e,origin.add(0,.06,0),r*.73,-phase*.3,n);
            for(int j=0;j<6;j++){double t=j*Math.PI/3+phase*.04;Vec3 a0=origin.add(Math.cos(t)*r*.73,.08,Math.sin(t)*r*.73),b=origin.add(Math.cos(t+Math.PI*2/3)*r*.73,.08,Math.sin(t+Math.PI*2/3)*r*.73);line(e,a0,b,4);}
            if(e.style()==SpellFx.LIGHTNING&&e.number()!=-1){
                Vec3 last=origin;for(int j=1;j<=16;j++){Vec3 next=origin.add(Math.sin(j*17+a.age)*.36,j*.6,Math.cos(j*13+a.age)*.36);line(e,last,next,2);if(j%4==0)line(e,next,next.add(Math.sin(j)*1.6,-1.1,Math.cos(j)*1.6),4);last=next;}
            }else if(e.style()==SpellFx.FROST&&a.age%5==0)for(int j=0;j<6;j++){double t=j*Math.PI/3;line(e,origin.add(Math.cos(t)*r*.6,0,Math.sin(t)*r*.6),origin.add(Math.cos(t)*r*.6,.5,Math.sin(t)*r*.6),3);}
        }else if(e.style()==SpellFx.BELL||e.style()==SpellFx.ERROR){
            ring(e,origin,r,phase,n);if(e.style()==SpellFx.BELL)line(e,origin,origin.add(0,11,0),24);
        }else if(e.style()>=8){
            for(int k=0;k<2;k++)ring(e,origin.add(0,k*.18,0),r*(1-k*.23),phase*(k==0?1:-1),n);
            for(int j=0;j<8;j++){double t=phase*.2+j*Math.PI/4;Vec3 q=origin.add(Math.cos(t)*r,0,Math.sin(t)*r);line(e,q,q.add(0,1.3,0),5);if(e.style()==8)line(e,q.add(-.16,.9,0),q.add(.16,.9,0),2);}
        }
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent tick){
        if(tick.phase!=TickEvent.Phase.END)return;var mc=Minecraft.getInstance();if(mc.level==null||mc.player==null){EVENTS.clear();return;}
        int density=density(),stride=density==0?1:density==1?2:4;budget=new int[]{900,480,180}[density];
        // Fair per-event share prevents an older six-second array starving new sword waves.
        int share=Math.max(12,budget/Math.max(1,EVENTS.size()));
        Iterator<Active> it=EVENTS.iterator();while(it.hasNext()){
            Active a=it.next();var e=a.event;if(a.age++>=e.ticks()||mc.player.distanceToSqr(e.from())>96*96){it.remove();continue;}
            int remaining=budget;budget=Math.min(remaining,share);
            if(e.from().distanceToSqr(e.to())>.01){if(a.age==1)projectile(e,stride);}
            else if(a.age==1||a.age%Math.max(1,stride-1)==0)spell(a,stride);
            budget=remaining-(Math.min(remaining,share)-Math.max(0,budget));
        }
    }
}
